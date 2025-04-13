package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import java.util.List;

public class ServerWorker implements Runnable{

    private ZContext ctx;
    private Socket worker;

    public ServerWorker(ZContext ctx) {
        this.ctx = ctx;
        this.worker = ctx.createSocket(SocketType.DEALER);
        this.worker.connect("inproc://backend");
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg msg = ZMsg.recvMsg(worker);
            if (msg == null) {
                break; // Interrupted
            }
            ZFrame address = msg.pop();
            ZFrame content = msg.pop();
            assert (content != null);
            msg.destroy();

            String request = content.toString();

            // Procesar el mensaje recibido
            String[] partes = request.split(",");
            int numSalones = Integer.parseInt(partes[0]);
            int numLaboratorios = Integer.parseInt(partes[1]);
            // Reservar salones y laboratorios
            List<Salon> salonesReservados = Recursos.reservarSalones(numSalones-numLaboratorios);
            List<Aula> laboratoriosReservados = Recursos.reservarLaboratorios(numLaboratorios);
            String responseContent = "";
            if (laboratoriosReservados != null && salonesReservados != null) {
                for (Aula laboratorio : laboratoriosReservados) {
                    if (laboratorio instanceof Salon) {
                        salonesReservados.add((Salon) laboratorio);
                        laboratoriosReservados.remove(laboratorio);
                    }
                }
                responseContent = getString(salonesReservados, laboratoriosReservados);
            }
            else {
                responseContent = "No hay suficientes recursos disponibles.";
            }
            ZMsg response = new ZMsg();
            response.add(address); // Dirección del cliente
            response.add(responseContent.getBytes(ZMQ.CHARSET)); // Respuesta personalizada

            // Enviar el mensaje de respuesta
            response.send(worker);

            // Limpiar recursos
            response.destroy();
            address.destroy();
        }
        ctx.destroy();
    }

    private static String getString(List<Salon> salonesReservados, List<Aula> laboratoriosReservados) {
        String responseContent = "";
        if (salonesReservados != null) {
            responseContent = "Se le han reservado los salones:\n";
            for (Salon salon : salonesReservados) {
                responseContent += salon.getId() + " ";
            }
        } else {
            responseContent += "No hay suficientes salones disponibles. ";
        }
        if (laboratoriosReservados != null) {
            responseContent += "\nSe le han reservado los laboratorios:\n";
            for (Aula laboratorio : laboratoriosReservados) {
                responseContent += laboratorio.getId() + " ";
            }
        } else {
            responseContent += "No hay suficientes laboratorios disponibles. ";
        }
        return responseContent;
    }
}
