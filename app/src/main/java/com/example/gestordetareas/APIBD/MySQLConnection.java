package com.example.gestordetareas.APIBD;


import android.app.Activity;
import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;

import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MySQLConnection {
    private static final String BASE_URL = "https://dnatech.com.ar/gonzalo/";
    private final RequestQueue requestQueue;
    private int timeout = 30000; // 30 segundos por defecto

    public interface DatabaseResponseListener {
        void onSuccess(JSONObject response);
        void onError(String error);
    }
    public interface CallbackTareasString {
        void onSuccess(String tareasJsonString);
        void onError(String errorMessage);
    }
    public interface CallbackTareas {
        void onSuccess(ArrayList<HashMap<String, String>> response);
        void onError(String error);
    }
    public interface SpeedTestListener {
        void onDownloadSpeed(float speedMbps);
        void onLatency(long latencyMs);
        void onError(String error);
    }

    public MySQLConnection(Context context) {
        this.requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public void setTimeout(int milliseconds) {
        this.timeout = milliseconds;
    }




    public void agregarTarea(String cliente, String problemas, String auto, String comentario,
                             boolean completado, String base64Imagen1, String base64Imagen2,
                             String base64Firma, DatabaseResponseListener listener) {

        String url = BASE_URL + "agregar_tareas.php";

        // Debug: Verificar que los datos Base64 no sean nulos o vacíos
        Log.d("BASE64_UPLOAD", "Imagen1 presente: " + (base64Imagen1 != null && !base64Imagen1.isEmpty()));
        Log.d("BASE64_UPLOAD", "Imagen2 presente: " + (base64Imagen2 != null && !base64Imagen2.isEmpty()));
        Log.d("BASE64_UPLOAD", "Firma presente: " + (base64Firma != null && !base64Firma.isEmpty()));

        // Crear el objeto JSON con todos los datos
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("cliente", cliente);
            jsonBody.put("problemas", problemas);
            jsonBody.put("auto", auto);
            jsonBody.put("comentario", comentario);
            jsonBody.put("completado", completado ? "1" : "0");

            if (base64Imagen1 != null && !base64Imagen1.isEmpty()) {
                jsonBody.put("imagen1", base64Imagen1);
            }
            if (base64Imagen2 != null && !base64Imagen2.isEmpty()) {
                jsonBody.put("imagen2", base64Imagen2);
            }
            if (base64Firma != null && !base64Firma.isEmpty()) {
                jsonBody.put("firma", base64Firma);
            }
        } catch (JSONException e) {
            listener.onError("Error creando JSON: " + e.getMessage());
            return;
        }

        // Crear la petición JSON
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Log.d("API_RESPONSE", response.toString());
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMsg = "Error de red";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    Log.e("API_ERROR", errorMsg);
                    listener.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        // Configurar política de reintentos
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(jsonObjectRequest);
    }

    // Método auxiliar para convertir File a Base64
    public static String fileToBase64(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            inputStream.read(bytes);
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("BASE64_CONVERSION", "Error al convertir archivo a Base64", e);
            return null;
        }
    }

    public static void Conexion(Context context,
                                final Response.Listener<String> listener,
                                final Response.ErrorListener errorListener) {

        String url = BASE_URL + "conexion.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                listener,
                error -> {
                    Log.e("API_ERROR", "Error en conexión: ", error);
                    errorListener.onErrorResponse(error);
                });

        Volley.newRequestQueue(context).add(request);
    }

    public void loginUsuario(String usuario, String clave, DatabaseResponseListener listener) {
        String url = BASE_URL + "Login.php";

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("usuario", usuario);
            jsonBody.put("clave", clave);
        } catch (JSONException e) {
            listener.onError("Error creando JSON: " + e.getMessage());
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                jsonBody,
                response -> {
                    Log.d("LOGIN_API", response.toString());
                    listener.onSuccess(response);
                },
                error -> {
                    String errorMsg = "Error de red";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    }
                    Log.e("LOGIN_ERROR", errorMsg);
                    listener.onError(errorMsg);
                }
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
        };

        request.setRetryPolicy(new DefaultRetryPolicy(
                timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(request);
    }

    public void testNetworkSpeed(Activity activity, SpeedTestListener listener) {
        String url = BASE_URL + "speedtest.php";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.has("error")) {
                            listener.onError(response.getString("error"));
                            return;
                        }

                        JSONObject data = response.getJSONObject("data");
                        float speedMbps = (float) data.getDouble("download_speed_mbps");
                        long latencyMs = data.getLong("latency_ms");
                        activity.runOnUiThread(() -> {
                            listener.onDownloadSpeed(speedMbps);
                            listener.onLatency(latencyMs);
                        });

                    } catch (JSONException e) {
                        listener.onError("Error en formato de respuesta: " + e.getMessage());
                    }
                },
                error -> {
                    String errorMsg = "Error de red";
                    if (error.networkResponse != null) {
                        errorMsg += " (Código: " + error.networkResponse.statusCode + ")";
                    }
                    listener.onError(errorMsg);
                }
        );

        request.setRetryPolicy(new DefaultRetryPolicy(
                20000, // 20 segundos de timeout
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }


    /// cargar tareas
    public static void cargarTareasFromUrl(String urlString, CallbackTareas callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Limpiar la respuesta de etiquetas HTML
                String cleanResponse = response.toString()
                        .replace("<pre>", "")
                        .replace("</pre>", "")
                        .trim();

                JSONArray jsonArray = new JSONArray(cleanResponse);
                //ArrayList<HashMap<String, String>> tareasList = parseJsonToArray(jsonArray);
                //callback.onSuccess(tareasList);

            } catch (Exception e) {
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        }).start();
    }


    public static void cargarTareasDeUrl(String urlString, CallbackTareas callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine).append("\n");
                }
                in.close();

                String rawData = response.toString();
                ArrayList<HashMap<String, String>> tareas = parsearRespuestaPHP(rawData);

                callback.onSuccess(tareas);
            } catch (Exception e) {
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        }).start();
    }
    private static ArrayList<HashMap<String, String>> parsearRespuestaPHP(String texto) {
        ArrayList<HashMap<String, String>> lista = new ArrayList<>();
        HashMap<String, String> itemActual = null;

        String[] lineas = texto.split("\n");
        for (String linea : lineas) {
            linea = linea.trim();

            if (linea.matches("^\\[\\d+\\] => Array$")) {
                // Nuevo objeto
                if (itemActual != null) {
                    lista.add(itemActual);
                }
                itemActual = new HashMap<>();
            } else if (linea.matches("^\\[.*?\\] => .*")) {
                // Línea tipo [clave] => valor
                int sepIndex = linea.indexOf("] =>");
                if (sepIndex > 0) {
                    String key = linea.substring(1, sepIndex);
                    String value = linea.substring(sepIndex + 4).trim();
                    if (itemActual != null) {
                        itemActual.put(key, value);
                    }
                }
            }
        }

        // Agregar último item si existe
        if (itemActual != null) {
            lista.add(itemActual);
        }

        return lista;
    }



    public static void obtenerTareasComoString(String url, CallbackTareasString callback) {
        cargarTareasFromUrl(url, new CallbackTareas() {
            @Override
            public void onSuccess(ArrayList<HashMap<String, String>> tareasList) {
                try {
                    JSONArray jsonArray = new JSONArray();

                    for (HashMap<String, String> tarea : tareasList) {
                        JSONObject jsonObject = new JSONObject(tarea);
                        jsonArray.put(jsonObject);
                    }

                    String resultado = jsonArray.toString(4); // 4 para formato bonito (indentado)
                    callback.onSuccess(resultado);

                } catch (Exception e) {
                    callback.onError("Error al convertir a JSON: " + e.getMessage());
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}