package com.etiqueta.validade.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidade que representa um produto cadastrado.
 * diasValidade: quantos dias o produto dura após abertura/manipulação.
 */
@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val diasValidade: Int,
    val categoria: String = "",
    val observacao: String = "",
    val formaArmazenamento: String = ""
)
