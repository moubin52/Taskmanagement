package com.example.taskmanagement

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import java.text.SimpleDateFormat
import java.util.*

class EditTaskDialog(
    private val task: Task,
    private val editTaskListener: (Task) -> Unit
) : DialogFragment()
{

    private lateinit var editTextTitle: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var buttonEditDate: Button
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    private val calendar: Calendar = Calendar.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_edit_task, container, false)
        editTextTitle = view.findViewById(R.id.editTextTitle)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        buttonEditDate = view.findViewById(R.id.buttonEditDate)
        buttonSave = view.findViewById(R.id.buttonSave)
        buttonCancel = view.findViewById(R.id.buttonCancel)
        editTextTitle.setText(task.title)
        editTextDescription.setText(task.description)

        if (task.dueDate?.isNotEmpty() == true) {
            buttonEditDate.text = (task.dueDate)
        } else {
            buttonEditDate.text = "Change Due Date"
        }
        buttonEditDate.setOnClickListener {
            showDatePickerDialog()
        }
        buttonSave.setOnClickListener {
            val editedTitle = editTextTitle.text.toString().trim()
            val editedDescription = editTextDescription.text.toString().trim()

            if (editedTitle.isNotEmpty()) {
                val editedTask = Task(
                    id = task.id,
                    title = editedTitle,
                    description = editedDescription,
                    dueDate = if (buttonEditDate.text == "Change Due Date") "" else formatDate(calendar.time)
                )
                editTaskListener(editedTask)
                dismiss()
            } else {
                Toast.makeText(context, "Title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        buttonCancel.setOnClickListener {
            dismiss()
        }
        return view
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                buttonEditDate.text = formatDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}
