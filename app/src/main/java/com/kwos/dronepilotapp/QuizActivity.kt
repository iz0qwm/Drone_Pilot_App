package com.kwos.dronepilotapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.kwos.dronepilotapp.R
import com.kwos.dronepilotapp.adapters.QuizAdapter
import com.kwos.dronepilotapp.data.Quiz
import com.kwos.dronepilotapp.data.Question

class QuizActivity : AppCompatActivity() {

    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var nextButton: Button
    private lateinit var counterText: TextView
    private val wrongQuestions = mutableListOf<Question>()

    private var quizId: String = ""
    private var questions: List<Question> = listOf()
    private var currentQuestionIndex = 0
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        //val rootView = findViewById<View>(android.R.id.content)
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)


        // INIZIO PADDING
        // EDGE-TO-EDGE
        // Modalità edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false) // Abilita modalità edge-to-edge

        // Imposta se il contenuto della status bar deve essere scuro (true) o chiaro (false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true // o false, dipende dal tema

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // FINE EDGE-TO-EDGE

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // GESTIONE INSETS
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // SOLO paddingBottom per evitare che l'ultima parte vada sotto la navigation bar
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        // fine padding
        // Nasconde la Action Bar
        supportActionBar?.hide()
        // FINE PADDING


        quizId = intent.getStringExtra("quizId") ?: return finish()

        questionText = findViewById(R.id.questionText)
        optionsGroup = findViewById(R.id.optionsGroup)
        nextButton = findViewById(R.id.nextButton)
        counterText = findViewById(R.id.questionCounter)

        nextButton.setOnClickListener { validateAndNext() }

        loadQuestions()
    }

    private fun loadQuestions() {
        FirebaseFirestore.getInstance()
            .collection("quizzes").document(quizId)
            .collection("questions").get()
            .addOnSuccessListener { result ->
                val allQuestions = result.documents
                    .mapNotNull { it.toObject(Question::class.java) }
                    .shuffled()

                questions = if (allQuestions.size >= 5) allQuestions.take(5) else allQuestions
                showQuestion()
            }
    }

    private fun showQuestion() {

        if (currentQuestionIndex >= questions.size) {
            // Fine quiz
            Log.d("QUIZ", "Domande sbagliate: ${wrongQuestions.size}")

            val intent = Intent(this, QuizResultActivity::class.java)
            intent.putExtra("score", score)
            intent.putExtra("total", questions.size)
            intent.putExtra("wrongQuestions", ArrayList(wrongQuestions))
            intent.putExtra("quizId", quizId)
            startActivity(intent)
            finish()
            return
        }

        val question = questions[currentQuestionIndex]

        counterText.text = "Domanda ${currentQuestionIndex + 1}/${questions.size}"
        questionText.text = question.questionText
        optionsGroup.removeAllViews()

        question.options.forEachIndexed { index, option ->
            val radioButton = RadioButton(this)
            radioButton.text = option
            radioButton.id = View.generateViewId()  // ID sicuro
            radioButton.tag = index  // Salva l’indice corretto come tag
            optionsGroup.addView(radioButton)
        }

    }

    private fun validateAndNext() {
        val selectedId = optionsGroup.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(this, "Seleziona una risposta", Toast.LENGTH_SHORT).show()
            return
        }

        val question = questions[currentQuestionIndex]
        val correct = question.correctOptionIndex

        val selectedRadioButton = findViewById<RadioButton>(selectedId)
        val selectedIndex = selectedRadioButton?.tag as? Int ?: -1

        if (correct !in question.options.indices) {
            Log.e("QUIZ", "❗️ correctOptionIndex fuori range: $correct per domanda '${question.questionText}'")
        }

        if (selectedIndex == correct) {
            score++
        } else {
            Log.d("QUIZ", "Aggiunta domanda sbagliata: ${question.questionText}")
            wrongQuestions.add(question)
        }

        currentQuestionIndex++
        showQuestion()
    }



}
