package com.example.test_application_for_elder_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.elderprojectfinal.databinding.ActivityMainBinding
import com.example.elderprojectfinal.sign_up
import com.example.elderprojectfinal.the_main_workout_match_activity
import com.example.test_application_for_elder_project.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserManager {
    var current_userId: String? = null
    var matched_userid:String? = null
}

class MainActivity : AppCompatActivity() {

    public lateinit var  binding:ActivityMainBinding
    public var firebaseAuth:FirebaseAuth = FirebaseAuth.getInstance()
    public var firebasefirestore:FirebaseFirestore = FirebaseFirestore.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }






        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)


        binding.logIn.setOnClickListener {
            var email = binding.editText2.toString().trim()
            var password = binding.editText.toString().trim()


            if (email.isNotEmpty() && password.isNotEmpty()) {
                sign_in(email, password)
            }

        }

        binding.signUp.setOnClickListener {
            var i: Intent = Intent(this, sign_up::class.java)
            startActivity(i)
        }


    }

    private fun sign_in(email: String, name: String) {
        firebaseAuth.signInWithEmailAndPassword(email, name)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the signed in user's id
                    UserManager.current_userId = firebaseAuth.currentUser?.uid ?: return@addOnCompleteListener
                    // Retrieve the user's document from Firestore
                    firebasefirestore.collection("users").document(UserManager.current_userId.toString()).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val role = document.getString("role") ?: ""
                                if (role.isNotEmpty()) {
                                    Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show()
                                    navigateToHome(role)
                                } else {
                                    Toast.makeText(this, "Role not found", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to get user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToHome(role: String) {
        when (role) {
            "elder" -> {
                var i:Intent = Intent(this,the_main_workout_match_activity::class.java)
                startActivity(i)
            }
            "parent" -> {

            }
            "family" -> {

            }
            else -> {
                Toast.makeText(this, "Invalid role", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
