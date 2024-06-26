package com.example.taskmanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class SignInActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonSignIn: Button
    private lateinit var buttonSignUp: Button

    private lateinit var auth: FirebaseAuth

    private fun signUpUserToFirestore(username: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val signUpDate = Calendar.getInstance().time // Get the current date
            val friendcode = generateRandomCode(5) // Generate a random 5-character code
            val db = FirebaseFirestore.getInstance()
            val userData = hashMapOf(
                "userId" to userId,
                "username" to username,
                "signUpDate" to signUpDate, // Add sign-up date to user data
                "completedTasks" to 0, // Initialize completed tasks counter to 0
                "unlockedBadges" to ArrayList<String>(),
                "friendcode" to friendcode, // Add friendcode to user data
                "friendslist" to ArrayList<String>() // Initialize friendslist as empty
                // Add more user data as needed
            )
            db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot added with ID: $userId")
                    // User registration and data storage successful
                    // Proceed with any additional UI updates or actions
                }
                .addOnFailureListener { e ->
                    // Handle error storing user data
                    Log.e(TAG, "Error adding document", e)
                }
        }
    }

    private fun generateRandomCode(length: Int): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        editTextUsername = findViewById(R.id.editTextUsername)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonSignIn = findViewById(R.id.buttonSignIn)
        buttonSignUp = findViewById(R.id.buttonSignUp)

        auth = FirebaseAuth.getInstance()

        buttonSignIn.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword("$username@placeholder.com", password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = FirebaseAuth.getInstance().currentUser
                            user?.let {
                                val userId = user.uid
                                val db = FirebaseFirestore.getInstance()

                                db.collection("users").document(userId).get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            val friendcode = document.getString("friendcode")
                                            val friendslist = document.get("friendslist") as? ArrayList<String>

                                            val intent = Intent(this, MainActivity::class.java).apply {
                                                putExtra("USERNAME", username)
                                                putExtra("FRIENDCODE", friendcode)
                                                putStringArrayListExtra("FRIENDSLIST", friendslist)
                                            }
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            Toast.makeText(baseContext, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSignUp.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword("$username@placeholder.com", password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign up success, add user data to Firestore
                            signUpUserToFirestore(username)
                            Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                        } else {
                            // If sign up fails, display a message to the user.
                            val errorMessage = task.exception?.message ?: "Sign-up failed. Please try again."
                            Toast.makeText(baseContext, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
