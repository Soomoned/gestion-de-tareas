package com.example.gestordetareas.Clases;

import java.util.ArrayList;
import java.util.List;

public class AUTOS {
    private String Patente;
    private List<TAREAS> lsTareas;

    public AUTOS(String patente) {
        this.Patente = patente;
        this.lsTareas = new ArrayList<>(); // Inicializar la lista
    }

    public AUTOS(List<TAREAS> lsTareas, String patente) {
        this.lsTareas = lsTareas != null ? lsTareas : new ArrayList<>();
        Patente = patente;
    }

    public String getPatente() {
        return Patente;
    }

    public void setPatente(String patente) {
        Patente = patente;
    }

    public List<TAREAS> getLsTareas() {
        return lsTareas;
    }

    public void setTarea(TAREAS tarea) {
        if (tarea != null) {
            this.lsTareas.add(tarea);
        }
    }
}
