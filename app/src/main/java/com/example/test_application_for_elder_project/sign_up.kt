package com.example.test_application_for_elder_project

import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.HashMap

@Suppress("IMPLICIT_CAST_TO_ANY")
class sign_up : AppCompatActivity() {
    lateinit var binding:ActivitySignUpBinding
    var firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
     var firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.button.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            var role: Any = when {
                binding.elderRadio.isChecked-> "elder"
                binding.parent.isChecked-> "parent"
                binding.eldersFamily.isChecked-> "eldersfamily"
                else -> {
                    Toast.makeText(this,"please select a role",Toast.LENGTH_LONG).show()

                }


            }


        }


    }

    private fun direct_to_the_respective_page(roleFinal: String){
       when (roleFinal){
           "elder" ->{

           }
           "parent"->{

           }
           "eldersfamily"->{

           }

       }


    }

    private fun sign_up(name:String, password:String, email:String, role_final:String){

        firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {
            val userid = firebaseAuth.currentUser

            val user_info_for_sign_up = hashMapOf(
                "name" to name,
                "email" to email,
                "password" to password,
                "time_signed_in" to System.currentTimeMillis()
            )

            firebaseFirestore.collection("users").document(userid.toString()).set(user_info_for_sign_up).addOnSuccessListener {
                Toast.makeText(this,"signed up", LENGTH_LONG).show()
                direct_to_the_respective_page(role_final)
            }

        }

    }
}