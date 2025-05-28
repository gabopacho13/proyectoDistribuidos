package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;
import java.net.InetAddress;

public class Server {

    public Server(){
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
                            String salonesData = leerJson("Salones.json");
                            String laboratoriosData = leerJson("Laboratorios.json");
                            String programasData = leerJson("ProgramasPorFacultad.json");
            
                            String heartbeatJson = String.format(
                                "{\"status\": \"estoy vivo\", \"salones\": %s, \"laboratorios\": %s, \"programas\": %s}",
                                salonesData, laboratoriosData, programasData
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
        ClassLoader classLoader = getClass().getClassLoader();
        java.net.URL recurso = classLoader.getResource(nombreArchivo);
        if (recurso == null) {
            throw new java.io.FileNotFoundException("No se encontró el archivo: " + nombreArchivo);
        }
        java.nio.file.Path path = java.nio.file.Paths.get(recurso.toURI());
        return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }
    

    public static void main(String[] args) throws Exception
    {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor iniciado en " + ip + "... esperando solicitudes de recursos.");
        //Server server = new Server();
        new Server();
    }
}
