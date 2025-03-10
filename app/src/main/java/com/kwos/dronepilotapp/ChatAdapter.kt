package com.kwos.dronepilotapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(private val context: Context, private var messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid

        holder.myMessage.isVisible = message.senderId == currentUser
        holder.otherMessage.isVisible = message.senderId != currentUser

        holder.myMessage.text = message.message
        holder.otherMessage.text = message.message
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val myMessage: TextView = view.findViewById(R.id.myMessage)
        val otherMessage: TextView = view.findViewById(R.id.otherMessage)
    }
}
