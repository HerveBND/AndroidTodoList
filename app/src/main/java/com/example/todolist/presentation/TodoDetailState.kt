package com.example.todolist.presentation

import com.example.todolist.domain.model.Todo

/**
 * État de l'écran de détail d'un todo.
 *
 * @property todo Le todo affiché (null si non trouvé ou en cours de chargement)
 * @property isLoading Indique si un chargement est en cours
 * @property error Message d'erreur éventuel
 */
data class TodoDetailState(
    val todo: Todo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
