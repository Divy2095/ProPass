package com.mpc.propass

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Patterns
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mpc.propass.network.model.EventDto

/**
 * Smart Form Registration screen implementation for ProPass Digital Identity System.
 * Handles local form validation, dynamic event header setup, and navigation to Review screen.
 */
class SmartFormRegistrationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE"
        const val EXTRA_EVENT_OVERLINE = "EXTRA_EVENT_OVERLINE"
        const val EXTRA_EVENT_SUBTITLE = "EXTRA_EVENT_SUBTITLE"
        const val EXTRA_EVENT_LOCATION = "EXTRA_EVENT_LOCATION"
        const val EXTRA_EVENT_MAX_DURATION = "EXTRA_EVENT_MAX_DURATION"
        const val EXTRA_EVENT_DTO = "EXTRA_EVENT_DTO"
        const val EXTRA_SCANNED_QR = "EXTRA_SCANNED_QR"
    }

    private lateinit var smartFormRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var stickyBottomBar: FrameLayout
    private lateinit var bottomBarSpacer: View
    private lateinit var btnBack: FrameLayout

    private lateinit var tvOverline: TextView
    private lateinit var tvEventTitle: TextView
    private lateinit var tvEventSubtitle: TextView

    private lateinit var rowFullName: LinearLayout
    private lateinit var tvValFullName: TextView
    private lateinit var tvErrorFullName: TextView

    private lateinit var rowEmail: LinearLayout
    private lateinit var tvValEmail: TextView
    private lateinit var tvErrorEmail: TextView

    private lateinit var rowInstitution: LinearLayout
    private lateinit var tvValInstitution: TextView
    private lateinit var tvErrorInstitution: TextView

    private lateinit var layoutPurposeSelect: FrameLayout
    private lateinit var tvSelectedPurpose: TextView
    private lateinit var tvErrorPurpose: TextView

    private lateinit var etDuration: EditText
    private lateinit var tvErrorDuration: TextView

    private lateinit var layoutVehicleContainer: FrameLayout
    private lateinit var etVehicle: EditText

    private lateinit var btnReviewSubmit: MaterialButton

    private var currentEventId: String = "techconf-2024"
    private var currentEventName: String = "TechConf 2024"
    private var maxDuration: Int = 5

    private val purposeOptions by lazy {
        arrayOf(
            getString(R.string.purpose_general_attendee),
            getString(R.string.purpose_speaker),
            getString(R.string.purpose_sponsor),
            getString(R.string.purpose_media)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_smart_form_registration)

        initViews()
        applyWindowInsets()
        setupEventHeader()
        setupRowInteractions()
        setupPurposeDropdown()
        setupInputBehaviors()
        setupSubmitButton()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        smartFormRoot = findViewById(R.id.smartFormRoot)
        topAppBar = findViewById(R.id.topAppBar)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        stickyBottomBar = findViewById(R.id.stickyBottomBar)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        btnBack = findViewById(R.id.btnBack)

        tvOverline = findViewById(R.id.tvOverline)
        tvEventTitle = findViewById(R.id.tvEventTitle)
        tvEventSubtitle = findViewById(R.id.tvEventSubtitle)

        rowFullName = findViewById(R.id.rowFullName)
        tvValFullName = findViewById(R.id.tvValFullName)
        tvErrorFullName = findViewById(R.id.tvErrorFullName)

        rowEmail = findViewById(R.id.rowEmail)
        tvValEmail = findViewById(R.id.tvValEmail)
        tvErrorEmail = findViewById(R.id.tvErrorEmail)

        rowInstitution = findViewById(R.id.rowInstitution)
        tvValInstitution = findViewById(R.id.tvValInstitution)
        tvErrorInstitution = findViewById(R.id.tvErrorInstitution)

        layoutPurposeSelect = findViewById(R.id.layoutPurposeSelect)
        tvSelectedPurpose = findViewById(R.id.tvSelectedPurpose)
        tvErrorPurpose = findViewById(R.id.tvErrorPurpose)

        etDuration = findViewById(R.id.etDuration)
        tvErrorDuration = findViewById(R.id.tvErrorDuration)

        layoutVehicleContainer = findViewById(R.id.layoutVehicleContainer)
        etVehicle = findViewById(R.id.etVehicle)

        btnReviewSubmit = findViewById(R.id.btnReviewSubmit)
    }

    private fun setupEventHeader() {
        @Suppress("DEPRECATION")
        val eventDto = intent.getSerializableExtra(EXTRA_EVENT_DTO) as? EventDto
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: eventDto?.slug ?: eventDto?.id ?: "techconf-2024"
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: eventDto?.title
        val eventOverline = intent.getStringExtra(EXTRA_EVENT_OVERLINE) ?: eventDto?.overline
        val eventSubtitle = intent.getStringExtra(EXTRA_EVENT_SUBTITLE) ?: eventDto?.subtitle
        val eventMaxDur = intent.getIntExtra(EXTRA_EVENT_MAX_DURATION, eventDto?.maxDuration ?: 5)
        maxDuration = if (eventMaxDur > 0) eventMaxDur else 5

        currentEventId = eventId

        if (!eventTitle.isNullOrBlank()) {
            currentEventName = eventTitle
            tvEventTitle.text = eventTitle
            tvOverline.text = eventOverline ?: getString(R.string.event_registration_overline)
            tvEventSubtitle.text = eventSubtitle ?: getString(R.string.event_subtitle)
            return
        }

        when (eventId.lowercase()) {
            "techconf-2024" -> {
                currentEventName = getString(R.string.event_title) // TechConf 2024
                tvOverline.text = getString(R.string.event_registration_overline)
                tvEventTitle.text = currentEventName
                tvEventSubtitle.text = getString(R.string.event_subtitle)
            }
            "google-office-visit" -> {
                currentEventName = "Google Office Visit"
                tvOverline.text = "VISITOR PASS"
                tvEventTitle.text = currentEventName
                tvEventSubtitle.text = "Check in for your scheduled campus visit."
            }
            "android-conf-2026" -> {
                currentEventName = "Android Conf 2026"
                tvOverline.text = "CONFERENCE PASS"
                tvEventTitle.text = currentEventName
                tvEventSubtitle.text = "Secure your badge for Android developer sessions."
            }
            else -> {
                currentEventName = eventId.split("-", "_").joinToString(" ") { segment ->
                    segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                tvOverline.text = "EVENT REGISTRATION"
                tvEventTitle.text = currentEventName
                tvEventSubtitle.text = "Complete your registration to secure your spot."
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(smartFormRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            // Top App Bar insets
            topAppBar.setPadding(
                dpToPx(20f).toInt(),
                systemBars.top,
                dpToPx(20f).toInt(),
                0
            )

            val topSpacerParams = topBarSpacer.layoutParams
            topSpacerParams.height = dpToPx(64f).toInt() + systemBars.top
            topBarSpacer.layoutParams = topSpacerParams

            // Sticky Bottom Bar insets
            val bottomPadding = dpToPx(16f).toInt() + systemBars.bottom
            stickyBottomBar.setPadding(
                dpToPx(20f).toInt(),
                dpToPx(16f).toInt(),
                dpToPx(20f).toInt(),
                bottomPadding
            )

            val bottomSpacerParams = bottomBarSpacer.layoutParams
            bottomSpacerParams.height = dpToPx(88f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    private fun setupRowInteractions() {
        // Back Button
        btnBack.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }
        btnBack.setOnClickListener {
            finish()
        }

        // Auto-filled rows tactile and edit feedback
        val rowTouchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        rowFullName.setOnTouchListener(rowTouchListener)
        rowFullName.setOnClickListener {
            showEditFieldDialog(
                title = getString(R.string.label_full_name),
                currentValue = tvValFullName.text.toString(),
                onValueConfirmed = { newValue ->
                    tvValFullName.text = newValue
                    clearFullNameError()
                }
            )
        }

        rowEmail.setOnTouchListener(rowTouchListener)
        rowEmail.setOnClickListener {
            showEditFieldDialog(
                title = getString(R.string.label_email_address),
                currentValue = tvValEmail.text.toString(),
                onValueConfirmed = { newValue ->
                    tvValEmail.text = newValue
                    clearEmailError()
                }
            )
        }

        rowInstitution.setOnTouchListener(rowTouchListener)
        rowInstitution.setOnClickListener {
            showEditFieldDialog(
                title = getString(R.string.label_institution),
                currentValue = tvValInstitution.text.toString(),
                onValueConfirmed = { newValue ->
                    tvValInstitution.text = newValue
                    clearInstitutionError()
                }
            )
        }
    }

    private fun showEditFieldDialog(
        title: String,
        currentValue: String,
        onValueConfirmed: (String) -> Unit
    ) {
        val input = EditText(this).apply {
            setText(currentValue)
            setSelection(text.length)
            setPadding(dpToPx(16f).toInt(), dpToPx(12f).toInt(), dpToPx(16f).toInt(), dpToPx(12f).toInt())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val text = input.text.toString().trim()
                onValueConfirmed(text)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupPurposeDropdown() {
        layoutPurposeSelect.setOnClickListener {
            layoutPurposeSelect.setBackgroundResource(R.drawable.bg_field_focused)
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.label_purpose_of_visit))
                .setItems(purposeOptions) { _, which ->
                    tvSelectedPurpose.text = purposeOptions[which]
                    clearPurposeError()
                    layoutPurposeSelect.setBackgroundResource(R.drawable.bg_field_normal)
                }
                .setOnDismissListener {
                    if (tvErrorPurpose.visibility != View.VISIBLE) {
                        layoutPurposeSelect.setBackgroundResource(R.drawable.bg_field_normal)
                    }
                }
                .show()
        }
    }

    private fun setupInputBehaviors() {
        // Enforce max 2 digits for Expected Duration
        etDuration.filters = arrayOf(InputFilter.LengthFilter(2))

        // Auto clear duration error on text change
        etDuration.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearDurationError()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Auto uppercase for Vehicle Number
        etVehicle.filters = arrayOf(InputFilter.AllCaps())

        // Focus styling for vehicle container
        etVehicle.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                layoutVehicleContainer.setBackgroundResource(R.drawable.bg_field_focused)
            } else {
                layoutVehicleContainer.setBackgroundResource(R.drawable.bg_field_normal)
            }
        }
    }

    private fun setupSubmitButton() {
        btnReviewSubmit.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        btnReviewSubmit.setOnClickListener { view ->
            if (validateForm()) {
                val fullName = tvValFullName.text.toString().trim()
                val email = tvValEmail.text.toString().trim()
                val institution = tvValInstitution.text.toString().trim()
                val purpose = tvSelectedPurpose.text.toString().trim()
                val duration = etDuration.text.toString().trim().toInt()
                val vehicle = etVehicle.text.toString().trim().takeIf { it.isNotBlank() }

                val registrationData = RegistrationData(
                    eventId = currentEventId,
                    eventName = currentEventName,
                    fullName = fullName,
                    email = email,
                    institution = institution,
                    purpose = purpose,
                    durationDays = duration,
                    vehicleNumber = vehicle
                )

                val intent = Intent(this, ReviewRegistrationActivity::class.java).apply {
                    putExtra(ReviewRegistrationActivity.EXTRA_REGISTRATION_DATA, registrationData)
                }
                startActivity(intent)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            }
        }
    }

    /**
     * Validates all required fields according to rules:
     * - Full Name: not blank
     * - Email: not blank & valid email regex
     * - Institution: not blank
     * - Purpose: must be selected from options
     * - Expected Duration: integer between 1 and 5
     * - Vehicle Number: optional
     */
    private fun validateForm(): Boolean {
        var isValid = true

        // 1. Full Name validation
        val fullName = tvValFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            tvErrorFullName.text = getString(R.string.error_full_name_required)
            tvErrorFullName.visibility = View.VISIBLE
            rowFullName.setBackgroundResource(R.drawable.bg_autofill_row_error)
            isValid = false
        } else {
            clearFullNameError()
        }

        // 2. Email Address validation
        val email = tvValEmail.text.toString().trim()
        if (email.isEmpty()) {
            tvErrorEmail.text = getString(R.string.error_email_required)
            tvErrorEmail.visibility = View.VISIBLE
            rowEmail.setBackgroundResource(R.drawable.bg_autofill_row_error)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tvErrorEmail.text = getString(R.string.error_email_invalid)
            tvErrorEmail.visibility = View.VISIBLE
            rowEmail.setBackgroundResource(R.drawable.bg_autofill_row_error)
            isValid = false
        } else {
            clearEmailError()
        }

        // 3. Institution validation
        val institution = tvValInstitution.text.toString().trim()
        if (institution.isEmpty()) {
            tvErrorInstitution.text = getString(R.string.error_institution_required)
            tvErrorInstitution.visibility = View.VISIBLE
            rowInstitution.setBackgroundResource(R.drawable.bg_autofill_row_error)
            isValid = false
        } else {
            clearInstitutionError()
        }

        // 4. Purpose of Visit validation
        val purpose = tvSelectedPurpose.text.toString().trim()
        if (purpose.isEmpty() || purpose == getString(R.string.hint_select_purpose) || purpose !in purposeOptions) {
            tvErrorPurpose.text = getString(R.string.error_purpose_required)
            tvErrorPurpose.visibility = View.VISIBLE
            layoutPurposeSelect.setBackgroundResource(R.drawable.bg_field_error)
            isValid = false
        } else {
            clearPurposeError()
        }

        // 5. Expected Duration validation (1 to maxDuration days)
        val durationStr = etDuration.text.toString().trim()
        val duration = durationStr.toIntOrNull()
        if (duration == null || duration < 1 || duration > maxDuration) {
            tvErrorDuration.text = if (maxDuration == 5) {
                getString(R.string.error_duration_invalid)
            } else if (maxDuration == 1) {
                "Enter a duration of 1 day"
            } else {
                "Enter a duration between 1 and $maxDuration days"
            }
            tvErrorDuration.visibility = View.VISIBLE
            etDuration.setBackgroundResource(R.drawable.bg_field_error)
            isValid = false
        } else {
            clearDurationError()
        }

        return isValid
    }

    private fun clearFullNameError() {
        tvErrorFullName.visibility = View.GONE
        rowFullName.setBackgroundResource(R.drawable.bg_autofill_row)
    }

    private fun clearEmailError() {
        tvErrorEmail.visibility = View.GONE
        rowEmail.setBackgroundResource(R.drawable.bg_autofill_row)
    }

    private fun clearInstitutionError() {
        tvErrorInstitution.visibility = View.GONE
        rowInstitution.setBackgroundResource(R.drawable.bg_autofill_row)
    }

    private fun clearPurposeError() {
        tvErrorPurpose.visibility = View.GONE
        layoutPurposeSelect.setBackgroundResource(R.drawable.bg_field_normal)
    }

    private fun clearDurationError() {
        tvErrorDuration.visibility = View.GONE
        etDuration.setBackgroundResource(R.drawable.bg_field_normal)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
