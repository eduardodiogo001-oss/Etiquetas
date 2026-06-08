package com.etiqueta.validade.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {

    @Query("SELECT * FROM produtos ORDER BY nome ASC")
    fun getAll(): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE nome LIKE '%' || :query || '%' ORDER BY nome ASC")
    fun buscar(query: String): Flow<List<Produto>>

    @Query("SELECT * FROM produtos WHERE id = :id")
    suspend fun getById(id: Long): Produto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(produto: Produto): Long

    @Update
    suspend fun atualizar(produto: Produto)

    @Delete
    suspend fun deletar(produto: Produto)
}
