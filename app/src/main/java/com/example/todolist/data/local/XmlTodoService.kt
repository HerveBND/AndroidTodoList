package com.example.todolist.data.local

import com.example.todolist.domain.model.Todo

/**
 * Service de persistance XML pour les todos.
 *
 * Gère la lecture/écriture de fichiers XML individuels pour chaque todo,
 * ainsi qu'un fichier d'index référençant tous les todos.
 *
 * Architecture :
 * - Un fichier XML par todo : `todos/<timestamp>.xml`
 * - Un fichier d'index : `todos_index.xml`
 */
interface XmlTodoService {

    /**
     * Charge tous les todos valides depuis l'index et leurs fichiers respectifs.
     *
     * Ignore automatiquement les todos marqués comme invalides dans l'index.
     * En cas d'erreur de parsing sur un fichier, le marque comme invalide et continue.
     *
     * @return Result contenant la liste des todos chargés avec succès, ou une erreur
     */
    suspend fun loadAllTodos(): Result<List<Todo>>

    /**
     * Crée un nouveau todo et son fichier XML.
     *
     * Opérations effectuées :
     * - Génère un UUID v4 pour l'ID
     * - Crée le fichier `<timestamp>.xml` dans le dossier todos/
     * - Ajoute une entrée dans l'index avec valid=true
     *
     * @param title Le titre du todo
     * @param description La description du todo (par défaut vide)
     * @param isCompleted L'état initial (par défaut false)
     * @return Result contenant le todo créé avec son UUID, ou une erreur
     */
    suspend fun createTodo(title: String, description: String = "", isCompleted: Boolean = false): Result<Todo>

    /**
     * Met à jour un todo existant.
     *
     * Modifie uniquement le fichier XML correspondant au todo.
     * L'index n'est pas modifié (sauf en cas d'erreur détectée).
     *
     * @param todo Le todo avec les modifications à persister
     * @return Result indiquant le succès ou l'échec de l'opération
     */
    suspend fun updateTodo(todo: Todo): Result<Unit>

    /**
     * Supprime un todo.
     *
     * Opérations effectuées :
     * - Supprime le fichier XML correspondant
     * - Retire l'entrée de l'index
     *
     * @param id L'UUID du todo à supprimer
     * @return Result indiquant le succès ou l'échec de l'opération
     */
    suspend fun deleteTodo(id: String): Result<Unit>

    /**
     * Récupère un todo spécifique par son UUID.
     *
     * @param id L'UUID du todo recherché
     * @return Result contenant le todo trouvé, null si non trouvé/invalide, ou une erreur
     */
    suspend fun getTodoById(id: String): Result<Todo?>

    /**
     * Marque un todo comme invalide dans l'index.
     *
     * Utilisé en cas de corruption de fichier détectée lors du parsing.
     * Le todo reste physiquement sur le disque mais ne sera plus chargé.
     *
     * @param id L'UUID du todo corrompu
     * @return Result indiquant le succès ou l'échec de l'opération
     */
    suspend fun markTodoAsInvalid(id: String): Result<Unit>
}
