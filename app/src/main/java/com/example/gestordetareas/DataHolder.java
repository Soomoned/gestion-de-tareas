package com.example.gestordetareas;

import android.content.Intent;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.util.Base64;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.example.gestordetareas.APIBD.MySQLConnection;
import com.example.gestordetareas.APITrello.APIHelper;
import com.example.gestordetareas.APITrello.APIResponseCallback;
import com.example.gestordetareas.APITrello.TrelloCard;
import com.example.gestordetareas.Clases.AUTOS;
import com.example.gestordetareas.Clases.Cliente;
import com.example.gestordetareas.Clases.TAREAS;
import com.example.gestordetareas.Clases.Zona;
import com.example.gestordetareas.funcionalidades.VolleyMultipartRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class DataHolder {
    private static DataHolder instance;
    private ArrayList<Cliente> cli = new ArrayList<>();
    private ArrayList<Zona> zonas = new ArrayList<>();
    private ArrayList<AUTOS> autos = new ArrayList<>();
    private ArrayList<TAREAS> tareas = new ArrayList<>();
    private TAREAS tarea = new TAREAS();
    private boolean cargado = false;
    private APIHelper apiH;
    private String usuario;
    private DataHolder() {}



    public interface DataLoadCallback {
        void onDataLoaded();      // Se llama cuando los datos se cargan correctamente
        void onDataLoadFailed();  // Se llama cuando falla la carga de datos
    }
    public interface TareasCallback {
        void onExito(ArrayList<TAREAS> tareas);
        void onFallo(String mensajeError);
    }
    public static synchronized DataHolder getInstance() {
        if (instance == null) {
            instance = new DataHolder();
        }
        return instance;
    }

    // Getters y Setters
    public APIHelper getApiH(){return apiH;}
    public TAREAS getTarea() {
        return tarea;
    }

    public void setTarea(TAREAS tarea) {
        this.tarea = tarea;
    }

    public ArrayList<Cliente> getClientes() {
        return cli;
    }

    public void setClientes(ArrayList<Cliente> clientes) {
        this.cli = clientes;
    }

    public ArrayList<Zona> getZonas() {
        return zonas;
    }

    public void setZonas(ArrayList<Zona> zonas) {
        this.zonas = zonas;
    }

    public ArrayList<TAREAS> getTareasPendientes(){
        ArrayList<TAREAS> lstTarea = new ArrayList<>();
        for (TAREAS t : tareas) {
            if(!t.isCompletada()){
                lstTarea.add(t);
            }
        }
        return lstTarea;
    }

    public ArrayList<AUTOS> getAutos() {
        return autos;
    }

    public void setAutos(ArrayList<AUTOS> autos) {
        this.autos = autos;
    }

    public void setUsuario(String user){
        this.usuario = user;
    }
    public String getUsuario(){
        return usuario;
    }
    public ArrayList<Cliente> getClientesByZona(String zonaName) {
        ArrayList<Cliente> filtered = new ArrayList<>();
        for (Cliente cliente : cli) {
            if (cliente.getZona().getName().equalsIgnoreCase(zonaName)) {
                filtered.add(cliente);
            }
        }
        return filtered;
    }

    public ArrayList<TAREAS> getTareas() {
        return tareas;
    }

    //CARGA DE DATOS
    public void cargoDatosTrello(APIHelper api, String aut, FragmentActivity th, DataLoadCallback callback) {
        // Limpiar e inicializar listas
        tareas = new ArrayList<>();
        cli = new ArrayList<>();
        zonas = new ArrayList<>();
        apiH = api;
        apiH.findListByName("SOPORTE CALLE", aut, new APIResponseCallback() {
            @Override
            public void onSuccess(ArrayList<TrelloCard> cards) {
                Log.d("DataHolder", "Tarjetas recibidas: " + (cards != null ? cards.size() : 0));

                if (cards == null || cards.isEmpty()) {
                    Log.w("DataHolder", "Lista de tarjetas vacía o nula");
                    th.runOnUiThread(() -> {
                        Toast.makeText(th, "No se encontraron tarjetas", Toast.LENGTH_SHORT).show();
                        callback.onDataLoadFailed();
                    });
                    return;
                }

                // Procesar las tarjetas
                processCards(cards, th);

                th.runOnUiThread(() -> {
                    callback.onDataLoaded();
                });
            }
            @Override
            public void onSuccess(){

            }
            @Override
            public void onError(VolleyError error) {
                Log.e("DataHolder", "Error en la API: " + error.getMessage());
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "Error al conectar con la API" + error.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onDataLoadFailed();
                });
            }

            @Override
            public void onEmptyResponse() {
                Log.w("DataHolder", "Respuesta vacía de la API");
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "No se encontraron datos", Toast.LENGTH_SHORT).show();
                    callback.onDataLoadFailed();
                });
            }
        });
    }


    private void processCards(ArrayList<TrelloCard> cards, FragmentActivity th) {
        if (cards == null || cards.isEmpty()) {
            Log.w("DataHolder", "Lista de tarjetas vacía o nula");
            return;
        }

        for (TrelloCard card : cards) {
            try {
                // Validar tarjeta básica
                if (card == null || card.getName() == null) {
                    Log.w("DataHolder", "Tarjeta inválida, se omite");
                    continue;
                }

                // Procesar cliente
                Cliente cliente = new Cliente();
                cliente.setNombre(card.getName() != null ? card.getName() : "Sin nombre");
                cliente.setTelefono(card.getTelefono() != null ? card.getTelefono() : "SN");
                // Manejar precinto de forma segura
                try {
                    if (card.getPrecinto() > 0) { // Si es int
                        cliente.setPrecinto(card.getPrecinto());
                    } else if (card.getPrecinto() != 0) {
                        cliente.setPrecinto(Integer.parseInt(String.valueOf(card.getPrecinto())));
                    }
                } catch (Exception e) {
                    Log.e("DataHolder", "Error con precinto: " + e.getMessage());
                    cliente.setPrecinto(0); // Valor por defecto
                }

                // Procesar zona
                String zonaNombre = card.getZona() != null ? card.getZona() : "Sin zona";
                boolean zonaExiste = false;

                for (Zona z : zonas) {
                    if (z != null && z.getName() != null && z.getName().equals(zonaNombre)) {
                        cliente.setZona(z);
                        zonaExiste = true;
                        break;
                    }
                }

                if (!zonaExiste) {
                    Zona nuevaZona = new Zona(zonaNombre, zonas.size() + 1);
                    zonas.add(nuevaZona);
                    cliente.setZona(nuevaZona);
                }

                // Dirección (manejar nulo)
                cliente.setDireccion(card.getDireccion() != null ? card.getDireccion() : "Sin dirección");

                //ubicacion
                String ubicacion ="Sin ubicacion";

                if(card.getAttachments() != null) {
                    List<TrelloCard.Attachment> attachments = card.getAttachments();
                    for (TrelloCard.Attachment attachment : attachments) {
                        if (attachment.getUrl() != null) {
                            // Es un enlace
                            ubicacion = attachment.getUrl();
                        }
                    }
                }
                cliente.setUlrUbicacion(ubicacion);
                // Agregar cliente a la lista
                cli.add(cliente);

                // Procesar tarea
                TAREAS tarea = new TAREAS();
                try {
                    // Manejar ID (podría ser alfanumérico)
                    tarea.setId(card.getIdShort() > 0 ? card.getIdShort() : tareas.size() + 1);
                } catch (Exception e) {
                    tarea.setId(tareas.size() + 1);
                }

                tarea.setIdReal(card.getId());
                tarea.setInicioReclamo(card.getDueDate());
                tarea.setCli(cliente);
                tarea.setProblema(card.getProblema() != null ? card.getProblema() : "Sin problema");
                tarea.setCaja(card.getCaja() != null ? card.getCaja() : "Sin caja");
                tarea.setCompletada(false);
                tarea.setPos(card.getPos());
                if(card.isCompletada()){
                    tarea.setCompletada(card.isCompletada());
                }
                if(!card.getComentarios().isEmpty()){
                    tarea.setComentarios(card.getComentarios());
                }
                tareas.add(tarea);


                cargado = true;

            } catch (Exception e) {
                Log.e("DataHolder", "Error grave procesando tarjeta: " + e.getMessage(), e);
                // Continuar con la siguiente tarjeta en lugar de crashear
            }
        }
        tareas.sort(Comparator.comparingInt(TAREAS::getPos));
        cargado = true;
        th.runOnUiThread(() -> {
            try {
            } catch (Exception e) {
                Log.e("DataHolder", "Error mostrando Toast: " + e.getMessage());
            }
        });

    }

    public void completoTarea(String cardId, FragmentActivity th) {
        apiH.markCardAsComplete(cardId, new APIResponseCallback() {
            @Override
            public void onSuccess(ArrayList<TrelloCard> cards) {
                // No usado en este caso
            }

            @Override
            public void onSuccess() {
                // Actualizar el estado local de la tarea
                tarea.setCompletada(true);

                // Actualizar la UI en el hilo principal
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "Tarea Finalizada", Toast.LENGTH_SHORT).show();
                    finalizarActividad(th);
                });
            }

            @Override
            public void onError(VolleyError error) {
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "Error al completar la tarea: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onEmptyResponse() {
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "No se recibió respuesta del servidor", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void finalizarActividad(FragmentActivity activity) {
        Intent intent = new Intent(activity, GENERAL.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    public Boolean estaCargado(){
        return cargado;
    }

    public void actualizarTareas(String aut, FragmentActivity th, DataLoadCallback callback) {
        if (apiH == null) {
            Log.e("DataHolder", "APIHelper no inicializado");
            callback.onDataLoadFailed();
            return;
        }

        apiH.findListByName("SOPORTE CALLE", aut, new APIResponseCallback() {
            @Override
            public void onSuccess(ArrayList<TrelloCard> newCards) {
                Log.d("DataHolder", "Tarjetas recibidas para actualización: " + (newCards != null ? newCards.size() : 0));

                if (newCards == null) {
                    Log.w("DataHolder", "No hay tarjetas nuevas para actualizar");
                    th.runOnUiThread(() -> {
                        Toast.makeText(th, "Error al obtener datos", Toast.LENGTH_SHORT).show();
                        callback.onDataLoadFailed();
                    });
                    return;
                }

                // Procesar las tarjetas nuevas y verificar eliminaciones
                processUpdatedCards(newCards, th, callback);

            }

            @Override
            public void onSuccess() {
                // No usado en este caso
            }

            @Override
            public void onError(VolleyError error) {
                Log.e("DataHolder", "Error en la API al actualizar: " + error.getMessage());
                th.runOnUiThread(() -> {
                    Toast.makeText(th, "Error al actualizar tareas", Toast.LENGTH_SHORT).show();
                    callback.onDataLoadFailed();
                });
            }

            @Override
            public void onEmptyResponse() {
                // Si la respuesta está vacía, verificar si todas las tareas fueron eliminadas
                processUpdatedCards(new ArrayList<>(), th, callback);
            }
        });
    }

    private void processUpdatedCards(ArrayList<TrelloCard> newCards, FragmentActivity th, DataLoadCallback callback) {
        int nuevasAgregadas = 0;
        int eliminadas = 0;

        // 1. Crear mapas para comparación
        Map<String, TrelloCard> newCardsMap = new HashMap<>();
        Map<String, TAREAS> existingTasksMap = new HashMap<>();

        // Llenar mapa de nuevas tarjetas
        for (TrelloCard card : newCards) {
            if (card != null && card.getId() != null) {
                newCardsMap.put(card.getId(), card);
            }
        }

        // Llenar mapa de tareas existentes
        for (TAREAS task : tareas) {
            if (task != null && task.getIdReal() != null) {
                existingTasksMap.put(task.getIdReal(), task);
            }
        }

        // 2. Verificar tareas eliminadas
        List<TAREAS> tasksToRemove = new ArrayList<>();
        for (Map.Entry<String, TAREAS> entry : existingTasksMap.entrySet()) {
            if (!newCardsMap.containsKey(entry.getKey())) {
                tasksToRemove.add(entry.getValue());
                eliminadas++;
                Log.d("DataHolder", "Tarea eliminada - ID: " + entry.getKey());
            }
        }
        tareas.removeAll(tasksToRemove);

        // 3. Procesar tareas nuevas o actualizadas
        for (TrelloCard card : newCards) {
            try {
                if (card == null || card.getName() == null || card.getId() == null) {
                    continue;
                }

                TAREAS existingTask = existingTasksMap.get(card.getId());
                if (existingTask != null) {
                    // Actualizar tarea existente (incluyendo posición)
                    updateExistingTask(existingTask, card);
                } else {
                    // Procesar como nueva tarea
                    nuevasAgregadas += processNewCard(card);
                }
            } catch (Exception e) {
                Log.e("DataHolder", "Error procesando tarjeta: " + e.getMessage(), e);
            }
        }

        // 4. Ordenar todas las tareas por posición ANTES de notificar
        tareas.sort(Comparator.comparingInt(TAREAS::getPos));

        // Log para verificar el orden
        Log.d("DataHolder", "Tareas ordenadas:");
        for (TAREAS t : tareas) {
            Log.d("DataHolder", "ID: " + t.getIdReal() + ", Pos: " + t.getPos() + ", Nombre: " + t.getCli().getNombre());
        }

        // 5. Notificar resultados
        final int finalNuevas = nuevasAgregadas;
        final int finalEliminadas = eliminadas;

        th.runOnUiThread(() -> {
            String message;
            if (finalNuevas > 0 && finalEliminadas > 0) {
                message = finalNuevas + " nuevas, " + finalEliminadas + " eliminadas";
            } else if (finalNuevas > 0) {
                message = finalNuevas + " tareas nuevas";
            } else if (finalEliminadas > 0) {
                message = finalEliminadas + " tareas eliminadas";
            } else {
                message = "Tareas actualizadas (posiciones cambiadas)";
            }

            callback.onDataLoaded();
        });
    }

    private int processNewCard(TrelloCard card) throws ParseException {
        // Procesar cliente nuevo
        Cliente cliente = new Cliente();
        cliente.setNombre(card.getName() != null ? card.getName() : "Sin nombre");
        cliente.setTelefono(card.getTelefono() != null ? card.getTelefono() : "SN");

        try {
            if (card.getPrecinto() > 0) {
                cliente.setPrecinto(card.getPrecinto());
            } else if (card.getPrecinto() != 0) {
                cliente.setPrecinto(Integer.parseInt(String.valueOf(card.getPrecinto())));
            }
        } catch (Exception e) {
            Log.e("DataHolder", "Error con precinto: " + e.getMessage());
            cliente.setPrecinto(0);
        }

        // Procesar zona
        String zonaNombre = card.getZona() != null ? card.getZona() : "Sin zona";
        Zona zonaCliente = findOrCreateZone(zonaNombre);

        cliente.setZona(zonaCliente);
        cliente.setDireccion(card.getDireccion() != null ? card.getDireccion() : "Sin dirección");

        // Usar la nueva versión mejorada de getUbicacionFromAttachments
        cliente.setUlrUbicacion(getUbicacionFromAttachments(card.getAttachments()));

        // Agregar cliente a la lista
        cli.add(cliente);

        // Crear nueva tarea
        TAREAS nuevaTarea = createTaskFromCard(card, cliente);
        nuevaTarea.setPos(card.getPos());
        tareas.add(nuevaTarea);
        tareas.sort(Comparator.comparingInt(TAREAS::getPos));

        return 1;
    }

    private void updateExistingTask(TAREAS existingTask, TrelloCard card) {
        // Actualizar campos básicos de la tarea
        existingTask.setProblema(card.getProblema() != null ? card.getProblema() : existingTask.getProblema());
        existingTask.setCaja(card.getCaja() != null ? card.getCaja() : existingTask.getCaja());
        existingTask.setCompletada(card.isCompletada());
        existingTask.setPos(card.getPos()); // ¡Importante! Actualizar la posición

        // Actualizar campos del cliente asociado
        Cliente cliente = existingTask.getCli();
        if (cliente != null) {
            cliente.setNombre(card.getName() != null ? card.getName() : cliente.getNombre());
            cliente.setTelefono(card.getTelefono() != null ? card.getTelefono() : cliente.getTelefono());
            cliente.setPrecinto(card.getPrecinto()); // Actualizar precinto si es necesario
        }

        // Log para depuración
        Log.d("DataHolder", "Tarea actualizada - ID: " + existingTask.getIdReal() +
                ", Posición anterior: " + existingTask.getPos() +
                ", Nueva posición: " + card.getPos());
    }

    // Métodos auxiliares
    private Zona findOrCreateZone(String zonaNombre) {
        for (Zona z : zonas) {
            if (z != null && z.getName() != null && z.getName().equals(zonaNombre)) {
                return z;
            }
        }
        Zona nuevaZona = new Zona(zonaNombre, zonas.size() + 1);
        zonas.add(nuevaZona);
        return nuevaZona;
    }

    private String getUbicacionFromAttachments(List<TrelloCard.Attachment> attachments) {
        if (attachments != null) {
            // Primera pasada: buscar URLs de Google Maps explícitos
            for (TrelloCard.Attachment attachment : attachments) {
                if (attachment.getUrl() != null &&
                        (attachment.getUrl().contains("maps.google") ||
                                attachment.getUrl().contains("google.com/maps") ||
                                attachment.getUrl().contains("goo.gl/maps"))) {
                    return attachment.getUrl();
                }
            }

            // Segunda pasada: buscar cualquier URL que no sea imagen
            for (TrelloCard.Attachment attachment : attachments) {
                if (attachment.getUrl() != null &&
                        !attachment.getUrl().matches("(?i).*\\.(jpg|jpeg|png|gif|bmp|webp)$") &&
                        !attachment.getUrl().startsWith("https://trello.com")) {
                    return attachment.getUrl();
                }
            }
        }
        return "Sin ubicacion";
    }

    private TAREAS createTaskFromCard(TrelloCard card, Cliente cliente) throws ParseException {
        TAREAS nuevaTarea = new TAREAS();
        try {
            nuevaTarea.setId(card.getIdShort() > 0 ? card.getIdShort() : tareas.size() + 1);
        } catch (Exception e) {
            nuevaTarea.setId(tareas.size() + 1);
        }
        nuevaTarea.setIdReal(card.getId());
        nuevaTarea.setInicioReclamo(card.getDueDate());
        nuevaTarea.setCli(cliente);
        nuevaTarea.setProblema(card.getProblema() != null ? card.getProblema() : "Sin problema");
        nuevaTarea.setCaja(card.getCaja() != null ? card.getCaja() : "Sin caja");
        nuevaTarea.setCompletada(card.isCompletada());

        if (!card.getComentarios().isEmpty()) {
            nuevaTarea.setComentarios(card.getComentarios());
        }

        return nuevaTarea;
    }

    public void agregarComentarioATarea(String cardId, String comentario, FragmentActivity activity, APIResponseCallback callback) {
        if (apiH == null) {
            Toast.makeText(activity, "API no inicializada", Toast.LENGTH_SHORT).show();
            callback.onError(new VolleyError("APIHelper no inicializado"));
            return;
        }

        apiH.addCommentToCard(cardId, comentario, new APIResponseCallback() {
            @Override
            public void onSuccess(ArrayList<TrelloCard> cards) {}

            @Override
            public void onSuccess() {
                activity.runOnUiThread(() -> {
                    //Toast.makeText(activity, "Comentario guardado", Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                });
            }

            @Override
            public void onError(VolleyError error) {
                activity.runOnUiThread(() -> {
                    //Toast.makeText(activity, "Error al guardar comentario", Toast.LENGTH_SHORT).show();
                    callback.onError(error);
                });
            }

            @Override
            public void onEmptyResponse() {
                callback.onEmptyResponse();
            }
        });
    }

    public void cargarImagenComoComentario2(String cardId, File imagenFile, FragmentActivity activity, APIResponseCallback callback) {
        if (apiH == null) {
            Toast.makeText(activity, "API no inicializada", Toast.LENGTH_SHORT).show();
            callback.onError(new VolleyError("APIHelper no inicializado"));
            return;
        }

        // Convertir imagen a Base64
        String imagenBase64 = convertirImagenABase64(imagenFile);
        if (imagenBase64 == null) {
            callback.onError(new VolleyError("Error al convertir imagen"));
            return;
        }

        // Crear comentario con la imagen embebida
        String comentario = "![Imagen](data:image/jpeg;base64," + imagenBase64 + ")";

        // Usar el método existente para agregar comentario
        agregarComentarioATarea(cardId, comentario, activity, callback);
    }

    private String convertirImagenABase64(File imagenFile) {
        try {
            FileInputStream inputStream = new FileInputStream(imagenFile);
            byte[] bytes = new byte[(int) imagenFile.length()];
            inputStream.read(bytes);
            inputStream.close();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("Base64Error", "Error al convertir imagen", e);
            return null;
        }
    }

    public void cargarImagenComoComentario(String cardId, File imagenFile, FragmentActivity activity, APIResponseCallback callback) {
        if (apiH == null || !imagenFile.exists()) {
            mostrarError(activity, "Recursos no disponibles", callback);
            return;
        }

        new Thread(() -> {
            try {
                // 1. Subir imagen como adjunto
                subirAdjuntoTrello(cardId, imagenFile, activity, new APIResponseCallback() {
                    @Override
                    public void onSuccess(ArrayList<TrelloCard> cards) {}

                    @Override
                    public void onSuccess() {
                        // 2. Crear comentario con enlace
                        String comentario = "Firma adjunta: " + obtenerNombreArchivo(imagenFile);
                        agregarComentarioATarea(cardId, comentario, activity, callback);
                    }

                    @Override
                    public void onError(VolleyError error) {
                        callback.onError(new VolleyError("Error al convertir imagen" + error));
                    }

                    @Override
                    public void onEmptyResponse() {
                        callback.onError(new VolleyError("Respuesta Vacia"));
                    }
                });
            } catch (Exception e) {
                callback.onError(new VolleyError("Error al convertir imagen "+e));
            }
        }).start();
    }


    private void subirAdjuntoTrello(String cardId, File archivo, FragmentActivity activity, APIResponseCallback callback) {
        String url = "https://api.trello.com/1/cards/" + cardId +
                "/attachments?key=" + apiH.getApiKey() +
                "&token=" + apiH.getApiToken();

        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json");

        Map<String, String> stringParams = new HashMap<>();
        stringParams.put("name", "Firma del cliente");

        Map<String, File> fileParams = new HashMap<>();
        fileParams.put("file", archivo);

        VolleyMultipartRequest request = new VolleyMultipartRequest.Builder()
                .setUrl(url)
                .setMethod(Request.Method.POST)
                .setHeaders(headers)
                .addStringParts(stringParams)
                .addFileParts(fileParams)
                .setListener(response -> {
                    // Éxito
                    activity.runOnUiThread(() -> callback.onSuccess());
                })
                .setErrorListener(error -> {
                    // Error
                    activity.runOnUiThread(() -> callback.onError(error));
                })
                .build();

        // Agregar a la cola de Volley
        Volley.newRequestQueue(activity).add(request);
    }

    private String obtenerTipoMIME(File archivo) {
        String extension = archivo.getName().substring(archivo.getName().lastIndexOf(".") + 1);
        switch (extension.toLowerCase()) {
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            default:
                return "application/octet-stream";
        }
    }

    private String obtenerNombreArchivo(File archivo) {
        return archivo.getName().replaceFirst("[.][^.]+$", "");
    }

    private void mostrarError(FragmentActivity activity, String mensaje, APIResponseCallback callback) {
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, mensaje, Toast.LENGTH_SHORT).show();
            callback.onError(new VolleyError(mensaje));
        });
    }

    //Cargar datos de la base de datos
    public void obtenerListaTareas(TareasCallback callback) {
        new Thread(() -> {
            try {
                String rawResponse = obtenerRespuestaServidor();
                parsearPhpDirecto(rawResponse);
                cargado = true;
                callback.onExito(tareas);
            } catch (Exception e) {
                callback.onFallo("Error: " + e.getMessage());
            }
        }).start();
    }

    private void parsearPhpDirecto(String phpResponse) {
        String[] bloques = phpResponse.split("\\[\\d+\\] => Array");

        for (String bloque : bloques) {
            if (bloque.trim().isEmpty()) continue;

            HashMap<String, String> datos = new HashMap<>();
            String[] lineas = bloque.split("\n");

            for (String linea : lineas) {
                if (linea.contains("=>")) {
                    String[] partes = linea.split("=>", 2);
                    if (partes.length < 2) continue;

                    String clave = partes[0].replaceAll("\\[|\\]|\\s", "");
                    String valor = partes[1].trim().replace("(", "").replace(")", "");
                    datos.put(clave, valor);
                }
            }

            if (datos.containsKey("id")) {
                TAREAS t = new TAREAS();
                Cliente cliente = new Cliente();

                // Cliente
                cliente.setNombre(datos.getOrDefault("cliente", "Sin nombre"));
                cliente.setTelefono(datos.getOrDefault("telefono", "SN"));
                cliente.setDireccion(datos.getOrDefault("direccion", "Sin dirección"));
                cliente.setZonaS(datos.getOrDefault("zona", "Sin zona"));
                cliente.setUlrUbicacion(datos.getOrDefault("url_mapa", "Sin ubicación"));

                try {
                    cliente.setPrecinto(Integer.parseInt(datos.getOrDefault("precinto", "0")));
                } catch (Exception e) {
                    Log.e("DataHolder", "Error con precinto: " + e.getMessage());
                    cliente.setPrecinto(0);
                }

                cli.add(cliente);

                // Tarea
                try {
                    t.setId(Integer.parseInt(datos.get("id")));
                } catch (Exception e) {
                    t.setId(tareas.size() + 1);
                }

                String usuario = datos.getOrDefault("usuario_asignado", "Sin usuario");
                t.setUsuario(usuario);

                Date fecha;
                try {
                    fecha = formatearFecha(usuario);
                } catch (ParseException e) {
                    fecha = new Date();
                }
                t.setInicioReclamo(fecha);

                t.setCli(cliente);
                String estado = "Sin Problema";

                switch (Integer.parseInt(datos.get("estado"))){
                    case 0:
                        estado="INSTALACION";
                        break;
                    case 1:
                        estado="Sin Servicio";
                        break;
                    case 2:
                        estado="Atenuacion";
                        break;
                    case 3:
                        estado="Servicio Intermitente";
                        break;
                    case 4:
                        estado="TRABAJO COORDINADO";
                        break;
                    case 5:
                        estado="Cambio o revision de ONU";
                        break;
                    case 6:
                        estado="Solucionado";
                        break;
                }
                t.setProblema(estado);
                t.setCaja(datos.getOrDefault("caja", "Sin caja"));
                t.setCompletada(false);
                t.setPos(tareas.size() + 1);

                if (datos.containsKey("detalle") && !datos.get("detalle").isEmpty()) {
                    t.setDetalle(datos.get("detalle"));
                }

                tareas.add(t);
            }
        }
    }

    private String obtenerRespuestaServidor() throws Exception {
        URL url = new URL("https://netlatin.dnatech.com.ar/admin/tickets.rpt.php");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = br.readLine()) != null) {
            response.append(line).append("\n");
        }
        br.close();

        return response.toString();
    }

    public static Date formatearFecha(String dateString) throws ParseException {
        // Definir el formato que coincide con tu String
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSS", Locale.getDefault());

        // Parsear el String a Date
        return formatter.parse(dateString);
    }


}

