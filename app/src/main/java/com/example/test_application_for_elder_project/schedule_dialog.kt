package com.example.test_application_for_elder_project

import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.elderprojectfinal.databinding.ScheduleDialogBinding
import com.example.test_application_for_elder_project.UserManager
import com.example.test_application_for_elder_project.databinding.ScheduleDialogBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class schedule_dialog(var userid:String,context: Context) :DialogFragment() {


    var binding_schedule_dialog:ScheduleDialogBinding = ScheduleDialogBinding.inflate(LayoutInflater.from(context))

    var firebaseFirestore:FirebaseFirestore = FirebaseFirestore.getInstance()

    var calendar:Calendar = Calendar.getInstance()

    public var defaulthour = calendar.get(Calendar.HOUR_OF_DAY)
    public var defaultminute = calendar.get(Calendar.MINUTE)


    private lateinit var start_time:String

    private lateinit var end_time:String

    private lateinit var selected_day:String


    init {

        val spinner = binding_schedule_dialog.dayspinner
        val array = listOf("monday","tuesday","wednesday","thursday","friday","saturday","sunday")

        var adapter_spinner = ArrayAdapter(context, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,array)

        spinner.adapter = adapter_spinner

        spinner.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selected_day= parent?.getItemAtPosition(position).toString()
            }

        }
        binding_schedule_dialog.finalizeSchedule.setOnClickListener {
            schedule_upload(start_time,end_time)
        }
        TODO()
        binding_schedule_dialog.button3.setOnClickListener {
            start_time_clock_to_schedule { selectedTime ->
                start_time = selectedTime
            }
        }
        TODO()
        binding_schedule_dialog.button4.setOnClickListener {
            end_time_scheduler { selected_end_time ->
                end_time = selected_end_time.toString()
            }
        }

    }



    fun end_time_scheduler(onTimeSelected: (String) -> Unit) {
        TODO()
        val defaultHour = 12
        val defaultMinute = 0

        val timePicker = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formatter = DateTimeFormatter.ofPattern("HH:mm") // Correct format

            val formattedTime = selectedTime.format(formatter)
            onTimeSelected(formattedTime)  // Return selected time via callback
        }, defaultHour, defaultMinute, true)

        timePicker.show()
    }
    private fun start_time_clock_to_schedule(onTimeSelected: (String) -> Unit) {
        TODO()
        val defaultHour = 12
        val defaultMinute = 0

        val timePicker = TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            val selectedTime = LocalTime.of(selectedHour, selectedMinute)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")  // Correct format

            val formattedTime = selectedTime.format(formatter)
            onTimeSelected(formattedTime)  // Return selected time
        }, defaultHour, defaultMinute, true)

        timePicker.show()
    }




    private fun schedule_upload( start_time:String, end_time:String) {

        var schedule_hashmap = hashMapOf(
            "selected_day" to selected_day,
            "start_time" to start_time,
            "end_time" to end_time
        )

        var user_ref_firebase = firebaseFirestore.collection("users").document(UserManager.current_userId.toString()).collection("schedule").document("preferences")

        user_ref_firebase.set(schedule_hashmap).addOnSuccessListener {

        }



    }


}