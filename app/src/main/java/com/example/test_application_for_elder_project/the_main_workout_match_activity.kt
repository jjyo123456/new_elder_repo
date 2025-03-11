package com.example.test_application_for_elder_project
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.data_classes_for_handelling_gemini_response.geminiresponse
import com.example.elderprojectfinal.databinding.ActivityTheMainWorkoutMatchBinding
import com.example.elderprojectfinal.databinding.MatchedUserProfileLayoutBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Response
import org.webrtc.*
import org.webrtc.PeerConnection.IceServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.Calendar


@SuppressLint("StaticFieldLeak")
val firestore:FirebaseFirestore = FirebaseFirestore.getInstance()




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

        binding = ActivityTheMainWorkoutMatchBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding = MatchedUserProfileLayoutBinding.inflate(layoutInflater)

        matchedUserProfileLayoutBinding.mainVideoCallButton.visibility = View.GONE




        binding.button5.setOnClickListener({
            matching()
        })




    }


    public fun matching(){

        var firebaseauth:FirebaseAuth = FirebaseAuth.getInstance()

        var user_collection = firestore.collection("users")
        current_user_id = firebaseauth.currentUser.toString()



        TODO()
        val a = firestore.collection().document().collection().get().addOnSuccessListener { document ->
            val user:object_for_matching = document.toObjects<object_for_matching>()bject(object_for_matching::class.java)

            val age_of_user = user.age
            val interests:Array<String> = user.interests
            val workout_tpye = user.workout_type
            val weight = user.weight
            val fitness_goal = user.fitness_goal

            user_collection
                .whereGreaterThanOrEqualTo("age", age_of_user - 2)
            .whereLessThanOrEqualTo("age", age_of_user + 2)
            .whereGreaterThanOrEqualTo("weight",  weight - 10)
            .whereLessThanOrEqualTo("weight", weight + 10)
            .whereEqualTo("fitness_goal", fitness_goal)
            .whereEqualTo("workout_mode", workout_tpye)
                .limit(1)
            .get()
                .addOnSuccessListener {document_of_match ->
                    val match = document_of_match.documents[0]
                  match_id = match.id

                   save_match(current_user_id , match_id){save_match_result ->



                    }

                    setContentView(R.layout.matched_user_profile_layout)

                    finalize_the_schedule_time(current_user_id,match_id)
                    TODO()
                    Put_in_the_info_for_the_profile(match_id)



                }
                .addOnFailureListener({

                })



        }







    }

    public fun save_match(current_user_idd:String, match_id:String, callback:(String) -> Unit){
        var firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread {

            firestore.collection("users").document(current_user_idd).collection("matches")
                .document(match_id)

            callback("save_succsessfull")
        }
    }

    public fun deletematch(){
        var firestore:FirebaseFirestore = FirebaseFirestore.getInstance()

        Thread{
        firestore.collection("users").document(current_user_id).collection("matches").document(match_id).delete().addOnSuccessListener {
        }

        }

    }

    public fun finalize_the_schedule_time(current_user_id: String, match_id: String){

        var current_user_ref_firebase = firestore.collection("users").document(current_user_id).collection("schedule").document("preferences")

        var current_user_days: List<String>? = null

        var current_user_start_time:String? = null

        var current_user_end_time:String? = null

        var matched_user_days:List<String>? = null

        var matched_user_start_time:String? = null

        var matched_user_end_time:String? = null

        TODO()
            current_user_ref_firebase.get().addOnSuccessListener {document->
            var current_user_days:List<String> = document.get("")
            var current_user_start_time = document.get("start_time")
            var current_user_end_time = document.get("end_time")
        }

        var mathced_user_ref_firebase = firestore.collection("users").document(match_id).collection("schedule").document("preferences")
        TODO()
        current_user_ref_firebase.get().addOnSuccessListener {document->
            var matched_user_days:List<String> = document.get("")
            var matched_user_start_time = document.get("start_time")
            var matched_user_end_time = document.get("end_time")


                sendToGeminiForScheduling(current_user_days,current_user_start_time,current_user_end_time,matched_user_days,matched_user_start_time,matched_user_end_time )


        }





    }

    suspend fun sendToGeminiForScheduling(
        currentUserDays: List<String>?, currentUserStartTime: String?, currentUserEndTime: String?,
        matchedUserDays: List<String>?, matchedUserStartTime: Any?, matchedUserEndTime: Any?
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



        var url:String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?"
        val gemini_retrofit = Retrofit.Builder().baseUrl(url).addConverterFactory(GsonConverterFactory.create()).build()

        var apiservie = gemini_retrofit.create(geminiapiservice::class.java)


        apiservie.generateresponse(apiKey,gemini_data_prompt(prompt))

    }

    public interface geminiapiservice{
        @Headers("content-type : application/json")
        @POST()
        suspend fun generateresponse(
            @Query("key") apiKey:String,
            @Body requestBody:  gemini_data_prompt
        ):geminiresponse_handler


    }








    data class gemini_data_prompt(val string:String){
        val prompt:String = string
    }

    data class geminiresponse_handler(var response:geminiresponse){
        var prompt_actual_response = response.candidates.firstOrNull()?.content.parts.firstOrNull()?.texts

        var best_day =
        var start_time =
        var end_tim =


           GlobalScope.launch {
               while(true){
                   delay(30000)
                   val calendar = Calendar.getInstance()
                   val day = calendar.get(Calendar.DAY_OF_MONTH)
                   val hour = calendar.get(Calendar.HOUR_OF_DAY)
                   val minute = calendar.get(Calendar.MINUTE)

                   if(day == best_day && ){
                       TODO()
                       matchedUserProfileLayoutBinding.mainVideoCallButton.visibility = View.VISIBLE
                       matchedUserProfileLayoutBinding.mainVideoCallButton.setOnClickListener({

                       })
                   }
               }




           }


    }






    // webrtc section







}