package com.mpc.propass

import android.animation.ValueAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
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
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.UserRepository
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.UserProfileDto
import com.mpc.propass.util.EditProfileDialogHelper
import kotlinx.coroutines.launch

/**
 * Profile Screen implementation for ProPass Digital Identity System.
 * Displays live backend user profile data, profile completion status with animated ring,
 * complete profile details, and editing actions.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var profileRoot: FrameLayout
    private lateinit var profileScrollView: View
    private lateinit var topBarSpacer: View
    private lateinit var bottomBarSpacer: View
    private lateinit var topAppBar: LinearLayout
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var profileProgressBar: ProgressBar

    // Hero section
    private lateinit var avatarContainer: FrameLayout
    private lateinit var badgeVerified: ImageView
    private lateinit var tvProfileFullName: TextView
    private lateinit var tvProfileHeadline: TextView
    private lateinit var tvProfileEmail: TextView

    // Completion section
    private lateinit var progressCircle: CircularProgressView
    private lateinit var tvProgressPercent: TextView
    private lateinit var tvCompletionSubtitle: TextView
    private lateinit var tvMissingFieldsHint: TextView
    private lateinit var btnEditProfileCompletion: MaterialButton

    // Details section
    private lateinit var tvDetailFullName: TextView
    private lateinit var tvDetailEmail: TextView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailOrg: TextView
    private lateinit var tvDetailPhone: TextView
    private lateinit var tvDetailLinkedin: TextView
    private lateinit var tvDetailAvatar: TextView

    // Action buttons
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnOrganizerPortal: MaterialButton
    private lateinit var btnLogout: MaterialButton

    // Bottom Navigation tabs
    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: FrameLayout
    private lateinit var tabMyPass: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var fabScan: FrameLayout

    // Repositories
    private val app: ProPassApplication by lazy { application as ProPassApplication }
    private val userRepository: UserRepository by lazy { app.userRepository }
    private val authRepository: AuthRepository by lazy { app.authRepository }

    // Cached state
    private var cachedProfile: UserProfileDto? = null
    private var cachedUser: AuthUserDto? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_profile)

        initViews()
        applyWindowInsets()
        setupInteractions()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        profileRoot = findViewById(R.id.profileRoot)
        profileScrollView = findViewById(R.id.profileScrollView)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        topAppBar = findViewById(R.id.topAppBar)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        profileProgressBar = findViewById(R.id.profileProgressBar)

        // Hero
        avatarContainer = findViewById(R.id.avatarContainer)
        badgeVerified = findViewById(R.id.badgeVerified)
        tvProfileFullName = findViewById(R.id.tvProfileFullName)
        tvProfileHeadline = findViewById(R.id.tvProfileHeadline)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)

        // Completion
        progressCircle = findViewById(R.id.progressCircle)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)
        tvCompletionSubtitle = findViewById(R.id.tvCompletionSubtitle)
        tvMissingFieldsHint = findViewById(R.id.tvMissingFieldsHint)
        btnEditProfileCompletion = findViewById(R.id.btnEditProfileCompletion)

        // Details
        tvDetailFullName = findViewById(R.id.tvDetailFullName)
        tvDetailEmail = findViewById(R.id.tvDetailEmail)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailOrg = findViewById(R.id.tvDetailOrg)
        tvDetailPhone = findViewById(R.id.tvDetailPhone)
        tvDetailLinkedin = findViewById(R.id.tvDetailLinkedin)
        tvDetailAvatar = findViewById(R.id.tvDetailAvatar)

        // Action buttons
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnOrganizerPortal = findViewById(R.id.btnOrganizerPortal)
        btnLogout = findViewById(R.id.btnLogout)

        // Bottom Navigation
        tabHome = findViewById(R.id.tabHome)
        tabScan = findViewById(R.id.tabScan)
        tabMyPass = findViewById(R.id.tabMyPass)
        tabProfile = findViewById(R.id.tabProfile)
        fabScan = findViewById(R.id.fabScan)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(profileRoot) { _, insets ->
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
            bottomNavBar.setPadding(0, 0, 0, systemBars.bottom)

            val bottomSpacerParams = bottomBarSpacer.layoutParams
            bottomSpacerParams.height = dpToPx(88f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    private fun setupInteractions() {
        val touchListener95 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        btnEditProfile.setOnTouchListener(touchListener95)
        btnEditProfile.setOnClickListener {
            openEditProfileDialog()
        }

        btnEditProfileCompletion.setOnTouchListener(touchListener95)
        btnEditProfileCompletion.setOnClickListener {
            openEditProfileDialog()
        }

        btnLogout.setOnTouchListener(touchListener95)
        btnLogout.setOnClickListener {
            performLogout()
        }

        btnOrganizerPortal.setOnTouchListener(touchListener95)
        btnOrganizerPortal.setOnClickListener {
            val intent = Intent(this, com.mpc.propass.organizer.ui.OrganizerDashboardActivity::class.java)
            startActivity(intent)
        }

        // Bottom Navigation
        tabHome.setOnClickListener {
            val intent = Intent(this, HomeDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        tabScan.setOnClickListener {
            startActivity(Intent(this, ScanQRActivity::class.java))
        }

        fabScan.setOnTouchListener(touchListener95)
        fabScan.setOnClickListener {
            startActivity(Intent(this, ScanQRActivity::class.java))
        }

        tabMyPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        tabProfile.setOnClickListener {
            loadProfileData()
        }

        // Clickable LinkedIn link
        tvDetailLinkedin.setOnClickListener {
            val url = cachedProfile?.linkedinUrl
            if (!url.isNullOrBlank()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfileData() {
        profileProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = userRepository.getUserProfile()
            profileProgressBar.visibility = View.GONE

            result.onSuccess { data ->
                cachedUser = data.user
                cachedProfile = data.profile
                bindProfileData(data.user, data.profile)
            }.onFailure { error ->
                Toast.makeText(
                    this@ProfileActivity,
                    error.message ?: getString(R.string.error_profile_load_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindProfileData(user: AuthUserDto, profile: UserProfileDto?) {
        val fullName = profile?.fullName?.takeUnless {
            it.isBlank() || it.equals("ProPass User", ignoreCase = true)
        } ?: user.email.substringBefore("@")

        tvProfileFullName.text = fullName
        tvProfileEmail.text = user.email

        val title = profile?.title?.takeIf { it.isNotBlank() }
        val org = profile?.organization?.takeIf { it.isNotBlank() }
        val headline = when {
            title != null && org != null -> "$title • $org"
            title != null -> title
            org != null -> org
            else -> getString(R.string.profile_headline_empty)
        }
        tvProfileHeadline.text = headline

        badgeVerified.visibility = if (profile?.isVerified == true) View.VISIBLE else View.GONE

        // Completion status
        val score = profile?.completionScore ?: 0
        tvProgressPercent.text = "$score%"
        animateProgressRing(score)

        val missing = mutableListOf<String>()
        if (profile?.fullName.isNullOrBlank() || profile?.fullName.equals("ProPass User", ignoreCase = true)) {
            missing.add(getString(R.string.label_full_name))
        }
        if (profile?.title.isNullOrBlank()) missing.add(getString(R.string.label_title))
        if (profile?.organization.isNullOrBlank()) missing.add(getString(R.string.label_organization))
        if (profile?.phone.isNullOrBlank()) missing.add(getString(R.string.label_phone))
        if (profile?.linkedinUrl.isNullOrBlank()) missing.add("LinkedIn")
        if (profile?.avatarUrl.isNullOrBlank()) missing.add("Avatar")

        if (missing.isEmpty()) {
            tvCompletionSubtitle.text = getString(R.string.profile_complete_all_done)
            tvMissingFieldsHint.visibility = View.GONE
            btnEditProfileCompletion.text = getString(R.string.profile_edit_btn)
        } else {
            tvCompletionSubtitle.text = getString(R.string.profile_complete_subtitle)
            tvMissingFieldsHint.text = getString(R.string.profile_missing_prefix) + missing.joinToString(", ")
            tvMissingFieldsHint.visibility = View.VISIBLE
            btnEditProfileCompletion.text = getString(R.string.btn_add_details)
        }

        // Details
        tvDetailFullName.text = profile?.fullName?.takeUnless {
            it.isBlank() || it.equals("ProPass User", ignoreCase = true)
        } ?: getString(R.string.profile_not_set)

        tvDetailEmail.text = user.email
        tvDetailTitle.text = profile?.title?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        tvDetailOrg.text = profile?.organization?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        tvDetailPhone.text = profile?.phone?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        tvDetailLinkedin.text = profile?.linkedinUrl?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        tvDetailAvatar.text = profile?.avatarUrl?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
    }

    private fun openEditProfileDialog() {
        EditProfileDialogHelper.showEditProfileDialog(
            context = this,
            coroutineScope = lifecycleScope,
            userRepository = userRepository,
            currentProfile = cachedProfile
        ) { updatedProfile ->
            cachedProfile = updatedProfile
            cachedUser?.let { bindProfileData(it, updatedProfile) }
        }
    }

    private fun animateProgressRing(targetProgress: Int) {
        ValueAnimator.ofFloat(0f, targetProgress.toFloat()).apply {
            duration = 800
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                progressCircle.setProgress(animator.animatedValue as Float)
            }
            start()
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.logout()
            Toast.makeText(this@ProfileActivity, "Logged out", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java).apply {
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
