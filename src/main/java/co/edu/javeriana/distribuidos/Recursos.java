package co.edu.javeriana.distribuidos;

import co.edu.javeriana.distribuidos.Services.Aulas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Recursos {

    private static final int NUM_SALONES = 380;
    private static final int NUM_LABORATORIOS = 60;
    private static Boolean lockSalones = false;
    private static Boolean lockLaboratorios = false;

    public static synchronized boolean verificarSalones(String semestre) {
        while (lockSalones) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        if (Aulas.leerArchivoSalones(semestre)) {
            lockSalones = false;
            Recursos.class.notifyAll();
            return true;
        }
        lockSalones = true;
        boolean verificacido = Aulas.EscribirArchivoSalones(semestre, NUM_SALONES);
        lockSalones = false;
        Recursos.class.notifyAll();
        return verificacido;
    }

    public static synchronized boolean verificarLaboratorios(String semestre) {
        if (Aulas.leerArchivoLaboratorios(semestre)) {
            return true;
        } else {
            while (lockLaboratorios) {
                try {
                    Recursos.class.wait();
                } catch (InterruptedException e) {
                    return false;
                }
            }
            lockLaboratorios = true;
            boolean verificacido = Aulas.EscribirArchivoLaboratorios(semestre, NUM_LABORATORIOS);
            lockLaboratorios = false;
            Recursos.class.notifyAll();
            return verificacido;
        }
    }

    public static synchronized boolean verificarDisponibilidad(String semestre, int numSalones, int numLaboratorios) {
        while (lockSalones) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        int verificadoSalones = Aulas.salonesDisponibles(semestre);
        while (lockSalones || lockLaboratorios) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        int verificadoLaboratorios = Aulas.laboratoriosDisponibles(semestre);
        return verificadoSalones >= numSalones && verificadoLaboratorios-verificadoSalones >= numLaboratorios;
    }

    public static synchronized List<Salon> reservarSalones(int numSalones, String facultad, String programa, String semestre) {
        List<Salon> reservados = new ArrayList<>();
        if (numSalones <= 0) {
            return reservados;
        }
        if (numSalones > NUM_SALONES) {
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
        reservados = Aulas.reservarSalones(numSalones, facultad, programa, semestre, false);
        lockSalones = false;
        Recursos.class.notifyAll();
        if (reservados == null || reservados.size() < numSalones) {
            return null;
        }
        return reservados;
    }

    public static synchronized List<Aula> reservarLaboratorios(int numLaboratorios, String facultad, String programa, String semestre) {
        List<Aula> reservados = new ArrayList<>();
        if (numLaboratorios <= 0) {
            return reservados;
        }
        if (numLaboratorios > NUM_SALONES + NUM_LABORATORIOS) {
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
        reservados = Aulas.reservarLaboratorios(numLaboratorios, facultad, programa, semestre);
        if (reservados.size() < numLaboratorios) {
            while (lockSalones) {
                try {
                    Recursos.class.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            lockSalones = true;
            reservados.addAll(Objects.requireNonNull(Aulas.reservarSalones(numLaboratorios - reservados.size(), facultad, programa, semestre, true)));
        }
        lockLaboratorios = false;
        lockSalones = false;
        Recursos.class.notifyAll();
        return reservados.size() < numLaboratorios ? null : reservados;
    }

    public synchronized static int getSalonesDisponibles(String semestre) {
        while (lockSalones) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return 0;
            }
        }
        return Aulas.salonesDisponibles(semestre);
    }

    public synchronized static int getLaboratoriosDisponibles(String semestre) {
        while (lockLaboratorios) {
            try {
                Recursos.class.wait();
            } catch (InterruptedException e) {
                return 0;
            }
        }
        return Aulas.laboratoriosDisponibles(semestre);
    }
}
