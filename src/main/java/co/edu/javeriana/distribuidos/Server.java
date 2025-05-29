package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Server {

    private static ZContext ctx;
    private static Socket frontend;
    private static Socket backend;
    private static Thread heartbeatThread;

    public Server(String semestre) {
        ctx = new ZContext();

        frontend = ctx.createSocket(SocketType.ROUTER);
        frontend.bind("tcp://*:5570");

        backend = ctx.createSocket(SocketType.DEALER);
        backend.bind("inproc://backend");

        // Workers
        for (int threadNbr = 0; threadNbr < 10; threadNbr++) {
            new Thread(new ServerWorker(ctx, threadNbr)).start();
        }

        // Heartbeat thread
        heartbeatThread = new Thread(() -> {
            try (ZContext ctxHeartbeat = new ZContext()) {
                Socket publisher = ctxHeartbeat.createSocket(SocketType.PUB);
                publisher.bind("tcp://*:5590");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String salonesData = leerJson("Salones" + semestre + ".json",semestre);
                        String laboratoriosData = leerJson("Laboratorios" + semestre + ".json",semestre);

                        String heartbeatJson = String.format(
                                "{\"status\": \"estoy vivo\", \"semestre\": \"%s\", \"salones\": %s, \"laboratorios\": %s}",
                                semestre, salonesData, laboratoriosData
                        );

                        publisher.send(heartbeatJson);
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        System.err.println("Error leyendo archivos JSON para heartbeat: " + e.getMessage());
                    }
                }
            }
        });

        heartbeatThread.start();

        // Proxy en un hilo separado para poder cerrar
        Thread proxyThread = new Thread(() -> {
            try {
                ZMQ.proxy(frontend, backend, null);
            } catch (Exception e) {
                System.out.println("Proxy cerrado.");
            }
        });
        proxyThread.start();
    }

    private String leerJson(String nombreArchivo, String semestre) throws Exception {
        java.nio.file.Path path = Paths.get("data", nombreArchivo);
        if (!Files.exists(path)) {
            Recursos.verificarSalones(semestre);
            Recursos.verificarLaboratorios(semestre);
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    // Método para cerrar el servidor
    public static void shutdown() {
        System.out.println("Cerrando servidor principal...");
        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            heartbeatThread.interrupt();
        }
        if (ctx != null && !ctx.isClosed()) {
            ctx.close();  // Esto cierra frontend, backend y workers
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.Server' '-Dexec.args=semestre'");
            return;
        }
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor iniciado en " + ip + "... esperando solicitudes de recursos.");
        new Server(args[0]);
    }
}
