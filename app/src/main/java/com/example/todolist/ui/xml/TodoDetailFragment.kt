package com.example.todolist.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.todolist.R
import com.example.todolist.databinding.FragmentTodoDetailBinding
import com.example.todolist.presentation.component.TodoDetailComponent
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment XML pour l'écran de détail d'un todo.
 *
 * Supporte deux modes :
 * - Mode création : champs titre et description vides, pas de statut ni date
 * - Mode édition : champs préremplis, statut et date visibles
 */
class TodoDetailFragment : Fragment() {

    private var _binding: FragmentTodoDetailBinding? = null
    private val binding get() = _binding!!

    private var component: TodoDetailComponent? = null
    private var isViewCreated = false
    private var isInitialLoad = true
    private var stateObserver: ((com.example.todolist.presentation.TodoDetailState) -> Unit)? = null

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        isViewCreated = true

        // Si le component a déjà été fourni, configurer l'UI
        component?.let {
            setupUI(it.isCreationMode)
            observeState()
        }
    }

    /**
     * Injecte le component depuis l'Activity.
     */
    fun setComponent(component: TodoDetailComponent) {
        this.component = component
        if (isViewCreated) {
            setupUI(component.isCreationMode)
            observeState()
        }
    }

    /**
     * Configure l'UI selon le mode (création ou édition).
     */
    private fun setupUI(isCreationMode: Boolean) {
        // En mode création, masquer les sections spécifiques à l'édition
        binding.statusSection.isVisible = !isCreationMode
        binding.createdAtSection.isVisible = !isCreationMode
        binding.deleteButton.isVisible = !isCreationMode
    }

    private fun setupListeners() {
        // Bouton sauvegarder
        binding.saveButton.setOnClickListener {
            val title = binding.titleEditText.text?.toString() ?: ""
            val description = binding.descriptionEditText.text?.toString() ?: ""
            val isCompleted = binding.statusCheckBox.isChecked
            component?.onSaveTodo(title, description, isCompleted)
        }

        // Bouton supprimer
        binding.deleteButton.setOnClickListener {
            component?.onDeleteTodo()
        }

        // Checkbox : mettre à jour le texte quand l'utilisateur change l'état
        // (la valeur est sauvegardée avec le bouton Save, pas immédiatement)
        binding.statusCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.statusCheckBox.text = if (isChecked) {
                getString(R.string.status_completed)
            } else {
                getString(R.string.status_not_completed)
            }
        }
    }

    private fun observeState() {
        val comp = component ?: return
        // Annuler l'ancienne subscription si elle existe
        stateObserver?.let { comp.state.unsubscribe(it) }

        val observer: (com.example.todolist.presentation.TodoDetailState) -> Unit = { state ->
            state.todo?.let { todo ->
                // Remplir les champs seulement au chargement initial
                // (pour ne pas écraser les modifications de l'utilisateur)
                if (isInitialLoad) {
                    binding.titleEditText.setText(todo.title)
                    binding.descriptionEditText.setText(todo.description)
                    binding.statusCheckBox.isChecked = todo.isCompleted
                    isInitialLoad = false
                }

                // Mettre à jour le texte de la checkbox selon son état actuel
                binding.statusCheckBox.text = if (binding.statusCheckBox.isChecked) {
                    getString(R.string.status_completed)
                } else {
                    getString(R.string.status_not_completed)
                }

                // Afficher la date de création
                val createdDate = Date(todo.createdAt)
                binding.createdAtTextView.text = dateFormat.format(createdDate)
            }

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
        isInitialLoad = true
    }
}
