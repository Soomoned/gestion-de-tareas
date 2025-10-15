package com.example.gestordetareas.ui.tarea;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.icu.text.SimpleDateFormat;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.android.volley.VolleyError;
import com.example.gestordetareas.APIBD.MySQLConnection;
import com.example.gestordetareas.APIMaps.Unshorten;
import com.example.gestordetareas.APITrello.APIResponseCallback;
import com.example.gestordetareas.APITrello.TrelloCard;
import com.example.gestordetareas.Clases.TAREAS;
import com.example.gestordetareas.DataHolder;
import com.example.gestordetareas.R;
import com.example.gestordetareas.funcionalidades.DownloadSpeedTest;
import com.example.gestordetareas.ui.SignaturePad;
import com.google.android.gms.common.util.BiConsumer;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.preference.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class TareaFragment extends Fragment {

    private DataHolder dataholder;
    private TAREAS tarea;
    private TextView txtCliente;
    private ImageButton btnMaps;
    private Button btnFinalizar;

    private MapView mapView;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ArrayList<File> fotos = new ArrayList<>();
    TextView txtCamera;
    ImageView imageViewCamera;
    private ImageView imageViewTest;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker markerUsuario;
    private File currentPhotoFile;
    private TextView txtVelocidad;
    private Button btnBD;
    private EditText editText;
    private ProgressDialog progressDialog;
    private boolean ubicacionCliente = false;

    private boolean mapaIniciado = false;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_tarea, container, false);
        inicializarProgressDialog();
        // Inicializar el ActivityResultLauncher para la cámara
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (currentPhotoFile != null) {
                            fotos.add(currentPhotoFile);
                            updatePhotoCounter();
                            Toast.makeText(requireContext(), "Foto capturada", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Si el usuario canceló, eliminar el archivo temporal
                        if (currentPhotoFile != null && currentPhotoFile.exists()) {
                            currentPhotoFile.delete();
                        }
                    }
                    currentPhotoFile = null;
                }
        );

        dataholder = DataHolder.getInstance();
        tarea = dataholder.getTarea();

        // Inicializar vistas
        txtCliente = root.findViewById(R.id.txtCliente);
        btnMaps = root.findViewById(R.id.imBtnMaps);
        btnFinalizar = root.findViewById(R.id.btnFinalizar);

        // Configurar vistas
        txtCliente.setText(formateoCliente());

        // Configurar listeners
        btnMaps.setOnClickListener(v -> abroMaps());
        btnFinalizar.setOnClickListener( (View v) -> {
            if ("INSTALACION".equals(tarea.getProblema())) {
                popupInstalacion();
            } else {
                inicioPopup();
            }
        });

        btnBD = root.findViewById(R.id.btnBD);
        btnBD.setVisibility(View.GONE);
        btnBD.setOnClickListener(v -> {
            prueboConexion();
        });
        return root;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamara();
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara requerido", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataholder = DataHolder.getInstance();
        tarea = dataholder.getTarea();
        procesarUrlMaps(tarea.getCli().getUlrUbicacion(), view);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (fusedLocationClient != null && locationCallback != null && mapView != null) {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(
                        new LocationRequest.Builder(5000).build(),
                        locationCallback,
                        null
                );
            }
        }
        FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        FloatingActionButton fab = requireActivity().findViewById(R.id.fab);
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        super.onDestroy();
    }

    private SpannableStringBuilder formateoCliente() {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (tarea == null) {
            builder.append("Tarea nula");
            return builder;
        }

        int start = 0;
        int end ;

        // Helper para aplicar estilos fácilmente
        BiConsumer<String, String> appendField = (label, value) -> {
            int startlocal = builder.length();
            builder.append(label);
            int endlocal = builder.length();
            builder.setSpan(new StyleSpan(Typeface.BOLD), startlocal, endlocal, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new RelativeSizeSpan(1.7f), startlocal, endlocal, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Agranda el label

            int valueStart = builder.length();
            builder.append(value).append("\n");
            builder.setSpan(new RelativeSizeSpan(1.7f), valueStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // Agranda el valor
        };



        // Cliente
        String nombre = tarea.getCli() != null && tarea.getCli().getNombre() != null ? tarea.getCli().getNombre() : "Sin nombre";
        appendField.accept("Cliente: ", nombre);

        // Precinto
        String precinto = tarea.getCli() != null && tarea.getCli().getPrecinto() > 0 ? String.valueOf(tarea.getCli().getPrecinto()) : "SN";
        appendField.accept("Precinto: ", precinto);

        // Zona
        String zona = tarea.getCli() != null && tarea.getCli().getZona() != null ? tarea.getCli().getZona().getName() : "Sin zona";
        appendField.accept("Zona: ", zona);

        // Caja
        String caja = tarea.getCaja() != null ? tarea.getCaja() : "Sin caja";
        appendField.accept("Caja: ", caja);

        // Problema
        String problema = tarea.getProblema() != null ? tarea.getProblema() : "Sin problema";
        start = builder.length();
        builder.append("Problema: ");
        end = builder.length();
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(1.7f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        start = builder.length();
        builder.append(problema);
        end = builder.length();


        int color;
        Context context = getContext(); // o requireContext() si estás en un Fragment

        switch (problema) {
            case "Sin Servicio":
                color = Color.RED;
                break;
            case "Servicio Intermitente":
                color = Color.parseColor("#FFA500"); // Naranja
                break;
            case "Solucionado":
                color = Color.parseColor("#2E7D32"); // Verde
                break;
            case "Sin Problema":
            case "":
            default:
                color = ContextCompat.getColor(context, R.color.problemaDefault);
                break;
        }


        builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new RelativeSizeSpan(1.7f), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return builder;
    }


    private void inicioPopup() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View popupView = inflater.inflate(R.layout.popup_custom, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);

        TextView txtMain = popupView.findViewById(R.id.title_popup);
        editText = popupView.findViewById(R.id.editText_popup);
        Spinner spinner = popupView.findViewById(R.id.spinner);
        ImageButton imageButton = popupView.findViewById(R.id.imageButton);
        ImageButton btnCamera = popupView.findViewById(R.id.btn_camera);
        ImageView imageViewProblemas = popupView.findViewById(R.id.imageViewProblemas);
        imageViewCamera = popupView.findViewById(R.id.imageViewCamera);
        ListView lstText = popupView.findViewById(R.id.lstTextProblemas);
        LinearLayout lyProblemas = popupView.findViewById(R.id.lyProblemas);
        LinearLayout lyCamera = popupView.findViewById(R.id.lyCamera);
        LinearLayout lySpeed = popupView.findViewById(R.id.lySpeed);

        txtCamera = popupView.findViewById(R.id.textView2);
        Button btnSpeed = popupView.findViewById(R.id.btnSpeed);
        SwitchCompat sCompletado =popupView.findViewById(R.id.sCompletado);
        txtVelocidad = popupView.findViewById(R.id.txtSpeed);
        btnSpeed.setText("TEST VELOCIDAD");
        txtVelocidad.setVisibility(View.GONE);
        imageViewTest = popupView.findViewById(R.id.imageViewTest);
        imageViewTest.setVisibility(View.GONE);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1001);
        }
        updatePhotoCounter();
        sCompletado.setOnClickListener(v -> {
            if(sCompletado.isChecked()) {
                editText.setHint("Detalle");
                lyCamera.setVisibility(View.VISIBLE);
                lySpeed.setVisibility(View.VISIBLE);
            }else{
                editText.setHint("Causa");
                lyCamera.setVisibility(View.GONE);
                lySpeed.setVisibility(View.GONE);
            }
        });
        ArrayList<String> problemasList = new ArrayList<>();

        imageViewProblemas.setVisibility(View.GONE);
        imageViewCamera.setVisibility(View.GONE);

        ArrayAdapter<String> problemasAdapter = new ArrayAdapter<String>(
                requireContext(),
                R.drawable.item_problema,
                R.id.txtProblema,
                problemasList
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ImageButton btnEliminar = view.findViewById(R.id.btnEliminar);

                btnEliminar.setOnClickListener(v -> {
                    problemasList.remove(position);
                    notifyDataSetChanged();
                    // Ocultar ListView si no hay elementos
                    lstText.setVisibility(problemasList.isEmpty() ? View.GONE : View.VISIBLE);
                });

                return view;
            }
        };
        lstText.setAdapter(problemasAdapter);
        lstText.setVisibility(View.GONE);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_custom,
                R.id.textSpinner,
                getResources().getStringArray(R.array.opciones_spinner)
        );
        ArrayAdapter<CharSequence> adapterCompletado = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_custom,
                R.id.textSpinner,
                getResources().getStringArray(R.array.opciones_completado)
        );

        adapter.setDropDownViewResource(R.layout.spinner_item_custom);
        adapterCompletado.setDropDownViewResource(R.layout.spinner_item_custom);
        spinner.setAdapter(adapter);


        imageButton.setOnClickListener(v -> {
            String problema = spinner.getSelectedItem().toString();
            if (!problemasList.contains(problema)) {
                problemasList.add(problema);
                problemasAdapter.notifyDataSetChanged();
                lstText.setVisibility(View.VISIBLE);
                imageViewProblemas.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(requireContext(), "Este problema ya fue agregado", Toast.LENGTH_SHORT).show();
            }
        });

        btnSpeed.setOnClickListener(v -> {
            probarVelocidad();
        });

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

                ActivityCompat.requestPermissions(
                        requireActivity(),
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE} :
                                new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION
                );
            } else {
                abrirCamara();
            }
        });

        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                spinner.performClick();
                spinner.post(() -> {
                    DisplayMetrics metrics = new DisplayMetrics();
                    requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int screenWidth = metrics.widthPixels;
                    spinner.setDropDownWidth((int)(screenWidth * 0.9));
                });
                return true;
            }
            return false;
        });

        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        Button btnCancel = popupView.findViewById(R.id.btn_cancel);
        Button btnAccept = popupView.findViewById(R.id.btn_accept);

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
        btnAccept.setOnClickListener(v -> {

            if (sCompletado.isChecked()) {
                if (problemasList.isEmpty()) {
                    Toast.makeText(requireContext(), "Debe agregar al menos un problema", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    return;
                } else if (fotos.size() < 2) {
                    Toast.makeText(requireContext(), "Debe agregar al menos 2 fotos", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    return;
                }
                if(imageViewTest.getVisibility() == View.GONE){
                    Toast.makeText(requireContext(), "Realizar test de velocidad", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    return;
                }
            } else {
                if (editText.getText().toString().trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Debe ingresar la causa del problema", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    return;
                }
            }

            mostrarPopupFirma(popupWindow, sCompletado, problemasList, editText);
        });
    }
    private void popupInstalacion() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View popupView = inflater.inflate(R.layout.popup_instalacion, null);

        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(requireView(), Gravity.CENTER, 0, 0);

        TextView txtTitle = popupView.findViewById(R.id.title_popup);
        editText = popupView.findViewById(R.id.editText_popup);
        Spinner spinner = popupView.findViewById(R.id.spinner);
        Button btnCancel = popupView.findViewById(R.id.btn_cancel);
        Button btnAccept = popupView.findViewById(R.id.btn_accept);
        ImageButton btnCamera = popupView.findViewById(R.id.btn_camera);
        LinearLayout lySpeed = popupView.findViewById(R.id.lySpeed);
        LinearLayout lyCamera = popupView.findViewById(R.id.lyCamera);
        LinearLayout lyCausa = popupView.findViewById(R.id.lyCausa);
        SwitchCompat sCompletado = popupView.findViewById(R.id.sCompletado);

        if (sCompletado.isChecked()) {
            lyCausa.setVisibility(View.GONE);
            lyCamera.setVisibility(View.VISIBLE);
            lySpeed.setVisibility(View.VISIBLE);
        } else {
            lyCausa.setVisibility(View.VISIBLE);
            lyCamera.setVisibility(View.GONE);
            lySpeed.setVisibility(View.GONE);
        }

        sCompletado.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                lyCausa.setVisibility(View.GONE);
                lyCamera.setVisibility(View.VISIBLE);
                lySpeed.setVisibility(View.VISIBLE);
            } else {
                lyCausa.setVisibility(View.VISIBLE);
                lyCamera.setVisibility(View.GONE);
                lySpeed.setVisibility(View.GONE);
            }
        });

        txtCamera = popupView.findViewById(R.id.textView2);
        Button btnSpeed = popupView.findViewById(R.id.btnSpeed);

        txtVelocidad = popupView.findViewById(R.id.txtSpeed);
        btnSpeed.setText("TEST VELOCIDAD");
        txtVelocidad.setVisibility(View.GONE);
        imageViewTest = popupView.findViewById(R.id.imageViewTest);
        imageViewTest.setVisibility(View.GONE);

        sCompletado.setOnClickListener(v -> {
            if(sCompletado.isChecked()) {
                lyCausa.setVisibility(View.GONE);
                lyCamera.setVisibility(View.VISIBLE);
                lySpeed.setVisibility(View.VISIBLE);
            }else{
                lyCausa.setVisibility(View.VISIBLE);
                lyCamera.setVisibility(View.GONE);
                lySpeed.setVisibility(View.GONE);
            }
        });


        btnSpeed.setOnClickListener(v -> {
            probarVelocidad();
        });
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_custom,
                R.id.textSpinner,
                getResources().getStringArray(R.array.opciones_spinner_causa)
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_custom);
        spinner.setAdapter(adapter);

        btnCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {

                ActivityCompat.requestPermissions(
                        requireActivity(),
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ?
                                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE} :
                                new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION
                );
            } else {
                abrirCamara();
            }
        });
        spinner.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                spinner.performClick();
                spinner.post(() -> {
                    DisplayMetrics metrics = new DisplayMetrics();
                    requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int screenWidth = metrics.widthPixels;
                    spinner.setDropDownWidth((int)(screenWidth * 0.9));
                });
                return true;
            }
            return false;
        });

        btnCancel.setOnClickListener(v -> popupWindow.dismiss());
        btnAccept.setOnClickListener(v -> {
            mostrarPopupFirmaInstalacion(popupWindow);
        });

    }




    private void abroMaps() {
        try {
            String mapsUrl = tarea.getCli().getUlrUbicacion();
            boolean formato = false;

            if (mapsUrl == null || mapsUrl.isEmpty()) {
                Toast.makeText(requireContext(), "No hay ubicación disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);

            if (mapsUrl.startsWith("https://maps")) {
                intent.setData(Uri.parse(mapsUrl));
                formato = true;
            } else {
                intent.setData(Uri.parse("geo:0,0?q=" + Uri.encode(tarea.getCli().getDireccion())));
            }

            if(formato) {
                Toast.makeText(requireContext(), "Formato Correcto", Toast.LENGTH_SHORT).show();
            }

            intent.setPackage("com.google.android.apps.maps");

            if (intent.resolveActivity(requireActivity().getPackageManager()) != null && formato) {
                startActivity(intent);
            } else if (formato) {
                String webUrl = mapsUrl.startsWith("http") ? mapsUrl :
                        "https://www.google.com/maps/search/?api=1&query=" + Uri.encode(mapsUrl);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)));
            } else {
                Toast.makeText(requireContext(), "UBICACION NO DISPONIBLE", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error al abrir Maps", Toast.LENGTH_SHORT).show();
            Log.e("MAPS_ERROR", "Error: " + e.getMessage());
        }
    }


    //trello
    private void agregoComentario(String comentario){
        DataHolder.getInstance().agregarComentarioATarea(
                dataholder.getTarea().getIdReal(),
                comentario,
                requireActivity(),
                new APIResponseCallback() {
                    @Override
                    public void onSuccess(ArrayList<TrelloCard> cards) {}

                    @Override
                    public void onSuccess() {
                        // Comentario exitoso
                        Log.d("Comentario", "¡Comentario agregado!");
                    }

                    @Override
                    public void onError(VolleyError error) {
                        Log.e("Comentario", "Error: " + error.getMessage());
                    }

                    @Override
                    public void onEmptyResponse() {}
                }
        );
    }

    //FUNCIONES PARA EL CONTROL DEL MAPA
    private void inicializarMapa(View view, double lat, double lon) {
        Context ctx = requireContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        mapView = view.findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        if (tarea != null && tarea.getCli() != null) {
            // 1. Centrar en el cliente (si existe)
            GeoPoint puntoCliente = new GeoPoint(lat, lon);
            mapController.setCenter(puntoCliente);

            Marker markerCliente = new Marker(mapView);
            markerCliente.setPosition(puntoCliente);
            markerCliente.setTitle("Ubicación del cliente");
            markerCliente.setIcon(ContextCompat.getDrawable(ctx, R.drawable.ic_cliente));
            mapView.getOverlays().add(markerCliente);
        } else {
            Toast.makeText(ctx, "No hay datos de ubicación del cliente", Toast.LENGTH_LONG).show();
        }

        // 2. Obtener ubicación del usuario (SIEMPRE, pero sin centrar si hay cliente)
        obtenerUbicacionUsuario(ctx, mapView);
    }
    private void procesarUrlMaps(String shortUrl, View view) {
        new Thread(() -> {
            try {
                String expandedUrl = Unshorten.expand(shortUrl);
                Log.d("URL_DEBUG", "URL expandida: " + expandedUrl);

                requireActivity().runOnUiThread(() -> {
                    if (expandedUrl != null && !expandedUrl.equals(shortUrl)) {
                        extractCoordinatesFromUrl(expandedUrl, view);
                    } else {
                        extractCoordinatesFromUrl(shortUrl, view);
                    }
                });
            } catch (IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Error al procesar URL", Toast.LENGTH_SHORT).show();
                    ubicacionCliente = false;
                    mostrarMapaUbicacionUsuario();
                    Log.e("URL_ERROR", "Error: " + e.getMessage());
                });
            }
        }).start();
    }
    private void extractCoordinatesFromUrl(String longUrl, View view) {
        try {
            Pattern pattern1 = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Pattern pattern2 = Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");

            Matcher matcher = pattern1.matcher(longUrl);
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                ubicacionCliente = true;
                inicializarMapa(view, lat, lng);
                return;
            }

            matcher = pattern2.matcher(longUrl);
            if (matcher.find()) {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                ubicacionCliente = true;
                inicializarMapa(view, lat, lng);
                return;
            }

            // Si no hay coordenadas
            ubicacionCliente = false;
            mostrarMapaUbicacionUsuario();

        } catch (Exception e) {
            ubicacionCliente = false;
            mostrarMapaUbicacionUsuario();
        }
    }
    private void mostrarMapaUbicacionUsuario() {
        Context ctx = requireContext();

        // Configurar el mapa
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        mapView = getView().findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Obtener la ubicación del usuario
        obtenerUbicacionUsuario(ctx, mapView);

    }
    private void obtenerUbicacionUsuario(Context context, MapView mapView) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (location == null) return;

                double latUsuario = location.getLatitude();
                double lonUsuario = location.getLongitude();
                GeoPoint puntoUsuario = new GeoPoint(latUsuario, lonUsuario);

                // A. Actualizar marcador del usuario
                if (markerUsuario == null && !mapaIniciado) {
                    markerUsuario = new Marker(mapView);
                    markerUsuario.setTitle("Tu ubicación");
                    markerUsuario.setIcon(ContextCompat.getDrawable(context, R.drawable.arrow_direction));
                    markerUsuario.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    mapView.getOverlays().add(markerUsuario);
                    mapaIniciado = true;
                }
                markerUsuario.setPosition(puntoUsuario);

                // B. Centrar SOLO si no hay cliente
                if (!ubicacionCliente) {
                    mapView.getController().setCenter(puntoUsuario);
                    mapView.getController().setZoom(15.0);
                }

                mapView.invalidate();
            }
        };

        // Configuración de alta precisión
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build();
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
    private void procesarDatosFinales(SwitchCompat spinnerCompletado, ArrayList<String> problemasList,
                                      EditText editText, String firmaPath) {
        // Mostrar diálogo de progreso

        if (spinnerCompletado.isChecked()) {
            String resultado = TextUtils.join(", ", problemasList);
            if (!editText.getText().toString().isEmpty()) {
                agregoComentario(editText.getText().toString());
            }
            tarea.setProblema(resultado);
            tarea.setRutaFirma(firmaPath);
        } else {
            agregoComentario("No completada: " + editText.getText().toString());
        }


        agregarTareaBD();
        fotos.clear();

    }
    private void mostrarUbicacionPorDefecto(MapView mapView) {
        // Centro aproximado de la ciudad (ej: Buenos Aires)
        GeoPoint puntoDefault = new GeoPoint(-34.596561, -58.947914); // Cambia por coordenadas de tu ciudad

        IMapController mapController = mapView.getController();
        mapController.setCenter(puntoDefault);
        mapController.setZoom(80.0);

        Toast.makeText(requireContext(), "No se pudo obtener tu ubicación. Mostrando mapa por defecto.", Toast.LENGTH_LONG).show();
    }

    private void iniciarActualizacionesUbicacion() {
        LocationRequest locationRequest = new LocationRequest.Builder(10000) // 10 segundos
                .setMinUpdateIntervalMillis(5000) // mínimo 5 segundos
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                Location location = locationResult.getLastLocation();
                if (tarea != null && tarea.getCli() != null) {
                    tarea.getCli().setLat(location.getLatitude());
                    tarea.getCli().setLon(location.getLongitude());
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
            );
        }
    }

    //uso de camara y manejo de archivos
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoFile = imageFile; // Guardar referencia al archivo actual
        return imageFile;
    }
    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();

                Uri photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".provider",
                        photoFile
                );

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);

            } catch (IOException e) {
                Toast.makeText(requireContext(), "Error al crear el archivo", Toast.LENGTH_SHORT).show();
                Log.e("CameraError", "Error al crear archivo", e);
            }
        } else {
            Toast.makeText(requireContext(), "No hay aplicación de cámara disponible", Toast.LENGTH_SHORT).show();
        }
    }
    private void updatePhotoCounter() {
        if (txtCamera != null) {
            txtCamera.setText("Agregar Fotos " + fotos.size() + "/2");
            if (fotos.size() >= 2) {
                imageViewCamera.setVisibility(View.VISIBLE);
            }
        }
    }



    /// Popup para la firma
    private void mostrarPopupFirma(PopupWindow popupAnterior, SwitchCompat sCompletado,
                                   ArrayList<String> problemasList, EditText editText) {
        // Inflar el layout del popup de firma
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View popupFirmaView = inflater.inflate(R.layout.dialog_signature, null);

        // Configurar el popup
        PopupWindow popupFirma = new PopupWindow(
                popupFirmaView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupFirma.setOutsideTouchable(false);
        popupFirma.setFocusable(true);

        // Obtener referencias a las vistas
        SignaturePad signaturePad = popupFirmaView.findViewById(R.id.signaturePad);
        Button btnClear = popupFirmaView.findViewById(R.id.btnClear);
        Button btnSave = popupFirmaView.findViewById(R.id.btnSave);

        // Configurar listeners
        btnClear.setOnClickListener(v -> signaturePad.clear());

        btnSave.setOnClickListener(v -> {
            if (!signaturePad.isEmpty()) {
                progressDialog.setMessage("FINALIZANDO TAREA");
                progressDialog.setCancelable(false);
                mostrarDialogo();
                new Thread(() -> {
                    Bitmap firmaBitmap = signaturePad.getSignatureBitmap();
                    String firmaPath = guardarFirma(firmaBitmap);

                    requireActivity().runOnUiThread(() -> {
                        if (firmaPath != null) {
                            popupFirma.dismiss();
                            popupAnterior.dismiss();
                            procesarDatosFinales(sCompletado, problemasList, editText, firmaPath);
                        }
                    });
                }).start();
            } else {
                Toast.makeText(requireContext(), "Por favor, proporcione una firma", Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar el popup de firma
        popupFirma.showAtLocation(requireView(), Gravity.CENTER, 0, 0);
    }
    private void mostrarPopupFirmaInstalacion(PopupWindow popupAnterior) {
        // Inflar el layout del popup de firma
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View popupFirmaView = inflater.inflate(R.layout.dialog_signature, null);

        // Configurar el popup
        PopupWindow popupFirma = new PopupWindow(
                popupFirmaView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupFirma.setOutsideTouchable(false);
        popupFirma.setFocusable(true);

        // Obtener referencias a las vistas
        SignaturePad signaturePad = popupFirmaView.findViewById(R.id.signaturePad);
        Button btnClear = popupFirmaView.findViewById(R.id.btnClear);
        Button btnSave = popupFirmaView.findViewById(R.id.btnSave);

        // Configurar listeners
        btnClear.setOnClickListener(v -> signaturePad.clear());

        btnSave.setOnClickListener(v -> {
            if (!signaturePad.isEmpty()) {
                progressDialog.setMessage("FINALIZANDO TAREA");
                progressDialog.setCancelable(false);
                //mostrarDialogo();
                new Thread(() -> {
                    Bitmap firmaBitmap = signaturePad.getSignatureBitmap();
                    String firmaPath = guardarFirma(firmaBitmap);

                    requireActivity().runOnUiThread(() -> {
                        if (firmaPath != null) {
                            //dataholder.getTarea().
                            popupFirma.dismiss();
                            popupAnterior.dismiss();
                            requireActivity().getSupportFragmentManager().popBackStack();
                            if (getActivity() != null) {
                                getActivity().onBackPressed();
                            }
                        }
                    });
                }).start();
            } else {
                Toast.makeText(requireContext(), "Por favor, proporcione una firma", Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar el popup de firma
        popupFirma.showAtLocation(requireView(), Gravity.CENTER, 0, 0);
    }
    private String guardarFirma(Bitmap firmaBitmap) {
        try {
            // Crear nombre de archivo con timestamp
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Firma_" + timeStamp + ".png";

            // Guardar en el directorio de documentos
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File firmaFile = new File(storageDir, fileName);

            FileOutputStream fos = new FileOutputStream(firmaFile);
            firmaBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            return firmaFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error al guardar la firma", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    //API Y BASE DE DATOS
    private void prueboConexion() {
        MySQLConnection.Conexion(getContext(),
                response -> {
                    // Aquí manejás la respuesta si fue exitosa
                    Log.d("CONEXION", "Respuesta del servidor: " + response);
                    Toast.makeText(getContext(), "Servidor respondió: " + response, Toast.LENGTH_SHORT).show();
                },
                error -> {
                    // Aquí manejás el error si no pudo conectarse
                    Log.e("CONEXION", "Error al conectar con el servidor", error);
                    Toast.makeText(getContext(), "Error de conexión con el servidor" + error, Toast.LENGTH_SHORT).show();
                }
        );
    }

    private void agregarTareaBD() {
        // Validaciones previas
        if (tarea == null || tarea.getCli() == null) {
            Toast.makeText(requireContext(), "Datos del cliente incompletos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar conexión a internet
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(), "No hay conexión a internet", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }




        // Preparar archivos con validación

        String imagen1b64 = null;
        String imagen2b64 = null;
        String firmab64 = null;

        try {
            if (fotos != null && !fotos.isEmpty()) {
                if (fotos.get(0) != null && fotos.get(0).exists()) {
                    imagen1b64 = convertirImagenABase64(fotos.get(0));
                }
                if (fotos.size() > 1 && fotos.get(1) != null && fotos.get(1).exists()) {
                    imagen2b64 = convertirImagenABase64(fotos.get(1));
                }
            }

            // Crear comentario con la imagen embebida

            if (tarea.getRutaFirma() != null && !tarea.getRutaFirma().isEmpty()) {
                firmab64 = convertirImagenABase64(new File(tarea.getRutaFirma()));
            }

        } catch (SecurityException e) {
            Log.e("FILE_ACCESS", "Error accediendo a archivos", e);
            Toast.makeText(requireContext(), "Error accediendo a los archivos", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Validar mínimo requerido para tarea completada
        if (tarea.isCompletada() && (imagen1b64 == null || imagen2b64 == null)) {
            Toast.makeText(requireContext(), "Se requieren 2 fotos para tareas completadas", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
            return;
        }

        // Configurar conexión
        MySQLConnection dbConnection = new MySQLConnection(requireContext());
        dbConnection.setTimeout(30000); // 30 segundos

        // Obtener datos necesarios
        String cliente = tarea.getCli().getNombre();
        String problemas = tarea.getProblema() != null ? tarea.getProblema() : "";
        String comentario = editText.getText() != null ? ""+editText.getText()+". Velocidad medida: "+txtVelocidad.getText() : ""+". Velocidad medida: "+txtVelocidad.getText();
        try {

            // Llamar al método de conexión
            dbConnection.agregarTarea(
                    cliente,
                    problemas,
                    dataholder.getUsuario(), // Ajustar según tu modelo de datos
                    comentario,
                    tarea.isCompletada(),
                    imagen1b64,
                    imagen2b64,
                    firmab64,
                    new MySQLConnection.DatabaseResponseListener() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            progressDialog.dismiss();
                            try {
                                if (response.getBoolean("success")) {
                                    // Procesar respuesta exitosa
                                    JSONObject paths = response.getJSONObject("paths");
                                    Log.d("UPLOAD_SUCCESS", "Imágenes subidas: " + paths.toString());


                                    // Opcional: Guardar IDs o rutas en tu objeto tarea
                                    //tarea.setIdServidor(response.optInt("id_tarea", -1));

                                    Toast.makeText(requireContext(), "Tarea guardada en servidor", Toast.LENGTH_SHORT).show();

                                    dataholder.completoTarea(tarea.getIdReal(), requireActivity());
                                    cargarFotoAComentarioTrello(new File(tarea.getRutaFirma()));
                                    // Limpiar datos después de guardar
                                    fotos.clear();
                                    tarea.setRutaFirma(null);
                                    //generarPDFTarea();
                                    // Cerrar fragmento/actividad si es necesario
                                    requireActivity().getSupportFragmentManager().popBackStack();

                                    if (getActivity() != null) {
                                        getActivity().onBackPressed();
                                    }

                                } else {
                                    String errorMsg = response.optString("message", "Error desconocido");
                                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show();
                                }

                            } catch (JSONException e) {
                                Log.e("JSON_ERROR", "Error parsing response", e);
                                Toast.makeText(requireContext(), "Error al procesar respuesta del servidor", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onError(String error) {
                            progressDialog.dismiss();
                            Log.e("NETWORK_ERROR", "Error: " + error);

                            // Manejar diferentes tipos de errores
                            if (error.contains("TimeoutError")) {
                                Toast.makeText(requireContext(), "Tiempo de espera agotado", Toast.LENGTH_LONG).show();
                            } else if (error.contains("NoConnectionError")) {
                                Toast.makeText(requireContext(), "Error de conexión", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(requireContext(), "Error al subir: " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    }
            );
        }catch (SecurityException e){
            Toast.makeText(requireContext(),"error: "+e,Toast.LENGTH_LONG).show();
        }
    }

    // Método auxiliar para verificar conexión
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private String convertirImagenABase64(File imagenFile) {
        if (imagenFile == null || !imagenFile.exists()) {
            return null;
        }

        try {
            FileInputStream inputStream = new FileInputStream(imagenFile);
            byte[] bytes = new byte[(int) imagenFile.length()];
            inputStream.read(bytes);
            inputStream.close();

            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            Log.e("Base64Error", "Error al convertir imagen a Base64", e);
            return null;
        }
    }

    private void inicializarProgressDialog() {
        if (getContext() == null) return;
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Cargando...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
    }

    private void mostrarDialogo() {
        if (progressDialog != null && !progressDialog.isShowing() && !isRemoving()) {
            progressDialog.show();
        }
    }
    private void cargarFotoAComentarioTrello(File fotoFile) {
        // Verificar que el fragmento aún está adjunto a la actividad
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (fotoFile == null || !fotoFile.exists()) {
            Toast.makeText(requireContext(), "Archivo de foto no válido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que el diálogo no esté ya mostrándose
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        DataHolder.getInstance().cargarImagenComoComentario(
                tarea.getIdReal(),
                fotoFile,
                requireActivity(),
                new APIResponseCallback() {
                    @Override
                    public void onSuccess(ArrayList<TrelloCard> cards) {}

                    @Override
                    public void onSuccess() {
                        if (isAdded() && getContext() != null) {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Foto cargada como comentario", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(VolleyError error) {
                        if (isAdded() && getContext() != null) {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "Error al cargar foto: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onEmptyResponse() {
                        if (isAdded() && getContext() != null) {
                            progressDialog.dismiss();
                            Toast.makeText(requireContext(), "No se recibió respuesta", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void generarPDFTarea() {
        // Crear documento PDF
        PdfDocument document = new PdfDocument();

        // Configurar página
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // Tamaño A4 en puntos (595x842)
        PdfDocument.Page page = document.startPage(pageInfo);

        // Canvas para dibujar
        android.graphics.Canvas canvas = page.getCanvas();

        // Configurar estilos
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(12);
        paint.setColor(android.graphics.Color.BLACK);

        android.graphics.Paint titlePaint = new android.graphics.Paint();
        titlePaint.setTextSize(14);
        titlePaint.setColor(android.graphics.Color.DKGRAY);
        titlePaint.setFakeBoldText(true);

        // Posiciones iniciales
        int margin = 40;
        int yPosition = margin + 50;

        // Título
        canvas.drawText("Reporte de Tarea Técnica", margin, yPosition, titlePaint);
        yPosition += 40;

        // Datos de la tarea
        String[][] datos = {
                {"Cliente", tarea.getCli().getNombre()},
                {"Precinto", String.valueOf(tarea.getCli().getPrecinto())},
                {"Zona", tarea.getCli().getZona().getName()},
                {"Caja", tarea.getCaja()},
                {"Problema", tarea.getProblema()},
                {"Fecha", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())}
        };

        // Dibujar datos
        for (String[] dato : datos) {
            canvas.drawText(dato[0] + ":", margin, yPosition, titlePaint);
            canvas.drawText(dato[1], margin + 120, yPosition, paint);
            yPosition += 30;
        }

        // Agregar firma
        if (tarea.getRutaFirma() != null && !tarea.getRutaFirma().isEmpty()) {
            try {
                android.graphics.Bitmap firmaBitmap = android.graphics.BitmapFactory.decodeFile(tarea.getRutaFirma());

                // Escalar la firma
                int firmaWidth = 200;
                int firmaHeight = 100;
                firmaBitmap = android.graphics.Bitmap.createScaledBitmap(firmaBitmap, firmaWidth, firmaHeight, true);

                yPosition += 50;
                canvas.drawText("Firma del abonado:", margin, yPosition, titlePaint);
                yPosition += 20;
                canvas.drawBitmap(firmaBitmap, margin, yPosition, paint);

            } catch (Exception e) {
                Log.e("PDF_ERROR", "Error al cargar firma: " + e.getMessage());
            }
        }

        document.finishPage(page);

        // Guardar archivo
        File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Reports");
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }

        String fileName = "Reporte_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        File file = new File(downloadsDir, fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(requireContext(), "PDF guardado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Opcional: Abrir el PDF después de crearlo
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);

        } catch (IOException e) {
            Log.e("PDF_ERROR", "Error al guardar PDF: " + e.getMessage());
            Toast.makeText(requireContext(), "Error al generar PDF", Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
    private void probarVelocidad() {
        mostrarDialogo();

        String fileUrl = "https://link.testfile.org/30MB"; // Asegurate que sea accesible

        DownloadSpeedTest.testDownloadSpeed(fileUrl, new DownloadSpeedTest.SpeedTestCallback() {
            @Override
            public void onSuccess(float speedMbps) {
                requireActivity().runOnUiThread(() -> {
                    String speedText = speedMbps % 1 == 0 ?
                            String.format(Locale.getDefault(), "%.0f Mbps", speedMbps) :
                            String.format(Locale.getDefault(), "%.1f Mbps", speedMbps);

                    txtVelocidad.setText(speedText);
                    txtVelocidad.setVisibility(View.VISIBLE);
                    if(imageViewTest.getVisibility() == View.GONE){
                        imageViewTest.setVisibility(View.VISIBLE);
                    }
                    progressDialog.dismiss();
                });
            }

            @Override
            public void onError(String error) {
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    txtVelocidad.setText(error);
                    txtVelocidad.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }


}