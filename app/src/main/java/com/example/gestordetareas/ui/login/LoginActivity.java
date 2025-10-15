package com.example.gestordetareas.ui.login;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gestordetareas.APIBD.MySQLConnection;
import com.example.gestordetareas.DataHolder;
import com.example.gestordetareas.GENERAL;
import com.example.gestordetareas.R;
import com.example.gestordetareas.databinding.ActivityLoginBinding;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding; // Cambio importante
    private DataHolder dataHolder;
    MySQLConnection dbConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater()); // Inflar con binding
        setContentView(binding.getRoot()); // Usar el root del binding

        dbConnection = new MySQLConnection(this);
        dataHolder = DataHolder.getInstance();
        binding.txtRecuperarContr.setOnClickListener(v->{
            Intent intent = new Intent(LoginActivity.this, GENERAL.class);
            startActivity(intent);
            finish();
        });
        binding.login.setOnClickListener(v -> { // Acceder a las vistas a través del binding
            String usuario = binding.username.getText().toString().trim();
            String clave = binding.password.getText().toString().trim();

            if (usuario.isEmpty() || clave.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.loading.setVisibility(View.VISIBLE);

            dbConnection.loginUsuario(usuario, clave, new MySQLConnection.DatabaseResponseListener() {
                @Override
                public void onSuccess(JSONObject response) {
                    binding.loading.setVisibility(View.GONE);
                    try {
                        boolean success = response.getBoolean("success");
                        if (success) {
                            JSONObject usuarioObj = response.getJSONObject("usuario");
                            String nombre = usuarioObj.getString("nombre_usuario");

                            Toast.makeText(LoginActivity.this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();
                            dataHolder.setUsuario(nombre);
                            Intent intent = new Intent(LoginActivity.this, GENERAL.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String mensaje = response.optString("message", "Credenciales incorrectas");
                            Toast.makeText(LoginActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(LoginActivity.this, "Error al procesar respuesta", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(String error) {
                    binding.loading.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
