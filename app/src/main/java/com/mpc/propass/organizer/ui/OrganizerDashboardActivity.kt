package com.mpc.propass.organizer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.mpc.propass.HomeDashboardActivity
import com.mpc.propass.ProPassApplication
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerEventRepository
import com.mpc.propass.organizer.data.OrganizerEventRepositoryImpl
import com.mpc.propass.organizer.data.OrganizerEventStore
import com.mpc.propass.organizer.model.OrganizerEvent
import com.mpc.propass.organizer.model.toOrganizerEvent
import com.mpc.propass.organizer.ui.adapter.OrganizerEventAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Organizer Dashboard Screen:
 * Displays active and published organizer events fetched from the backend,
 * provides empty-state guidance, and launches the Create Event flow.
 */
class OrganizerDashboardActivity : AppCompatActivity() {

    private lateinit var btnBackToAttendee: ImageButton
    private lateinit var btnSwitchMode: MaterialButton
    private lateinit var btnEmptyCreateEvent: MaterialButton
    private lateinit var fabCreateEvent: ExtendedFloatingActionButton
    private lateinit var tvEventCountBadge: TextView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetryEvents: MaterialButton
    private lateinit var dashboardProgressBar: ProgressBar
    private lateinit var rvOrganizerEvents: RecyclerView

    private var fetchJob: Job? = null

    private val eventAdapter = OrganizerEventAdapter { event ->
        // On event clicked, view event details & registrations management (Phase 4D)
        val intent = Intent(this, OrganizerEventDetailActivity::class.java).apply {
            putExtra(OrganizerEventDetailActivity.EXTRA_EVENT, event)
            putExtra(OrganizerEventDetailActivity.EXTRA_EVENT_ID, event.id)
        }
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authRepository = (application as? ProPassApplication)?.authRepository
        if (authRepository != null && !authRepository.isOrganizer()) {
            Toast.makeText(this, "Organizer access required.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupEdgeToEdge()
        setContentView(R.layout.activity_organizer_dashboard)

        initViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshEvents()
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
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
        layoutErrorState = findViewById(R.id.layoutErrorState)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetryEvents = findViewById(R.id.btnRetryEvents)
        dashboardProgressBar = findViewById(R.id.dashboardProgressBar)
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

        btnRetryEvents.setOnClickListener {
            refreshEvents()
        }
    }

    private fun refreshEvents() {
        val repo: OrganizerEventRepository = (application as? ProPassApplication)?.organizerEventRepository
            ?: OrganizerEventRepositoryImpl()

        dashboardProgressBar.visibility = View.VISIBLE
        layoutErrorState.visibility = View.GONE

        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            val result = repo.getMyEvents()
            dashboardProgressBar.visibility = View.GONE

            result.onSuccess { eventDtos ->
                val orgEvents = eventDtos.map { it.toOrganizerEvent() }

                // Synchronize with in-memory store
                OrganizerEventStore.clear()
                for (evt in orgEvents) {
                    OrganizerEventStore.saveEvent(evt)
                }

                renderEvents(orgEvents)
            }.onFailure { error ->
                val cachedEvents = OrganizerEventStore.getEvents()
                if (cachedEvents.isNotEmpty()) {
                    renderEvents(cachedEvents)
                    Toast.makeText(
                        this@OrganizerDashboardActivity,
                        "Failed to refresh events: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showError(error.message ?: "Unable to load organizer events")
                }
            }
        }
    }

    private fun renderEvents(events: List<OrganizerEvent>) {
        layoutErrorState.visibility = View.GONE
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

    private fun showError(message: String) {
        layoutEmptyState.visibility = View.GONE
        rvOrganizerEvents.visibility = View.GONE
        fabCreateEvent.visibility = View.GONE
        layoutErrorState.visibility = View.VISIBLE
        tvErrorMessage.text = message
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
