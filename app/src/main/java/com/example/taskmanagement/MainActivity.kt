package com.example.taskmanagement

import TaskAdapter
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), ItemTouchHelperAdapter {
    private val taskRepository = TaskRepository()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var emptyStateLayout: View
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var textViewWelcome: TextView
    private lateinit var taskCountTextView: TextView
    private lateinit var profileButton: FloatingActionButton
    private val db = FirebaseFirestore.getInstance()
    private lateinit var pointsTextView: TextView
    private lateinit var currentUserId: String
    private lateinit var currentUsername: String
    private lateinit var currentFriendcode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskCountTextView = findViewById(R.id.taskCountTextView)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        textViewWelcome = findViewById(R.id.textViewWelcome)
        profileButton = findViewById(R.id.buttonProfile)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        pointsTextView = findViewById(R.id.pointsTextView)

        // Retrieve the original user ID from the intent extras
        currentUserId = intent.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        currentUsername = intent.getStringExtra("USERNAME") ?:""
        currentFriendcode = intent.getStringExtra("FRIENDCODE") ?:""

        if (currentUserId.isNotEmpty()) {
            fetchAndDisplayUserDetails(currentUserId)
        } else {
            Log.e(TAG, "User ID is null or empty")
        }

        // Initialize RecyclerView
        taskAdapter = TaskAdapter(
            emptyList()
        ) { task -> showEditTaskDialog(task) }
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter

        // Initialize DrawerLayout and NavigationView
        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val buttonOpenDrawer: FloatingActionButton = findViewById(R.id.buttonSettings)
        val navigationView: NavigationView = findViewById(R.id.navigationView)

        buttonOpenDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Initialize profileButton click listener
        profileButton.setOnClickListener {
            // Start ProfileActivity and pass the extras
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("USER_ID", currentUserId)
            startActivity(intent)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_friends -> {
                    showFriendsActivity()
                    true
                }
                R.id.nav_badges -> {
                    showBadgesActivity()
                    true
                }
                R.id.nav_signout -> {
                    showSignActivity()
                    true
                }
                else -> false
            }
        }

        // Setup item touch helper for swipe-to-delete functionality
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onItemDismiss(viewHolder.adapterPosition)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerViewTasks)

        // Show Add Task dialog when FAB is clicked
        val buttonAddTask: FloatingActionButton = findViewById(R.id.buttonAddTask)
        buttonAddTask.setOnClickListener {
            showAddTaskDialog()
        }
        fetchAndDisplayPoints()
        observeTasks()
    }

    private fun fetchAndDisplayUserDetails(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val username = documentSnapshot.getString("username") ?: "guest"
                    // Update the welcome message with the username
                    val welcomeMessage = "Welcome back, $username"
                    textViewWelcome.text = welcomeMessage
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user details", e)
            }
    }

    private fun showSignActivity() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun showFriendsActivity() {
        val intent = Intent(this, FriendsActivity::class.java)
        intent.putExtra("USER_ID", currentUserId)
        intent.putExtra("FRIENDCODE", currentFriendcode)
        startActivity(intent)
    }

    private fun showBadgesActivity() {
        val intent = Intent(this, BadgesActivity::class.java)
        intent.putExtra("USER_ID", currentUserId)
        intent.putExtra("USERNAME", currentUsername)
        startActivity(intent)
    }

    override fun onItemDismiss(position: Int) {
        val task = taskAdapter.getTaskAtPosition(position)

        if (task != null) {
            val db = FirebaseFirestore.getInstance()
            val documentRef = db.collection("tasks").document(task.id)

            Log.d(TAG, "Attempting to delete document: ${documentRef.path}")

            // Increment completion count for the user
            if (currentUserId.isNotEmpty()) {
                val userRef = db.collection("users").document(currentUserId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val currentCompletedTasks = snapshot.getLong("completed") ?: 0
                    val currentPoints = snapshot.getLong("points") ?: 0 // Get current points
                    Log.d(TAG, "Current completed tasks: $currentCompletedTasks")
                    transaction.update(userRef, "completed", currentCompletedTasks + 1)
                    transaction.update(userRef, "points", currentPoints + 2) // Increment points by 2
                }
                    .addOnSuccessListener {
                        Log.d(TAG, "User completion count updated successfully")
                        // Once completion count is updated, proceed to delete the task
                        deleteTask(documentRef, position)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating user completion count", e)
                    }
            } else {
                Log.e(TAG, "User ID is null or empty")
            }
        } else {
            Log.e(TAG, "Task is null")
        }
    }

    private fun fetchAndDisplayPoints() {
        val userRef = db.collection("users").document(currentUserId)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val points = documentSnapshot.getLong("points") ?: 0
                    pointsTextView.text = "Points: $points"
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching points", e)
            }
    }

    private fun deleteTask(documentRef: DocumentReference, position: Int) {
        documentRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
                val updatedTasks = taskAdapter.getTasks().toMutableList()
                if (position >= 0 && position < updatedTasks.size) {
                    updatedTasks.removeAt(position)
                    taskAdapter.updateTasks(updatedTasks)
                    Log.d(TAG, "Task removed from adapter")
                } else {
                    Log.w(TAG, "Invalid position $position for list size ${updatedTasks.size}")
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting document", e)
            }
    }

    private fun showAddTaskDialog() {
        // Display the dialog to add a new task
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editTextTitle = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val editTextDescription = dialogView.findViewById<EditText>(R.id.editTextDescription)
        val buttonAdd = dialogView.findViewById<Button>(R.id.buttonAdd)
        val buttonPickDate = dialogView.findViewById<Button>(R.id.buttonPickDate)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = builder.create()

        val calendar = Calendar.getInstance()
        var selectedDate: Date? = null

        buttonPickDate.setOnClickListener {
            // Allow the user to pick a date for the task
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.time
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        buttonAdd.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val dueDate = selectedDate?.let { formatDate(it) }

            if (title.isNotEmpty()) {
                // Generate a random ID for the new task
                val taskId = UUID.randomUUID().toString()

                // Create a new task object with the random ID and user ID
                val newTask = Task(id = taskId, title = title, description = description, dueDate = dueDate, userId = currentUserId)

                // Store the task document in the new collection in Firestore
                val db = FirebaseFirestore.getInstance()
                db.collection("tasks") // Adjust to your new collection name
                    .document(taskId)
                    .set(newTask)
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot added with ID: $taskId")
                        dialog.dismiss()
                        // Increment points directly here
                        val userRef = db.collection("users").document(currentUserId)
                        db.runTransaction { transaction ->
                            val snapshot = transaction.get(userRef)
                            val currentPoints = snapshot.getLong("points") ?: 0
                            transaction.update(userRef, "points", currentPoints + 1) // Increment by 1 points for task creation
                        }
                            .addOnSuccessListener {
                                Log.d(TAG, "Points incremented successfully")
                                fetchAndDisplayPoints()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error incrementing points", e)
                            }
                    }
                    .addOnFailureListener { e ->
                        // Handle error adding task
                        Log.e(TAG, "Error adding document", e)
                    }
            } else {
                // Notify the user if the title is empty
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        buttonCancel.setOnClickListener {
            // Dismiss the dialog
            dialog.dismiss()
        }

        // Show the dialog
        dialog.show()
    }

    private fun showEditTaskDialog(task: Task) {
        val editTaskDialog = EditTaskDialog(task) { editedTask ->
            taskRepository.updateTask(editedTask)
        }
        val fragmentManager: FragmentManager = supportFragmentManager
        editTaskDialog.show(fragmentManager, "EditTaskDialog")
    }

    private fun observeTasks() {
        // Check if user ID is not empty
        if (currentUserId.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val tasksCollectionRef = db.collection("tasks")

            // Query tasks for the current user
            tasksCollectionRef
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val tasksList = mutableListOf<Task>()

                        for (document in snapshot.documents) {
                            val task = document.toObject(Task::class.java)
                            task?.let { tasksList.add(it) }
                        }

                        // Update the UI with the retrieved tasks
                        taskAdapter.updateTasks(tasksList)
                        updateEmptyStateVisibility(tasksList.isEmpty())
                        updateTaskCount(tasksList.size) // Update task count
                        fetchAndDisplayPoints()
                    } else {
                        Log.d(TAG, "Current data: null")
                    }
                }
        } else {
            Log.e(TAG, "User ID is null or empty")
        }
    }

    private fun updateTaskCount(count: Int) {
        taskCountTextView.text = "Tasks: $count"
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }

    private fun updateEmptyStateVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerViewTasks.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerViewTasks.visibility = View.VISIBLE
        }
    }
}
