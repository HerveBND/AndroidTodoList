package com.example.todolist.presentation.presenter

import android.util.Log
import com.example.todolist.data.repository.TodoRepository
import com.example.todolist.presentation.TodoDetailState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presenter pour l'écran de détail d'un todo.
 *
 * Gère la logique métier :
 * - Chargement du todo spécifique (mode édition)
 * - Création d'un nouveau todo (mode création)
 * - Toggle de l'état de complétion
 * - Suppression du todo (déclenchera la navigation retour via le component)
 *
 * @property todoId L'UUID du todo à afficher (null pour mode création)
 * @property repository Repository pour accéder aux données
 * @property scope CoroutineScope pour les opérations asynchrones
 * @property onTodoDeleted Callback appelé après suppression réussie
 * @property onTodoCreated Callback appelé après création réussie avec l'ID du nouveau todo
 * @property onTodoUpdated Callback appelé après mise à jour réussie
 */
class TodoDetailPresenter(
    private val todoId: String?,
    private val repository: TodoRepository,
    private val scope: CoroutineScope,
    private val onTodoDeleted: () -> Unit,
    private val onTodoCreated: (String) -> Unit = {},
    private val onTodoUpdated: () -> Unit = {}
) {
    private val _state = MutableStateFlow(TodoDetailState())
    val state: StateFlow<TodoDetailState> = _state.asStateFlow()

    val isCreationMode: Boolean = todoId == null

    companion object {
        private const val TAG = "TodoDetailPresenter"
    }

    init {
        if (!isCreationMode && todoId != null) {
            // Mode édition : observer les changements dans le repository
            repository.todos
                .onEach { todos ->
                    val todo = todos.find { it.id == todoId }
                    if (todo != null) {
                        _state.update { it.copy(todo = todo, isLoading = false) }
                    }
                }
                .launchIn(scope)

            // Charger le todo au démarrage
            loadTodo()
        } else {
            // Mode création : pas de chargement
            _state.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Charge le todo depuis le repository.
     */
    private fun loadTodo() {
        if (todoId == null) return

        _state.update { it.copy(isLoading = true, error = null) }

        scope.launch {
            repository.getTodoById(todoId).fold(
                onSuccess = { todo ->
                    if (todo != null) {
                        Log.d(TAG, "Todo loaded: ${todo.id}")
                        _state.update { it.copy(todo = todo, isLoading = false) }
                    } else {
                        Log.w(TAG, "Todo not found: $todoId")
                        _state.update {
                            it.copy(
                                todo = null,
                                isLoading = false,
                                error = "Tâche non trouvée"
                            )
                        }
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Error loading todo", error)
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
     * Sauvegarde le todo (création ou mise à jour).
     */
    fun onSaveTodo(title: String, description: String, isCompleted: Boolean = false) {
        if (title.isBlank()) {
            _state.update { it.copy(error = "Le titre ne peut pas être vide") }
            return
        }

        scope.launch {
            if (isCreationMode) {
                // Création
                repository.addTodo(title, description).fold(
                    onSuccess = { newTodo ->
                        Log.d(TAG, "Todo created: ${newTodo.id}")
                        onTodoCreated(newTodo.id)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error creating todo", error)
                        _state.update {
                            it.copy(error = "Erreur de création: ${error.message}")
                        }
                    }
                )
            } else {
                // Mise à jour (inclut title, description et isCompleted)
                val currentTodo = _state.value.todo ?: return@launch
                val updatedTodo = currentTodo.copy(
                    title = title,
                    description = description,
                    isCompleted = isCompleted
                )
                repository.updateTodo(updatedTodo).fold(
                    onSuccess = {
                        Log.d(TAG, "Todo updated: ${updatedTodo.id}")
                        onTodoUpdated()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error updating todo", error)
                        _state.update {
                            it.copy(error = "Erreur de mise à jour: ${error.message}")
                        }
                    }
                )
            }
        }
    }

    /**
     * Toggle l'état de complétion du todo.
     */
    fun onToggleTodo() {
        val id = todoId ?: return
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
     * Supprime le todo et déclenche la navigation retour.
     */
    fun onDeleteTodo() {
        val id = todoId ?: return
        scope.launch {
            repository.deleteTodo(id).fold(
                onSuccess = {
                    Log.d(TAG, "Todo deleted: $id")
                    // Déclencher la navigation retour
                    onTodoDeleted()
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
