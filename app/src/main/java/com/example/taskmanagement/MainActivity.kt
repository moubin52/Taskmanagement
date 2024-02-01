package com.example.taskmanagement

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), ItemTouchHelperAdapter {
    private val taskRepository = TaskRepository()
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var database: DatabaseReference
    private val databaseReference: DatabaseReference = Firebase.database.reference.child("Tasks")
    private lateinit var emptyStateLayout: View
    private lateinit var recyclerViewTasks: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        database = Firebase.database.reference.child("Tasks")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)
        supportActionBar?.hide()
        val buttonAddTask: FloatingActionButton = findViewById(R.id.buttonAddTask)
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        taskAdapter = TaskAdapter(
            emptyList(),
            { task -> },
            this,
            { task -> showEditTaskDialog(task) }
        )
        recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        recyclerViewTasks.adapter = taskAdapter
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
        buttonAddTask.setOnClickListener {
            showAddTaskDialog()
        }
        observeTasks()
    }

    private fun showAddTaskDialog() {
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
                val newTask = Task(title = title, description = description, dueDate = dueDate)
                taskRepository.addTask(newTask)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

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
        taskRepository.getAllTasks { tasks ->
            Log.d("MainActivity", "Retrieved tasks: $tasks")
            taskAdapter.updateTasks(tasks)
            updateEmptyStateVisibility(tasks.isEmpty())
        }
    }

    override fun onItemDismiss(position: Int) {
        val task = taskAdapter.getTaskAtPosition(position)
        taskRepository.deleteTask(task.id)
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
