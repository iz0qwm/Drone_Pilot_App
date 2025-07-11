package com.kwos.dronepilotapp.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kwos.dronepilotapp.R
import com.kwos.dronepilotapp.data.Quiz
import android.view.LayoutInflater
import android.view.ViewGroup



class QuizAdapter(
    private val quizzes: List<Quiz>,
    private val onItemClick: (Quiz) -> Unit
) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

    inner class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.quizTitle)
        val description: TextView = itemView.findViewById(R.id.quizDescription)

        init {
            itemView.setOnClickListener {
                onItemClick(quizzes[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz, parent, false)
        return QuizViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quiz = quizzes[position]
        holder.title.text = quiz.title
        holder.description.text = quiz.description
    }

    override fun getItemCount(): Int = quizzes.size
}
