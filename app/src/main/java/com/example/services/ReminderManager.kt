package com.example.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.data.model.ReminderModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.*

class ReminderManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("reminders_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, ReminderModel::class.java)
    private val adapter = moshi.adapter<List<ReminderModel>>(listType)

    init {
        createNotificationChannel()
    }

    fun getReminders(): List<ReminderModel> {
        val json = prefs.getString("reminders_list", null) ?: return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReminders(reminders: List<ReminderModel>) {
        val json = adapter.toJson(reminders)
        prefs.edit().putString("reminders_list", json).apply()
    }

    fun addReminder(reminder: ReminderModel) {
        val list = getReminders().toMutableList()
        list.add(reminder)
        saveReminders(list)
        scheduleAlarm(reminder)
    }

    fun deleteReminder(id: String) {
        val list = getReminders().filter { it.id != id }
        saveReminders(list)
        cancelAlarm(id)
    }

    fun toggleReminder(id: String, enabled: Boolean) {
        val list = getReminders().map {
            if (it.id == id) it.copy(enabled = enabled) else it
        }
        saveReminders(list)
        val updated = list.find { it.id == id }
        if (updated != null) {
            if (enabled) scheduleAlarm(updated) else cancelAlarm(updated.id)
        }
    }

    private fun scheduleAlarm(reminder: ReminderModel) {
        if (!reminder.enabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", reminder.title)
            putExtra("id", reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, reminder.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback if exact alarm requires user toggle
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } catch (ignored: Exception) {}
        }
    }

    private fun cancelAlarm(id: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "bill_reminders",
                "PowerPulse Bill & Utility Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority heads-up alerts for utility bill due dates"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
