package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;
import java.net.InetAddress;

public class Server {

    public Server(String semestre){
        try (ZContext ctx = new ZContext()) {
            //  Frontend socket talks to clients over TCP
            Socket frontend = ctx.createSocket(SocketType.ROUTER);
            frontend.bind("tcp://*:5570");

            //  Backend socket talks to workers over inproc
            Socket backend = ctx.createSocket(SocketType.DEALER);
            backend.bind("inproc://backend");

            //  Launch pool of worker threads, precise number is not critical
            for (int threadNbr = 0; threadNbr < 10; threadNbr++)
                new Thread(new ServerWorker(ctx, threadNbr)).start();

            new Thread(() -> {
                try (ZContext ctxHeartbeat = new ZContext()) {
                    Socket publisher = ctxHeartbeat.createSocket(SocketType.PUB);
                    publisher.bind("tcp://*:5572");

                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            String salonesData = leerJson("Salones" + semestre + ".json");
                            String laboratoriosData = leerJson("Laboratorios" + semestre + ".json");

                            String heartbeatJson = String.format(
                                    "{\"status\": \"estoy vivo\", \"semestre\": %s, \"salones\": %s, \"laboratorios\": %s}",
                                    semestre, salonesData, laboratoriosData
                            );

                            publisher.send(heartbeatJson);
                            Thread.sleep(2000);

                        } catch (Exception e) {
                            System.err.println("Error leyendo archivos JSON para heartbeat: " + e.getMessage());
                        }
                    }
                }
            }).start();
                
        
            //  Connect backend to frontend via a proxy
            ZMQ.proxy(frontend, backend, null);
        }
    }


    private String leerJson(String nombreArchivo) throws Exception {
        // Modificado para leer desde la carpeta /data en el sistema de archivos
        java.nio.file.Path path = java.nio.file.Paths.get("data", nombreArchivo);
        if (!java.nio.file.Files.exists(path)) {
            throw new java.io.FileNotFoundException("No se encontró el archivo: " + path.toAbsolutePath());
        }
        return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
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
