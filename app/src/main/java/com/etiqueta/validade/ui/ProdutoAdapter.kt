package com.etiqueta.validade.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.etiqueta.validade.data.Produto
import com.etiqueta.validade.databinding.ItemProdutoBinding

class ProdutoAdapter(
    private val onItemClick: (Produto) -> Unit,
    private val onItemLongClick: (Produto) -> Unit
) : ListAdapter<Produto, ProdutoAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Produto>() {
            override fun areItemsTheSame(a: Produto, b: Produto) = a.id == b.id
            override fun areContentsTheSame(a: Produto, b: Produto) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemProdutoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(produto: Produto) {
            binding.tvNomeProduto.text = produto.nome
            binding.tvDiasValidade.text = "${produto.diasValidade} dias"
            binding.tvCategoria.text = produto.categoria.ifBlank { "Sem categoria" }

            // Cor do badge de dias
            val color = when {
                produto.diasValidade <= 2 -> 0xFFD32F2F.toInt()  // vermelho
                produto.diasValidade <= 5 -> 0xFFF57C00.toInt()  // laranja
                else -> 0xFF388E3C.toInt()                        // verde
            }
            binding.tvDiasValidade.setTextColor(color)

            binding.root.setOnClickListener { onItemClick(produto) }
            binding.root.setOnLongClickListener { onItemLongClick(produto); true }
            binding.btnImprimir.setOnClickListener { onItemClick(produto) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProdutoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
