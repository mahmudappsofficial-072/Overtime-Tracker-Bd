package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class OvertimeApp : Application() {
    companion object {
        lateinit var instance: OvertimeApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            com.example.receiver.TrashAutoDeleteScheduler.scheduleNextAutoDelete(this)
        } catch (_: Exception) {}
        try {
            FirebaseApp.initializeApp(this)
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        } catch (e: Exception) {
            Log.w("OvertimeApp", "Firebase App Check initialization failed: ${e.message}")
        }
    }
}
