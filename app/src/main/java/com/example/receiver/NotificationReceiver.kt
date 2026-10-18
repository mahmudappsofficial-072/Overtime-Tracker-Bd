package com.example.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val reminderType = intent?.getIntExtra("REMINDER_TYPE", 1) ?: 1
        ReminderScheduler.createNotificationChannel(context)

        val (title, body, subText, reqCode, openQuickOt) = when (reminderType) {
            1 -> Tuple(
                "🕒 আজকের ওভারটাইম এন্ট্রি করুন এখনই ✅",
                "আজ কত ঘণ্টা ওভারটাইম করেছেন? এখনই এন্ট্রি করে আজকের অতিরিক্ত আয়ের হিসাব দেখে নিন। 💰",
                "দৈনিক ওভারটাইম রিমাইন্ডার • Overtime Tracker BD",
                1001,
                true
            )
            else -> Tuple(
                "🌙 আজকের ডিউটি ও ওটি হিসাব যাচাই করুন",
                "আজকের ডিউটি, ওভারটাইম এন্ট্রি কি সম্পন্ন হয়েছে? মাসিক বেতন নির্ভুল রাখতে এখনই রেকর্ড যাচাই করুন। ✍️",
                "দৈনিক হিসাব যাচাই • Overtime Tracker BD",
                1002,
                false
            )
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (openQuickOt) {
                putExtra("OPEN_QUICK_OT", true)
            } else {
                putExtra("OPEN_HOME", true)
            }
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(context, reqCode, launchIntent, flags)

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText(subText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                NotificationManagerCompat.from(context).notify(reqCode, builder.build())
            } catch (_: Exception) {}
        }

        // Schedule next reminder for the same time tomorrow
        ReminderScheduler.scheduleNextReminder(context, reminderType)
    }

    private data class Tuple(
        val title: String,
        val body: String,
        val subText: String,
        val reqCode: Int,
        val openQuickOt: Boolean
    )
}
