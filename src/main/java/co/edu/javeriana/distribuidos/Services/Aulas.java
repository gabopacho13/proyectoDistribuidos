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

    public static List<Salon> leerArchivoSalones() {
        InputStream inputStream = Aulas.class.getClassLoader().getResourceAsStream("Salones.json");

        if (inputStream == null) {
            System.out.println("No se pudo encontrar el archivo en recursos.");
            return new ArrayList<>();
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Salon>>(){}.getType();
            List<Salon> salones = gson.fromJson(reader, listType);
            return salones.isEmpty() ? new ArrayList<>() : salones;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static List<Laboratorio> leerArchivoLaboratorios() {
        InputStream inputStream = Aulas.class.getClassLoader().getResourceAsStream("Laboratorios.json");

        if (inputStream == null) {
            System.out.println("No se pudo encontrar el archivo en recursos.");
            return new ArrayList<>();
        }

        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Laboratorio>>(){}.getType();
            List<Laboratorio> laboratorios = gson.fromJson(reader, listType);
            return laboratorios.isEmpty() ? new ArrayList<>() : laboratorios;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
