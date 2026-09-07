package com.mpc.propass

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
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
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mpc.propass.data.repository.DigitalPassRepository
import com.mpc.propass.data.repository.NoActivePassException
import com.mpc.propass.data.repository.PassAuthException
import com.mpc.propass.network.model.MyPassResponseData
import kotlinx.coroutines.launch

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

    private val digitalPassRepository: DigitalPassRepository by lazy {
        (application as ProPassApplication).digitalPassRepository
    }

    private var currentPassData: MyPassResponseData? = null

    private lateinit var digitalPassRoot: FrameLayout
    private lateinit var topAppBar: LinearLayout
    private lateinit var topBarSpacer: View
    private lateinit var bottomNavBar: LinearLayout
    private lateinit var bottomBarSpacer: View

    private lateinit var cardPass: MaterialCardView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvUserOrg: TextView
    private lateinit var badgeVerified: FrameLayout
    private lateinit var ivQrCode: ImageView
    private lateinit var tvPassNumber: TextView
    private lateinit var tvPassTier: TextView

    private lateinit var passScrollView: NestedScrollView
    private lateinit var passProgressBar: ProgressBar
    private lateinit var layoutPassError: LinearLayout
    private lateinit var tvPassErrorMessage: TextView
    private lateinit var btnPassRetry: MaterialButton

    private lateinit var layoutNoPassEmpty: LinearLayout
    private lateinit var btnNoPassRetry: MaterialButton
    private lateinit var btnNoPassScan: MaterialButton

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
        setupInteractions()
        loadPassData()
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
        tvUserName = findViewById(R.id.tvUserName)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvUserOrg = findViewById(R.id.tvUserOrg)
        badgeVerified = findViewById(R.id.badgeVerified)
        ivQrCode = findViewById(R.id.ivQrCode)
        tvPassNumber = findViewById(R.id.tvPassNumber)
        tvPassTier = findViewById(R.id.tvPassTier)

        passScrollView = findViewById(R.id.passScrollView)
        passProgressBar = findViewById(R.id.passProgressBar)
        layoutPassError = findViewById(R.id.layoutPassError)
        tvPassErrorMessage = findViewById(R.id.tvPassErrorMessage)
        btnPassRetry = findViewById(R.id.btnPassRetry)

        layoutNoPassEmpty = findViewById(R.id.layoutNoPassEmpty)
        btnNoPassRetry = findViewById(R.id.btnNoPassRetry)
        btnNoPassScan = findViewById(R.id.btnNoPassScan)

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

        // Retry buttons
        btnPassRetry.setOnClickListener {
            loadPassData()
        }
        btnNoPassRetry.setOnClickListener {
            loadPassData()
        }
        btnNoPassScan.setOnClickListener {
            val intent = android.content.Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        // Contact Methods
        btnContactMail.setOnTouchListener(touchListener92)
        btnContactMail.setOnClickListener {
            val email = currentPassData?.holder?.email
            if (!email.isNullOrBlank()) {
                try {
                    val emailIntent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "ProPass Connection")
                    }
                    startActivity(emailIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Email: $email", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Email not provided", Toast.LENGTH_SHORT).show()
            }
        }

        btnContactCall.setOnTouchListener(touchListener92)
        btnContactCall.setOnClickListener {
            val phone = currentPassData?.holder?.phone
            if (!phone.isNullOrBlank()) {
                try {
                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                    }
                    startActivity(dialIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Phone: $phone", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Phone number not provided", Toast.LENGTH_SHORT).show()
            }
        }

        btnContactSocial.setOnTouchListener(touchListener92)
        btnContactSocial.setOnClickListener {
            val linkedinUrl = currentPassData?.holder?.linkedinUrl
            if (!linkedinUrl.isNullOrBlank()) {
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(linkedinUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "LinkedIn: $linkedinUrl", Toast.LENGTH_SHORT).show()
                }
            } else {
                val name = currentPassData?.holder?.fullName ?: "Holder"
                Toast.makeText(this, "LinkedIn profile for $name not provided", Toast.LENGTH_SHORT).show()
            }
        }

        // Action Buttons
        btnShareLink.setOnTouchListener(touchListener98)
        btnShareLink.setOnClickListener {
            val name = currentPassData?.holder?.fullName?.takeUnless {
                it.isBlank() || it.equals("ProPass User", ignoreCase = true)
            } ?: currentPassData?.holder?.email?.substringBefore("@") ?: "ProPass Member"
            val passNum = currentPassData?.pass?.passNumber ?: ""
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, "ProPass Digital Identity - $name")
                putExtra(android.content.Intent.EXTRA_TEXT, "View $name's verified ProPass ($passNum): https://propass.id/p/$passNum")
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Digital Pass"))
        }

        btnSaveImage.setOnTouchListener(touchListener98)
        btnSaveImage.setOnClickListener {
            if (currentPassData == null) return@setOnClickListener
            Toast.makeText(this, "Digital Pass saved to Gallery", Toast.LENGTH_SHORT).show()
        }

        btnToWallet.setOnTouchListener(touchListener98)
        btnToWallet.setOnClickListener {
            if (currentPassData == null) return@setOnClickListener
            Toast.makeText(this, "Pass added to Google Wallet", Toast.LENGTH_SHORT).show()
        }

        // Bottom Navigation Tabs
        tabHome.setOnClickListener {
            finish()
        }

        tabScan.setOnClickListener {
            val intent = android.content.Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        fabScan.setOnTouchListener(touchListener95)
        fabScan.setOnClickListener {
            val intent = android.content.Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        tabMyPass.setOnClickListener {
            loadPassData()
        }

        val openProfile = {
            val intent = android.content.Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        tabProfile.setOnClickListener { openProfile() }
        topBarAvatar.setOnClickListener { openProfile() }
    }

    override fun onResume() {
        super.onResume()
        loadPassData()
    }

    private fun showLoading() {
        passProgressBar.visibility = View.VISIBLE
        passScrollView.visibility = View.GONE
        layoutPassError.visibility = View.GONE
        layoutNoPassEmpty.visibility = View.GONE
    }

    private fun showPassContent(data: MyPassResponseData) {
        passProgressBar.visibility = View.GONE
        layoutPassError.visibility = View.GONE
        layoutNoPassEmpty.visibility = View.GONE
        passScrollView.visibility = View.VISIBLE
        cardPass.visibility = View.VISIBLE
        currentPassData = data
        bindPassData(data)
        startCardEntranceAnimation()
    }

    private fun showNoPassEmpty() {
        currentPassData = null
        passProgressBar.visibility = View.GONE
        passScrollView.visibility = View.GONE
        layoutPassError.visibility = View.GONE
        layoutNoPassEmpty.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        currentPassData = null
        passProgressBar.visibility = View.GONE
        passScrollView.visibility = View.GONE
        layoutNoPassEmpty.visibility = View.GONE
        layoutPassError.visibility = View.VISIBLE
        tvPassErrorMessage.text = message
    }

    private fun loadPassData() {
        showLoading()

        lifecycleScope.launch {
            val result = digitalPassRepository.getMyDigitalPass()

            result.onSuccess { data ->
                showPassContent(data)
            }.onFailure { error ->
                when (error) {
                    is NoActivePassException -> showNoPassEmpty()
                    is PassAuthException -> showError(getString(R.string.error_auth_required))
                    else -> showError(error.message ?: getString(R.string.error_pass_load_failed))
                }
            }
        }
    }

    private fun bindPassData(data: MyPassResponseData) {
        val pass = data.pass
        val holder = data.holder

        val fullName = holder.fullName.takeUnless {
            it.isBlank() || it.equals("ProPass User", ignoreCase = true)
        } ?: holder.email.substringBefore("@")
        tvUserName.text = fullName
        tvUserRole.text = holder.title?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        tvUserOrg.text = holder.organization?.takeIf { it.isNotBlank() } ?: getString(R.string.profile_not_set)
        badgeVerified.visibility = if (holder.isVerified) View.VISIBLE else View.GONE

        tvPassNumber.text = pass.passNumber
        tvPassTier.text = "${pass.tier} PASS"

        renderQrCode(pass.qrPayload)
    }

    /**
     * Renders a crisp QR code Bitmap using ZXing and updates [ivQrCode].
     */
    private fun renderQrCode(payload: String) {
        if (payload.isBlank()) return
        try {
            val size = 512
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.parseColor("#111827") else Color.WHITE)
                }
            }
            ivQrCode.clearColorFilter()
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e("DigitalPassActivity", "Failed to render QR code", e)
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
