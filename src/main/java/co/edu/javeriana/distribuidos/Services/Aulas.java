package co.edu.javeriana.distribuidos.Services;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import co.edu.javeriana.distribuidos.Aula;
import co.edu.javeriana.distribuidos.Salon;
import co.edu.javeriana.distribuidos.Laboratorio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Aulas {

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
        return escribirArchivo(salones, "Salones" + semestre + ".json");
    }

    public static Boolean EscribirArchivoLaboratorios(String semestre, int numLaboratorios) {
        List<Laboratorio> laboratorios = new ArrayList<>();
        for (int i = 1; i <= numLaboratorios; i++) {
            laboratorios.add(new Laboratorio(i));
        }
        return escribirArchivo(laboratorios, "Laboratorios" + semestre + ".json");
    }

    private static <T> Boolean escribirArchivo(List<T> lista, String nombreArchivo) {
        String rutaDirectorio = "data/";
        File directorio = new File(rutaDirectorio);
        if (!directorio.exists()) {
            directorio.mkdirs(); // Crear el directorio si no existe
        }
        String rutaCompleta = rutaDirectorio + nombreArchivo;
        try (Writer writer = new FileWriter(rutaCompleta)) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<T>>(){}.getType();
            gson.toJson(lista, type, writer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int salonesDisponibles(String semestre){
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
        }
    }

    public static int laboratoriosDisponibles(String semestre){
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
        }
    }

    public static List<Salon> reservarSalones(int numSalones, String facultad, String programa, String semestre, boolean esLaboratorio) {
        List<Salon> reservados = new ArrayList<>();
        if (numSalones <= 0 || numSalones > salonesDisponibles(semestre)) {
            return null; // No se pueden reservar salones
        }
        try {
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
            escribirArchivo(salones, "Salones" + semestre + ".json");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return reservados.size() < numSalones ? null : reservados;
    }

    public static List<Aula> reservarLaboratorios(int numLaboratorios, String facultad, String programa, String semestre) {
        List<Aula> reservados = new ArrayList<>();
        if (numLaboratorios <= 0) {
            return new ArrayList<>(); // No se pueden reservar laboratorios
        }
        try {
            InputStream inputStream = new FileInputStream("data/Laboratorios" + semestre + ".json");
            if (inputStream == null) {
                return new ArrayList<>(); // No hay laboratorios disponibles
            }
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
            escribirArchivo(laboratorios, "Laboratorios" + semestre + ".json");
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        return reservados;
    }
}
