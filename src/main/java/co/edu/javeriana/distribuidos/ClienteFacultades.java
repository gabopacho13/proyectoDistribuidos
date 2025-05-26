package co.edu.javeriana.distribuidos;

import org.zeromq.*;

public class ClienteFacultades implements Runnable{

    private String facultad;
    private String semestre;
    private String servidor;
    private ZContext ctx;
    private ZMQ.Socket escuchaProgramas;

    private ZMQ.Socket client;

    public ClienteFacultades(String facultad, String semestre, String servidor) {
        this.facultad = facultad;
        this.semestre = semestre;
        this.servidor = servidor;
        this.ctx = new ZContext();
        this.escuchaProgramas = ctx.createSocket(SocketType.REP);
        this.escuchaProgramas.bind("tcp://*:5571");
        this.client = ctx.createSocket(SocketType.DEALER);
        this.client.connect("tcp://" + servidor + ":5570");
    }

    @Override
    public void run() {
        System.out.println("Facultad " + facultad + " conectada a " + servidor + ":5570 y escuchando en 5571.");
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println( "Esperando solicitudes...");
            byte[] data = escuchaProgramas.recv(0);
            if (data == null) {
                System.out.println("No se recibió ningún mensaje.");
                continue;
            }
            String request = new String(data, ZMQ.CHARSET);
            System.out.println("Mensaje recibido: " + request);
            String[] partes = request.split(",");
            System.out.println("Solicitud recibida en la facultad " + facultad + ": " + request);
            if (partes.length != 4) {
                System.out.println("El mensaje no tiene el formato correcto.");
                continue;
            }
            String programa = partes[0];
            String semestre = partes[1];
            if (!semestre.equals(this.semestre)) {
                System.out.println("El semestre solicitado no coincide con el semestre de la facultad.");
                continue;
            }
            int numAulas;
            try {
                numAulas = Integer.parseInt(partes[2]);
            } catch (NumberFormatException e) {
                System.out.println("El número de aulas debe ser un número entero.");
                continue;
            }
            int numLaboratorios;
            try {
                numLaboratorios = Integer.parseInt(partes[3]);
            } catch (NumberFormatException e) {
                System.out.println("El número de laboratorios debe ser un número entero.");
                continue;
            }

            // Enviar un mensaje al servidor
            String mensaje = facultad + "," + programa + "," + semestre + "," + numAulas + "," + numLaboratorios;
            client.send(mensaje.getBytes(ZMQ.CHARSET), 0);

            // Recibir la respuesta del servidor
            ZMsg msg = ZMsg.recvMsg(client);
            if (msg != null) {
                ZFrame frame = msg.getLast(); // obtiene el último frame
                String respuesta = frame != null ? frame.getString(ZMQ.CHARSET) : null;

                System.out.println("Respuesta del servidor: " + respuesta + ".\nEnviando respuesta al programa...");
                if (respuesta == null) {
                    respuesta = "No se pudo procesar la solicitud.";
                }
                escuchaProgramas.send(respuesta.getBytes(ZMQ.CHARSET), 0);
                msg.destroy();
                System.out.println("Respuesta enviada al programa " + programa);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 3){
            System.out.println("modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteFacultades' '-Dexec.args=facultad, semestre, ipServidor'");
            return;
        }
        String facultad = args[0];
        String semestre = args[1];
        String servidor = args[2];

        // Crear un nuevo hilo para el cliente
        Thread clienteThread = new Thread(new ClienteFacultades(facultad, semestre, servidor));
        clienteThread.start();

        // Esperar a que el hilo termine
        try {
            clienteThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
