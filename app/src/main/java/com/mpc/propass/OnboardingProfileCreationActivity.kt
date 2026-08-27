package com.mpc.propass

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * Onboarding - Profile Creation screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch specifications:
 * - Ambient subtle blue primary glow with 4s pulse
 * - Profile card rotated -2deg with shadow, avatar, and skeleton lines
 * - Floating +12deg tilted Verified badge with 3s continuous vertical bounce
 * - "Create your profile once." typography section
 * - 16dp rounded "Next" and "Skip" action buttons
 */
class OnboardingProfileCreationActivity : AppCompatActivity() {

    private lateinit var profileCreationScrollView: View
    private lateinit var mainContentSection: LinearLayout
    private lateinit var bottomActionLayout: LinearLayout
    private lateinit var ambientGlow: View
    private lateinit var profileCard: MaterialCardView
    private lateinit var verifiedBadge: FrameLayout
    private lateinit var skeletonName: View
    private lateinit var skeletonRole: View
    private lateinit var skeletonDetail1: View
    private lateinit var skeletonDetail2: View
    private lateinit var skeletonDetail3: View
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: MaterialButton

    private val runningAnimators = mutableListOf<ValueAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_onboarding_profile_creation)

        initViews()
        applyWindowInsets()
        startAmbientGlowAnimation()
        startVerifiedBadgeBounce()
        startSkeletonShimmer()
        setupCardInteraction()
        setupButtonListeners()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        profileCreationScrollView = findViewById(R.id.profileCreationScrollView)
        mainContentSection = findViewById(R.id.mainContentSection)
        bottomActionLayout = findViewById(R.id.bottomActionLayout)
        ambientGlow = findViewById(R.id.ambientGlow)
        profileCard = findViewById(R.id.profileCard)
        verifiedBadge = findViewById(R.id.verifiedBadge)
        skeletonName = findViewById(R.id.skeletonName)
        skeletonRole = findViewById(R.id.skeletonRole)
        skeletonDetail1 = findViewById(R.id.skeletonDetail1)
        skeletonDetail2 = findViewById(R.id.skeletonDetail2)
        skeletonDetail3 = findViewById(R.id.skeletonDetail3)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(profileCreationScrollView) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            val topPadding = dpToPx(24f).toInt() + systemBars.top
            val bottomPadding = dpToPx(32f).toInt() + systemBars.bottom

            mainContentSection.setPadding(
                dpToPx(20f).toInt(),
                topPadding,
                dpToPx(20f).toInt(),
                0
            )

            bottomActionLayout.setPadding(
                dpToPx(20f).toInt(),
                dpToPx(24f).toInt(),
                dpToPx(20f).toInt(),
                bottomPadding
            )

            insets
        }
    }

    /**
     * Subtle 4-second infinite pulse on the background ambient glow.
     */
    private fun startAmbientGlowAnimation() {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.90f, 1.10f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.90f, 1.10f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.40f, 0.70f)

        val glowAnim = ObjectAnimator.ofPropertyValuesHolder(
            ambientGlow,
            scaleX,
            scaleY,
            alpha
        ).apply {
            duration = 2000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        runningAnimators.add(glowAnim)
    }

    /**
     * Continuous 3-second vertical floating bounce for the +12deg tilted Verified badge.
     */
    private fun startVerifiedBadgeBounce() {
        val travelDistance = dpToPx(6f)
        val bounceAnim = ObjectAnimator.ofFloat(
            verifiedBadge,
            View.TRANSLATION_Y,
            -travelDistance,
            travelDistance
        ).apply {
            duration = 1500
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        runningAnimators.add(bounceAnim)
    }

    /**
     * Staggered 2-second pulse on skeleton placeholder lines.
     */
    private fun startSkeletonShimmer() {
        val smoothInterpolator = AccelerateDecelerateInterpolator()

        val nameAnim = ObjectAnimator.ofFloat(skeletonName, View.ALPHA, 0.6f, 1.0f).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = smoothInterpolator
            start()
        }
        runningAnimators.add(nameAnim)

        val roleAnim = ObjectAnimator.ofFloat(skeletonRole, View.ALPHA, 0.3f, 0.6f).apply {
            duration = 1000
            startDelay = 200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = smoothInterpolator
            start()
        }
        runningAnimators.add(roleAnim)

        val detail1Anim = ObjectAnimator.ofFloat(skeletonDetail1, View.ALPHA, 0.2f, 0.4f).apply {
            duration = 1000
            startDelay = 400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = smoothInterpolator
            start()
        }
        runningAnimators.add(detail1Anim)
    }

    /**
     * Tactile card tilt animation on press (resting at -2deg, straightens to 0deg on touch).
     */
    private fun setupCardInteraction() {
        profileCard.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .rotation(0f)
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(200)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .rotation(-2f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(300)
                        .start()
                }
            }
            false
        }
    }

    private fun setupButtonListeners() {
        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        btnNext.setOnTouchListener(touchListener)
        btnSkip.setOnTouchListener(touchListener)

        btnNext.setOnClickListener {
            val intent = Intent(this, OnboardingSmartScanActivity::class.java)
            startActivity(intent)
        }

        btnSkip.setOnClickListener {
            Toast.makeText(this, "Onboarding skipped", Toast.LENGTH_SHORT).show()
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

    override fun onDestroy() {
        super.onDestroy()
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()
    }
}
