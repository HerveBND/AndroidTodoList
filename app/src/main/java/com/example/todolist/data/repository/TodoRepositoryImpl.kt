package com.example.todolist.data.repository

import android.util.Log
import com.example.todolist.data.local.XmlTodoService
import com.example.todolist.domain.model.Todo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation du repository pour les todos.
 *
 * Stratégie :
 * - Maintient un cache en mémoire (MutableStateFlow<List<Todo>>)
 * - Charge tous les todos au démarrage
 * - Chaque opération CRUD met à jour le cache ET persiste immédiatement via XmlTodoService
 *   (simulation d'un push vers un serveur)
 */
@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val xmlTodoService: XmlTodoService
) : TodoRepository {

    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    override val todos: Flow<List<Todo>> = _todos.asStateFlow()

    companion object {
        private const val TAG = "TodoRepository"
    }

    override suspend fun loadTodos(): Result<Unit> {
        return try {
            val result = xmlTodoService.loadAllTodos()
            result.fold(
                onSuccess = { todosList ->
                    _todos.value = todosList
                    Log.d(TAG, "Loaded ${todosList.size} todos into cache")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load todos", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading todos", e)
            Result.failure(e)
        }
    }

    override suspend fun addTodo(title: String, description: String): Result<Todo> {
        return try {
            val result = xmlTodoService.createTodo(title, description)
            result.fold(
                onSuccess = { newTodo ->
                    // Mettre à jour le cache avec le nouveau todo
                    val updatedList = _todos.value + newTodo
                    _todos.value = updatedList
                    Log.d(TAG, "Added todo to cache: ${newTodo.id}")
                    Result.success(newTodo)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to create todo", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding todo", e)
            Result.failure(e)
        }
    }

    override suspend fun updateTodo(todo: Todo): Result<Unit> {
        return try {
            val result = xmlTodoService.updateTodo(todo)
            result.fold(
                onSuccess = {
                    // Mettre à jour le cache
                    val updatedList = _todos.value.map {
                        if (it.id == todo.id) todo else it
                    }
                    _todos.value = updatedList
                    Log.d(TAG, "Updated todo in cache: ${todo.id}")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to update todo", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating todo", e)
            Result.failure(e)
        }
    }

    override suspend fun toggleTodoCompletion(id: String): Result<Unit> {
        return try {
            val currentTodo = _todos.value.find { it.id == id }
                ?: return Result.failure(IllegalArgumentException("Todo not found: $id"))

            val updatedTodo = currentTodo.copy(isCompleted = !currentTodo.isCompleted)
            updateTodo(updatedTodo)
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling todo completion", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTodo(id: String): Result<Unit> {
        return try {
            val result = xmlTodoService.deleteTodo(id)
            result.fold(
                onSuccess = {
                    // Retirer du cache
                    val updatedList = _todos.value.filterNot { it.id == id }
                    _todos.value = updatedList
                    Log.d(TAG, "Deleted todo from cache: $id")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to delete todo", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting todo", e)
            Result.failure(e)
        }
    }

    override suspend fun getTodoById(id: String): Result<Todo?> {
        return try {
            // D'abord chercher dans le cache (plus rapide)
            val cachedTodo = _todos.value.find { it.id == id }
            if (cachedTodo != null) {
                Log.d(TAG, "Found todo in cache: $id")
                return Result.success(cachedTodo)
            }

            // Si pas dans le cache, chercher dans le service
            val result = xmlTodoService.getTodoById(id)
            result.fold(
                onSuccess = { todo ->
                    Log.d(TAG, "Found todo in service: $id")
                    Result.success(todo)
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to get todo by id", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting todo by id", e)
            Result.failure(e)
        }
    }
}
