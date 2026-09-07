package com.mpc.propass.organizer.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mpc.propass.R
import com.mpc.propass.network.model.RegistrationPurposeMapper
import com.mpc.propass.organizer.model.OrganizerRegistrationDto

/**
 * Adapter displaying the list of registrations for an event in the Organizer flow.
 */
class OrganizerRegistrationAdapter(
    private val onRegistrationClick: (OrganizerRegistrationDto) -> Unit
) : RecyclerView.Adapter<OrganizerRegistrationAdapter.RegistrationViewHolder>() {

    private val items = mutableListOf<OrganizerRegistrationDto>()

    fun submitList(newItems: List<OrganizerRegistrationDto>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_organizer_registration, parent, false)
        return RegistrationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
        holder.bind(items[position], onRegistrationClick)
    }

    override fun getItemCount(): Int = items.size

    class RegistrationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardRegistrationItem: View = itemView.findViewById(R.id.cardRegistrationItem)
        private val tvAttendeeName: TextView = itemView.findViewById(R.id.tvAttendeeName)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvAttendeeEmail: TextView = itemView.findViewById(R.id.tvAttendeeEmail)
        private val tvAttendeeInstitution: TextView = itemView.findViewById(R.id.tvAttendeeInstitution)
        private val tvRegistrationMeta: TextView = itemView.findViewById(R.id.tvRegistrationMeta)
        private val tvAnswersCount: TextView = itemView.findViewById(R.id.tvAnswersCount)

        fun bind(
            registration: OrganizerRegistrationDto,
            onRegistrationClick: (OrganizerRegistrationDto) -> Unit
        ) {
            val displayName = registration.attendee?.fullName?.takeIf { it.isNotBlank() }
                ?: registration.fullName.takeIf { it.isNotBlank() }
                ?: "Anonymous Attendee"
            tvAttendeeName.text = displayName

            val email = registration.attendee?.email?.takeIf { it.isNotBlank() }
                ?: registration.email.takeIf { it.isNotBlank() }
                ?: "No email provided"
            tvAttendeeEmail.text = email

            val institution = registration.attendee?.organization?.takeIf { it.isNotBlank() }
                ?: registration.attendee?.institution?.takeIf { it.isNotBlank() }
                ?: registration.institution.takeIf { it.isNotBlank() }
                ?: "No institution provided"
            tvAttendeeInstitution.text = institution

            // Status styling
            val status = registration.status.uppercase()
            tvStatusBadge.text = status
            when (status) {
                "CONFIRMED" -> {
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_confirmed_pill)
                    tvStatusBadge.setTextColor(Color.parseColor("#137333"))
                }
                "PENDING" -> {
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_status_pending_pill)
                    tvStatusBadge.setTextColor(Color.parseColor("#B06000"))
                }
                else -> {
                    tvStatusBadge.setBackgroundResource(R.drawable.bg_org_pill)
                    tvStatusBadge.setTextColor(ContextCompat.getColor(itemView.context, R.color.primary))
                }
            }

            val friendlyPurpose = RegistrationPurposeMapper.toFriendlyDisplay(registration.purpose)
            val durationText = "${registration.durationDays} Day(s)"
            tvRegistrationMeta.text = "$friendlyPurpose • $durationText"

            val answerCount = registration.answers.size
            tvAnswersCount.text = if (answerCount == 1) "1 answer" else "$answerCount answers"

            cardRegistrationItem.setOnClickListener {
                onRegistrationClick(registration)
            }
        }
    }
}
