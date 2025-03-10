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
import com.example.drivingefficiencyapp.ML.DriverClassifierRule
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.ML.DriverClassifierML
import com.example.drivingefficiencyapp.auth.LoginActivity
import com.example.drivingefficiencyapp.databinding.ProfileActivityBinding
import com.example.drivingefficiencyapp.trip.Trip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    private lateinit var classifier: DriverClassifierML
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // Initialize ML classifier with context
        classifier = DriverClassifierML(this)

        auth = FirebaseAuth.getInstance()
        profileImageHandler = ProfileImageHandler(this)

        setupUI()
        loadProfileImage()

        // Call updateDriverProfile when the activity is created
        updateDriverProfile()
    }

    override fun onResume() {
        super.onResume()
        // Refresh driver profile when returning to the activity
        updateDriverProfile()
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

    private fun updateDriverProfile() {
        val userId = auth.currentUser?.uid ?: return

        // Show loading state
        binding.avgEfficiencyScoreText.text = "--"
        binding.driverCategoryText.text = "Loading..."
        binding.driverDescriptionText.text = ""
        binding.driverFeedbackText.text = ""

        firestore.collection("users").document(userId)
            .collection("trips")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                val trips = documents.mapNotNull { it.toObject(Trip::class.java) }

                if (trips.size >= 3) {
                    // Use ML classifier's classify method instead of rule-based classifyDriver
                    val category = classifier.classify(trips)
                    val avgScore = trips.map { it.efficiencyScore }.average().toInt()
                    val lastTrip = trips.maxByOrNull { it.timestamp?.seconds ?: 0 }

                    binding.avgEfficiencyScoreText.text = avgScore.toString()
                    binding.driverCategoryText.text = category.label
                    binding.driverDescriptionText.text = category.description

                    if (lastTrip != null) {
                        // We can still use the personalized feedback from DriverClassifier
                        val ruleBasedClassifier = DriverClassifierRule()
                        binding.driverFeedbackText.text =
                            ruleBasedClassifier.getPersonalizedFeedback(category, lastTrip)
                    }
                } else {
                    // Not enough trips
                    binding.avgEfficiencyScoreText.text = "--"
                    binding.driverCategoryText.text = getString(R.string.not_classified)
                    binding.driverDescriptionText.text = getString(R.string.need_more_trips)
                    binding.driverFeedbackText.text = ""
                }
            }
            .addOnFailureListener {
                // Error handling
                binding.driverCategoryText.text = "Error loading trips"
                binding.driverDescriptionText.text = ""
            }
    }
}