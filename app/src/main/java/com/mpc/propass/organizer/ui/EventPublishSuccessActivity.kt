package com.mpc.propass.organizer.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.mpc.propass.R
import com.mpc.propass.organizer.model.OrganizerEvent

/**
 * Event Publish Success & QR Code Screen:
 * Displays the published event confirmation, rendered 512x512 ZXing QR code bitmap,
 * event metadata summary, and navigation back to the Organizer Dashboard.
 */
class EventPublishSuccessActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EVENT = "extra_organizer_event"
        const val EXTRA_IS_VIEW_MODE = "extra_is_view_mode"
        private const val TAG = "EventPublishSuccess"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvTopBarTitle: TextView
    private lateinit var ivPublishBadgeIcon: ImageView
    private lateinit var tvPublishTitle: TextView
    private lateinit var tvPublishSubtitle: TextView
    private lateinit var ivEventQrCode: ImageView
    private lateinit var tvQrPayload: TextView
    private lateinit var tvEventName: TextView
    private lateinit var tvEventDesc: TextView
    private lateinit var tvEventDateTime: TextView
    private lateinit var tvEventLocation: TextView
    private lateinit var tvEventFieldsCount: TextView
    private lateinit var tvMockBadge: TextView
    private lateinit var btnBackToDashboard: MaterialButton

    private var event: OrganizerEvent? = null
    private var isViewMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_event_publish_success)

        parseIntentExtras()
        initViews()
        populateEventData()
        setupListeners()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun parseIntentExtras() {
        event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EVENT, OrganizerEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EVENT) as? OrganizerEvent
        }

        isViewMode = intent.getBooleanExtra(EXTRA_IS_VIEW_MODE, false)
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvTopBarTitle = findViewById(R.id.tvTopBarTitle)
        ivPublishBadgeIcon = findViewById(R.id.ivPublishBadgeIcon)
        tvPublishTitle = findViewById(R.id.tvPublishTitle)
        tvPublishSubtitle = findViewById(R.id.tvPublishSubtitle)
        ivEventQrCode = findViewById(R.id.ivEventQrCode)
        tvQrPayload = findViewById(R.id.tvQrPayload)
        tvEventName = findViewById(R.id.tvEventName)
        tvEventDesc = findViewById(R.id.tvEventDesc)
        tvEventDateTime = findViewById(R.id.tvEventDateTime)
        tvEventLocation = findViewById(R.id.tvEventLocation)
        tvEventFieldsCount = findViewById(R.id.tvEventFieldsCount)
        tvMockBadge = findViewById(R.id.tvMockBadge)
        btnBackToDashboard = findViewById(R.id.btnBackToDashboard)

        val root = findViewById<View>(R.id.publishSuccessRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            findViewById<View>(R.id.topBarContent).setPadding(
                16.dpToPx(),
                statusBar.top,
                16.dpToPx(),
                0
            )

            findViewById<View>(R.id.stickyBottomBar).setPadding(
                20.dpToPx(),
                16.dpToPx(),
                20.dpToPx(),
                16.dpToPx() + navBar.bottom
            )
            insets
        }
    }

    private fun populateEventData() {
        val currentEvent = event ?: return

        if (isViewMode) {
            tvTopBarTitle.text = getString(R.string.organizer_portal_title)
            tvPublishTitle.text = currentEvent.name
            tvPublishSubtitle.text = "Share this ProPass QR code with attendees to allow instant registration."
            tvMockBadge.text = "Active Event • Published"
            ivPublishBadgeIcon.setImageResource(R.drawable.ic_badge)
            ivPublishBadgeIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
        } else {
            tvTopBarTitle.text = getString(R.string.publish_success_title)
            tvPublishTitle.text = getString(R.string.publish_success_title)
            tvPublishSubtitle.text = getString(R.string.publish_success_desc)
            tvMockBadge.text = getString(R.string.publish_mock_badge)
            ivPublishBadgeIcon.setImageResource(R.drawable.ic_check)
            ivPublishBadgeIcon.setColorFilter(Color.parseColor("#4CAF50"))
        }

        tvEventName.text = currentEvent.name
        if (currentEvent.description.isNotBlank()) {
            tvEventDesc.visibility = View.VISIBLE
            tvEventDesc.text = currentEvent.description
        } else {
            tvEventDesc.visibility = View.GONE
        }

        tvEventDateTime.text = if (currentEvent.startTime.isNotBlank() && currentEvent.endTime.isNotBlank()) {
            "${currentEvent.date} • ${currentEvent.startTime} - ${currentEvent.endTime}"
        } else {
            currentEvent.date
        }

        tvEventLocation.text = currentEvent.location
        tvQrPayload.text = currentEvent.qrPayload

        val defaultCount = currentEvent.questions.count { it.isDefaultField }
        val customCount = currentEvent.questions.count { !it.isDefaultField }
        tvEventFieldsCount.text = "$defaultCount default fields, $customCount custom questions"

        renderQrCode(currentEvent.qrPayload)
    }

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
            ivEventQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render QR code bitmap", e)
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            navigateToDashboard()
        }

        btnBackToDashboard.setOnClickListener {
            navigateToDashboard()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, OrganizerDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        navigateToDashboard()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
