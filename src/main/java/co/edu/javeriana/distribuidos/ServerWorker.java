package co.edu.javeriana.distribuidos;

import co.edu.javeriana.distribuidos.Services.Aulas;
import org.zeromq.*;
import java.util.ArrayList;
import java.util.List;

public class ServerWorker implements Runnable {

    private final ZContext ctx;
    private final int threadNbr;
    private final String semestre;

    public ServerWorker(ZContext ctx, String semestre, int threadNbr) {
        this.ctx = ctx;
        this.threadNbr = threadNbr;
        this.semestre = semestre;
    }

    @Override
    public void run() {
        // Socket REQ conectado al backend del broker embebido
        ZMQ.Socket worker = ctx.createSocket(SocketType.REQ);
        worker.setIdentity(("worker-" + threadNbr).getBytes(ZMQ.CHARSET));
        worker.connect("inproc://backend");

        // Enviar READY para indicar que está disponible
        worker.send("READY");

        System.out.println("Worker " + threadNbr + " conectado al backend embebido (inproc://backend)");

        while (!Thread.currentThread().isInterrupted()) {
            String request = worker.recvStr();
            if (request == null) continue;

            System.out.println("Worker " + threadNbr + " ha recibido: " + request);

            String[] partes = request.split(",");
            String responseContent;

            if (partes.length != 5) {
                responseContent = "El mensaje no tiene el formato correcto. Se recibieron " + partes.length + " parámetros.";
                worker.send(responseContent);
                continue;
            }

            String facultad = partes[0];
            String programa = partes[1];
            String semestreReq = partes[2];
            int numSalones, numLaboratorios;

            try {
                numSalones = Integer.parseInt(partes[3]);
                numLaboratorios = Integer.parseInt(partes[4]);
            } catch (NumberFormatException e) {
                worker.send("Número de salones o laboratorios no es válido.");
                continue;
            }

            if (!Recursos.verificarSalones(semestreReq) || !Recursos.verificarLaboratorios(semestreReq)) {
                worker.send("Error en la lectura de recursos. Intente nuevamente.");
                continue;
            }

            if (!Recursos.verificarDisponibilidad(semestreReq, numSalones - numLaboratorios, numLaboratorios)) {
                responseContent = "No hay suficientes recursos disponibles.\n" +
                        "Salones disponibles: " + Recursos.getSalonesDisponibles(semestreReq) + "\n" +
                        "Laboratorios disponibles: " + Recursos.getLaboratoriosDisponibles(semestreReq);
                worker.send(responseContent);
                continue;
            }

            List<Salon> salonesReservados = Recursos.reservarSalones(numSalones - numLaboratorios, facultad, programa, semestreReq);
            List<Aula> laboratoriosReservados = Recursos.reservarLaboratorios(numLaboratorios, facultad, programa, semestreReq);

            if (laboratoriosReservados != null && salonesReservados != null) {
                List<Salon> salonesLaboratorios = new ArrayList<>();
                for (Aula laboratorio : laboratoriosReservados) {
                    if (laboratorio instanceof Salon) {
                        salonesReservados.add((Salon) laboratorio);
                        salonesLaboratorios.add((Salon) laboratorio);
                    }
                }
                laboratoriosReservados.removeAll(salonesLaboratorios);
                responseContent = getString(salonesReservados, laboratoriosReservados);
            } else {
                responseContent = "";
                if (salonesReservados == null) {
                    responseContent += "No hay suficientes salones disponibles.\n";
                }
                if (laboratoriosReservados == null) {
                    responseContent += "No hay suficientes laboratorios disponibles.\n";
                }
                responseContent += "Se canceló la reserva.\n" +
                        "Salones disponibles: " + Recursos.getSalonesDisponibles(semestreReq) + "\n" +
                        "Laboratorios disponibles: " + Recursos.getLaboratoriosDisponibles(semestreReq);
            }

            worker.send(responseContent);
        }

        worker.close();
    }

    private static String getString(List<Salon> salonesReservados, List<Aula> laboratoriosReservados) {
        List<Salon> laboratoriosAmbulatorios = new ArrayList<>();
        StringBuilder response = new StringBuilder("Se han reservado los salones:\n");

        for (Salon salon : salonesReservados) {
            response.append(salon.getId()).append(" ");
            if (salon.getEsLaboratorio()) {
                laboratoriosAmbulatorios.add(salon);
            }
        }

        if (!laboratoriosAmbulatorios.isEmpty()) {
            response.append("\nLos siguientes salones se reservaron como laboratorios:\n");
            for (Salon laboratorio : laboratoriosAmbulatorios) {
                response.append(laboratorio.getId()).append(" ");
            }
        }

        if (!laboratoriosReservados.isEmpty()) {
            response.append("\nSe reservaron los laboratorios:\n");
            for (Aula lab : laboratoriosReservados) {
                response.append(lab.getId()).append(" ");
            }
        }

        return response.toString();
    }
}
