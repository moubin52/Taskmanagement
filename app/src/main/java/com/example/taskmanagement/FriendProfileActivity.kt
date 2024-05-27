package com.example.taskmanagement

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.content.ContentValues.TAG

class FriendProfileActivity : AppCompatActivity() {

    private lateinit var textViewWelcome: TextView
    private lateinit var textCompletedTasks: TextView
    private lateinit var textAccountDate: TextView
    private lateinit var recyclerViewOwnedBadges: RecyclerView
    private lateinit var buttonHome: FloatingActionButton

    private lateinit var badgeAdapter: BadgeAdapter
    private val badgeList = ArrayList<Badge>()
    private lateinit var db: FirebaseFirestore

    private lateinit var friendUsername: String
    private lateinit var friendAccountDate: String
    private var friendCompletedTasks: Long = 0
    private lateinit var originalUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_profile)

        textViewWelcome = findViewById(R.id.textViewWelcome)
        textCompletedTasks = findViewById(R.id.textCompletedTasks)
        textAccountDate = findViewById(R.id.textAccountDate)
        recyclerViewOwnedBadges = findViewById(R.id.recyclerViewOwnedBadges)
        buttonHome = findViewById(R.id.buttonHome)

        db = FirebaseFirestore.getInstance()

        // Retrieve data from the intent
        friendUsername = intent.getStringExtra("FRIEND_USERNAME") ?: "Friend"
        friendAccountDate = intent.getStringExtra("FRIEND_ACCOUNT_DATE") ?: "Unknown"
        friendCompletedTasks = intent.getLongExtra("FRIEND_COMPLETED_TASKS", 0)
        originalUserId = intent.getStringExtra("ORIGINAL_USER_ID") ?: return

        // Set the welcome text with friend's username
        textViewWelcome.text = "$friendUsername's Profile"

        buttonHome.setOnClickListener {
            navigateToMainActivity()
        }

        badgeAdapter = BadgeAdapter(this, badgeList)
        recyclerViewOwnedBadges.layoutManager = LinearLayoutManager(this)
        recyclerViewOwnedBadges.adapter = badgeAdapter

        // Display friend's profile data
        displayFriendProfile()

        // Fetch and display friend's badges
        fetchFriendBadges(friendUsername)
    }

    private fun displayFriendProfile() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedAccountDate = dateFormat.format(SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH).parse(friendAccountDate) ?: return)

        textCompletedTasks.text = "Completed Tasks: $friendCompletedTasks"
        textAccountDate.text = "Account created: $formattedAccountDate"
    }

    private fun fetchFriendBadges(friendUsername: String) {
        db.collection("users")
            .whereEqualTo("username", friendUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val friendDocument = querySnapshot.documents.first()
                    val friendId = friendDocument.id
                    db.collection("users").document(friendId).collection("badges").get()
                        .addOnSuccessListener { documents ->
                            badgeList.clear()
                            for (document in documents) {
                                val badgeId = document.id
                                val badgeName = document.getString("name") ?: "Unknown"
                                val badgeImage = document.getString("image") ?: ""
                                val badgeCost = document.getLong("cost")?.toInt() ?: 0
                                badgeList.add(Badge(badgeId, badgeName, badgeImage, badgeCost))
                            }
                            badgeAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this, "Error fetching badges: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching friend data: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("USER_ID", originalUserId) // Pass the original user ID
        startActivity(intent)
        finish()
    }
}
