package com.example.todolist.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.todolist.databinding.FragmentTodoListBinding
import com.example.todolist.presentation.component.TodoListComponent
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment XML pour l'écran de liste des todos.
 *
 * Observe le TodoListComponent et met à jour l'UI en conséquence.
 * Gère l'interaction utilisateur et délègue la logique au component.
 */
class TodoListFragment : Fragment() {

    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!

    private var component: TodoListComponent? = null
    private lateinit var adapter: TodoAdapter
    private var isViewCreated = false
    private var stateObserver: ((com.example.todolist.presentation.TodoListState) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        isViewCreated = true

        // Si le component a déjà été fourni, observer l'état maintenant
        component?.let { observeState() }
    }

    /**
     * Injecte le component depuis l'Activity.
     * L'observation de l'état est différée jusqu'à ce que la vue soit créée.
     */
    fun setComponent(component: TodoListComponent) {
        this.component = component
        // Observer seulement si la vue est déjà créée
        if (isViewCreated) {
            observeState()
        }
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onTodoClick = { todoId ->
                component?.onTodoClick(todoId)
            },
            onToggleClick = { todoId ->
                component?.onToggleTodo(todoId)
            },
            onDeleteClick = { todoId ->
                component?.onDeleteTodo(todoId)
            }
        )

        binding.todosRecyclerView.adapter = adapter
    }

    private fun setupListeners() {
        // FAB pour créer une nouvelle tâche
        binding.addFab.setOnClickListener {
            component?.onCreateNewTodo()
        }
    }

    private fun observeState() {
        val comp = component ?: return
        // Annuler l'ancienne subscription si elle existe
        stateObserver?.let { comp.state.unsubscribe(it) }

        val observer: (com.example.todolist.presentation.TodoListState) -> Unit = { state ->
            // Mettre à jour la liste des todos
            adapter.submitList(state.todos)

            // Afficher/masquer l'état vide
            binding.emptyStateTextView.isVisible = state.todos.isEmpty() && !state.isLoading

            // Afficher/masquer le chargement
            binding.progressBar.isVisible = state.isLoading

            // Afficher les erreurs
            state.error?.let { error ->
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        }
        stateObserver = observer
        comp.state.subscribe(observer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Annuler la subscription pour éviter les NPE après destruction de la vue
        stateObserver?.let { component?.state?.unsubscribe(it) }
        stateObserver = null
        _binding = null
        isViewCreated = false
    }
}
