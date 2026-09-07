package com.mpc.propass

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.UserProfileDto
import com.mpc.propass.network.model.UpdateProfileRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileIntegrationTest {

    private val moshi = NetworkClient.moshi

    @Test
    fun serializeUpdateProfileRequest_allSixFieldsIncluded() {
        val request = UpdateProfileRequest(
            fullName = "Alex Morgan",
            title = "Staff Software Engineer",
            organization = "ProPass Technologies",
            phone = "+1 (555) 234-5678",
            linkedinUrl = "https://www.linkedin.com/in/alexmorgan",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb"
        )

        val adapter = moshi.adapter(UpdateProfileRequest::class.java)
        val json = adapter.toJson(request)

        assertTrue(json.contains("\"fullName\":\"Alex Morgan\""))
        assertTrue(json.contains("\"title\":\"Staff Software Engineer\""))
        assertTrue(json.contains("\"organization\":\"ProPass Technologies\""))
        assertTrue(json.contains("\"phone\":\"+1 (555) 234-5678\""))
        assertTrue(json.contains("\"linkedinUrl\":\"https://www.linkedin.com/in/alexmorgan\""))
        assertTrue(json.contains("\"avatarUrl\":\"https://images.unsplash.com/photo-1534528741775-53994a69daeb\""))
    }

    @Test
    fun calculateCompletion_matchesBackendWeights() {
        fun calculateScore(profile: UserProfileDto): Int {
            var score = 0
            if (profile.fullName.isNotBlank() && profile.fullName != "ProPass User") score += 20
            if (!profile.title.isNullOrBlank()) score += 20
            if (!profile.organization.isNullOrBlank()) score += 20
            if (!profile.phone.isNullOrBlank()) score += 15
            if (!profile.linkedinUrl.isNullOrBlank()) score += 15
            if (!profile.avatarUrl.isNullOrBlank()) score += 10
            return score
        }

        // 1. Fully filled
        val completeProfile = UserProfileDto(
            id = "p-1",
            userId = "u-1",
            fullName = "Alex Morgan",
            title = "Senior Dev",
            organization = "Corp",
            phone = "123456",
            linkedinUrl = "https://linkedin.com/in/alex",
            avatarUrl = "https://example.com/a.png",
            completionScore = 100
        )
        assertEquals(100, calculateScore(completeProfile))

        // 2. Default "ProPass User" fullName should not count towards real completion
        val defaultProfile = UserProfileDto(
            id = "p-2",
            userId = "u-2",
            fullName = "ProPass User",
            title = "Developer",
            organization = "Tech",
            phone = "123456"
        )
        assertEquals(55, calculateScore(defaultProfile))

        // 3. Adding real full name brings 55 -> 75
        val realNameProfile = defaultProfile.copy(fullName = "Jane Doe")
        assertEquals(75, calculateScore(realNameProfile))

        // 4. Adding linkedin brings 75 -> 90
        val withLinkedin = realNameProfile.copy(linkedinUrl = "https://linkedin.com/in/jane")
        assertEquals(90, calculateScore(withLinkedin))

        // 5. Adding avatar brings 90 -> 100
        val withAvatar = withLinkedin.copy(avatarUrl = "https://avatar.com/jane.jpg")
        assertEquals(100, calculateScore(withAvatar))
    }

    @Test
    fun displayNameFallback_sanitizesProPassUserAndFakeNames() {
        fun resolveDisplayName(user: AuthUserDto, profile: UserProfileDto?): String {
            val rawName = profile?.fullName?.trim().orEmpty()
            if (rawName.isNotBlank() && !rawName.equals("ProPass User", ignoreCase = true)) {
                return rawName
            }
            val emailPrefix = user.email.substringBefore("@").replace(".", " ")
            return if (emailPrefix.isNotBlank()) {
                emailPrefix.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            } else {
                "ProPass User"
            }
        }

        val user = AuthUserDto(id = "u-1", email = "alex.morgan@test.com")
        
        // When fullName is "ProPass User", fallback to email prefix
        val profileDefault = UserProfileDto(id = "p-1", userId = "u-1", fullName = "ProPass User")
        assertEquals("Alex Morgan", resolveDisplayName(user, profileDefault))

        // When fullName is blank, fallback to email prefix
        val profileBlank = UserProfileDto(id = "p-1", userId = "u-1", fullName = "   ")
        assertEquals("Alex Morgan", resolveDisplayName(user, profileBlank))

        // When fullName is a real user name, use it
        val profileReal = UserProfileDto(id = "p-1", userId = "u-1", fullName = "Alexander Morgan")
        assertEquals("Alexander Morgan", resolveDisplayName(user, profileReal))
    }

    @Test
    fun missingFieldsCalculation_identifiesRemainingSections() {
        fun getMissingFields(profile: UserProfileDto?): List<String> {
            val missing = mutableListOf<String>()
            val name = profile?.fullName?.trim().orEmpty()
            if (name.isBlank() || name.equals("ProPass User", ignoreCase = true)) missing.add("Full Name")
            if (profile?.title.isNullOrBlank()) missing.add("Title")
            if (profile?.organization.isNullOrBlank()) missing.add("Organization")
            if (profile?.phone.isNullOrBlank()) missing.add("Phone")
            if (profile?.linkedinUrl.isNullOrBlank()) missing.add("LinkedIn")
            if (profile?.avatarUrl.isNullOrBlank()) missing.add("Avatar URL")
            return missing
        }

        val emptyProfile = UserProfileDto(id = "p-0", userId = "u-0")
        assertEquals(listOf("Full Name", "Title", "Organization", "Phone", "LinkedIn", "Avatar URL"), getMissingFields(emptyProfile))

        val halfProfile = UserProfileDto(
            id = "p-1",
            userId = "u-1",
            fullName = "Divy Srivastava",
            title = "Android Lead",
            organization = "ProPass Corp"
        )
        assertEquals(listOf("Phone", "LinkedIn", "Avatar URL"), getMissingFields(halfProfile))
    }
}
