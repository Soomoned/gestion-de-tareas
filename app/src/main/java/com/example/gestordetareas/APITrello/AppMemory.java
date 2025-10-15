package com.example.gestordetareas.APITrello;

import java.util.ArrayList;
import java.util.List;

public class AppMemory {
    // ... (código anterior)
    private static List<TrelloBoard> boards = new ArrayList<>();
    private static List<TrelloList> listas = new ArrayList<>();

    private static List<TrelloCard> cartas = new ArrayList<>();

    public static void addBoard(TrelloBoard board) {
        boards.add(board);
    }

    public static List<TrelloBoard> getBoards() {
        return new ArrayList<>(boards); // Retorna copia para evitar modificaciones externas
    }

    public static void clearMemory() {
        boards.clear();
    }
    public static void setCurrentBoard(List<TrelloBoard> lists) {
        boards = new ArrayList<>(lists);
    }
    public static void setCurrentLists(List<TrelloList> lists) {
        listas = new ArrayList<>(lists);
    }
    public static void setCurrentCards(List<TrelloCard> cards) {
        cartas = new ArrayList<>(cards);
    }

    public static List<TrelloList> getCurrentLists() {
        return new ArrayList<>(listas);
    }

    public static void setBoards(List<TrelloBoard> boards) {
        AppMemory.boards = boards;
    }

    public static List<TrelloList> getListas() {
        return listas;
    }

    public static void setListas(List<TrelloList> listas) {
        AppMemory.listas = listas;
    }

    public static List<TrelloCard> getCartas() {
        return cartas;
    }

    public static void setCartas(List<TrelloCard> cartas) {
        AppMemory.cartas = cartas;
    }
}