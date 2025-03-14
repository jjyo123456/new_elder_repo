package com.example.test_application_for_elder_project

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivitySignUpBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class Sign_up : AppCompatActivity() {
    lateinit var binding: ActivitySignUpBinding
    var firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
     var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val interests = mutableListOf<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //setting the binding varriable for this layout
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // creating the code to be executed when the sign_up button is clicked where the info will be extracted from the detitextview where info is typed
        binding.button.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()


            val chipGroup = binding.chipGroup
            for(i in 0 until chipGroup.childCount){
                val chips = chipGroup.getChildAt(i) as Chip
                if(chips.isChecked){
                    interests.add(chips.text.toString())
                }
        }

            var role: Any = when {
                binding.elderRadio.isChecked-> "elder"
                binding.parent.isChecked-> "parent"
                binding.eldersFamily.isChecked-> "eldersfamily"
                else -> {
                    Toast.makeText(this,"please select a role",Toast.LENGTH_LONG).show()

                }
            }


                // If an elder user is logging in using a code




                // If a normal sign-up is happening
                sign_up(name, email, password, role.toString())





        }


    }


    private fun sign_up(name:String, password:String, email:String, role_final:String){

        //creating new user in firebaseAuth with the username and password
        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {


            val user_info_for_sign_up = hashMapOf(
                "name" to name,
                "email" to email,
                "password" to password,
                "role" to role_final,
                "interests" to interests,
                "time_signed_in" to System.currentTimeMillis()
            )

            // creating a new ocument with the current user's id in the collection of users inside the firestore
            firebaseFirestore.collection("users").document(UserManager.current_userId.toString()).set(user_info_for_sign_up).addOnSuccessListener {
                Toast.makeText(this,"signed up", LENGTH_LONG).show()
                direct_to_the_respective_page(role_final)
            }

        }

    }

    private fun direct_to_the_respective_page(roleFinal: String){
       when (roleFinal){
           "elder" ->{
               var i: Intent = Intent(this,the_main_workout_match_activity::class.java)
               startActivity(i)
           }
           "parent"->{

           }
           "eldersfamily"->{

           }

       }


    }




}