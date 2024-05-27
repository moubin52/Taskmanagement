package com.example.taskmanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
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
    private val badgesList = mutableListOf<Badge>()
    private var tasksListenerRegistration: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        textViewWelcome = findViewById(R.id.textViewWelcome)
        textAccountDate = findViewById(R.id.textAccountDate)
        taskCountTextView = findViewById(R.id.textViewTaskCount)
        textCompletedTasks = findViewById(R.id.textCompletedTasks)
        recyclerViewOwnedBadges = findViewById(R.id.recyclerViewOwnedBadges)

        recyclerViewOwnedBadges.layoutManager = LinearLayoutManager(this)
        badgesAdapter = BadgeAdapter(this, badgesList)
        recyclerViewOwnedBadges.adapter = badgesAdapter

        // Retrieve the current user's ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        fetchAccountCreationDate()
        fetchCompletedTasksCount()
        fetchOwnedBadges()

        // Update welcome message with username
        val username = intent.getStringExtra("USERNAME")
        val welcomeMessage = "$username's Profile"
        textViewWelcome.text = welcomeMessage

        // Check if user ID is not null
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val tasksCollectionRef = db.collection("tasks")

            // Query tasks for the current user
            tasksListenerRegistration = tasksCollectionRef
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Handle error
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val tasksCount = snapshot.size()
                        taskCountTextView.text = "Total Tasks: $tasksCount"
                    }
                }
        }

        // Set OnClickListener to buttonHome
        val buttonHome: FloatingActionButton = findViewById(R.id.buttonHome)
        buttonHome.setOnClickListener {
            val username = intent.getStringExtra("USERNAME")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("USERNAME", username)
            }
            startActivity(intent)
            finish() // Optional: Finish the ProfileActivity to remove it from the back stack
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
                            // Format the date to display in "dd/mm/yy" format
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
                        // Handle error
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
        // Remove the snapshot listener when the activity is destroyed
        tasksListenerRegistration?.remove()
    }
}
