package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Disabled per user request: No automated meal reminder notifications
    }

    companion object {
        const val CHANNEL_ID = "meal_reminder_channel"

        fun scheduleAllMealReminders(context: Context) {
            // Disabled per user request: Cancel any active alarms
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val requestCodes = listOf(101, 102, 103, 104)
                requestCodes.forEach { code ->
                    val intent = Intent(context, MealReminderReceiver::class.java)
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        code,
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pendingIntent != null) {
                        alarmManager.cancel(pendingIntent)
                    }
                }
            } catch (_: Exception) {}
        }
    }
}
