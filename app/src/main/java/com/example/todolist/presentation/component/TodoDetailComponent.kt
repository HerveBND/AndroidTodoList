package com.example.todolist.presentation.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.example.todolist.data.repository.TodoRepository
import com.example.todolist.presentation.TodoDetailState
import com.example.todolist.presentation.presenter.TodoDetailPresenter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Component Decompose pour l'écran de détail d'un todo.
 *
 * Expose l'état et les actions disponibles pour l'UI.
 * Supporte deux modes : affichage/édition d'un todo existant, ou création d'un nouveau.
 */
interface TodoDetailComponent {
    /**
     * État observable du détail du todo.
     */
    val state: Value<TodoDetailState>

    /**
     * Indique si on est en mode création (true) ou édition (false).
     */
    val isCreationMode: Boolean

    /**
     * Sauvegarde le todo (création ou mise à jour).
     */
    fun onSaveTodo(title: String, description: String, isCompleted: Boolean = false)

    /**
     * Supprime le todo et déclenche la navigation retour.
     */
    fun onDeleteTodo()

    /**
     * Retour vers la liste.
     */
    fun onBack()

    /**
     * Toggle l'état de complétion du todo.
     */
    fun onToggleTodo()
}

/**
 * Implémentation par défaut du TodoDetailComponent.
 *
 * @property componentContext Context Decompose pour le lifecycle
 * @property todoId L'UUID du todo à afficher (null pour création)
 * @property repository Repository pour accéder aux données
 * @property onNavigateBack Callback pour retourner à la liste
 * @property onTodoCreated Callback appelé après création d'un nouveau todo
 */
class DefaultTodoDetailComponent(
    componentContext: ComponentContext,
    private val todoId: String?,
    private val repository: TodoRepository,
    private val onNavigateBack: () -> Unit,
    private val onTodoCreated: (String) -> Unit = {}
) : TodoDetailComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()

    override val isCreationMode: Boolean = todoId == null

    private val presenter = TodoDetailPresenter(
        todoId = todoId,
        repository = repository,
        scope = scope,
        onTodoDeleted = {
            // Après suppression réussie, retour à la liste
            onNavigateBack()
        },
        onTodoCreated = onTodoCreated,
        onTodoUpdated = {
            // Après mise à jour réussie, retour à la liste
            onNavigateBack()
        }
    )

    private val _state = MutableValue(TodoDetailState())
    override val state: Value<TodoDetailState> = _state

    init {
        // Observer le state du presenter et le convertir en Value Decompose
        presenter.state
            .onEach { newState ->
                _state.value = newState
            }
            .launchIn(scope)
    }

    override fun onSaveTodo(title: String, description: String, isCompleted: Boolean) {
        presenter.onSaveTodo(title, description, isCompleted)
    }

    override fun onDeleteTodo() {
        presenter.onDeleteTodo()
    }

    override fun onBack() {
        onNavigateBack()
    }

    override fun onToggleTodo() {
        presenter.onToggleTodo()
    }
}
