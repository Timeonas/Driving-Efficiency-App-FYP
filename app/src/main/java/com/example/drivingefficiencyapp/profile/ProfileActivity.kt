package com.example.drivingefficiencyapp.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.auth.LoginActivity
import com.example.drivingefficiencyapp.databinding.ProfileActivityBinding
import com.example.drivingefficiencyapp.profile.ProfileImageHandler
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

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
            try {
                val imageUrl = profileImageHandler.getCurrentProfileImageUrl()
                if (!isFinishing) {
                    Glide.with(this@ProfileActivity)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_default)
                        .circleCrop()
                        .into(binding.profileImage)
                }
            } catch (e: Exception) {
                // Handle error case
                binding.profileImage.setImageResource(R.drawable.ic_profile_default)
            }
        }
    }
}