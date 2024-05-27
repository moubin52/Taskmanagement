package com.example.taskmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FriendsAdapter(
    private val friendsList: List<Friend>,
    private val onViewClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendsAdapter.FriendViewHolder>() {

    data class Friend(val username: String, val friendCode: String)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friendsList[position]
        holder.bind(friend, onViewClick)
    }

    override fun getItemCount() = friendsList.size

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFriendUsername: TextView = itemView.findViewById(R.id.textViewFriendUsername)
        private val buttonViewFriend: Button = itemView.findViewById(R.id.buttonViewFriend)

        fun bind(friend: Friend, onViewClick: (Friend) -> Unit) {
            textViewFriendUsername.text = friend.username
            buttonViewFriend.setOnClickListener {
                onViewClick(friend)
            }
        }
    }
}
