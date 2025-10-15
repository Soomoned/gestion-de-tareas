package com.example.gestordetareas.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.gestordetareas.APIBD.MySQLConnection;
import com.example.gestordetareas.APITrello.APIHelper;
import com.example.gestordetareas.Adapters.TareasAdapter;
import com.example.gestordetareas.Clases.TAREAS;
import com.example.gestordetareas.DataHolder;
import com.example.gestordetareas.R;
import com.example.gestordetareas.databinding.FragmentHomeBinding;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    //DE ACTIVITY MAIN
    private static final String API_KEY = "49d0f879ef7021fcb38d3711f5da23b7";
    private static final String API_TOKEN = "ATTA5d4ebd227fa5ff8c3ef1a2382b5bc10600e90686af25b7b7579d607a15d62e8b2C35804A";
    private DataHolder dataHolder;
    private APIHelper api;
    //DE ACTIVITY MAIN

    private ArrayList<TAREAS> tareas = new ArrayList<>();
    private TareasAdapter tareasAdp;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Inicializar componentes
        dataHolder = DataHolder.getInstance();

        // Inicializar el adaptador
        tareasAdp = new TareasAdapter(requireContext(), tareas);

        // Configurar el ListView
        binding.lvMain.setAdapter(tareasAdp);

        binding.lvMain.setVisibility(View.INVISIBLE);

        binding.pBar.setVisibility(View.INVISIBLE);
        //conseguirString();

        if(!dataHolder.estaCargado()){
            try{
                api = new APIHelper(API_KEY, API_TOKEN, binding.pBar, requireActivity());
                cargarTareas();
                inicializoListView();
            } catch (Exception e) {
                Log.e("HomeFragment", "Fallo inicializando datos: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Error al iniciar carga de datos", Toast.LENGTH_LONG).show();
            }

        } else {inicializoListView();}

       binding.lvMain.setOnItemClickListener((parent, view, position, id) -> {
            TAREAS tareaSeleccionada = tareasAdp.getItem(position);
            if (tareaSeleccionada != null) {
                dataHolder.setTarea(tareaSeleccionada);

                // Navegar usando Navigation Component
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.nav_tarea);
            }
        });
        //homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void inicializoListView() {
        try {
            tareas.clear();
            tareas.addAll(dataHolder.getTareasPendientes());
            tareasAdp.notifyDataSetChanged();
            binding.lvMain.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error en la carga de datos", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    private void cargoDatos() {
        // Usa el ProgressBar del binding, no una variable no inicializada
        binding.pBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            dataHolder.cargoDatosTrello(api, "PENDIENTES", requireActivity(), new DataHolder.DataLoadCallback() {
                @Override
                public void onDataLoaded() {
                    requireActivity().runOnUiThread(() -> {
                        if (dataHolder.getTareas() != null) {

                            inicializoListView();
                        }
                        binding.pBar.setVisibility(View.GONE); // Usar binding aquí también
                    });
                }

                @Override
                public void onDataLoadFailed() {
                    requireActivity().runOnUiThread(() -> {
                        binding.pBar.setVisibility(View.GONE);
                        Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }).start();
    }

    private void cargarTareas() {
        mostrarCargando(true);

        dataHolder.obtenerListaTareas(new DataHolder.TareasCallback() {
            @Override
            public void onExito(ArrayList<TAREAS> tareas) {
                requireActivity().runOnUiThread(() -> {
                    mostrarCargando(false);

                    inicializoListView();
                    mostrarMensaje("Datos cargados correctamente" + dataHolder.getTareas().size());
                });
            }

            @Override
            public void onFallo(String mensajeError) {
                requireActivity().runOnUiThread(() -> {
                    mostrarCargando(false);
                    mostrarError(mensajeError);
                    Log.e("HomeFragment API", "Fallo inicializando datos: " + mensajeError);
                });
            }
        });
    }

    private void mostrarCargando(boolean mostrar) {
        binding.pBar.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        binding.lvMain.setVisibility(mostrar ? View.GONE : View.VISIBLE);
    }

    private void mostrarMensaje(String mensaje) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show();
    }

    private void mostrarError(String error) {
        Toast.makeText(requireContext(), "Error: " + error, Toast.LENGTH_LONG).show();
    }
    // Agrega esta interfaz dentro de la clase HomeFragment
    public interface UpdateCallback {
        void onUpdateComplete(int nuevasTareas);
        void onUpdateFailed();
    }

    // Agrega este método para manejar la actualización
    public void actualizarTareas(UpdateCallback callback) {
        binding.pBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            dataHolder.actualizarTareas("PENDIENTES", requireActivity(), new DataHolder.DataLoadCallback() {
                @Override
                public void onDataLoaded() {
                    requireActivity().runOnUiThread(() -> {
                        int nuevasTareas = dataHolder.getTareas().size() - tareas.size();
                        tareas.clear();
                        tareas.addAll(dataHolder.getTareasPendientes());
                        tareasAdp.notifyDataSetChanged();
                        binding.pBar.setVisibility(View.GONE);
                        callback.onUpdateComplete(nuevasTareas);

                    });
                }

                @Override
                public void onDataLoadFailed() {
                    requireActivity().runOnUiThread(() -> {
                        binding.pBar.setVisibility(View.GONE);
                        callback.onUpdateFailed();
                    });
                }
            });
        }).start();
    }

    public void conseguirString(){
        MySQLConnection.obtenerTareasComoString("https://netlatin.dnatech.com.ar/admin/tickets.rpt.php", new MySQLConnection.CallbackTareasString() {
            @Override
            public void onSuccess(String tareasJsonString) {
                Log.d("TareasString", tareasJsonString);
                Toast.makeText(requireContext(),tareasJsonString,Toast.LENGTH_LONG).show();
                // O lo podés mostrar en un TextView
                // textView.setText(tareasJsonString);
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("TareasError", errorMessage);
                Toast.makeText(requireContext(),errorMessage,Toast.LENGTH_LONG).show();
            }
        });

    }
}