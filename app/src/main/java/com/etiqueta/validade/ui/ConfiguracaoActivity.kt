package com.etiqueta.validade.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.etiqueta.validade.R
import com.etiqueta.validade.databinding.ActivityConfiguracaoBinding
import com.etiqueta.validade.print.ElginPrintManager

class ConfiguracaoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfiguracaoBinding
    private val prefs by lazy { getSharedPreferences("config", MODE_PRIVATE) }
    private var tipoConexao = ElginPrintManager.ConnectionType.TCP_IP
    private var tipoImpressora = ElginPrintManager.PrinterType.ZPL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfiguracaoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Configurações da Impressora"

        carregarConfig()
        setupTipoImpressora()
        setupTipoConexao()

        binding.btnSalvarConfig.setOnClickListener { salvarConfig() }
        binding.btnScanBluetooth.setOnClickListener { buscarDispBluetooth() }
    }

    private fun setupTipoImpressora() {
        binding.rgTipoImpressora.setOnCheckedChangeListener { _, id ->
            tipoImpressora = if (id == R.id.rbEscPos) ElginPrintManager.PrinterType.ESCPOS
                             else ElginPrintManager.PrinterType.ZPL
            binding.layoutPapel.visibility =
                if (tipoImpressora == ElginPrintManager.PrinterType.ESCPOS) View.VISIBLE else View.GONE
        }
    }

    private fun setupTipoConexao() {
        binding.rgConexao.setOnCheckedChangeListener { _, id ->
            tipoConexao = when (id) {
                R.id.rbTcpIp      -> ElginPrintManager.ConnectionType.TCP_IP
                R.id.rbBluetooth  -> ElginPrintManager.ConnectionType.BLUETOOTH
                R.id.rbUsb        -> ElginPrintManager.ConnectionType.USB
                else              -> ElginPrintManager.ConnectionType.TCP_IP
            }
            atualizarVisibilidade()
        }
    }

    private fun atualizarVisibilidade() {
        binding.layoutTcpIp.visibility =
            if (tipoConexao == ElginPrintManager.ConnectionType.TCP_IP) View.VISIBLE else View.GONE
        binding.layoutBluetooth.visibility =
            if (tipoConexao == ElginPrintManager.ConnectionType.BLUETOOTH) View.VISIBLE else View.GONE
        binding.tvUsbInfo.visibility =
            if (tipoConexao == ElginPrintManager.ConnectionType.USB) View.VISIBLE else View.GONE
    }

    private fun carregarConfig() {
        val config = ElginPrintManager.loadConfig(prefs)

        tipoConexao = config.connectionType
        when (config.connectionType) {
            ElginPrintManager.ConnectionType.TCP_IP    -> binding.rbTcpIp.isChecked = true
            ElginPrintManager.ConnectionType.BLUETOOTH -> binding.rbBluetooth.isChecked = true
            ElginPrintManager.ConnectionType.USB       -> binding.rbUsb.isChecked = true
        }

        tipoImpressora = config.printerType
        if (config.printerType == ElginPrintManager.PrinterType.ESCPOS) {
            binding.rbEscPos.isChecked = true
            binding.layoutPapel.visibility = View.VISIBLE
        } else {
            binding.rbZpl.isChecked = true
        }
        if (config.paperWidthMm == 58) binding.rb58mm.isChecked = true
        else binding.rb80mm.isChecked = true

        binding.etIp.setText(config.ip)
        binding.etPorta.setText(config.port.toString())
        binding.etBtAddress.setText(config.btAddress)

        atualizarVisibilidade()
    }

    private fun salvarConfig() {
        val ip        = binding.etIp.text.toString().trim()
        val port      = binding.etPorta.text.toString().toIntOrNull() ?: 9100
        val btAddress = binding.etBtAddress.text.toString().trim()
        val paperWidth = if (binding.rb58mm.isChecked) 58 else 80

        ElginPrintManager.saveConfig(prefs, tipoConexao, ip, port, btAddress, tipoImpressora, paperWidth)
        Toast.makeText(this, "Configurações salvas!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buscarDispBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 101)
                return
            }
        }

        val adapter = BluetoothAdapter.getDefaultAdapter()
        val paired: Set<BluetoothDevice>? = adapter?.bondedDevices

        if (paired.isNullOrEmpty()) {
            Toast.makeText(this, "Nenhum dispositivo Bluetooth pareado.", Toast.LENGTH_LONG).show()
            return
        }

        val nomes     = paired.map { "${it.name} (${it.address})" }.toTypedArray()
        val addresses = paired.map { it.address }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Selecione a impressora")
            .setItems(nomes) { _, i -> binding.etBtAddress.setText(addresses[i]) }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
