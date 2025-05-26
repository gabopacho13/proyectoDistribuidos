package co.edu.javeriana.distribuidos;

import org.zeromq.*;

public class ClienteProgramas implements Runnable{

    private String programa;
    private String semestre;
    private int numAulas;
    private int numLaboratorios;
    private String ipFacultad;

    public ClienteProgramas(String programa, String semestre, int numAulas, int numLaboratorios, String ipFacultad) {
        this.programa = programa;
        this.semestre = semestre;
        this.numAulas = numAulas;
        this.numLaboratorios = numLaboratorios;
        this.ipFacultad = ipFacultad;
    }

    @Override
    public void run(){
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect("tcp://"+ ipFacultad + ":5571");
            System.out.println("la conexión con la facultad se ha realizado satisfactoriamente. Solicitando salones...");
            String mensaje = programa + "," + semestre + "," + numAulas + "," + numLaboratorios;

            requester.send(mensaje.getBytes(ZMQ.CHARSET), 0);
            ZMsg msg = ZMsg.recvMsg(requester);
            if (msg != null) {
                ZFrame frame = msg.getLast(); // obtiene el último frame
                String respuesta = frame != null ? frame.getString(ZMQ.CHARSET) : null;

                System.out.println("Respuesta del servidor: " + respuesta);
                msg.destroy();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 5){
            System.out.println("modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteProgramas' '-Dexec.args=programa, semestre, numAulas(salones+labs), numLaboratorios, ipFacultad'");
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

        // Crear un nuevo hilo para el cliente
        Thread clienteThread = new Thread(new ClienteProgramas(programa, semestre, numAulas, numLaboratorios, ipFacultad));
        clienteThread.start();

        // Esperar a que el hilo termine
        try {
            clienteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
