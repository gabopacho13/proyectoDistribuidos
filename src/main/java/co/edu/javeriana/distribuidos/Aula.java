package co.edu.javeriana.distribuidos;

import lombok.Getter;

import java.io.Serializable;

@Getter
public abstract class Aula implements Serializable {

    int id;
    Boolean disponible;
    String facultadAsignada;
    String programaAsignado;

    public Aula(int id) {
        this.id = id;
        this.disponible = true;
        this.facultadAsignada = "";
        this.programaAsignado = "";
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    public void setFacultadAsignada(String facultadAsignada) {
        this.facultadAsignada = facultadAsignada;
    }

    public void setProgramaAsignado(String programaAsignado) {
        this.programaAsignado = programaAsignado;
    }
}
