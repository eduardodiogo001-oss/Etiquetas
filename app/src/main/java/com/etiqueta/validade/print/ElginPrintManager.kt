package com.etiqueta.validade.print

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.etiqueta.validade.data.EtiquetaTemplate
import com.etiqueta.validade.data.LinhaLivre
import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

object ElginPrintManager {

    enum class ConnectionType { TCP_IP, BLUETOOTH, USB }
    enum class PrinterType { ZPL, ESCPOS }

    fun saveConfig(prefs: SharedPreferences, type: ConnectionType, ip: String = "",
                   port: Int = 9100, btAddress: String = "",
                   printerType: PrinterType = PrinterType.ZPL, paperWidthMm: Int = 80) {
        prefs.edit()
            .putString("conn_type", type.name)
            .putString("ip", ip).putInt("port", port)
            .putString("bt_address", btAddress)
            .putString("printer_type", printerType.name)
            .putInt("paper_width_mm", paperWidthMm)
            .apply()
    }

    fun loadConfig(prefs: SharedPreferences) = PrintConfig(
        connectionType = ConnectionType.valueOf(
            prefs.getString("conn_type", ConnectionType.TCP_IP.name) ?: ConnectionType.TCP_IP.name),
        ip = prefs.getString("ip", "192.168.1.100") ?: "192.168.1.100",
        port = prefs.getInt("port", 9100),
        btAddress = prefs.getString("bt_address", "") ?: "",
        printerType = PrinterType.valueOf(
            prefs.getString("printer_type", PrinterType.ZPL.name) ?: PrinterType.ZPL.name),
        paperWidthMm = prefs.getInt("paper_width_mm", 80)
    )

    data class PrintConfig(
        val connectionType: ConnectionType,
        val ip: String, val port: Int, val btAddress: String,
        val printerType: PrinterType = PrinterType.ZPL,
        val paperWidthMm: Int = 80
    )

    // ─── Impressão de etiqueta de VALIDADE ───────────────────────────────────

    fun imprimirValidade(
        context: Context, config: PrintConfig,
        nomeProduto: String, formaArmazenamento: String = "",
        dataProducao: Date, dataValidade: Date,
        copias: Int, template: EtiquetaTemplate
    ) {
        if (config.printerType == PrinterType.ESCPOS) {
            val bytes = buildEscPosValidade(config.paperWidthMm, nomeProduto, formaArmazenamento, dataProducao, dataValidade, copias, template)
            enviarBytes(context, config, bytes)
        } else {
            val zpl = buildZplValidade(context, template, nomeProduto, formaArmazenamento, dataProducao, dataValidade, copias)
            enviar(context, config, zpl)
        }
    }

    // ─── Impressão de etiqueta LIVRE ─────────────────────────────────────────

    fun imprimirLivre(
        context: Context, config: PrintConfig,
        template: EtiquetaTemplate, copias: Int,
        linhas: List<LinhaLivre>
    ) {
        val zpl = buildZplLivre(context, template, linhas, copias)
        enviar(context, config, zpl)
    }

    // ─── ZPL Validade ────────────────────────────────────────────────────────

    private fun buildZplValidade(
        context: Context, t: EtiquetaTemplate,
        nomeProduto: String, formaArmazenamento: String,
        producao: Date, validade: Date, copias: Int
    ): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val w = t.larguraMm * 8
        val h = t.alturaMm * 8
        val margin = 8
        return buildString {
            repeat(copias) {
                append("^XA\n^PW$w\n^LL$h\n^CI28\n")

                var y = margin

                // ── Nome da loja (topo, se configurado) ──────────────────────
                if (t.nomeLoja.isNotBlank()) {
                    append("^FO$margin,$y^FB${w - margin * 2},1,,C^A0N,18,18^FD${t.nomeLoja}^FS\n")
                    y += 22
                    append("^FO$margin,$y^GB${w - margin * 2},1,1^FS\n")
                    y += 5
                }

                // ── Nome do produto: centralizado, negrito, grande ────────────
                if (t.mostrarNomeProduto) {
                    val fs = t.tamanhoFonteNome
                    append("^FO$margin,$y^FB${w - margin * 2},1,,C^A0N,$fs,$fs^FD${nomeProduto.take(24)}^FS\n")
                    y += fs + 4
                    // linha de destaque abaixo do nome
                    append("^FO$margin,$y^GB${w - margin * 2},2,2^FS\n")
                    y += 6
                }

                // ── Forma de armazenamento ────────────────────────────────────
                if (t.mostrarArmazenamento && formaArmazenamento.isNotBlank()) {
                    val fd = t.tamanhoFonteDatas
                    append("^FO$margin,$y^A0N,$fd,$fd^FD$formaArmazenamento^FS\n")
                    y += fd + 4
                }

                // ── Data de produção ──────────────────────────────────────────
                if (t.mostrarDataProducao) {
                    val fd = t.tamanhoFonteDatas
                    append("^FO$margin,$y^A0N,$fd,$fd^FD${t.labelProducao} ${fmt.format(producao)}^FS\n")
                    y += fd + 4
                }

                // ── Dias de validade (opcional) ───────────────────────────────
                if (t.mostrarDiasValidade) {
                    val diff = ((validade.time - producao.time) / 86400000).toInt()
                    append("^FO$margin,$y^A0N,18,18^FDVálido por $diff dias^FS\n")
                    y += 26
                }

                // ── Data de validade: caixa dupla + centralizada negrito grande ─
                if (t.mostrarDataValidade) {
                    val fs = t.tamanhoFonteNome
                    val boxH = fs + 18          // altura total da caixa
                    val bx = margin             // x da caixa externa
                    val bw = w - margin * 2     // largura da caixa
                    // contorno único
                    append("^FO$bx,$y^GB$bw,$boxH,1^FS\n")
                    // texto centralizado dentro da caixa
                    val ty = y + (boxH - fs) / 2
                    append("^FO$bx,$ty^FB$bw,1,,C^A0N,$fs,$fs^FD${t.labelValidade} ${fmt.format(validade)}^FS\n")
                    y += boxH + 4
                }

                // ── Logo: canto inferior direito, sem fundo ───────────────────
                if (t.mostrarLogo && t.logoPath.isNotBlank()) {
                    val logoH = minOf(32, h - y - margin)
                    val logoW = logoH
                    if (logoH > 4) {
                        val logoZpl = logoToZpl(t.logoPath, logoH, logoW)
                        if (logoZpl != null) {
                            append("^FO${w - logoW - margin},${h - logoH - margin}$logoZpl\n")
                        }
                    }
                }

                append("^XZ\n")
            }
        }
    }

    // ─── ZPL Livre ───────────────────────────────────────────────────────────

    private fun buildZplLivre(
        context: Context, t: EtiquetaTemplate,
        linhas: List<LinhaLivre>, copias: Int
    ): String {
        val w = t.larguraMm * 8
        val h = t.alturaMm * 8
        return buildString {
            repeat(copias) {
                append("^XA\n^PW$w\n^LL$h\n^CI28\n")
                var y = 8

                // Logo
                if (t.mostrarLogo && t.logoPath.isNotBlank()) {
                    val logoZpl = logoToZpl(t.logoPath, h - 16, 40)
                    if (logoZpl != null) append("^FO${w - 48},8$logoZpl\n")
                }

                // Linhas de texto
                for (linha in linhas) {
                    val fs = linha.tamanhoFonte
                    val x = if (linha.centralizado) (w / 2) else 10
                    val just = if (linha.centralizado) "^JC" else ""
                    val bold = if (linha.negrito) "^MD3" else "^MD0"
                    append("$bold^FO$x,$y$just^A0N,$fs,$fs^FD${linha.texto.take(30)}^FS\n")
                    y += fs + 4
                    if (y > h - 10) return@repeat
                }

                append("^XZ\n")
            }
        }
    }

    // ─── Logo → ZPL GF (bitmap comprimido) ──────────────────────────────────

    private fun logoToZpl(path: String, maxH: Int, maxW: Int): String? {
        return try {
            val bmp = BitmapFactory.decodeFile(path) ?: return null
            val scaled = Bitmap.createScaledBitmap(bmp, maxW, maxH, true)
            val gray = scaled.copy(Bitmap.Config.ARGB_8888, false)

            val bytesPerRow = (maxW + 7) / 8
            val totalBytes = bytesPerRow * maxH
            val data = ByteArray(totalBytes)
            var idx = 0
            for (row in 0 until maxH) {
                for (col in 0 until bytesPerRow) {
                    var b = 0
                    for (bit in 0 until 8) {
                        val px = col * 8 + bit
                        if (px < maxW) {
                            val pixel = gray.getPixel(px, row)
                            val lum = (0.299 * ((pixel shr 16) and 0xFF) +
                                       0.587 * ((pixel shr 8) and 0xFF) +
                                       0.114 * (pixel and 0xFF)).toInt()
                            if (lum < 128) b = b or (0x80 shr bit)
                        }
                    }
                    data[idx++] = b.toByte()
                }
            }
            val hex = data.joinToString("") { "%02X".format(it) }
            "\n^GFA,$totalBytes,$totalBytes,$bytesPerRow,$hex"
        } catch (e: Exception) { null }
    }

    // ─── ESC/POS Builder ────────────────────────────────────────────────────

    private fun buildEscPosValidade(
        paperWidthMm: Int,
        nomeProduto: String, formaArmazenamento: String,
        producao: Date, validade: Date, copias: Int,
        t: EtiquetaTemplate
    ): ByteArray {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val cols = if (paperWidthMm >= 80) 48 else 32
        val sep = "-".repeat(cols)

        val ESC = 0x1B.toByte(); val GS = 0x1D.toByte(); val LF = 0x0A.toByte()
        fun cmd(vararg b: Int) = b.map { it.toByte() }.toByteArray()

        val init       = cmd(0x1B, 0x40)            // ESC @ initialize
        val center     = cmd(0x1B, 0x61, 0x01)      // ESC a 1 center
        val left       = cmd(0x1B, 0x61, 0x00)      // ESC a 0 left
        val boldOn     = cmd(0x1B, 0x45, 0x01)      // ESC E 1
        val boldOff    = cmd(0x1B, 0x45, 0x00)      // ESC E 0
        val dblSize    = cmd(0x1D, 0x21, 0x11)      // GS ! double w+h
        val normalSize = cmd(0x1D, 0x21, 0x00)      // GS ! normal
        val cut        = cmd(0x1D, 0x56, 0x42, 0x08) // GS V B partial cut

        val out = ByteArrayOutputStream()
        fun w(b: ByteArray) = out.write(b)
        fun w(s: String)    = out.write(s.toByteArray(Charsets.UTF_8))
        fun nl()            = out.write(byteArrayOf(LF))

        repeat(copias) {
            w(init)

            // Nome da loja
            if (t.nomeLoja.isNotBlank()) {
                w(center); w(boldOn)
                w(t.nomeLoja.take(cols)); nl()
                w(boldOff); w(left)
                w(sep); nl()
            }

            // Nome do produto — centralizado, duplo, negrito
            if (t.mostrarNomeProduto) {
                w(center); w(boldOn); w(dblSize)
                w(nomeProduto.take(cols / 2)); nl()
                w(normalSize); w(boldOff); w(left)
                w(sep); nl()
            }

            // Forma de armazenamento
            if (t.mostrarArmazenamento && formaArmazenamento.isNotBlank()) {
                w(left); w(formaArmazenamento.take(cols)); nl()
            }

            // Data de produção
            if (t.mostrarDataProducao) {
                w(left); w("${t.labelProducao} ${fmt.format(producao)}"); nl()
            }

            // Dias de validade
            if (t.mostrarDiasValidade) {
                val diff = ((validade.time - producao.time) / 86400000).toInt()
                w(left); w("Valido por $diff dias"); nl()
            }

            // Data de validade — centralizada, duplo, negrito, com moldura
            if (t.mostrarDataValidade) {
                val moldura = "*".repeat(cols)
                w(left); w(moldura); nl()
                w(center); w(boldOn); w(dblSize)
                w("${t.labelValidade} ${fmt.format(validade)}"); nl()
                w(normalSize); w(boldOff); w(left)
                w(moldura); nl()
            }

            nl(); nl(); nl()
            w(cut)
        }
        return out.toByteArray()
    }

    // ─── Envio ───────────────────────────────────────────────────────────────

    private fun enviarBytes(context: Context, config: PrintConfig, data: ByteArray) {
        when (config.connectionType) {
            ConnectionType.TCP_IP    -> Socket(config.ip, config.port).use { s ->
                s.soTimeout = 5000; s.getOutputStream().run { write(data); flush() }
            }
            ConnectionType.BLUETOOTH -> {
                if (config.btAddress.isBlank()) throw IllegalStateException("Endereço Bluetooth não configurado")
                val socket = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                    .getRemoteDevice(config.btAddress)
                    .createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()
                socket.outputStream.use { it.write(data); it.flush() }
                socket.close()
            }
            ConnectionType.USB -> printViaUsb(context, String(data, Charsets.ISO_8859_1))
        }
    }

    private fun enviar(context: Context, config: PrintConfig, zpl: String) {
        when (config.connectionType) {
            ConnectionType.TCP_IP -> printViaTcp(config.ip, config.port, zpl)
            ConnectionType.BLUETOOTH -> printViaBluetooth(context, config.btAddress, zpl)
            ConnectionType.USB -> printViaUsb(context, zpl)
        }
    }

    private fun printViaTcp(ip: String, port: Int, zpl: String) {
        Socket(ip, port).use { socket ->
            socket.soTimeout = 5000
            socket.getOutputStream().apply { write(zpl.toByteArray(Charsets.UTF_8)); flush() }
        }
    }

    private fun printViaBluetooth(context: Context, address: String, zpl: String) {
        if (address.isBlank()) throw IllegalStateException("Endereço Bluetooth não configurado")
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("Bluetooth não disponível")
        val socket = adapter.getRemoteDevice(address)
            .createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
        socket.connect()
        socket.outputStream.use { it.write(zpl.toByteArray(Charsets.UTF_8)); it.flush() }
        socket.close()
    }

    private fun printViaUsb(context: Context, zpl: String) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as android.hardware.usb.UsbManager
        val device = usbManager.deviceList.values.firstOrNull()
            ?: throw IllegalStateException("Nenhuma impressora USB encontrada")
        val connection = usbManager.openDevice(device)
            ?: throw IllegalStateException("Sem permissão USB")
        val iface = device.getInterface(0)
        val endpoint = (0 until iface.endpointCount).map { iface.getEndpoint(it) }
            .firstOrNull { it.direction == android.hardware.usb.UsbConstants.USB_DIR_OUT }
            ?: throw IllegalStateException("Endpoint USB não encontrado")
        connection.claimInterface(iface, true)
        val data = zpl.toByteArray(Charsets.UTF_8)
        connection.bulkTransfer(endpoint, data, data.size, 5000)
        connection.releaseInterface(iface)
        connection.close()
    }
}
