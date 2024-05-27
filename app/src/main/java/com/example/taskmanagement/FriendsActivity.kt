package com.example.taskmanagement

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.content.ContentValues.TAG

class FriendsActivity : AppCompatActivity() {

    private lateinit var textViewFriendCode: TextView
    private lateinit var editTextSearch: EditText
    private lateinit var buttonAdd: Button
    private lateinit var recyclerViewFriends: RecyclerView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val friendsList = ArrayList<FriendsAdapter.Friend>()
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        textViewFriendCode = findViewById(R.id.textViewFriendCode)
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonAdd = findViewById(R.id.buttonAdd)
        recyclerViewFriends = findViewById(R.id.recyclerViewFriends)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val username = intent.getStringExtra("USERNAME") ?: ""
        val friendcode = intent.getStringExtra("FRIENDCODE") ?: ""
        val friendslist = intent.getStringArrayListExtra("FRIENDSLIST") ?: arrayListOf()
        val currentUser = auth.currentUser
        currentUserId = currentUser?.uid ?: ""

        textViewFriendCode.text = "Friend Code: $friendcode"

        // Initialize the adapter and set it to the RecyclerView
        friendsAdapter = FriendsAdapter(friendsList) { friend ->
            // Handle the view button click here
            navigateToFriendProfileActivity(friend)
        }
        recyclerViewFriends.layoutManager = LinearLayoutManager(this)
        recyclerViewFriends.adapter = friendsAdapter

        buttonAdd.setOnClickListener {
            val enteredFriendCode = editTextSearch.text.toString().trim()
            if (enteredFriendCode.isNotEmpty()) {
                addFriendByFriendCode(friendcode, enteredFriendCode, friendslist, username)
            } else {
                Toast.makeText(this, "Please enter a friend code", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch friends list from Firestore and update RecyclerView
        fetchFriendsList(username)
    }

    private fun fetchFriendsList(username: String) {
        val currentUserId = auth.currentUser?.uid
        currentUserId?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    document?.let {
                        val friendsIds = document.get("friendslist") as? List<String> ?: emptyList()
                        if (friendsIds.isNotEmpty()) {
                            db.collection("users").whereIn("userId", friendsIds).get()
                                .addOnSuccessListener { documents ->
                                    friendsList.clear()
                                    for (doc in documents) {
                                        val friendUsername = doc.getString("username") ?: "Unknown"
                                        val friendFriendCode = doc.getString("friendcode") ?: "Unknown"
                                        friendsList.add(FriendsAdapter.Friend(friendUsername, friendFriendCode))
                                    }
                                    friendsAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error fetching friends details", e)
                                    Toast.makeText(this, "Error fetching friends details", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching user data", e)
                    Toast.makeText(this, "Error fetching user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addFriendByFriendCode(currentUserFriendCode: String, friendCodeToAdd: String, currentUserFriendsList: ArrayList<String>, username: String) {
        val currentUserId = auth.currentUser?.uid

        // Check if the user is trying to add themselves
        if (currentUserFriendCode == friendCodeToAdd) {
            Toast.makeText(this, "You cannot add yourself as a friend", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the friend is already in the user's friends list
        db.collection("users").whereEqualTo("friendcode", friendCodeToAdd).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Friend code not found", Toast.LENGTH_SHORT).show()
                } else {
                    val friendUserId = documents.documents[0].getString("userId")
                    if (friendUserId != null && currentUserId != null) {
                        if (currentUserFriendsList.contains(friendUserId)) {
                            Toast.makeText(this, "This friend is already in your friends list", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Add the friend to both users' friendslist
                        val currentUserDoc = db.collection("users").document(currentUserId)
                        val friendUserDoc = db.collection("users").document(friendUserId)

                        db.runBatch { batch ->
                            batch.update(currentUserDoc, "friendslist", FieldValue.arrayUnion(friendUserId))
                            batch.update(friendUserDoc, "friendslist", FieldValue.arrayUnion(currentUserId))
                        }.addOnSuccessListener {
                            Toast.makeText(this, "Friend added successfully", Toast.LENGTH_SHORT).show()
                            currentUserFriendsList.add(friendUserId) // Update local friends list
                            fetchFriendsList(username) // Refresh the friends list
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error adding friend", e)
                            Toast.makeText(this, "Failed to add friend. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Error: User not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error searching friend code. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToFriendProfileActivity(friend: FriendsAdapter.Friend) {
        val friendUsername = friend.username
        db.collection("users")
            .whereEqualTo("username", friendUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val friendDocument = querySnapshot.documents.first()
                    val friendAccountDateTimestamp = friendDocument.getTimestamp("signUpDate")
                    val friendAccountDate = friendAccountDateTimestamp?.toDate()?.toString() ?: "Unknown"
                    val friendTotalTasks = friendDocument.getLong("totalTasks") ?: 0
                    val friendCompletedTasks = friendDocument.getLong("completedTasks") ?: 0

                    val intent = Intent(this, FriendProfileActivity::class.java)
                    intent.putExtra("FRIEND_USERNAME", friendUsername)
                    intent.putExtra("FRIEND_ACCOUNT_DATE", friendAccountDate)
                    intent.putExtra("FRIEND_TOTAL_TASKS", friendTotalTasks)
                    intent.putExtra("FRIEND_COMPLETED_TASKS", friendCompletedTasks)
                    intent.putExtra("ORIGINAL_USER_ID", currentUserId) // Pass the original user ID
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to fetch friend data", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch friend data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
