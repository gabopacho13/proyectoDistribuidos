package co.edu.javeriana.distribuidos;

import co.edu.javeriana.distribuidos.Services.ProgramasPorFacultad;
import org.zeromq.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ClienteProgramas implements Runnable{

    private String programa;
    private String semestre;
    private int numAulas;
    private int numLaboratorios;
    private String ipFacultad;
    private int finPuerto;

    public ClienteProgramas(String programa, String semestre, int numAulas, int numLaboratorios, String ipFacultad, int finPuerto) {
        this.programa = programa;
        this.semestre = semestre;
        this.numAulas = numAulas;
        this.numLaboratorios = numLaboratorios;
        this.ipFacultad = ipFacultad;
        this.finPuerto = finPuerto;
    }

    @Override
    public void run(){
        try (ZContext context = new ZContext()) {
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect("tcp://" + ipFacultad + ":55" + finPuerto);
            System.out.println("la conexión con la facultad se ha realizado satisfactoriamente. Solicitando salones...");

            String mensaje = programa + "," + semestre + "," + numAulas + "," + numLaboratorios;
            requester.send(mensaje.getBytes(StandardCharsets.UTF_8), 0);

            // Crear un poller para esperar respuesta con timeout
            ZMQ.Poller poller = context.createPoller(1);
            poller.register(requester, ZMQ.Poller.POLLIN);

            int eventos = poller.poll(7000);  // Esperar 7 segundos
            if (eventos == -1 || !poller.pollin(0)) {
                System.out.println("No se recibió respuesta de la facultad para el programa " + programa + ".");
            } else {
                ZMsg msg = ZMsg.recvMsg(requester, ZMQ.DONTWAIT);  // Recibir sin bloquear
                if (msg != null) {
                    ZFrame frame = msg.getLast();
                    String respuesta = frame != null ? frame.getString(StandardCharsets.UTF_8) : null;
                    System.out.println("Respuesta del servidor: " + respuesta);
                    msg.destroy();
                } else {
                    System.out.println("La respuesta recibida es nula.");
                }
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 5){
            System.out.println("modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteProgramas' '-Dexec.args=programa(separar palabras por \"_\") semestre numAulas(salones+labs) numLaboratorios ipFacultad'");
            return;
        }
        String programa = args[0];
        String semestre = args[1];
        int numAulas;
        try {
            numAulas = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.out.println("El número de aulas debe ser un número entero.");
            return;
        }
        int numLaboratorios;
        try {
            numLaboratorios = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.out.println("El número de laboratorios debe ser un número entero.");
            return;
        }
        String ipFacultad = args[4];
        if (programa.equals("todos")){
            List<List<String>> programasPorFacultad = ProgramasPorFacultad.getProgramasPorFacultad();
            for (int i = 0; i < programasPorFacultad.size(); i++) {
                List<String> facultadProgramas = programasPorFacultad.get(i);
                for (String programaActual : facultadProgramas) {
                    System.out.println("Solicitando salones para el programa: " + programaActual);
                    // Crear un nuevo hilo para cada programa
                    Thread clienteThread = new Thread(new ClienteProgramas(programaActual, semestre, numAulas, numLaboratorios, ipFacultad, i + 71));
                    clienteThread.start();
                    /*
                    // Esperar a que el hilo termine
                    try {
                        clienteThread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/
                }
            }
        }else {
            // Crear un nuevo hilo para el cliente
            Thread clienteThread = new Thread(new ClienteProgramas(programa, semestre, numAulas, numLaboratorios, ipFacultad, 71));
            clienteThread.start();

            // Esperar a que el hilo termine
            try {
                clienteThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}