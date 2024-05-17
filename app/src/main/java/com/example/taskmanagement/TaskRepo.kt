package com.example.taskmanagement

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class TaskRepository {
    private val db = FirebaseFirestore.getInstance()
    private val databaseReference: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("Tasks")

    fun getAllTasksForUser(userId: String, callback: (List<Task>) -> Unit) {
        db.collection("users").document(userId).collection("tasks")
            .get()
            .addOnSuccessListener { result ->
                val tasks = mutableListOf<Task>()
                for (document in result) {
                    val task = document.toObject(Task::class.java)
                    tasks.add(task)
                }
                callback(tasks)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting tasks for user", exception)
                callback(emptyList())
            }
    }


    fun getAllTasks(callback: (List<Task>) -> Unit) {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tasks = mutableListOf<Task>()
                for (snapshot in dataSnapshot.children) {
                    val task = snapshot.getValue(Task::class.java)
                    task?.let { tasks.add(it) }
                }
                callback(tasks)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TaskRepository", "Error fetching tasks: ${databaseError.message}")
            }
        })
    }

    fun addTask(task: Task) {
        val newTaskReference = databaseReference.push()
        task.id = newTaskReference.key.toString()
        newTaskReference.setValue(task)
    }

    fun updateTask(task: Task) {
        databaseReference.child(task.id).setValue(task)
    }

    fun deleteTask(taskId: String) {
        databaseReference.child(taskId).removeValue()
    }
}

