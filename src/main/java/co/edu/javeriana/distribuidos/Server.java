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

            //  Connect backend to frontend via a proxy
            ZMQ.proxy(frontend, backend, null);
        }
    }

    public static void main(String[] args) throws Exception
    {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor iniciado en " + ip + "... esperando solicitudes de recursos.");
        Server server = new Server();
    }
}
