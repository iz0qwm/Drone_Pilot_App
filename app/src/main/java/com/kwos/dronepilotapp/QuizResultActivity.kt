package com.kwos.dronepilotapp

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kwos.dronepilotapp.data.Question
import android.content.Intent
import android.widget.Button

class QuizResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_result)

        // Edge-to-edge UI
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        supportActionBar?.hide()

        val score = intent.getIntExtra("score", 0)
        val total = intent.getIntExtra("total", 0)
        @Suppress("UNCHECKED_CAST")
        val wrongQuestions = intent.getSerializableExtra("wrongQuestions") as? ArrayList<Question> ?: arrayListOf()

        val scoreText = findViewById<TextView>(R.id.scoreText)
        scoreText.text = "Hai risposto correttamente a $score su $total domande"

        val container = findViewById<LinearLayout>(R.id.wrongAnswersContainer)

        if (wrongQuestions.isEmpty()) {
            findViewById<TextView>(R.id.wrongAnswersLabel).text = "✅ Nessun errore, ottimo lavoro!"
        } else {
            wrongQuestions.forEachIndexed { i, question ->
                val textView = TextView(this).apply {
                    text = "🔸 ${question.questionText}\n📘 ${question.explanation}\n"
                    textSize = 16f
                    setPadding(0, 12, 0, 12)
                }
                container.addView(textView)
            }
        }

        val retryButton = findViewById<Button>(R.id.retryButton)
        val backToHomeButton = findViewById<Button>(R.id.backToHomeButton)

        // Riprova quiz
        retryButton.setOnClickListener {
            val quizId = intent.getStringExtra("quizId") ?: return@setOnClickListener
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("quizId", quizId)
            startActivity(intent)
            finish()
        }

        // Torna alla home
        backToHomeButton.setOnClickListener {
            val intent = Intent(this, QuizHomeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

    }
}
