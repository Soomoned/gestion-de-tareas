package com.example.gestordetareas.Adapters;

import com.example.gestordetareas.R;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.gestordetareas.Clases.TAREAS;
import java.util.ArrayList;

public class TareasAdapter extends ArrayAdapter<TAREAS> {

    public TareasAdapter(Context context, ArrayList<TAREAS> tareas) {
        super(context, 0, tareas != null ? tareas : new ArrayList<>());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        TAREAS tarea = getItem(position);
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_tarea, parent, false);
        }

        TextView tvNombre = convertView.findViewById(R.id.tvNombre);
        TextView tvPrecinto = convertView.findViewById(R.id.tvPrecinto);
        TextView tvZona = convertView.findViewById(R.id.tvZona);
        TextView tvProblema = convertView.findViewById(R.id.tvProblema);

        Context context = getContext();

        int primaryColor = ContextCompat.getColor(context, R.color.text_primary);
        int secondaryColor = ContextCompat.getColor(context, R.color.text_secondary);

        // Asignar colores adaptativos al texto base
        tvNombre.setTextColor(primaryColor);
        tvPrecinto.setTextColor(secondaryColor);
        tvZona.setTextColor(secondaryColor);

        if (tarea != null && tarea.getCli() != null) {
            tvNombre.setText(tarea.getCli().getNombre() != null ? tarea.getCli().getNombre() : "Sin nombre");
            tvPrecinto.setText("Precinto: " + (tarea.getCli().getPrecinto() > 0 ? tarea.getCli().getPrecinto() : "SN"));
            tvZona.setText("Zona: " + (tarea.getCli().getZona() != null ? tarea.getCli().getZona().getName() : "Sin zona"));
        }

        String problema = tarea.getProblema() != null ? tarea.getProblema() : "Sin problema";
        tvProblema.setText(problema);

        // Cambiar color del texto según el tipo de problema
        switch (problema) {
            case "Sin Servicio":
                tvProblema.setTextColor(Color.RED);
                break;
            case "Servicio Intermitente":
                tvProblema.setTextColor(Color.parseColor("#FFA500")); // Naranja
                break;
            case "Solucionado":
                tvProblema.setTextColor(Color.parseColor("#2E7D32")); // Verde oscuro
                break;
            case "Sin Problema":
                tvProblema.setTextColor(Color.BLACK);
                break;
            default:
                tvProblema.setTextColor(Color.BLACK); // Usa color adaptativo
        }
        return convertView;
    }
}
