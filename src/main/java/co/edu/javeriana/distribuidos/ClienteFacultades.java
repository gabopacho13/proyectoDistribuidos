package co.edu.javeriana.distribuidos;

import org.zeromq.*;

public class ClienteFacultades implements Runnable{

    private String facultad;
    private String semestre;
    private int numAulas;
    private int numLaboratorios;
    private String servidor;

    public ClienteFacultades(String facultad, String semestre, int numAulas, int numLaboratorios, String servidor) {
        this.facultad = facultad;
        this.semestre = semestre;
        this.numAulas = numAulas;
        this.numLaboratorios = numLaboratorios;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try (ZContext ctx = new ZContext()) {
            // Crear un socket DEALER para el cliente
            ZMQ.Socket client = ctx.createSocket(SocketType.DEALER);
            client.connect("tcp://"+ servidor + ":5570");
            // Enviar un mensaje al servidor
            String mensaje = facultad + "," + semestre + "," + numAulas + "," + numLaboratorios;
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
        if (args.length < 3){
            System.out.println("modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteFacultades' '-Dexec.args=facultad, semestre, numAulas(salones+labs), numLaboratorios, servidor'");
            return;
        }
        String facultad = args[0];
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
        String servidor = args[4];

        // Crear un nuevo hilo para el cliente
        Thread clienteThread = new Thread(new ClienteFacultades(facultad, semestre, numAulas, numLaboratorios, servidor));
        clienteThread.start();

        // Esperar a que el hilo termine
        try {
            clienteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
