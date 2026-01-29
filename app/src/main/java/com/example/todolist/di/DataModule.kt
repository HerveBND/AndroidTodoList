package com.example.todolist.di

import com.example.todolist.data.local.XmlTodoService
import com.example.todolist.data.local.XmlTodoServiceImpl
import com.example.todolist.data.repository.TodoRepository
import com.example.todolist.data.repository.TodoRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Module Dagger fournissant les dépendances de la couche Data.
 *
 * Lie les interfaces à leurs implémentations :
 * - XmlTodoService → XmlTodoServiceImpl
 * - TodoRepository → TodoRepositoryImpl
 */
@Module
abstract class DataModule {

    companion object {
        /**
         * Fournit le [CoroutineDispatcher] dédié aux opérations IO.
         *
         * Injecté via le qualifier [@IoDispatcher], il peut être remplacé
         * par un TestDispatcher dans les tests unitaires.
         */
        @Provides
        @Singleton
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
    }

    /**
     * Lie l'interface XmlTodoService à son implémentation.
     *
     * @param impl L'implémentation concrète (injectée automatiquement)
     * @return L'interface XmlTodoService
     */
    @Binds
    @Singleton
    abstract fun bindXmlTodoService(impl: XmlTodoServiceImpl): XmlTodoService

    /**
     * Lie l'interface TodoRepository à son implémentation.
     *
     * @param impl L'implémentation concrète (injectée automatiquement)
     * @return L'interface TodoRepository
     */
    @Binds
    @Singleton
    abstract fun bindTodoRepository(impl: TodoRepositoryImpl): TodoRepository
}
