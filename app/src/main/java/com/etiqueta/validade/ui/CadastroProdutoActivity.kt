package com.etiqueta.validade.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etiqueta.validade.R
import com.etiqueta.validade.data.Produto
import com.etiqueta.validade.databinding.ActivityCadastroProdutoBinding

class CadastroProdutoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroProdutoBinding
    private val viewModel: MainViewModel by viewModels()
    private var produtoId: Long = 0L

    private val opcoesArmazenamento = listOf(
        "Não especificado",
        "Resfriado (2°C a 8°C)",
        "Congelado (-18°C ou abaixo)",
        "Temperatura ambiente",
        "Refrigerado após aberto",
        "Local fresco e seco",
        "Ao abrigo da luz e calor",
        "Refrigerado e ao abrigo da luz"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroProdutoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupSpinnerArmazenamento()

        produtoId = intent.getLongExtra("produto_id", 0L)
        if (produtoId != 0L) {
            supportActionBar?.title = "Editar Produto"
            binding.etNome.setText(intent.getStringExtra("produto_nome"))
            binding.etDias.setText(intent.getIntExtra("produto_dias", 0).toString())
            binding.etCategoria.setText(intent.getStringExtra("produto_categoria"))
            binding.etObservacao.setText(intent.getStringExtra("produto_obs"))
            val armazenamento = intent.getStringExtra("produto_armazenamento") ?: ""
            val idx = opcoesArmazenamento.indexOf(armazenamento).takeIf { it >= 0 } ?: 0
            binding.spinnerArmazenamento.setSelection(idx)
        } else {
            supportActionBar?.title = "Novo Produto"
        }

        binding.btnSalvar.setOnClickListener { salvar() }
    }

    private fun setupSpinnerArmazenamento() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcoesArmazenamento)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerArmazenamento.adapter = adapter
    }

    private fun salvar() {
        val nome = binding.etNome.text.toString().trim()
        val diasStr = binding.etDias.text.toString().trim()
        val categoria = binding.etCategoria.text.toString().trim()
        val obs = binding.etObservacao.text.toString().trim()
        val armazenamento = opcoesArmazenamento[binding.spinnerArmazenamento.selectedItemPosition]
            .let { if (it == "Não especificado") "" else it }

        if (nome.isBlank()) {
            binding.etNome.error = "Informe o nome do produto"
            return
        }
        val dias = diasStr.toIntOrNull()
        if (dias == null || dias <= 0) {
            binding.etDias.error = "Informe um número de dias válido"
            return
        }

        val produto = Produto(
            id = produtoId,
            nome = nome,
            diasValidade = dias,
            categoria = categoria,
            observacao = obs,
            formaArmazenamento = armazenamento
        )
        viewModel.salvarProduto(produto)
        Toast.makeText(this, "Produto salvo!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
