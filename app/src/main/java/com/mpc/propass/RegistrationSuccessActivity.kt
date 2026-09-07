package com.mpc.propass

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
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
 * Registration Success screen for ProPass Digital Identity System.
 * Confirms event registration and provides direct navigation to Home or My Pass.
 */
class RegistrationSuccessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_REGISTRATION_DATA = "EXTRA_REGISTRATION_DATA"
    }

    private lateinit var successRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var stickyBottomBar: LinearLayout
    private lateinit var bottomBarSpacer: View
    private lateinit var badgeSuccessContainer: FrameLayout

    private lateinit var tvSuccessTitle: TextView
    private lateinit var tvSuccessEventTitle: TextView
    private lateinit var tvSuccessAttendeeName: TextView

    private lateinit var btnViewMyPass: MaterialButton
    private lateinit var btnGoHome: MaterialButton

    private var registrationData: RegistrationData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_registration_success)

        initViews()
        applyWindowInsets()
        loadRegistrationData()
        animateSuccessBadge()
        setupInteractions()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        successRoot = findViewById(R.id.successRoot)
        topAppBar = findViewById(R.id.topAppBar)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        stickyBottomBar = findViewById(R.id.stickyBottomBar)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)
        badgeSuccessContainer = findViewById(R.id.badgeSuccessContainer)

        tvSuccessTitle = findViewById(R.id.tvSuccessTitle)
        tvSuccessEventTitle = findViewById(R.id.tvSuccessEventTitle)
        tvSuccessAttendeeName = findViewById(R.id.tvSuccessAttendeeName)

        btnViewMyPass = findViewById(R.id.btnViewMyPass)
        btnGoHome = findViewById(R.id.btnGoHome)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(successRoot) { _, insets ->
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
            bottomSpacerParams.height = dpToPx(130f).toInt() + systemBars.bottom
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
            finish()
            return
        }

        tvSuccessEventTitle.text = data.eventName
        tvSuccessAttendeeName.text = "${data.fullName} • ${data.purpose}"
    }

    private fun animateSuccessBadge() {
        badgeSuccessContainer.scaleX = 0f
        badgeSuccessContainer.scaleY = 0f
        badgeSuccessContainer.alpha = 0f

        badgeSuccessContainer.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(1.4f))
            .start()
    }

    private fun setupInteractions() {
        val touchListener98 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        // View My Pass Button
        btnViewMyPass.setOnTouchListener(touchListener98)
        btnViewMyPass.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val intent = Intent(this, DigitalPassActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
            finish()
        }

        // Go Home Button
        btnGoHome.setOnTouchListener(touchListener98)
        btnGoHome.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            val intent = Intent(this, HomeDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
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
