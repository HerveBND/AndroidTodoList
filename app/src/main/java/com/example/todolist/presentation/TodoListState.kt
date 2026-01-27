package com.example.todolist.presentation

import com.example.todolist.domain.model.Todo

/**
 * État de l'écran de liste des todos.
 *
 * @property todos Liste des todos à afficher
 * @property isLoading Indique si un chargement est en cours
 * @property error Message d'erreur éventuel
 */
data class TodoListState(
    val todos: List<Todo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
