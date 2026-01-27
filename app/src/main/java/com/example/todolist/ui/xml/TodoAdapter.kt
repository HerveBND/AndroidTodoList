package com.example.todolist.ui.xml

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTodoBinding
import com.example.todolist.domain.model.Todo

/**
 * Adapter pour afficher la liste des todos dans un RecyclerView.
 *
 * Utilise ListAdapter avec DiffUtil pour des mises à jour efficaces.
 * Gère les callbacks pour les actions sur les todos.
 *
 * @property onTodoClick Callback appelé lors du clic sur un todo (navigation vers détail)
 * @property onToggleClick Callback appelé lors du clic sur la checkbox
 * @property onDeleteClick Callback appelé lors du clic sur le bouton supprimer
 */
class TodoAdapter(
    private val onTodoClick: (String) -> Unit,
    private val onToggleClick: (String) -> Unit,
    private val onDeleteClick: (String) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder pour un item todo.
     */
    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) {
            binding.apply {
                // Afficher le titre
                todoTitleTextView.text = todo.title

                // Gérer l'état de complétion
                todoCheckBox.isChecked = todo.isCompleted

                // Style du texte selon l'état
                if (todo.isCompleted) {
                    todoTitleTextView.paintFlags =
                        todoTitleTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    todoTitleTextView.alpha = 0.6f
                } else {
                    todoTitleTextView.paintFlags =
                        todoTitleTextView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    todoTitleTextView.alpha = 1.0f
                }

                // Listener pour le clic sur la checkbox
                todoCheckBox.setOnClickListener {
                    onToggleClick(todo.id)
                }

                // Listener pour le clic sur le titre (navigation vers détail)
                todoTitleTextView.setOnClickListener {
                    onTodoClick(todo.id)
                }

                // Listener pour le bouton supprimer
                deleteButton.setOnClickListener {
                    onDeleteClick(todo.id)
                }
            }
        }
    }

    /**
     * DiffUtil Callback pour calculer les différences entre deux listes de todos.
     */
    private class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
        override fun areItemsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Todo, newItem: Todo): Boolean {
            return oldItem == newItem
        }
    }
}
