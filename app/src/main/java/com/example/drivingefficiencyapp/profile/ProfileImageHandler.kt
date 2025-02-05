package com.example.drivingefficiencyapp.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class ProfileImageHandler(private val activity: AppCompatActivity) {
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var pickMedia: ActivityResultLauncher<PickVisualMediaRequest>? = null
    private var onImageSelectedCallback: ((Boolean, String?) -> Unit)? = null

    init {
        setupImagePicker()
    }

    private fun setupImagePicker() {
        pickMedia = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let {
                uploadImage(it)
            } ?: run {
                onImageSelectedCallback?.invoke(false, "No image selected")
            }
        }
    }

    fun pickImage(callback: (Boolean, String?) -> Unit) {
        onImageSelectedCallback = callback
        pickMedia?.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun uploadImage(imageUri: Uri) {
        onImageSelectedCallback?.invoke(true, null) // Start loading

        val userId = auth.currentUser?.uid ?: run {
            onImageSelectedCallback?.invoke(false, "User not logged in")
            return
        }

        // Debug log
        activity.runOnUiThread {
            Toast.makeText(activity, "User ID: $userId", Toast.LENGTH_SHORT).show()
        }

        // Simplified path
        val imageRef = storage.reference.child("$userId.jpg")

        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        updateProfilePicture(downloadUri)
                    }
                    .addOnFailureListener { exception ->
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Download URL error: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                        onImageSelectedCallback?.invoke(false, "Failed to get download URL: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                activity.runOnUiThread {
                    Toast.makeText(activity, "Upload error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
                onImageSelectedCallback?.invoke(false, "Failed to upload image: ${exception.message}")
            }
    }

    private fun updateProfilePicture(downloadUri: Uri) {
        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            .setPhotoUri(downloadUri)
            .build()

        auth.currentUser?.updateProfile(profileUpdates)
            ?.addOnSuccessListener {
                // Clear the cache so it will be updated with the new image
                ProfileImageCache.clearCache()
                onImageSelectedCallback?.invoke(false, null)
            }
            ?.addOnFailureListener { exception ->
                onImageSelectedCallback?.invoke(false, "Failed to update profile: ${exception.message}")
            }
    }

    suspend fun getCurrentProfileImageUrl(): Uri? {
        try {
            val userId = auth.currentUser?.uid ?: return null
            val imageRef = storage.reference.child("$userId.jpg")

            // First check if the file exists
            return try {
                val metadata = imageRef.metadata.await()
                if (metadata != null) {
                    // File exists, get download URL
                    imageRef.downloadUrl.await()
                } else {
                    null
                }
            } catch (e: Exception) {
                // File doesn't exist or other error
                null
            }
        } catch (e: Exception) {
            activity.runOnUiThread {
                Toast.makeText(activity, "Error getting image: ${e.message}", Toast.LENGTH_LONG).show()
            }
            return null
        }
    }
}