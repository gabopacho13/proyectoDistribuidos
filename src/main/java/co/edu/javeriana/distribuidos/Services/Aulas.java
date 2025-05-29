package co.edu.javeriana.distribuidos.Services;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import co.edu.javeriana.distribuidos.Aula;
import co.edu.javeriana.distribuidos.Salon;
import co.edu.javeriana.distribuidos.Laboratorio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Aulas {

    private static final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private static ReentrantLock getLock(String semestre) {
        return locks.computeIfAbsent(semestre, k -> new ReentrantLock());
    }

    public static Boolean leerArchivoSalones(String semestre) {
        try {
            InputStream inputStream = new FileInputStream("data/Salones" + semestre + ".json");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Boolean leerArchivoLaboratorios(String semestre) {
        try {
            InputStream inputStream = new FileInputStream("data/Laboratorios" + semestre + ".json");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static Boolean EscribirArchivoSalones(String semestre, int numSalones) {
        List<Salon> salones = new ArrayList<>();
        for (int i = 1; i <= numSalones; i++) {
            salones.add(new Salon(i));
        }
        return escribirArchivoSegura(salones, "Salones" + semestre + ".json", semestre);
    }

    public static Boolean EscribirArchivoLaboratorios(String semestre, int numLaboratorios) {
        List<Laboratorio> laboratorios = new ArrayList<>();
        for (int i = 1; i <= numLaboratorios; i++) {
            laboratorios.add(new Laboratorio(i));
        }
        return escribirArchivoSegura(laboratorios, "Laboratorios" + semestre + ".json", semestre);
    }

    private static <T> Boolean escribirArchivoSegura(List<T> lista, String nombreArchivo, String semestre) {
        ReentrantLock lock = getLock(semestre);
        lock.lock();
        try {
            String rutaDirectorio = "data/";
            File directorio = new File(rutaDirectorio);
            if (!directorio.exists()) {
                directorio.mkdirs();
            }
            String rutaCompleta = rutaDirectorio + nombreArchivo;
            try (Writer writer = new FileWriter(rutaCompleta)) {
                Gson gson = new Gson();
                Type type = new TypeToken<List<T>>() {}.getType();
                gson.toJson(lista, type, writer);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public static int salonesDisponibles(String semestre) {
        ReentrantLock lock = getLock(semestre);
        lock.lock();
        try {
            InputStream inputStream = new FileInputStream("data/Salones" + semestre + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            Type type = new TypeToken<List<Salon>>(){}.getType();
            List<Salon> salones = gson.fromJson(reader, type);
            return (int) salones.stream().filter(Salon::getDisponible).count();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public static int laboratoriosDisponibles(String semestre) {
        ReentrantLock lock = getLock(semestre);
        lock.lock();
        try {
            InputStream inputStream = new FileInputStream("data/Laboratorios" + semestre + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            Type type = new TypeToken<List<Laboratorio>>(){}.getType();
            List<Laboratorio> laboratorios = gson.fromJson(reader, type);
            return (int) laboratorios.stream().filter(Laboratorio::getDisponible).count() + salonesDisponibles(semestre);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            lock.unlock();
        }
    }

    public static List<Salon> reservarSalones(int numSalones, String facultad, String programa, String semestre, boolean esLaboratorio) {
        List<Salon> reservados = new ArrayList<>();
        if (numSalones <= 0) return reservados;

        ReentrantLock lock = getLock(semestre);
        lock.lock();
        try {
            if (numSalones > salonesDisponibles(semestre)) {
                return null;
            }
            InputStream inputStream = new FileInputStream("data/Salones" + semestre + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            Type type = new TypeToken<List<Salon>>(){}.getType();
            List<Salon> salones = gson.fromJson(reader, type);

            for (Salon salon : salones) {
                if (salon.getDisponible() && reservados.size() < numSalones) {
                    salon.setDisponible(false);
                    salon.setFacultadAsignada(facultad);
                    salon.setProgramaAsignado(programa);
                    salon.setEsLaboratorio(esLaboratorio);
                    reservados.add(salon);
                }
            }

            escribirArchivoSegura(salones, "Salones" + semestre + ".json", semestre);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            lock.unlock();
        }

        return reservados.size() < numSalones ? null : reservados;
    }

    public static List<Aula> reservarLaboratorios(int numLaboratorios, String facultad, String programa, String semestre) {
        List<Aula> reservados = new ArrayList<>();
        if (numLaboratorios <= 0) return reservados;

        ReentrantLock lock = getLock(semestre);
        lock.lock();
        try {
            InputStream inputStream = new FileInputStream("data/Laboratorios" + semestre + ".json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Gson gson = new Gson();
            Type type = new TypeToken<List<Laboratorio>>(){}.getType();
            List<Laboratorio> laboratorios = gson.fromJson(reader, type);

            for (Aula laboratorio : laboratorios) {
                if (laboratorio.getDisponible() && reservados.size() < numLaboratorios) {
                    laboratorio.setDisponible(false);
                    laboratorio.setFacultadAsignada(facultad);
                    laboratorio.setProgramaAsignado(programa);
                    reservados.add(laboratorio);
                }
            }

            escribirArchivoSegura(laboratorios, "Laboratorios" + semestre + ".json", semestre);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        } finally {
            lock.unlock();
        }

        return reservados;
    }
}
