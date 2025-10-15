package com.example.gestordetareas.APITrello;

import java.util.ArrayList;
import java.util.List;

public class TrelloCard {
    private String id;
    private String name;
    private String description;
    private String dueDate;
    private String listName;
    private int Precinto;
    private String Caja;
    private String Problema;
    private String Prioridad;
    private String Zona;
    private String Direccion;
    private int idShort;
    private int pos;
    private ArrayList<String> comentarios = new ArrayList<>();
    private String telefono;
    private List<Attachment> attachments = new ArrayList<>(); ;
    private boolean completada;
    public TrelloCard(){}
    public TrelloCard(String id, String name, String description, String dueDate, String listName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dueDate = dueDate;
        this.listName = listName;
    }

    // Getters y Setters
    public int getPos(){
        return pos;
    }
    public void setPos(int p){
        this.pos = p;
    }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDueDate() { return dueDate; }
    public String getListName() { return listName; }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }


    public void setListName(String listName) {
        this.listName = listName;
    }

    public int getPrecinto() {
        return Precinto;
    }
    public void setPrecinto(String precinto) {
        try {
            Precinto = Integer.parseInt(precinto);

        } catch (NumberFormatException e) {
            Precinto = 0;
        }
    }

    public String getCaja() {
        return Caja;
    }
    public void addAttachment(Attachment attachment) {
        this.attachments.add(attachment);
    }
    public void setCaja(String caja) {
        Caja = caja;
    }

    public String getProblema() {
        return Problema;
    }

    public void setProblema(String problema) {
        Problema = problema;
    }

    public String getPrioridad() {
        return Prioridad;
    }

    public void setPrioridad(String prioridad) {
        Prioridad = prioridad;
    }

    public String getZona() {
        return Zona;
    }

    public void setZona(String zona) {
        Zona = zona;
    }

    public void setPrecinto(int precinto) {
        Precinto = precinto;
    }

    public String getDireccion() {
        return Direccion;
    }

    public void setDireccion(String direccion) {
        Direccion = direccion;
    }

    public int getIdShort() {
        return idShort;
    }

    public void setIdShort(int idShort) {
        this.idShort = idShort;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isCompletada() {
        return completada;
    }

    public void setCompletada(boolean completada) {
        this.completada = completada;
    }

    @Override
    public String toString() {
        String datos = "ID: " + id + "\n" +"NOMBRE: "+ name
                +"PRECINTO: "+ Precinto + ". CAJA: " + Caja +"\nPROBLEMA: "+ Problema +".\nZONA: "+ Zona +". DIRECCION:" + Direccion
                + ".\n DESCRIPCION: " +(description.isEmpty() ? "" : "\n" + description);
        return datos;
    }

    public static class Attachment {
        private String id;
        private String name;
        private String url;
        private String mimeType;  // Nuevo campo
        private boolean isUpload; // Nuevo campo

        // Getters y Setters existentes...
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        // Añade estos nuevos métodos
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }

        public boolean isUpload() { return isUpload; }
        public void setUpload(boolean upload) { isUpload = upload; }
    }

    public ArrayList<String> getComentarios() {
        return comentarios;
    }

    public void setComentarios(ArrayList<String> comentarios) {
        this.comentarios = comentarios;
    }
}
