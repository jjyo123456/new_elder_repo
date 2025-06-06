package com.example.test_application_for_elder_project
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.data_classes_for_handelling_gemini_response.geminiresponse
import com.example.test_application_for_elder_project.databinding.ActivityTheMainWorkoutMatchBinding
import com.example.test_application_for_elder_project.databinding.MatchedUserProfileLayoutBinding
import com.example.test_application_for_elder_project.test_object.main_video_call_button
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale



object test_object{
    lateinit var test_response:TextView
    lateinit var main_video_call_button:Button
}




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







    public fun test_meeting_scheduler(){
        allow_video_call()
    }


    // schedule the meeting related things after receiving the response from gemin
    private fun scheduleMeetingReminder(bestDay: String, startTime: String) {
        allow_video_call()
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


                        // Start the WebRTC setup activity

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



    public fun allow_video_call(){
        main_video_call_button = findViewById(R.id.main_video_call_button)
        main_video_call_button.setOnClickListener {
            Log.d("GeminiResponseHandler", "Video Call Button Clicked!")
            val intent = Intent(this,AgoraSDKTrial::class.java)
            startActivity(intent)
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
                                                test_object.main_video_call_button = findViewById<Button>(R.id.main_video_call_button)
                                                test_meeting_scheduler()
                                                test_object.test_response = findViewById(R.id.interests)
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

### User Schedules:
- **User 1:** Available on $currentUserDays from $currentUserStartTime to $currentUserEndTime.
- **User 2:** Available on $matchedUserDays from $matchedUserStartTime to $matchedUserEndTime.

### Instructions:
1. **Identify the overlapping time slot** where both users are available.
2. If one user's availability **spans midnight**, interpret it correctly as a next-day availability.
3. Prioritize the **earliest available overlapping time slot**.
4. If **no overlap exists**, return:
   ```json
   {
       "best_day": "None",
       "start_time": "None",
       "end_time": "None"
   }
    """.trimIndent()


    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = model.generateContent(prompt) // ✅ Correct variable name

            withContext(Dispatchers.Main) {
                test_object.test_response.text = response.text  // ✅ Using the correct variable
            }
        } catch (e: Exception) {
            Log.e("GeminiScheduling", "API request failed: ${e.message}")
        }
    }
    val baseUrl = "https://generativelanguage.googleapis.com/v1beta/"
}














    // Retrofit API Service


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

               // scheduleMeetingReminder(bestDay, startTime)

            } catch (e: JSONException) {
                Log.e("GeminiResponseHandler", "Error parsing response: ${e.message}")
            }
        }


    }









