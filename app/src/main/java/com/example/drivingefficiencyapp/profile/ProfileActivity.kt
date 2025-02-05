package com.example.drivingefficiencyapp.profile

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.auth.LoginActivity
import com.example.drivingefficiencyapp.databinding.ProfileActivityBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

/**
 * User profile activity for the Driving Efficiency App.
 *
 * This activity displays the user's email and a button to sign out.
 *
 * @author Tim Samoska
 * @since January 20, 2025
 */

class ProfileActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ProfileActivityBinding
    private lateinit var profileImageHandler: ProfileImageHandler
    private val storage = FirebaseStorage.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        profileImageHandler = ProfileImageHandler(this)

        setupUI()
        loadProfileImage()
    }

    private fun setupUI() {
        // Display user email
        auth.currentUser?.let { user ->
            binding.emailText.text = user.email
        }

        // Setup profile image click
        binding.profileImage.setOnClickListener {
            pickProfileImage()
        }

        // Setup change photo button
        binding.changePhotoButton.setOnClickListener {
            pickProfileImage()
        }

        // Setup sign out
        binding.signOutButton.setOnClickListener {
            // Clear the image cache before signing out
            ProfileImageCache.clearCache()
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun pickProfileImage() {
        profileImageHandler.pickImage { isLoading, _ ->
            binding.profileImageProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                loadProfileImage()
            }
        }
    }

    private fun loadProfileImage() {
        lifecycleScope.launch {
            val cachedImage = ProfileImageCache.getCachedImage()
            if (cachedImage != null) {
                binding.profileImage.setImageDrawable(cachedImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.ic_profile_default)
            }

            val hasProfileImage = try {
                val userId = auth.currentUser?.uid ?: return@launch
                val imageRef = storage.reference.child("$userId.jpg")
                imageRef.metadata.await()
                true
            } catch (e: Exception) {
                false
            }

            if (!hasProfileImage) {
                return@launch
            }

            try {
                val imageUrl = profileImageHandler.getCurrentProfileImageUrl()

                if (!isFinishing) {
                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                lifecycleScope.launch {
                                    ProfileImageCache.clearCache()
                                    ProfileImageCache.preloadProfileImage(this@ProfileActivity)
                                }
                                return false
                            }
                        })
                        .into(binding.profileImage)
                }
            } catch (e: Exception) {
                binding.profileImage.setImageResource(R.drawable.ic_profile_default)
            }
        }
    }
}