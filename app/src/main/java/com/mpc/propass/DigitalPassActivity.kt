package com.mpc.propass

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
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
 * Digital Pass screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch specifications:
 * - Fixed translucent top header with ProPass logo, "My Pass" title, and avatar
 * - Verified profile card with 96dp avatar, verified check badge, title, and organization pill
 * - Quick contact action buttons (Email, Phone, LinkedIn)
 * - Section divider with "SCAN TO CONNECT" and high-contrast 140dp QR box
 * - Action buttons: "Share Link", "Save Image", and "To Wallet"
 * - Fixed bottom navigation bar with active "My Pass" tab and floating center Scan FAB
 */
class DigitalPassActivity : AppCompatActivity() {

    private lateinit var digitalPassRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var bottomBarSpacer: View

    private lateinit var cardPass: MaterialCardView
    private lateinit var btnContactMail: FrameLayout
    private lateinit var btnContactCall: FrameLayout
    private lateinit var btnContactSocial: FrameLayout

    private lateinit var btnShareLink: MaterialButton
    private lateinit var btnSaveImage: MaterialButton
    private lateinit var btnToWallet: MaterialButton

    private lateinit var tabHome: LinearLayout
    private lateinit var tabScan: FrameLayout
    private lateinit var tabMyPass: LinearLayout
    private lateinit var tabProfile: LinearLayout
    private lateinit var fabScan: FrameLayout
    private lateinit var topBarAvatar: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_digital_pass)

        initViews()
        applyWindowInsets()
        startCardEntranceAnimation()
        setupInteractions()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        digitalPassRoot = findViewById(R.id.digitalPassRoot)
        topAppBar = findViewById(R.id.topAppBar)
        topBarSpacer = findViewById(R.id.topBarSpacer)
        bottomNavBar = findViewById(R.id.bottomNavBar)
        bottomBarSpacer = findViewById(R.id.bottomBarSpacer)

        cardPass = findViewById(R.id.cardPass)
        btnContactMail = findViewById(R.id.btnContactMail)
        btnContactCall = findViewById(R.id.btnContactCall)
        btnContactSocial = findViewById(R.id.btnContactSocial)

        btnShareLink = findViewById(R.id.btnShareLink)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnToWallet = findViewById(R.id.btnToWallet)

        tabHome = findViewById(R.id.tabHome)
        tabScan = findViewById(R.id.tabScan)
        tabMyPass = findViewById(R.id.tabMyPass)
        tabProfile = findViewById(R.id.tabProfile)
        fabScan = findViewById(R.id.fabScan)
        topBarAvatar = findViewById(R.id.topBarAvatar)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(digitalPassRoot) { _, insets ->
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
            bottomSpacerParams.height = dpToPx(120f).toInt() + systemBars.bottom
            bottomBarSpacer.layoutParams = bottomSpacerParams

            insets
        }
    }

    /**
     * Subtle entrance fade + vertical slide animation.
     */
    private fun startCardEntranceAnimation() {
        cardPass.alpha = 0f
        cardPass.translationY = dpToPx(30f)

        cardPass.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun setupInteractions() {
        // Touch feedback helpers
        val touchListener98 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        val touchListener92 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start()
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

        cardPass.setOnTouchListener(touchListener98)

        // Contact Methods
        btnContactMail.setOnTouchListener(touchListener92)
        btnContactMail.setOnClickListener {
            try {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:elena.rodriguez@acmecorp.com")
                    putExtra(Intent.EXTRA_SUBJECT, "ProPass Connection")
                }
                startActivity(emailIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Email: elena.rodriguez@acmecorp.com", Toast.LENGTH_SHORT).show()
            }
        }

        btnContactCall.setOnTouchListener(touchListener92)
        btnContactCall.setOnClickListener {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:+15552345678")
                }
                startActivity(dialIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Phone: +1 (555) 234-5678", Toast.LENGTH_SHORT).show()
            }
        }

        btnContactSocial.setOnTouchListener(touchListener92)
        btnContactSocial.setOnClickListener {
            Toast.makeText(this, "Opening LinkedIn Profile: Elena Rodriguez", Toast.LENGTH_SHORT).show()
        }

        // Action Buttons
        btnShareLink.setOnTouchListener(touchListener98)
        btnShareLink.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "ProPass Digital Identity - Elena Rodriguez")
                putExtra(Intent.EXTRA_TEXT, "View Elena Rodriguez's verified ProPass: https://propass.id/p/elena-rodriguez")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Digital Pass"))
        }

        btnSaveImage.setOnTouchListener(touchListener98)
        btnSaveImage.setOnClickListener {
            Toast.makeText(this, "Digital Pass saved to Gallery", Toast.LENGTH_SHORT).show()
        }

        btnToWallet.setOnTouchListener(touchListener98)
        btnToWallet.setOnClickListener {
            Toast.makeText(this, "Pass added to Google Wallet", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation Tabs
        tabHome.setOnClickListener {
            finish()
        }

        tabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        fabScan.setOnTouchListener(touchListener95)
        fabScan.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabMyPass.setOnClickListener {
            // Already on My Pass
        }

        tabProfile.setOnClickListener {
            Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
        }

        topBarAvatar.setOnClickListener {
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
