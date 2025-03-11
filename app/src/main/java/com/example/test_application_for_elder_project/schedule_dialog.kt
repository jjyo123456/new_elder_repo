package com.example.test_application_for_elder_project

import android.app.TimePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.elderprojectfinal.databinding.ScheduleDialogBinding
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Time
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Formatter

class schedule_dialog(var userid:String,context: Context) :DialogFragment() {


    var binding:ScheduleDialogBinding = ScheduleDialogBinding.inflate(LayoutInflater.from(context))

    var firebaseauth:FirebaseAuth = FirebaseAuth.getInstance()

    var firebaseFirestore:FirebaseFirestore = FirebaseFirestore.getInstance()

    var calendar:Calendar = Calendar.getInstance()

    public var defaulthour = calendar.get(Calendar.HOUR_OF_DAY)
    public var defaultminute = calendar.get(Calendar.MINUTE)


    private lateinit var start_time:String

    private lateinit var end_time:String


        init {
            binding.finalizeSchedule.setOnClickListener {
            schedule_upload(start_time,end_time)
            }

            binding.button3.setOnClickListener {
                start_time = start_time_clock_to_schedule()
            }
            binding.button4.setOnClickListener {
                end_time = end_time_schedular()
            }

        }



    private fun end_time_schedular(): String {

            var selecthour = 0
            var selectedminute = 0



            var timepicker = TimePickerDialog(this,{,selecthour,selectedminute
            }, defaulthour ,defaultminute,
                true

            )

            timepicker.show()

            var selected_time:LocalTime = LocalTime.of(selecthour,selectedminute)

            var formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:MM")

            return selected_time.format(formatter)
    }






    private fun start_time_clock_to_schedule(): String {
        var selecthour = 0
        var selectedminute = 0



        var timepicker = TimePickerDialog(this,{,selecthour,selectedminute
        }, defaulthour ,defaultminute,
            true

        )

        timepicker.show()

        var selected_time:LocalTime = LocalTime.of(selecthour,selectedminute)

        var formatter: DateTimeFormatter? = DateTimeFormatter.ofPattern("HH:MM")

        return selected_time.format(formatter)



    }



    private fun schedule_upload( start_time:String, end_time:String) {

        var schedule_hashmap = hashMapOf(
            "start_time" to start_time,
            "end_time" to end_time
        )

        var user_ref_firebase = firebaseFirestore.collection("users").document(userid).collection("schedule").document("preferences")

        user_ref_firebase.update("schedule_availlability",schedule_hashmap).addOnSuccessListener {

        }



    }


}