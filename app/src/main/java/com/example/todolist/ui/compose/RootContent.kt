package com.example.todolist.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arkivanov.decompose.extensions.compose.jetpack.stack.Children
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.example.todolist.R
import com.example.todolist.presentation.component.RootComponent

/**
 * Contenu racine de l'application Compose.
 *
 * Gère la navigation entre TodoListScreen et TodoDetailScreen
 * en utilisant Decompose ChildStack.
 *
 * Avantage vs XML : Pas de FragmentManager, pas de transactions,
 * Decompose gère tout de façon type-safe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootContent(
    component: RootComponent,
    modifier: Modifier = Modifier
) {
    val childStack by component.childStack.subscribeAsState()
    val activeChild = childStack.active.instance

    // Déterminer si on affiche le bouton back
    val showBackButton = activeChild is RootComponent.Child.DetailChild

    // Déterminer le titre
    val title = when (val child = activeChild) {
        is RootComponent.Child.ListChild -> stringResource(R.string.todo_list_title)
        is RootComponent.Child.DetailChild -> {
            if (child.component.isCreationMode) {
                stringResource(R.string.new_task_title)
            } else {
                stringResource(R.string.detail_title)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = {
                                (activeChild as? RootComponent.Child.DetailChild)
                                    ?.component
                                    ?.onBack()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Children(
            stack = childStack,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            animation = stackAnimation(fade() + scale())
        ) { child ->
            when (val instance = child.instance) {
                is RootComponent.Child.ListChild -> {
                    TodoListScreen(
                        component = instance.component,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is RootComponent.Child.DetailChild -> {
                    TodoDetailScreen(
                        component = instance.component,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
