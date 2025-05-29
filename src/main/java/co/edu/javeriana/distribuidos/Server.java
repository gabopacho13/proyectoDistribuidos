package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Queue;

public class Server {

    public Server(){
        try (ZContext ctx = new ZContext()) {
            Socket frontend = ctx.createSocket(SocketType.ROUTER);
            frontend.bind("tcp://*:5570");

            Socket backend = ctx.createSocket(SocketType.ROUTER);
            backend.bind("inproc://backend");

            for (int threadNbr = 0; threadNbr < 10; threadNbr++)
                new Thread(new ServerWorker(ctx, threadNbr)).start();

            //  Queue of available workers
            Queue<String> workerQueue = new LinkedList<String>();

            while (!Thread.currentThread().isInterrupted()) {
                //  Initialize poll set
                ZMQ.Poller items = ctx.createPoller(2);

                items.register(backend, ZMQ.Poller.POLLIN);

                if (workerQueue.size() > 0)
                    items.register(frontend, ZMQ.Poller.POLLIN);

                if (items.poll() < 0)
                    break; //  Interrupted

                //  Handle worker activity on backend
                if (items.pollin(0)) {

                    //  Queue worker address for LRU routing
                    workerQueue.add(backend.recvStr());

                    //  Second frame is empty
                    String empty = backend.recvStr();
                    assert (empty.length() == 0);

                    //  Third frame is READY or else a client reply address
                    String clientAddr = backend.recvStr();
                    //  If client reply, send rest back to frontend
                    if (!clientAddr.equals("READY")) {

                        empty = backend.recvStr();
                        assert (empty.length() == 0);

                        String reply = backend.recvStr();
                        System.out.println("Enviando respuesta al cliente: " + clientAddr);
                        frontend.sendMore(clientAddr);
                        frontend.sendMore("");
                        frontend.send(reply);
                    }
                }

                if (items.pollin(1)) {
                    //  Now get next client request, route to LRU worker
                    //  Client request is [address][empty][request]
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
        }
    }

    public static void main(String[] args) throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("Servidor iniciado en " + ip + "... esperando solicitudes de recursos.");
        new Server();
    }
}
