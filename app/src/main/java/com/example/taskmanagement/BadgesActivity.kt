package com.example.taskmanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BadgesActivity : AppCompatActivity(), BuyBadgeDialogFragment.OnBadgeBoughtListener {

    private lateinit var recyclerViewBadges: RecyclerView
    private lateinit var badgesAdapter: BadgeAdapter
    private val badgesList = mutableListOf<Badge>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var textViewUserPoints: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badges)

        recyclerViewBadges = findViewById(R.id.recyclerViewBadges)
        textViewUserPoints = findViewById(R.id.textViewUserPoints)
        recyclerViewBadges.layoutManager = LinearLayoutManager(this)
        badgesAdapter = BadgeAdapter(this, badgesList)
        recyclerViewBadges.adapter = badgesAdapter

        auth = FirebaseAuth.getInstance()

        val buttonHome: FloatingActionButton = findViewById(R.id.buttonHome)
        buttonHome.setOnClickListener {
            val username = intent.getStringExtra("USERNAME")
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("USERNAME", username)
            }
            startActivity(intent)
            finish()
        }

        fetchAndDisplayPoints()
        fetchBadges()
    }

    private fun fetchAndDisplayPoints() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userPointsRef = db.collection("users").document(userId)
            userPointsRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    val points = documentSnapshot.getLong("points")?.toInt() ?: 0
                    textViewUserPoints.text = "Points: $points"
                }
                .addOnFailureListener { exception ->
                    // Handle failure
                }
        }
    }

    private fun fetchBadges() {
        db.collection("badges").get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val badge = document.toObject(Badge::class.java)
                    badgesList.add(badge)
                }
                badgesAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }

    override fun onBadgeBought() {
        fetchAndDisplayPoints()
    }
}

class BuyBadgeDialogFragment : DialogFragment() {

    private var badgeId: String? = null
    private var badgeCost: Int = 0
    private var listener: OnBadgeBoughtListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        badgeId = arguments?.getString(ARG_BADGE_ID)
        badgeCost = arguments?.getInt(ARG_BADGE_COST) ?: 0
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnBadgeBoughtListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnBadgeBoughtListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_buy_badge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set click listeners for Confirm and Cancel buttons
        view.findViewById<Button>(R.id.buttonConfirm).setOnClickListener {
            badgeId?.let { id -> attemptToBuyBadge(id, badgeCost) }
        }

        view.findViewById<Button>(R.id.buttonCancel).setOnClickListener {
            // Dismiss the dialog
            dismiss()
        }
    }

    private fun attemptToBuyBadge(badgeId: String, badgeCost: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

            userRef.get().addOnSuccessListener { documentSnapshot ->
                val currentPoints = documentSnapshot.getLong("points")?.toInt() ?: 0
                val unlockedBadges = (documentSnapshot.get("unlockedBadges") as? List<String> ?: emptyList()).toMutableList()

                if (unlockedBadges.contains(badgeId)) {
                    // User already owns the badge, display toast message
                    Toast.makeText(context, "You already own this badge!", Toast.LENGTH_SHORT).show()
                } else if (currentPoints >= badgeCost) {
                    // Update points and unlocked badges
                    val newPoints = currentPoints - badgeCost
                    unlockedBadges.add(badgeId)

                    userRef.update(mapOf(
                        "points" to newPoints,
                        "unlockedBadges" to unlockedBadges
                    )).addOnSuccessListener {
                        // Successfully bought the badge
                        listener?.onBadgeBought()
                        dismiss()
                    }.addOnFailureListener {
                        // Handle the error
                    }
                } else {

                }
            }.addOnFailureListener {
                // Handle the error
            }
        }
    }


    companion object {
        private const val ARG_BADGE_ID = "badge_id"
        private const val ARG_BADGE_COST = "badge_cost"

        fun newInstance(badgeId: String, badgeCost: Int): BuyBadgeDialogFragment {
            val fragment = BuyBadgeDialogFragment()
            val args = Bundle().apply {
                putString(ARG_BADGE_ID, badgeId)
                putInt(ARG_BADGE_COST, badgeCost)
            }
            fragment.arguments = args
            return fragment
        }
    }

    interface OnBadgeBoughtListener {
        fun onBadgeBought()
    }
}
