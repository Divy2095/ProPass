package com.mpc.propass.organizer

import com.mpc.propass.network.NetworkClient
import com.mpc.propass.network.model.FormQuestionDto
import com.mpc.propass.organizer.data.OrganizerFormRepository
import com.mpc.propass.organizer.data.OrganizerFormRepositoryImpl
import com.mpc.propass.organizer.model.FormQuestion
import com.mpc.propass.organizer.model.FormQuestionType
import com.mpc.propass.organizer.model.toDto
import com.mpc.propass.organizer.model.toFormQuestion
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrganizerFormRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OrganizerFormRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val apiService = NetworkClient.createApiService(
            baseUrl = server.url("/").toString(),
            enableLogging = false
        )
        repository = OrganizerFormRepositoryImpl(apiService = apiService)
    }

    @After
    fun tearDown() {
        server.shutdown()
        NetworkClient.reset()
    }

    @Test
    fun getForm_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "data": {
                    "form": {
                        "id": "form-101",
                        "eventId": "evt-123",
                        "questions": [
                            {
                                "id": "q1",
                                "label": "Full Name",
                                "type": "SHORT_TEXT",
                                "isRequired": true,
                                "options": [],
                                "isDefaultField": true,
                                "orderIndex": 0
                            },
                            {
                                "id": "q2",
                                "label": "T-Shirt Size",
                                "type": "MULTIPLE_CHOICE",
                                "isRequired": true,
                                "options": ["S", "M", "L", "XL"],
                                "isDefaultField": false,
                                "orderIndex": 1
                            }
                        ]
                    }
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val result = repository.getForm("evt-123")

        assertTrue(result.isSuccess)
        val form = result.getOrThrow()
        assertEquals("form-101", form.id)
        assertEquals("evt-123", form.eventId)
        assertEquals(2, form.questions.size)

        val q2 = form.questions[1]
        assertEquals("T-Shirt Size", q2.label)
        assertEquals("MULTIPLE_CHOICE", q2.type)
        assertTrue(q2.isRequired)
        assertEquals(listOf("S", "M", "L", "XL"), q2.options)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v1/events/evt-123/form", recorded.path)
    }

    @Test
    fun getForm_notFound_404() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Event not found",
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(404).setBody(json))

        val result = repository.getForm("non-existent-event")

        assertFalse(result.isSuccess)
        assertEquals("Event not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun getForm_forbidden_403() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "You do not have permission to manage this event's form",
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(403).setBody(json))

        val result = repository.getForm("other-user-event")

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("permission") == true)
    }

    @Test
    fun saveForm_success_200() = runBlocking {
        val json = """
            {
                "success": true,
                "message": "Registration form saved successfully",
                "data": {
                    "form": {
                        "id": "form-101",
                        "eventId": "evt-123",
                        "questions": [
                            {
                                "id": "q1",
                                "label": "Full Name",
                                "type": "SHORT_TEXT",
                                "isRequired": true,
                                "options": [],
                                "isDefaultField": true,
                                "orderIndex": 0
                            },
                            {
                                "id": "q2",
                                "label": "Dietary Preference",
                                "type": "CHECKBOX",
                                "isRequired": false,
                                "options": ["Vegetarian", "Vegan", "Gluten-Free"],
                                "isDefaultField": false,
                                "orderIndex": 1
                            }
                        ]
                    },
                    "questions": [
                        {
                            "id": "q1",
                            "label": "Full Name",
                            "type": "SHORT_TEXT",
                            "isRequired": true,
                            "options": [],
                            "isDefaultField": true,
                            "orderIndex": 0
                        },
                        {
                            "id": "q2",
                            "label": "Dietary Preference",
                            "type": "CHECKBOX",
                            "isRequired": false,
                            "options": ["Vegetarian", "Vegan", "Gluten-Free"],
                            "isDefaultField": false,
                            "orderIndex": 1
                        }
                    ]
                },
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(200).setBody(json))

        val questions = listOf(
            FormQuestion(
                id = "q1",
                label = "Full Name",
                type = FormQuestionType.SHORT_TEXT,
                isRequired = true,
                isDefaultField = true
            ).toDto(0),
            FormQuestion(
                id = "q2",
                label = "Dietary Preference",
                type = FormQuestionType.CHECKBOX,
                isRequired = false,
                options = listOf("Vegetarian", "Vegan", "Gluten-Free"),
                isDefaultField = false
            ).toDto(1)
        )

        val result = repository.saveForm("evt-123", questions)

        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertEquals(2, saved.questions.size)
        assertEquals("Dietary Preference", saved.questions[1].label)
        assertEquals("CHECKBOX", saved.questions[1].type)

        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/api/v1/events/evt-123/form", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("Dietary Preference"))
        assertTrue(body.contains("Vegetarian"))
    }

    @Test
    fun saveForm_validationError_400() = runBlocking {
        val json = """
            {
                "success": false,
                "error": "Choice-based questions must have at least 2 options",
                "timestamp": "2026-09-07T12:00:00Z"
            }
        """.trimIndent()

        server.enqueue(MockResponse().setResponseCode(400).setBody(json))

        val result = repository.saveForm("evt-123", emptyList())

        assertFalse(result.isSuccess)
        assertEquals("Choice-based questions must have at least 2 options", result.exceptionOrNull()?.message)
    }

    @Test
    fun modelMappingExtensions() {
        val domainQuestion = FormQuestion(
            id = "test-q1",
            label = "Years of Experience",
            type = FormQuestionType.SHORT_TEXT,
            isRequired = true,
            options = emptyList(),
            isDefaultField = false
        )

        val dto = domainQuestion.toDto(orderIndex = 3)
        assertEquals("test-q1", dto.id)
        assertEquals("Years of Experience", dto.label)
        assertEquals("SHORT_TEXT", dto.type)
        assertTrue(dto.isRequired)
        assertEquals(3, dto.orderIndex)

        val backToDomain = dto.toFormQuestion()
        assertEquals(domainQuestion.id, backToDomain.id)
        assertEquals(domainQuestion.label, backToDomain.label)
        assertEquals(domainQuestion.type, backToDomain.type)
        assertEquals(domainQuestion.isRequired, backToDomain.isRequired)
    }
}
