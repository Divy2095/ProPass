package com.mpc.propass

import com.mpc.propass.util.AuthInputValidator
import com.mpc.propass.util.ConfirmPasswordValidation
import com.mpc.propass.util.EmailValidation
import com.mpc.propass.util.PasswordValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthInputValidatorTest {

    @Test
    fun testValidateEmail_emptyOrBlank_returnsEmpty() {
        assertEquals(EmailValidation.Empty, AuthInputValidator.validateEmail(null))
        assertEquals(EmailValidation.Empty, AuthInputValidator.validateEmail(""))
        assertEquals(EmailValidation.Empty, AuthInputValidator.validateEmail("   "))
    }

    @Test
    fun testValidateEmail_invalidFormats_returnsInvalidFormat() {
        assertEquals(EmailValidation.InvalidFormat, AuthInputValidator.validateEmail("not-an-email"))
        assertEquals(EmailValidation.InvalidFormat, AuthInputValidator.validateEmail("user@"))
        assertEquals(EmailValidation.InvalidFormat, AuthInputValidator.validateEmail("@example.com"))
        assertEquals(EmailValidation.InvalidFormat, AuthInputValidator.validateEmail("user@example"))
        assertEquals(EmailValidation.InvalidFormat, AuthInputValidator.validateEmail("user@.com"))
    }

    @Test
    fun testValidateEmail_validFormats_returnsValid() {
        assertEquals(EmailValidation.Valid, AuthInputValidator.validateEmail("alex.morgan@example.com"))
        assertEquals(EmailValidation.Valid, AuthInputValidator.validateEmail("user+tag@domain.co.uk"))
        assertEquals(EmailValidation.Valid, AuthInputValidator.validateEmail("test_123@sub.domain.org"))
    }

    @Test
    fun testValidatePassword_emptyOrBlank_returnsEmpty() {
        assertEquals(PasswordValidation.Empty, AuthInputValidator.validatePassword(null))
        assertEquals(PasswordValidation.Empty, AuthInputValidator.validatePassword(""))
        assertEquals(PasswordValidation.Empty, AuthInputValidator.validatePassword("   "))
    }

    @Test
    fun testValidatePassword_tooShort_returnsTooShort() {
        assertEquals(PasswordValidation.TooShort, AuthInputValidator.validatePassword("1234567"))
        assertEquals(PasswordValidation.TooShort, AuthInputValidator.validatePassword("short"))
        assertEquals(PasswordValidation.TooShort, AuthInputValidator.validatePassword("a"))
    }

    @Test
    fun testValidatePassword_tooLong_returnsTooLong() {
        val longPassword = "a".repeat(129)
        assertEquals(PasswordValidation.TooLong, AuthInputValidator.validatePassword(longPassword))
    }

    @Test
    fun testValidatePassword_validLength_returnsValid() {
        assertEquals(PasswordValidation.Valid, AuthInputValidator.validatePassword("12345678"))
        assertEquals(PasswordValidation.Valid, AuthInputValidator.validatePassword("SecurePassword123!"))
        val maxValidPassword = "a".repeat(128)
        assertEquals(PasswordValidation.Valid, AuthInputValidator.validatePassword(maxValidPassword))
    }

    @Test
    fun testValidateConfirmPassword_empty_returnsEmpty() {
        assertEquals(
            ConfirmPasswordValidation.Empty,
            AuthInputValidator.validateConfirmPassword("Password123!", null)
        )
        assertEquals(
            ConfirmPasswordValidation.Empty,
            AuthInputValidator.validateConfirmPassword("Password123!", "")
        )
        assertEquals(
            ConfirmPasswordValidation.Empty,
            AuthInputValidator.validateConfirmPassword("Password123!", "   ")
        )
    }

    @Test
    fun testValidateConfirmPassword_mismatch_returnsMismatch() {
        assertEquals(
            ConfirmPasswordValidation.Mismatch,
            AuthInputValidator.validateConfirmPassword("Password123!", "Password456!")
        )
        assertEquals(
            ConfirmPasswordValidation.Mismatch,
            AuthInputValidator.validateConfirmPassword("Password123!", "password123!")
        )
    }

    @Test
    fun testValidateConfirmPassword_match_returnsValid() {
        assertEquals(
            ConfirmPasswordValidation.Valid,
            AuthInputValidator.validateConfirmPassword("Password123!", "Password123!")
        )
    }
}
