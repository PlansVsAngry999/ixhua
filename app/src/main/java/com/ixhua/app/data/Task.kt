
package com.ixhua.app.data

data class Task(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val category: String = "PERSONAL",
    val priority: String = "MEDIA",
    val dueDate: String? = null,

    // Fecha y hora independiente del recordatorio
    val reminderAt: Long? = null,

    val completed: Boolean = false,
    val xpAwarded: Boolean = false,

    // Lista a la que pertenece la tarea
    val listId: String = "default"
)
