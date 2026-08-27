package com.mpc.propass

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Splash Screen implementation for ProPass Digital Identity System.
 * Matches the exact layout, typography, palette, and animations from Stitch.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var splashRoot: View
    private lateinit var orbTopLeft: View
    private lateinit var orbBottomRight: View
    private lateinit var iconContainer: FrameLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvTagline: TextView
    private lateinit var loaderContainer: FrameLayout
    private lateinit var loaderThumb: View

    private val runningAnimators = mutableListOf<ValueAnimator>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_splash)

        initViews()
        applyWindowInsets()
        startEntranceAnimations()
        startAmbientLoopAnimations()
        startLoaderAnimation()
        scheduleNavigationToLogin()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        splashRoot = findViewById(R.id.splashRoot)
        orbTopLeft = findViewById(R.id.orbTopLeft)
        orbBottomRight = findViewById(R.id.orbBottomRight)
        iconContainer = findViewById(R.id.iconContainer)
        tvTitle = findViewById(R.id.tvTitle)
        tvTagline = findViewById(R.id.tvTagline)
        loaderContainer = findViewById(R.id.loaderContainer)
        loaderThumb = findViewById(R.id.loaderThumb)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(splashRoot) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            // Adjust bottom margin of loader to respect gesture pill and navigation bar
            val baseBottomMargin = dpToPx(48f).toInt()
            val layoutParams = loaderContainer.layoutParams as? FrameLayout.LayoutParams
                ?: (loaderContainer.layoutParams as? android.widget.RelativeLayout.LayoutParams)
            
            if (layoutParams is android.widget.RelativeLayout.LayoutParams) {
                layoutParams.bottomMargin = baseBottomMargin + systemBars.bottom
                loaderContainer.layoutParams = layoutParams
            }
            insets
        }
    }

    /**
     * Staggered slide-up and fade-in animations corresponding to the Stitch design specifications:
     * - Icon: 0ms delay, 800ms duration
     * - Title: 200ms delay, 800ms duration
     * - Tagline: 400ms delay, 800ms duration
     * - Loader: 1000ms delay, 1000ms duration fade-in
     */
    private fun startEntranceAnimations() {
        val slideDistance = dpToPx(20f)
        val decelerateInterpolator = DecelerateInterpolator(1.5f)

        // 1. Icon Container
        iconContainer.alpha = 0f
        iconContainer.translationY = slideDistance
        iconContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setInterpolator(decelerateInterpolator)
            .start()

        // 2. Title ("ProPass")
        tvTitle.alpha = 0f
        tvTitle.translationY = slideDistance
        tvTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(200)
            .setDuration(800)
            .setInterpolator(decelerateInterpolator)
            .start()

        // 3. Tagline ("Your professional identity. One pass.")
        tvTagline.alpha = 0f
        tvTagline.translationY = slideDistance
        tvTagline.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(400)
            .setDuration(800)
            .setInterpolator(decelerateInterpolator)
            .start()

        // 4. Loading Indicator Container
        loaderContainer.alpha = 0f
        loaderContainer.animate()
            .alpha(1f)
            .setStartDelay(1000)
            .setDuration(1000)
            .setInterpolator(decelerateInterpolator)
            .start()
    }

    /**
     * Subtle ambient gradient orbs pulsing continuously:
     * - Top-left primary blue orb: 8s cycle (4s each way)
     * - Bottom-right secondary blue orb: 10s cycle (5s each way)
     */
    private fun startAmbientLoopAnimations() {
        val smoothInterpolator = AccelerateDecelerateInterpolator()

        // Top-Left Orb Pulse
        val orb1ScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.92f, 1.08f)
        val orb1ScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.92f, 1.08f)
        val orb1Alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.45f, 0.75f)
        val orb1Anim = ObjectAnimator.ofPropertyValuesHolder(
            orbTopLeft,
            orb1ScaleX,
            orb1ScaleY,
            orb1Alpha
        ).apply {
            duration = 4000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = smoothInterpolator
            start()
        }
        runningAnimators.add(orb1Anim)

        // Bottom-Right Orb Pulse
        val orb2ScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f, 1.10f)
        val orb2ScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f, 1.10f)
        val orb2Alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.50f, 0.85f)
        val orb2Anim = ObjectAnimator.ofPropertyValuesHolder(
            orbBottomRight,
            orb2ScaleX,
            orb2ScaleY,
            orb2Alpha
        ).apply {
            duration = 5000
            startDelay = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = smoothInterpolator
            start()
        }
        runningAnimators.add(orb2Anim)
    }

    /**
     * Continuous vertical bouncing thumb inside the capsule progress bar (32dp - 11dp = 21dp travel distance).
     */
    private fun startLoaderAnimation() {
        val travelDistance = dpToPx(21f)
        val loaderAnim = ObjectAnimator.ofFloat(loaderThumb, View.TRANSLATION_Y, 0f, travelDistance).apply {
            duration = 1000
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        runningAnimators.add(loaderAnim)
    }

    /**
     * Automatically transitions from Splash to Login after the entrance animations complete (~2500ms).
     */
    private fun scheduleNavigationToLogin() {
        handler.postDelayed({
            if (!isFinishing && !isDestroyed) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }, 2500)
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
        handler.removeCallbacksAndMessages(null)
        runningAnimators.forEach { it.cancel() }
        runningAnimators.clear()
    }
}
