package com.mpc.propass.organizer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.mpc.propass.HomeDashboardActivity
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerEventStore
import com.mpc.propass.organizer.model.OrganizerEvent
import com.mpc.propass.organizer.ui.adapter.OrganizerEventAdapter

/**
 * Organizer Dashboard Screen:
 * Displays active and published organizer events, provides empty-state guidance,
 * and launches the Create Event flow.
 */
class OrganizerDashboardActivity : AppCompatActivity() {

    private lateinit var btnBackToAttendee: ImageButton
    private lateinit var btnSwitchMode: MaterialButton
    private lateinit var btnEmptyCreateEvent: MaterialButton
    private lateinit var fabCreateEvent: ExtendedFloatingActionButton
    private lateinit var tvEventCountBadge: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvOrganizerEvents: RecyclerView

    private val eventAdapter = OrganizerEventAdapter { event ->
        // On event clicked, view event QR & details
        val intent = Intent(this, EventPublishSuccessActivity::class.java).apply {
            putExtra(EventPublishSuccessActivity.EXTRA_EVENT, event)
            putExtra(EventPublishSuccessActivity.EXTRA_IS_VIEW_MODE, true)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_organizer_dashboard)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshEvents()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        btnBackToAttendee = findViewById(R.id.btnBackToAttendee)
        btnSwitchMode = findViewById(R.id.btnSwitchMode)
        btnEmptyCreateEvent = findViewById(R.id.btnEmptyCreateEvent)
        fabCreateEvent = findViewById(R.id.fabCreateEvent)
        tvEventCountBadge = findViewById(R.id.tvEventCountBadge)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        rvOrganizerEvents = findViewById(R.id.rvOrganizerEvents)

        rvOrganizerEvents.layoutManager = LinearLayoutManager(this)
        rvOrganizerEvents.adapter = eventAdapter

        val root = findViewById<View>(R.id.organizerDashboardRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            findViewById<View>(R.id.topBarContent).setPadding(
                16.dpToPx(),
                statusBar.top,
                16.dpToPx(),
                0
            )
            insets
        }
    }

    private fun setupListeners() {
        btnBackToAttendee.setOnClickListener {
            navigateBackToAttendee()
        }

        btnSwitchMode.setOnClickListener {
            navigateBackToAttendee()
        }

        btnEmptyCreateEvent.setOnClickListener {
            launchCreateEvent()
        }

        fabCreateEvent.setOnClickListener {
            launchCreateEvent()
        }
    }

    private fun refreshEvents() {
        val events = OrganizerEventStore.getEvents()
        tvEventCountBadge.text = "${events.size} Events"

        if (events.isEmpty()) {
            layoutEmptyState.visibility = View.VISIBLE
            rvOrganizerEvents.visibility = View.GONE
            fabCreateEvent.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvOrganizerEvents.visibility = View.VISIBLE
            fabCreateEvent.visibility = View.VISIBLE
            eventAdapter.submitList(events)
        }
    }

    private fun launchCreateEvent() {
        startActivity(Intent(this, CreateEventActivity::class.java))
    }

    private fun navigateBackToAttendee() {
        val intent = Intent(this, HomeDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
