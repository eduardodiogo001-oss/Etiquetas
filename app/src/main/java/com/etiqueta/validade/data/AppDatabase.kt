package com.etiqueta.validade.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Produto::class, EtiquetaTemplate::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao
    abstract fun etiquetaTemplateDao(): EtiquetaTemplateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "etiqueta_validade.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(PrepopulateCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PrepopulateCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = database.produtoDao()
                    val produtos = listOf(
                        Produto(nome = "Ketchup",            diasValidade = 7,  categoria = "Condimentos"),
                        Produto(nome = "Maionese",           diasValidade = 5,  categoria = "Condimentos"),
                        Produto(nome = "Mostarda",           diasValidade = 7,  categoria = "Condimentos"),
                        Produto(nome = "Molho de pimenta",  diasValidade = 7,  categoria = "Condimentos"),
                        Produto(nome = "Frango cozido",     diasValidade = 3,  categoria = "Proteínas"),
                        Produto(nome = "Carne moída",       diasValidade = 2,  categoria = "Proteínas"),
                        Produto(nome = "Ovo cozido",        diasValidade = 7,  categoria = "Proteínas"),
                        Produto(nome = "Arroz cozido",      diasValidade = 3,  categoria = "Grãos"),
                        Produto(nome = "Feijão cozido",     diasValidade = 3,  categoria = "Grãos"),
                        Produto(nome = "Macarrão cozido",   diasValidade = 3,  categoria = "Grãos"),
                        Produto(nome = "Alface",            diasValidade = 5,  categoria = "Vegetais"),
                        Produto(nome = "Tomate fatiado",    diasValidade = 3,  categoria = "Vegetais"),
                        Produto(nome = "Cebola fatiada",    diasValidade = 3,  categoria = "Vegetais"),
                        Produto(nome = "Queijo fatiado",    diasValidade = 5,  categoria = "Laticínios"),
                        Produto(nome = "Requeijão aberto",  diasValidade = 15, categoria = "Laticínios"),
                        Produto(nome = "Leite aberto",      diasValidade = 3,  categoria = "Laticínios"),
                        Produto(nome = "Iogurte aberto",    diasValidade = 5,  categoria = "Laticínios"),
                        Produto(nome = "Molho branco",      diasValidade = 3,  categoria = "Molhos"),
                        Produto(nome = "Molho vermelho",    diasValidade = 5,  categoria = "Molhos"),
                        Produto(nome = "Suco natural",      diasValidade = 2,  categoria = "Bebidas"),
                        Produto(nome = "Chantilly",         diasValidade = 3,  categoria = "Confeitaria"),
                        Produto(nome = "Brigadeiro",        diasValidade = 5,  categoria = "Confeitaria"),
                        Produto(nome = "Batata cozida",     diasValidade = 3,  categoria = "Vegetais"),
                        Produto(nome = "Cenoura ralada",    diasValidade = 4,  categoria = "Vegetais"),
                        Produto(nome = "Bolo simples",      diasValidade = 4,  categoria = "Confeitaria"),
                    )
                    produtos.forEach { dao.inserir(it) }

                    // Template padrão
                    val tDao = database.etiquetaTemplateDao()
                    tDao.inserir(EtiquetaTemplate(
                        nome = "Padrão Validade",
                        tipo = EtiquetaTemplate.TIPO_VALIDADE,
                        larguraMm = 50, alturaMm = 30,
                        mostrarLogo = false,
                        mostrarDataProducao = true,
                        mostrarDataValidade = true
                    ))
                }
            }
        }
    }
}
