package com.mpc.propass.organizer.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mpc.propass.ProPassApplication
import com.mpc.propass.R
import com.mpc.propass.network.model.RegistrationPurposeMapper
import com.mpc.propass.organizer.data.OrganizerRegistrationRepository
import com.mpc.propass.organizer.model.OrganizerRegistrationAnswerDto
import com.mpc.propass.organizer.model.OrganizerRegistrationDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Detailed view for an attendee's registration on an organizer-owned event.
 * Displays verified identity credentials, registration metadata, and dynamic form responses.
 */
class OrganizerRegistrationDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGISTRATION_ID = "extra_registration_id"
        const val EXTRA_REGISTRATION = "extra_registration"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvTopBarTitle: TextView
    private lateinit var tvTopBarSubtitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var scrollContent: NestedScrollView

    // Identity Views
    private lateinit var tvAttendeeName: TextView
    private lateinit var tvAttendeeEmail: TextView
    private lateinit var layoutPhone: View
    private lateinit var tvAttendeePhone: TextView
    private lateinit var tvAttendeeInstitution: TextView
    private lateinit var layoutTitle: View
    private lateinit var tvAttendeeTitle: TextView

    // Metadata Views
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvRegisteredAt: TextView
    private lateinit var tvPurpose: TextView
    private lateinit var tvDuration: TextView
    private lateinit var layoutVehicle: View
    private lateinit var tvVehicleNumber: TextView

    // Answers Views
    private lateinit var layoutAnswersContainer: LinearLayout
    private lateinit var tvNoAnswersFallback: TextView

    private var registrationId: String = ""
    private var initialRegistration: OrganizerRegistrationDto? = null
    private var repository: OrganizerRegistrationRepository? = null
    private var fetchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_organizer_registration_detail)

        val app = application as? ProPassApplication
        repository = app?.organizerRegistrationRepository

        resolveIntentData()
        initViews()
        applyWindowInsets()
        setupListeners()

        if (initialRegistration != null) {
            bindRegistration(initialRegistration!!)
            showContent()
        }

        if (registrationId.isNotBlank()) {
            loadRegistrationDetail()
        } else if (initialRegistration == null) {
            showError("Invalid registration ID")
        }
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
    }

    private fun applyWindowInsets() {
        val root = findViewById<View>(R.id.registrationDetailRoot) ?: return
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
        registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID) ?: ""
        initialRegistration = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_REGISTRATION, OrganizerRegistrationDto::class.java)
            } else {
                intent.getSerializableExtra(EXTRA_REGISTRATION) as? OrganizerRegistrationDto
            }
        } catch (_: Exception) {
            null
        }

        if (registrationId.isBlank() && initialRegistration != null) {
            registrationId = initialRegistration!!.id
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTopBarTitle = findViewById(R.id.tvTopBarTitle)
        tvTopBarSubtitle = findViewById(R.id.tvTopBarSubtitle)
        progressBar = findViewById(R.id.progressBar)
        layoutErrorState = findViewById(R.id.layoutErrorState)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        scrollContent = findViewById(R.id.scrollContent)

        tvAttendeeName = findViewById(R.id.tvAttendeeName)
        tvAttendeeEmail = findViewById(R.id.tvAttendeeEmail)
        layoutPhone = findViewById(R.id.layoutPhone)
        tvAttendeePhone = findViewById(R.id.tvAttendeePhone)
        tvAttendeeInstitution = findViewById(R.id.tvAttendeeInstitution)
        layoutTitle = findViewById(R.id.layoutTitle)
        tvAttendeeTitle = findViewById(R.id.tvAttendeeTitle)

        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvRegisteredAt = findViewById(R.id.tvRegisteredAt)
        tvPurpose = findViewById(R.id.tvPurpose)
        tvDuration = findViewById(R.id.tvDuration)
        layoutVehicle = findViewById(R.id.layoutVehicle)
        tvVehicleNumber = findViewById(R.id.tvVehicleNumber)

        layoutAnswersContainer = findViewById(R.id.layoutAnswersContainer)
        tvNoAnswersFallback = findViewById(R.id.tvNoAnswersFallback)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnRetry.setOnClickListener {
            loadRegistrationDetail()
        }
    }

    private fun loadRegistrationDetail() {
        val repo = repository ?: return

        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            if (initialRegistration == null) {
                showLoading()
            }

            val result = repo.getRegistrationDetail(registrationId)
            if (result.isSuccess) {
                val detail = result.getOrNull()
                if (detail != null) {
                    bindRegistration(detail)
                    showContent()
                } else if (initialRegistration == null) {
                    showError("Registration record not found")
                }
            } else {
                if (initialRegistration == null) {
                    val err = result.exceptionOrNull()?.message ?: "Failed to load registration details"
                    showError(err)
                }
            }
        }
    }

    private fun bindRegistration(registration: OrganizerRegistrationDto) {
        val displayName = registration.attendee?.fullName?.takeIf { it.isNotBlank() }
            ?: registration.fullName.takeIf { it.isNotBlank() }
            ?: "Anonymous Attendee"
        tvAttendeeName.text = displayName
        tvTopBarSubtitle.text = displayName

        val email = registration.attendee?.email?.takeIf { it.isNotBlank() }
            ?: registration.email.takeIf { it.isNotBlank() }
            ?: "Not provided"
        tvAttendeeEmail.text = email

        val phone = registration.attendee?.phone?.takeIf { it.isNotBlank() }
            ?: registration.phone?.takeIf { it.isNotBlank() }
            ?: "Not provided"
        tvAttendeePhone.text = phone

        val institution = registration.attendee?.organization?.takeIf { it.isNotBlank() }
            ?: registration.attendee?.institution?.takeIf { it.isNotBlank() }
            ?: registration.institution.takeIf { it.isNotBlank() }
            ?: "Not provided"
        tvAttendeeInstitution.text = institution

        val title = registration.attendee?.title?.takeIf { it.isNotBlank() }
        if (title != null) {
            layoutTitle.visibility = View.VISIBLE
            tvAttendeeTitle.text = title
        } else {
            layoutTitle.visibility = View.GONE
        }

        // Metadata
        val status = registration.status.uppercase()
        tvStatusBadge.text = status
        when (status) {
            "CONFIRMED" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed_pill)
                tvStatusBadge.setTextColor(Color.parseColor("#137333"))
            }
            "PENDING" -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending_pill)
                tvStatusBadge.setTextColor(Color.parseColor("#B06000"))
            }
            else -> {
                tvStatusBadge.setBackgroundResource(R.drawable.bg_org_pill)
                tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.primary))
            }
        }

        tvRegisteredAt.text = "Registered: ${formatTimestamp(registration.registeredAt)}"
        tvPurpose.text = "Purpose: ${RegistrationPurposeMapper.toFriendlyDisplay(registration.purpose)}"
        tvDuration.text = "Access Duration: ${registration.durationDays} Day(s)"

        if (!registration.vehicleNumber.isNullOrBlank()) {
            layoutVehicle.visibility = View.VISIBLE
            tvVehicleNumber.text = "Vehicle: ${registration.vehicleNumber}"
        } else {
            layoutVehicle.visibility = View.GONE
        }

        // Form answers
        bindAnswers(registration.answers)
    }

    private fun bindAnswers(answers: List<OrganizerRegistrationAnswerDto>) {
        layoutAnswersContainer.removeAllViews()

        if (answers.isEmpty()) {
            tvNoAnswersFallback.visibility = View.VISIBLE
            return
        }

        tvNoAnswersFallback.visibility = View.GONE

        answers.forEachIndexed { index, answer ->
            val answerView = createAnswerRowView(answer)
            layoutAnswersContainer.addView(answerView)

            if (index < answers.size - 1) {
                val divider = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dpToPx(1)
                    ).apply {
                        topMargin = dpToPx(12)
                        bottomMargin = dpToPx(12)
                    }
                    setBackgroundColor(ContextCompat.getColor(this@OrganizerRegistrationDetailActivity, R.color.divider_color))
                }
                layoutAnswersContainer.addView(divider)
            }
        }
    }

    private fun createAnswerRowView(answer: OrganizerRegistrationAnswerDto): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Header: Label + Question Type Badge
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(4)
            }
        }

        val tvLabel = TextView(this).apply {
            text = answer.questionLabel.ifBlank { "Question" }
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@OrganizerRegistrationDetailActivity, R.color.on_surface))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvLabel)

        val friendlyType = when (answer.questionType.uppercase()) {
            "SHORT_TEXT" -> "Short Text"
            "LONG_TEXT" -> "Long Text"
            "MULTIPLE_CHOICE" -> "Multiple Choice"
            "CHECKBOX" -> "Checkbox"
            else -> answer.questionType
        }

        val tvType = TextView(this).apply {
            text = friendlyType
            textSize = 11f
            setTextColor(ContextCompat.getColor(this@OrganizerRegistrationDetailActivity, R.color.secondary))
            setBackgroundResource(R.drawable.bg_org_pill)
            setPadding(dpToPx(8), dpToPx(2), dpToPx(8), dpToPx(2))
        }
        headerRow.addView(tvType)
        container.addView(headerRow)

        // Value
        val tvValue = TextView(this).apply {
            val rawValue = answer.value.trim()
            if (rawValue.isBlank()) {
                text = "Not provided"
                setTextColor(ContextCompat.getColor(this@OrganizerRegistrationDetailActivity, R.color.outline))
                setTypeface(typeface, Typeface.ITALIC)
            } else {
                // If checkbox with comma separated values, format nicely
                if (answer.questionType.equals("CHECKBOX", ignoreCase = true) && rawValue.contains(",")) {
                    val items = rawValue.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    text = items.joinToString("\n") { "• $it" }
                } else {
                    text = rawValue
                }
                setTextColor(ContextCompat.getColor(this@OrganizerRegistrationDetailActivity, R.color.on_surface_variant))
                setTypeface(typeface, Typeface.NORMAL)
            }
            textSize = 14f
            setLineSpacing(dpToPx(2).toFloat(), 1f)
        }
        container.addView(tvValue)

        return container
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun formatTimestamp(isoString: String): String {
        if (isoString.isBlank()) return "Unknown"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(isoString) ?: return isoString
            val outputFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (_: Exception) {
            try {
                val inputFormat2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = inputFormat2.parse(isoString) ?: return isoString
                val outputFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
                outputFormat.format(date)
            } catch (_: Exception) {
                isoString
            }
        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scrollContent.visibility = View.GONE
        layoutErrorState.visibility = View.GONE
    }

    private fun showContent() {
        progressBar.visibility = View.GONE
        scrollContent.visibility = View.VISIBLE
        layoutErrorState.visibility = View.GONE
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        scrollContent.visibility = View.GONE
        layoutErrorState.visibility = View.VISIBLE
        tvErrorMessage.text = message
    }
}
