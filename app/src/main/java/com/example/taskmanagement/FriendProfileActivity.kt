package com.example.taskmanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

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
                    val unlockedBadges = friendDocument.get("unlockedBadges") as? List<String> ?: emptyList()
                    Log.d(TAG, "Friend ID: $friendId") // Log the friend ID
                    Log.d(TAG, "Unlocked Badges: $unlockedBadges") // Log the unlocked badges

                    if (unlockedBadges.isNotEmpty()) {
                        db.collection("badges")
                            .whereIn("id", unlockedBadges)
                            .get()
                            .addOnSuccessListener { documents ->
                                badgeList.clear()
                                for (document in documents) {
                                    val badgeId = document.id
                                    val badgeName = document.getString("name") ?: "Unknown"
                                    val badgeImage = document.getString("image") ?: ""
                                    val badgeCost = document.getLong("cost")?.toInt() ?: 0
                                    badgeList.add(Badge(badgeId, badgeName, badgeImage, badgeCost))
                                }
                                Log.d(TAG, "Badges fetched: ${badgeList.size}") // Log fetched badges count
                                badgeAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { exception ->
                                Log.e(TAG, "Error fetching badges: ${exception.message}") // Log the error
                                Toast.makeText(this, "Error fetching badges: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.d(TAG, "No unlocked badges for user: $friendUsername")
                    }
                } else {
                    Log.e(TAG, "No friend document found") // Log if no document is found
                    Toast.makeText(this, "Friend not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error fetching friend data: ${exception.message}") // Log the error
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
