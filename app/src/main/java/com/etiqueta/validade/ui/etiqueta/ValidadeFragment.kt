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

        view.findViewById<TextView>(R.id.tvDialogProduto).text = produto.nome
        view.findViewById<TextView>(R.id.tvDialogAbertura).text = "Produção: ${fmt.format(dataProducao)}"
        view.findViewById<TextView>(R.id.tvDialogValidade).text = "Validade: ${fmt.format(validade)}"
        view.findViewById<TextView>(R.id.tvDialogDias).text = "${produto.diasValidade} dias"

        val np = view.findViewById<NumberPicker>(R.id.npCopias).apply { minValue = 1; maxValue = 50; value = 1 }

        // Spinner de template
        val spinner = view.findViewById<Spinner>(R.id.spinnerTemplate)
        val nomes = templates.map { it.nome }
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, nomes)

        AlertDialog.Builder(requireContext())
            .setTitle("Imprimir Etiqueta de Validade")
            .setView(view)
            .setPositiveButton("🖨 Imprimir") { _, _ ->
                val template = templates[spinner.selectedItemPosition]
                viewModel.imprimirValidade(requireContext(), produto, dataProducao, np.value, template)
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
