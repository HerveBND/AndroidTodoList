package com.example.todolist.data.repository

import com.example.todolist.domain.model.Todo
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstrait pour la gestion des todos.
 *
 * Fournit une interface de haut niveau pour les opérations CRUD sur les todos,
 * en exposant un Flow pour observer les changements en temps réel.
 */
interface TodoRepository {

    /**
     * Flow observable de la liste des todos.
     * Émet une nouvelle liste à chaque modification (création, mise à jour, suppression).
     */
    val todos: Flow<List<Todo>>

    /**
     * Charge tous les todos depuis la source de données.
     * Initialise le cache interne et émet la liste dans le Flow.
     *
     * @return Result indiquant le succès ou l'échec du chargement initial
     */
    suspend fun loadTodos(): Result<Unit>

    /**
     * Ajoute un nouveau todo.
     *
     * Met à jour le cache local et persiste immédiatement via le service.
     * Émet la nouvelle liste dans le Flow.
     *
     * @param title Le titre du nouveau todo
     * @param description La description du todo (optionnelle)
     * @return Result contenant le todo créé, ou une erreur
     */
    suspend fun addTodo(title: String, description: String = ""): Result<Todo>

    /**
     * Met à jour un todo existant (changement de titre ou d'état).
     *
     * Met à jour le cache local et persiste immédiatement via le service.
     * Émet la nouvelle liste dans le Flow.
     *
     * @param todo Le todo avec les modifications
     * @return Result indiquant le succès ou l'échec
     */
    suspend fun updateTodo(todo: Todo): Result<Unit>

    /**
     * Bascule l'état de complétion d'un todo.
     *
     * @param id L'UUID du todo
     * @return Result indiquant le succès ou l'échec
     */
    suspend fun toggleTodoCompletion(id: String): Result<Unit>

    /**
     * Supprime un todo.
     *
     * Retire du cache local et persiste immédiatement via le service.
     * Émet la nouvelle liste dans le Flow.
     *
     * @param id L'UUID du todo à supprimer
     * @return Result indiquant le succès ou l'échec
     */
    suspend fun deleteTodo(id: String): Result<Unit>

    /**
     * Récupère un todo spécifique par son UUID.
     *
     * @param id L'UUID du todo recherché
     * @return Result contenant le todo ou null si non trouvé
     */
    suspend fun getTodoById(id: String): Result<Todo?>
}
