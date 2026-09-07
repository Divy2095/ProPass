package com.mpc.propass.organizer.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerEventStore
import com.mpc.propass.organizer.model.FormQuestion
import com.mpc.propass.organizer.model.FormQuestionType
import com.mpc.propass.organizer.model.OrganizerEventDraft

/**
 * Screen 3 of Organizer Flow:
 * Renders an exact preview of the registration form as attendees will experience it.
 * Provides the final "Publish Event" action to persist the local mock event.
 */
class FormPreviewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DRAFT = "extra_organizer_event_draft"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvPreviewEventTitle: TextView
    private lateinit var tvPreviewEventSubtitle: TextView
    private lateinit var tvPreviewEventMetadata: TextView
    private lateinit var layoutPreviewPhone: View
    private lateinit var tvPreviewPhoneLabel: TextView
    private lateinit var tvCustomQuestionsHeader: TextView
    private lateinit var containerCustomQuestions: LinearLayout
    private lateinit var btnBackToBuilder: MaterialButton
    private lateinit var btnPublishEvent: MaterialButton

    private lateinit var draft: OrganizerEventDraft

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_form_preview)

        loadDraftFromIntent()
        initViews()
        bindEventData()
        renderCustomQuestions()
        setupListeners()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun loadDraftFromIntent() {
        val passedDraft = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_DRAFT, OrganizerEventDraft::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_DRAFT) as? OrganizerEventDraft
        }
        draft = passedDraft ?: OrganizerEventDraft()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvPreviewEventTitle = findViewById(R.id.tvPreviewEventTitle)
        tvPreviewEventSubtitle = findViewById(R.id.tvPreviewEventSubtitle)
        tvPreviewEventMetadata = findViewById(R.id.tvPreviewEventMetadata)
        layoutPreviewPhone = findViewById(R.id.layoutPreviewPhone)
        tvPreviewPhoneLabel = findViewById(R.id.tvPreviewPhoneLabel)
        tvCustomQuestionsHeader = findViewById(R.id.tvCustomQuestionsHeader)
        containerCustomQuestions = findViewById(R.id.containerCustomQuestions)
        btnBackToBuilder = findViewById(R.id.btnBackToBuilder)
        btnPublishEvent = findViewById(R.id.btnPublishEvent)

        val root = findViewById<View>(R.id.formPreviewRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            findViewById<View>(R.id.topBar).setPadding(
                16.dpToPx(),
                statusBar.top + 12.dpToPx(),
                16.dpToPx(),
                12.dpToPx()
            )
            findViewById<View>(R.id.bottomActionContainer).setPadding(
                20.dpToPx(),
                12.dpToPx(),
                20.dpToPx(),
                navBar.bottom + 16.dpToPx()
            )
            insets
        }
    }

    private fun bindEventData() {
        tvPreviewEventTitle.text = draft.name.ifBlank { "Untitled Event" }
        tvPreviewEventSubtitle.text = draft.description.ifBlank { "Complete your registration to secure your spot." }

        val metadata = buildString {
            if (draft.date.isNotBlank()) append(draft.date)
            if (draft.startTime.isNotBlank() && draft.endTime.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append("${draft.startTime} - ${draft.endTime}")
            }
            if (draft.location.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(draft.location)
            }
        }
        tvPreviewEventMetadata.text = metadata.ifBlank { "Location and timing details" }

        val phoneField = draft.questions.firstOrNull { it.id == "default-phone" }
        if (phoneField != null) {
            layoutPreviewPhone.visibility = View.VISIBLE
            tvPreviewPhoneLabel.text = if (phoneField.isRequired) "Phone Number *" else "Phone Number (Optional)"
        } else {
            layoutPreviewPhone.visibility = View.GONE
        }
    }

    private fun renderCustomQuestions() {
        containerCustomQuestions.removeAllViews()
        val customQuestions = draft.questions.filter { !it.isDefaultField }

        if (customQuestions.isEmpty()) {
            tvCustomQuestionsHeader.visibility = View.GONE
            return
        }

        tvCustomQuestionsHeader.visibility = View.VISIBLE

        for (question in customQuestions) {
            val questionView = createQuestionPreviewView(question)
            containerCustomQuestions.addView(questionView)
        }
    }

    private fun createQuestionPreviewView(question: FormQuestion): View {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            orientation = LinearLayout.VERTICAL
        }

        val labelText = if (question.isRequired) {
            "${question.label} *"
        } else {
            "${question.label} (Optional)"
        }

        when (question.type) {
            FormQuestionType.SHORT_TEXT -> {
                val til = TextInputLayout(this, null, com.google.android.material.R.attr.textInputStyle).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    hint = labelText
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
                    setBoxCornerRadii(12f.dpToPxFloat(), 12f.dpToPxFloat(), 12f.dpToPxFloat(), 12f.dpToPxFloat())
                    boxBackgroundColor = ContextCompat.getColor(this@FormPreviewActivity, R.color.surface_container)
                    boxStrokeWidth = 0
                }
                val et = TextInputEditText(til.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    isFocusable = false
                    isClickable = false
                    hint = "Attendee answer..."
                }
                til.addView(et)
                container.addView(til)
            }

            FormQuestionType.LONG_TEXT -> {
                val til = TextInputLayout(this, null, com.google.android.material.R.attr.textInputStyle).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    hint = labelText
                    boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_FILLED
                    setBoxCornerRadii(12f.dpToPxFloat(), 12f.dpToPxFloat(), 12f.dpToPxFloat(), 12f.dpToPxFloat())
                    boxBackgroundColor = ContextCompat.getColor(this@FormPreviewActivity, R.color.surface_container)
                    boxStrokeWidth = 0
                }
                val et = TextInputEditText(til.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    minLines = 3
                    isFocusable = false
                    isClickable = false
                    hint = "Attendee detailed paragraph..."
                }
                til.addView(et)
                container.addView(til)
            }

            FormQuestionType.MULTIPLE_CHOICE -> {
                val tvLabel = TextView(this).apply {
                    text = labelText
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(this@FormPreviewActivity, R.color.on_surface))
                    setPadding(0, 0, 0, 8.dpToPx())
                }
                container.addView(tvLabel)

                val rg = RadioGroup(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                for (option in question.options) {
                    val rb = RadioButton(this).apply {
                        text = option
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(this@FormPreviewActivity, R.color.on_surface_variant))
                        isEnabled = false
                    }
                    rg.addView(rb)
                }
                container.addView(rg)
            }

            FormQuestionType.CHECKBOX -> {
                val tvLabel = TextView(this).apply {
                    text = labelText
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(this@FormPreviewActivity, R.color.on_surface))
                    setPadding(0, 0, 0, 8.dpToPx())
                }
                container.addView(tvLabel)

                for (option in question.options) {
                    val cb = MaterialCheckBox(this).apply {
                        text = option
                        textSize = 14f
                        setTextColor(ContextCompat.getColor(this@FormPreviewActivity, R.color.on_surface_variant))
                        isEnabled = false
                    }
                    container.addView(cb)
                }
            }
        }

        return container
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnBackToBuilder.setOnClickListener {
            finish()
        }

        btnPublishEvent.setOnClickListener {
            publishEvent()
        }
    }

    private fun publishEvent() {
        val event = draft.toPublishedEvent()
        OrganizerEventStore.saveEvent(event)

        val intent = Intent(this, EventPublishSuccessActivity::class.java).apply {
            putExtra(EventPublishSuccessActivity.EXTRA_EVENT, event)
            putExtra(EventPublishSuccessActivity.EXTRA_IS_VIEW_MODE, false)
        }
        startActivity(intent)
        finish()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToPxFloat(): Float = this * resources.displayMetrics.density
}
