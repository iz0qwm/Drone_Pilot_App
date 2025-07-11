package com.kwos.dronepilotapp

import android.R.id.closeButton
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.kwos.dronepilotapp.QuizActivity
import com.kwos.dronepilotapp.R
import com.kwos.dronepilotapp.adapters.QuizAdapter
import com.kwos.dronepilotapp.data.Quiz
import android.widget.TextView


class QuizHomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var quizAdapter: QuizAdapter
    private val quizList = mutableListOf<Quiz>()
    private lateinit var closeButton: Button
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quizhome)


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


        title = "🎓 Quiz formativo"
        // TODO: carica quiz da Firebase
        recyclerView = findViewById(R.id.quizListRecyclerView)
        closeButton = findViewById(R.id.close_quiz_button)
        loadingText = findViewById(R.id.loadingText)

        recyclerView.layoutManager = LinearLayoutManager(this)

        quizAdapter = QuizAdapter(quizList) { selectedQuiz ->
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("quizId", selectedQuiz.id)
            startActivity(intent)
        }

        recyclerView.adapter = quizAdapter

        closeButton.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }


        loadQuizzesFromFirestore()
    }

    private fun loadQuizzesFromFirestore() {
        loadingText.visibility = View.VISIBLE

        FirebaseFirestore.getInstance().collection("quizzes")
            .get()
            .addOnSuccessListener { result ->
                quizList.clear()
                for (doc in result) {
                    val quiz = Quiz(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        category = doc.getString("category") ?: ""
                    )
                    quizList.add(quiz)
                }
                quizAdapter.notifyDataSetChanged()
                loadingText.visibility = View.GONE  // Nasconde la scritta dopo il caricamento
            }
            .addOnFailureListener {
                loadingText.text = "⚠️ Errore durante il caricamento."
            }
    }



}
