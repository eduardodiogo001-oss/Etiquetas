package com.etiqueta.validade.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EtiquetaTemplateDao {

    @Query("SELECT * FROM etiqueta_templates ORDER BY nome ASC")
    fun getAll(): Flow<List<EtiquetaTemplate>>

    @Query("SELECT * FROM etiqueta_templates WHERE tipo = :tipo ORDER BY nome ASC")
    fun getByTipo(tipo: String): Flow<List<EtiquetaTemplate>>

    @Query("SELECT * FROM etiqueta_templates WHERE id = :id")
    suspend fun getById(id: Long): EtiquetaTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(template: EtiquetaTemplate): Long

    @Update
    suspend fun atualizar(template: EtiquetaTemplate)

    @Delete
    suspend fun deletar(template: EtiquetaTemplate)
}
