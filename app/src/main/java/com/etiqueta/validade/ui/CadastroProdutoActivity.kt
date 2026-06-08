package com.etiqueta.validade.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etiqueta.validade.data.Produto
import com.etiqueta.validade.databinding.ActivityCadastroProdutoBinding

class CadastroProdutoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCadastroProdutoBinding
    private val viewModel: MainViewModel by viewModels()
    private var produtoId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCadastroProdutoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Edição de produto existente
        produtoId = intent.getLongExtra("produto_id", 0L)
        if (produtoId != 0L) {
            supportActionBar?.title = "Editar Produto"
            binding.etNome.setText(intent.getStringExtra("produto_nome"))
            binding.etDias.setText(intent.getIntExtra("produto_dias", 0).toString())
            binding.etCategoria.setText(intent.getStringExtra("produto_categoria"))
            binding.etObservacao.setText(intent.getStringExtra("produto_obs"))
        } else {
            supportActionBar?.title = "Novo Produto"
        }

        binding.btnSalvar.setOnClickListener { salvar() }
    }

    private fun salvar() {
        val nome = binding.etNome.text.toString().trim()
        val diasStr = binding.etDias.text.toString().trim()
        val categoria = binding.etCategoria.text.toString().trim()
        val obs = binding.etObservacao.text.toString().trim()

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
            observacao = obs
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
