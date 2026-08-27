package com.mpc.propass

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Home Dashboard screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch specifications:
 * - Fixed translucent top header with ProPass logo, "Home" title, and avatar
 * - Greeting header ("Hello, Sarah") with online status dot
 * - Royal blue Digital Pass preview card with QR code and premium badge
 * - 2-column quick actions ("Scan QR", "My Pass")
 * - Profile completion card with animated 85% radial progress ring and "Add Details" button
 * - Recent activity feed with contextual badges (TechConf 2024, Google Office Visit)
 * - Fixed bottom navigation bar with elevated center Scan action button
 */
class HomeDashboardActivity : AppCompatActivity() {

    private lateinit var homeDashboardRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var bottomBarSpacer: View

    private lateinit var cardDigitalPass: MaterialCardView
    private lateinit var cardScanQr: MaterialCardView
    private lateinit var cardMyPass: MaterialCardView
    private lateinit var cardProfileCompletion: MaterialCardView
    private lateinit var btnAddDetails: MaterialButton
    private lateinit var progressCircle: CircularProgressView
    private lateinit var tvProgressPercent: TextView

    private lateinit var itemActivity1: MaterialCardView
    private lateinit var itemActivity2: MaterialCardView

    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: FrameLayout
    private lateinit var tabMyPass: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var fabScan: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_home_dashboard)

        initViews()
        applyWindowInsets()
        startProgressRingAnimation()
        setupInteractions()
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

        cardDigitalPass = findViewById(R.id.cardDigitalPass)
        cardScanQr = findViewById(R.id.cardScanQr)
        cardMyPass = findViewById(R.id.cardMyPass)
        cardProfileCompletion = findViewById(R.id.cardProfileCompletion)
        btnAddDetails = findViewById(R.id.btnAddDetails)
        progressCircle = findViewById(R.id.progressCircle)
        tvProgressPercent = findViewById(R.id.tvProgressPercent)

        itemActivity1 = findViewById(R.id.itemActivity1)
        itemActivity2 = findViewById(R.id.itemActivity2)

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
     * Smooth clockwise progress ring entrance animation (0% -> 85%).
     */
    private fun startProgressRingAnimation() {
        ValueAnimator.ofFloat(0f, 85f).apply {
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
        // Tactile helper for 0.98x scaling
        val touchListener98 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        // Tactile helper for 0.95x scaling (buttons and grid cards)
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

        btnAddDetails.setOnTouchListener(touchListener95)
        btnAddDetails.setOnClickListener {
            Toast.makeText(this, "Add Profile Details", Toast.LENGTH_SHORT).show()
        }

        itemActivity1.setOnTouchListener(touchListener98)
        itemActivity1.setOnClickListener {
            Toast.makeText(this, "TechConf 2024 Form Details", Toast.LENGTH_SHORT).show()
        }

        itemActivity2.setOnTouchListener(touchListener98)
        itemActivity2.setOnClickListener {
            Toast.makeText(this, "Google Office Visit Check-in", Toast.LENGTH_SHORT).show()
        }

        fabScan.setOnTouchListener(touchListener95)
        fabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabHome.setOnClickListener {
            // Already on home
        }

        tabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabMyPass.setOnClickListener {
            val intent = Intent(this, DigitalPassActivity::class.java)
            startActivity(intent)
        }

        tabProfile.setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
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
