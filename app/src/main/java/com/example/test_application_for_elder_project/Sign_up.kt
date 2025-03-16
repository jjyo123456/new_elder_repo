package com.example.test_application_for_elder_project

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivitySignUpBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Sign_up : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val firebaseFirestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.button.setOnClickListener {
            val name = binding.name.text.toString().trim()
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (!validateInputs(name, email, password)) return@setOnClickListener

            val interests = getSelectedInterests()
            if (interests.isEmpty()) {
                showToast("Please select at least one interest")
                return@setOnClickListener
            }

            val role = getSelectedRole() ?: return@setOnClickListener

            signUp(name, email, password, role, interests)
        }
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        return when {
            name.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                showToast("All fields are required")
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showToast("Invalid email format")
                false
            }
            password.length < 6 -> {
                showToast("Password must be at least 6 characters long")
                false
            }
            else -> true
        }
    }

    private fun getSelectedInterests(): List<String> {
        val interests = mutableListOf<String>()
        for (i in 0 until binding.chipGroup.childCount) {
            val chip = binding.chipGroup.getChildAt(i) as? Chip
            if (chip?.isChecked == true) {
                interests.add(chip.text.toString())
            }
        }
        return interests
    }

    private fun getSelectedRole(): String? {
        return when {
            binding.elderRadio.isChecked -> "elder"
            binding.parent.isChecked -> "parent"
            binding.eldersFamily.isChecked -> "eldersfamily"
            else -> {
                showToast("Please select a role")
                null
            }
        }
    }

    private fun signUp(name: String, email: String, password: String, role: String, interests: List<String>) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    showToast("User authentication failed")
                    return@addOnSuccessListener
                }

                val userInfo = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to role,
                    "interests" to interests,
                    "time_signed_in" to System.currentTimeMillis()
                )

                firebaseFirestore.collection("users")
                    .document(currentUser.uid)
                    .set(userInfo)
                    .addOnSuccessListener {
                        showToast("Signed up successfully")
                        navigateToRolePage(role)
                    }
                    .addOnFailureListener { exception ->
                        showToast("Firestore Error: ${exception.localizedMessage}")
                    }
            }
            .addOnFailureListener { exception ->
                showToast("Sign-up failed: ${exception.localizedMessage}")
            }
    }

    private fun navigateToRolePage(role: String) {
        val intent = when (role) {
            "elder" -> Intent(this, The_main_workout_match_activity::class.java)
            "parent" -> {}
            "eldersfamily" ->{}
            else -> null
        }
        intent?.let {
            startActivity(it as Intent?) }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
