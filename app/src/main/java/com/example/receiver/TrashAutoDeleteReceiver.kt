package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TrashAutoDeleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = com.example.data.repository.OvertimeRepository(db, context)
                repo.purgeExpiredTrash()
            } catch (_: Exception) {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val cutoff = System.currentTimeMillis() - TrashAutoDeleteScheduler.TWENTY_FOUR_HOURS_MILLIS
                    db.trashDao().purgeExpiredTrash(cutoff)
                    TrashAutoDeleteScheduler.scheduleNextAutoDelete(context)
                } catch (_: Exception) {}
            } finally {
                pendingResult.finish()
            }
        }
    }
}
