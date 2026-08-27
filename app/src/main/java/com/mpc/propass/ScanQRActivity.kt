package com.mpc.propass

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Scan QR screen with real CameraX preview, physical torch control,
 * Google ML Kit barcode scanner, and ProPass event URL validation.
 *
 * Supported ProPass QR Format:
 * https://propass.id/event/<eventId>
 */
class ScanQRActivity : AppCompatActivity() {

    private lateinit var scanQrRoot: FrameLayout
    private lateinit var cameraPreviewView: PreviewView
    private lateinit var topAppBar: LinearLayout
    private lateinit var btnBack: FrameLayout
    private lateinit var layoutHintCapsule: LinearLayout
    private lateinit var tvScanHint: TextView
    private lateinit var viewScannerLaser: View
    private lateinit var layoutBottomControls: LinearLayout

    private lateinit var btnFlashlight: FrameLayout
    private lateinit var ivFlashlightIcon: ImageView
    private lateinit var btnShutter: FrameLayout
    private lateinit var btnGallery: FrameLayout

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService? = null
    private var barcodeScanner: BarcodeScanner? = null

    private var laserAnimator: ValueAnimator? = null
    private var isFlashlightOn: Boolean = false

    @Volatile
    private var isQrDetected: Boolean = false

    private var lastInvalidToastTimestamp: Long = 0L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            tvScanHint.text = getString(R.string.scan_hint_align_qr)
            startCamera()
        } else {
            tvScanHint.text = "Camera permission is required to scan QR codes."
            Toast.makeText(
                this,
                "Camera permission denied. Grant permission in Settings to enable scanner.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_scan_qr)

        initViews()
        applyWindowInsets()
        initMlKitScanner()
        startLaserAnimation()
        setupInteractions()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onResume() {
        super.onResume()
        isQrDetected = false
        checkCameraPermissionAndStart()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = false
    }

    private fun initViews() {
        scanQrRoot = findViewById(R.id.scanQrRoot)
        cameraPreviewView = findViewById(R.id.cameraPreviewView)
        topAppBar = findViewById(R.id.topAppBar)
        btnBack = findViewById(R.id.btnBack)
        layoutHintCapsule = findViewById(R.id.layoutHintCapsule)
        tvScanHint = findViewById(R.id.tvScanHint)
        viewScannerLaser = findViewById(R.id.viewScannerLaser)
        layoutBottomControls = findViewById(R.id.layoutBottomControls)

        btnFlashlight = findViewById(R.id.btnFlashlight)
        ivFlashlightIcon = findViewById(R.id.ivFlashlightIcon)
        btnShutter = findViewById(R.id.btnShutter)
        btnGallery = findViewById(R.id.btnGallery)

        cameraPreviewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(scanQrRoot) { _, insets ->
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

            // Top Hint Capsule insets
            val hintParams = layoutHintCapsule.layoutParams as FrameLayout.LayoutParams
            hintParams.topMargin = dpToPx(64f).toInt() + systemBars.top + dpToPx(24f).toInt()
            layoutHintCapsule.layoutParams = hintParams

            // Bottom Controls Bar insets
            val bottomControlsParams = layoutBottomControls.layoutParams as FrameLayout.LayoutParams
            bottomControlsParams.bottomMargin = dpToPx(48f).toInt() + systemBars.bottom
            layoutBottomControls.layoutParams = bottomControlsParams

            insets
        }
    }

    private fun initMlKitScanner() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
    }

    private fun checkCameraPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                tvScanHint.text = "Camera permission is required to scan QR codes."
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e("ScanQRActivity", "Failed to get CameraProvider", e)
                Toast.makeText(this, "Unable to initialize camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(cameraPreviewView.surfaceProvider)
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val executor = cameraExecutor ?: ContextCompat.getMainExecutor(this)
        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            processImageProxy(imageProxy)
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            provider.unbindAll()
            camera = provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalysis
            )
            if (isFlashlightOn && camera?.cameraInfo?.hasFlashUnit() == true) {
                camera?.cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            Log.e("ScanQRActivity", "Camera binding failed", e)
        }
    }

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null && !isQrDetected) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            barcodeScanner?.process(image)
                ?.addOnSuccessListener { barcodes ->
                    if (!isQrDetected && barcodes.isNotEmpty()) {
                        val qrBarcode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                        val rawValue = qrBarcode?.rawValue
                        if (!rawValue.isNullOrBlank() && !isQrDetected) {
                            handleScannedQrCode(rawValue)
                        }
                    }
                }
                ?.addOnFailureListener { e ->
                    Log.w("ScanQRActivity", "Barcode recognition failure", e)
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    /**
     * Validates decoded QR content against the ProPass event format:
     * https://propass.id/event/<eventId>
     */
    private fun parseProPassEventId(qrContent: String): String? {
        if (qrContent.isBlank()) return null
        return try {
            val pattern = Regex("^(?:https?)://(?:www\\.)?propass\\.id/event/([a-zA-Z0-9_-]+)/?$", RegexOption.IGNORE_CASE)
            val match = pattern.find(qrContent.trim())
            match?.groupValues?.get(1)?.lowercase()?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun handleScannedQrCode(qrValue: String) {
        Log.d("ScanQRActivity", "QR detected: $qrValue")

        val eventId = parseProPassEventId(qrValue)
        if (eventId != null) {
            // Valid ProPass Event QR
            if (isQrDetected) return
            isQrDetected = true

            Log.d("ScanQRActivity", "ProPass event ID: $eventId")

            runOnUiThread {
                scanQrRoot.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                Toast.makeText(
                    this,
                    "ProPass Event Detected: $eventId",
                    Toast.LENGTH_SHORT
                ).show()

                // Turn off torch before navigating
                if (isFlashlightOn) {
                    camera?.cameraControl?.enableTorch(false)
                }

                // Unbind camera use cases to prevent further processing
                cameraProvider?.unbindAll()

                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, SmartFormRegistrationActivity::class.java).apply {
                        putExtra(SmartFormRegistrationActivity.EXTRA_EVENT_ID, eventId)
                        putExtra("EXTRA_SCANNED_QR", qrValue)
                    }
                    startActivity(intent)
                }, 350)
            }
        } else {
            // Invalid / Non-ProPass QR Code
            Log.w("ScanQRActivity", "QR rejected: Not a ProPass event QR ($qrValue)")

            val now = SystemClock.elapsedRealtime()
            if (now - lastInvalidToastTimestamp > 1500) {
                lastInvalidToastTimestamp = now
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Not a ProPass event QR",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Continuous 2000ms laser scanning animation across the 256dp frame.
     */
    private fun startLaserAnimation() {
        val travelDistancePx = dpToPx(252f)

        laserAnimator = ValueAnimator.ofFloat(0f, travelDistancePx).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                viewScannerLaser.translationY = value
                val fraction = animation.animatedFraction
                viewScannerLaser.alpha = when {
                    fraction < 0.1f -> fraction * 10f
                    fraction > 0.9f -> (1f - fraction) * 10f
                    else -> 1f
                }
            }
            start()
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

        val touchListener90 = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(100).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
            false
        }

        // Back Button
        btnBack.setOnTouchListener(touchListener90)
        btnBack.setOnClickListener {
            finish()
        }

        // Real Physical Flashlight Toggle
        btnFlashlight.setOnTouchListener(touchListener95)
        btnFlashlight.setOnClickListener {
            toggleFlashlight()
        }

        // Gallery Button
        btnGallery.setOnTouchListener(touchListener95)
        btnGallery.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_gallery_coming_soon), Toast.LENGTH_SHORT).show()
        }

        // Center Shutter Button (Tactile touch & re-align guidance feedback)
        btnShutter.setOnTouchListener(touchListener90)
        btnShutter.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            if (isQrDetected) {
                isQrDetected = false
                bindCameraUseCases()
            } else {
                Toast.makeText(
                    this,
                    "Align any ProPass QR code within the frame to auto-scan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun toggleFlashlight() {
        val cam = camera
        if (cam == null || cam.cameraInfo.hasFlashUnit() == false) {
            Toast.makeText(this, "Flashlight unavailable on this device", Toast.LENGTH_SHORT).show()
            return
        }

        isFlashlightOn = !isFlashlightOn
        cam.cameraControl.enableTorch(isFlashlightOn)

        if (isFlashlightOn) {
            ivFlashlightIcon.setImageResource(R.drawable.ic_flashlight_on)
            ivFlashlightIcon.setColorFilter(Color.parseColor("#FFE082"))
            btnFlashlight.setBackgroundResource(R.drawable.bg_camera_control_circle_active)
        } else {
            ivFlashlightIcon.setImageResource(R.drawable.ic_flashlight_off)
            ivFlashlightIcon.setColorFilter(Color.WHITE)
            btnFlashlight.setBackgroundResource(R.drawable.bg_camera_control_circle)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        laserAnimator?.cancel()
        laserAnimator = null
        if (isFlashlightOn) {
            camera?.cameraControl?.enableTorch(false)
        }
        cameraExecutor?.shutdown()
        cameraExecutor = null
        barcodeScanner?.close()
        barcodeScanner = null
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
