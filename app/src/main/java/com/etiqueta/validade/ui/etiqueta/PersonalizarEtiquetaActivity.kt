package com.etiqueta.validade.ui.etiqueta

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.etiqueta.validade.data.EtiquetaTemplate
import com.etiqueta.validade.databinding.ActivityPersonalizarEtiquetaBinding
import com.etiqueta.validade.ui.MainViewModel
import java.io.File
import java.io.FileOutputStream

class PersonalizarEtiquetaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalizarEtiquetaBinding
    private val viewModel: MainViewModel by viewModels()
    private var templateId: Long = 0L
    private var tipoTemplate: String = EtiquetaTemplate.TIPO_VALIDADE
    private var logoPath: String = ""
    private var mostrarLogo: Boolean = false

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> salvarLogoLocalmente(uri) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalizarEtiquetaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Personalizar Etiqueta"

        templateId = intent.getLongExtra("template_id", 0L)
        tipoTemplate = intent.getStringExtra("tipo") ?: EtiquetaTemplate.TIPO_VALIDADE

        setupVisibilidade()
        carregarTemplate()
        setupBotoes()
    }

    private fun setupVisibilidade() {
        val isValidade = tipoTemplate == EtiquetaTemplate.TIPO_VALIDADE
        binding.layoutCamposValidade.visibility = if (isValidade) View.VISIBLE else View.GONE
    }

    private fun carregarTemplate() {
        if (templateId == 0L) return
        viewModel.templates.observe(this) { lista ->
            lista.find { it.id == templateId }?.let { t ->
                binding.etNomeTemplate.setText(t.nome)
                binding.etNomeLoja.setText(t.nomeLoja)
                binding.etLargura.setText(t.larguraMm.toString())
                binding.etAltura.setText(t.alturaMm.toString())
                binding.etLabelProducao.setText(t.labelProducao)
                binding.etLabelValidade.setText(t.labelValidade)
                binding.switchNomeProduto.isChecked = t.mostrarNomeProduto
                binding.switchDataProducao.isChecked = t.mostrarDataProducao
                binding.switchDataValidade.isChecked = t.mostrarDataValidade
                binding.switchDiasValidade.isChecked = t.mostrarDiasValidade
                binding.switchLogo.isChecked = t.mostrarLogo
                logoPath = t.logoPath
                mostrarLogo = t.mostrarLogo
                atualizarLogoUI()
            }
        }
    }

    private fun setupBotoes() {
        binding.switchLogo.setOnCheckedChangeListener { _, checked ->
            mostrarLogo = checked
            binding.layoutLogo.visibility = if (checked) View.VISIBLE else View.GONE
        }

        binding.btnEscolherLogo.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        binding.btnRemoverLogo.setOnClickListener {
            logoPath = ""
            binding.ivLogoPrevia.setImageDrawable(null)
            binding.tvLogoSelecionada.text = "Nenhuma logo selecionada"
        }

        binding.btnSalvar.setOnClickListener { salvarTemplate() }
    }

    private fun salvarLogoLocalmente(uri: Uri) {
        try {
            val dir = File(filesDir, "logos").also { it.mkdirs() }
            val file = File(dir, "logo_${System.currentTimeMillis()}.png")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            logoPath = file.absolutePath
            binding.ivLogoPrevia.setImageURI(Uri.fromFile(file))
            binding.tvLogoSelecionada.text = "Logo: ${file.name}"
            Toast.makeText(this, "Logo carregada!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao carregar logo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun atualizarLogoUI() {
        binding.layoutLogo.visibility = if (mostrarLogo) View.VISIBLE else View.GONE
        if (logoPath.isNotBlank()) {
            val file = File(logoPath)
            if (file.exists()) {
                binding.ivLogoPrevia.setImageURI(Uri.fromFile(file))
                binding.tvLogoSelecionada.text = "Logo: ${file.name}"
            }
        }
    }

    private fun salvarTemplate() {
        val nome = binding.etNomeTemplate.text.toString().ifBlank { "Meu Template" }
        val largura = binding.etLargura.text.toString().toIntOrNull() ?: 50
        val altura = binding.etAltura.text.toString().toIntOrNull() ?: 30

        val template = EtiquetaTemplate(
            id = templateId,
            nome = nome,
            tipo = tipoTemplate,
            larguraMm = largura,
            alturaMm = altura,
            nomeLoja = binding.etNomeLoja.text.toString(),
            mostrarLogo = mostrarLogo,
            logoPath = logoPath,
            mostrarNomeProduto = binding.switchNomeProduto.isChecked,
            mostrarDataProducao = binding.switchDataProducao.isChecked,
            mostrarDataValidade = binding.switchDataValidade.isChecked,
            mostrarDiasValidade = binding.switchDiasValidade.isChecked,
            labelProducao = binding.etLabelProducao.text.toString().ifBlank { "Produção:" },
            labelValidade = binding.etLabelValidade.text.toString().ifBlank { "Validade:" }
        )
        viewModel.salvarTemplate(template)
        Toast.makeText(this, "Template salvo!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { onBackPressedDispatcher.onBackPressed(); return true }
}
