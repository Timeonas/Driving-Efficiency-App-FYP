package com.example.drivingefficiencyapp.auth

/**
 * Registration screen activity for the Driving Efficiency App.
 *
 * This activity allows new users to create an account using email and password,
 * which is then registered with Firebase Authentication.
 *
 * @author Tim Samoska
 * @since January 21, 2025
 */

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.ui.MainMenuActivity
import com.example.drivingefficiencyapp.databinding.RegisterActivityBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: RegisterActivityBinding

    /**
     * Creates the registration screen and sets up the registration form.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        binding = RegisterActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        //Set up registration button click listener
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (validateForm(email, password, confirmPassword)) {
                createAccount(email, password)
            }
        }

        //Set up link to login screen
        binding.backToLoginButton.setOnClickListener {
            finish() //Return to login activity
        }
    }

    /**
     * Validates the registration form input fields.
     *
     * @param email The email entered by the user
     * @param password The password entered by the user
     * @param confirmPassword The confirmation password entered by the user
     * @return Boolean indicating if the form is valid
     */
    private fun validateForm(email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        //Check if email is empty
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            isValid = false
        }

        //Check if password is empty
        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            isValid = false
        }

        //Check if confirm password is empty
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordEditText.error = "Confirm password is required"
            isValid = false
        }

        //Check if passwords match
        if (password != confirmPassword) {
            binding.confirmPasswordEditText.error = "Passwords do not match"
            isValid = false
        }

        //Check password length
        if (password.length < 6) {
            binding.passwordEditText.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    /**
     * Creates a new user account using Firebase Authentication.
     *
     * @param email The email to register
     * @param password The password to register
     */
    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //Registration success
                    Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                    //Navigate to main menu
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    //Clear activity stack
                    finishAffinity()
                } else {
                    //Registration failed
                    val message = when (task.exception) {
                        is FirebaseAuthWeakPasswordException -> "Password is too weak"
                        is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                        is FirebaseAuthUserCollisionException -> "Email already in use"
                        else -> "Registration failed"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
    }
}