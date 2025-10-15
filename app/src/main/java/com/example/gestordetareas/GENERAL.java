package com.example.gestordetareas;

import static android.app.ProgressDialog.show;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gestordetareas.APITrello.APIHelper;
import com.example.gestordetareas.Adapters.TareasAdapter;
import com.example.gestordetareas.Clases.TAREAS;
import com.example.gestordetareas.ui.home.HomeFragment;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gestordetareas.databinding.ActivityGeneralBinding;

import java.util.ArrayList;

public class GENERAL extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityGeneralBinding binding;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGeneralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarGeneral.toolbar);
        // En la clase GENERAL, modifica el onClickListener del FAB
        binding.appBarGeneral.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Mostrar Snackbar de carga
                Snackbar snackbar = Snackbar.make(view, "Actualizando tareas...", Snackbar.LENGTH_INDEFINITE);
                snackbar.setAnchorView(R.id.fab);
                snackbar.show();

                // Obtener el NavHostFragment
                Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_general);

                if (navHostFragment != null) {
                    // Obtener el fragmento actual dentro del NavHostFragment
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);

                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).actualizarTareas(new HomeFragment.UpdateCallback() {
                            @Override
                            public void onUpdateComplete(int nuevasTareas) {
                                snackbar.dismiss();
                                String message = nuevasTareas > 0 ?
                                        "Se agregaron " + nuevasTareas + " tareas nuevas" :
                                        "No hay tareas nuevas";
                                Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
                                        .setAnchorView(R.id.fab)
                                        .show();
                            }
                            @Override
                            public void onUpdateFailed() {
                                snackbar.dismiss();
                                Snackbar.make(view, "Error al actualizar tareas", Snackbar.LENGTH_SHORT)
                                        .setAnchorView(R.id.fab)
                                        .show();
                            }
                        });
                    } else {
                        snackbar.dismiss();
                        Snackbar.make(view, "Actualización solo disponible en la vista principal", Snackbar.LENGTH_SHORT)
                                .setAnchorView(R.id.fab)
                                .show();
                    }
                } else {
                    snackbar.dismiss();
                    Snackbar.make(view, "Error al acceder a la vista principal", Snackbar.LENGTH_SHORT)
                            .setAnchorView(R.id.fab)
                            .show();
                }

            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_gallery,
                R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_general);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.g_e_n_e_r_a_l, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_general);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


}