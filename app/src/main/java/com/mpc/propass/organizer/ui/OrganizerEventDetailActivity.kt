package com.mpc.propass.organizer.ui

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mpc.propass.ProPassApplication
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerRegistrationRepository
import com.mpc.propass.organizer.model.OrganizerEvent
import com.mpc.propass.organizer.model.OrganizerEventDraft
import kotlinx.coroutines.launch

/**
 * Organizer Event Detail / Management screen.
 * Displays published event details, current registration count,
 * and provides access to view registrations, event QR pass, and form details.
 */
class OrganizerEventDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT = "extra_organizer_event"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvTopBarTitle: TextView
    private lateinit var tvEventStatusBadge: TextView
    private lateinit var tvEventSlug: TextView
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventDescription: TextView
    private lateinit var tvEventDateTime: TextView
    private lateinit var tvEventLocation: TextView
    private lateinit var tvEventDuration: TextView
    private lateinit var tvRegistrationCountBadge: TextView
    private lateinit var btnViewRegistrations: MaterialButton
    private lateinit var btnViewQrCode: MaterialButton
    private lateinit var btnEditForm: MaterialButton

    private var event: OrganizerEvent? = null
    private var registrationRepository: OrganizerRegistrationRepository? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_organizer_event_detail)

        val app = application as? ProPassApplication
        registrationRepository = app?.organizerRegistrationRepository

        resolveIntentData()
        initViews()
        bindEventData()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshRegistrationCount()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true

        val root = findViewById<View>(R.id.eventDetailRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            findViewById<View>(R.id.appBarLayout)?.setPadding(0, statusBars.top, 0, 0)
            view.setPadding(0, 0, 0, navBars.bottom)
            insets
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveIntentData() {
        event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EVENT, OrganizerEvent::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_EVENT) as? OrganizerEvent
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTopBarTitle = findViewById(R.id.tvTopBarTitle)
        tvEventStatusBadge = findViewById(R.id.tvEventStatusBadge)
        tvEventSlug = findViewById(R.id.tvEventSlug)
        tvEventTitle = findViewById(R.id.tvEventTitle)
        tvEventDescription = findViewById(R.id.tvEventDescription)
        tvEventDateTime = findViewById(R.id.tvEventDateTime)
        tvEventLocation = findViewById(R.id.tvEventLocation)
        tvEventDuration = findViewById(R.id.tvEventDuration)
        tvRegistrationCountBadge = findViewById(R.id.tvRegistrationCountBadge)
        btnViewRegistrations = findViewById(R.id.btnViewRegistrations)
        btnViewQrCode = findViewById(R.id.btnViewQrCode)
        btnEditForm = findViewById(R.id.btnEditForm)
    }

    private fun bindEventData() {
        val current = event ?: return

        tvTopBarTitle.text = current.name
        tvEventTitle.text = current.name
        tvEventStatusBadge.text = current.status
        tvEventSlug.text = current.slug

        if (current.description.isNotBlank()) {
            tvEventDescription.visibility = View.VISIBLE
            tvEventDescription.text = current.description
        } else {
            tvEventDescription.visibility = View.GONE
        }

        tvEventDateTime.text = if (current.startTime.isNotBlank() && current.endTime.isNotBlank()) {
            "${current.date} • ${current.startTime} - ${current.endTime}"
        } else {
            current.date.ifBlank { "Date not specified" }
        }

        tvEventLocation.text = current.location.ifBlank { "Location not specified" }
        tvEventDuration.text = "Valid for ${current.maxDurationDays} day(s) access"

        val count = current.attendeeCount
        tvRegistrationCountBadge.text = "$count Registered"
    }

    private fun refreshRegistrationCount() {
        val current = event ?: return
        val repo = registrationRepository ?: return

        lifecycleScope.launch {
            val result = repo.getEventRegistrations(current.id)
            if (result.isSuccess) {
                val data = result.getOrNull()
                val count = data?.count ?: data?.registrations?.size ?: 0
                tvRegistrationCountBadge.text = "$count Registered"
            }
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnViewRegistrations.setOnClickListener {
            val current = event ?: return@setOnClickListener
            val intent = Intent(this, OrganizerRegistrationsActivity::class.java).apply {
                putExtra(OrganizerRegistrationsActivity.EXTRA_EVENT_ID, current.id)
                putExtra(OrganizerRegistrationsActivity.EXTRA_EVENT_TITLE, current.name)
            }
            startActivity(intent)
        }

        btnViewQrCode.setOnClickListener {
            val current = event ?: return@setOnClickListener
            val intent = Intent(this, EventPublishSuccessActivity::class.java).apply {
                putExtra(EventPublishSuccessActivity.EXTRA_EVENT, current)
                putExtra(EventPublishSuccessActivity.EXTRA_IS_VIEW_MODE, true)
            }
            startActivity(intent)
        }

        btnEditForm.setOnClickListener {
            val current = event ?: return@setOnClickListener
            val draft = OrganizerEventDraft(
                id = current.id,
                slug = current.slug,
                name = current.name,
                description = current.description,
                date = current.date,
                startTime = current.startTime,
                endTime = current.endTime,
                location = current.location,
                maxDurationDays = current.maxDurationDays,
                qrPayload = current.qrPayload,
                questions = current.questions.toMutableList()
            )
            val intent = Intent(this, FormBuilderActivity::class.java).apply {
                putExtra(FormBuilderActivity.EXTRA_DRAFT, draft)
            }
            startActivity(intent)
        }
    }
}
