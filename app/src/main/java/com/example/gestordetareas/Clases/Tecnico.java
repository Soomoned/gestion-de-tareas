package com.example.gestordetareas.Clases;

public class Tecnico {
    private int ID;
    private String nombre;

    public Tecnico(){
        this.ID = 1;
        this.nombre = "USUARIO 1";
    }

    public Tecnico(int id, String nom){
        this.ID = id;
        this.nombre = nom;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}
