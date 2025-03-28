package com.example.test_application_for_elder_project

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.launch

class Phsycological_test : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_phsycological_test)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var question1 = findViewById<TextView>(R.id.questionText1)
        var question2 = findViewById<TextView>(R.id.questionText2)
        var question3 = findViewById<TextView>(R.id.questionText3)

        var answer1 = findViewById<EditText>(R.id.answerInput1)
        var answer2 = findViewById<EditText>(R.id.answerInput2)
        var answer3 = findViewById<EditText>(R.id.answerInput3)

        var submitButton = findViewById<Button>(R.id.nextButton)

        var api_key = "AIzaSyDJW69wH1BqmlnSu7XoK9Avhp5v8q_PuE4"

        var model =
            GenerativeModel("gemini-2.0-flash", api_key, generationConfig = generationConfig {
                temperature = 1f
                topK = 40
                topP = 0.95f
                maxOutputTokens = 8192
                responseMimeType = "text/plain"
            })

        var chat = model.startChat()

        submitButton.setOnClickListener { v ->
            lifecycleScope.launch {
                val prompt = """
            Evaluate the following psychological responses based on the given questions: 
            Question 1: ${question1.text}
            Answer: ${answer1.text}

            Question 2: ${question2.text}
            Answer: ${answer2.text}

            Question 3: ${question3.text}
            Answer: ${answer3.text}
        """.trimIndent()

                val response =
                    chat.sendMessage(prompt) // Calling the suspend function inside coroutine
                // Handle response here (e.g., show it in a TextView)
            }

        }
    }
}



