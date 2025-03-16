package com.example.test_application_for_elder_project
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri.Builder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.example.elderprojectfinal.data_classes_for_handelling_gemini_response.geminiresponse
import com.example.test_application_for_elder_project.databinding.ActivityTheMainWorkoutMatchBinding
import com.example.test_application_for_elder_project.databinding.MatchedUserProfileLayoutBinding
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.android.gms.dynamic.SupportFragmentWrapper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Calendar
import java.util.Locale








class The_main_workout_match_activity : AppCompatActivity() {

    val firestore:FirebaseFirestore = FirebaseFirestore.getInstance()


    lateinit var binding_for_activity_main_matching:ActivityTheMainWorkoutMatchBinding

    lateinit var bestday:String
    lateinit var start_time:String
    lateinit var end_time:String




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




            var button: Button = findViewById<Button>(R.id.button5)

        button.setOnClickListener {

            matching()
        }




    }


    fun matching() {


        val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

        val userCollection = firestore.collection("users")
        val currentUserId = firebaseAuth.currentUser?.uid ?: return  // Return if user ID is null

        userCollection.document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(ObjectForMatching::class.java)

                user?.let {
                    val currentRole = it.role
                    val interests = it.interests as? List<String> ?: emptyList()

                    if (interests.isEmpty()) return@addOnSuccessListener  // No interests = no matching

                    // Determine the opposite role
                    val targetRole = if (currentRole == "elder") "parent" else "elder"

                    userCollection
                        .whereEqualTo("role", targetRole)
                        .whereArrayContainsAny("interests",interests)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { documentOfMatch ->  // documentOfMatch is a QuerySnapshot
                            val match = documentOfMatch.documents.firstOrNull() // Safe way to get the first match

                            if (match != null) {
                                UserManager.matched_userid = match.id

                                Toast.makeText(this,"matched user ${UserManager.matched_userid.toString()}",Toast.LENGTH_LONG).show()

                                save_match{
                                    Toast.makeText(this,it,Toast.LENGTH_LONG).show()
                                }

                                CoroutineScope(Dispatchers.Main).launch {
                                    val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

                                    // Debugging - Check if matched_userid is valid
                                    Log.d("FirestoreDebug", "Matched User ID: ${UserManager.matched_userid}")

                                    // Check if binding is initialized



                                    firestore.collection("users").document(UserManager.matched_userid.toString())
                                        .get()
                                        .addOnSuccessListener { document ->
                                            if (document.exists()) {
                                                Log.d("FirestoreDebug", "Document retrieved: ${document.data}")

                                                    var name = findViewById<TextView>(R.id.name)
                                                var email = findViewById<TextView>(R.id.email)
                                                    var interest = findViewById<TextView>(R.id.interests)
                                                name.setText(document.getString("name"))
                                                email.setText(document.getString("email"))

                                                val interestsList = document.get("interests") as? List<*>
                                                interest.text = interestsList?.joinToString(", ") ?: "No interests available"

                                            } else {
                                                Log.d("FirestoreDebug", "No document found for ID: ${UserManager.matched_userid}")
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FirestoreError", "Error fetching user data: ${e.message}")
                                        }
                                    setContentView(R.layout.matched_user_profile_layout)
                                    Put_in_the_info_for_the_profile()

                                    var schedule_dialog = findViewById<Button>(R.id.schedule_dialog_button)
                                    schedule_dialog.setOnClickListener{
                                        val schedule_dialog_insta = Schedule_dialog.newInstance(UserManager.current_userId.toString())
                                        schedule_dialog_insta.show(supportFragmentManager,"ScheduleDialog")
                                    }
                                }
                            } else {
                                Log.d("Matching", "No matching user found with role: $targetRole")
                                Toast.makeText(this, "No matching user found with role: $targetRole", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("FirestoreError", "Error fetching match: ${exception.message}")
                            Toast.makeText(this, "Error fetching match: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }


    }

    suspend fun call_finalize_schedule(){
        finalize_the_schedule_time()
    }

        // webrtc section


    }


    fun save_match(callback:(String) -> Unit){
        val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

        val matchData = hashMapOf(
            "matchedUserId" to UserManager.matched_userid.toString(),
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("users")
            .document(UserManager.current_userId.toString())
            .collection("matches")
            .document(UserManager.matched_userid.toString())  // Store match under current user's matches
            .set(matchData)  // Save data in Firestore
            .addOnSuccessListener {
                callback("Match saved successfully!")
            }
            .addOnFailureListener { e ->
                callback("Error saving match: ${e.message}")
            }
        firestore.collection("users")
            .document(UserManager.matched_userid.toString())
            .collection("matches")
            .document(UserManager.current_userId.toString())  // Store match under current user's matches
            .set(matchData)  // Save data in Firestore
            .addOnSuccessListener {
                callback("Match saved successfully!")
            }
            .addOnFailureListener { e ->
                callback("Error saving match: ${e.message}")
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
        val firestore:FirebaseFirestore = FirebaseFirestore.getInstance()
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
    matchedUserStartTime: String?,
    matchedUserEndTime: String?
) {
    val apiKey:String = "AIzaSyDJW69wH1BqmlnSu7XoK9Avhp5v8q_PuE4"
    val model = GenerativeModel(
        "gemini-2.0-flash",
        // Retrieve API key as an environmental variable defined in a Build Configuration
        // see https://github.com/google/secrets-gradle-plugin for further instructions
        apiKey,
        generationConfig = generationConfig {
            temperature = 1f
            topK = 40
            topP = 0.95f
            maxOutputTokens = 8192
            responseMimeType = "text/plain"
        },
    )
    val prompt = """
        Find the best available time for two users based on their schedules.
        User 1: Available on $currentUserDays from $currentUserStartTime to $currentUserEndTime
        User 2: Available on $matchedUserDays from $matchedUserStartTime to $matchedUserEndTime
        Suggest a single best time slot where both users are available. Return the answer in JSON format like - 
        
        {
        "best_day": "Tuesday",
        "start_time": "4:00 AM",
        "end_time": "5:00 AM"
        }
    """.trimIndent()

    val baseUrl = "https://generativelanguage.googleapis.com/v1beta/"

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)  // ✅ Corrected base URL (ends with '/')
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(GeminiApiService::class.java)

    try {
        val response = apiService.generateResponse(
            apiKey,
            GeminiDataPrompt(prompt)
        )

        if (response != null) {
            GeminiResponseHandler(response).handleResponse()
        } else {
            Log.e("GeminiScheduling", "Received null response from API")
        }
    } catch (e: Exception) {
        Log.e("GeminiScheduling", "API request failed: ${e.message}")
    }
}



    // Retrofit API Service
    interface GeminiApiService {
        @Headers("Content-Type: application/json")
        @POST(".")
        suspend fun generateResponse(
            @Query("key") apiKey: String,
            @Body requestBody: GeminiDataPrompt
        ): GeminiResponse
    }

    // Data Classes
    data class GeminiDataPrompt(val prompt: String)

    data class GeminiResponse(
        val candidates: List<Candidate>?
    )

    data class Candidate(
        val content: Content?
    )

    data class Content(
        val parts: List<Part>?
    )

    data class Part(
        val texts: List<String>?
    )

    // Response Handler
    class GeminiResponseHandler(private val response: GeminiResponse) {

        fun handleResponse() {
            val promptActualResponse = response.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()?.texts?.firstOrNull()

            if (promptActualResponse.isNullOrEmpty()) {
                Log.e("GeminiResponseHandler", "Received empty or null response from Gemini")
                return
            }

            try {
                val jsonObject = JSONObject(promptActualResponse)

                val bestDay = jsonObject.optString("best_day", "Unknown")
                val startTime = jsonObject.optString("start_time", "00:00 AM")
                val endTime = jsonObject.optString("end_time", "00:00 AM")

                Log.d("GeminiResponseHandler", "Best Day: $bestDay, Start Time: $startTime, End Time: $endTime")

                scheduleMeetingReminder(bestDay, startTime)

            } catch (e: JSONException) {
                Log.e("GeminiResponseHandler", "Error parsing response: ${e.message}")
            }
        }

        private fun scheduleMeetingReminder(bestDay: String, startTime: String) {
            CoroutineScope(Dispatchers.Main).launch {
                while (true) {
                    delay(30000) // Check every 30 seconds

                    val calendar = Calendar.getInstance()
                    val currentDay = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
                    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = calendar.get(Calendar.MINUTE)

                    val (meetingHour, meetingMinute) = parseTime(startTime)

                    if (currentDay == bestDay && currentHour == meetingHour && currentMinute == meetingMinute) {
                        Log.d("GeminiResponseHandler", "Meeting time reached! Showing call button.")

                        // Perform UI updates safely
                        withContext(Dispatchers.Main) {
                            matchedUserProfileLayoutBinding?.mainVideoCallButton?.visibility = View.VISIBLE
                            matchedUserProfileLayoutBinding?.mainVideoCallButton?.setOnClickListener {
                                Log.d("GeminiResponseHandler", "Video Call Button Clicked!")
                                // TODO: Implement call initiation logic
                            }
                        }
                        break // Stop checking once the meeting time is reached
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
fun Put_in_the_info_for_the_profile() {

}

