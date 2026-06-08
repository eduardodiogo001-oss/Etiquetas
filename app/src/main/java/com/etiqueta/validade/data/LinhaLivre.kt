package com.etiqueta.validade.data

/**
 * Representa uma linha de texto/número em uma etiqueta livre.
 * Serializada como JSON dentro de EtiquetaTemplate.linhasLivresJson
 */
data class LinhaLivre(
    val id: String = java.util.UUID.randomUUID().toString(),
    val texto: String = "",
    val tamanhoFonte: Int = 22,
    val negrito: Boolean = false,
    val centralizado: Boolean = false
)
