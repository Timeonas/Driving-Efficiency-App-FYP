package com.example.drivingefficiencyapp

import android.app.Application
import com.example.drivingefficiencyapp.viewLayer.profile.ProfileImageCache
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DrivingEfficiencyApp : Application() {
    // Create an application scope that will be canceled when the application is destroyed
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        // Preload profile image if user is logged in
        FirebaseAuth.getInstance().currentUser?.let {
            applicationScope.launch {
                ProfileImageCache.preloadProfileImage(applicationContext)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Clear any resources when the application is terminated
        ProfileImageCache.clearCache()
    }
}