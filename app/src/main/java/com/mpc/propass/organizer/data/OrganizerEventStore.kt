package com.mpc.propass.organizer.data

import com.mpc.propass.organizer.model.OrganizerEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory local store for organizer events during Phase 1.
 * Thread-safe and persistent for the application process lifetime.
 */
object OrganizerEventStore {

    private val events = CopyOnWriteArrayList<OrganizerEvent>()

    fun getEvents(): List<OrganizerEvent> = events.toList()

    fun saveEvent(event: OrganizerEvent) {
        // Replace existing by ID or prepend
        val index = events.indexOfFirst { it.id == event.id }
        if (index >= 0) {
            events[index] = event
        } else {
            events.add(0, event)
        }
    }

    fun deleteEvent(id: String): Boolean {
        return events.removeAll { it.id == id }
    }

    fun getEventById(id: String): OrganizerEvent? {
        return events.firstOrNull { it.id == id }
    }

    fun clear() {
        events.clear()
    }

    fun count(): Int = events.size
}
