package com.mpc.propass.organizer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpc.propass.R
import com.mpc.propass.organizer.model.FormQuestion

class FormQuestionAdapter(
    private val onEdit: (FormQuestion, Int) -> Unit,
    private val onDelete: (FormQuestion, Int) -> Unit
) : RecyclerView.Adapter<FormQuestionAdapter.QuestionViewHolder>() {

    private val questions = mutableListOf<FormQuestion>()

    fun submitList(newQuestions: List<FormQuestion>) {
        questions.clear()
        questions.addAll(newQuestions)
        notifyDataSetChanged()
    }

    fun getQuestions(): List<FormQuestion> = questions.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_form_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position, onEdit, onDelete)
    }

    override fun getItemCount(): Int = questions.size

    class QuestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvQuestionTypeBadge: TextView = itemView.findViewById(R.id.tvQuestionTypeBadge)
        private val tvRequiredBadge: TextView = itemView.findViewById(R.id.tvRequiredBadge)
        private val btnEditQuestion: ImageButton = itemView.findViewById(R.id.btnEditQuestion)
        private val btnDeleteQuestion: ImageButton = itemView.findViewById(R.id.btnDeleteQuestion)
        private val tvQuestionLabel: TextView = itemView.findViewById(R.id.tvQuestionLabel)
        private val tvOptionsSummary: TextView = itemView.findViewById(R.id.tvOptionsSummary)

        fun bind(
            question: FormQuestion,
            position: Int,
            onEdit: (FormQuestion, Int) -> Unit,
            onDelete: (FormQuestion, Int) -> Unit
        ) {
            tvQuestionLabel.text = question.label
            tvQuestionTypeBadge.text = question.type.displayName

            if (question.isRequired) {
                tvRequiredBadge.visibility = View.VISIBLE
                tvRequiredBadge.text = "* Required"
            } else {
                tvRequiredBadge.visibility = View.GONE
            }

            if (question.options.isNotEmpty()) {
                tvOptionsSummary.visibility = View.VISIBLE
                tvOptionsSummary.text = "Options: ${question.options.joinToString(", ")}"
            } else {
                tvOptionsSummary.visibility = View.GONE
            }

            btnEditQuestion.setOnClickListener {
                onEdit(question, position)
            }

            btnDeleteQuestion.setOnClickListener {
                onDelete(question, position)
            }
        }
    }
}
