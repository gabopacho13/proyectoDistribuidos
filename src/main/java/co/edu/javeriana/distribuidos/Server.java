package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Queue;

public class Server {

    private static ZContext ctx;
    private static Socket frontend;
    private static Socket backend;
    private static Thread heartbeatThread;
    private static volatile boolean running = true;
    private static final Queue<String> workerQueue = new LinkedList<>();

    public Server(String semestre) throws Exception {
        ctx = new ZContext();

        frontend = ctx.createSocket(SocketType.ROUTER);
        frontend.bind("tcp://*:5570");

        backend = ctx.createSocket(SocketType.ROUTER);
        backend.bind("inproc://backend");

        // Iniciar workers
        for (int threadNbr = 0; threadNbr < 10; threadNbr++)
            new Thread(new ServerWorker(ctx, threadNbr)).start();

        // Iniciar thread de heartbeat
        heartbeatThread = new Thread(() -> {
            try (ZContext ctxHeartbeat = new ZContext()) {
                Socket publisher = ctxHeartbeat.createSocket(SocketType.PUB);
                publisher.bind("tcp://*:5572");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String salonesData = leerJson("Salones" + semestre + ".json");
                        String laboratoriosData = leerJson("Laboratorios" + semestre + ".json");

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

        // Ciclo principal del servidor
        while (running && !Thread.currentThread().isInterrupted()) {
            ZMQ.Poller items = ctx.createPoller(2);
            items.register(backend, ZMQ.Poller.POLLIN);
            if (!workerQueue.isEmpty()) {
                items.register(frontend, ZMQ.Poller.POLLIN);
            }

            if (items.poll(1000) < 0) {
                break;
            }

            if (items.pollin(0)) {
                // backend activity
                String workerAddr = backend.recvStr();
                String empty = backend.recvStr();
                assert (empty.length() == 0);
                String clientAddr = backend.recvStr();

                if (!"READY".equals(clientAddr)) {
                    empty = backend.recvStr();
                    assert (empty.length() == 0);
                    String reply = backend.recvStr();
                    System.out.println("Enviando respuesta al cliente: " + clientAddr);
                    frontend.sendMore(clientAddr);
                    frontend.sendMore("");
                    frontend.send(reply);
                }

                // Agregar worker a la cola
                workerQueue.add(workerAddr);
            }

            if (items.pollin(1)) {
                // frontend activity
                String clientAddr = frontend.recvStr();
                String empty = frontend.recvStr();
                assert (empty.length() == 0);
                String request = frontend.recvStr();

                String workerAddr = workerQueue.poll();
                System.out.println("Enviando solicitud a: " + workerAddr);
                backend.sendMore(workerAddr);
                backend.sendMore("");
                backend.sendMore(clientAddr);
                backend.sendMore("");
                backend.send(request);
            }
        }

        // Cierre de recursos
        shutdown();
    }

    private String leerJson(String nombreArchivo) throws Exception {
        java.nio.file.Path path = Paths.get("data", nombreArchivo);
        if (!Files.exists(path)) {
            throw new java.io.FileNotFoundException("No se encontró el archivo: " + path.toAbsolutePath());
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    public static void shutdown() {
        System.out.println("Apagando servidor principal...");

        running = false;

        if (heartbeatThread != null && heartbeatThread.isAlive()) {
            heartbeatThread.interrupt();
        }

        if (ctx != null && !ctx.isClosed()) {
            ctx.close();  // Esto cierra sockets, workers e hilo poller
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
