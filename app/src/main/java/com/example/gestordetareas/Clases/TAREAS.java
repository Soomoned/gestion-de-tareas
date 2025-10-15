package com.example.gestordetareas.Clases;

import java.util.ArrayList;
import java.util.TimeZone;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TAREAS {
    private int id;
    private String idReal;
    private String problema;
    private Cliente cli;
    private boolean Completada;
    private Tecnico tec;
    private Date inicioReclamo;
    private String solucion;
    private AUTOS movilResolvio;
    private String firma;
    private String caja;
    private int pos;
    private ArrayList<String> comentarios = new ArrayList<>();
    private String rutaFirma;
    private String detalle;
    public String usuario;
    public TAREAS() {
        this.inicioReclamo = new Date(); // Inicializar fecha por defecto
        this.caja = "";
        this.problema = "";
        this.solucion = "";
        this.firma = "";
    }


    public TAREAS(int id, String problema, Cliente cli, boolean completada, Date inicioReclamo, String solucion, AUTOS movilResolvio) {
        this.id = id;
        this.problema = problema;
        this.cli = cli;
        Completada = completada;
        this.inicioReclamo = inicioReclamo;
        this.solucion = solucion;
        this.movilResolvio = movilResolvio;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProblema() {
        return problema;
    }

    public void setProblema(String problema) {
        this.problema = problema;
    }

    public Cliente getCli() {
        return cli;
    }

    public void setCli(Cliente cli) {
        this.cli = cli;
    }

    public boolean isCompletada() {
        return Completada;
    }

    public void setCompletada(boolean completada) {
        Completada = completada;
    }

    public Date getInicioReclamo() {
        return inicioReclamo;
    }

    public void setInicioReclamo(Date inicioReclamo) {
        this.inicioReclamo = inicioReclamo;
    }
    public void setInicioReclamo(String inicioReclamo) throws ParseException {
        try {

            SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            formato.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date fecha = formato.parse(inicioReclamo);

            //Log.d("FECHA", "Convertido a Date: " + fecha.toString());
            this.inicioReclamo = fecha;

        } catch (ParseException e) {
            Log.e("FECHA", "Error al parsear la fecha: " + e.getMessage());
        }

    }

    public String getSolucion() {
        return solucion;
    }

    public void setSolucion(String solucion) {
        this.solucion = solucion;
    }

    public AUTOS getMovilResolvio() {
        return movilResolvio;
    }

    public void setMovilResolvio(AUTOS movilResolvio) {
        this.movilResolvio = movilResolvio;
    }

    public String getCaja() {
        return caja;
    }


    public void setCaja(String caja) {
        this.caja = caja;
    }

    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    public Tecnico getTec() {
        return tec;
    }

    public void setTec(Tecnico tec) {
        this.tec = tec;
    }


    @Override
    public String toString(){
        SimpleDateFormat formatoCorto = new SimpleDateFormat("dd/MM/yy");
        String fecreclamo = inicioReclamo != null ? formatoCorto.format(inicioReclamo) : "Sin fecha";

        if (cli == null) {
            return "Tarea sin cliente asignado";
        }

        String zonaInfo = cli.getZona() != null ? cli.getZona().getName() : "Sin zona";
        String cajaInfo = (caja == null || caja.isEmpty()) ? "" : " Caja: " + caja;

        return cli.getNombre() + " " + cli.getPrecinto() +
                "\nZona " + zonaInfo +
                "\nDirección: " + cli.getDireccion() +
                cajaInfo + "\n" +
                (problema != null ? problema : "Sin problema especificado") +
                "\nFecha inicio: " + fecreclamo;
    }

    public ArrayList<String> getComentarios() {
        return comentarios;
    }

    public String getIdReal() {
        return idReal;
    }

    public void setIdReal(String idReal) {
        this.idReal = idReal;
    }

    public void setComentarios(ArrayList<String> comentarios) {
        this.comentarios = comentarios;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public String getRutaFirma() {
        return rutaFirma;
    }

    public void setRutaFirma(String rutaFirma) {
        this.rutaFirma = rutaFirma;
    }
}
