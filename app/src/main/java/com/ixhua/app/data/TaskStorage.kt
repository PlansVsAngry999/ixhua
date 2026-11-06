
package com.ixhua.app.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object TaskStorage {

    private const val TAG = "IxhuaStorage"
    private const val PREFS_NAME = "ixhua_storage"

    private const val TASKS_KEY = "saved_tasks"
    private const val LISTS_KEY = "saved_lists"
    private const val TOTAL_XP_KEY = "total_xp"

    private fun getPreferences(context: Context) =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    // =========================
    // GUARDAR TAREAS
    // =========================

    fun saveTasks(
        context: Context,
        tasks: List<Task>
    ): Boolean {
        return try {
            val jsonArray = JSONArray()

            tasks.forEach { task ->
                val jsonObject = JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("category", task.category)
                    put("priority", task.priority)
                    put("dueDate", task.dueDate ?: JSONObject.NULL)
                    put("reminderAt", task.reminderAt ?: JSONObject.NULL)
                    put("completed", task.completed)
                    put("xpAwarded", task.xpAwarded)
                    put("listId", task.listId)
                }

                jsonArray.put(jsonObject)
            }

            val result = getPreferences(context)
                .edit()
                .putString(TASKS_KEY, jsonArray.toString())
                .commit()

            if (!result) {
                Log.e(TAG, "No se pudieron guardar las tareas")
            } else {
                Log.d(TAG, "Tareas guardadas: ${tasks.size}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar tareas", e)
            false
        }
    }

    // =========================
    // RECUPERAR TAREAS
    // =========================

    fun loadTasks(context: Context): List<Task> {
        return try {
            val savedData = getPreferences(context)
                .getString(TASKS_KEY, null)

            if (savedData.isNullOrBlank()) {
                Log.d(TAG, "No existen tareas guardadas")
                return emptyList()
            }

            val jsonArray = JSONArray(savedData)
            val tasks = mutableListOf<Task>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                val dueDate = if (
                    item.isNull("dueDate")
                ) {
                    null
                } else {
                    item.optString("dueDate")
                }

                val reminderAt = if (
                    item.isNull("reminderAt") ||
                    !item.has("reminderAt")
                ) {
                    null
                } else {
                    item.optLong("reminderAt")
                }

                tasks.add(
                    Task(
                        id = item.optLong(
                            "id",
                            System.currentTimeMillis() + i
                        ),
                        title = item.optString("title"),
                        category = item.optString(
                            "category",
                            "PERSONAL"
                        ),
                        priority = item.optString(
                            "priority",
                            "MEDIA"
                        ),
                        dueDate = dueDate,
                        reminderAt = reminderAt,
                        completed = item.optBoolean(
                            "completed",
                            false
                        ),
                        xpAwarded = item.optBoolean(
                            "xpAwarded",
                            false
                        ),
                        listId = item.optString(
                            "listId",
                            "default"
                        )
                    )
                )
            }

            Log.d(TAG, "Tareas recuperadas: ${tasks.size}")
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error al recuperar tareas", e)
            emptyList()
        }
    }

    // =========================
    // GUARDAR LISTAS
    // =========================

    fun saveLists(
        context: Context,
        lists: List<ListItem>
    ): Boolean {
        return try {
            val jsonArray = JSONArray()

            lists.forEach { list ->
                val jsonObject = JSONObject().apply {
                    put("id", list.id)
                    put("name", list.name)
                    put("color", list.color)
                    put("createdAt", list.createdAt)
                }

                jsonArray.put(jsonObject)
            }

            val result = getPreferences(context)
                .edit()
                .putString(LISTS_KEY, jsonArray.toString())
                .commit()

            if (!result) {
                Log.e(TAG, "No se pudieron guardar las listas")
            } else {
                Log.d(TAG, "Listas guardadas: ${lists.size}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar listas", e)
            false
        }
    }

    // =========================
    // RECUPERAR LISTAS
    // =========================

    fun loadLists(context: Context): List<ListItem> {
        return try {
            val savedData = getPreferences(context)
                .getString(LISTS_KEY, null)

            if (savedData.isNullOrBlank()) {
                return emptyList()
            }

            val jsonArray = JSONArray(savedData)
            val lists = mutableListOf<ListItem>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)

                lists.add(
                    ListItem(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        color = item.optString(
                            "color",
                            "PURPLE"
                        ),
                        createdAt = item.optLong(
                            "createdAt",
                            System.currentTimeMillis()
                        )
                    )
                )
            }

            Log.d(TAG, "Listas recuperadas: ${lists.size}")
            lists
        } catch (e: Exception) {
            Log.e(TAG, "Error al recuperar listas", e)
            emptyList()
        }
    }

    // =========================
    // EXPERIENCIA (XP)
    // =========================

    fun saveTotalXp(
        context: Context,
        totalXp: Int
    ): Boolean {
        return try {
            getPreferences(context)
                .edit()
                .putInt(TOTAL_XP_KEY, totalXp.coerceAtLeast(0))
                .commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar XP", e)
            false
        }
    }

    fun loadTotalXp(context: Context): Int {
        return try {
            getPreferences(context)
                .getInt(TOTAL_XP_KEY, 0)
                .coerceAtLeast(0)
        } catch (e: Exception) {
            Log.e(TAG, "Error al recuperar XP", e)
            0
        }
    }

    // =========================
    // ELIMINAR DATOS
    // =========================

    fun clearTasks(context: Context): Boolean {
        return getPreferences(context)
            .edit()
            .remove(TASKS_KEY)
            .commit()
    }

    fun clearLists(context: Context): Boolean {
        return getPreferences(context)
            .edit()
            .remove(LISTS_KEY)
            .commit()
    }
}
