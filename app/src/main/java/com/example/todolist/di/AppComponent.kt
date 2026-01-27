package com.example.todolist.di

import com.example.todolist.data.repository.TodoRepository
import dagger.Component
import javax.inject.Singleton

/**
 * Component Dagger principal de l'application.
 *
 * Assemble tous les modules et expose les dépendances nécessaires
 * pour l'injection dans l'application.
 *
 * Modules inclus :
 * - AppModule : Fournit le Context
 * - DataModule : Fournit XmlTodoService et TodoRepository
 */
@Singleton
@Component(modules = [AppModule::class, DataModule::class])
interface AppComponent {

    /**
     * Expose le TodoRepository pour injection.
     *
     * Utilisé par les Presenters et Components Decompose.
     *
     * @return L'instance singleton du TodoRepository
     */
    fun todoRepository(): TodoRepository

    /**
     * Factory pour créer le AppComponent.
     */
    @Component.Factory
    interface Factory {
        fun create(appModule: AppModule): AppComponent
    }
}
