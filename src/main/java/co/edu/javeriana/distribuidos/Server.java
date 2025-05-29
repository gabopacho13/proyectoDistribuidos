package co.edu.javeriana.distribuidos;

import org.zeromq.ZContext;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.net.InetAddress;

public class Server {

    private static volatile boolean running = true;

    public Server(String semestre) throws Exception {
        try (ZContext ctx = new ZContext()) {

            // Lanzar workers conectados al backend interno
            for (int i = 0; i < 10; i++) {
                new Thread(new ServerWorker(ctx, semestre, i)).start();
            }

            // Lanzar hilo de heartbeat (como antes)
            Thread heartbeatThread = new Thread(() -> {
                try (ZContext ctxHeartbeat = new ZContext()) {
                    var publisher = ctxHeartbeat.createSocket(SocketType.PUB);
                    publisher.bind("tcp://*:5572");

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String salonesData = leerJson("Salones" + semestre + ".json", semestre);
                            String laboratoriosData = leerJson("Laboratorios" + semestre + ".json", semestre);

                            String heartbeatJson = String.format(
                                    "{\"status\": \"estoy vivo\", \"semestre\": \"%s\", \"salones\": %s, \"laboratorios\": %s}",
                                    semestre, salonesData, laboratoriosData
                            );

                            publisher.send(heartbeatJson);
                            Thread.sleep(2000);
                        } catch (Exception e) {
                            System.err.println("Error leyendo JSON en heartbeat: " + e.getMessage());
                        }
                    }
                }
            });
            heartbeatThread.start();

            // Crear ROUTER para clientes
            ZMQ.Socket frontend = ctx.createSocket(SocketType.ROUTER);
            frontend.bind("tcp://*:5570");

            // Crear DEALER backend interno para workers
            ZMQ.Socket backend = ctx.createSocket(SocketType.DEALER);
            backend.bind("inproc://backend");

            System.out.println("Broker embebido iniciado (frontend: tcp://*:5570, backend: inproc://backend)");

            // Proxy interno (broker): conecta clientes y workers
            ZMQ.proxy(frontend, backend, null);

            System.out.println("Servidor detenido.");
        }
    }

    private static String leerJson(String nombreArchivo, String semestre) throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get("data", nombreArchivo);
        if (!java.nio.file.Files.exists(path)) {
            Recursos.verificarSalones(semestre);
            Recursos.verificarLaboratorios(semestre);
        }
        return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.Server' '-Dexec.args=semestre'");
            return;
        }

        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor iniciado en " + ip + " con broker embebido...");
        new Server(args[0]);
    }
}
