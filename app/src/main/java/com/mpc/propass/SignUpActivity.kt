package com.mpc.propass

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.mpc.propass.util.AuthInputValidator
import com.mpc.propass.util.ConfirmPasswordValidation
import com.mpc.propass.util.EmailValidation
import com.mpc.propass.util.PasswordValidation
import kotlinx.coroutines.launch

/**
 * Account Registration Screen implementation for ProPass Digital Identity System.
 * Matches the exact visual styling and design language of LoginActivity:
 * - App Icon (96dp x 96dp) with subtle elevation
 * - "Create Account" header & subtitle
 * - Floating filled text inputs (Email, Password, Confirm Password)
 * - Primary 56dp Sign Up button with tactile feedback & submission guard
 * - Form validation with shake animations
 * - Clear error mapping for 400 validation, 409 conflict, and network issues
 * - Session persistence via AuthRepository and immediate navigation to HomeDashboardActivity
 */
class SignUpActivity : AppCompatActivity() {

    private val authRepository: AuthRepository by lazy {
        (application as? ProPassApplication)?.authRepository
            ?: AuthRepositoryImpl(tokenStorage = DataStoreTokenStorage.create(this))
    }

    private lateinit var signUpScrollView: View
    private lateinit var signUpRootContainer: View
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var tvSignInFooter: TextView

    private var isSubmitting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

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
        signUpScrollView = findViewById(R.id.signUpScrollView)
        signUpRootContainer = findViewById(R.id.signUpRootContainer)
        tilEmail = findViewById(R.id.tilEmail)
        etEmail = findViewById(R.id.etEmail)
        tilPassword = findViewById(R.id.tilPassword)
        etPassword = findViewById(R.id.etPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        tvSignInFooter = findViewById(R.id.tvSignInFooter)
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(signUpScrollView) { _, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom) + dpToPx(32f).toInt()
            val topPadding = systemBars.top + dpToPx(32f).toInt()
            val horizPadding = dpToPx(20f).toInt()

            signUpRootContainer.setPadding(horizPadding, topPadding, horizPadding, bottomPadding)
            insets
        }
    }

    /**
     * Styles the footer text: "Already have an account? Sign In" with "Sign In" highlighted in primary blue.
     */
    private fun setupFooterSpan() {
        val fullText = getString(R.string.signup_already_have_account) + getString(R.string.signup_sign_in)
        val spannable = SpannableStringBuilder(fullText)

        val start = fullText.indexOf(getString(R.string.signup_sign_in))
        if (start >= 0) {
            val end = start + getString(R.string.signup_sign_in).length
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
        tvSignInFooter.text = spannable
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

        etConfirmPassword.doAfterTextChanged {
            if (tilConfirmPassword.error != null) {
                tilConfirmPassword.error = null
                tilConfirmPassword.isErrorEnabled = false
            }
        }
    }

    private fun setupButtonListeners() {
        btnSignUp.setOnClickListener {
            handleSignUp()
        }

        tvSignInFooter.setOnClickListener {
            finish()
        }
    }

    /**
     * Validates inputs and submits account registration request to backend API.
     */
    private fun handleSignUp() {
        if (isSubmitting) return

        val email = etEmail.text?.toString()?.trim() ?: ""
        val password = etPassword.text?.toString()?.trim() ?: ""
        val confirmPassword = etConfirmPassword.text?.toString()?.trim() ?: ""

        var isValid = true

        when (AuthInputValidator.validateEmail(email)) {
            EmailValidation.Empty -> {
                tilEmail.error = getString(R.string.error_empty_email)
                shakeView(tilEmail)
                isValid = false
            }
            EmailValidation.InvalidFormat -> {
                tilEmail.error = getString(R.string.error_invalid_email)
                shakeView(tilEmail)
                isValid = false
            }
            EmailValidation.Valid -> Unit
        }

        when (AuthInputValidator.validatePassword(password)) {
            PasswordValidation.Empty -> {
                tilPassword.error = getString(R.string.error_empty_password)
                shakeView(tilPassword)
                isValid = false
            }
            PasswordValidation.TooShort -> {
                tilPassword.error = getString(R.string.error_password_too_short)
                shakeView(tilPassword)
                isValid = false
            }
            PasswordValidation.TooLong -> {
                tilPassword.error = getString(R.string.error_password_too_short)
                shakeView(tilPassword)
                isValid = false
            }
            PasswordValidation.Valid -> Unit
        }

        when (AuthInputValidator.validateConfirmPassword(password, confirmPassword)) {
            ConfirmPasswordValidation.Empty -> {
                tilConfirmPassword.error = getString(R.string.error_empty_confirm_password)
                shakeView(tilConfirmPassword)
                isValid = false
            }
            ConfirmPasswordValidation.Mismatch -> {
                tilConfirmPassword.error = getString(R.string.error_passwords_do_not_match)
                shakeView(tilConfirmPassword)
                isValid = false
            }
            ConfirmPasswordValidation.Valid -> Unit
        }

        if (!isValid) return

        // Submit registration credentials to backend API
        isSubmitting = true
        btnSignUp.isEnabled = false
        btnSignUp.text = getString(R.string.signup_creating_account)
        btnSignUp.alpha = 0.8f

        lifecycleScope.launch {
            val result = authRepository.register(email, password)
            if (!isFinishing && !isDestroyed) {
                isSubmitting = false
                btnSignUp.isEnabled = true
                btnSignUp.text = getString(R.string.signup_btn_text)
                btnSignUp.alpha = 1.0f

                result.onSuccess {
                    Toast.makeText(this@SignUpActivity, getString(R.string.signup_success_toast), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignUpActivity, HomeDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }.onFailure { exception ->
                    val errorMsg = exception.message ?: getString(R.string.error_signup_failed)
                    when {
                        errorMsg.contains("already exists", ignoreCase = true) || errorMsg.contains("Conflict", ignoreCase = true) -> {
                            tilEmail.error = errorMsg
                            shakeView(tilEmail)
                        }
                        errorMsg.contains("password", ignoreCase = true) -> {
                            tilPassword.error = errorMsg
                            shakeView(tilPassword)
                        }
                        else -> {
                            Toast.makeText(this@SignUpActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
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

        btnSignUp.setOnTouchListener(touchListener)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}
