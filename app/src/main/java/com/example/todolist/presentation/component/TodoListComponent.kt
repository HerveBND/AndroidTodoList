package com.example.todolist.presentation.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.Value
import com.example.todolist.data.repository.TodoRepository
import com.example.todolist.presentation.TodoListState
import com.example.todolist.presentation.presenter.TodoListPresenter
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.decompose.value.MutableValue
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.StateFlow

/**
 * Component Decompose pour l'écran de liste des todos.
 *
 * Expose l'état et les actions disponibles pour l'UI.
 */
interface TodoListComponent {
    /**
     * État observable de la liste des todos.
     */
    val state: Value<TodoListState>

    /**
     * Navigue vers l'écran de création d'un nouveau todo.
     */
    fun onCreateNewTodo()

    /**
     * Toggle l'état de complétion d'un todo.
     *
     * @param id L'UUID du todo
     */
    fun onToggleTodo(id: String)

    /**
     * Supprime un todo depuis la liste.
     *
     * @param id L'UUID du todo à supprimer
     */
    fun onDeleteTodo(id: String)

    /**
     * Navigue vers le détail d'un todo.
     *
     * @param id L'UUID du todo à afficher
     */
    fun onTodoClick(id: String)

    /**
     * Recharge la liste des todos.
     */
    fun onRefresh()
}

/**
 * Implémentation par défaut du TodoListComponent.
 *
 * @property componentContext Context Decompose pour le lifecycle
 * @property repository Repository pour accéder aux données
 * @property onNavigateToDetail Callback pour naviguer vers le détail (null pour création)
 */
class DefaultTodoListComponent(
    componentContext: ComponentContext,
    private val repository: TodoRepository,
    private val onNavigateToDetail: (String?) -> Unit
) : TodoListComponent, ComponentContext by componentContext {

    private val scope = coroutineScope()
    private val presenter = TodoListPresenter(repository, scope)

    private val _state = MutableValue(TodoListState())
    override val state: Value<TodoListState> = _state

    init {
        // Observer le state du presenter et le convertir en Value Decompose
        presenter.state
            .onEach { newState ->
                _state.value = newState
            }
            .launchIn(scope)
    }

    override fun onCreateNewTodo() {
        onNavigateToDetail(null)
    }

    override fun onToggleTodo(id: String) {
        presenter.onToggleTodo(id)
    }

    override fun onDeleteTodo(id: String) {
        presenter.onDeleteTodo(id)
    }

    override fun onTodoClick(id: String) {
        onNavigateToDetail(id)
    }

    override fun onRefresh() {
        presenter.loadTodos()
    }
}
