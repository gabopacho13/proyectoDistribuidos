package co.edu.javeriana.distribuidos.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.javac.Main;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class ProgramasPorFacultad {
    public static boolean buscarFacultad(String facultad) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = ProgramasPorFacultad.class.getClassLoader().getResourceAsStream("ProgramasPorFacultad.json");
            if (inputStream == null) {
                System.out.println("No se encontró el archivo ProgramasPorFacultad.json");
                return false;
            }

            // El JSON es un mapa con nombres de facultad como claves
            Map<String, Object> facultades = mapper.readValue(inputStream, Map.class);
            return facultades.containsKey(facultad);
        } catch (Exception e) {
            System.out.println("Error al leer facultades.json: " + e.getMessage());
            return false;
        }
    }

    public static boolean buscarPrograma(String facultad, String programa) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = ProgramasPorFacultad.class.getClassLoader().getResourceAsStream("ProgramasPorFacultad.json");
            if (inputStream == null) {
                System.out.println("No se encontró el archivo ProgramasPorFacultad.json");
                return false;
            }

            // El JSON es un mapa de facultad -> lista de programas
            Map<String, List<String>> facultades = mapper.readValue(inputStream, new TypeReference<>() {});
            List<String> programas = facultades.get(facultad);
            return programas != null && programas.contains(programa);
        } catch (Exception e) {
            System.out.println("Error al leer facultades.json: " + e.getMessage());
            return false;
        }
    }
}
