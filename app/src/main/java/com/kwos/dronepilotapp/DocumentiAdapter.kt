// DocumentiAdapter.kt
package com.kwos.dronepilotapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kwos.dronepilotapp.models.Documento

class DocumentiAdapter(
    private val documentiList: List<Documento>,
    private val onItemClick: (Documento) -> Unit
) : RecyclerView.Adapter<DocumentiAdapter.DocumentoViewHolder>() {

    class DocumentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textTitle: TextView = view.findViewById(R.id.textDocumentTitle)
        val textType: TextView = view.findViewById(R.id.textDocumentType)
        val textExpiry: TextView = view.findViewById(R.id.textDocumentExpiry)
        val iconPreview: ImageView = view.findViewById(R.id.iconDocumentPreview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_documento, parent, false)
        return DocumentoViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentoViewHolder, position: Int) {
        val documento = documentiList[position]
        holder.textTitle.text = documento.title
        holder.textType.text = documento.type
        holder.textExpiry.text = documento.expiryDate.ifBlank { "Nessuna scadenza" }

        Glide.with(holder.itemView.context)
            .load(documento.fileUrl)
            .placeholder(R.drawable.ic_file_placeholder)
            .into(holder.iconPreview)

        holder.itemView.setOnClickListener {
            onItemClick(documento)
        }
    }

    override fun getItemCount() = documentiList.size
}
