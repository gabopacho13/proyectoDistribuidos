package co.edu.javeriana.distribuidos;

import lombok.Getter;

@Getter
public class Salon extends Aula{

    private Boolean esLaboratorio;

    public Salon() {
        super();
    }

    public Salon(int id) {
        super(id);
        this.esLaboratorio = false;
    }

    public void setEsLaboratorio(Boolean esLaboratorio) {
        this.esLaboratorio = esLaboratorio;
    }
}
