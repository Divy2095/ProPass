package com.mpc.propass

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mpc.propass.data.local.DataStoreTokenStorage
import com.mpc.propass.data.repository.AuthRepository
import com.mpc.propass.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.launch

/**
 * Login Screen implementation for ProPass Digital Identity System.
 * Matches the exact Stitch design specifications:
 * - ProPass Logo in 96dp x 96dp card
 * - Welcome back header & subtitle
 * - Floating filled text inputs with 12dp radius (#EDEEEF background)
 * - Password visibility toggle
 * - Primary 56dp Login button with tactile press feedback
 * - "OR" divider & Google sign-in button
 * - Form validation with shake animation
 */
class LoginActivity : AppCompatActivity() {

    private val authRepository: AuthRepository by lazy {
        (application as? ProPassApplication)?.authRepository
            ?: AuthRepositoryImpl(tokenStorage = DataStoreTokenStorage.create(this))
    }

    private lateinit var loginScrollView: View
    private lateinit var loginRootContainer: View
    private lateinit var loginContentColumn: View
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvSignUpFooter: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_login)

        initViews()
        applyWindowInsets()
        setupFooterSpan()
        setupTextWatchers()
        setupButtonListeners()
        setupTactileFeedback()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        loginScrollView = findViewById(R.id.loginScrollView)
        loginRootContainer = findViewById(R.id.loginRootContainer)
        loginContentColumn = findViewById(R.id.loginContentColumn)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnGoogle = findViewById(R.id.btnGoogle)
        tvSignUpFooter = findViewById(R.id.tvSignUpFooter)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(loginScrollView) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom) + dpToPx(32f).toInt()
            val topPadding = systemBars.top + dpToPx(32f).toInt()
            val horizPadding = dpToPx(20f).toInt()

            loginRootContainer.setPadding(horizPadding, topPadding, horizPadding, bottomPadding)
            insets
        }
    }

    /**
     * Styles the footer text: "Don't have an account? Sign Up" with "Sign Up" highlighted in primary blue.
     */
    private fun setupFooterSpan() {
        val fullText = getString(R.string.login_dont_have_account) + getString(R.string.login_sign_up)
        val spannable = SpannableStringBuilder(fullText)

        val start = fullText.indexOf(getString(R.string.login_sign_up))
        if (start >= 0) {
            val end = start + getString(R.string.login_sign_up).length
            val primaryColor = ContextCompat.getColor(this, R.color.primary)
            spannable.setSpan(
                ForegroundColorSpan(primaryColor),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvSignUpFooter.text = spannable
    }

    private fun setupTextWatchers() {
        etEmail.doAfterTextChanged {
            if (tilEmail.error != null) {
                tilEmail.error = null
                tilEmail.isErrorEnabled = false
            }
        }

        etPassword.doAfterTextChanged {
            if (tilPassword.error != null) {
                tilPassword.error = null
                tilPassword.isErrorEnabled = false
            }
        }
    }

    private fun setupButtonListeners() {
        btnLogin.setOnClickListener {
            handleLogin()
        }

        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Continue with Google", Toast.LENGTH_SHORT).show()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Forgot password?", Toast.LENGTH_SHORT).show()
        }

        tvSignUpFooter.setOnClickListener {
            val intent = Intent(this, OnboardingProfileCreationActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Validates email & password inputs and displays visual feedback matching the Stitch animation specs.
     */
    private fun handleLogin() {
        if (isSubmitting) return

        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""

        var isValid = true

        if (TextUtils.isEmpty(email)) {
            tilEmail.error = getString(R.string.error_empty_email)
            shakeView(tilEmail)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = getString(R.string.error_invalid_email)
            shakeView(tilEmail)
            isValid = false
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.error = getString(R.string.error_empty_password)
            shakeView(tilPassword)
            isValid = false
        }

        if (!isValid) return

        // Submit credentials to backend API
        isSubmitting = true
        btnLogin.isEnabled = false
        btnLogin.text = getString(R.string.login_logging_in)
        btnLogin.alpha = 0.8f

        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            if (!isFinishing && !isDestroyed) {
                isSubmitting = false
                btnLogin.isEnabled = true
                btnLogin.text = getString(R.string.login_btn_text)
                btnLogin.alpha = 1.0f

                result.onSuccess {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_success_toast), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, OnboardingProfileCreationActivity::class.java)
                    startActivity(intent)
                    finish()
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: getString(R.string.error_login_failed)
                    tilPassword.error = errorMsg
                    shakeView(tilPassword)
                    Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Subtle horizontal shake animation when input validation fails.
     */
    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(
            view,
            View.TRANSLATION_X,
            0f, -12f, 12f, -8f, 8f, -4f, 4f, 0f
        )
        shake.duration = 500
        shake.interpolator = AccelerateDecelerateInterpolator()
        shake.start()
    }

    /**
     * Adds tactile scale feedback (0.98x) on touch press.
     */
    private fun setupTactileFeedback() {
        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }
            }
            false
        }

        btnLogin.setOnTouchListener(touchListener)
        btnGoogle.setOnTouchListener(touchListener)
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
    }
}
