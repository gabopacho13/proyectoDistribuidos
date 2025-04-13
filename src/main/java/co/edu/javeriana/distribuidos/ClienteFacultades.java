package co.edu.javeriana.distribuidos;

import org.zeromq.*;

public class ClienteFacultades implements Runnable{

    @Override
    public void run() {
        try (ZContext ctx = new ZContext()) {
            // Crear un socket DEALER para el cliente
            ZMQ.Socket client = ctx.createSocket(SocketType.DEALER);
            client.connect("tcp://localhost:5570");

            // Enviar un mensaje al servidor
            String mensaje = "10,5";
            client.send(mensaje.getBytes(ZMQ.CHARSET), 0);

            // Recibir la respuesta del servidor
            ZMsg msg = ZMsg.recvMsg(client);
            if (msg != null) {
                ZFrame frame = msg.getLast(); // obtiene el último frame
                String respuesta = frame != null ? frame.getString(ZMQ.CHARSET) : null;

                System.out.println("Respuesta del servidor: " + respuesta);

                msg.destroy();
            }
        }
    }

    public static void main(String[] args) {
        // Crear un nuevo hilo para el cliente
        Thread clienteThread = new Thread(new ClienteFacultades());
        clienteThread.start();

        // Esperar a que el hilo termine
        try {
            clienteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
