package com.example.gestordetareas.APITrello;

public class TrelloBoard {
    private String id;
    private String name;

    public TrelloBoard(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters y setters
    public String getId() { return id; }
    public String getName() { return name; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "TrelloBoard{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}