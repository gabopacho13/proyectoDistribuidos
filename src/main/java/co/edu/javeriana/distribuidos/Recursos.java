package co.edu.javeriana.distribuidos;

import java.util.ArrayList;
import java.util.List;

public class Recursos {

    private static final int NUM_SALONES = 380;
    private static final int NUM_LABORATORIOS = 60;
    private static List<Salon> salones;
    private static List<Laboratorio> laboratorios;

    private static Boolean lockSalones = false;
    private static Boolean lockLaboratorios = false;

    public static synchronized void inicializarRecursos() {
        salones = new ArrayList<>();
        laboratorios = new ArrayList<>();
        for (int i = 0; i < NUM_SALONES; i++) {
            salones.add(new Salon(i+1));
        }
        for (int i = 0; i < NUM_LABORATORIOS; i++) {
            laboratorios.add(new Laboratorio(i+1));
        }
    }

    public static synchronized List<Salon> reservarSalones(int numSalones, String facultad) {
        List<Salon> reservados = new ArrayList<>();
        int numSalonesFaltantes = numSalones;
        if (numSalonesFaltantes <= 0) {
            return reservados;
        }
        if (numSalonesFaltantes > NUM_SALONES) {
            return null;
        }
        while (lockSalones) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        lockSalones = true;
        for (Salon salon : salones) {
            if (salon.getDisponible()) {
                reservados.add(salon);
                salon.setDisponible(false);
                salon.setFacultadAsignada(facultad);
                numSalonesFaltantes--;
                if (numSalonesFaltantes == 0) {
                    break;
                }
            }
        }
        if (numSalonesFaltantes > 0) {
            for (Aula aula : reservados) {
                aula.setDisponible(true);
                aula.setFacultadAsignada("");
            }
            reservados.clear();
        }
        lockSalones = false;
        Recursos.class.notifyAll();
        return reservados.size()<numSalones ? null : reservados;
    }

    public static synchronized List<Aula> reservarLaboratorios(int numLaboratorios, String facultad) {
        List<Aula> reservados = new ArrayList<>();
        int numLaboratoriosFaltantes = numLaboratorios;
        if (numLaboratoriosFaltantes <= 0) {
            return reservados;
        }
        if (numLaboratoriosFaltantes > NUM_SALONES+NUM_LABORATORIOS) {
            return null;
        }
        while (lockLaboratorios) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return null;
            }
        }
        lockLaboratorios = true;
        for (Laboratorio laboratorio : laboratorios) {
            if (laboratorio.getDisponible()) {
                reservados.add(laboratorio);
                laboratorio.setDisponible(false);
                laboratorio.setFacultadAsignada(facultad);
                numLaboratoriosFaltantes--;
                if (numLaboratoriosFaltantes == 0) {
                    break;
                }
            }
        }
        if (numLaboratoriosFaltantes > 0) {
            while (lockSalones) {
                try {
                    Recursos.class.wait();
                } catch (InterruptedException e) {
                    for (Aula aula : reservados) {
                        aula.setDisponible(true);
                        aula.setFacultadAsignada("");
                    }
                    reservados.clear();
                    lockLaboratorios = false;
                    Recursos.class.notifyAll();
                    return null;
                }
            }
            lockSalones = true;
            for (Salon salon : salones) {
                if (salon.getDisponible()) {
                    salon.setEsLaboratorio(true);
                    reservados.add(salon);
                    salon.setDisponible(false);
                    salon.setFacultadAsignada(facultad);
                    numLaboratoriosFaltantes--;
                    if (numLaboratoriosFaltantes == 0) {
                        break;
                    }
                }
            }
        }
        if (numLaboratoriosFaltantes > 0) {
            for (Aula aula : reservados) {
                aula.setDisponible(true);
                aula.setFacultadAsignada("");
                if (aula instanceof Salon){
                    ((Salon) aula).setEsLaboratorio(false);
                }
            }
            reservados.clear();
        }
        lockLaboratorios = false;
        lockSalones = false;
        Recursos.class.notifyAll();
        return reservados.size()<numLaboratorios ? null : reservados;
    }

    public static synchronized void liberarSalones(List<Salon> salonesLiberados) {
        while (lockLaboratorios) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error al liberar salones: ", e);
            }
        }
        lockLaboratorios = true;
        for (Salon salon : salonesLiberados) {
            salon.setDisponible(true);
            salon.setFacultadAsignada("");
        }
        lockLaboratorios = false;
        Recursos.class.notifyAll();
    }

    public static synchronized void liberarAulas(List<Aula> aulasLiberadas){
        while (lockLaboratorios || lockSalones){
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error al liberar aulas: ", e);
            }
        }
        lockLaboratorios = true;
        lockSalones = true;
        for (Aula aula : aulasLiberadas) {
            aula.setDisponible(true);
            aula.setFacultadAsignada("");
            if (aula instanceof Salon){
                ((Salon) aula).setEsLaboratorio(false);
            }
        }
        lockLaboratorios = false;
        lockSalones = false;
        Recursos.class.notifyAll();
    }

    public static synchronized int getSalonesDisponibles() {
        int disponibles = 0;
        while (lockSalones) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error al contar salones disponibles: ", e);
            }
        }
        for (Salon salon : salones) {
            if (salon.getDisponible()) {
                disponibles++;
            }
        }
        return disponibles;
    }

    public static synchronized int getLaboratoriosDisponibles() {
        int disponibles = 0;
        while (lockLaboratorios) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException("Error al contar laboratorios disponibles: ", e);
            }
        }
        for (Laboratorio laboratorio : laboratorios) {
            if (laboratorio.getDisponible()) {
                disponibles++;
            }
        }
        return disponibles;
    }
}
