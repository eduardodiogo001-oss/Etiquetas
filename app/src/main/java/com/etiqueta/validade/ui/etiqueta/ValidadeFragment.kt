package com.etiqueta.validade.ui.etiqueta

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.etiqueta.validade.R
import com.etiqueta.validade.data.EtiquetaTemplate
import com.etiqueta.validade.data.Produto
import com.etiqueta.validade.databinding.FragmentValidadeBinding
import com.etiqueta.validade.ui.CadastroProdutoActivity
import com.etiqueta.validade.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class ValidadeFragment : Fragment() {

    private var _binding: FragmentValidadeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: com.etiqueta.validade.ui.ProdutoAdapter
    private val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private var dataProducao = Date()
    private var templatesValidade: List<EtiquetaTemplate> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentValidadeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupSearch()
        setupProducaoDate()
        observeViewModel()

        binding.fabAddProduto.setOnClickListener {
            startActivity(Intent(requireContext(), CadastroProdutoActivity::class.java))
        }
    }

    private fun setupRecycler() {
        adapter = com.etiqueta.validade.ui.ProdutoAdapter(
            onItemClick = { mostrarDialogImprimir(it) },
            onItemLongClick = { mostrarMenuEdicao(it) }
        )
        binding.recyclerProdutos.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.recyclerProdutos.adapter = adapter
    }

    private fun setupSearch() {
        binding.etBusca.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = viewModel.buscar(s?.toString() ?: "")
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupProducaoDate() {
        binding.tvDataProducao.text = "Produção: ${fmt.format(dataProducao)}"
        binding.tvDataProducao.setOnClickListener { showDatePicker() }
        binding.btnAlterarData.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { time = dataProducao }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            dataProducao = cal.time
            binding.tvDataProducao.text = "Produção: ${fmt.format(dataProducao)}"
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun observeViewModel() {
        viewModel.produtos.observe(viewLifecycleOwner) { lista ->
            adapter.submitList(lista)
            binding.tvSemProdutos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.templatesValidade.observe(viewLifecycleOwner) { lista ->
            templatesValidade = lista
        }

        viewModel.printState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MainViewModel.PrintState.Idle -> binding.progressBar.visibility = View.GONE
                is MainViewModel.PrintState.Printing -> binding.progressBar.visibility = View.VISIBLE
                is MainViewModel.PrintState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.msg, Snackbar.LENGTH_LONG).show()
                    viewModel.resetPrintState()
                }
                is MainViewModel.PrintState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    AlertDialog.Builder(requireContext())
                        .setTitle("Erro na impressão")
                        .setMessage(state.msg)
                        .setPositiveButton("OK") { _, _ -> viewModel.resetPrintState() }
                        .show()
                }
            }
        }
    }

    private fun mostrarDialogImprimir(produto: Produto) {
        val templates = templatesValidade
            .takeIf { it.isNotEmpty() }
            ?: listOf(EtiquetaTemplate(nome = "Padrão", tipo = EtiquetaTemplate.TIPO_VALIDADE))

        val validade = viewModel.calcularValidade(produto, dataProducao)
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_imprimir, null)

        val tvNomeLoja      = view.findViewById<TextView>(R.id.tvDialogNomeLoja)
        val dividerLoja     = view.findViewById<View>(R.id.dividerLoja)
        val tvProduto       = view.findViewById<TextView>(R.id.tvDialogProduto)
        val tvArmazenamento = view.findViewById<TextView>(R.id.tvDialogArmazenamento)
        val tvAbertura      = view.findViewById<TextView>(R.id.tvDialogAbertura)
        val tvValidade      = view.findViewById<TextView>(R.id.tvDialogValidade)
        val tvDias          = view.findViewById<TextView>(R.id.tvDialogDias)
        val ivLogo          = view.findViewById<android.widget.ImageView>(R.id.ivDialogLogo)
        val tvDimensoes     = view.findViewById<TextView>(R.id.tvDialogDimensoes)

        tvProduto.text  = produto.nome
        tvAbertura.text = "Produção: ${fmt.format(dataProducao)}"
        tvValidade.text = "Validade: ${fmt.format(validade)}"
        tvDias.text     = "${produto.diasValidade} dias"
        if (produto.formaArmazenamento.isNotBlank()) {
            tvArmazenamento.text = produto.formaArmazenamento
        }

        fun aplicarTemplate(t: EtiquetaTemplate) {
            // Nome da loja
            if (t.nomeLoja.isNotBlank()) {
                tvNomeLoja.text = t.nomeLoja
                tvNomeLoja.visibility = View.VISIBLE
                dividerLoja.visibility = View.VISIBLE
            } else {
                tvNomeLoja.visibility = View.GONE
                dividerLoja.visibility = View.GONE
            }

            // Logo
            if (t.mostrarLogo && t.logoPath.isNotBlank()) {
                val file = java.io.File(t.logoPath)
                if (file.exists()) {
                    ivLogo.setImageURI(android.net.Uri.fromFile(file))
                    ivLogo.visibility = View.VISIBLE
                } else {
                    ivLogo.visibility = View.GONE
                }
            } else {
                ivLogo.visibility = View.GONE
            }

            // Campos de texto
            tvProduto.visibility       = if (t.mostrarNomeProduto)  View.VISIBLE else View.GONE
            tvArmazenamento.visibility = if (t.mostrarArmazenamento && produto.formaArmazenamento.isNotBlank()) View.VISIBLE else View.GONE
            tvAbertura.visibility      = if (t.mostrarDataProducao) View.VISIBLE else View.GONE
            tvValidade.visibility      = if (t.mostrarDataValidade) View.VISIBLE else View.GONE
            tvDias.visibility          = if (t.mostrarDiasValidade) View.VISIBLE else View.GONE

            // Dimensões
            tvDimensoes.text = "${t.larguraMm} × ${t.alturaMm} mm"
        }

        val np = view.findViewById<NumberPicker>(R.id.npCopias).apply { minValue = 1; maxValue = 50; value = 1 }

        val spinner = view.findViewById<Spinner>(R.id.spinnerTemplate)
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, templates.map { it.nome })
        aplicarTemplate(templates[0])
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                aplicarTemplate(templates[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val rgFormato = view.findViewById<android.widget.RadioGroup>(R.id.rgFormato)

        AlertDialog.Builder(requireContext())
            .setTitle("Imprimir Etiqueta de Validade")
            .setView(view)
            .setPositiveButton("🖨 Imprimir") { _, _ ->
                val template = templates[spinner.selectedItemPosition]
                val formato = if (rgFormato.checkedRadioButtonId == R.id.rbFormatoCupom)
                    com.etiqueta.validade.print.ElginPrintManager.PrinterType.ESCPOS
                else
                    com.etiqueta.validade.print.ElginPrintManager.PrinterType.ZPL
                viewModel.imprimirValidade(requireContext(), produto, dataProducao, np.value, template, formato)
            }
            .setNeutralButton("✏️ Personalizar") { _, _ ->
                val intent = Intent(requireContext(), PersonalizarEtiquetaActivity::class.java)
                templates.firstOrNull()?.let { intent.putExtra("template_id", it.id) }
                startActivity(intent)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarMenuEdicao(produto: Produto) {
        AlertDialog.Builder(requireContext())
            .setTitle(produto.nome)
            .setItems(arrayOf("✏️ Editar", "🗑 Excluir")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(requireContext(), CadastroProdutoActivity::class.java).apply {
                        putExtra("produto_id", produto.id)
                        putExtra("produto_nome", produto.nome)
                        putExtra("produto_dias", produto.diasValidade)
                        putExtra("produto_categoria", produto.categoria)
                        putExtra("produto_obs", produto.observacao)
                        putExtra("produto_armazenamento", produto.formaArmazenamento)
                    })
                    1 -> AlertDialog.Builder(requireContext())
                        .setTitle("Excluir \"${produto.nome}\"?")
                        .setPositiveButton("Excluir") { _, _ -> viewModel.deletarProduto(produto) }
                        .setNegativeButton("Cancelar", null).show()
                }
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
