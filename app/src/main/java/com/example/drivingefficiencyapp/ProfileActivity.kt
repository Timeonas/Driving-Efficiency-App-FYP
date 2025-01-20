package com.example.drivingefficiencyapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.databinding.ProfileActivityBinding
import com.google.firebase.auth.FirebaseAuth

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

    /**
     * Creates the profile screen and sets up the sign out button.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ProfileActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance() //Get the current instance of Firebase Auth

        //Display user email
        auth.currentUser?.let { user ->
            binding.emailText.text = user.email
        }

        //Set up sign out button
        binding.signOutButton.setOnClickListener {
            auth.signOut() //Firebase signout
            //Go to login screen
            val intent = Intent(this, LoginActivity::class.java)
            //Clear the back stack so user can't go back after signing out
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}