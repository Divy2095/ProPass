package com.mpc.propass

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton

/**
 * Onboarding - Smart Scan screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch specifications:
 * - Top 3-segment page progress indicator
 * - Hero illustration card with animated laser QR scanner and success pop badge
 * - "Scan. Auto-fill. Submit." typography section
 * - Pinned 56dp Pill "Get Started" action button
 */
class OnboardingSmartScanActivity : AppCompatActivity() {

    private lateinit var onboardingScrollView: View
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var bottomActionArea: FrameLayout
    private lateinit var scannerLaser: View
    private lateinit var successBadge: FrameLayout
    private lateinit var btnGetStarted: MaterialButton

    private var scanLoopAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_onboarding_smart_scan)

        initViews()
        applyWindowInsets()
        startScannerAnimationLoop()
        setupButtonInteractions()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        onboardingScrollView = findViewById(R.id.onboardingScrollView)
        indicatorContainer = findViewById(R.id.indicatorContainer)
        bottomActionArea = findViewById(R.id.bottomActionArea)
        scannerLaser = findViewById(R.id.scannerLaser)
        successBadge = findViewById(R.id.successBadge)
        btnGetStarted = findViewById(R.id.btnGetStarted)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(onboardingScrollView) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            // Add status bar insets to top indicator
            val topMargin = dpToPx(24f).toInt() + systemBars.top
            val indicatorParams = indicatorContainer.layoutParams as? LinearLayout.LayoutParams
            if (indicatorParams != null) {
                indicatorParams.topMargin = topMargin
                indicatorContainer.layoutParams = indicatorParams
            }

            // Add navigation bar insets to bottom action area
            val bottomPadding = dpToPx(20f).toInt() + systemBars.bottom
            bottomActionArea.setPadding(
                dpToPx(20f).toInt(),
                dpToPx(16f).toInt(),
                dpToPx(20f).toInt(),
                bottomPadding
            )

            insets
        }
    }

    /**
     * Recreates the Stitch 3-second infinite scan and checkmark pop cycle:
     * - Laser scans from top to bottom (0% - 50%)
     * - Laser fades out (50% - 60%)
     * - Success badge pops up in center (60% - 85%)
     * - Reset and loop (85% - 100%)
     */
    private fun startScannerAnimationLoop() {
        val totalTravel = dpToPx(144f)
        val startY = -dpToPx(8f)

        scanLoopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()

            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float

                // 1. Laser scanning phase (0.0 -> 0.6)
                when {
                    progress < 0.10f -> {
                        val subProgress = progress / 0.10f
                        scannerLaser.alpha = subProgress
                        scannerLaser.translationY = startY + (totalTravel * 0.10f * subProgress)
                    }
                    progress in 0.10f..0.50f -> {
                        val subProgress = (progress - 0.10f) / 0.40f
                        scannerLaser.alpha = 1.0f
                        scannerLaser.translationY = startY + (totalTravel * (0.10f + 0.90f * subProgress))
                    }
                    progress in 0.50f..0.60f -> {
                        val subProgress = (progress - 0.50f) / 0.10f
                        scannerLaser.alpha = 1.0f - subProgress
                        scannerLaser.translationY = startY + totalTravel
                    }
                    else -> {
                        scannerLaser.alpha = 0f
                        scannerLaser.translationY = startY
                    }
                }

                // 2. Success Badge pop phase (0.6 -> 0.9)
                when {
                    progress < 0.60f -> {
                        successBadge.alpha = 0f
                        successBadge.scaleX = 0.8f
                        successBadge.scaleY = 0.8f
                    }
                    progress in 0.60f..0.70f -> {
                        val subProgress = (progress - 0.60f) / 0.10f
                        successBadge.alpha = subProgress
                        val scale = 0.8f + (0.3f * subProgress) // 0.8 -> 1.1
                        successBadge.scaleX = scale
                        successBadge.scaleY = scale
                    }
                    progress in 0.70f..0.80f -> {
                        val subProgress = (progress - 0.70f) / 0.10f
                        successBadge.alpha = 1.0f
                        val scale = 1.1f - (0.1f * subProgress) // 1.1 -> 1.0
                        successBadge.scaleX = scale
                        successBadge.scaleY = scale
                    }
                    progress in 0.80f..0.90f -> {
                        val subProgress = (progress - 0.80f) / 0.10f
                        successBadge.alpha = 1.0f - subProgress
                        val scale = 1.0f - (0.2f * subProgress) // 1.0 -> 0.8
                        successBadge.scaleX = scale
                        successBadge.scaleY = scale
                    }
                    else -> {
                        successBadge.alpha = 0f
                        successBadge.scaleX = 0.8f
                        successBadge.scaleY = 0.8f
                    }
                }
            }
            start()
        }
    }

    private fun setupButtonInteractions() {
        btnGetStarted.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        btnGetStarted.setOnClickListener {
            val intent = Intent(this, SmartFormRegistrationActivity::class.java)
            startActivity(intent)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        scanLoopAnimator?.cancel()
        scanLoopAnimator = null
    }
}
