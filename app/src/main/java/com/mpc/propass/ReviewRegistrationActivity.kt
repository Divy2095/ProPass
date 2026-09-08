package com.mpc.propass

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.mpc.propass.data.repository.DuplicateRegistrationException
import com.mpc.propass.data.repository.RegistrationAuthException
import com.mpc.propass.data.repository.RegistrationRepository
import com.mpc.propass.data.repository.RegistrationValidationException
import com.mpc.propass.network.model.RegistrationAnswerDto
import com.mpc.propass.network.model.RegistrationPurposeMapper
import kotlinx.coroutines.launch

/**
 * Review Registration screen for ProPass Digital Identity System.
 * Displays all validated registration details before final submission.
 */
class ReviewRegistrationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGISTRATION_DATA = "EXTRA_REGISTRATION_DATA"
    }

    private val registrationRepository: RegistrationRepository by lazy {
        (application as ProPassApplication).registrationRepository
    }

    private var isSubmitting: Boolean = false

    private lateinit var reviewRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var stickyBottomBar: LinearLayout
    private lateinit var bottomBarSpacer: View
    private lateinit var btnBack: FrameLayout

    private lateinit var tvReviewEventTitle: TextView
    private lateinit var tvReviewFullName: TextView
    private lateinit var tvReviewEmail: TextView
    private lateinit var tvReviewInstitution: TextView
    private lateinit var tvReviewPurpose: TextView
    private lateinit var tvReviewDuration: TextView
    private lateinit var tvReviewVehicle: TextView

    private lateinit var cardVisitDetails: View
    private lateinit var cardAdditionalDetails: View
    private lateinit var layoutDynamicAnswers: LinearLayout

    private lateinit var btnEditBack: MaterialButton
    private lateinit var btnSubmitRegistration: MaterialButton

    private var registrationData: RegistrationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_review_registration)

        initViews()
        applyWindowInsets()
        loadRegistrationData()
        setupInteractions()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        reviewRoot = findViewById(R.id.reviewRoot)
        topAppBar = findViewById(R.id.topAppBar)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        stickyBottomBar = findViewById(R.id.stickyBottomBar)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        btnBack = findViewById(R.id.btnBack)

        tvReviewEventTitle = findViewById(R.id.tvReviewEventTitle)
        tvReviewFullName = findViewById(R.id.tvReviewFullName)
        tvReviewEmail = findViewById(R.id.tvReviewEmail)
        tvReviewInstitution = findViewById(R.id.tvReviewInstitution)
        tvReviewPurpose = findViewById(R.id.tvReviewPurpose)
        tvReviewDuration = findViewById(R.id.tvReviewDuration)
        tvReviewVehicle = findViewById(R.id.tvReviewVehicle)

        cardVisitDetails = findViewById(R.id.cardVisitDetails)
        cardAdditionalDetails = findViewById(R.id.cardAdditionalDetails)
        layoutDynamicAnswers = findViewById(R.id.layoutDynamicAnswers)

        btnEditBack = findViewById(R.id.btnEditBack)
        btnSubmitRegistration = findViewById(R.id.btnSubmitRegistration)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(reviewRoot) { _, insets ->
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
            bottomSpacerParams.height = dpToPx(96f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    @Suppress("DEPRECATION")
    private fun loadRegistrationData() {
        val data = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_REGISTRATION_DATA, RegistrationData::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_REGISTRATION_DATA) as? RegistrationData
        }

        registrationData = data ?: run {
            Toast.makeText(this, "Registration details unavailable", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvReviewEventTitle.text = data.eventName
        tvReviewFullName.text = data.fullName
        tvReviewEmail.text = data.email
        tvReviewInstitution.text = data.institution

        val isDefaultOrEmptyLegacy = data.vehicleNumber.isNullOrBlank() &&
            (data.purpose.isBlank() || data.purpose.equals("General Attendee", ignoreCase = true) || data.purpose.equals("GENERAL_ATTENDEE", ignoreCase = true))

        if (isDefaultOrEmptyLegacy && data.answers.isNotEmpty()) {
            cardVisitDetails.visibility = View.GONE
        } else {
            cardVisitDetails.visibility = View.VISIBLE
            tvReviewPurpose.text = data.purpose
            tvReviewDuration.text = if (data.durationDays == 1) "1 day" else "${data.durationDays} days"
            tvReviewVehicle.text = data.vehicleNumber.takeIf { !it.isNullOrBlank() } ?: getString(R.string.review_vehicle_not_provided)
        }

        if (data.answers.isNotEmpty()) {
            cardAdditionalDetails.visibility = View.VISIBLE
            layoutDynamicAnswers.removeAllViews()
            data.answers.forEachIndexed { index, ans ->
                val rowLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvLabel = TextView(this).apply {
                    text = ans.questionLabel
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@ReviewRegistrationActivity, R.color.secondary))
                    textSize = 12f
                }
                rowLayout.addView(tvLabel)

                val tvValue = TextView(this).apply {
                    text = ans.value.ifBlank { "—" }
                    setTextColor(androidx.core.content.ContextCompat.getColor(this@ReviewRegistrationActivity, R.color.on_surface))
                    textSize = 15f
                    val params = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(2f).toInt()
                    }
                    layoutParams = params
                }
                rowLayout.addView(tvValue)

                layoutDynamicAnswers.addView(rowLayout)

                if (index < data.answers.size - 1) {
                    val divider = View(this).apply {
                        val params = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dpToPx(1f).toInt()
                        ).apply {
                            topMargin = dpToPx(12f).toInt()
                            bottomMargin = dpToPx(12f).toInt()
                        }
                        layoutParams = params
                        setBackgroundColor(androidx.core.content.ContextCompat.getColor(this@ReviewRegistrationActivity, R.color.outline_variant))
                    }
                    layoutDynamicAnswers.addView(divider)
                }
            }
        } else {
            cardAdditionalDetails.visibility = View.GONE
        }
    }

    private fun setupInteractions() {
        val touchListener90 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        val touchListener98 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        // Back buttons (Top Bar & Edit Button)
        btnBack.setOnTouchListener(touchListener90)
        btnBack.setOnClickListener {
            finish()
        }

        btnEditBack.setOnTouchListener(touchListener98)
        btnEditBack.setOnClickListener {
            finish()
        }

        // Submit Registration button
        btnSubmitRegistration.setOnTouchListener(touchListener98)
        btnSubmitRegistration.setOnClickListener { view ->
            if (isSubmitting) return@setOnClickListener
            val data = registrationData ?: return@setOnClickListener

            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            isSubmitting = true
            btnSubmitRegistration.isEnabled = false
            btnSubmitRegistration.text = getString(R.string.review_submitting)

            lifecycleScope.launch {
                val backendPurpose = RegistrationPurposeMapper.toBackendPurpose(data.purpose)
                val answerDtos = data.answers.map { RegistrationAnswerDto(it.questionId, it.value) }
                val result = registrationRepository.createRegistration(
                    eventId = data.eventId,
                    fullName = data.fullName,
                    email = data.email,
                    institution = data.institution,
                    purpose = backendPurpose,
                    durationDays = data.durationDays,
                    vehicleNumber = data.vehicleNumber,
                    answers = answerDtos
                )

                result.onSuccess { responseData ->
                    isSubmitting = false
                    val reg = responseData.registration
                    val confirmedData = RegistrationData(
                        eventId = reg.event?.slug?.ifBlank { reg.eventId } ?: data.eventId,
                        eventName = reg.event?.title?.ifBlank { data.eventName } ?: data.eventName,
                        fullName = reg.fullName,
                        email = reg.email,
                        institution = reg.institution,
                        purpose = RegistrationPurposeMapper.toFriendlyDisplay(reg.purpose),
                        durationDays = reg.durationDays,
                        vehicleNumber = reg.vehicleNumber,
                        answers = data.answers
                    )

                    val intent = Intent(this@ReviewRegistrationActivity, RegistrationSuccessActivity::class.java).apply {
                        putExtra(RegistrationSuccessActivity.EXTRA_REGISTRATION_DATA, confirmedData)
                    }
                    startActivity(intent)
                    finish()
                }.onFailure { error ->
                    isSubmitting = false
                    btnSubmitRegistration.isEnabled = true
                    btnSubmitRegistration.text = getString(R.string.btn_submit_registration)
                    btnSubmitRegistration.performHapticFeedback(HapticFeedbackConstants.REJECT)

                    if (error is DuplicateRegistrationException) {
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(this@ReviewRegistrationActivity)
                            .setTitle("Already Registered")
                            .setMessage(error.message ?: "You have already registered for this event. Your pass is ready in your digital pass wallet.")
                            .setPositiveButton("View My Pass") { _, _ ->
                                val intent = Intent(this@ReviewRegistrationActivity, DigitalPassActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }
                                startActivity(intent)
                                finish()
                            }
                            .setNegativeButton("Back to Home") { _, _ ->
                                val intent = Intent(this@ReviewRegistrationActivity, HomeDashboardActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                finish()
                            }
                            .setCancelable(true)
                            .show()
                    } else {
                        val errorMessage = when (error) {
                            is RegistrationValidationException -> error.message ?: getString(R.string.error_registration_failed)
                            is RegistrationAuthException -> getString(R.string.error_auth_required)
                            else -> error.message ?: getString(R.string.error_registration_failed)
                        }
                        Toast.makeText(this@ReviewRegistrationActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
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
