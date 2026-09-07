package com.mpc.propass.util

/**
 * Result types for email input validation.
 */
sealed class EmailValidation {
    object Valid : EmailValidation()
    object Empty : EmailValidation()
    object InvalidFormat : EmailValidation()
}

/**
 * Result types for password input validation.
 */
sealed class PasswordValidation {
    object Valid : PasswordValidation()
    object Empty : PasswordValidation()
    object TooShort : PasswordValidation()
    object TooLong : PasswordValidation()
}

/**
 * Result types for confirm password input validation.
 */
sealed class ConfirmPasswordValidation {
    object Valid : ConfirmPasswordValidation()
    object Empty : ConfirmPasswordValidation()
    object Mismatch : ConfirmPasswordValidation()
}

/**
 * Reusable validator for authentication inputs (login and sign up).
 * Uses platform-independent regex for unit-testability without Android mocks.
 */
object AuthInputValidator {

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Validates email address format and presence.
     */
    fun validateEmail(email: String?): EmailValidation {
        val trimmed = email?.trim()
        return when {
            trimmed.isNullOrEmpty() -> EmailValidation.Empty
            !EMAIL_REGEX.matches(trimmed) -> EmailValidation.InvalidFormat
            else -> EmailValidation.Valid
        }
    }

    /**
     * Validates password presence and length (8 to 128 characters per backend contract).
     */
    fun validatePassword(password: String?): PasswordValidation {
        val trimmed = password?.trim()
        return when {
            trimmed.isNullOrEmpty() -> PasswordValidation.Empty
            trimmed.length < 8 -> PasswordValidation.TooShort
            trimmed.length > 128 -> PasswordValidation.TooLong
            else -> PasswordValidation.Valid
        }
    }

    /**
     * Validates confirm password matches the original password.
     */
    fun validateConfirmPassword(password: String?, confirmPassword: String?): ConfirmPasswordValidation {
        val trimmedConfirm = confirmPassword?.trim()
        return when {
            trimmedConfirm.isNullOrEmpty() -> ConfirmPasswordValidation.Empty
            password?.trim() != trimmedConfirm -> ConfirmPasswordValidation.Mismatch
            else -> ConfirmPasswordValidation.Valid
        }
    }
}
