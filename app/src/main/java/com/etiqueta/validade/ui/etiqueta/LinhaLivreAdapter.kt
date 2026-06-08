package com.etiqueta.validade.ui.etiqueta

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.etiqueta.validade.R
import com.etiqueta.validade.data.LinhaLivre

class LinhaLivreAdapter(
    private val linhas: MutableList<LinhaLivre>,
    private val onDelete: (Int) -> Unit,
    private val onEdit: (Int) -> Unit
) : RecyclerView.Adapter<LinhaLivreAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTexto: TextView = view.findViewById(R.id.tvLinhaTexto)
        val tvInfo: TextView = view.findViewById(R.id.tvLinhaInfo)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditarLinha)
        val btnDeletar: ImageButton = view.findViewById(R.id.btnDeletarLinha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_linha_livre, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val linha = linhas[position]
        holder.tvTexto.text = linha.texto.ifBlank { "(vazio)" }
        holder.tvTexto.textSize = minOf(linha.tamanhoFonte.toFloat(), 22f)
        val info = buildString {
            append("${linha.tamanhoFonte}pt")
            if (linha.negrito) append(" • Negrito")
            if (linha.centralizado) append(" • Centralizado")
        }
        holder.tvInfo.text = info
        holder.btnEditar.setOnClickListener { onEdit(holder.adapterPosition) }
        holder.btnDeletar.setOnClickListener { onDelete(holder.adapterPosition) }
    }

    override fun getItemCount() = linhas.size
}
