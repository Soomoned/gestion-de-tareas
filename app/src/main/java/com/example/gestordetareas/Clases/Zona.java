package com.example.gestordetareas.Clases;

public class Zona {

    private String nombre;
    private int id;

    public Zona(){
        this("Zona 9999", 9999);
    }

    public Zona(String nom, int i){
        this.nombre = nom != null ? nom : "Zona sin nombre";
        this.id = i;
    }

    public String getName() {
        return this.nombre;
    }

    public int getId() {
        return this.id;
    }

    public void setName(String nombre) {
        this.nombre = nombre != null ? nombre : this.nombre;
    }

    public void setId(int id) {
        this.id = id;
    }
}
