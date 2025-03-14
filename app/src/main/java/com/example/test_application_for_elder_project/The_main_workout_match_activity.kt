package com.example.test_application_for_elder_project
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.data_classes_for_handelling_gemini_response.geminiresponse
import com.example.test_application_for_elder_project.databinding.ActivityTheMainWorkoutMatchBinding
import com.example.test_application_for_elder_project.databinding.MatchedUserProfileLayoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Calendar
import java.util.Locale


@SuppressLint("StaticFieldLeak")
val firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

@SuppressLint("StaticFieldLeak")
lateinit var binding_for_activity_main_matching:ActivityTheMainWorkoutMatchBinding

lateinit var bestday:String
lateinit var start_time:String
lateinit var end_time:String




@Suppress("UNREACHABLE_CODE")
class the_main_workout_match_activity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_the_main_workout_match)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding_for_activity_main_matching = ActivityTheMainWorkoutMatchBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding = MatchedUserProfileLayoutBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding.mainVideoCallButton.visibility = View.GONE




        binding_for_activity_main_matching.button5.setOnClickListener {
            matching()
        }


    }


    public fun matching() {

        var firebaseauth: FirebaseAuth = FirebaseAuth.getInstance()

        var user_collection = firestore.collection("users")
        current_user_id = firebaseauth.currentUser.toString()





        firestore.collection("users").document(UserManager.current_userId.toString())
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(object_for_matching::class.java)

                user?.let {
                    val age_of_user = it.age
                    val interests: List<String> = it.interests as List<String>

                    if (age_of_user.toInt() > 60) {
                        user_collection.whereGreaterThanOrEqualTo("age", 10)
                            .whereLessThanOrEqualTo("age", 18)
                            .whereArrayContainsAny("interests", interests)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { document_of_match ->
                                if (!document_of_match.isEmpty) {
                                    val match = document_of_match.documents[0]
                                    UserManager.matched_userid = match.id

                                    save_match {
                                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                                    }

                                    setContentView(R.layout.matched_user_profile_layout)

                                    CoroutineScope(Dispatchers.Default).launch {
                                        finalize_the_schedule_time()
                                    }
                                    Put_in_the_info_for_the_profile()
                                }
                            }
                    } else if (age_of_user.toInt() < 20) {
                        user_collection.whereGreaterThanOrEqualTo("age", 60)
                            .whereLessThanOrEqualTo("age", 80)
                            .whereArrayContainsAny("interests", interests)
                            .limit(1)
                            .get()
                            .addOnSuccessListener { document_of_match ->
                                if (!document_of_match.isEmpty) {
                                    val match = document_of_match.documents[0]
                                    UserManager.matched_userid = match.id

                                    save_match {
                                        Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                                    }

                                    setContentView(R.layout.matched_user_profile_layout)

                                    CoroutineScope(Dispatchers.Default).launch {
                                        finalize_the_schedule_time()
                                    }

                                    Put_in_the_info_for_the_profile()
                                }
                            }
                    }

                }


            }





        // webrtc section


    }

    fun save_match(callback:(String) -> Unit){
        var firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread {

            firestore.collection("users").document(UserManager.current_userId.toString()).collection("matches")
                .document(UserManager.matched_userid.toString())

            callback("save_succsessfull")
        }
    }

    fun deletematch() {
        var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread {
            firestore.collection("users").document(current_user_id).collection("matches")
                .document(match_id).delete().addOnSuccessListener {
                }

        }

    }

    suspend fun finalize_the_schedule_time() {

        var current_user_ref_firebase =
            firestore.collection("users").document(UserManager.current_userId.toString()).collection("schedule")
                .document("preferences")

        lateinit var current_user_days: String

        var current_user_start_time: String? = null

        var current_user_end_time: String? = null

        lateinit var matched_user_days: String

        var matched_user_start_time: String? = null

        var matched_user_end_time: String? = null


        current_user_ref_firebase.get().addOnSuccessListener { document ->
            current_user_days = document.get("selected_day").toString()
            current_user_start_time = document.get("start_time").toString()
            current_user_end_time = document.get("end_time").toString()
        }

        var mathced_user_ref_firebase =
            firestore.collection("users").document(UserManager.matched_userid.toString()).collection("schedule")
                .document("preferences")

        mathced_user_ref_firebase.get().addOnSuccessListener { document ->
            matched_user_days = document.get("selected_day").toString()
            matched_user_start_time = document.get("start_time").toString()
            matched_user_end_time = document.get("end_time").toString()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    sendToGeminiForScheduling(
                        current_user_days,
                        current_user_start_time,
                        current_user_end_time,
                        matched_user_days,
                        matched_user_start_time,
                        matched_user_end_time
                    )
                } catch (e: Exception) {
                    Log.e("GeminiError", "Error scheduling meeting: ${e.message}")
                }
            }
        }


    }

    suspend fun sendToGeminiForScheduling(
        currentUserDays: String,
        currentUserStartTime: String?,
        currentUserEndTime: String?,
        matchedUserDays: String,
        matchedUserStartTime: Any?,
        matchedUserEndTime: Any?
    ) {

        val prompt = """
        Find the best available time for two users based on their schedules.
        User 1: Available on $currentUserDays from $currentUserStartTime to $currentUserEndTime
        User 2: Available on $matchedUserDays from $matchedUserStartTime to $matchedUserEndTime
        Suggest a single best time slot where both users are available.return the answer in Json format like - 
        
        {
        best_day : "Tuesday"
        start_time : 4:00 AM
        end_tme : 5:00 AM
    """.trimIndent()


        var url: String =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"
        val gemini_retrofit =
            Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create())
                .build()

        var apiservie = gemini_retrofit.create(geminiapiservice::class.java)


        try {
            var response = apiservie.generateresponse(apiKey, gemini_data_prompt(prompt))
            if(response != null){
                val responseBody = response.body()
                responseBody?.handle_response()
            }
        }
        catch (e:Exception){

        }
    }

    public interface geminiapiservice {
        @Headers("content-type : application/json")
        @POST()
        suspend fun generateresponse(
            @Query("key") apiKey: String,
            @Body requestBody: gemini_data_prompt
        ): Response<geminiresponse>


    }


    data class gemini_data_prompt(val prompt: String)

    data class GeminiResponseHandler(var response: geminiresponse) {
        fun handle_response() {
            // Extracting the actual response text
            val promptActualResponse =
                response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.texts?.firstOrNull()

            if (promptActualResponse.isNullOrEmpty()) {
                Log.e("GeminiResponseHandler", "Received empty or null response from Gemini")
                return
            }

            try {
                // Parse JSON only if it’s valid
                val jsonObject = JSONObject(promptActualResponse)

                val bestDay = jsonObject.optString("best_day", "Unknown")
                val startTime = jsonObject.optString("start_time", "00:00 AM")
                val endTime = jsonObject.optString("end_time", "00:00 AM")

                Log.d("GeminiResponseHandler", "Best Day: $bestDay, Start Time: $startTime, End Time: $endTime")

            } catch (e: JSONException) {
                Log.e("GeminiResponseHandler", "Error parsing response: ${e.message}")
            }


                GlobalScope.launch {
                    while (true) {
                        delay(30000)
                        val calendar = Calendar.getInstance()
                        val current_Day = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                        val current_hour = calendar.get(Calendar.HOUR_OF_DAY)
                        val current_minute = calendar.get(Calendar.MINUTE)

                        val (meetingHour, meetingMinute) = parseTime(start_time)


                        if (current_Day == bestday && current_hour == meetingHour && current_minute == meetingMinute) {
                            TODO()
                            matchedUserProfileLayoutBinding.mainVideoCallButton.visibility =
                                View.VISIBLE
                            matchedUserProfileLayoutBinding.mainVideoCallButton.setOnClickListener({

                            })
                        }
                    }


                }
        }

        private fun parseTime(time: String): Pair<Int, Int> {
            val timeParts = time.split(" ")
            val timeValues = timeParts[0].split(":").map { it.toInt() }
            val isPM = timeParts[1].equals("PM", ignoreCase = true)

            var hour = timeValues[0]
            val minute = timeValues.getOrElse(1) { 0 }

            if (isPM && hour != 12) {
                hour += 12
            } else if (!isPM && hour == 12) {
                hour = 0
            }

            return Pair(hour, minute)
        }
    }

    public fun Put_in_the_info_for_the_profile(){

    }
}