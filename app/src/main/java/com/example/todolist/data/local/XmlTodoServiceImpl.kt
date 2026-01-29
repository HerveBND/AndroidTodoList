package com.example.todolist.data.local

import android.content.Context
import android.util.Log
import android.util.Xml
import com.example.todolist.di.IoDispatcher
import com.example.todolist.domain.model.Todo
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implémentation du service de persistance XML pour les todos.
 *
 * Thread-safe grâce à l'utilisation d'un Mutex.
 * Gère un fichier d'index et des fichiers individuels par todo.
 *
 * @property context Context Android pour accéder au système de fichiers
 * @property ioDispatcher Dispatcher dédié aux opérations IO, injectable pour les tests
 */
@Singleton
class XmlTodoServiceImpl @Inject constructor(
    private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : XmlTodoService {

    private val mutex = Mutex()
    private val todosDir: File by lazy {
        File(context.filesDir, "todos").also { it.mkdirs() }
    }
    private val indexFile: File by lazy {
        File(context.filesDir, "todos_index.xml")
    }

    companion object {
        private const val TAG = "XmlTodoService"

        // Tags XML pour l'index
        private const val TAG_INDEX = "index"
        private const val TAG_TODO_REF = "todo-ref"
        private const val TAG_ID = "id"
        private const val TAG_FILENAME = "filename"
        private const val TAG_VALID = "valid"

        // Tags XML pour un todo
        private const val TAG_TODO = "todo"
        private const val TAG_TITLE = "title"
        private const val TAG_DESCRIPTION = "description"
        private const val TAG_IS_COMPLETED = "isCompleted"
        private const val TAG_CREATED_AT = "createdAt"
    }

    /**
     * Classe représentant une référence dans l'index
     */
    private data class TodoRef(
        val id: String,
        val filename: String,
        val valid: Boolean
    )

    override suspend fun loadAllTodos(): Result<List<Todo>> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                if (!indexFile.exists()) {
                    Log.d(TAG, "Index file does not exist, returning empty list")
                    return@withContext Result.success(emptyList())
                }

                val todoRefs = parseIndex()
                val todos = mutableListOf<Todo>()

                for (ref in todoRefs) {
                    if (!ref.valid) {
                        Log.d(TAG, "Skipping invalid todo: ${ref.id}")
                        continue
                    }

                    val todoFile = File(todosDir, ref.filename)
                    if (!todoFile.exists()) {
                        Log.w(TAG, "Todo file not found: ${ref.filename}, marking as invalid")
                        markTodoAsInvalidInternal(ref.id)
                        continue
                    }

                    try {
                        val todo = parseTodoFile(todoFile)
                        todos.add(todo)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing todo file: ${ref.filename}", e)
                        markTodoAsInvalidInternal(ref.id)
                    }
                }

                Log.d(TAG, "Loaded ${todos.size} todos")
                Result.success(todos)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading todos", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun createTodo(title: String, description: String, isCompleted: Boolean): Result<Todo> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                val id = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val filename = "$timestamp.xml"
                val todo = Todo(
                    id = id,
                    title = title,
                    description = description,
                    isCompleted = isCompleted,
                    createdAt = timestamp
                )

                // Créer le fichier du todo
                val todoFile = File(todosDir, filename)
                writeTodoFile(todoFile, todo)

                // Mettre à jour l'index
                val todoRefs = if (indexFile.exists()) parseIndex().toMutableList() else mutableListOf()
                todoRefs.add(TodoRef(id, filename, valid = true))
                writeIndex(todoRefs)

                Log.d(TAG, "Created todo: $id")
                Result.success(todo)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating todo", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateTodo(todo: Todo): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                // Trouver le fichier correspondant dans l'index
                if (!indexFile.exists()) {
                    return@withContext Result.failure(IOException("Index file not found"))
                }

                val todoRefs = parseIndex()
                val ref = todoRefs.find { it.id == todo.id }
                    ?: return@withContext Result.failure(IOException("Todo not found in index: ${todo.id}"))

                if (!ref.valid) {
                    return@withContext Result.failure(IOException("Todo is marked as invalid: ${todo.id}"))
                }

                val todoFile = File(todosDir, ref.filename)
                if (!todoFile.exists()) {
                    Log.w(TAG, "Todo file not found during update: ${ref.filename}")
                    markTodoAsInvalidInternal(todo.id)
                    return@withContext Result.failure(IOException("Todo file not found"))
                }

                writeTodoFile(todoFile, todo)
                Log.d(TAG, "Updated todo: ${todo.id}")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating todo", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteTodo(id: String): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                if (!indexFile.exists()) {
                    return@withContext Result.failure(IOException("Index file not found"))
                }

                val todoRefs = parseIndex().toMutableList()
                val ref = todoRefs.find { it.id == id }
                    ?: return@withContext Result.failure(IOException("Todo not found in index: $id"))

                // Supprimer le fichier
                val todoFile = File(todosDir, ref.filename)
                if (todoFile.exists()) {
                    todoFile.delete()
                }

                // Retirer de l'index
                todoRefs.removeIf { it.id == id }
                writeIndex(todoRefs)

                Log.d(TAG, "Deleted todo: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting todo", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getTodoById(id: String): Result<Todo?> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                if (!indexFile.exists()) {
                    return@withContext Result.success(null)
                }

                val todoRefs = parseIndex()
                val ref = todoRefs.find { it.id == id } ?: return@withContext Result.success(null)

                if (!ref.valid) {
                    return@withContext Result.success(null)
                }

                val todoFile = File(todosDir, ref.filename)
                if (!todoFile.exists()) {
                    markTodoAsInvalidInternal(id)
                    return@withContext Result.success(null)
                }

                val todo = parseTodoFile(todoFile)
                Result.success(todo)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting todo by id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun markTodoAsInvalid(id: String): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                markTodoAsInvalidInternal(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking todo as invalid", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Version interne (non-suspend) pour marquer comme invalide.
     * Appelée depuis des contextes déjà synchronisés.
     */
    private fun markTodoAsInvalidInternal(id: String) {
        if (!indexFile.exists()) return

        val todoRefs = parseIndex().map {
            if (it.id == id) it.copy(valid = false) else it
        }
        writeIndex(todoRefs)
        Log.d(TAG, "Marked todo as invalid: $id")
    }

    /**
     * Parse le fichier d'index et retourne la liste des références.
     */
    private fun parseIndex(): List<TodoRef> {
        val refs = mutableListOf<TodoRef>()

        FileInputStream(indexFile).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var eventType = parser.eventType
            var currentId: String? = null
            var currentFilename: String? = null
            var currentValid: Boolean? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            TAG_ID -> currentId = parser.nextText()
                            TAG_FILENAME -> currentFilename = parser.nextText()
                            TAG_VALID -> currentValid = parser.nextText().toBoolean()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == TAG_TODO_REF) {
                            if (currentId != null && currentFilename != null && currentValid != null) {
                                refs.add(TodoRef(currentId, currentFilename, currentValid))
                            }
                            currentId = null
                            currentFilename = null
                            currentValid = null
                        }
                    }
                }
                eventType = parser.next()
            }
        }

        return refs
    }

    /**
     * Écrit le fichier d'index avec la liste des références.
     */
    private fun writeIndex(refs: List<TodoRef>) {
        FileOutputStream(indexFile).use { outputStream ->
            val serializer = Xml.newSerializer()
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag(null, TAG_INDEX)

            for (ref in refs) {
                serializer.startTag(null, TAG_TODO_REF)

                serializer.startTag(null, TAG_ID)
                serializer.text(ref.id)
                serializer.endTag(null, TAG_ID)

                serializer.startTag(null, TAG_FILENAME)
                serializer.text(ref.filename)
                serializer.endTag(null, TAG_FILENAME)

                serializer.startTag(null, TAG_VALID)
                serializer.text(ref.valid.toString())
                serializer.endTag(null, TAG_VALID)

                serializer.endTag(null, TAG_TODO_REF)
            }

            serializer.endTag(null, TAG_INDEX)
            serializer.endDocument()
        }
    }

    /**
     * Parse un fichier todo et retourne l'objet Todo.
     */
    private fun parseTodoFile(file: File): Todo {
        FileInputStream(file).use { inputStream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(inputStream, null)

            var id: String? = null
            var title: String? = null
            var description: String = ""
            var isCompleted: Boolean? = null
            var createdAt: Long? = null

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            TAG_ID -> id = parser.nextText()
                            TAG_TITLE -> title = parser.nextText()
                            TAG_DESCRIPTION -> description = parser.nextText()
                            TAG_IS_COMPLETED -> isCompleted = parser.nextText().toBoolean()
                            TAG_CREATED_AT -> createdAt = parser.nextText().toLong()
                        }
                    }
                }
                eventType = parser.next()
            }

            if (id == null || title == null || isCompleted == null || createdAt == null) {
                throw XmlPullParserException("Incomplete todo data in file: ${file.name}")
            }

            return Todo(id, title, description, isCompleted, createdAt)
        }
    }

    /**
     * Écrit un fichier todo.
     */
    private fun writeTodoFile(file: File, todo: Todo) {
        FileOutputStream(file).use { outputStream ->
            val serializer = Xml.newSerializer()
            serializer.setOutput(outputStream, "UTF-8")
            serializer.startDocument("UTF-8", true)
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)

            serializer.startTag(null, TAG_TODO)

            serializer.startTag(null, TAG_ID)
            serializer.text(todo.id)
            serializer.endTag(null, TAG_ID)

            serializer.startTag(null, TAG_TITLE)
            serializer.text(todo.title)
            serializer.endTag(null, TAG_TITLE)

            serializer.startTag(null, TAG_DESCRIPTION)
            serializer.text(todo.description)
            serializer.endTag(null, TAG_DESCRIPTION)

            serializer.startTag(null, TAG_IS_COMPLETED)
            serializer.text(todo.isCompleted.toString())
            serializer.endTag(null, TAG_IS_COMPLETED)

            serializer.startTag(null, TAG_CREATED_AT)
            serializer.text(todo.createdAt.toString())
            serializer.endTag(null, TAG_CREATED_AT)

            serializer.endTag(null, TAG_TODO)
            serializer.endDocument()
        }
    }
}
