package com.etiqueta.validade.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.etiqueta.validade.data.*
import com.etiqueta.validade.print.ElginPrintManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val produtoRepo = ProdutoRepository(db.produtoDao())
    private val templateDao = db.etiquetaTemplateDao()

    private val _searchQuery = MutableStateFlow("")
    val produtos: LiveData<List<Produto>> = _searchQuery
        .flatMapLatest { q -> if (q.isBlank()) produtoRepo.getAll() else produtoRepo.buscar(q) }
        .asLiveData()

    val templates: LiveData<List<EtiquetaTemplate>> = templateDao.getAll().asLiveData()
    val templatesValidade = templateDao.getByTipo(EtiquetaTemplate.TIPO_VALIDADE).asLiveData()
    val templatesLivre = templateDao.getByTipo(EtiquetaTemplate.TIPO_LIVRE).asLiveData()

    private val _printState = MutableLiveData<PrintState>(PrintState.Idle)
    val printState: LiveData<PrintState> = _printState

    fun buscar(q: String) { _searchQuery.value = q }

    fun calcularValidade(produto: Produto, dataProducao: Date = Date()): Date {
        val cal = Calendar.getInstance().apply { time = dataProducao; add(Calendar.DAY_OF_YEAR, produto.diasValidade) }
        return cal.time
    }

    fun imprimirValidade(context: Context, produto: Produto, dataProducao: Date, copias: Int, template: EtiquetaTemplate) {
        val config = ElginPrintManager.loadConfig(context.getSharedPreferences("config", Context.MODE_PRIVATE))
        val validade = calcularValidade(produto, dataProducao)
        viewModelScope.launch(Dispatchers.IO) {
            _printState.postValue(PrintState.Printing)
            try {
                ElginPrintManager.imprimirValidade(context, config, produto.nome, dataProducao, validade, copias, template)
                _printState.postValue(PrintState.Success("$copias etiqueta(s) enviadas!"))
            } catch (e: Exception) { _printState.postValue(PrintState.Error(e.message ?: "Erro")) }
        }
    }

    fun imprimirLivre(context: Context, template: EtiquetaTemplate, linhas: List<LinhaLivre>, copias: Int) {
        val config = ElginPrintManager.loadConfig(context.getSharedPreferences("config", Context.MODE_PRIVATE))
        viewModelScope.launch(Dispatchers.IO) {
            _printState.postValue(PrintState.Printing)
            try {
                ElginPrintManager.imprimirLivre(context, config, template, copias, linhas)
                _printState.postValue(PrintState.Success("$copias etiqueta(s) enviadas!"))
            } catch (e: Exception) { _printState.postValue(PrintState.Error(e.message ?: "Erro")) }
        }
    }

    fun resetPrintState() { _printState.value = PrintState.Idle }
    fun salvarProduto(p: Produto) = viewModelScope.launch(Dispatchers.IO) { if (p.id == 0L) produtoRepo.inserir(p) else produtoRepo.atualizar(p) }
    fun deletarProduto(p: Produto) = viewModelScope.launch(Dispatchers.IO) { produtoRepo.deletar(p) }
    fun salvarTemplate(t: EtiquetaTemplate) = viewModelScope.launch(Dispatchers.IO) { if (t.id == 0L) templateDao.inserir(t) else templateDao.atualizar(t) }
    fun deletarTemplate(t: EtiquetaTemplate) = viewModelScope.launch(Dispatchers.IO) { templateDao.deletar(t) }

    sealed class PrintState {
        object Idle : PrintState()
        object Printing : PrintState()
        data class Success(val msg: String) : PrintState()
        data class Error(val msg: String) : PrintState()
    }
}
