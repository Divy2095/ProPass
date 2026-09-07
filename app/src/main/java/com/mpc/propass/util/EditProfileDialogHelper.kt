package com.mpc.propass.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mpc.propass.R
import com.mpc.propass.data.repository.UserRepository
import com.mpc.propass.network.model.UpdateProfileRequest
import com.mpc.propass.network.model.UserProfileDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reusable helper that exposes the complete 6-field Edit Profile dialog
 * across HomeDashboardActivity and ProfileActivity.
 */
object EditProfileDialogHelper {

    fun showEditProfileDialog(
        context: Context,
        coroutineScope: CoroutineScope,
        userRepository: UserRepository,
        currentProfile: UserProfileDto?,
        onProfileUpdated: (UserProfileDto) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_profile, null)
        val tilFullName = dialogView.findViewById<TextInputLayout>(R.id.tilProfileFullName)
        val etFullName = dialogView.findViewById<TextInputEditText>(R.id.etProfileFullName)
        val tilTitle = dialogView.findViewById<TextInputLayout>(R.id.tilProfileTitle)
        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etProfileTitle)
        val tilOrg = dialogView.findViewById<TextInputLayout>(R.id.tilProfileOrg)
        val etOrg = dialogView.findViewById<TextInputEditText>(R.id.etProfileOrg)
        val tilPhone = dialogView.findViewById<TextInputLayout>(R.id.tilProfilePhone)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etProfilePhone)
        val tilLinkedin = dialogView.findViewById<TextInputLayout>(R.id.tilProfileLinkedin)
        val etLinkedin = dialogView.findViewById<TextInputEditText>(R.id.etProfileLinkedin)
        val tilAvatar = dialogView.findViewById<TextInputLayout>(R.id.tilProfileAvatar)
        val etAvatar = dialogView.findViewById<TextInputEditText>(R.id.etProfileAvatar)

        // Pre-fill existing data (filtering out fake "ProPass User")
        val existingName = currentProfile?.fullName?.takeUnless {
            it.isBlank() || it.equals("ProPass User", ignoreCase = true)
        } ?: ""
        etFullName.setText(existingName)
        etTitle.setText(currentProfile?.title ?: "")
        etOrg.setText(currentProfile?.organization ?: "")
        etPhone.setText(currentProfile?.phone ?: "")
        etLinkedin.setText(currentProfile?.linkedinUrl ?: "")
        etAvatar.setText(currentProfile?.avatarUrl ?: "")

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.dialog_edit_profile_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val fullName = etFullName.text?.toString()?.trim() ?: ""
                val title = etTitle.text?.toString()?.trim() ?: ""
                val org = etOrg.text?.toString()?.trim() ?: ""
                val phone = etPhone.text?.toString()?.trim() ?: ""
                val linkedin = etLinkedin.text?.toString()?.trim() ?: ""
                val avatar = etAvatar.text?.toString()?.trim() ?: ""

                // Validation
                if (fullName.isEmpty()) {
                    tilFullName.error = context.getString(R.string.error_full_name_empty)
                    return@setOnClickListener
                } else {
                    tilFullName.error = null
                }

                if (linkedin.isNotEmpty() && !linkedin.startsWith("http://") && !linkedin.startsWith("https://")) {
                    tilLinkedin.error = context.getString(R.string.error_invalid_url)
                    return@setOnClickListener
                } else {
                    tilLinkedin.error = null
                }

                if (avatar.isNotEmpty() && !avatar.startsWith("http://") && !avatar.startsWith("https://")) {
                    tilAvatar.error = context.getString(R.string.error_invalid_url)
                    return@setOnClickListener
                } else {
                    tilAvatar.error = null
                }

                saveBtn.isEnabled = false
                saveBtn.text = "Saving..."

                coroutineScope.launch {
                    val request = UpdateProfileRequest(
                        fullName = fullName,
                        title = title.ifBlank { null },
                        organization = org.ifBlank { null },
                        phone = phone.ifBlank { null },
                        linkedinUrl = linkedin.ifBlank { null },
                        avatarUrl = avatar.ifBlank { null }
                    )

                    val result = userRepository.updateUserProfile(request)
                    saveBtn.isEnabled = true
                    saveBtn.text = context.getString(R.string.btn_save)

                    result.onSuccess { responseData ->
                        Toast.makeText(context, R.string.profile_updated_toast, Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        responseData.profile?.let { onProfileUpdated(it) }
                    }.onFailure { error ->
                        Toast.makeText(
                            context,
                            error.message ?: "Failed to update profile",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }
}
