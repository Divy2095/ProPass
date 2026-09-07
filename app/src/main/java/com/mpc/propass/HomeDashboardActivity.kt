package com.mpc.propass

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.DashboardRepository
import com.mpc.propass.data.repository.UserRepository
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.DashboardResponseData
import com.mpc.propass.network.model.UpdateProfileRequest
import com.mpc.propass.network.model.UserProfileDto
import kotlinx.coroutines.launch

/**
 * Home Dashboard screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch specifications:
 * - Fixed translucent top header with ProPass logo, "Home" title, and avatar
 * - Greeting header ("Hello, Sarah") with online status dot
 * - Royal blue Digital Pass preview card with QR code and premium badge
 * - 2-column quick actions ("Scan QR", "My Pass")
 * - Profile completion card with animated radial progress ring and "Add Details" / "Edit Profile" button
 * - Recent activity feed with live PostgreSQL registration activity
 * - Fixed bottom navigation bar with elevated center Scan action button
 */
class HomeDashboardActivity : AppCompatActivity() {

    private lateinit var homeDashboardRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var bottomBarSpacer: View
    private lateinit var dashboardProgressBar: ProgressBar

    // Greeting section
    private lateinit var tvHomeUserName: TextView
    private lateinit var headerAvatarContainer: FrameLayout
    private lateinit var topBarAvatar: FrameLayout

    // Digital Pass preview card
    private lateinit var cardDigitalPass: MaterialCardView
    private lateinit var tvPassTier: TextView
    private lateinit var tvPassUserName: TextView
    private lateinit var tvPassUserRole: TextView
    private lateinit var tvPassUserOrg: TextView

    // Quick Actions
    private lateinit var cardScanQr: MaterialCardView
    private lateinit var cardMyPass: MaterialCardView

    // Profile Completion card
    private lateinit var cardProfileCompletion: MaterialCardView
    private lateinit var btnAddDetails: MaterialButton
    private lateinit var progressCircle: CircularProgressView
    private lateinit var tvProgressPercent: TextView

    // Organizer Portal card
    private lateinit var cardOrganizerPortal: MaterialCardView

    // Recent Activity items
    private lateinit var itemActivity1: MaterialCardView
    private lateinit var tvActivity1Title: TextView
    private lateinit var tvActivity1Subtitle: TextView
    private lateinit var itemActivity2: MaterialCardView
    private lateinit var tvActivity2Title: TextView
    private lateinit var tvActivity2Subtitle: TextView
    private lateinit var tvNoRecentActivity: TextView

    // Bottom Navigation tabs
    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: FrameLayout
    private lateinit var tabMyPass: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var fabScan: FrameLayout

    // Repositories
    private val app: ProPassApplication by lazy { application as ProPassApplication }
    private val authRepository: AuthRepository by lazy { app.authRepository }
    private val userRepository: UserRepository by lazy { app.userRepository }
    private val dashboardRepository: DashboardRepository by lazy { app.dashboardRepository }

    // Cached state
    private var cachedProfile: UserProfileDto? = null
    private var cachedUser: AuthUserDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_home_dashboard)

        initViews()
        applyWindowInsets()
        setupInteractions()
        loadDashboardData()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        homeDashboardRoot = findViewById(R.id.homeDashboardRoot)
        topAppBar = findViewById(R.id.topAppBar)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        dashboardProgressBar = findViewById(R.id.dashboardProgressBar)

        // Greeting
        tvHomeUserName = findViewById(R.id.tvHomeUserName)
        headerAvatarContainer = findViewById(R.id.headerAvatarContainer)
        topBarAvatar = findViewById(R.id.topBarAvatar)

        // Pass card
        cardDigitalPass = findViewById(R.id.cardDigitalPass)
        tvPassTier = findViewById(R.id.tvPassTier)
        tvPassUserName = findViewById(R.id.tvPassUserName)
        tvPassUserRole = findViewById(R.id.tvPassUserRole)
        tvPassUserOrg = findViewById(R.id.tvPassUserOrg)

        // Quick Actions
        cardScanQr = findViewById(R.id.cardScanQr)
        cardMyPass = findViewById(R.id.cardMyPass)

        // Profile Completion
        cardProfileCompletion = findViewById(R.id.cardProfileCompletion)
        btnAddDetails = findViewById(R.id.btnAddDetails)
        progressCircle = findViewById(R.id.progressCircle)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)

        // Organizer Portal
        cardOrganizerPortal = findViewById(R.id.cardOrganizerPortal)

        // Recent Activity
        itemActivity1 = findViewById(R.id.itemActivity1)
        tvActivity1Title = findViewById(R.id.tvActivity1Title)
        tvActivity1Subtitle = findViewById(R.id.tvActivity1Subtitle)
        itemActivity2 = findViewById(R.id.itemActivity2)
        tvActivity2Title = findViewById(R.id.tvActivity2Title)
        tvActivity2Subtitle = findViewById(R.id.tvActivity2Subtitle)
        tvNoRecentActivity = findViewById(R.id.tvNoRecentActivity)

        // Navigation
        tabHome = findViewById(R.id.tabHome)
        tabScan = findViewById(R.id.tabScan)
        tabMyPass = findViewById(R.id.tabMyPass)
        tabProfile = findViewById(R.id.tabProfile)
        fabScan = findViewById(R.id.fabScan)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(homeDashboardRoot) { _, insets ->
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

            // Bottom Navigation Bar insets
            val bottomPadding = systemBars.bottom
            bottomNavBar.setPadding(
                0,
                0,
                0,
                bottomPadding
            )

            val bottomSpacerParams = bottomBarSpacer.layoutParams
            bottomSpacerParams.height = dpToPx(88f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    /**
     * Loads live aggregated dashboard data from GET /api/v1/dashboard.
     */
    fun loadDashboardData() {
        lifecycleScope.launch {
            dashboardProgressBar.visibility = View.VISIBLE

            val result = dashboardRepository.getDashboard()
            dashboardProgressBar.visibility = View.GONE

            result.onSuccess { data ->
                bindDashboardData(data)
            }.onFailure { error ->
                handleDashboardError(error)
            }
        }
    }

    /**
     * Binds real backend data into the existing dashboard UI views.
     */
    private fun bindDashboardData(data: DashboardResponseData) {
        cachedProfile = data.profile
        cachedUser = data.user

        // 1. Greeting Section
        val realName = data.profile?.fullName?.takeUnless {
            it.isBlank() || it.equals("ProPass User", ignoreCase = true)
        }
        val firstName = realName?.trim()?.split(" ")?.firstOrNull()
            ?: data.user.email.substringBefore("@")
        tvHomeUserName.text = "Hello, $firstName"

        // 2. Digital Pass Preview Card
        val passTier = data.pass?.tier ?: "STANDARD"
        tvPassTier.text = "PROPASS $passTier"
        tvPassUserName.text = realName ?: data.user.email
        tvPassUserRole.text = data.profile?.title?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_not_set)
        tvPassUserOrg.text = data.profile?.organization?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_not_set)

        // 3. Profile Completion Ring
        val completionScore = data.profile?.completionScore ?: 0
        animateProgressRing(completionScore.toFloat())
        tvProgressPercent.text = "${completionScore}%"
        btnAddDetails.text = if (completionScore >= 100) {
            getString(R.string.profile_edit_btn)
        } else {
            getString(R.string.btn_add_details)
        }

        // 4. Recent Activity
        val activities = data.recentActivity
        if (activities.isEmpty()) {
            itemActivity1.visibility = View.GONE
            itemActivity2.visibility = View.GONE
            tvNoRecentActivity.visibility = View.VISIBLE
        } else {
            tvNoRecentActivity.visibility = View.GONE

            // Item 1
            val act1 = activities[0]
            itemActivity1.visibility = View.VISIBLE
            tvActivity1Title.text = act1.eventTitle
            tvActivity1Subtitle.text = formatActivitySubtitle(act1.purpose, act1.eventLocation)
            itemActivity1.setOnClickListener {
                Toast.makeText(
                    this,
                    "${act1.eventTitle} (${act1.status})",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Item 2
            if (activities.size > 1) {
                val act2 = activities[1]
                itemActivity2.visibility = View.VISIBLE
                tvActivity2Title.text = act2.eventTitle
                tvActivity2Subtitle.text = formatActivitySubtitle(act2.purpose, act2.eventLocation)
                itemActivity2.setOnClickListener {
                    Toast.makeText(
                        this,
                        "${act2.eventTitle} (${act2.status})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                itemActivity2.visibility = View.GONE
            }
        }
    }

    private fun formatActivitySubtitle(purpose: String?, location: String?): String {
        val purposeFormatted = purpose
            ?.replace('_', ' ')
            ?.lowercase()
            ?.split(" ")
            ?.joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
            ?: "Attendee"

        return if (!location.isNullOrBlank()) {
            "$purposeFormatted • $location"
        } else {
            purposeFormatted
        }
    }

    /**
     * Handles API or network failures with user feedback and retry.
     */
    private fun handleDashboardError(error: Throwable) {
        val message = error.message ?: getString(R.string.dashboard_error_loading)

        if (message.contains("Unauthorized", ignoreCase = true) ||
            message.contains("Session expired", ignoreCase = true)
        ) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            return
        }

        Snackbar.make(homeDashboardRoot, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(R.string.action_retry) {
                loadDashboardData()
            }
            .show()
    }

    /**
     * Smooth radial progress ring animation.
     */
    private fun animateProgressRing(targetPercent: Float) {
        ValueAnimator.ofFloat(0f, targetPercent.coerceIn(0f, 100f)).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                progressCircle.setProgress(animatedValue)
            }
            start()
        }
    }

    private fun setupInteractions() {
        val touchListener98 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        val touchListener95 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        cardDigitalPass.setOnTouchListener(touchListener98)
        cardDigitalPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java)
            startActivity(intent)
        }

        cardScanQr.setOnTouchListener(touchListener95)
        cardScanQr.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        cardMyPass.setOnTouchListener(touchListener95)
        cardMyPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java)
            startActivity(intent)
        }

        // Profile Editing flow
        btnAddDetails.setOnTouchListener(touchListener95)
        btnAddDetails.setOnClickListener {
            openEditProfileDialog()
        }

        // Organizer Portal flow
        cardOrganizerPortal.setOnTouchListener(touchListener98)
        cardOrganizerPortal.setOnClickListener {
            val intent = Intent(this, com.mpc.propass.organizer.ui.OrganizerDashboardActivity::class.java)
            startActivity(intent)
        }

        itemActivity1.setOnTouchListener(touchListener98)
        itemActivity2.setOnTouchListener(touchListener98)

        fabScan.setOnTouchListener(touchListener95)
        fabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabHome.setOnClickListener {
            loadDashboardData()
        }

        tabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabMyPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java)
            startActivity(intent)
        }

        // Profile destination
        val openProfile = {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        tabProfile.setOnClickListener { openProfile() }
        topBarAvatar.setOnClickListener { openProfile() }
        headerAvatarContainer.setOnClickListener { openProfile() }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    /**
     * Complete 6-field profile editing dialog via EditProfileDialogHelper.
     */
    private fun openEditProfileDialog() {
        com.mpc.propass.util.EditProfileDialogHelper.showEditProfileDialog(
            context = this,
            coroutineScope = lifecycleScope,
            userRepository = userRepository,
            currentProfile = cachedProfile
        ) {
            loadDashboardData()
        }
    }

    /**
     * Logs out the user via AuthRepository and returns to LoginActivity.
     */
    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.logout()
            Toast.makeText(this@HomeDashboardActivity, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@HomeDashboardActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
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
