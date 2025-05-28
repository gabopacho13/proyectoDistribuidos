package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class BackupServer {

    public BackupServer() {
        try (ZContext ctx = new ZContext()) {
            // Socket para recibir heartbeat
            Socket subscriber = ctx.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5572"); // PONER IP  del servidor principal
            subscriber.subscribe("");

            long lastHeartbeat = System.currentTimeMillis();

            // Socket interno para workers
            Socket backend = ctx.createSocket(SocketType.DEALER);
            backend.bind("inproc://backend");

            // Lanzar workers (igual que el principal)
            for (int threadNbr = 0; threadNbr < 10; threadNbr++) {
                new Thread(new ServerWorker(ctx, threadNbr)).start();
            }

            System.out.println("Servidor de respaldo iniciado, escuchando heartbeat...");

            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(subscriber, ZMQ.DONTWAIT);
                if (msg != null) {
                    lastHeartbeat = System.currentTimeMillis();
                    String jsonData = msg.getLast().getString(StandardCharsets.UTF_8);
                    System.out.println("Heartbeat con datos recibido del servidor principal.");

                    // Parsear el JSON recibido
                    org.json.JSONObject root = new org.json.JSONObject(jsonData);

                    java.nio.file.Files.write(
                        java.nio.file.Paths.get("Salones.json"),
                        root.getJSONArray("salones").toString(2).getBytes(StandardCharsets.UTF_8)
                    );
                    java.nio.file.Files.write(
                        java.nio.file.Paths.get("Laboratorios.json"),
                        root.getJSONArray("laboratorios").toString(2).getBytes(StandardCharsets.UTF_8)
                    );
                    java.nio.file.Files.write(
                        java.nio.file.Paths.get("ProgramasPorFacultad.json"),
                        root.getJSONObject("programas").toString(2).getBytes(StandardCharsets.UTF_8)
                    );

                    msg.destroy();
                } else if (System.currentTimeMillis() - lastHeartbeat > 3000) {
                    System.out.println("¡Servidor principal inactivo! Asumiendo rol principal en puerto 5570...");
                    subscriber.close();

                    Socket frontend = ctx.createSocket(SocketType.ROUTER);
                    frontend.bind("tcp://*:5570");
                    ZMQ.proxy(frontend, backend, null);
                    break;
                }
                Thread.sleep(500);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor de respaldo en " + ip + " listo para asumir si el principal falla.");
        new BackupServer();
    }
}
