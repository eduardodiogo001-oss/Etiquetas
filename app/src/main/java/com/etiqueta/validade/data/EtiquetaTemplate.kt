package com.etiqueta.validade.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Template de etiqueta personalizada salvo pelo usuário.
 * Pode ser do tipo VALIDADE (vinculado a produto) ou LIVRE (texto/número livre).
 */
@Entity(tableName = "etiqueta_templates")
data class EtiquetaTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,                      // nome do template
    val tipo: String = TIPO_VALIDADE,      // "VALIDADE" ou "LIVRE"

    // Layout
    val larguraMm: Int = 50,
    val alturaMm: Int = 30,
    val mostrarLogo: Boolean = false,
    val logoPath: String = "",             // caminho do arquivo de logo no armazenamento interno

    // Campos de validade
    val mostrarNomeProduto: Boolean = true,
    val mostrarDataProducao: Boolean = true,
    val mostrarDataValidade: Boolean = true,
    val mostrarDiasValidade: Boolean = false,
    val mostrarArmazenamento: Boolean = true,
    val labelProducao: String = "Produção:",
    val labelValidade: String = "Validade:",

    // Etiqueta livre — linhas de texto customizadas (JSON)
    val linhasLivresJson: String = "[]",

    // Estilo
    val nomeLoja: String = "",
    val tamanhoFonteNome: Int = 28,
    val tamanhoFonteDatas: Int = 22
) {
    companion object {
        const val TIPO_VALIDADE = "VALIDADE"
        const val TIPO_LIVRE = "LIVRE"
    }
}
