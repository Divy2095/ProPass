package com.mpc.propass.organizer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpc.propass.R
import com.mpc.propass.organizer.model.OrganizerEvent

class OrganizerEventAdapter(
    private val onEventClick: (OrganizerEvent) -> Unit
) : RecyclerView.Adapter<OrganizerEventAdapter.EventViewHolder>() {

    private val items = mutableListOf<OrganizerEvent>()

    fun submitList(newItems: List<OrganizerEvent>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_organizer_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(items[position], onEventClick)
    }

    override fun getItemCount(): Int = items.size

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardEventItem: View = itemView.findViewById(R.id.cardEventItem)
        private val tvEventStatus: TextView = itemView.findViewById(R.id.tvEventStatus)
        private val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        private val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        private val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
        private val tvQuestionCount: TextView = itemView.findViewById(R.id.tvQuestionCount)

        fun bind(event: OrganizerEvent, onEventClick: (OrganizerEvent) -> Unit) {
            tvEventName.text = event.name
            tvEventDate.text = if (event.startTime.isNotBlank() && event.endTime.isNotBlank()) {
                "${event.date} • ${event.startTime} - ${event.endTime}"
            } else {
                event.date
            }
            tvEventLocation.text = event.location
            tvEventStatus.text = event.status

            val qCount = event.questions.size
            val regCount = event.attendeeCount
            tvQuestionCount.text = "$regCount registered • $qCount form fields"

            cardEventItem.setOnClickListener {
                onEventClick(event)
            }
        }
    }
}
