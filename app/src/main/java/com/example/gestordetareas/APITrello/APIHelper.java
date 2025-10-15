package com.example.gestordetareas.APITrello;
import android.content.Context;
import android.graphics.Bitmap;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;


import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class APIHelper {

    private static final String TAG = "TrelloAPI";
    private TextView resultTextView;
    private ProgressBar progressBar;
    private String API_KEY;
    private String API_TOKEN;
    private FragmentActivity instancia;
    private Map<String, String> customFieldsMap = new HashMap<>();
    private Map<String,  Map<String, String>> customFieldOptionsMap = new HashMap<>();

    public String getApiKey() {
        return API_KEY;
    }

    public String getApiToken() {
        return API_TOKEN;
    }


    public interface SimpleStringCallback {
        void onResult(String result);
    }
    public APIHelper(String key, String token, TextView txtv, ProgressBar pBar, FragmentActivity ins) {
        this.API_KEY = key;
        this.API_TOKEN = token;
        this.progressBar = pBar;
        this.resultTextView = txtv;
        this.instancia = ins;
    }
    public APIHelper(String key, String token, ProgressBar pBar, FragmentActivity ins) {
        this.API_KEY = key;
        this.API_TOKEN = token;
        this.progressBar = pBar;
        this.instancia = ins;
    }
    private void findBoardByName(String boardName) {
        String url = "https://api.trello.com/1/members/me/boards?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response == null || response.length() == 0) {
                            showError("No se encontraron tableros");
                            return;
                        }

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject board = response.getJSONObject(i);
                            String name = board.getString("name");

                            if (boardName.equals(name)) {
                                String boardId = board.getString("id");
                                getBoardLists(boardId);
                                return;
                            }
                        }
                        showError("No se encontró el tablero: " + boardName);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear JSON", e);
                        showError("Error al procesar los tableros");
                    }
                },
                error -> {
                    Log.e(TAG, "Error en la solicitud", error);
                    showError("Error al conectar con Trello: " + error.getMessage());
                });

        // Configurar tiempo de espera
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 segundos de timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(instancia).add(request);
    }

    private void getBoardLists(String boardId) {
        String url = "https://api.trello.com/1/boards/" + boardId + "/lists?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest listsRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response == null || response.length() == 0) {
                            showError("El tablero no contiene listas");
                            return;
                        }

                        StringBuilder sb = new StringBuilder();
                        sb.append("Listas del tablero:\n\n");

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject listJson = response.getJSONObject(i);
                            String id = listJson.getString("id");
                            String name = listJson.getString("name");
                            boolean closed = listJson.getBoolean("closed");

                            if (!closed) {
                                sb.append("• ").append(name).append("\n");
                                sb.append("   ID: ").append(id).append("\n\n");
                            }
                        }

                        instancia.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            //resultTextView.setText(sb.toString());
                        });

                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear listas", e);
                        showError("Error al procesar las listas");
                    }
                },
                error -> {
                    Log.e(TAG, "Error al obtener listas", error);
                    showError("Error al cargar las listas: " + error.getMessage());
                });

        listsRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(instancia).add(listsRequest);
    }

    private void showError(String message) {
        instancia.runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
           // resultTextView.setText(message);
            Log.e(TAG, message);
        });
    }

    private void getTrelloBoards() {
        String url = "https://api.trello.com/1/members/me/boards?key=" + API_KEY + "&token=" + API_TOKEN;

        RequestQueue queue = Volley.newRequestQueue(instancia);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            StringBuilder result = new StringBuilder();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject board = response.getJSONObject(i);
                                String name = board.getString("name");
                                String id = board.getString("id");

                                result.append("Tablero: ").append(name)
                                        .append("\nID: ").append(id)
                                        .append("\n\n");

                                // Guardar en memoria (ejemplo simple)
                                AppMemory.addBoard(new TrelloBoard(id, name));
                            }

                            //resultTextView.setText(result.toString());
                            Log.d(TAG, "Datos obtenidos: " + AppMemory.getBoards().size() + " tableros");

                        } catch (JSONException e) {
                            Log.e(TAG, "Error al parsear JSON: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error en la solicitud: " + error.getMessage());
                        //resultTextView.setText("Error al conectar con Trello");
                    }
                });

        queue.add(jsonArrayRequest);
    }

    public void getCardsFromList(String listId, String listName, ArrayList<TrelloCard> cards) {
        String url = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + API_KEY + "&token=" + API_TOKEN;
        JsonArrayRequest cardsRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject cardJson = response.getJSONObject(i);
                            String cardId = cardJson.getString("id");
                            String name = cardJson.getString("name");
                            String desc = cardJson.optString("desc", "");
                            // Crear la tarjeta con datos básicos
                            TrelloCard card = new TrelloCard(cardId, name, desc, "", listName);

                            // Obtener campos personalizados PARA ESTA TARJETA
                            fetchCustomFieldsForCard(card, cardId, () -> {
                                cards.add(card);
                                if (cards.size() == response.length()) {
                                    // Todas las tarjetas procesadas
                                    AppMemory.setCurrentCards(cards);
                                    //updateCardsUI(cards);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al parsear tarjetas", e);
                    }
                },
                error -> Log.e(TAG, "Error al obtener tarjetas", error)
        );

        Volley.newRequestQueue(instancia).add(cardsRequest);
    }


    // Método auxiliar para formatear la fecha
    private String formatDueDate(String dueDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(dueDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dueDate; // Si falla el parseo, devolver el formato original
        }
    }

    // Metodo auxiliar para obtener el nombre del campo por su ID
    private String getFieldNameById(String fieldId, Map<String, String> customFieldsMap) {
        for (Map.Entry<String, String> entry : customFieldsMap.entrySet()) {
            if (entry.getValue().equals(fieldId)) {
                return entry.getKey();
            }
        }
        return null;
    }


    public void findListByName(String boardName, String listName, APIResponseCallback callback) {
        // Mostrar estado de carga
        progressBar.setVisibility(View.VISIBLE);
        //resultTextView.setText("Buscando lista '" + listName + "' en tablero '" + boardName + "'...");

        // Paso 1: Buscar el tablero por nombre
        String boardsUrl = "https://api.trello.com/1/members/me/boards?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest boardRequest = new JsonArrayRequest(
                Request.Method.GET,
                boardsUrl,
                null,
                response -> {
                    try {
                        // Buscar el tablero
                        String boardId = null;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject board = response.getJSONObject(i);
                            if (boardName.equals(board.getString("name"))) {
                                boardId = board.getString("id");
                                break;
                            }
                        }

                        if (boardId == null) {
                            instancia.runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                callback.onError(new VolleyError("Tablero no encontrado: " + boardName));
                            });
                            return;
                        }

                        // Paso 2: Obtener campos personalizados del tablero primero
                        String finalBoardId = boardId;
                        fetchBoardCustomFields(boardId, () -> {
                            // Paso 3: Obtener las listas del tablero
                            String listsUrl = "https://api.trello.com/1/boards/" + finalBoardId + "/lists?key=" + API_KEY + "&token=" + API_TOKEN;

                            JsonArrayRequest listsRequest = new JsonArrayRequest(
                                    Request.Method.GET,
                                    listsUrl,
                                    null,
                                    listsResponse -> {
                                        try {
                                            String foundListId = null;
                                            String foundListName = null;

                                            for (int i = 0; i < listsResponse.length(); i++) {
                                                JSONObject list = listsResponse.getJSONObject(i);
                                                if (listName.equals(list.getString("name"))) {
                                                    foundListId = list.getString("id");
                                                    foundListName = list.getString("name");
                                                    break;
                                                }
                                            }

                                            if (foundListId != null) {
                                                // Paso 4: Obtener las tarjetas de la lista con campos personalizados
                                                getCardsWithCustomFields(foundListId, foundListName, callback);
                                            } else {
                                                instancia.runOnUiThread(() -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    callback.onEmptyResponse();
                                                });
                                            }
                                        } catch (JSONException e) {
                                            instancia.runOnUiThread(() -> {
                                                progressBar.setVisibility(View.GONE);
                                                callback.onError(new VolleyError("Error procesando listas"));
                                            });
                                        }
                                    },
                                    error -> {
                                        instancia.runOnUiThread(() -> {
                                            progressBar.setVisibility(View.GONE);
                                            callback.onError(error);
                                        });
                                    });

                            listsRequest.setRetryPolicy(new DefaultRetryPolicy(
                                    15000, // 15 segundos timeout
                                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                            Volley.newRequestQueue(instancia).add(listsRequest);
                        });

                    } catch (JSONException e) {
                        instancia.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            callback.onError(new VolleyError("Error procesando tableros"));
                        });
                    }
                },
                error -> {
                    instancia.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        callback.onError(error);
                    });
                });

        boardRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 segundos timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(instancia).add(boardRequest);
    }

    private void getCardsWithCustomFields(String listId, String listName, APIResponseCallback callback) {
        String cardsUrl = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + API_KEY + "&token=" + API_TOKEN;
        ArrayList<TrelloCard> cards = new ArrayList<>();

        JsonArrayRequest cardsRequest = new JsonArrayRequest(
                Request.Method.GET,
                cardsUrl,
                null,
                response -> {
                    try {
                        if (response.length() == 0) {
                            instancia.runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                callback.onEmptyResponse();
                            });
                            return;
                        }

                        AtomicInteger processedCards = new AtomicInteger(0);
                        int totalCards = response.length();

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject cardJson = response.getJSONObject(i);
                            String cardId = cardJson.getString("id");
                            String name = cardJson.getString("name");
                            String desc = cardJson.optString("desc", "");
                            String due = cardJson.optString("start", "");
                            int idShort = cardJson.getInt("idShort");
                            int pos = cardJson.getInt("pos");
                            boolean estado = cardJson.getBoolean("dueComplete");
                            TrelloCard card = new TrelloCard(cardId, name, desc, due, listName);
                            card.setCompletada(estado);

                            card.setIdShort(idShort);
                            card.setPos(pos);

                            // Proceso en dos pasos:
                            // 1. Obtener campos personalizados
                            fetchCustomFieldsForCard(card, cardId, () -> {
                                // 2. Cuando terminan los campos, obtener adjuntos
                                fetchCardAttachments(cardId, new AttachmentCallback() {
                                    @Override
                                    public void onAttachmentsLoaded(List<TrelloCard.Attachment> attachments) {
                                        for (TrelloCard.Attachment attachment : attachments) {
                                            card.addAttachment(attachment);
                                        }

                                        cards.add(card);
                                        if (processedCards.incrementAndGet() == totalCards) {
                                            instancia.runOnUiThread(() -> {
                                                progressBar.setVisibility(View.GONE);
                                                cards.sort(Comparator.comparingInt(TrelloCard::getPos));
                                                callback.onSuccess(cards);
                                            });
                                        }
                                    }

                                    @Override
                                    public void onError(VolleyError error) {
                                        // Continuar sin adjuntos si hay error
                                        cards.add(card);
                                        if (processedCards.incrementAndGet() == totalCards) {
                                            instancia.runOnUiThread(() -> {
                                                progressBar.setVisibility(View.GONE);
                                                cards.sort(Comparator.comparingInt(TrelloCard::getPos));
                                                callback.onSuccess(cards);
                                            });
                                        }
                                    }
                                });
                            });
                            fetchCardComments(cardId, new APIHelper.CommentsCallback() {
                                @Override
                                public void onCommentsLoaded(List<String> comments) {
                                    ArrayList<String> lista = new ArrayList<>();
                                    for (String comment : comments) {
                                        lista.add(comment);
                                    }
                                    if(!lista.isEmpty()) {
                                        card.setComentarios(lista);
                                    }
                                }

                                @Override
                                public void onError(VolleyError error) {
                                    Log.e("Comentarios", "Error: " + error.getMessage());
                                }
                            });
                        }
                    } catch (JSONException e) {
                        instancia.runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            callback.onError(new VolleyError("Error procesando tarjetas"));
                        });
                    }
                },
                error -> {
                    instancia.runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        callback.onError(error);
                    });
                });

        Volley.newRequestQueue(instancia).add(cardsRequest);
    }

    private void fetchBoardCustomFields(String boardId, Runnable onComplete) {
        String url = "https://api.trello.com/1/boards/" + boardId + "/customFields?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        customFieldsMap.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject field = response.getJSONObject(i);
                            String fieldName = field.getString("name");
                            String fieldId = field.getString("id");

                            customFieldsMap.put(fieldName, fieldId);  // nombre → id

                            // Si tiene opciones (como dropdowns), guardamos sus valores
                            if (field.has("options")) {
                                JSONArray options = field.getJSONArray("options");
                                Map<String, String> optionsMap = new HashMap<>();
                                for (int j = 0; j < options.length(); j++) {
                                    JSONObject option = options.getJSONObject(j);
                                    String optionId = option.getString("id");
                                    JSONObject value = option.getJSONObject("value");
                                    String optionText = value.getString("text");
                                    optionsMap.put(optionId, optionText);
                                }
                                customFieldOptionsMap.put(fieldId, optionsMap); // fieldId → opciones
                            }
                        }
                        onComplete.run();
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al procesar campos personalizados", e);
                        onComplete.run(); // Continuar de todos modos
                    }
                },
                error -> {
                    Log.e(TAG, "Error al obtener campos personalizados", error);
                    onComplete.run(); // Continuar de todos modos
                });

        Volley.newRequestQueue(instancia).add(request);
    }
    private void fetchCustomFieldsForCard(TrelloCard card, String cardId, Runnable onComplete) {
        String url = "https://api.trello.com/1/cards/" + cardId + "/customFieldItems?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject fieldItem = response.getJSONObject(i);
                            String fieldId = fieldItem.getString("idCustomField");

                            // Buscar el nombre del campo por ID
                            String fieldName = null;
                            for (Map.Entry<String, String> entry : customFieldsMap.entrySet()) {
                                if (entry.getValue().equals(fieldId)) {
                                    fieldName = entry.getKey();
                                    break;
                                }
                            }


                            if (fieldName != null) {
                                String fieldValue = "";
                                JSONObject valueObj = fieldItem.optJSONObject("value");
                                if (valueObj != null) {
                                    fieldValue = valueObj.optString("text", "");
                                } else {
                                    // Para dropdowns, usamos el idValue
                                    String idOption = fieldItem.optString("idValue", "");
                                    Map<String, String> optionsMap = customFieldOptionsMap.get(fieldId);
                                    if (optionsMap != null) {
                                        fieldValue = optionsMap.getOrDefault(idOption, "");
                                    }
                                }

                                // Asignar a la tarjeta según el nombre del campo
                                switch (fieldName) {
                                    case "Precinto":
                                        card.setPrecinto(fieldValue);
                                        break;
                                    case "Caja":
                                        card.setCaja(fieldValue);
                                        break;
                                    case "Problema":
                                        String problema = fieldValue;
                                        if(problema.isEmpty()){
                                            card.setProblema("No muestra el problema");
                                        } else {
                                            card.setProblema(fieldValue);
                                        }
                                        break;
                                    case "Prioridad":
                                        card.setPrioridad(fieldValue);
                                        break;
                                    case "Zona":
                                        card.setZona(fieldValue);
                                        break;
                                    case "Direccion":
                                        card.setDireccion(fieldValue);
                                        break;
                                    case "Telefono Cliente":
                                        card.setTelefono(fieldValue);
                                        break;
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error al procesar campos de tarjeta", e);
                    } finally {
                        onComplete.run();
                    }
                },
                error -> {
                    Log.e(TAG, "Error al obtener campos de tarjeta", error);
                    onComplete.run();
                });

        Volley.newRequestQueue(instancia).add(request);
    }



    public void fetchCardAttachments(String cardId, AttachmentCallback callback) {
        String url = "https://api.trello.com/1/cards/" + cardId +
                "/attachments?key=" + API_KEY + "&token=" + API_TOKEN +
                "&fields=name,url,mimeType,isUpload";

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<TrelloCard.Attachment> attachments = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject attachmentJson = response.getJSONObject(i);
                            TrelloCard.Attachment attachment = new TrelloCard.Attachment();

                            // Configurar propiedades básicas
                            attachment.setId(attachmentJson.getString("id"));
                            attachment.setName(attachmentJson.optString("name", "Sin nombre"));
                            attachment.setUrl(attachmentJson.getString("url"));

                            // Configurar nuevas propiedades
                            if (attachmentJson.has("mimeType")) {
                                attachment.setMimeType(attachmentJson.getString("mimeType"));
                            } else {
                                attachment.setMimeType("");
                            }

                            if (attachmentJson.has("isUpload")) {
                                attachment.setUpload(attachmentJson.getBoolean("isUpload"));
                            } else {
                                attachment.setUpload(false);
                            }

                            attachments.add(attachment);
                        }
                        callback.onAttachmentsLoaded(attachments);
                    } catch (JSONException e) {
                        callback.onError(new VolleyError("Error parsing attachments: " + e.getMessage()));
                    }
                },
                callback::onError
        );

        Volley.newRequestQueue(instancia).add(request);
    }

    public interface AttachmentCallback {
        void onAttachmentsLoaded(List<TrelloCard.Attachment> attachments);
        void onError(VolleyError error);
    }
    public void getCardsJsonFromList(String boardName, String listName, SimpleStringCallback callback) {
        String boardsUrl = "https://api.trello.com/1/members/me/boards?key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest boardRequest = new JsonArrayRequest(
                Request.Method.GET,
                boardsUrl,
                null,
                response -> {
                    try {
                        String boardId = null;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject board = response.getJSONObject(i);
                            if (boardName.equals(board.getString("name"))) {
                                boardId = board.getString("id");
                                break;
                            }
                        }

                        if (boardId == null) {
                            callback.onResult("No se encontró el tablero: " + boardName);
                            return;
                        }

                        String listsUrl = "https://api.trello.com/1/boards/" + boardId + "/lists?key=" + API_KEY + "&token=" + API_TOKEN;

                        JsonArrayRequest listsRequest = new JsonArrayRequest(
                                Request.Method.GET,
                                listsUrl,
                                null,
                                listsResponse -> {
                                    try {
                                        String listId = null;
                                        for (int j = 0; j < listsResponse.length(); j++) {
                                            JSONObject list = listsResponse.getJSONObject(j);
                                            if (listName.equals(list.getString("name"))) {
                                                listId = list.getString("id");
                                                break;
                                            }
                                        }

                                        if (listId == null) {
                                            callback.onResult("No se encontró la lista: " + listName);
                                            return;
                                        }

                                        String cardsUrl = "https://api.trello.com/1/lists/" + listId + "/cards?key=" + API_KEY + "&token=" + API_TOKEN;

                                        JsonArrayRequest cardsRequest = new JsonArrayRequest(
                                                Request.Method.GET,
                                                cardsUrl,
                                                null,
                                                cardsResponse -> {
                                                    callback.onResult(cardsResponse.toString()+"\n");
                                                },
                                                error -> {
                                                    callback.onResult("Error al obtener tarjetas: " + error.getMessage());
                                                });

                                        Volley.newRequestQueue(instancia).add(cardsRequest);

                                    } catch (JSONException e) {
                                        callback.onResult("Error procesando listas.");
                                    }
                                },
                                error -> callback.onResult("Error al obtener listas.")
                        );

                        Volley.newRequestQueue(instancia).add(listsRequest);

                    } catch (JSONException e) {
                        callback.onResult("Error procesando tableros.");
                    }
                },
                error -> callback.onResult("Error al obtener tableros.")
        );

        Volley.newRequestQueue(instancia).add(boardRequest);
    }

    public void fetchCardComments(String cardId, CommentsCallback callback) {
        String url = "https://api.trello.com/1/cards/" + cardId +
                "/actions?filter=commentCard&key=" + API_KEY + "&token=" + API_TOKEN;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        List<String> comments = new ArrayList<>();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject commentAction = response.getJSONObject(i);
                            JSONObject data = commentAction.getJSONObject("data");
                            String text = data.getString("text");
                            comments.add(text);
                        }
                        callback.onCommentsLoaded(comments);
                    } catch (JSONException e) {
                        callback.onError(new VolleyError("Error parsing comments: " + e.getMessage()));
                    }
                },
                callback::onError
        );

        Volley.newRequestQueue(instancia).add(request);
    }

    // Nuevo callback
    public interface CommentsCallback {
        void onCommentsLoaded(List<String> comments);
        void onError(VolleyError error);
    }

    public void markCardAsComplete(String cardId, APIResponseCallback callback) {
        String url = "https://api.trello.com/1/cards/" + cardId +
                "?key=" + API_KEY + "&token=" + API_TOKEN +
                "&dueComplete=true";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    // Llamar al callback de éxito
                    if (callback != null) {
                        instancia.runOnUiThread(() -> callback.onSuccess());
                    }
                },
                error -> {
                    // Llamar al callback de error
                    if (callback != null) {
                        instancia.runOnUiThread(() -> callback.onError(error));
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(instancia).add(request);
    }

    public void addCommentToCard(String cardId, String commentText, APIResponseCallback callback) {
        String url = "https://api.trello.com/1/cards/" + cardId +
                "/actions/comments?text=" + commentText +
                "&key=" + API_KEY + "&token=" + API_TOKEN;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    instancia.runOnUiThread(() -> {
                        callback.onSuccess(); // Notificar éxito
                    });
                },
                error -> {
                    instancia.runOnUiThread(() -> {
                        callback.onError(error);
                    });
                });

        // Configurar política de reintentos
        request.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15 segundos timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(instancia).add(request);
    }
    /*
    public void subirImagenATrello(String cardId, Bitmap imagen, Context context) {
        String url = "https://api.trello.com/1/cards/" + cardId + "/attachments"
                + "?key=" + API_KEY
                + "&token=" + API_TOKEN;

        // Convertir el Bitmap a archivo temporal
        try {
            File file = File.createTempFile("img", ".jpg", context.getCacheDir());
            FileOutputStream out = new FileOutputStream(file);
            imagen.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.close();

            String boundary = "===" + System.currentTimeMillis() + "===";

            VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(
                    Request.Method.POST, url,
                    response -> Log.d("TRELLO", "Imagen subida correctamente"),
                    error -> Log.e("TRELLO", "Error al subir imagen", error)
            ) {
                @Override
                public String getBodyContentType() {
                    return "multipart/form-data;boundary=" + boundary;
                }

                @Override
                public Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    try {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        params.put("file", new DataPart("foto.jpg", imageBytes, "image/jpeg"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return params;
                }
            };

            Volley.newRequestQueue(context).add(multipartRequest);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

}
