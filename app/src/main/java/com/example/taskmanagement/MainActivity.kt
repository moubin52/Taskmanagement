package com.example.taskmanagement

import TaskAdapter
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues.TAG
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        taskCountTextView = findViewById(R.id.taskCountTextView)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        taskAdapter = TaskAdapter(
            emptyList(),
            { task -> },
            this,
            { task -> showEditTaskDialog(task) }
        )
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter

        textViewWelcome = findViewById(R.id.textViewWelcome)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawerLayout)
        val buttonOpenDrawer: FloatingActionButton = findViewById(R.id.buttonSettings)
        val navigationView: NavigationView = findViewById(R.id.navigationView)

        buttonOpenDrawer.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }



        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_login -> {
                    showLoginDialog()
                    true
                }
                R.id.nav_signup -> {
                    showSignUpDialog()
                    true
                }
                else -> false
            }
        }

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // Setup RecyclerView and TaskAdapter
        taskAdapter = TaskAdapter(
            emptyList(),
            { task -> },
            this,
            { task -> showEditTaskDialog(task) }
        )
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter

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
        taskCountTextView = findViewById(R.id.taskCountTextView)
        observeTasks()
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null)
        val editTextUsername = dialogView.findViewById<EditText>(R.id.editTextUsernameLogin)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPasswordLogin)
        val buttonLogin = dialogView.findViewById<Button>(R.id.buttonLogIn)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelLogIn)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = builder.create()

        buttonLogin.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword("$username@placeholder.com", password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            textViewWelcome.text = "Welcome back, $username"
                            dialog.dismiss()
                            observeTasks()
                        } else {
                            Toast.makeText(baseContext, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun signUpUserToFirestore(username: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val db = FirebaseFirestore.getInstance()
            val userData = hashMapOf(
                "userId" to userId,
                "username" to username
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




    private fun showSignUpDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signup, null)
        val editTextUsername = dialogView.findViewById<EditText>(R.id.editTextUsername)
        val editTextPassword = dialogView.findViewById<EditText>(R.id.editTextPassword)
        val buttonSignUp = dialogView.findViewById<Button>(R.id.buttonSignUp)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancelSignUp)
        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
        val dialog = builder.create()

        buttonSignUp.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                // Create user with Firebase Authentication
                FirebaseAuth.getInstance().createUserWithEmailAndPassword("$username@placeholder.com", password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign up success, add user data to Firestore
                            signUpUserToFirestore(username)
                            // Update UI or perform other actions upon successful signup
                            Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
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

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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
                // Get the current user ID
                val currentUser = FirebaseAuth.getInstance().currentUser
                val userId = currentUser?.uid

                if (userId != null) {
                    // Generate a random ID for the new task
                    val taskId = UUID.randomUUID().toString()

                    // Create a new task object with the random ID and user ID
                    val newTask = Task(id = taskId, title = title, description = description, dueDate = dueDate, userId = userId)

                    // Store the task document in the new collection in Firestore
                    val db = FirebaseFirestore.getInstance()
                    db.collection("tasks") // Adjust to your new collection name
                        .document(taskId)
                        .set(newTask)
                        .addOnSuccessListener {
                            Log.d(TAG, "DocumentSnapshot added with ID: $taskId")
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            // Handle error adding task
                            Log.e(TAG, "Error adding document", e)
                        }
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

    private var tasksListenerRegistration: ListenerRegistration? = null

    private fun observeTasks() {
        // Get the current user ID
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        // Check if user ID is not null
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val tasksCollectionRef = db.collection("tasks")

            // Query tasks for the current user
            tasksCollectionRef
                .whereEqualTo("userId", userId)
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
                    } else {
                        Log.d(TAG, "Current data: null")
                    }
                }
        } else {
            // Handle the case where the user is not logged in
            // For example, show a login dialog or redirect to the login screen
        }
    }

    private fun updateTaskCount(count: Int) {
        taskCountTextView.text = "Tasks: $count"
    }
    override fun onItemDismiss(position: Int) {
        val task = taskAdapter.getTaskAtPosition(position)

        if (task != null) {
            val db = FirebaseFirestore.getInstance()
            val documentRef = db.collection("tasks").document(task.id)

            Log.d(TAG, "Attempting to delete document: ${documentRef.path}")

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
        } else {
            Log.e(TAG, "Task is null")
        }
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