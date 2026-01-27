package com.example.todolist.presentation.presenter

import android.util.Log
import com.example.todolist.data.repository.TodoRepository
import com.example.todolist.presentation.TodoListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presenter pour l'écran de liste des todos.
 *
 * Gère la logique métier :
 * - Chargement des todos
 * - Ajout d'un todo
 * - Toggle de l'état de complétion
 * - Suppression d'un todo
 *
 * @property repository Repository pour accéder aux données
 * @property scope CoroutineScope pour les opérations asynchrones
 */
class TodoListPresenter(
    private val repository: TodoRepository,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(TodoListState())
    val state: StateFlow<TodoListState> = _state.asStateFlow()

    companion object {
        private const val TAG = "TodoListPresenter"
    }

    init {
        // Observer les changements dans le repository
        repository.todos
            .onEach { todos ->
                _state.update { it.copy(todos = todos, isLoading = false) }
            }
            .launchIn(scope)

        // Charger les todos au démarrage
        loadTodos()
    }

    /**
     * Charge tous les todos depuis le repository.
     */
    fun loadTodos() {
        _state.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            repository.loadTodos().fold(
                onSuccess = {
                    Log.d(TAG, "Todos loaded successfully")
                    // Forcer isLoading = false (le Flow peut ne pas émettre si liste vide)
                    _state.update { it.copy(isLoading = false) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading todos", error)
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Erreur de chargement: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Ajoute un nouveau todo.
     *
     * @param title Le titre du todo à ajouter
     */
    fun onAddTodo(title: String) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Le titre ne peut pas être vide") }
            return
        }

        scope.launch {
            repository.addTodo(title).fold(
                onSuccess = { newTodo ->
                    Log.d(TAG, "Todo added: ${newTodo.id}")
                    // L'état sera mis à jour via le Flow du repository
                },
                onFailure = { error ->
                    Log.e(TAG, "Error adding todo", error)
                    _state.update {
                        it.copy(error = "Erreur d'ajout: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Toggle l'état de complétion d'un todo.
     *
     * @param id L'UUID du todo
     */
    fun onToggleTodo(id: String) {
        scope.launch {
            repository.toggleTodoCompletion(id).fold(
                onSuccess = {
                    Log.d(TAG, "Todo toggled: $id")
                    // L'état sera mis à jour via le Flow du repository
                },
                onFailure = { error ->
                    Log.e(TAG, "Error toggling todo", error)
                    _state.update {
                        it.copy(error = "Erreur de modification: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Supprime un todo.
     *
     * @param id L'UUID du todo à supprimer
     */
    fun onDeleteTodo(id: String) {
        scope.launch {
            repository.deleteTodo(id).fold(
                onSuccess = {
                    Log.d(TAG, "Todo deleted: $id")
                    // L'état sera mis à jour via le Flow du repository
                },
                onFailure = { error ->
                    Log.e(TAG, "Error deleting todo", error)
                    _state.update {
                        it.copy(error = "Erreur de suppression: ${error.message}")
                    }
                }
            )
        }
    }

    /**
     * Efface le message d'erreur.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
