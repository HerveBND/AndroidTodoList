package com.example.todolist

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.essenty.lifecycle.subscribe
import com.example.todolist.databinding.ActivityMainBinding
import com.example.todolist.presentation.component.DefaultRootComponent
import com.example.todolist.presentation.component.RootComponent
import com.example.todolist.ui.xml.TodoDetailFragment
import com.example.todolist.ui.xml.TodoListFragment

/**
 * Activity principale de l'application (version XML/Fragments).
 *
 * Gère :
 * - L'initialisation du RootComponent Decompose
 * - La navigation entre TodoListFragment et TodoDetailFragment
 * - L'observation de la ChildStack pour afficher le bon fragment
 * - Le bouton back
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurer la toolbar
        setSupportActionBar(binding.toolbar)

        // Initialiser le RootComponent avec le repository depuis Dagger
        val app = TodoApplication.from(this)
        rootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            repository = app.appComponent.todoRepository()
        )

        // Observer la stack de navigation
        observeChildStack()

        // Gérer le bouton back
        setupBackPressedHandler()
    }

    /**
     * Observer la ChildStack de Decompose et afficher le fragment approprié.
     */
    private fun observeChildStack() {
        rootComponent.childStack.subscribe { stack ->
            val child = stack.active.instance

            when (child) {
                is RootComponent.Child.ListChild -> {
                    showListFragment(child.component)
                }

                is RootComponent.Child.DetailChild -> {
                    showDetailFragment(child.component)
                }
            }
        }
    }

    /**
     * Affiche le TodoListFragment avec le component injecté.
     */
    private fun showListFragment(component: com.example.todolist.presentation.component.TodoListComponent) {
        // Mettre à jour le titre
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        binding.toolbar.title = getString(R.string.todo_list_title)

        // Créer ou réutiliser le fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (currentFragment is TodoListFragment) {
            // Le fragment liste est déjà affiché, juste réinjecter le component
            currentFragment.setComponent(component)
        } else {
            // Afficher un nouveau fragment liste
            val fragment = TodoListFragment()
            fragment.setComponent(component)

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment, LIST_FRAGMENT_TAG)
                .commit()
        }
    }

    /**
     * Affiche le TodoDetailFragment avec le component injecté.
     */
    private fun showDetailFragment(component: com.example.todolist.presentation.component.TodoDetailComponent) {
        // Mettre à jour le titre avec bouton back
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = if (component.isCreationMode) {
            getString(R.string.new_task_title)
        } else {
            getString(R.string.detail_title)
        }

        // Afficher le fragment (simple replace, pas de backstack)
        // Decompose gère la navigation, pas FragmentManager
        val fragment = TodoDetailFragment()
        fragment.setComponent(component)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, DETAIL_FRAGMENT_TAG)
            .commit()
    }

    private var backCallback: OnBackPressedCallback? = null

    /**
     * Configure le gestionnaire de bouton back pour Decompose.
     */
    private fun setupBackPressedHandler() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // Ce callback n'est actif que quand on est sur l'écran liste (pas de backstack)
                finish()
            }
        }
        onBackPressedDispatcher.addCallback(this, backCallback!!)

        // Observer la stack pour activer/désactiver notre callback
        // Quand il y a un backstack, Decompose gère le back (handleBackButton = true)
        // Quand il n'y a pas de backstack, notre callback termine l'activité
        rootComponent.childStack.subscribe { stack ->
            val hasBackStack = stack.backStack.isNotEmpty()
            backCallback?.isEnabled = !hasBackStack
        }
    }

    /**
     * Gérer le bouton back de la toolbar.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        private const val LIST_FRAGMENT_TAG = "TodoListFragment"
        private const val DETAIL_FRAGMENT_TAG = "TodoDetailFragment"
    }
}
