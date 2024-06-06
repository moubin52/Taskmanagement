package com.example.taskmanagement


import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
class TaskRepository {
    private val databaseReference: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("Tasks")

    fun updateTask(task: Task) {
        databaseReference.child(task.id).setValue(task)
    }
}

