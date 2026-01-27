package com.example.todolist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.defaultComponentContext
import com.example.todolist.presentation.component.DefaultRootComponent
import com.example.todolist.ui.compose.RootContent
import com.example.todolist.ui.theme.TodoListTheme

/**
 * Activity Compose de l'application.
 *
 * Version alternative utilisant Jetpack Compose au lieu des Fragments XML.
 *
 * Comparaison avec MainActivity (version XML) :
 * - Plus simple : pas de FragmentManager, pas de ViewBinding
 * - Pas de observation manuelle de la ChildStack
 * - setContent() remplace setContentView() + inflate
 * - Le thème est appliqué via Composable, pas via XML
 */
class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialiser le RootComponent avec le repository depuis Dagger
        val app = TodoApplication.from(this)
        val rootComponent = DefaultRootComponent(
            componentContext = defaultComponentContext(),
            repository = app.appComponent.todoRepository()
        )

        setContent {
            TodoListTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootContent(
                        component = rootComponent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
