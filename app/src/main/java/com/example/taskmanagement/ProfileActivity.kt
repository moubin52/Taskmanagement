package com.example.taskmanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var textViewWelcome: TextView
    private lateinit var taskCountTextView: TextView
    private lateinit var textAccountDate: TextView
    private lateinit var textCompletedTasks: TextView
    private lateinit var recyclerViewOwnedBadges: RecyclerView
    private lateinit var badgesAdapter: BadgeAdapter
    private lateinit var buttonDeleteAccount: Button
    private val badgesList = mutableListOf<Badge>()
    private var tasksListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val welcomeMessage = "Your Profile"
        textViewWelcome = findViewById(R.id.textViewWelcome)
        textAccountDate = findViewById(R.id.textAccountDate)
        taskCountTextView = findViewById(R.id.textViewTaskCount)
        textCompletedTasks = findViewById(R.id.textCompletedTasks)
        recyclerViewOwnedBadges = findViewById(R.id.recyclerViewOwnedBadges)
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount)

        recyclerViewOwnedBadges.layoutManager = LinearLayoutManager(this)
        badgesAdapter = BadgeAdapter(this, badgesList)
        recyclerViewOwnedBadges.adapter = badgesAdapter

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        fetchAccountCreationDate()
        fetchCompletedTasksCount()
        fetchOwnedBadges()
        textViewWelcome.text = welcomeMessage

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val tasksCollectionRef = db.collection("tasks")

            tasksListenerRegistration = tasksCollectionRef
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val tasksCount = snapshot.size()
                        taskCountTextView.text = "Total Tasks: $tasksCount"
                    }
                }
        }

        val buttonHome: FloatingActionButton = findViewById(R.id.buttonHome)
        buttonHome.setOnClickListener {
            val username = intent.getStringExtra("USERNAME")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("USERNAME", username)
            }
            startActivity(intent)
            finish()
        }

        buttonDeleteAccount.setOnClickListener {
            confirmAccountDeletion()
        }
    }

    private fun confirmAccountDeletion() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        val db = FirebaseFirestore.getInstance()
        val userDocumentRef = db.collection("users").document(userId)

        userDocumentRef.delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, SignInActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to delete account.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to delete account data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchAccountCreationDate() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocumentRef = db.collection("users").document(userId)
            userDocumentRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val creationDate = documentSnapshot.getDate("signUpDate")

                        if (creationDate != null) {
                            val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
                            val formattedDate = dateFormat.format(creationDate)
                            textAccountDate.text = "Account created on: $formattedDate"
                        } else {
                            Log.e(TAG, "Creation date is null")
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                }
        }
    }

    private fun fetchCompletedTasksCount() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        userId?.let {
            db.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }

                    snapshot?.let {
                        val completedCount = snapshot.getLong("completed") ?: 0
                        textCompletedTasks.text = "Completed Tasks: $completedCount"
                    }
                }
        }
    }

    private fun fetchOwnedBadges() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocumentRef = db.collection("users").document(userId)
            userDocumentRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val unlockedBadgesIds = documentSnapshot.get("unlockedBadges") as? List<String> ?: emptyList()
                        if (unlockedBadgesIds.isNotEmpty()) {
                            db.collection("badges")
                                .whereIn("id", unlockedBadgesIds)
                                .get()
                                .addOnSuccessListener { documents ->
                                    badgesList.clear()
                                    for (document in documents) {
                                        val badge = document.toObject(Badge::class.java)
                                        badgesList.add(badge)
                                    }
                                    badgesAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { exception ->
                                    // Handle error
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tasksListenerRegistration?.remove()
    }
}
