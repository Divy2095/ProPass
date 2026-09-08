package com.mpc.propass

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.mpc.propass.data.repository.RegistrationRepository
import com.mpc.propass.network.model.RegistrationAnswerDto
import com.mpc.propass.network.model.RegistrationDto
import com.mpc.propass.network.model.RegistrationPurposeMapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Attendee Registration & Event Details Screen:
 * Displays complete event metadata, attendee registration status, duration, purpose,
 * and attendee's responses to custom event form questions.
 */
class AttendeeRegistrationDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGISTRATION_ID = "extra_registration_id"
        const val EXTRA_REGISTRATION = "extra_registration"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_EVENT_SLUG = "extra_event_slug"
    }

    private lateinit var rootLayout: View
    private lateinit var scrollContent: NestedScrollView
    private lateinit var topBarSpacer: View
    private lateinit var bottomBarSpacer: View
    private lateinit var topAppBar: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvTopBarTitle: TextView
    private lateinit var tvTopBarSubtitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutErrorState: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton

    // Header Views
    private lateinit var tvHeaderCategory: TextView
    private lateinit var tvHeaderEventTitle: TextView
    private lateinit var tvRegistrationStatusBadge: TextView
    private lateinit var tvEventStatusBadge: TextView

    // Event Info Views
    private lateinit var cardEventDetails: MaterialCardView
    private lateinit var layoutEventDescription: View
    private lateinit var tvEventDescription: TextView
    private lateinit var layoutEventDate: View
    private lateinit var tvEventDate: TextView
    private lateinit var layoutEventTime: View
    private lateinit var tvEventTime: TextView
    private lateinit var layoutEventLocation: View
    private lateinit var tvEventLocation: TextView

    // Registration Views
    private lateinit var cardRegistrationDetails: MaterialCardView
    private lateinit var tvAttendeeName: TextView
    private lateinit var tvAttendeeEmail: TextView
    private lateinit var tvAttendeeInstitution: TextView
    private lateinit var tvRegisteredAt: TextView
    private lateinit var tvDuration: TextView
    private lateinit var layoutPurpose: View
    private lateinit var tvPurpose: TextView
    private lateinit var layoutVehicle: View
    private lateinit var tvVehicleNumber: TextView

    // Responses Views
    private lateinit var cardResponses: MaterialCardView
    private lateinit var tvResponsesHeader: TextView
    private lateinit var layoutDynamicResponsesContainer: LinearLayout
    private lateinit var tvNoResponsesFallback: TextView

    // Actions
    private lateinit var btnViewDigitalPass: MaterialButton
    private lateinit var btnBackToDashboard: MaterialButton

    private var registrationId: String = ""
    private var eventId: String = ""
    private var initialRegistration: RegistrationDto? = null
    private var fetchJob: Job? = null

    private val registrationRepository: RegistrationRepository by lazy {
        (application as ProPassApplication).registrationRepository
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_attendee_registration_detail)

        resolveIntentData()
        initViews()
        applyWindowInsets()
        setupListeners()

        if (initialRegistration != null) {
            bindRegistration(initialRegistration!!)
            showContent()
        }

        if (registrationId.isNotBlank() || eventId.isNotBlank()) {
            loadRegistrationDetails()
        } else if (initialRegistration == null) {
            showError("Registration record could not be found.")
        }
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

    @Suppress("DEPRECATION")
    private fun resolveIntentData() {
        registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID).orEmpty()
        eventId = intent.getStringExtra(EXTRA_EVENT_ID)
            ?: intent.getStringExtra(EXTRA_EVENT_SLUG).orEmpty()

        initialRegistration = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra(EXTRA_REGISTRATION, RegistrationDto::class.java)
            } else {
                intent.getSerializableExtra(EXTRA_REGISTRATION) as? RegistrationDto
            }
        } catch (_: Exception) {
            null
        }

        if (registrationId.isBlank() && initialRegistration != null) {
            registrationId = initialRegistration!!.id
        }
        if (eventId.isBlank() && initialRegistration != null) {
            eventId = initialRegistration!!.eventId
        }
    }

    private fun initViews() {
        rootLayout = findViewById(R.id.attendeeRegistrationDetailRoot)
        scrollContent = findViewById(R.id.scrollContent)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        topAppBar = findViewById(R.id.topAppBar)
        btnBack = findViewById(R.id.btnBack)
        tvTopBarTitle = findViewById(R.id.tvTopBarTitle)
        tvTopBarSubtitle = findViewById(R.id.tvTopBarSubtitle)
        progressBar = findViewById(R.id.progressBar)
        layoutErrorState = findViewById(R.id.layoutErrorState)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)

        tvHeaderCategory = findViewById(R.id.tvHeaderCategory)
        tvHeaderEventTitle = findViewById(R.id.tvHeaderEventTitle)
        tvRegistrationStatusBadge = findViewById(R.id.tvRegistrationStatusBadge)
        tvEventStatusBadge = findViewById(R.id.tvEventStatusBadge)

        cardEventDetails = findViewById(R.id.cardEventDetails)
        layoutEventDescription = findViewById(R.id.layoutEventDescription)
        tvEventDescription = findViewById(R.id.tvEventDescription)
        layoutEventDate = findViewById(R.id.layoutEventDate)
        tvEventDate = findViewById(R.id.tvEventDate)
        layoutEventTime = findViewById(R.id.layoutEventTime)
        tvEventTime = findViewById(R.id.tvEventTime)
        layoutEventLocation = findViewById(R.id.layoutEventLocation)
        tvEventLocation = findViewById(R.id.tvEventLocation)

        cardRegistrationDetails = findViewById(R.id.cardRegistrationDetails)
        tvAttendeeName = findViewById(R.id.tvAttendeeName)
        tvAttendeeEmail = findViewById(R.id.tvAttendeeEmail)
        tvAttendeeInstitution = findViewById(R.id.tvAttendeeInstitution)
        tvRegisteredAt = findViewById(R.id.tvRegisteredAt)
        tvDuration = findViewById(R.id.tvDuration)
        layoutPurpose = findViewById(R.id.layoutPurpose)
        tvPurpose = findViewById(R.id.tvPurpose)
        layoutVehicle = findViewById(R.id.layoutVehicle)
        tvVehicleNumber = findViewById(R.id.tvVehicleNumber)

        cardResponses = findViewById(R.id.cardResponses)
        tvResponsesHeader = findViewById(R.id.tvResponsesHeader)
        layoutDynamicResponsesContainer = findViewById(R.id.layoutDynamicResponsesContainer)
        tvNoResponsesFallback = findViewById(R.id.tvNoResponsesFallback)

        btnViewDigitalPass = findViewById(R.id.btnViewDigitalPass)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            topAppBar.setPadding(0, systemBars.top, 0, 0)
            val topSpacerParams = topBarSpacer.layoutParams
            topSpacerParams.height = dpToPx(64f).toInt() + systemBars.top
            topBarSpacer.layoutParams = topSpacerParams

            val bottomSpacerParams = bottomBarSpacer.layoutParams
            bottomSpacerParams.height = dpToPx(40f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBackToDashboard.setOnClickListener {
            finish()
        }

        btnViewDigitalPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java)
            startActivity(intent)
        }

        btnRetry.setOnClickListener {
            loadRegistrationDetails()
        }
    }

    private fun loadRegistrationDetails() {
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch {
            if (initialRegistration == null) {
                showLoading()
            }

            val result = if (registrationId.isNotBlank()) {
                registrationRepository.getRegistrationById(registrationId)
            } else {
                // Lookup in all user registrations
                val myRegs = registrationRepository.getMyRegistrations()
                if (myRegs.isSuccess) {
                    val match = myRegs.getOrNull()?.find { it.eventId == eventId || it.event?.slug == eventId }
                    if (match != null) Result.success(match)
                    else Result.failure(Exception("Registration not found for event $eventId"))
                } else {
                    Result.failure(myRegs.exceptionOrNull() ?: Exception("Failed to load registrations"))
                }
            }

            result.onSuccess { reg ->
                bindRegistration(reg)
                showContent()
            }.onFailure { error ->
                if (initialRegistration != null) {
                    Toast.makeText(
                        this@AttendeeRegistrationDetailActivity,
                        "Using cached details. Server refresh failed: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showError(error.message ?: "Couldn't load your registration data. Check your connection and try again.")
                }
            }
        }
    }

    private fun bindRegistration(reg: RegistrationDto) {
        val event = reg.event
        val eventTitle = event?.title?.takeIf { it.isNotBlank() } ?: "Event Registration"

        // Top App Bar
        tvTopBarSubtitle.text = eventTitle
        tvHeaderEventTitle.text = eventTitle

        val category = event?.overline?.takeIf { it.isNotBlank() } ?: "EVENT REGISTRATION"
        tvHeaderCategory.text = category

        // Status Badges
        val regStatus = reg.status.ifBlank { "CONFIRMED" }
        tvRegistrationStatusBadge.text = regStatus
        if (regStatus.equals("CONFIRMED", ignoreCase = true)) {
            tvRegistrationStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed_pill)
            tvRegistrationStatusBadge.setTextColor(Color.parseColor("#137333"))
        } else {
            tvRegistrationStatusBadge.setBackgroundResource(R.drawable.bg_tag_chip)
            tvRegistrationStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.on_surface_variant))
        }

        tvEventStatusBadge.text = if (event?.isActive == false) "INACTIVE EVENT" else "ACTIVE EVENT"

        // 1. EVENT CARD
        // Description: Only shown if backend provides one
        val desc = event?.description?.trim()
        if (!desc.isNullOrBlank()) {
            layoutEventDescription.visibility = View.VISIBLE
            tvEventDescription.text = desc
        } else {
            layoutEventDescription.visibility = View.GONE
        }

        // Event Date
        val dateText = event?.date?.takeIf { it.isNotBlank() }
            ?: formatIsoDate(event?.startDate)
            ?: "Date not specified"
        tvEventDate.text = dateText

        // Event Time
        val startTime = event?.startTime?.trim()
        val endTime = event?.endTime?.trim()
        when {
            !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> {
                layoutEventTime.visibility = View.VISIBLE
                tvEventTime.text = "$startTime - $endTime"
            }
            !startTime.isNullOrBlank() -> {
                layoutEventTime.visibility = View.VISIBLE
                tvEventTime.text = startTime
            }
            else -> {
                layoutEventTime.visibility = View.GONE
            }
        }

        // Event Location
        val locationText = event?.location?.trim()
        if (!locationText.isNullOrBlank()) {
            layoutEventLocation.visibility = View.VISIBLE
            tvEventLocation.text = locationText
        } else {
            layoutEventLocation.visibility = View.GONE
        }

        // 2. REGISTRATION CARD
        tvAttendeeName.text = reg.fullName.ifBlank { "Attendee" }
        tvAttendeeEmail.text = reg.email.ifBlank { "Email not provided" }
        tvAttendeeInstitution.text = reg.institution.ifBlank { "Institution not provided" }
        tvRegisteredAt.text = formatIsoDateTime(reg.registeredAt)

        val durationDays = reg.durationDays.coerceAtLeast(1)
        tvDuration.text = "$durationDays Day${if (durationDays == 1) "" else "s"}"

        val purposeDisplay = RegistrationPurposeMapper.toFriendlyDisplay(reg.purpose)
        if (purposeDisplay.isNotBlank()) {
            layoutPurpose.visibility = View.VISIBLE
            tvPurpose.text = purposeDisplay
        } else {
            layoutPurpose.visibility = View.GONE
        }

        val vehicle = reg.vehicleNumber?.trim()
        if (!vehicle.isNullOrBlank()) {
            layoutVehicle.visibility = View.VISIBLE
            tvVehicleNumber.text = vehicle
        } else {
            layoutVehicle.visibility = View.GONE
        }

        // 3. YOUR RESPONSES CARD
        bindCustomResponses(reg.answers)
    }

    private fun bindCustomResponses(answers: List<RegistrationAnswerDto>) {
        layoutDynamicResponsesContainer.removeAllViews()

        // Filter out default identity questions that are already shown in identity section
        val filteredAnswers = answers.filterNot { ans ->
            ans.questionId == "default-full-name" ||
            ans.questionId == "default-email" ||
            ans.displayLabel.equals("Full Name", ignoreCase = true) ||
            ans.displayLabel.equals("Email Address", ignoreCase = true)
        }

        if (filteredAnswers.isEmpty()) {
            tvNoResponsesFallback.visibility = View.VISIBLE
            layoutDynamicResponsesContainer.visibility = View.GONE
            return
        }

        tvNoResponsesFallback.visibility = View.GONE
        layoutDynamicResponsesContainer.visibility = View.VISIBLE

        for ((index, ans) in filteredAnswers.withIndex()) {
            val itemView = createAnswerView(ans, isLast = index == filteredAnswers.size - 1)
            layoutDynamicResponsesContainer.addView(itemView)
        }
    }

    private fun createAnswerView(answer: RegistrationAnswerDto, isLast: Boolean): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (!isLast) bottomMargin = dpToPx(14f).toInt()
            }
            layoutParams = params
        }

        // Question Label
        val tvLabel = TextView(this).apply {
            val labelText = if (answer.isRequired) {
                "${answer.displayLabel} *"
            } else {
                answer.displayLabel
            }
            text = labelText
            setTextColor(ContextCompat.getColor(this@AttendeeRegistrationDetailActivity, R.color.on_surface_variant))
            textSize = 12f
            val font = ResourcesCompat.getFont(this@AttendeeRegistrationDetailActivity, R.font.inter_medium)
            if (font != null) typeface = font
            includeFontPadding = false
        }
        container.addView(tvLabel)

        // Submitted Answer
        val tvValue = TextView(this).apply {
            val valueText = answer.value.trim().ifBlank { "Not provided" }
            text = valueText
            setTextColor(ContextCompat.getColor(this@AttendeeRegistrationDetailActivity, R.color.on_surface))
            textSize = 14f
            val font = ResourcesCompat.getFont(this@AttendeeRegistrationDetailActivity, R.font.inter_regular)
            if (font != null) typeface = font
            includeFontPadding = false
            setLineSpacing(dpToPx(2f), 1.0f)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(3f).toInt()
            }
            layoutParams = params
        }
        container.addView(tvValue)

        return container
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

    private fun formatIsoDate(isoString: String?): String? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(isoString) ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(isoString)

            date?.let {
                val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                outputFormat.format(it)
            }
        } catch (_: Exception) {
            isoString
        }
    }

    private fun formatIsoDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Just now"
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = inputFormat.parse(isoString) ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(isoString)

            date?.let {
                val outputFormat = SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault())
                outputFormat.format(it)
            } ?: isoString
        } catch (_: Exception) {
            isoString
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
