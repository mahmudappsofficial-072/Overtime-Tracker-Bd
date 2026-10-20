package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object TrashAutoDeleteScheduler {
    private const val REQUEST_CODE_TRASH_AUTO_DELETE = 2001
    const val TWENTY_FOUR_HOURS_MILLIS = 86400000L // 24 hours in milliseconds

    fun scheduleNextAutoDelete(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(appContext)
                val earliestDeletedAt = db.trashDao().getEarliestDeletedTimestamp()
                val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return@launch
                val intent = Intent(appContext, TrashAutoDeleteReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
                val pendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_CODE_TRASH_AUTO_DELETE, intent, flags)

                if (earliestDeletedAt == null) {
                    // No items left in recycle bin, cancel any scheduled background delete alarm
                    alarmManager.cancel(pendingIntent)
                    return@launch
                }

                val now = System.currentTimeMillis()
                val targetTriggerTime = earliestDeletedAt + TWENTY_FOUR_HOURS_MILLIS

                if (targetTriggerTime <= now) {
                    // Item already reached or passed 24 hours: purge immediately and check next
                    val cutoff = now - TWENTY_FOUR_HOURS_MILLIS
                    db.trashDao().purgeExpiredTrash(cutoff)
                    val nextEarliest = db.trashDao().getEarliestDeletedTimestamp()
                    if (nextEarliest == null) {
                        alarmManager.cancel(pendingIntent)
                        return@launch
                    }
                    val nextTriggerTime = nextEarliest + TWENTY_FOUR_HOURS_MILLIS
                    setExactAlarm(alarmManager, nextTriggerTime, pendingIntent)
                } else {
                    setExactAlarm(alarmManager, targetTriggerTime, pendingIntent)
                }
            } catch (_: Exception) {}
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }
}
