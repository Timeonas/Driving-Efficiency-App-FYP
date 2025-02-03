// DrivingEfficiencyApp.kt
package com.example.drivingefficiencyapp

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

class DrivingEfficiencyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase Auth early
        FirebaseAuth.getInstance()

        // Configure Firestore with persistent cache
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(104857600) // 100MB cache size
                    .build()
            )
            .build()

        FirebaseFirestore.getInstance().firestoreSettings = settings
    }
}