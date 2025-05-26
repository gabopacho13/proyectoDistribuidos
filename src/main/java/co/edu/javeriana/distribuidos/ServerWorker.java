package co.edu.javeriana.distribuidos;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.List;

public class ServerWorker implements Runnable{

    private ZContext ctx;
    private Socket worker;
    private int threadNbr;

    public ServerWorker(ZContext ctx, int threadNbr) {
        this.ctx = ctx;
        this.worker = ctx.createSocket(SocketType.DEALER);
        this.worker.connect("inproc://backend");
        this.threadNbr = threadNbr;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg msg = ZMsg.recvMsg(worker);
            System.out.println("El trabajador " + threadNbr + " ha recibido una solicitud...");
            if (msg == null) {
                continue; // Interrupted
            }
            ZFrame address = msg.pop();
            ZFrame content = msg.pop();
            assert (content != null);
            msg.destroy();

            String request = content.toString();

            // Procesar el mensaje recibido
            String[] partes = request.split(",");
            if (partes.length != 5) {
                System.out.println("El mensaje no tiene el formato correcto.");
                continue;
            }
            String facultad = partes[0];
            String programa = partes[1];
            String semestre = partes[2];
            int numSalones = Integer.parseInt(partes[3]);
            int numLaboratorios = Integer.parseInt(partes[4]);
            // Reservar salones y laboratorios
            List<Salon> salonesReservados = Recursos.reservarSalones(numSalones-numLaboratorios, facultad, programa);
            List<Aula> laboratoriosReservados = Recursos.reservarLaboratorios(numLaboratorios, facultad, programa);
            String responseContent = "";
            if (laboratoriosReservados != null && salonesReservados != null) {
                List<Salon> salonesLaboratorios = new ArrayList<>();
                for (Aula laboratorio : laboratoriosReservados) {
                    if (laboratorio instanceof Salon) {
                        salonesReservados.add((Salon) laboratorio);
                        salonesLaboratorios.add((Salon) laboratorio);
                    }
                }
                for (Salon salon : salonesLaboratorios) {
                    laboratoriosReservados.remove(salon);
                }
                responseContent = getString(salonesReservados, laboratoriosReservados);
            }
            else {
                if (salonesReservados == null){
                    responseContent = "No hay suficientes salones disponibles. \n";
                }
                if (laboratoriosReservados == null){
                    responseContent += "No hay suficientes laboratorios disponibles. \n";
                }
                if (salonesReservados != null){
                    Recursos.liberarSalones(salonesReservados);
                }
                if (laboratoriosReservados != null){
                    Recursos.liberarAulas(laboratoriosReservados);
                }
                responseContent += "Se ha cancelado la reserva. Intente nuevamente.\n";
                responseContent += "Número de salones disponibles: " + Recursos.getSalonesDisponibles() + " (Recuerde que los salones podrán utilizarse como laboratorios ambulatorios de ser requerido)\n";
                responseContent += "Número de laboratorios disponibles: " + Recursos.getLaboratoriosDisponibles();
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
        List<Salon> laboratoriosAmbulatorios = new ArrayList<>();
        StringBuilder responseContent = new StringBuilder("Se le han reservado los salones:\n");
        for (Salon salon : salonesReservados) {
            responseContent.append(salon.getId()).append(" ");
            if (salon.getEsLaboratorio()) {
                laboratoriosAmbulatorios.add(salon);
            }
        }
        if (!laboratoriosAmbulatorios.isEmpty()){
            responseContent.append("\nLos siguientes salones se han reservado como laboratorios:\n");
        }
        for (Salon laboratorio : laboratoriosAmbulatorios) {
            responseContent.append(laboratorio.getId()).append(" ");
        }
        if (!laboratoriosReservados.isEmpty()){
            responseContent.append("\nSe le han reservado los laboratorios:\n");
            for (Aula laboratorio : laboratoriosReservados) {
                responseContent.append(laboratorio.getId()).append(" ");
            }
        }
        return responseContent.toString();
    }
}
