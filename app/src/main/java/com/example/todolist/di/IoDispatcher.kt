package com.example.todolist.di

import javax.inject.Qualifier

/**
 * Qualifier Dagger pour identifier le [kotlinx.coroutines.CoroutineDispatcher]
 * dédié aux opérations d'entrée/sortie (IO).
 *
 * Permet d'injecter [kotlinx.coroutines.Dispatchers.IO] de manière explicite
 * et testable (remplaçable par un TestDispatcher en tests unitaires).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
