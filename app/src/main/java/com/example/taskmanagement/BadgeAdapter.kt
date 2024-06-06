package com.example.taskmanagement

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class   BadgeAdapter(private val context: Context, private val badgesList: MutableList<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val itemView = LayoutInflater.from(context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val currentBadge = badgesList[position]
        holder.bind(currentBadge)

        holder.itemView.setOnClickListener {
            showBuyBadgeDialog(context, currentBadge.id, currentBadge.cost)
        }
    }

    override fun getItemCount() = badgesList.size

    inner class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageViewBadge: ImageView = itemView.findViewById(R.id.imageViewBadge)
        private val textViewBadgeName: TextView = itemView.findViewById(R.id.textViewBadgeName)
        private val textViewBadgeCost: TextView = itemView.findViewById(R.id.textViewBadgeCost)

        fun bind(badge: Badge) {
            textViewBadgeName.text = badge.name
            textViewBadgeCost.text = "Cost: ${badge.cost}"
            Picasso.get().load(badge.image).into(imageViewBadge)
        }
    }

    private fun showBuyBadgeDialog(context: Context, badgeId: String, badgeCost: Int) {
        if (context is AppCompatActivity) {
            val fragmentManager = context.supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            val previousFragment = fragmentManager.findFragmentByTag("buyBadgeDialog")
            previousFragment?.let { fragmentTransaction.remove(it) }

            val dialogFragment = BuyBadgeDialogFragment.newInstance(badgeId, badgeCost)
            dialogFragment.show(fragmentTransaction, "buyBadgeDialog")
        }
    }
}
