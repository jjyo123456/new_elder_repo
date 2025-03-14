package com.example.test_application_for_elder_project

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.test_application_for_elder_project.databinding.ActivityProfileForElderFinalBinding
import com.example.test_application_for_elder_project.databinding.ActivityProfileForFamilyBinding
import com.example.test_application_for_elder_project.databinding.ChangeDetailsForEldersBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Profile_for_family : AppCompatActivity() {
    var binding_for_family_layout: ActivityProfileForFamilyBinding = ActivityProfileForFamilyBinding.inflate(layoutInflater)
    var binding_for_elder_layout: ActivityProfileForElderFinalBinding = ActivityProfileForElderFinalBinding.inflate(layoutInflater)
    var binding_for_elder_change_layout = ChangeDetailsForEldersBinding.inflate(layoutInflater)
    var db:FirebaseFirestore = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_for_family)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets

           // load_User_Data()
        }
        binding_for_family_layout.changeElderProfile.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                elder_user_data()
            }


            // Replace the root view with the new layout
            setContentView(binding_for_elder_layout.root)
        }

        binding_for_elder_layout.changeElderProfile.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                updateUserData()
            }

            setContentView(binding_for_elder_change_layout.root)
        }


    }

private fun load_User_Data() {
    db.collection("users").document(UserManager.current_userId.toString()).get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val name = document.getString("name") ?: "No Name"
                val email = document.getString("email") ?: "No Email"
                val interests = document.get("interests") as? List<String> ?: emptyList()

                binding_for_family_layout.name.setText(name)
                binding_for_family_layout.email.setText(email)

                binding_for_family_layout.interests.setText(interests.joinToString(","))
            }
        }
}

    fun elder_user_data() {
        db.collection("users").document(UserManager.current_userId.toString()).get()
            .addOnSuccessListener { document ->
               var managed_elder = document.get("managed_elder") as? Map<*, *>
                var elder_id = managed_elder?.get("elder_id") as? String
                if(elder_id != null) {
                    db.collection("users").document(elder_id.toString()).get()
                        .addOnSuccessListener { document ->
                            binding_for_elder_layout.name.setText(
                                document.getString("name") ?: "No Name"
                            )
                            binding_for_elder_layout.email.setText(document.getString("email"))
                            binding_for_elder_layout.interests.setText(
                                (document.getString("interests") as? List<String>
                                    ?: emptyList()).joinToString(",")
                            )
                        }


            }
                else {
                    setContentView(binding_for_elder_change_layout.root)
                    binding_for_elder_change_layout.updateDetailsElders.hint = "save_details"
                    binding_for_elder_change_layout.updateDetailsElders.setOnClickListener {
                        createElderProfile()
                    }
                }
            }
            }


    fun updateUserData() {
        val newName = binding_for_elder_change_layout.name
        val newEmail = binding_for_elder_change_layout.email
        val newInterests = binding_for_elder_change_layout.interests.text.split(',').map{it.trim()}

        val updates = hashMapOf(
            "name" to newName,
            "email" to newEmail,
            "interests" to newInterests
        )

        db.collection("users").document(UserManager.current_userId.toString()).update(updates)
            .addOnSuccessListener {
                binding_for_elder_layout.name.text = newName.toString()
                binding_for_elder_layout.email.text= newEmail.toString()
                binding_for_elder_layout.interests.text = newInterests.joinToString(", ")
                Toast.makeText(this, "Updated Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun createElderProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val elderRef = db.collection("users").document() // Auto-generated ID
            val elderId = elderRef.id  // This will be used as the login code

            val elderData = hashMapOf(
                "name" to binding_for_elder_change_layout.name.text.toString(),
                "email" to binding_for_elder_change_layout.email.text.toString(),
                "interests" to binding_for_elder_change_layout.interests.text.split(',')
                    .map { it.trim() },
                "role" to "elder",  // Role for differentiation
                "managed_by" to UserManager.current_userId.toString() // Link to family member
            )

            elderRef.set(elderData)
                .addOnSuccessListener {
                    saveElderIdForFamily(elderId)  // Save elderId in family member's profile
                    binding_for_elder_change_layout.textView2.text = elderId
                }
                .addOnFailureListener {
                }

        }
    }

    // Store the elder's ID in the family member’s profile
    fun saveElderIdForFamily(elderId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val familyUpdate = hashMapOf("managed_elder" to elderId)

            db.collection("users").document(UserManager.current_userId.toString())
                .update(familyUpdate as Map<String, Any>)
                .addOnSuccessListener {
                }
        }
    }

}