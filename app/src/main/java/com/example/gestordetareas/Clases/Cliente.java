package com.example.gestordetareas.Clases;

public class Cliente {
    private boolean Prioritario = false;
    private String Nombre;
    private int Precinto;
    private String Direccion;
    private String ulrUbicacion;
    private Double lat;
    private Double lon;
    private String telefono;
    private Zona zona;
    private String zonaS;

    public Cliente(){
    }



    public String getZonaS(){
        return zonaS;
    };
    public void setZonaS(String zona){
        zonaS = zona;
    }

    public boolean isPrioritario() {
        return Prioritario;
    }

    public void setPrioritario(boolean prioritario) {
        Prioritario = prioritario;
    }

    public String getNombre() {
        return Nombre != null ? Nombre : "";
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getDireccion() {
        return Direccion;
    }

    public void setDireccion(String direccion) {
        Direccion = direccion;
    }

    public String getUlrUbicacion() {
        return ulrUbicacion;
    }

    public void setUlrUbicacion(String ulrUbicacion) {
        this.ulrUbicacion = ulrUbicacion;
    }

    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona zona) {
        this.zona = zona;
    }

    public int getPrecinto() {
        return Precinto;
    }

    public void setPrecinto(int precinto) {
        Precinto = precinto;
    }

    @Override
    public String toString(){
        String priori = Prioritario ? "ALTA" : "BAJA";
        String zonaNombre = zona != null ? zona.getName() : "Sin zona";

        return String.format(
                "NOMBRE: %s\nPrecinto: %d\nZona: %s\nDireccion: %s\nUbicacion: %s\nPrioridad: %s",
                Nombre != null ? Nombre : "Sin nombre",
                Precinto,
                zonaNombre,
                Direccion != null ? Direccion : "Sin dirección",
                ulrUbicacion != null ? ulrUbicacion : "Sin URL",
                priori
        );
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }
}
