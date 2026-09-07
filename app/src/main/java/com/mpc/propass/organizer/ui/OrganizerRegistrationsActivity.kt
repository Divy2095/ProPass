package com.mpc.propass.organizer.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mpc.propass.ProPassApplication
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerRegistrationRepository
import com.mpc.propass.organizer.model.OrganizerRegistrationDto
import com.mpc.propass.organizer.ui.adapter.OrganizerRegistrationAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Organizer Registrations List screen.
 * Displays all attendee registrations for a specific event owned by the authenticated organizer.
 */
class OrganizerRegistrationsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_TITLE = "extra_event_title"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvRegistrationsTitle: TextView
    private lateinit var tvEventSubtitle: TextView
    private lateinit var tvTotalCountBadge: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvRegistrations: RecyclerView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton

    private lateinit var adapter: OrganizerRegistrationAdapter
    private var repository: OrganizerRegistrationRepository? = null

    private var eventId: String = ""
    private var eventTitle: String = ""
    private var fetchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_organizer_registrations)

        val app = application as? ProPassApplication
        repository = app?.organizerRegistrationRepository

        eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
        eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""

        initViews()
        setupRecyclerView()
        setupListeners()

        if (eventTitle.isNotBlank()) {
            tvEventSubtitle.text = eventTitle
        }

        loadRegistrations()
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        val root = findViewById<View>(R.id.registrationsRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            findViewById<View>(R.id.appBarLayout)?.setPadding(0, statusBars.top, 0, 0)
            view.setPadding(0, 0, 0, navBars.bottom)
            insets
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvRegistrationsTitle = findViewById(R.id.tvRegistrationsTitle)
        tvEventSubtitle = findViewById(R.id.tvEventSubtitle)
        tvTotalCountBadge = findViewById(R.id.tvTotalCountBadge)
        progressBar = findViewById(R.id.progressBar)
        rvRegistrations = findViewById(R.id.rvRegistrations)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        layoutErrorState = findViewById(R.id.layoutErrorState)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
    }

    private fun setupRecyclerView() {
        adapter = OrganizerRegistrationAdapter { registration ->
            val intent = Intent(this, OrganizerRegistrationDetailActivity::class.java).apply {
                putExtra(OrganizerRegistrationDetailActivity.EXTRA_REGISTRATION_ID, registration.id)
                putExtra(OrganizerRegistrationDetailActivity.EXTRA_REGISTRATION, registration)
            }
            startActivity(intent)
        }
        rvRegistrations.layoutManager = LinearLayoutManager(this)
        rvRegistrations.adapter = adapter
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnRetry.setOnClickListener {
            loadRegistrations()
        }
    }

    private fun loadRegistrations() {
        if (eventId.isBlank()) {
            showError("Invalid event ID")
            return
        }

        val repo = repository
        if (repo == null) {
            showError("Registration repository not initialized")
            return
        }

        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            showLoading()

            val result = repo.getEventRegistrations(eventId)
            if (result.isSuccess) {
                val data = result.getOrNull()
                val list = data?.registrations ?: emptyList()
                val count = data?.count ?: list.size

                tvTotalCountBadge.text = "$count Total"
                if (data?.event?.title?.isNotBlank() == true) {
                    tvEventSubtitle.text = data.event.title
                }

                if (list.isEmpty()) {
                    showEmpty()
                } else {
                    showContent(list)
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to load registrations"
                showError(err)
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        rvRegistrations.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.GONE
    }

    private fun showContent(registrations: List<OrganizerRegistrationDto>) {
        progressBar.visibility = View.GONE
        rvRegistrations.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.GONE
        adapter.submitList(registrations)
    }

    private fun showEmpty() {
        progressBar.visibility = View.GONE
        rvRegistrations.visibility = View.GONE
        layoutEmptyState.visibility = View.VISIBLE
        layoutErrorState.visibility = View.GONE
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        rvRegistrations.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE
        layoutErrorState.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }
}
