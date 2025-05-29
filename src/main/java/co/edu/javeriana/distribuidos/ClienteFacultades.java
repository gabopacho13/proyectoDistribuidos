package co.edu.javeriana.distribuidos;

import co.edu.javeriana.distribuidos.Services.ProgramasPorFacultad;
import org.zeromq.*;

import java.nio.charset.StandardCharsets;

public class ClienteFacultades implements Runnable {

    private String facultad;
    private String semestre;
    private String servidor;
    private ZContext ctx;
    private ZMQ.Socket escuchaProgramas;

    public ClienteFacultades(String facultad, String semestre, String servidor) {
        this.facultad = facultad;
        this.semestre = semestre;
        this.servidor = servidor;
        this.ctx = new ZContext();

        // Cambiar puerto a uno no usado por el broker (ej. 5580)
        this.escuchaProgramas = ctx.createSocket(SocketType.REP);
        this.escuchaProgramas.bind("tcp://*:5580");
    }

    @Override
    public void run() {
        System.out.println("Facultad " + facultad + " conectada al servidor en " + servidor + ":5570. Escuchando en puerto 5580...");
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("Esperando solicitudes...");

            byte[] data = escuchaProgramas.recv(0);
            if (data == null) {
                escuchaProgramas.send("No se recibió ningún mensaje.".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            String request = new String(data, StandardCharsets.UTF_8);
            System.out.println("Solicitud recibida: " + request);
            String[] partes = request.split(",");
            if (partes.length != 4) {
                escuchaProgramas.send("Formato inválido. Debe ser: programa,semestre,numAulas,numLaboratorios".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            String programa = partes[0];
            String reqSemestre = partes[1];
            int numAulas, numLaboratorios;

            if (!ProgramasPorFacultad.buscarPrograma(facultad.toLowerCase(), programa.toLowerCase())) {
                escuchaProgramas.send(("El programa " + programa + " no pertenece a la facultad " + facultad + ".").getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            if (!reqSemestre.equals(this.semestre)) {
                escuchaProgramas.send(("El semestre " + reqSemestre + " no corresponde con el configurado (" + this.semestre + ").").getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            try {
                numAulas = Integer.parseInt(partes[2]);
                numLaboratorios = Integer.parseInt(partes[3]);

                if (numLaboratorios > numAulas) {
                    escuchaProgramas.send("Los laboratorios no pueden exceder el número de aulas.".getBytes(StandardCharsets.UTF_8), 0);
                    continue;
                }
            } catch (NumberFormatException e) {
                escuchaProgramas.send("Los valores de aulas y laboratorios deben ser enteros.".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            String mensaje = facultad + "," + programa + "," + semestre + "," + numAulas + "," + numLaboratorios;
            String[] servidores = {this.servidor, "10.43.101.91"};
            boolean exito = false;
            String respuesta = null;

            for (String servidorIntento : servidores) {
                ZMQ.Socket tempClient = ctx.createSocket(SocketType.DEALER);
                tempClient.connect("tcp://" + servidorIntento + ":5570");

                tempClient.send(mensaje.getBytes(StandardCharsets.UTF_8), 0);

                ZMQ.Poller poller = ctx.createPoller(1);
                poller.register(tempClient, ZMQ.Poller.POLLIN);

                int eventos = poller.poll(5000); // Timeout 5 segundos

                if (eventos != -1 && poller.pollin(0)) {
                    ZMsg msg = ZMsg.recvMsg(tempClient);
                    if (msg != null) {
                        ZFrame frame = msg.getLast();
                        respuesta = frame != null ? frame.getString(StandardCharsets.UTF_8) : null;
                        msg.destroy();
                        exito = true;
                        this.servidor = servidorIntento;
                        tempClient.close();
                        break;
                    }
                }

                tempClient.close();
            }

            if (!exito) {
                respuesta = "Error: No se obtuvo respuesta del servidor para el programa " + programa + ".";
            }

            escuchaProgramas.send(respuesta.getBytes(StandardCharsets.UTF_8), 0);
            System.out.println("Respuesta enviada: " + respuesta);
        }

        escuchaProgramas.close();
        ctx.close();
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Uso: mvn exec:java -Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteFacultades -Dexec.args=\"facultad semestre ipServidor\"");
            return;
        }

        String facultad = args[0];
        String semestre = args[1];
        String servidor = args[2];

        if (!ProgramasPorFacultad.buscarFacultad(facultad.toLowerCase())) {
            System.out.println("Facultad no válida: " + facultad);
            return;
        }

        Thread clienteThread = new Thread(new ClienteFacultades(facultad, semestre, servidor));
        clienteThread.start();

        try {
            clienteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
