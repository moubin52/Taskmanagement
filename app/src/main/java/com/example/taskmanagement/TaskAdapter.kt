import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.R
import com.example.taskmanagement.Task

class TaskAdapter(
    private var tasks: List<Task>,
    private val editTaskListener: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        val dueDateTextView: TextView = itemView.findViewById(R.id.dueDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.titleTextView.text = currentTask.title
        holder.descriptionTextView.text = currentTask.description

        if (currentTask.dueDate?.isNotEmpty() == true) {
            holder.dueDateTextView.visibility = View.VISIBLE
            holder.dueDateTextView.text = "Due Date: ${currentTask.dueDate}"
        } else {
            holder.dueDateTextView.visibility = View.GONE
        }
        holder.itemView.setOnLongClickListener {
            editTaskListener(currentTask)
            true
        }
    }

    fun getTasks(): List<Task> {
        return tasks
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTasks(newTasks: List<Task>) {
        Log.d("TaskAdapter", "Updating tasks: ${newTasks.size}")
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun getTaskAtPosition(position: Int): Task {
        return tasks[position]
    }
}
