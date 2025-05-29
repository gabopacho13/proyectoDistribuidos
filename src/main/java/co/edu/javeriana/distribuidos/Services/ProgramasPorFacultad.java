package co.edu.javeriana.distribuidos.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProgramasPorFacultad {

    private static final Map<String, List<String>> datos = cargarDatos();

    private static Map<String, List<String>> cargarDatos() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = ProgramasPorFacultad.class.getClassLoader().getResourceAsStream("ProgramasPorFacultad.json");
            if (inputStream == null) {
                System.out.println("No se encontró el archivo ProgramasPorFacultad.json");
                return Collections.emptyMap();
            }

            // El JSON es un mapa de facultad -> lista de programas
            return mapper.readValue(inputStream, new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception e) {
            System.out.println("Error al leer ProgramasPorFacultad.json: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    public static boolean buscarFacultad(String facultad) {
        return datos.containsKey(facultad);
    }

    public static boolean buscarPrograma(String facultad, String programa) {
        List<String> programas = datos.get(facultad);
        return programas != null && programas.contains(programa);
    }

    public static List<String> getFacultades() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = ProgramasPorFacultad.class.getClassLoader().getResourceAsStream("ProgramasPorFacultad.json");
            if (inputStream == null) {
                System.out.println("No se encontró el archivo ProgramasPorFacultad.json");
                return List.of();
            }

            // El JSON es un mapa con nombres de facultad como claves
            Map<String, Object> facultades = mapper.readValue(inputStream, Map.class);
            return List.copyOf(facultades.keySet());
        } catch (Exception e) {
            System.out.println("Error al leer facultades.json: " + e.getMessage());
            return List.of();
        }
    }

    public static List<List<String>> getProgramasPorFacultad() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = ProgramasPorFacultad.class.getClassLoader().getResourceAsStream("ProgramasPorFacultad.json");
            if (inputStream == null) {
                System.out.println("No se encontró el archivo ProgramasPorFacultad.json");
                return List.of();
            }

            // El JSON es un mapa de facultad -> lista de programas
            Map<String, List<String>> facultades = mapper.readValue(inputStream, new TypeReference<>() {});
            return List.copyOf(facultades.values());
        } catch (Exception e) {
            System.out.println("Error al leer facultades.json: " + e.getMessage());
            return List.of();
        }
    }
}
