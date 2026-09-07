package com.mpc.propass.organizer

import com.mpc.propass.organizer.data.OrganizerEventStore
import com.mpc.propass.organizer.model.FormQuestion
import com.mpc.propass.organizer.model.FormQuestionType
import com.mpc.propass.organizer.model.OrganizerEventDraft
import com.mpc.propass.util.ProPassQrParser
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrganizerEventTest {

    @Before
    fun setUp() {
        OrganizerEventStore.clear()
    }

    @After
    fun tearDown() {
        OrganizerEventStore.clear()
    }

    @Test
    fun testDefaultDraftInitialization() {
        val draft = OrganizerEventDraft.createDefaultDraft()

        assertTrue(draft.id.isNotBlank())
        assertEquals("", draft.name)
        assertEquals(1, draft.maxDurationDays)
        assertEquals(3, draft.questions.size)

        val fullName = draft.questions.firstOrNull { it.id == "default-full-name" }
        assertNotNull(fullName)
        assertEquals("Full Name", fullName?.label)
        assertTrue(fullName?.isRequired == true)
        assertTrue(fullName?.isDefaultField == true)

        val email = draft.questions.firstOrNull { it.id == "default-email" }
        assertNotNull(email)
        assertEquals("Email Address", email?.label)
        assertTrue(email?.isRequired == true)
        assertTrue(email?.isDefaultField == true)

        val phone = draft.questions.firstOrNull { it.id == "default-phone" }
        assertNotNull(phone)
        assertEquals("Phone Number", phone?.label)
        assertFalse(phone?.isRequired == true)
        assertTrue(phone?.isDefaultField == true)
    }

    @Test
    fun testCustomQuestionAdditionAndRemoval() {
        val draft = OrganizerEventDraft.createDefaultDraft()

        val customQ1 = FormQuestion(
            label = "Dietary Preferences",
            type = FormQuestionType.MULTIPLE_CHOICE,
            options = listOf("Vegetarian", "Vegan", "Halal", "Standard"),
            isRequired = true
        )
        draft.questions.add(customQ1)

        val customQ2 = FormQuestion(
            label = "GitHub Profile URL",
            type = FormQuestionType.SHORT_TEXT,
            isRequired = false
        )
        draft.questions.add(customQ2)

        assertEquals(5, draft.questions.size)
        assertEquals(2, draft.questions.count { !it.isDefaultField })

        // Remove Q1
        draft.questions.removeAll { it.id == customQ1.id }
        assertEquals(4, draft.questions.size)
        assertNull(draft.questions.firstOrNull { it.id == customQ1.id })
    }

    @Test
    fun testPublishedEventConversionAndQrPayload() {
        val draft = OrganizerEventDraft.createDefaultDraft().apply {
            name = "Google I/O Extended 2026!"
            description = "Annual developer conference"
            date = "2026-06-15"
            startTime = "09:00 AM"
            endTime = "05:00 PM"
            location = "San Francisco, CA"
            maxDurationDays = 2
        }

        val event = draft.toPublishedEvent()

        assertEquals(draft.id, event.id)
        assertEquals("Google I/O Extended 2026!", event.name)
        assertEquals("Annual developer conference", event.description)
        assertEquals("2026-06-15", event.date)
        assertEquals("09:00 AM", event.startTime)
        assertEquals("05:00 PM", event.endTime)
        assertEquals("San Francisco, CA", event.location)
        assertEquals(2, event.maxDurationDays)
        assertEquals("PUBLISHED", event.status)

        // Slug should be normalized
        assertEquals("https://propass.id/event/google-i-o-extended-2026", event.qrPayload)

        // QR Parser compatibility check
        assertTrue(ProPassQrParser.isProPassQr(event.qrPayload))
        assertEquals("google-i-o-extended-2026", ProPassQrParser.parseEventId(event.qrPayload))
    }

    @Test
    fun testOrganizerEventStoreCrud() {
        assertTrue(OrganizerEventStore.getEvents().isEmpty())

        val draft1 = OrganizerEventDraft.createDefaultDraft().apply {
            name = "Hackathon 2026"
            date = "2026-10-01"
            location = "Tech Hub"
        }
        val event1 = draft1.toPublishedEvent()
        OrganizerEventStore.saveEvent(event1)

        val draft2 = OrganizerEventDraft.createDefaultDraft().apply {
            name = "AI Summit 2026"
            date = "2026-11-15"
            location = "Convention Center"
        }
        val event2 = draft2.toPublishedEvent()
        OrganizerEventStore.saveEvent(event2)

        val allEvents = OrganizerEventStore.getEvents()
        assertEquals(2, allEvents.size)
        // Newest event first
        assertEquals(event2.id, allEvents[0].id)
        assertEquals(event1.id, allEvents[1].id)

        // Find by ID
        val found = OrganizerEventStore.getEventById(event1.id)
        assertNotNull(found)
        assertEquals("Hackathon 2026", found?.name)

        // Update existing event
        val updatedEvent1 = event1.copy(name = "Hackathon 2026 - Updated", attendeeCount = 42)
        OrganizerEventStore.saveEvent(updatedEvent1)

        assertEquals(2, OrganizerEventStore.getEvents().size)
        assertEquals("Hackathon 2026 - Updated", OrganizerEventStore.getEventById(event1.id)?.name)
        assertEquals(42, OrganizerEventStore.getEventById(event1.id)?.attendeeCount)

        // Delete event
        val deleted = OrganizerEventStore.deleteEvent(event1.id)
        assertTrue(deleted)
        assertEquals(1, OrganizerEventStore.getEvents().size)
        assertNull(OrganizerEventStore.getEventById(event1.id))
    }
}
