package com.mpc.propass.organizer.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.mpc.propass.R
import com.mpc.propass.organizer.model.OrganizerEventDraft
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Screen 1 of Organizer Flow:
 * Collects core event details, timing, location, and maximum pass duration.
 */
class CreateEventActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tilEventName: TextInputLayout
    private lateinit var etEventName: TextInputEditText
    private lateinit var tilEventDescription: TextInputLayout
    private lateinit var etEventDescription: TextInputEditText
    private lateinit var tilEventDate: TextInputLayout
    private lateinit var etEventDate: TextInputEditText
    private lateinit var tilStartTime: TextInputLayout
    private lateinit var etStartTime: TextInputEditText
    private lateinit var tilEndTime: TextInputLayout
    private lateinit var etEndTime: TextInputEditText
    private lateinit var tilEventLocation: TextInputLayout
    private lateinit var etEventLocation: TextInputEditText
    private lateinit var tilMaxDuration: TextInputLayout
    private lateinit var etMaxDuration: TextInputEditText
    private lateinit var btnContinueToBuilder: MaterialButton

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
        setContentView(R.layout.activity_create_event)

        initViews()
        setupListeners()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true
        insetsController.isAppearanceLightNavigationBars = true
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tilEventName = findViewById(R.id.tilEventName)
        etEventName = findViewById(R.id.etEventName)
        tilEventDescription = findViewById(R.id.tilEventDescription)
        etEventDescription = findViewById(R.id.etEventDescription)
        tilEventDate = findViewById(R.id.tilEventDate)
        etEventDate = findViewById(R.id.etEventDate)
        tilStartTime = findViewById(R.id.tilStartTime)
        etStartTime = findViewById(R.id.etStartTime)
        tilEndTime = findViewById(R.id.tilEndTime)
        etEndTime = findViewById(R.id.etEndTime)
        tilEventLocation = findViewById(R.id.tilEventLocation)
        etEventLocation = findViewById(R.id.etEventLocation)
        tilMaxDuration = findViewById(R.id.tilMaxDuration)
        etMaxDuration = findViewById(R.id.etMaxDuration)
        btnContinueToBuilder = findViewById(R.id.btnContinueToBuilder)

        val root = findViewById<View>(R.id.createEventRoot)
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

        // Date Picker
        etEventDate.setOnClickListener {
            showDatePicker()
        }
        tilEventDate.setEndIconOnClickListener {
            showDatePicker()
        }

        // Start Time Picker
        etStartTime.setOnClickListener {
            showTimePicker { formattedTime -> etStartTime.setText(formattedTime) }
        }
        tilStartTime.setEndIconOnClickListener {
            showTimePicker { formattedTime -> etStartTime.setText(formattedTime) }
        }

        // End Time Picker
        etEndTime.setOnClickListener {
            showTimePicker { formattedTime -> etEndTime.setText(formattedTime) }
        }
        tilEndTime.setEndIconOnClickListener {
            showTimePicker { formattedTime -> etEndTime.setText(formattedTime) }
        }

        btnContinueToBuilder.setOnClickListener {
            validateAndProceed()
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                etEventDate.setText(sdf.format(calendar.time))
                tilEventDate.error = null
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val sdf = SimpleDateFormat("hh:mm a", Locale.US)
                onTimeSelected(sdf.format(calendar.time))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }

    private fun validateAndProceed() {
        val name = etEventName.text?.toString()?.trim().orEmpty()
        val desc = etEventDescription.text?.toString()?.trim().orEmpty()
        val date = etEventDate.text?.toString()?.trim().orEmpty()
        val startTime = etStartTime.text?.toString()?.trim().orEmpty()
        val endTime = etEndTime.text?.toString()?.trim().orEmpty()
        val location = etEventLocation.text?.toString()?.trim().orEmpty()
        val durationStr = etMaxDuration.text?.toString()?.trim().orEmpty()

        var isValid = true

        if (name.isBlank()) {
            tilEventName.error = getString(R.string.error_event_name_required)
            isValid = false
        } else {
            tilEventName.error = null
        }

        if (date.isBlank()) {
            tilEventDate.error = getString(R.string.error_event_date_required)
            isValid = false
        } else {
            tilEventDate.error = null
        }

        if (location.isBlank()) {
            tilEventLocation.error = getString(R.string.error_event_location_required)
            isValid = false
        } else {
            tilEventLocation.error = null
        }

        val duration = durationStr.toIntOrNull() ?: 0
        if (duration < 1) {
            tilMaxDuration.error = getString(R.string.error_event_duration_invalid)
            isValid = false
        } else {
            tilMaxDuration.error = null
        }

        if (!isValid) return

        val draft = OrganizerEventDraft(
            name = name,
            description = desc,
            date = date,
            startTime = startTime,
            endTime = endTime,
            location = location,
            maxDurationDays = duration
        )

        val intent = Intent(this, FormBuilderActivity::class.java).apply {
            putExtra(FormBuilderActivity.EXTRA_DRAFT, draft)
        }
        startActivity(intent)
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
