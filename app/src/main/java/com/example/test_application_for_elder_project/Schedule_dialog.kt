package com.example.test_application_for_elder_project

import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.test_application_for_elder_project.databinding.ScheduleDialogBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

open class Schedule_dialog : DialogFragment() {

    private lateinit var binding: ScheduleDialogBinding
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var userid: String

    private var calendar: Calendar = Calendar.getInstance()
    private var defaultHour: Int = calendar.get(Calendar.HOUR_OF_DAY)
    private var defaultMinute: Int = calendar.get(Calendar.MINUTE)

    private var start_time: String = "00:00"
    private var end_time: String = "00:00"
    private var selected_day: String = "monday" // Default value

    companion object {
        fun newInstance(userid: String): Schedule_dialog {
            val fragment = Schedule_dialog()
            val args = Bundle()
            args.putString("userid", userid)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userid = arguments?.getString("userid") ?: ""
        firebaseFirestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = ScheduleDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Spinner
        val spinner = binding.daySpinner
        val daysArray = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val adapterSpinner = ArrayAdapter(requireContext(), androidx.appcompat.R.layout.support_simple_spinner_dropdown_item, daysArray)
        spinner.adapter = adapterSpinner

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selected_day = parent?.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Time Pickers
        binding.button3.setOnClickListener {
            showTimePicker { selectedTime -> start_time = selectedTime }
        }

        binding.button4.setOnClickListener {
            showTimePicker { selectedTime -> end_time = selectedTime }
        }

        // Upload Schedule
        binding.finalizeSchedule.setOnClickListener {
            scheduleUpload()
        }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val timePicker = TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val formattedTime = selectedTime.format(formatter)
            onTimeSelected(formattedTime)
        }, defaultHour, defaultMinute, true)

        timePicker.show()
    }

    private fun scheduleUpload() {
        if (UserManager.current_userId.toString().isEmpty()) return  // Prevent null userid issue

        val scheduleHashMap = hashMapOf(
            "selected_day" to selected_day,
            "start_time" to start_time,
            "end_time" to end_time
        )

        val userRef = firebaseFirestore.collection("users")
            .document(userid)
            .collection("schedule")
            .document("preferences")

        userRef.set(scheduleHashMap).addOnSuccessListener {
            var finalize_schedule = The_main_workout_match_activity()
            CoroutineScope(Dispatchers.IO).launch {
                finalize_schedule.call_finalize_schedule()
            }
            dismiss()  // Close dialog on success
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            setCanceledOnTouchOutside(false)  // Prevent dismiss on outside touch
        }
    }
}
