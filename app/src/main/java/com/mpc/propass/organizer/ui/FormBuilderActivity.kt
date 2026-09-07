package com.mpc.propass.organizer.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mpc.propass.ProPassApplication
import com.mpc.propass.R
import com.mpc.propass.organizer.data.OrganizerEventStore
import com.mpc.propass.organizer.data.OrganizerFormRepository
import com.mpc.propass.organizer.data.OrganizerFormRepositoryImpl
import com.mpc.propass.organizer.model.FormQuestion
import com.mpc.propass.organizer.model.FormQuestionType
import com.mpc.propass.organizer.model.OrganizerEventDraft
import com.mpc.propass.organizer.model.toDto
import com.mpc.propass.organizer.model.toFormQuestion
import com.mpc.propass.organizer.ui.adapter.FormQuestionAdapter
import kotlinx.coroutines.launch

/**
 * Screen 2 of Organizer Flow:
 * Form Builder enabling organizers to configure required/optional default fields
 * and add, edit, or delete custom attendee registration questions.
 */
class FormBuilderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DRAFT = "extra_organizer_event_draft"
    }

    private lateinit var btnBack: ImageButton
    private lateinit var tvSummaryEventName: TextView
    private lateinit var tvSummaryEventDetails: TextView
    private lateinit var cbPhoneRequired: MaterialCheckBox
    private lateinit var tvCustomQuestionsCount: TextView
    private lateinit var btnAddQuestion: MaterialButton
    private lateinit var tvNoCustomQuestions: TextView
    private lateinit var rvCustomQuestions: RecyclerView
    private lateinit var btnPreviewForm: MaterialButton

    private lateinit var draft: OrganizerEventDraft
    private lateinit var questionAdapter: FormQuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_form_builder)

        loadDraftFromIntent()
        initViews()
        setupListeners()
        updateCustomQuestionsUi()
        loadExistingFormIfAvailable()
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
        tvSummaryEventName = findViewById(R.id.tvSummaryEventName)
        tvSummaryEventDetails = findViewById(R.id.tvSummaryEventDetails)
        cbPhoneRequired = findViewById(R.id.cbPhoneRequired)
        tvCustomQuestionsCount = findViewById(R.id.tvCustomQuestionsCount)
        btnAddQuestion = findViewById(R.id.btnAddQuestion)
        tvNoCustomQuestions = findViewById(R.id.tvNoCustomQuestions)
        rvCustomQuestions = findViewById(R.id.rvCustomQuestions)
        btnPreviewForm = findViewById(R.id.btnPreviewForm)

        tvSummaryEventName.text = draft.name.ifBlank { "Untitled Event" }
        tvSummaryEventDetails.text = buildString {
            if (draft.date.isNotBlank()) append(draft.date)
            if (draft.location.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(draft.location)
            }
        }.ifBlank { "Event Details Pending" }

        val phoneQuestion = draft.questions.firstOrNull { it.id == "default-phone" }
        cbPhoneRequired.isChecked = phoneQuestion?.isRequired == true

        questionAdapter = FormQuestionAdapter(
            onEdit = { question, _ -> showQuestionDialog(question) },
            onDelete = { question, _ -> deleteQuestion(question) }
        )
        rvCustomQuestions.layoutManager = LinearLayoutManager(this)
        rvCustomQuestions.adapter = questionAdapter

        val root = findViewById<View>(R.id.formBuilderRoot)
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

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        cbPhoneRequired.setOnCheckedChangeListener { _, isChecked ->
            draft.questions.firstOrNull { it.id == "default-phone" }?.isRequired = isChecked
        }

        btnAddQuestion.setOnClickListener {
            showQuestionDialog(null)
        }

        btnPreviewForm.setOnClickListener {
            saveFormAndPreview()
        }
    }

    private fun loadExistingFormIfAvailable() {
        val eventId = draft.id
        if (eventId.isBlank()) return

        val formRepo: OrganizerFormRepository = (application as? ProPassApplication)?.organizerFormRepository
            ?: OrganizerFormRepositoryImpl()

        lifecycleScope.launch {
            val result = formRepo.getForm(eventId)
            result.onSuccess { formDto ->
                if (formDto.questions.isNotEmpty()) {
                    val questions = formDto.questions.map { it.toFormQuestion() }
                    draft.questions.clear()
                    draft.questions.addAll(questions)
                    val phoneQuestion = draft.questions.firstOrNull { it.id == "default-phone" || it.label.equals("Phone Number", ignoreCase = true) }
                    cbPhoneRequired.isChecked = phoneQuestion?.isRequired == true
                    updateCustomQuestionsUi()
                }
            }
        }
    }

    private fun saveFormAndPreview() {
        val eventId = draft.id
        if (eventId.isBlank()) {
            val intent = Intent(this, FormPreviewActivity::class.java).apply {
                putExtra(FormPreviewActivity.EXTRA_DRAFT, draft)
            }
            startActivity(intent)
            return
        }

        btnPreviewForm.isEnabled = false
        btnPreviewForm.text = "Saving Form..."

        val formRepo: OrganizerFormRepository = (application as? ProPassApplication)?.organizerFormRepository
            ?: OrganizerFormRepositoryImpl()

        draft.questions.firstOrNull { it.id == "default-phone" || it.label.equals("Phone Number", ignoreCase = true) }?.isRequired = cbPhoneRequired.isChecked

        val dtoList = draft.questions.mapIndexed { index, q -> q.toDto(index) }

        lifecycleScope.launch {
            val result = formRepo.saveForm(eventId, dtoList)
            btnPreviewForm.isEnabled = true
            btnPreviewForm.setText(R.string.btn_preview_form)

            result.onSuccess { savedForm ->
                if (savedForm.questions.isNotEmpty()) {
                    val savedQuestions = savedForm.questions.map { it.toFormQuestion() }
                    draft.questions.clear()
                    draft.questions.addAll(savedQuestions)
                }
                OrganizerEventStore.saveEvent(draft.toPublishedEvent())

                val intent = Intent(this@FormBuilderActivity, FormPreviewActivity::class.java).apply {
                    putExtra(FormPreviewActivity.EXTRA_DRAFT, draft)
                }
                startActivity(intent)
            }.onFailure { error ->
                Toast.makeText(
                    this@FormBuilderActivity,
                    error.message ?: "Failed to save form",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateCustomQuestionsUi() {
        val customQuestions = draft.questions.filter { !it.isDefaultField }
        questionAdapter.submitList(customQuestions)

        tvCustomQuestionsCount.text = "${customQuestions.size} custom field${if (customQuestions.size == 1) "" else "s"}"

        if (customQuestions.isEmpty()) {
            tvNoCustomQuestions.visibility = View.VISIBLE
            rvCustomQuestions.visibility = View.GONE
        } else {
            tvNoCustomQuestions.visibility = View.GONE
            rvCustomQuestions.visibility = View.VISIBLE
        }
    }

    private fun deleteQuestion(question: FormQuestion) {
        draft.questions.removeAll { it.id == question.id }
        updateCustomQuestionsUi()
    }

    private fun showQuestionDialog(existingQuestion: FormQuestion?) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_form_question, null)
        val tilLabel = dialogView.findViewById<TextInputLayout>(R.id.tilQuestionLabel)
        val etLabel = dialogView.findViewById<TextInputEditText>(R.id.etQuestionLabel)
        val rgType = dialogView.findViewById<RadioGroup>(R.id.rgQuestionType)
        val tilOptions = dialogView.findViewById<TextInputLayout>(R.id.tilQuestionOptions)
        val etOptions = dialogView.findViewById<TextInputEditText>(R.id.etQuestionOptions)
        val cbRequired = dialogView.findViewById<MaterialCheckBox>(R.id.cbIsRequired)

        // Pre-fill if editing
        existingQuestion?.let { q ->
            etLabel.setText(q.label)
            cbRequired.isChecked = q.isRequired
            when (q.type) {
                FormQuestionType.SHORT_TEXT -> rgType.check(R.id.rbShortText)
                FormQuestionType.LONG_TEXT -> rgType.check(R.id.rbLongText)
                FormQuestionType.MULTIPLE_CHOICE -> {
                    rgType.check(R.id.rbMultipleChoice)
                    tilOptions.visibility = View.VISIBLE
                    etOptions.setText(q.options.joinToString(", "))
                }
                FormQuestionType.CHECKBOX -> {
                    rgType.check(R.id.rbCheckbox)
                    tilOptions.visibility = View.VISIBLE
                    etOptions.setText(q.options.joinToString(", "))
                }
            }
        }

        // Toggle options input based on selected type
        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbMultipleChoice || checkedId == R.id.rbCheckbox) {
                tilOptions.visibility = View.VISIBLE
            } else {
                tilOptions.visibility = View.GONE
            }
        }

        val dialogTitle = if (existingQuestion != null) {
            getString(R.string.dialog_edit_question_title)
        } else {
            getString(R.string.dialog_add_question_title)
        }

        val dialog = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_ProPass_MaterialAlertDialog)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_save, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val label = etLabel.text?.toString()?.trim().orEmpty()
                if (label.isBlank()) {
                    tilLabel.error = "Question label cannot be blank"
                    return@setOnClickListener
                } else {
                    tilLabel.error = null
                }

                val type = when (rgType.checkedRadioButtonId) {
                    R.id.rbLongText -> FormQuestionType.LONG_TEXT
                    R.id.rbMultipleChoice -> FormQuestionType.MULTIPLE_CHOICE
                    R.id.rbCheckbox -> FormQuestionType.CHECKBOX
                    else -> FormQuestionType.SHORT_TEXT
                }

                val options = if (type == FormQuestionType.MULTIPLE_CHOICE || type == FormQuestionType.CHECKBOX) {
                    val raw = etOptions.text?.toString().orEmpty()
                    raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                if ((type == FormQuestionType.MULTIPLE_CHOICE || type == FormQuestionType.CHECKBOX) && options.isEmpty()) {
                    tilOptions.error = "Please provide at least 2 options separated by commas"
                    return@setOnClickListener
                } else {
                    tilOptions.error = null
                }

                val isRequired = cbRequired.isChecked

                if (existingQuestion != null) {
                    // Update existing
                    existingQuestion.label = label
                    existingQuestion.type = type
                    existingQuestion.isRequired = isRequired
                    existingQuestion.options = options
                } else {
                    // Create new
                    val newQuestion = FormQuestion(
                        label = label,
                        type = type,
                        isRequired = isRequired,
                        options = options,
                        isDefaultField = false
                    )
                    draft.questions.add(newQuestion)
                }

                dialog.dismiss()
                updateCustomQuestionsUi()
            }
        }

        dialog.show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
