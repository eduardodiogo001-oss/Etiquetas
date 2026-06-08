package com.etiqueta.validade.data

import kotlinx.coroutines.flow.Flow

class ProdutoRepository(private val dao: ProdutoDao) {

    fun getAll(): Flow<List<Produto>> = dao.getAll()

    fun buscar(query: String): Flow<List<Produto>> = dao.buscar(query)

    suspend fun inserir(produto: Produto): Long = dao.inserir(produto)

    suspend fun atualizar(produto: Produto) = dao.atualizar(produto)

    suspend fun deletar(produto: Produto) = dao.deletar(produto)
}
