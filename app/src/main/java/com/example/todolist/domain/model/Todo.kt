package com.example.todolist.domain.model

/**
 * Modèle métier représentant une tâche (todo).
 *
 * @property id UUID v4 unique de la tâche (ex: "550e8400-e29b-41d4-a716-446655440000")
 * @property title Titre de la tâche
 * @property description Description détaillée de la tâche (optionnelle)
 * @property isCompleted État de complétion de la tâche
 * @property createdAt Timestamp de création en millisecondes (epoch)
 */
data class Todo(
    val id: String,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean,
    val createdAt: Long
)
