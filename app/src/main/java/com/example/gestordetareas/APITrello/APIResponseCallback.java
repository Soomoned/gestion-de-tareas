package com.example.gestordetareas.APITrello;

import com.android.volley.VolleyError;
import java.util.ArrayList;

public interface APIResponseCallback {
    void onSuccess(ArrayList<TrelloCard> cards);
    void onSuccess(); // Sobrecarga para operaciones sin retorno de datos
    void onError(VolleyError error);
    void onEmptyResponse();
}