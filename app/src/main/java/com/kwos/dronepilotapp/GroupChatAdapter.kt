package com.kwos.dronepilotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupChatAdapter(private val messages: List<GroupChatActivity.Message>) :
    RecyclerView.Adapter<GroupChatAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_messagegroupchat, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.senderNameTextView.text = message.senderName
        holder.messageTextView.text = message.text
    }



    override fun getItemCount(): Int {
        return messages.size
    }

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderNameTextView: TextView = itemView.findViewById(R.id.textViewSenderGroup)
        val messageTextView: TextView = itemView.findViewById(R.id.textViewMessageGroup)
    }
}
