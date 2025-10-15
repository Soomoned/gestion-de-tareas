package com.example.gestordetareas.APITrello;

public class TrelloList {
    private String id;
    private String name;
    private boolean closed;

    public TrelloList(String id, String name, boolean closed) {
        this.id = id;
        this.name = name;
        this.closed = closed;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isClosed() { return closed; }

    @Override
    public String toString() {
        return name + (closed ? " (archivada)" : "");
    }
}