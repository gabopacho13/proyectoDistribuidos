package co.edu.javeriana.distribuidos;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class BackupServer {

    public BackupServer(String ipServidor) {
        try (ZContext ctx = new ZContext()) {
            // Socket para recibir heartbeat
            Socket subscriber = ctx.createSocket(SocketType.SUB);
            subscriber.connect("tcp://" + ipServidor + ":5572"); // PONER IP  del servidor principal
            subscriber.subscribe("");
            String semestre = "2023-2"; // Asignar semestre por defecto, se puede modificar según necesidad

            long lastHeartbeat = System.currentTimeMillis();

            // Socket interno para workers
            Socket backend = ctx.createSocket(SocketType.ROUTER);
            backend.bind("inproc://backend");

            // Lanzar workers (igual que el principal)
            for (int threadNbr = 0; threadNbr < 10; threadNbr++) {
                new Thread(new ServerWorker(ctx, threadNbr)).start();
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
                        if (servidorPrincipal != null && servidorPrincipal.isAlive()) {
                            System.out.println("Servidor original volvió. Cerrando el servidor temporal...");
                            Server.shutdown();  // Llama al método para cerrar el servidor
                            servidorPrincipal.join();  // Espera a que termine
                        }
                        isPrimary = false;
                    }

                    // procesar JSON, guardar archivos
                    String jsonData = msg.getLast().getString(StandardCharsets.UTF_8);
                    org.json.JSONObject root = new org.json.JSONObject(jsonData);
                    semestre = root.getString("semestre");
                    // Guardar los archivos en la carpeta "data"
                    java.nio.file.Path dataDir = java.nio.file.Paths.get("data");
                    if (!java.nio.file.Files.exists(dataDir)) {
                        java.nio.file.Files.createDirectories(dataDir);
                    }

                    java.nio.file.Files.write(
                            dataDir.resolve("Salones" + semestre + ".json"),
                            root.getJSONArray("salones").toString(2).getBytes(StandardCharsets.UTF_8)
                    );
                    java.nio.file.Files.write(
                            dataDir.resolve("Laboratorios" + semestre + ".json"),
                            root.getJSONArray("laboratorios").toString(2).getBytes(StandardCharsets.UTF_8)
                    );

                    msg.destroy();
                } else if (!isPrimary && (now - lastHeartbeat > 3000)) {
                    System.out.println("¡Servidor principal inactivo! Asumiendo rol principal...");
                    String finalSemestre = semestre;
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
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.BackupServer' '-Dexec.args=ipServidor'");
            System.exit(1);
        }
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor de respaldo en " + ip + " listo para asumir si el principal falla.");
        new BackupServer(args[0]);
    }
}
