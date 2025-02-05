package com.example.drivingefficiencyapp.profile

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object ProfileImageCache {
    private var cachedProfileImage: Drawable? = null

    suspend fun preloadProfileImage(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext
                val imageRef = FirebaseStorage.getInstance().reference.child("$userId.jpg")
                val imageUrl = imageRef.downloadUrl.await()

                // Preload and cache the image using Glide
                cachedProfileImage = Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .submit()
                    .get()
            } catch (e: Exception) {
                //Handle error silently
            }
        }
    }

    fun getCachedImage(): Drawable? = cachedProfileImage

    fun clearCache() {
        cachedProfileImage = null
    }
}