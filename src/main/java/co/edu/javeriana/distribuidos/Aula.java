package co.edu.javeriana.distribuidos;

import lombok.Getter;

import java.io.Serializable;

@Getter
public abstract class Aula implements Serializable {

    int id;
    Boolean disponible;

    public Aula(int id) {
        this.id = id;
        this.disponible = true;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }
}
