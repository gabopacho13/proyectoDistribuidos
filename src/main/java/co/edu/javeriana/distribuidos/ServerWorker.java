package co.edu.javeriana.distribuidos;

import co.edu.javeriana.distribuidos.Services.Aulas;
import org.zeromq.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ServerWorker implements Runnable {

    private ZContext ctx;
    private ZMQ.Socket worker;
    private int threadNbr;

    public ServerWorker(ZContext ctx, int threadNbr) {
        this.ctx = ctx;
        this.worker = ctx.createSocket(SocketType.REQ);
        this.worker.setIdentity(("worker-" + threadNbr).getBytes(ZMQ.CHARSET));
        this.worker.connect("inproc://backend");
        worker.send("READY");
        this.threadNbr = threadNbr;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String address = worker.recvStr();
            String empty = worker.recvStr();
            assert (empty.isEmpty());
            String request = worker.recvStr();
            System.out.println("El trabajador " + threadNbr + " ha recibido una solicitud...");

            String[] partes = request.split(",");
            String responseContent;

            if (partes.length != 5) {
                responseContent = "El mensaje no tiene el formato correcto. Se recibieron " + partes.length + " parámetros.";
                worker.sendMore(address);
                worker.sendMore("");
                worker.send(responseContent);
                continue;
            }

            String facultad = partes[0];
            String programa = partes[1];
            String semestre = partes[2];
            int numSalones = Integer.parseInt(partes[3]);
            int numLaboratorios = Integer.parseInt(partes[4]);

            if (!Recursos.verificarSalones(semestre) || !Recursos.verificarLaboratorios(semestre)) {
                responseContent = "Ha ocurrido un error con la lectura de la base de recursos. Por favor, intente nuevamente.";
                worker.sendMore(address);
                worker.sendMore("");
                worker.send(responseContent);
                continue;
            }

            if (!Recursos.verificarDisponibilidad(semestre, numSalones - numLaboratorios, numLaboratorios)) {
                responseContent = "No hay suficientes recursos disponibles. \n" +
                        "Número de salones disponibles: " + Recursos.getSalonesDisponibles(semestre) + "\n" +
                        "Número de laboratorios disponibles: " + Recursos.getLaboratoriosDisponibles(semestre);
                worker.sendMore(address);
                worker.sendMore("");
                worker.send(responseContent);
                continue;
            }

            List<Salon> salonesReservados = Recursos.reservarSalones(numSalones - numLaboratorios, facultad, programa, semestre);
            List<Aula> laboratoriosReservados = Recursos.reservarLaboratorios(numLaboratorios, facultad, programa, semestre);

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
            } else {
                responseContent = "";
                if (salonesReservados == null) {
                    responseContent += "No hay suficientes salones disponibles. \n";
                }
                if (laboratoriosReservados == null) {
                    responseContent += "No hay suficientes laboratorios disponibles. \n";
                }
                responseContent += "Se ha cancelado la reserva. Intente nuevamente.\n";
                responseContent += "Número de salones disponibles: " + Recursos.getSalonesDisponibles(semestre) + "\n";
                responseContent += "Número de laboratorios disponibles: " + Recursos.getLaboratoriosDisponibles(semestre);
            }

            worker.sendMore(address);
            worker.sendMore("");
            worker.send(responseContent);
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
        if (!laboratoriosAmbulatorios.isEmpty()) {
            responseContent.append("\nLos siguientes salones se han reservado como laboratorios:\n");
            for (Salon laboratorio : laboratoriosAmbulatorios) {
                responseContent.append(laboratorio.getId()).append(" ");
            }
        }
        if (!laboratoriosReservados.isEmpty()) {
            responseContent.append("\nSe le han reservado los laboratorios:\n");
            for (Aula laboratorio : laboratoriosReservados) {
                responseContent.append(laboratorio.getId()).append(" ");
            }
        }
        return responseContent.toString();
    }
}
