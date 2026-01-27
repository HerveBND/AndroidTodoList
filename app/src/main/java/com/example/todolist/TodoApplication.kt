package com.example.todolist

import android.app.Application
import com.example.todolist.di.AppComponent
import com.example.todolist.di.AppModule
import com.example.todolist.di.DaggerAppComponent

/**
 * Classe Application de l'application TodoList.
 *
 * Initialise le graph d'injection de dépendances Dagger
 * et expose le AppComponent pour l'accès global.
 */
class TodoApplication : Application() {

    /**
     * Instance du component Dagger principal.
     * Initialisé dans onCreate() et accessible globalement.
     */
    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialiser le graph Dagger
        appComponent = DaggerAppComponent.factory()
            .create(AppModule(applicationContext))
    }

    companion object {
        /**
         * Récupère l'instance de TodoApplication depuis un Context.
         *
         * @param context Le context Android
         * @return L'instance de TodoApplication
         */
        fun from(context: android.content.Context): TodoApplication {
            return context.applicationContext as TodoApplication
        }
    }
}
