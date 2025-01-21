package com.example.drivingefficiencyapp

/**
 * Login screen activity for the Driving Efficiency App.
 *
 * This activity displays a login screen with email and password fields for the user to sign in.
 *
 * @author Tim Samoska
 * @since January 20, 2025
 */

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.drivingefficiencyapp.databinding.LoginActivityBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth //Firebase Auth
    private lateinit var binding: LoginActivityBinding //View Binding

    /**
     * Creates the login screen and sets up the login buttons.
     *
     * @param savedInstanceState If it exists, this activity is re-constructed
     * from a previous saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        //Jetpack View Binding
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        //Set up your login button click listener
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            signIn(email, password)
        }

        //Set up registration link
        binding.registerButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

    }

    /**
     * Is run when the user clicks the login button with their email and password.
     * If successful, the user is taken to the main menu. Else, a message is displayed.
     *
     * @param email The email the user entered.
     * @param password The password the user entered.
     */
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    //Sign in success, update UI with the signed in user's information
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                } else {
                    //If sign in fails, display a message to the user
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }
}