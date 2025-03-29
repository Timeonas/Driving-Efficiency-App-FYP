package com.example.drivingefficiencyapp.viewLayer.auth

/**
 * Forgot password screen activity for the Driving Efficiency App.
 *
 * This activity allows users to request a password reset email by entering their email address.
 * Uses Firebase Authentication to handle the password reset process.
 *
 * @author Tim Samoska
 * @since January 21, 2025
 */

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.databinding.ForgotPasswordActivityBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth //Firebase Auth
    private lateinit var binding: ForgotPasswordActivityBinding //View Binding

    /**
     * Creates the forgot password screen and sets up the reset password button.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = ForgotPasswordActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        //Set up reset password button click listener
        binding.resetPasswordButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()

            if (validateEmail(email)) {
                sendPasswordResetEmail(email)
            }
        }

        //Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    /**
     * Validates the email input field.
     *
     * @param email The email entered by the user
     * @return Boolean indicating if the email is valid
     */
    private fun validateEmail(email: String): Boolean {
        //Check if email is empty
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            return false
        }
        return true
    }

    /**
     * Sends a password reset email using Firebase Authentication.
     * Shows a success message and finishes the activity if successful,
     * or shows an error message if the email fails to send.
     *
     * @param email The email address to send the reset link to
     */
    private fun sendPasswordResetEmail(email: String) {
        //Send password reset email
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                //Check if email was sent successfully, show message accordingly
                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Password reset email sent. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } else {
                    //If email fails to send, show error message
                    Toast.makeText(
                        this,
                        "Failed to send reset email: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}