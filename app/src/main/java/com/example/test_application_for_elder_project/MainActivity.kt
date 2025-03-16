package com.example.test_application_for_elder_project

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object UserManager {
    var current_userId: String? = null
    var matched_userid: String? = null
    val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    public val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firebasefirestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.logIn.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val code = binding.editextForCode.text.toString().trim()

            if(code.isNotEmpty()){
                sign_in_with_code()
            }
            else if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show()
            } else {
                sign_in(email, password)
            }
        }

        binding.signUp.setOnClickListener {
            startActivity(Intent(this, Sign_up::class.java))
        }


    }

    private fun sign_in(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    UserManager.current_userId = firebaseAuth.currentUser?.uid
                    UserManager.current_userId?.let { userId ->
                        firebasefirestore.collection("users").document(userId).get()
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
                    } ?: Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Sign in failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToHome(role: String) {
        val intent = when (role) {
            "elder" ->{var i= Intent(this, The_main_workout_match_activity::class.java)
            startActivity(i)
            }

            "parent" -> {
                var i = Intent(this, The_main_workout_match_activity::class.java)
                startActivity(i)
            }
            "family" -> {
                var i = Intent(this,Profile_for_family::class.java)
                startActivity(i)
            }
            else -> {
                Toast.makeText(this, "Invalid role", Toast.LENGTH_SHORT).show()
                return
            }
        }
     //   startActivity(intent)
    }

    private fun sign_in_with_code() {
        val code = binding.editextForCode.text.toString().trim()

        if (code.isEmpty()) {
            Toast.makeText(this, "Please enter a code", Toast.LENGTH_LONG).show()
            return
        }

        firebasefirestore.collection("users").document(code).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val email = document.getString("email") ?: ""
                    val password = document.getString("password") ?: ""

                    if (email.isNotEmpty() && password.isNotEmpty()) {
                        sign_in(email, password)
                    } else {
                        Toast.makeText(this, "Invalid email or password", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "User not found", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user", Toast.LENGTH_LONG).show()
            }
    }
}
