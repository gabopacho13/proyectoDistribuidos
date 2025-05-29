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

    private ZMQ.Socket client;  

    public ClienteFacultades(String facultad, String semestre, String servidor) {
        this.facultad = facultad;
        this.semestre = semestre;
        this.servidor = servidor;
        this.ctx = new ZContext();

        this.escuchaProgramas = ctx.createSocket(SocketType.REP);
        this.escuchaProgramas.bind("tcp://*:5571");

        this.client = ctx.createSocket(SocketType.REQ);
        this.client.setIdentity(facultad.getBytes(StandardCharsets.UTF_8));
        this.client.connect("tcp://" + servidor + ":5570");  // conecta al ROUTER del servidor
    }

    @Override
    public void run() {
        System.out.println("Facultad " + facultad + " conectada a " + servidor + ":5570 y escuchando en 5571.");
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("Esperando solicitudes...");
            byte[] data = escuchaProgramas.recv(0);
            if (data == null) {
                escuchaProgramas.send("No se recibió ningún mensaje.".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }
            String request = new String(data, StandardCharsets.UTF_8);
            System.out.println("Mensaje recibido: " + request);

            String[] partes = request.split(",");
            if (partes.length != 4) {
                escuchaProgramas.send("El mensaje no tiene el formato correcto. Debe ser: programa, semestre, numAulas, numLaboratorios".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            String programa = partes[0];
            if (!ProgramasPorFacultad.buscarPrograma(facultad.toLowerCase(), programa.toLowerCase())) {
                escuchaProgramas.send(("El programa " + programa + " no es válido para la facultad " + facultad + ".").getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }
            String semestre = partes[1];
            if (!semestre.equals(this.semestre)) {
                escuchaProgramas.send(("El semestre " + semestre + " no es válido para la facultad " + facultad + ".").getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }
            int numAulas;
            try {
                numAulas = Integer.parseInt(partes[2]);
            } catch (NumberFormatException e) {
                escuchaProgramas.send("El número de aulas debe ser un número entero.".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }
            int numLaboratorios;
            try {
                numLaboratorios = Integer.parseInt(partes[3]);
                if (numLaboratorios > numAulas) {
                    escuchaProgramas.send("El número de laboratorios no puede ser mayor que el número de aulas.".getBytes(StandardCharsets.UTF_8), 0);
                    continue;
                }
            } catch (NumberFormatException e) {
                escuchaProgramas.send("El número de laboratorios debe ser un número entero.".getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            // Enviar mensaje al servidor con formato esperado (incluye facultad al principio)
            String mensaje = facultad + "," + programa + "," + semestre + "," + numAulas + "," + numLaboratorios;
            System.out.println("Enviando solicitud al servidor: " + mensaje);
            boolean exito = false;
            String[] servidores = {this.servidor, "localhost" /*"10.43.101.91"*/};
            int intentos = 0;
            String respuesta = null;

            while (intentos < 2) {
                ZMQ.Socket tempClient = ctx.createSocket(SocketType.REQ);
                tempClient.connect("tcp://" + servidores[intentos] + ":5570");
                tempClient.send(mensaje);

                ZMQ.Poller poller = ctx.createPoller(1);
                poller.register(tempClient, ZMQ.Poller.POLLIN);

                int eventos = poller.poll(5000); // 5 segundos por intento

                if (eventos != -1 && poller.pollin(0)) {
                    ZMsg msg = ZMsg.recvMsg(tempClient);
                    if (msg != null) {
                        ZFrame frame = msg.getLast();
                        respuesta = client.recvStr();
                        msg.destroy();
                        exito = true;
                        this.client.close(); // Cerrar conexión anterior
                        this.client = tempClient; // Asignar el nuevo socket como el activo
                        this.servidor = servidores[intentos]; // Actualizar IP activa
                        break;
                    }
                }

                // Falló este intento
                tempClient.close();
                intentos++;
            }

// Si no hubo éxito en ambos intentos
            if (!exito) {
                this.client.close();
                this.client = ctx.createSocket(SocketType.REQ);
                this.client.connect("tcp://" + servidores[0] + ":5570"); // Volver al original
                String fallo = "Fallo al obtener respuesta del servidor para el programa " + programa + ".";
                System.out.println(fallo);
                escuchaProgramas.send(fallo.getBytes(StandardCharsets.UTF_8), 0);
                continue;
            }

            System.out.println("Respuesta del servidor: " + respuesta + ".\nEnviando respuesta al programa...");
            escuchaProgramas.send(respuesta.getBytes(StandardCharsets.UTF_8), 0);
            System.out.println("Respuesta enviada al programa " + programa);
        }
    }

    public static void main(String[] args) {
        if (args.length < 3){
            System.out.println("modo de uso: mvn exec:java '-Dexec.mainClass=co.edu.javeriana.distribuidos.ClienteFacultades' '-Dexec.args=facultad semestre ipServidor'");
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
