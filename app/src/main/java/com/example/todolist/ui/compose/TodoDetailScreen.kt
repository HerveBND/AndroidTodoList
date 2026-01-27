package com.example.todolist.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetpack.subscribeAsState
import com.example.todolist.R
import com.example.todolist.presentation.component.TodoDetailComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Écran de détail d'un todo en Compose.
 *
 * Supporte deux modes :
 * - Mode création : champs vides, pas de statut ni date
 * - Mode édition : champs préremplis, statut et date visibles
 */
@Composable
fun TodoDetailScreen(
    component: TodoDetailComponent,
    modifier: Modifier = Modifier
) {
    val state by component.state.subscribeAsState()
    val isCreationMode = component.isCreationMode

    // États locaux pour les champs éditables
    var title by remember(state.todo?.title) {
        mutableStateOf(state.todo?.title ?: "")
    }
    var description by remember(state.todo?.description) {
        mutableStateOf(state.todo?.description ?: "")
    }
    var isCompleted by remember(state.todo?.isCompleted) {
        mutableStateOf(state.todo?.isCompleted ?: false)
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.error != null && !isCreationMode -> {
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            else -> {
                TodoDetailContent(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    isCompleted = isCompleted,
                    onCompletedChange = { isCompleted = it },
                    isCreationMode = isCreationMode,
                    todo = state.todo,
                    onSave = { component.onSaveTodo(title, description, isCompleted) },
                    onDelete = component::onDeleteTodo
                )
            }
        }
    }
}

/**
 * Contenu de l'écran de détail.
 */
@Composable
private fun TodoDetailContent(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isCompleted: Boolean,
    onCompletedChange: (Boolean) -> Unit,
    isCreationMode: Boolean,
    todo: com.example.todolist.domain.model.Todo?,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Titre (éditable)
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.title_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description (éditable)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.description_hint)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        // Sections visibles uniquement en mode édition
        if (!isCreationMode && todo != null) {
            Spacer(modifier = Modifier.height(24.dp))

            // Statut
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.status_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = onCompletedChange
                        )
                        Text(
                            text = if (isCompleted) {
                                stringResource(R.string.status_completed)
                            } else {
                                stringResource(R.string.status_not_completed)
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date de création
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.created_at_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatDate(todo.createdAt),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Bouton sauvegarder
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.save_button))
        }

        // Bouton supprimer (uniquement en mode édition)
        if (!isCreationMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete_task_button))
            }
        }
    }
}

/**
 * Formate un timestamp en date lisible.
 */
private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("d MMMM yyyy 'à' HH:mm", Locale.FRANCE)
    return dateFormat.format(Date(timestamp))
}
