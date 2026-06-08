package com.etiqueta.validade.ui.etiqueta

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.etiqueta.validade.R
import com.etiqueta.validade.data.EtiquetaTemplate
import com.etiqueta.validade.data.LinhaLivre
import com.etiqueta.validade.databinding.FragmentEtiquetaLivreBinding
import com.etiqueta.validade.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject

class EtiquetaLivreFragment : Fragment() {

    private var _binding: FragmentEtiquetaLivreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var linhasAdapter: LinhaLivreAdapter
    private val linhas = mutableListOf<LinhaLivre>()
    private var templateAtual: EtiquetaTemplate? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEtiquetaLivreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLinhasRecycler()
        setupButtons()
        observeViewModel()
    }

    private fun setupLinhasRecycler() {
        linhasAdapter = LinhaLivreAdapter(
            linhas = linhas,
            onDelete = { pos -> linhas.removeAt(pos); linhasAdapter.notifyItemRemoved(pos); updatePrevia() },
            onEdit = { pos -> mostrarDialogEditarLinha(pos) }
        )
        binding.recyclerLinhas.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLinhas.adapter = linhasAdapter
    }

    private fun setupButtons() {
        binding.btnAdicionarLinha.setOnClickListener { mostrarDialogNovaLinha() }
        binding.btnImprimir.setOnClickListener { imprimirLivre() }
        binding.btnSalvarTemplate.setOnClickListener { salvarComoTemplate() }
        binding.btnCarregarTemplate.setOnClickListener { carregarTemplate() }
        binding.btnPersonalizar.setOnClickListener {
            startActivity(Intent(requireContext(), PersonalizarEtiquetaActivity::class.java).apply {
                putExtra("tipo", EtiquetaTemplate.TIPO_LIVRE)
                templateAtual?.let { putExtra("template_id", it.id) }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.printState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MainViewModel.PrintState.Printing -> binding.progressBar.visibility = View.VISIBLE
                is MainViewModel.PrintState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Snackbar.make(binding.root, state.msg, Snackbar.LENGTH_LONG).show()
                    viewModel.resetPrintState()
                }
                is MainViewModel.PrintState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    AlertDialog.Builder(requireContext()).setTitle("Erro").setMessage(state.msg)
                        .setPositiveButton("OK") { _, _ -> viewModel.resetPrintState() }.show()
                }
                else -> binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun mostrarDialogNovaLinha() = mostrarDialogLinha(-1, LinhaLivre())

    private fun mostrarDialogEditarLinha(pos: Int) = mostrarDialogLinha(pos, linhas[pos])

    private fun mostrarDialogLinha(pos: Int, linha: LinhaLivre) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_editar_linha, null)
        val etTexto = view.findViewById<EditText>(R.id.etTextoLinha)
        val sbFonte = view.findViewById<SeekBar>(R.id.sbFonte)
        val tvFonte = view.findViewById<TextView>(R.id.tvFonteVal)
        val cbNegrito = view.findViewById<CheckBox>(R.id.cbNegrito)
        val cbCentral = view.findViewById<CheckBox>(R.id.cbCentralizado)

        etTexto.setText(linha.texto)
        sbFonte.max = 40; sbFonte.progress = linha.tamanhoFonte - 14
        tvFonte.text = "${linha.tamanhoFonte}pt"
        cbNegrito.isChecked = linha.negrito
        cbCentral.isChecked = linha.centralizado

        sbFonte.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) { tvFonte.text = "${p + 14}pt" }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        AlertDialog.Builder(requireContext())
            .setTitle(if (pos < 0) "Nova linha" else "Editar linha")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                val nova = LinhaLivre(
                    id = linha.id,
                    texto = etTexto.text.toString(),
                    tamanhoFonte = sbFonte.progress + 14,
                    negrito = cbNegrito.isChecked,
                    centralizado = cbCentral.isChecked
                )
                if (pos < 0) { linhas.add(nova); linhasAdapter.notifyItemInserted(linhas.size - 1) }
                else { linhas[pos] = nova; linhasAdapter.notifyItemChanged(pos) }
                updatePrevia()
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun updatePrevia() {
        binding.tvPrevia.text = linhas.joinToString("\n") { l ->
            val b = if (l.negrito) "**" else ""
            val c = if (l.centralizado) "  " else ""
            "$c$b${l.texto}$b"
        }.ifEmpty { "(Nenhuma linha adicionada)" }
    }

    private fun imprimirLivre() {
        if (linhas.isEmpty()) { Toast.makeText(requireContext(), "Adicione pelo menos uma linha", Toast.LENGTH_SHORT).show(); return }
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_copias, null)
        val np = view.findViewById<NumberPicker>(R.id.npCopias).apply { minValue = 1; maxValue = 50; value = 1 }
        AlertDialog.Builder(requireContext())
            .setTitle("Quantidade de cópias")
            .setView(view)
            .setPositiveButton("🖨 Imprimir") { _, _ ->
                val t = templateAtual ?: EtiquetaTemplate(nome = "Livre", tipo = EtiquetaTemplate.TIPO_LIVRE)
                viewModel.imprimirLivre(requireContext(), t, linhas.toList(), np.value)
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun salvarComoTemplate() {
        val et = EditText(requireContext()).apply { hint = "Nome do template" }
        AlertDialog.Builder(requireContext())
            .setTitle("Salvar template")
            .setView(et)
            .setPositiveButton("Salvar") { _, _ ->
                val json = JSONArray().apply { linhas.forEach { l ->
                    put(JSONObject().apply {
                        put("id", l.id); put("texto", l.texto)
                        put("tamanhoFonte", l.tamanhoFonte)
                        put("negrito", l.negrito); put("centralizado", l.centralizado)
                    })
                }}.toString()
                val t = EtiquetaTemplate(
                    nome = et.text.toString().ifBlank { "Template livre" },
                    tipo = EtiquetaTemplate.TIPO_LIVRE,
                    linhasLivresJson = json
                )
                viewModel.salvarTemplate(t)
                Toast.makeText(requireContext(), "Template salvo!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun carregarTemplate() {
        val lista = viewModel.templatesLivre.value ?: emptyList()
        if (lista.isEmpty()) { Toast.makeText(requireContext(), "Nenhum template salvo", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(requireContext())
            .setTitle("Carregar template")
            .setItems(lista.map { it.nome }.toTypedArray()) { _, i ->
                val t = lista[i]
                templateAtual = t
                linhas.clear()
                try {
                    val arr = JSONArray(t.linhasLivresJson)
                    for (j in 0 until arr.length()) {
                        val o = arr.getJSONObject(j)
                        linhas.add(LinhaLivre(o.getString("id"), o.getString("texto"),
                            o.getInt("tamanhoFonte"), o.getBoolean("negrito"), o.getBoolean("centralizado")))
                    }
                } catch (_: Exception) {}
                linhasAdapter.notifyDataSetChanged()
                updatePrevia()
            }.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
