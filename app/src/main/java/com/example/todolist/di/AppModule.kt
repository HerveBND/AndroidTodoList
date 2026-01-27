package com.example.todolist.di

import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Module Dagger fournissant les dépendances de base de l'application.
 *
 * Fournit principalement le Context Android nécessaire pour
 * l'accès au système de fichiers et autres ressources Android.
 */
@Module
class AppModule(private val applicationContext: Context) {

    /**
     * Fournit le Context de l'application.
     *
     * @return Le Context Android de l'application
     */
    @Provides
    @Singleton
    fun provideContext(): Context {
        return applicationContext
    }
}
