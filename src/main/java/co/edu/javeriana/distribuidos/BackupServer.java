package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import org.json.JSONObject;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackupServer {

    public BackupServer(String ipServidor) {
        try (ZContext ctx = new ZContext()) {
            // Socket SUB para recibir heartbeat
            Socket subscriber = ctx.createSocket(SocketType.SUB);
            subscriber.connect("tcp://" + ipServidor + ":5572");
            subscriber.subscribe(ZMQ.SUBSCRIPTION_ALL);

            String semestre = "2023-2"; // Semestre por defecto
            long lastHeartbeat = System.currentTimeMillis();

            // Backend local para workers
            Socket backend = ctx.createSocket(SocketType.ROUTER);
            backend.bind("inproc://backend");

            // Lanzar workers
            for (int threadNbr = 0; threadNbr < 10; threadNbr++) {
                new Thread(new ServerWorker(ctx, semestre, threadNbr)).start();
            }

            System.out.println("Servidor de respaldo iniciado, escuchando heartbeat...");

            boolean isPrimary = false;
            Thread servidorPrincipal = null;

            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(subscriber, ZMQ.DONTWAIT);
                long now = System.currentTimeMillis();

                if (msg != null) {
                    lastHeartbeat = now;

                    if (isPrimary) {
                        System.out.println("Servidor principal volvió. El respaldo puede finalizar su rol activo.");
                        isPrimary = false;
                        // En una implementación más avanzada podrías detener el servidor aquí si es necesario
                    }

                    try {
                        String jsonData = msg.getLast().getString(StandardCharsets.UTF_8);
                        JSONObject root = new JSONObject(jsonData);
                        semestre = root.getString("semestre");

                        Path dataDir = Paths.get("data");
                        if (!Files.exists(dataDir)) {
                            Files.createDirectories(dataDir);
                        }

                        Files.write(dataDir.resolve("Salones" + semestre + ".json"),
                                root.getJSONArray("salones").toString(2).getBytes(StandardCharsets.UTF_8));
                        Files.write(dataDir.resolve("Laboratorios" + semestre + ".json"),
                                root.getJSONArray("laboratorios").toString(2).getBytes(StandardCharsets.UTF_8));

                    } catch (Exception e) {
                        System.err.println("Error al guardar archivos de backup:");
                        e.printStackTrace();
                    }

                    msg.destroy();
                } else if (!isPrimary && (now - lastHeartbeat > 3000)) {
                    System.out.println("¡Servidor principal inactivo! Asumiendo rol principal...");

                    final String finalSemestre = semestre;
                    servidorPrincipal = new Thread(() -> {
                        try {
                            Server.main(new String[]{finalSemestre});
                        } catch (Exception e) {
                            System.out.println("Error ejecutando servidor principal desde respaldo.");
                            e.printStackTrace();
                        }
                    });
                    servidorPrincipal.start();
                    isPrimary = true;
                }

                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println("Error en el servidor de respaldo:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Uso: mvn exec:java -Dexec.mainClass=co.edu.javeriana.distribuidos.BackupServer -Dexec.args=ipServidor");
            System.exit(1);
        }
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor de respaldo ejecutándose en " + ip);
        new BackupServer(args[0]);
    }
}
