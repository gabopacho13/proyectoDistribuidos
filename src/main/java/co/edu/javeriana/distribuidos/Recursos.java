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

    public static synchronized List<Salon> reservarSalones(int numSalones) {
        List<Salon> reservados = new ArrayList<>();
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
                numSalones--;
                if (numSalones == 0) {
                    break;
                }
            }
        }
        if (numSalones > 0) {
            for (Aula aula : reservados) {
                aula.setDisponible(true);
            }
            reservados.clear();
        }
        lockSalones = false;
        Recursos.class.notifyAll();
        return reservados.size()<numSalones ? null : reservados;
    }

    public static synchronized List<Aula> reservarLaboratorios(int numLaboratorios) {
        List<Aula> reservados = new ArrayList<>();
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
                numLaboratorios--;
                if (numLaboratorios == 0) {
                    break;
                }
            }
        }
        if (numLaboratorios > 0) {
            while (lockSalones) {
                try {
                    Recursos.class.wait();
                } catch (InterruptedException e) {
                    for (Aula aula : reservados) {
                        aula.setDisponible(true);
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
                    numLaboratorios--;
                    if (numLaboratorios == 0) {
                        break;
                    }
                }
            }
        }
        if (numLaboratorios > 0) {
            for (Aula aula : reservados) {
                aula.setDisponible(true);
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
}
