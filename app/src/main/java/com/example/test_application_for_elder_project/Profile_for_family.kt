package com.example.test_application_for_elder_project

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_application_for_elder_project.databinding.ActivityProfileForElderFinalBinding
import com.example.test_application_for_elder_project.databinding.ActivityProfileForFamilyBinding
import com.example.test_application_for_elder_project.databinding.ChangeDetailsForEldersBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Profile_for_family : AppCompatActivity() {
    private lateinit var bindingForFamily: ActivityProfileForFamilyBinding
    private lateinit var bindingForElder: ActivityProfileForElderFinalBinding
    private lateinit var bindingForElderChange: ChangeDetailsForEldersBinding
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        bindingForFamily = ActivityProfileForFamilyBinding.inflate(layoutInflater)
        bindingForElder = ActivityProfileForElderFinalBinding.inflate(layoutInflater)
        bindingForElderChange = ChangeDetailsForEldersBinding.inflate(layoutInflater)

        // Set the initial content view (family profile)
        setContentView(bindingForFamily.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Load the user's profile data from Firestore
        loadUserData()

        // Handle the "Change Elder Profile" button click
        bindingForFamily.changeElderProfile.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                elderUserData()
            }
        }

        // Handle updating elder details
        bindingForElder.changeElderProfile.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                updateUserData()
            }
        }
    }

    private fun loadUserData() {
        db.collection("users").document(UserManager.current_userId.toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "No Name"
                    val email = document.getString("email") ?: "No Email"
                    val interests = document.get("interests") as? List<String> ?: emptyList()

                    runOnUiThread {
                        bindingForFamily.name.setText(name)
                        bindingForFamily.email.setText(email)
                        bindingForFamily.interests.setText(interests.joinToString(", "))
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun elderUserData() {
        db.collection("users").document(UserManager.current_userId.toString()).get()
            .addOnSuccessListener { document ->
                val managedElder = document.get("managed_elder") as? Map<*, *>
                val elderId = managedElder?.get("elder_id") as? String

                if (elderId != null) {
                    db.collection("users").document(elderId).get()
                        .addOnSuccessListener { elderDoc ->
                            runOnUiThread {
                                bindingForElder.name.setText(elderDoc.getString("name") ?: "No Name")
                                bindingForElder.email.setText(elderDoc.getString("email"))
                                bindingForElder.interests.setText(
                                    (elderDoc.get("interests") as? List<String> ?: emptyList()).joinToString(", ")
                                )

                                // Now show the elder's profile view
                                setContentView(bindingForElder.root)
                            }
                        }
                } else {
                    runOnUiThread {
                        setContentView(bindingForElderChange.root)
                        bindingForElderChange.updateDetailsElders.hint = "Save Details"
                        bindingForElderChange.updateDetailsElders.setOnClickListener {
                            createElderProfile()
                        }
                    }
                }
            }
    }

    private fun updateUserData() {
        val newName = bindingForElderChange.name.text.toString()
        val password = bindingForElderChange.password.text.toString()
        val newEmail = bindingForElderChange.email.text.toString()
        val newInterests = bindingForElderChange.interests.text.toString().split(",").map { it.trim() }

        val updates = mapOf(
            "name" to newName,
            "password" to password,
            "email" to newEmail,
            "interests" to newInterests
        )

        db.collection("users").document(UserManager.current_userId.toString()).update(updates)
            .addOnSuccessListener {
                runOnUiThread {
                    bindingForElder.name.text = newName
                    bindingForElder.email.text = newEmail
                    bindingForElder.interests.text = newInterests.joinToString(", ")
                    Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createElderProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val elderRef = db.collection("users").document()
            val elderId = elderRef.id

            val elderData = mapOf(
                "name" to bindingForElderChange.name.text.toString(),
                "password" to bindingForElderChange.password.text.toString(),
                "email" to bindingForElderChange.email.text.toString(),
                "interests" to bindingForElderChange.interests.text.toString().split(",").map { it.trim() },
                "role" to "elder",
                "managed_by" to UserManager.current_userId.toString()
            )

            elderRef.set(elderData)
                .addOnSuccessListener {
                    saveElderIdForFamily(elderId)
                    runOnUiThread {
                        bindingForElderChange.textView2.text = elderId

                        UserManager.firebaseAuth.createUserWithEmailAndPassword(bindingForElderChange.email.text.toString(),bindingForElderChange.password.text.toString())
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@Profile_for_family, "Failed to create elder profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveElderIdForFamily(elderId: String) {

        db.collection("users").document(UserManager.current_userId.toString())
            .update(mapOf("managed_elder" to elderId))
            .addOnSuccessListener {
                Toast.makeText(this, "Elder linked successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to link elder", Toast.LENGTH_SHORT).show()
            }
    }
}
