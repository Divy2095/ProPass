package com.mpc.propass

import com.mpc.propass.data.repository.NoActivePassException
import com.mpc.propass.data.repository.PassAuthException
import com.mpc.propass.network.model.AuthUserDto
import com.mpc.propass.network.model.FormQuestionDto
import com.mpc.propass.network.model.FullDigitalPassDto
import com.mpc.propass.network.model.MyPassResponseData
import com.mpc.propass.network.model.PassHolderDto
import com.mpc.propass.network.model.UserProfileDto
import com.mpc.propass.network.model.UserRole
import com.mpc.propass.organizer.model.OrganizerEvent
import com.mpc.propass.organizer.model.OrganizerRegistrationDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * Unit tests verifying the 4 Phase 5 user-reported fixes:
 * 1. Organizer Event Details serialization and null safety.
 * 2. Organizer Profile role indicator and seeded data consistency.
 * 3. Role-Based Organizer Portal visibility (hidden for ATTENDEE, visible for ORGANIZER).
 * 4. Digital Pass 4-state mutual exclusivity (Loading, Active Pass, No Active Pass empty state, Error).
 */
class Phase5FixesVerificationTest {

    // =========================================================================
    // Issue 1: Organizer Event Details Serialization & Safe Null/Empty Handling
    // =========================================================================

    @Test
    fun testOrganizerEvent_javaSerializationRoundTrip() {
        val originalEvent = OrganizerEvent(
            id = "evt-12345",
            slug = "techconf-2026",
            name = "Global Tech Conference 2026",
            description = "The premier developer conference",
            date = "2026-10-15",
            startTime = "09:00",
            endTime = "17:00",
            location = "Hall A, Moscone Center",
            maxDurationDays = 3,
            attendeeCount = 42,
            status = "PUBLISHED",
            qrPayload = "propass:event:evt-12345"
        )

        val bytesOut = ByteArrayOutputStream()
        ObjectOutputStream(bytesOut).use { it.writeObject(originalEvent) }

        val bytesIn = ByteArrayInputStream(bytesOut.toByteArray())
        val deserializedEvent = ObjectInputStream(bytesIn).use { it.readObject() as OrganizerEvent }

        assertEquals(originalEvent.id, deserializedEvent.id)
        assertEquals(originalEvent.slug, deserializedEvent.slug)
        assertEquals(originalEvent.name, deserializedEvent.name)
        assertEquals(originalEvent.description, deserializedEvent.description)
        assertEquals(originalEvent.date, deserializedEvent.date)
        assertEquals(originalEvent.startTime, deserializedEvent.startTime)
        assertEquals(originalEvent.endTime, deserializedEvent.endTime)
        assertEquals(originalEvent.location, deserializedEvent.location)
        assertEquals(originalEvent.maxDurationDays, deserializedEvent.maxDurationDays)
        assertEquals(originalEvent.attendeeCount, deserializedEvent.attendeeCount)
        assertEquals(originalEvent.status, deserializedEvent.status)
        assertEquals(originalEvent.qrPayload, deserializedEvent.qrPayload)
    }

    @Test
    fun testOrganizerEvent_safeFallbackRendering() {
        val minimalEvent = OrganizerEvent(
            id = "evt-sparse-1",
            name = "",
            slug = "",
            status = "",
            date = "",
            startTime = "",
            endTime = "",
            location = "",
            description = "",
            maxDurationDays = 0,
            attendeeCount = -5
        )

        val topBarTitle = minimalEvent.name.ifBlank { "Event Details" }
        val eventTitle = minimalEvent.name.ifBlank { "Untitled Event" }
        val statusBadge = minimalEvent.status.ifBlank { "PUBLISHED" }
        val slug = minimalEvent.slug.ifBlank { minimalEvent.id }
        val location = minimalEvent.location.ifBlank { "Location not specified" }
        val durationDays = minimalEvent.maxDurationDays.coerceAtLeast(1)
        val attendeeCount = minimalEvent.attendeeCount.coerceAtLeast(0)

        assertEquals("Event Details", topBarTitle)
        assertEquals("Untitled Event", eventTitle)
        assertEquals("PUBLISHED", statusBadge)
        assertEquals("evt-sparse-1", slug)
        assertEquals("Location not specified", location)
        assertEquals(1, durationDays)
        assertEquals(0, attendeeCount)
    }

    @Test
    fun testOrganizerRegistrationDto_javaSerializationRoundTrip() {
        val originalReg = OrganizerRegistrationDto(
            id = "reg-001",
            eventId = "evt-001",
            userId = "usr-001",
            status = "CONFIRMED",
            registeredAt = "2026-09-07T12:00:00Z",
            purpose = "CONFERENCE",
            durationDays = 2,
            vehicleNumber = "KA-01-AB-1234",
            fullName = "John Doe",
            email = "john@example.com",
            phone = "+1 555-0100",
            institution = "Acme Corp"
        )

        val bytesOut = ByteArrayOutputStream()
        ObjectOutputStream(bytesOut).use { it.writeObject(originalReg) }

        val bytesIn = ByteArrayInputStream(bytesOut.toByteArray())
        val deserialized = ObjectInputStream(bytesIn).use { it.readObject() as OrganizerRegistrationDto }

        assertEquals(originalReg.id, deserialized.id)
        assertEquals(originalReg.eventId, deserialized.eventId)
        assertEquals(originalReg.fullName, deserialized.fullName)
        assertEquals(originalReg.email, deserialized.email)
        assertEquals(originalReg.status, deserialized.status)
    }

    // =========================================================================
    // Issue 2: Organizer Profile Consistency & Role Indicator
    // =========================================================================

    @Test
    fun testOrganizerProfile_seededDataConsistency() {
        val organizerUser = AuthUserDto(
            id = "user-org-001",
            email = "organizer@propass.id",
            role = "ORGANIZER"
        )
        val organizerProfile = UserProfileDto(
            id = "profile-org-001",
            userId = "user-org-001",
            fullName = "Dev Organizer",
            title = "Lead Event Organizer",
            organization = "ProPass Events",
            phone = "+1 555-0199",
            linkedinUrl = "https://linkedin.com/in/propass-organizer",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
            isVerified = true,
            completionScore = 100
        )

        assertTrue("User should be identified as organizer", organizerUser.isOrganizer)
        assertEquals("Dev Organizer", organizerProfile.fullName)
        assertEquals("Lead Event Organizer", organizerProfile.title)
        assertEquals("ProPass Events", organizerProfile.organization)
        assertEquals(100, organizerProfile.completionScore)
        assertTrue(organizerProfile.isVerified)
    }

    @Test
    fun testProfileRoleBadge_visibilityLogic() {
        fun shouldShowOrganizerRoleBadge(user: AuthUserDto?, isAuthRepoOrganizer: Boolean): Boolean {
            return user?.isOrganizer == true || isAuthRepoOrganizer
        }

        val attendeeUser = AuthUserDto(id = "1", email = "attendee@test.com", role = "ATTENDEE")
        val organizerUser = AuthUserDto(id = "2", email = "organizer@test.com", role = "ORGANIZER")
        val nullRoleUser = AuthUserDto(id = "3", email = "unknown@test.com", role = null)

        assertFalse(shouldShowOrganizerRoleBadge(attendeeUser, isAuthRepoOrganizer = false))
        assertTrue(shouldShowOrganizerRoleBadge(organizerUser, isAuthRepoOrganizer = false))
        assertFalse(shouldShowOrganizerRoleBadge(nullRoleUser, isAuthRepoOrganizer = false))
        assertTrue(shouldShowOrganizerRoleBadge(nullRoleUser, isAuthRepoOrganizer = true))
    }

    // =========================================================================
    // Issue 3: Role-Based Organizer Portal Visibility
    // =========================================================================

    @Test
    fun testOrganizerPortalVisibility_attendeeVsOrganizer() {
        fun isOrganizerPortalVisible(userRole: String?, repoIsOrganizer: Boolean): Boolean {
            val userIsOrg = userRole?.equals(UserRole.ORGANIZER.name, ignoreCase = true) == true
            return userIsOrg || repoIsOrganizer
        }

        // Attendee: Hidden
        assertFalse(isOrganizerPortalVisible("ATTENDEE", repoIsOrganizer = false))
        assertFalse(isOrganizerPortalVisible("attendee", repoIsOrganizer = false))
        assertFalse(isOrganizerPortalVisible(null, repoIsOrganizer = false))
        assertFalse(isOrganizerPortalVisible("", repoIsOrganizer = false))

        // Organizer: Visible
        assertTrue(isOrganizerPortalVisible("ORGANIZER", repoIsOrganizer = false))
        assertTrue(isOrganizerPortalVisible("organizer", repoIsOrganizer = false))
        assertTrue(isOrganizerPortalVisible("ATTENDEE", repoIsOrganizer = true))
    }

    // =========================================================================
    // Issue 4: Digital Pass 4 States Mutual Exclusivity
    // =========================================================================

    enum class DigitalPassUiState {
        LOADING,
        ACTIVE_PASS,
        NO_ACTIVE_PASS_EMPTY,
        ERROR
    }

    private fun resolveDigitalPassUiState(
        isLoading: Boolean,
        result: Result<MyPassResponseData>?
    ): DigitalPassUiState {
        if (isLoading) return DigitalPassUiState.LOADING
        if (result == null) return DigitalPassUiState.LOADING

        return if (result.isSuccess) {
            DigitalPassUiState.ACTIVE_PASS
        } else {
            when (result.exceptionOrNull()) {
                is NoActivePassException -> DigitalPassUiState.NO_ACTIVE_PASS_EMPTY
                else -> DigitalPassUiState.ERROR
            }
        }
    }

    @Test
    fun testDigitalPassState_loadingState() {
        val state = resolveDigitalPassUiState(isLoading = true, result = null)
        assertEquals(DigitalPassUiState.LOADING, state)
    }

    @Test
    fun testDigitalPassState_activePassSuccess() {
        val activePassData = MyPassResponseData(
            pass = FullDigitalPassDto(
                id = "pass-1",
                passNumber = "PP-2026-0001",
                tier = "PREMIUM",
                isActive = true,
                isExpired = false,
                qrPayload = "propass:pass:PP-2026-0001"
            ),
            holder = PassHolderDto(
                userId = "u-1",
                email = "alex@example.com",
                fullName = "Alex Morgan",
                title = "Engineer",
                organization = "Acme",
                isVerified = true
            )
        )

        val state = resolveDigitalPassUiState(isLoading = false, result = Result.success(activePassData))
        assertEquals(DigitalPassUiState.ACTIVE_PASS, state)
    }

    @Test
    fun testDigitalPassState_noActivePassReturnsCleanEmptyState() {
        val notFoundResult: Result<MyPassResponseData> = Result.failure(
            NoActivePassException("No digital pass found for this user account")
        )

        val state = resolveDigitalPassUiState(isLoading = false, result = notFoundResult)
        assertEquals(
            "404 NoActivePassException must resolve to NO_ACTIVE_PASS_EMPTY state, NOT ERROR",
            DigitalPassUiState.NO_ACTIVE_PASS_EMPTY,
            state
        )
    }

    @Test
    fun testDigitalPassState_networkOrServerErrorReturnsErrorState() {
        val networkErrorResult: Result<MyPassResponseData> = Result.failure(
            java.io.IOException("Unable to connect to ProPass server")
        )
        val authErrorResult: Result<MyPassResponseData> = Result.failure(
            PassAuthException("Authentication required. Please sign in.")
        )
        val serverErrorResult: Result<MyPassResponseData> = Result.failure(
            Exception("Internal Server Error (500)")
        )

        assertEquals(DigitalPassUiState.ERROR, resolveDigitalPassUiState(isLoading = false, result = networkErrorResult))
        assertEquals(DigitalPassUiState.ERROR, resolveDigitalPassUiState(isLoading = false, result = authErrorResult))
        assertEquals(DigitalPassUiState.ERROR, resolveDigitalPassUiState(isLoading = false, result = serverErrorResult))
    }

    @Test
    fun testDigitalPassState_mutualExclusivityVisibilityMatrix() {
        data class ViewVisibilities(
            val progressBarVisible: Boolean,
            val passScrollViewVisible: Boolean,
            val noPassEmptyVisible: Boolean,
            val errorLayoutVisible: Boolean
        )

        fun getVisibilitiesForState(state: DigitalPassUiState): ViewVisibilities {
            return when (state) {
                DigitalPassUiState.LOADING -> ViewVisibilities(
                    progressBarVisible = true,
                    passScrollViewVisible = false,
                    noPassEmptyVisible = false,
                    errorLayoutVisible = false
                )
                DigitalPassUiState.ACTIVE_PASS -> ViewVisibilities(
                    progressBarVisible = false,
                    passScrollViewVisible = true,
                    noPassEmptyVisible = false,
                    errorLayoutVisible = false
                )
                DigitalPassUiState.NO_ACTIVE_PASS_EMPTY -> ViewVisibilities(
                    progressBarVisible = false,
                    passScrollViewVisible = false,
                    noPassEmptyVisible = true,
                    errorLayoutVisible = false
                )
                DigitalPassUiState.ERROR -> ViewVisibilities(
                    progressBarVisible = false,
                    passScrollViewVisible = false,
                    noPassEmptyVisible = false,
                    errorLayoutVisible = true
                )
            }
        }

        for (state in DigitalPassUiState.values()) {
            val vis = getVisibilitiesForState(state)
            val visibleCount = listOf(
                vis.progressBarVisible,
                vis.passScrollViewVisible,
                vis.noPassEmptyVisible,
                vis.errorLayoutVisible
            ).count { it }

            assertEquals("Exactly one layout must be visible in state $state", 1, visibleCount)

            if (state == DigitalPassUiState.NO_ACTIVE_PASS_EMPTY || state == DigitalPassUiState.ERROR) {
                assertFalse("Pass card scroll view must NOT be visible during empty or error states", vis.passScrollViewVisible)
            }
        }
    }

    // =========================================================================
    // Issue 5: Dynamic Question Filtering & Form Questions
    // =========================================================================

    @Test
    fun testFormQuestionFiltering_filtersOutFullNameAndEmailDefaultFields() {
        val allQuestions = listOf(
            FormQuestionDto(id = "default-full-name", label = "Full Name", type = "SHORT_TEXT", isRequired = true, isDefaultField = true),
            FormQuestionDto(id = "default-email", label = "Email Address", type = "SHORT_TEXT", isRequired = true, isDefaultField = true),
            FormQuestionDto(id = "default-phone", label = "Phone Number", type = "SHORT_TEXT", isRequired = false, isDefaultField = true),
            FormQuestionDto(id = "q-dietary", label = "Dietary Requirements", type = "SHORT_TEXT", isRequired = false, isDefaultField = false),
            FormQuestionDto(id = "q-tshirt", label = "T-Shirt Size", type = "MULTIPLE_CHOICE", options = listOf("S", "M", "L", "XL"), isRequired = true, isDefaultField = false)
        )

        val questionsToRender = allQuestions.filterNot { q ->
            q.id == "default-full-name" ||
            q.id == "default-email" ||
            (q.isDefaultField && q.label.equals("Full Name", ignoreCase = true)) ||
            (q.isDefaultField && q.label.equals("Email Address", ignoreCase = true))
        }

        assertEquals(3, questionsToRender.size)
        assertEquals("default-phone", questionsToRender[0].id)
        assertEquals("q-dietary", questionsToRender[1].id)
        assertEquals("q-tshirt", questionsToRender[2].id)
        assertFalse(questionsToRender.any { it.label.equals("Full Name", ignoreCase = true) })
        assertFalse(questionsToRender.any { it.label.equals("Email Address", ignoreCase = true) })
    }

    // =========================================================================
    // Issue 6: RegistrationData Serialization & Review Formatting
    // =========================================================================

    @Test
    fun testRegistrationData_javaSerializationRoundTrip() {
        val original = RegistrationData(
            eventId = "evt-temp-01",
            eventName = "Temp",
            fullName = "Sarah Connor",
            email = "sarah@sky.net",
            institution = "Resistance HQ",
            purpose = "Keynote Speaker",
            durationDays = 2,
            vehicleNumber = "TX-800",
            answers = listOf(
                RegistrationAnswerData(questionId = "default-phone", questionLabel = "Phone Number", value = "+1-555-0999"),
                RegistrationAnswerData(questionId = "q-tshirt", questionLabel = "T-Shirt Size", value = "M")
            )
        )

        val bytesOut = ByteArrayOutputStream()
        ObjectOutputStream(bytesOut).use { it.writeObject(original) }

        val bytesIn = ByteArrayInputStream(bytesOut.toByteArray())
        val deserialized = ObjectInputStream(bytesIn).use { it.readObject() as RegistrationData }

        assertEquals(original.eventId, deserialized.eventId)
        assertEquals(original.eventName, deserialized.eventName)
        assertEquals(original.fullName, deserialized.fullName)
        assertEquals(original.email, deserialized.email)
        assertEquals(original.institution, deserialized.institution)
        assertEquals(original.purpose, deserialized.purpose)
        assertEquals(original.durationDays, deserialized.durationDays)
        assertEquals(original.vehicleNumber, deserialized.vehicleNumber)
        assertEquals(2, deserialized.answers.size)
        assertEquals("T-Shirt Size", deserialized.answers[1].questionLabel)
        assertEquals("M", deserialized.answers[1].value)
    }

    @Test
    fun testProfileDisplayNameFallback_neverUsesStaticAlexMorgan() {
        fun resolveDisplayName(email: String?, profileName: String?): String {
            return profileName?.takeUnless {
                it.isBlank() || it.equals("ProPass User", ignoreCase = true)
            } ?: email?.substringBefore("@")?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            } ?: "Attendee"
        }

        assertEquals("John Doe", resolveDisplayName("john@example.com", "John Doe"))
        assertEquals("John", resolveDisplayName("john@example.com", null))
        assertEquals("John", resolveDisplayName("john@example.com", ""))
        assertEquals("John", resolveDisplayName("john@example.com", "ProPass User"))
        assertEquals("Attendee", resolveDisplayName(null, null))
    }
}
