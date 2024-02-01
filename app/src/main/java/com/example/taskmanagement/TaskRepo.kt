package com.example.taskmanagement

import android.util.Log
import com.google.firebase.database.*

class TaskRepository {

    private val databaseReference: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("Tasks")

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

