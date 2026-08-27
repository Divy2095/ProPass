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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton

/**
 * Review Registration screen for ProPass Digital Identity System.
 * Displays all validated registration details before final submission.
 */
class ReviewRegistrationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGISTRATION_DATA = "EXTRA_REGISTRATION_DATA"
    }

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
        registrationData = intent.getSerializableExtra(EXTRA_REGISTRATION_DATA) as? RegistrationData

        val data = registrationData ?: RegistrationData(
            eventId = "techconf-2024",
            eventName = "TechConf 2024",
            fullName = "Alex Morgan",
            email = "alex.morgan@example.com",
            institution = "University of Technology",
            purpose = "General Attendee",
            durationDays = 3,
            vehicleNumber = null
        )

        tvReviewEventTitle.text = data.eventName
        tvReviewFullName.text = data.fullName
        tvReviewEmail.text = data.email
        tvReviewInstitution.text = data.institution
        tvReviewPurpose.text = data.purpose
        tvReviewDuration.text = if (data.durationDays == 1) "1 day" else "${data.durationDays} days"
        tvReviewVehicle.text = data.vehicleNumber.takeIf { !it.isNullOrBlank() } ?: getString(R.string.review_vehicle_not_provided)
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
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            btnSubmitRegistration.isEnabled = false
            btnSubmitRegistration.text = getString(R.string.review_submitting)

            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, RegistrationSuccessActivity::class.java).apply {
                    putExtra(RegistrationSuccessActivity.EXTRA_REGISTRATION_DATA, registrationData)
                }
                startActivity(intent)
                finish()
            }, 450)
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
