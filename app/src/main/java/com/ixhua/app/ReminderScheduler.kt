
package com.ixhua.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ixhua.app.data.Task

object ReminderScheduler {

    private const val CHANNEL_ID = "ixhua_task_reminders"
    private const val CHANNEL_NAME = "Recordatorios de tareas"
    private const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_TASK_TITLE = "task_title"

    fun schedule(context: Context, task: Task) {
        cancel(context, task.id)

        val reminderTime = task.reminderAt ?: return

        if (task.completed || reminderTime <= System.currentTimeMillis()) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.ixhua.app.TASK_REMINDER.${task.id}"
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTime,
            pendingIntent
        )
    }

    fun cancel(context: Context, taskId: Long) {
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = "com.ixhua.app.TASK_REMINDER.$taskId"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos programados para tus tareas de Ixhua"
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    internal fun showNotification(
        context: Context,
        taskId: Long,
        title: String
    ) {
        createNotificationChannel(context)

        val notificationIntent = Intent(
            context,
            MainActivity::class.java
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("¡Es hora de tu tarea!")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Recuerda completar: $title")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        try {
            NotificationManagerCompat
                .from(context)
                .notify(taskId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Android puede bloquear la notificación si falta el permiso.
        }
    }
}

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        val title = intent.getStringExtra("task_title") ?: "Tarea pendiente"

        if (taskId == -1L) return

        ReminderScheduler.showNotification(
            context,
            taskId,
            title
        )
    }
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val tasks = com.ixhua.app.data.TaskStorage.loadTasks(context)

        tasks.forEach { task ->
            if (!task.completed && task.reminderAt != null) {
                ReminderScheduler.schedule(context, task)
            }
        }
    }
}
