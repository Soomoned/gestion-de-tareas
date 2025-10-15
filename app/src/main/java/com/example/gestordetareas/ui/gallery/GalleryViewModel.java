package com.example.gestordetareas.ui.gallery;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class GalleryViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public GalleryViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Registro" +
                "* Se dejan adjuntados mas de un problema.\n" +
                "* Se hicieron mejoras a la actualizacion de la lista.\n"+
                "* Se hicieron mejoras en la vista de las tareas.\n"+
                "* Se hicieron mejoras en la vista general.\n"+
                "* Se pueden agregar varios Problemas."+
                "* Opcion para elegir si una tarea se completo satisfactoriamente o no.\n\n"+
                "* Se puede ver la ubicacion en tiempo real y la del cliente.\n(Ubicacion cliente de prureba)\n\n"+
                "A futuro:\n"+
                "* Que se pueda dejar adjuntada una foto\n" +
                "* Que se actualice la lista de tareas si una se le quita o pone el tilde de completada \n"+
                "* Implementar base de datos"+
                "* Agregar opciones para ver los comentarios" +
                "* Mejorar la vista en la pantalla de finalizacion"
                );
    }

    public LiveData<String> getText() {
        return mText;
    }
}