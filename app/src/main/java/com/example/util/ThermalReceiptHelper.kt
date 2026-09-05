package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import com.example.data.local.SaleWithItems
import com.example.data.model.DrawResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


fun String.removeEmojis(): String {
    return this.replace(Regex("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\s]"), "").trim()
}

object ThermalReceiptHelper {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    fun getDrawTxCode(saleId: String, drawId: String, index: Int = 0): String {
        val cleanSale = saleId.replace("-", "").uppercase()
        val cleanDraw = drawId.replace("-", "").uppercase()
        val combined = "$cleanSale:$cleanDraw:$index"
        val hash = (combined.hashCode().toLong() and 0xFFFFFFFFL).toString(16).uppercase().padStart(8, '0')
        val sub = ((cleanDraw.hashCode() xor (index * 7919)).toLong() and 0xFFFFL).toString(16).uppercase().padStart(4, '0')
        return "${hash.take(8)}-$sub"
    }

    /**
     * Formats receipt text strictly optimized for 32 columns (standard 58mm thermal paper width).
     * Automatically separates multiple draws, displays independent TX codes and draw results if present.
     */
    fun formatTicket58mm(saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()): String {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.createdAt))
        val timeStr = timeFormat.format(Date(sale.createdAt))

        val lineDivider = "--------------------------------" // 32 chars
        val doubleDivider = "================================" // 32 chars

        // Group items by draw so that each draw is separated and independent
        val groupedByDraw = items.groupBy { it.drawId }

        return buildString {
            appendLine(doubleDivider)
            appendLine(centerText("LOTERIA", 32))
            appendLine(centerText("COMPROBANTE", 32))
            appendLine(doubleDivider)
            appendLine("FECHA   : $dateStr")
            appendLine("HORA    : $timeStr")
            appendLine("CLIENTE : ${sale.customerName.take(22)}")
            appendLine("VENDEDOR: ${sale.userName.take(22)}")
            if (sale.status == "ANULADA") {
                appendLine(centerText("*** TICKET ANULADO ***", 32))
            }
            appendLine(lineDivider)

            var drawIndex = 0
            var grandTotalPrizes = 0.0
            groupedByDraw.forEach { (drawId, drawItems) ->
                val firstItem = drawItems.first()
                val drawName = firstItem.drawName
                val drawTxCode = getDrawTxCode(sale.id, drawId, drawIndex)
                val drawSubtotal = drawItems.sumOf { it.total }
                val res = resultsMap[drawId]

                // Draw Header
                appendLine(centerText(">> $drawName <<", 32))

                // Draw Results if present
                if (res != null && (res.firstPrize.isNotBlank() || res.secondPrize.isNotBlank() || res.thirdPrize.isNotBlank())) {
                    val r1 = res.firstPrize.takeIf { it.isNotBlank() } ?: "--"
                    val r2 = res.secondPrize.takeIf { it.isNotBlank() } ?: "--"
                    val r3 = res.thirdPrize.takeIf { it.isNotBlank() } ?: "--"
                    appendLine(centerText("RESULTADOS: [$r1] [$r2] [$r3]", 32))
                }

                // Table Header for this Draw
                // NUMERO(8)  TIPO(6)  PZS(6)  MONTO(12) = 32
                appendLine("NUMERO    TIPO     PZS     MONTO")
                appendLine("- - - - - - - - - - - - - - - -")

                var drawPrizeTotal = 0.0
                drawItems.forEach { play ->
                    val playPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
                    drawPrizeTotal += playPrize

                    val numStr = play.number.padEnd(8, ' ')
                    val modStr = (if (play.modality.uppercase().startsWith("CH")) "CH" else if (play.modality.uppercase().startsWith("P")) "PL" else play.modality).padEnd(6, ' ')
                    val qtyStr = "x${play.quantity}".padEnd(6, ' ')
                    val totStr = "$${String.format(Locale.US, "%.2f", play.total)}".padStart(12, ' ')
                    appendLine("$numStr$modStr$qtyStr$totStr")

                    if (playPrize > 0.0) {
                        appendLine("  ★ PREMIO: +$${String.format(Locale.US, "%.2f", playPrize)}")
                    }
                }
                grandTotalPrizes += drawPrizeTotal

                // Draw Subtotal & Independent TX
                appendLine("TX: $drawTxCode")
                if (drawPrizeTotal > 0.0) {
                    val prizeLine = "PREMIO SORTEO: +$${String.format(Locale.US, "%.2f", drawPrizeTotal)}"
                    appendLine(prizeLine.padStart(32, ' '))
                }
                val subtotalLine = "SUBTOTAL: $${String.format(Locale.US, "%.2f", drawSubtotal)}"
                appendLine(subtotalLine.padStart(32, ' '))
                appendLine(lineDivider)
                drawIndex++
            }

            val totalLabel = "TOTAL :"
            val totalAmount = "$${String.format(Locale.US, "%.2f", sale.total)}"
            val totalSpaces = (32 - totalLabel.length - totalAmount.length).coerceAtLeast(1)
            appendLine(totalLabel + " ".repeat(totalSpaces) + totalAmount)

            if (grandTotalPrizes > 0.0) {
                val prizeLabel = "TOTAL PREMIADO:"
                val prizeAmount = "$${String.format(Locale.US, "%.2f", grandTotalPrizes)}"
                val prizeSpaces = (32 - prizeLabel.length - prizeAmount.length).coerceAtLeast(1)
                appendLine(prizeLabel + " ".repeat(prizeSpaces) + prizeAmount)
            }
            appendLine(doubleDivider)
            appendLine(centerText("IMPORTANTE", 32))
            appendLine(centerText("Sin comprobante no se pagan premios.", 32))
            appendLine(centerText("Por favor verificar su ticket,", 32))
            appendLine(centerText("no se aceptan cambios luego del cierre.", 32))
            appendLine(centerText("¡GRACIAS POR SU COMPRA!", 32))
            appendLine(doubleDivider)
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val leftPadding = (width - text.length) / 2
        val rightPadding = width - text.length - leftPadding
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding)
    }

    /**
     * Formats WhatsApp friendly text including attached draw details, hours, subtotals, prizes and disclaimers.
     */
    fun formatWhatsAppTicket(saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()): String {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.createdAt))
        val timeStr = timeFormat.format(Date(sale.createdAt))
        val groupedByDraw = items.groupBy { it.drawId }

        val totalPrizes = if (sale.status == "ANULADA") 0.0 else {
            items.sumOf { play ->
                val res = resultsMap[play.drawId]
                if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
            }
        }

        return buildString {
            appendLine("🎟️ *LOTERIA - COMPROBANTE OFICIAL*")
            appendLine("Ticket: #${sale.ticketNumber}")
            appendLine("📅 Fecha: $dateStr  ⏰ Hora: $timeStr")
            appendLine("👤 Cliente: ${sale.customerName}")
            appendLine("💼 Vendedor: ${sale.userName}")
            if (sale.status == "ANULADA") {
                appendLine("❌ *TICKET ANULADO*")
            }
            if (totalPrizes > 0.0) {
                appendLine()
                appendLine("🏆 *¡GANADOR!* 🏆")
                appendLine("💰 *TOTAL PREMIADO: $${String.format(Locale.US, "%.2f", totalPrizes)}*")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📍 *SORTEOS Y HORARIOS:*")

            var dIdx = 0
            groupedByDraw.forEach { (drawId, drawItems) ->
                val first = drawItems.first()
                val subtotal = drawItems.sumOf { it.total }
                val tx = getDrawTxCode(sale.id, drawId, dIdx)
                val res = resultsMap[drawId]
                val drawPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) {
                    drawItems.sumOf { PrizeCalculator.calculateItemPrize(it, res) }
                } else 0.0

                appendLine()
                appendLine("🔹 *${first.drawName}*")
                if (res != null && (res.firstPrize.isNotBlank() || res.secondPrize.isNotBlank() || res.thirdPrize.isNotBlank())) {
                    val r1 = res.firstPrize.ifBlank { "--" }
                    val r2 = res.secondPrize.ifBlank { "--" }
                    val r3 = res.thirdPrize.ifBlank { "--" }
                    appendLine("   🎯 Resultados: 1ro:[$r1] 2do:[$r2] 3ro:[$r3]")
                }

                drawItems.forEach { itm ->
                    val mod = if (itm.modality.uppercase().startsWith("CH")) "CH" else if (itm.modality.uppercase().startsWith("P")) "PL" else itm.modality
                    val pPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) PrizeCalculator.calculateItemPrize(itm, res) else 0.0
                    val winTag = if (pPrize > 0.0) " 🏆 (+$${String.format(Locale.US, "%.2f", pPrize)})" else ""
                    appendLine("   • ${itm.number} | $mod | x${itm.quantity} | $${String.format(Locale.US, "%.2f", itm.total)}$winTag")
                }
                appendLine("   TX: $tx  |  Subtotal: $${String.format(Locale.US, "%.2f", subtotal)}")
                if (drawPrize > 0.0) {
                    appendLine("   ⭐ *Premio Sorteo: +$${String.format(Locale.US, "%.2f", drawPrize)}*")
                }
                dIdx++
            }

            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("💵 *TOTAL VENTA: $${String.format(Locale.US, "%.2f", sale.total)}*")
            if (totalPrizes > 0.0) {
                appendLine("🎉 *TOTAL PREMIADO: $${String.format(Locale.US, "%.2f", totalPrizes)}*")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("⚠️ *IMPORTANTE:* Sin comprobante no se pagan premios.")
            appendLine("📌 *Por favor verificar su ticket, no se aceptan cambios luego del cierre.*")
            appendLine("✨ *¡GRACIAS POR SU COMPRA!*")
        }
    }

    /**
     * Strictly shares ticket through WhatsApp / WhatsApp Business.
     * Shares high-resolution compact ticket image alongside the formatted text.
     */
    fun shareTicketStrictlyToWhatsApp(context: Context, saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()) {
        val ticketText = formatWhatsAppTicket(saleWithItems, resultsMap)
        val bitmap = renderTicketBitmap(saleWithItems, resultsMap)

        try {
            val cacheDir = java.io.File(context.cacheDir, "tickets").apply { mkdirs() }
            val imageFile = java.io.File(cacheDir, "Ticket_${saleWithItems.sale.ticketNumber}.jpg")
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val imageUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            // Check for standard WhatsApp or WhatsApp Business
            val packageManager = context.packageManager
            val whatsappPackages = listOf("com.whatsapp", "com.whatsapp.w4b")
            var targetPackage: String? = null

            for (pkg in whatsappPackages) {
                try {
                    packageManager.getPackageInfo(pkg, 0)
                    targetPackage = pkg
                    break
                } catch (_: Exception) { }
            }

            if (targetPackage != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    setPackage(targetPackage)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, ticketText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Direct WhatsApp Web/API fallback
                val uri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(ticketText)}")
                val browserIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, ticketText)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "No se pudo abrir WhatsApp. Verifique que la aplicación esté instalada.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun renderTicketBitmap(saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()): Bitmap {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val groupedByDraw = items.groupBy { it.drawId }

        val totalTicketPrizes = if (sale.status == "ANULADA") 0.0 else {
            items.sumOf { play ->
                val res = resultsMap[play.drawId]
                if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
            }
        }

        // Standard 58mm compact receipt width (380px) for authentic ticket aspect ratio
        val width = 380
        var estimatedHeight = 240
        if (totalTicketPrizes > 0.0) estimatedHeight += 70
        if (sale.status == "ANULADA") estimatedHeight += 40

        groupedByDraw.forEach { (drawId, drawItems) ->
            val res = resultsMap[drawId]
            estimatedHeight += 36 // Draw header
            if (res != null && (res.firstPrize.isNotBlank() || res.secondPrize.isNotBlank() || res.thirdPrize.isNotBlank())) {
                estimatedHeight += 48 // Results box
            }
            estimatedHeight += 24 // Column headers
            drawItems.forEach { play ->
                val pPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
                estimatedHeight += if (pPrize > 0.0) 38 else 22
            }
            estimatedHeight += 32 // Subtotal & TX
            estimatedHeight += 12 // Spacing
        }
        estimatedHeight += 60 // Total box
        estimatedHeight += 72 // Disclaimer box
        estimatedHeight += 40 // Footer

        val height = estimatedHeight.coerceAtLeast(480)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white background
        canvas.drawColor(Color.WHITE)

        val cardRect = RectF(6f, 6f, width - 6f, height - 6f)
        val bgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 16f, 16f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(cardRect, 16f, 16f, borderPaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        var y = 42f

        // LOTERIA Title in rich Green
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#15803D")
        canvas.drawText("LOTERIA", 22f, y, textPaint)
        y += 18f

        textPaint.textSize = 11.5f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#16A34A")
        canvas.drawText("COMPROBANTE", 22f, y, textPaint)
        y += 20f

        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        textPaint.textSize = 10.5f
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("FECHA: ${dateFormat.format(Date(sale.createdAt))}", 22f, y, textPaint)
        y += 16f
        canvas.drawText("HORA:  ${timeFormat.format(Date(sale.createdAt))}", 22f, y, textPaint)
        y += 20f

        // Top-Right QR Code
        val qrSize = 56f
        val qrLeft = width - 22f - qrSize
        val qrTop = 22f
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
        val qrBgPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(qrRect, 6f, 6f, qrBgPaint)
        val qrBorderPaint = Paint().apply {
            color = Color.parseColor("#BBF7D0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(qrRect, 6f, 6f, qrBorderPaint)

        // Draw deterministic QR pattern in Green
        val qrData = "TICKET:${sale.ticketNumber}|TOTAL:${sale.total}|DATE:${sale.createdAt}"
        val qrHash = qrData.hashCode().toLong() and 0xFFFFFFFFL
        val matrixSize = 21
        val cellW = (qrSize - 8f) / matrixSize
        val qrCellPaint = Paint().apply {
            color = Color.parseColor("#15803D")
            style = Paint.Style.FILL
        }
        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                val inFinder = (r < 6 && c < 6) || (r < 6 && c >= matrixSize - 6) || (r >= matrixSize - 6 && c < 6)
                val inTiming = (r == 5 || c == 5)
                val isDarkCell = if (inFinder) {
                    val finderR = if (r >= matrixSize - 6) r - (matrixSize - 6) else r
                    val finderC = if (c >= matrixSize - 6) c - (matrixSize - 6) else c
                    finderR == 0 || finderR == 5 || finderC == 0 || finderC == 5 || (finderR in 2..3 && finderC in 2..3)
                } else if (inTiming) {
                    (r + c) % 2 == 0
                } else {
                    val bit1 = ((qrHash shr ((r * matrixSize + c) % 31)) and 1L) == 1L
                    val bit2 = ((r * 7 + c * 11) % 3) == 0
                    bit1 xor bit2
                }
                if (isDarkCell) {
                    canvas.drawRect(
                        qrLeft + 4f + c * cellW,
                        qrTop + 4f + r * cellW,
                        qrLeft + 4f + (c + 1) * cellW,
                        qrTop + 4f + (r + 1) * cellW,
                        qrCellPaint
                    )
                }
            }
        }

        // Ticket Anulado Banner
        if (sale.status == "ANULADA") {
            val annulRect = RectF(22f, y, width - 22f, y + 28f)
            val annulBg = Paint().apply { color = Color.parseColor("#FFEBEE"); style = Paint.Style.FILL }
            val annulBorder = Paint().apply { color = Color.parseColor("#E53935"); style = Paint.Style.STROKE; strokeWidth = 1.5f }
            canvas.drawRoundRect(annulRect, 6f, 6f, annulBg)
            canvas.drawRoundRect(annulRect, 6f, 6f, annulBorder)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.textSize = 11.5f
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textPaint.color = Color.parseColor("#C62828")
            canvas.drawText("*** TICKET ANULADO ***", (width / 2).toFloat(), y + 19f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            y += 36f
        }

        // Winner Banner if won
        if (totalTicketPrizes > 0.0) {
            val bannerRect = RectF(22f, y, width - 22f, y + 54f)
            val bannerPaint = Paint().apply {
                color = Color.parseColor("#FEF3C7")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(bannerRect, 10f, 10f, bannerPaint)

            val bannerBorder = Paint().apply {
                color = Color.parseColor("#F59E0B")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(bannerRect, 10f, 10f, bannerBorder)

            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textPaint.textSize = 13f
            textPaint.color = Color.parseColor("#B45309")
            canvas.drawText("¡ GANADOR! ¡¡¡", (width / 2).toFloat(), y + 20f, textPaint)

            textPaint.textSize = 15f
            textPaint.color = Color.parseColor("#92400E")
            canvas.drawText("TOTAL: $${String.format(Locale.US, "%.2f", totalTicketPrizes)}", (width / 2).toFloat(), y + 42f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            y += 64f
        }

        // Cliente y Vendedor Card
        val metaRect = RectF(22f, y, width - 22f, y + 44f)
        val metaBg = Paint().apply { color = Color.parseColor("#F8FAFC"); style = Paint.Style.FILL }
        val metaBorder = Paint().apply { color = Color.parseColor("#E2E8F0"); style = Paint.Style.STROKE; strokeWidth = 1f }
        canvas.drawRoundRect(metaRect, 8f, 8f, metaBg)
        canvas.drawRoundRect(metaRect, 8f, 8f, metaBorder)

        textPaint.textSize = 10.5f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("CLIENTE:", 32f, y + 18f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        val custName = if (sale.customerName.length > 20) sale.customerName.take(19) + "…" else sale.customerName
        canvas.drawText(custName, 106f, y + 18f, textPaint)

        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("VENDEDOR:", 32f, y + 34f, textPaint)
        textPaint.color = Color.parseColor("#0F172A")
        val sellerName = if (sale.userName.length > 20) sale.userName.take(19) + "…" else sale.userName
        canvas.drawText(sellerName, 106f, y + 34f, textPaint)
        y += 54f

        var dIdx = 0
        groupedByDraw.forEach { (drawId, drawItems) ->
            val first = drawItems.first()
            val drawTx = getDrawTxCode(sale.id, drawId, dIdx)
            val drawSubtotal = drawItems.sumOf { it.total }
            val res = resultsMap[drawId]

            // Draw header in Green
            textPaint.textAlign = Paint.Align.CENTER
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textPaint.textSize = 12f
            textPaint.color = Color.parseColor("#15803D")
            canvas.drawText("📈 🇭🇳 ${first.drawName} 🌙", (width / 2).toFloat(), y + 14f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            y += 24f

            // Results box if present (3 clean columns)
            if (res != null && (res.firstPrize.isNotBlank() || res.secondPrize.isNotBlank() || res.thirdPrize.isNotBlank())) {
                val resRect = RectF(22f, y, width - 22f, y + 38f)
                val resBg = Paint().apply {
                    color = Color.parseColor("#F1F5F9")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(resRect, 8f, 8f, resBg)

                textPaint.textSize = 9.5f
                textPaint.color = Color.parseColor("#D97706")
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("1er Premio", width * 0.22f, y + 14f, textPaint)
                textPaint.color = Color.parseColor("#475569")
                canvas.drawText("2do Premio", width * 0.50f, y + 14f, textPaint)
                textPaint.color = Color.parseColor("#D97706")
                canvas.drawText("3er Premio", width * 0.78f, y + 14f, textPaint)

                textPaint.textSize = 13.5f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textPaint.color = Color.parseColor("#D97706")
                canvas.drawText(res.firstPrize.ifBlank { "--" }, width * 0.22f, y + 30f, textPaint)
                textPaint.color = Color.parseColor("#334155")
                canvas.drawText(res.secondPrize.ifBlank { "--" }, width * 0.50f, y + 30f, textPaint)
                textPaint.color = Color.parseColor("#D97706")
                canvas.drawText(res.thirdPrize.ifBlank { "--" }, width * 0.78f, y + 30f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                y += 46f
            }

            // Plays table column headers
            textPaint.textSize = 10f
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textPaint.color = Color.parseColor("#64748B")
            canvas.drawText("NÚMERO", 26f, y, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("TIPO", 140f, y, textPaint)
            canvas.drawText("PZS", 215f, y, textPaint)
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("MONTO", width - 26f, y, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            y += 16f

            drawItems.forEach { play ->
                val pPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
                val mod = if (play.modality.uppercase().startsWith("CH")) "CH" else if (play.modality.uppercase().startsWith("P")) "PL" else play.modality

                if (pPrize > 0.0) {
                    val winRowRect = RectF(22f, y - 12f, width - 22f, y + 22f)
                    val winRowPaint = Paint().apply {
                        color = Color.parseColor("#FEF9C3")
                        style = Paint.Style.FILL
                    }
                    canvas.drawRoundRect(winRowRect, 4f, 4f, winRowPaint)
                }

                textPaint.textSize = 12.5f
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textPaint.color = if (pPrize > 0.0) Color.parseColor("#B45309") else Color.parseColor("#0F172A")
                canvas.drawText(play.number + if (pPrize > 0.0) " •" else "", 26f, y, textPaint)

                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textPaint.color = Color.parseColor("#475569")
                textPaint.textAlign = Paint.Align.CENTER
                canvas.drawText(mod, 140f, y, textPaint)
                canvas.drawText("x${play.quantity}", 215f, y, textPaint)

                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textPaint.color = Color.parseColor("#0F172A")
                canvas.drawText("$${String.format(Locale.US, "%.2f", play.total)}", width - 26f, y, textPaint)
                textPaint.textAlign = Paint.Align.LEFT
                y += 16f

                if (pPrize > 0.0) {
                    textPaint.textAlign = Paint.Align.RIGHT
                    textPaint.textSize = 10.5f
                    textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                    textPaint.color = Color.parseColor("#D97706")
                    canvas.drawText("+$${String.format(Locale.US, "%.2f", pPrize)}", width - 26f, y, textPaint)
                    textPaint.textAlign = Paint.Align.LEFT
                    y += 16f
                }
            }

            // Subtotal and TX
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textPaint.color = Color.parseColor("#64748B")
            canvas.drawText("TX: $drawTx", 26f, y + 4f, textPaint)

            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = 10.5f
            textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textPaint.color = Color.parseColor("#0F172A")
            canvas.drawText("SUBTOTAL: $${String.format(Locale.US, "%.2f", drawSubtotal)}", width - 26f, y + 4f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT
            y += 18f

            // Subtle divider between draws
            val dashPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            }
            val dashPath = Path().apply {
                moveTo(26f, y)
                lineTo(width - 26f, y)
            }
            canvas.drawPath(dashPath, dashPaint)
            y += 14f

            dIdx++
        }

        // Total Box in Vibrant Green
        val totalRect = RectF(22f, y, width - 22f, y + 44f)
        val totalPaint = Paint().apply {
            color = Color.parseColor("#15803D")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(totalRect, 10f, 10f, totalPaint)

        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.WHITE
        canvas.drawText("TOTAL :", 38f, y + 27f, textPaint)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 18f
        canvas.drawText("$${String.format(Locale.US, "%.2f", sale.total)}", width - 38f, y + 29f, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
        y += 56f

        // Disclaimer Box
        val discRect = RectF(22f, y, width - 22f, y + 54f)
        val discBg = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(discRect, 8f, 8f, discBg)
        val discBorder = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(discRect, 8f, 8f, discBorder)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#DC2626")
        canvas.drawText("⚠️ IMPORTANTE: Sin comprobante no se pagan premios.", (width / 2).toFloat(), y + 20f, textPaint)

        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#475569")
        canvas.drawText("Por favor verificar su ticket, no se aceptan cambios luego del cierre.", (width / 2).toFloat(), y + 38f, textPaint)
        y += 68f

        // Footer
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textPaint.color = Color.parseColor("#64748B")
        canvas.drawText("¡ G R A C I A S   P O R   S U   C O M P R A !", (width / 2).toFloat(), y, textPaint)

        return bitmap
    }

    fun saveReceiptAsImage(context: Context, saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()) {
        try {
            val bitmap = renderTicketBitmap(saleWithItems, resultsMap)
            val filename = "Ticket_${saleWithItems.sale.ticketNumber}_${System.currentTimeMillis()}.jpg"

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Loteria")
                    put(android.provider.MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                Toast.makeText(context, "📸 Foto del ticket guardada en la galería", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No se pudo crear el archivo de imagen", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al guardar foto: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates raw ESC/POS command bytes optimized for 58mm portable thermal printers.
     */
    fun generateEscPos58mm(saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()): ByteArray {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateStr = dateFormat.format(Date(sale.createdAt))
        val timeStr = timeFormat.format(Date(sale.createdAt))

        val output = mutableListOf<Byte>()

        fun addBytes(vararg bytes: Int) {
            bytes.forEach { output.add(it.toByte()) }
        }

        fun addString(text: String) {
            val bytes = text.toByteArray(charset("ISO-8859-1"))
            bytes.forEach { output.add(it) }
        }

        // Initialize printer: ESC @
        addBytes(0x1B, 0x40)

        // Center align: ESC a 1
        addBytes(0x1B, 0x61, 0x01)

        // Double height + Bold for Title: ESC ! 0x30
        addBytes(0x1B, 0x21, 0x30)
        addString("LOTERIA\n")

        // Normal text: ESC ! 0x00
        addBytes(0x1B, 0x21, 0x00)
        addString("COMPROBANTE\n")
        addString("================================\n")

        // Left align: ESC a 0
        addBytes(0x1B, 0x61, 0x00)
        addString("FECHA   : $dateStr\n")
        addString("HORA    : $timeStr\n")
        addString("CLIENTE : ${sale.customerName}\n")
        addString("VENDEDOR: ${sale.userName}\n")

        if (sale.status == "ANULADA") {
            addBytes(0x1B, 0x61, 0x01)
            addBytes(0x1B, 0x45, 0x01) // Bold
            addString("*** TICKET ANULADO ***\n")
            addBytes(0x1B, 0x45, 0x00)
            addBytes(0x1B, 0x61, 0x00)
        }

        addString("--------------------------------\n")

        val groupedByDraw = items.groupBy { it.drawId }
        var drawIndex = 0
        var grandTotalPrizes = 0.0
        groupedByDraw.forEach { (drawId, drawItems) ->
            val firstItem = drawItems.first()
            val drawName = firstItem.drawName
            val drawTxCode = getDrawTxCode(sale.id, drawId, drawIndex)
            val drawSubtotal = drawItems.sumOf { it.total }
            val res = resultsMap[drawId]

            // Draw Header
            addBytes(0x1B, 0x61, 0x01) // Center
            addBytes(0x1B, 0x45, 0x01) // Bold
            addString(">> ${drawName.removeEmojis()} <<\n")
            addBytes(0x1B, 0x45, 0x00) // Bold off

            if (res != null && (res.firstPrize.isNotBlank() || res.secondPrize.isNotBlank() || res.thirdPrize.isNotBlank())) {
                val r1 = res.firstPrize.takeIf { it.isNotBlank() } ?: "--"
                val r2 = res.secondPrize.takeIf { it.isNotBlank() } ?: "--"
                val r3 = res.thirdPrize.takeIf { it.isNotBlank() } ?: "--"
                addString("RESULTADOS: [$r1] [$r2] [$r3]\n")
            }

            addBytes(0x1B, 0x61, 0x00) // Left
            addString("NUMERO    TIPO     PZS     MONTO\n")
            addString("- - - - - - - - - - - - - - - - \n")

            var drawPrizeTotal = 0.0
            drawItems.forEach { play ->
                val playPrize = if (sale.status == "ANULADA") 0.0 else if (res != null) PrizeCalculator.calculateItemPrize(play, res) else 0.0
                drawPrizeTotal += playPrize

                val numStr = play.number.padEnd(8, ' ')
                val modStr = (if (play.modality.uppercase().startsWith("CH")) "CH" else if (play.modality.uppercase().startsWith("P")) "PL" else play.modality).padEnd(6, ' ')
                val qtyStr = "x${play.quantity}".padEnd(6, ' ')
                val totStr = "$${String.format(Locale.US, "%.2f", play.total)}".padStart(12, ' ')
                addString("$numStr$modStr$qtyStr$totStr\n")

                if (playPrize > 0.0) {
                    addString("  * PREMIO: +$${String.format(Locale.US, "%.2f", playPrize)}\n")
                }
            }
            grandTotalPrizes += drawPrizeTotal

            addString("TX: $drawTxCode\n")
            if (drawPrizeTotal > 0.0) {
                addBytes(0x1B, 0x61, 0x02) // Right align
                addString("PREMIO SORTEO: +$${String.format(Locale.US, "%.2f", drawPrizeTotal)}\n")
                addBytes(0x1B, 0x61, 0x00) // Left align
            }
            val subtotalLine = "SUBTOTAL: $${String.format(Locale.US, "%.2f", drawSubtotal)}\n"
            addBytes(0x1B, 0x61, 0x02) // Right align
            addString(subtotalLine)
            addBytes(0x1B, 0x61, 0x00) // Left align
            addString("--------------------------------\n")
            drawIndex++
        }

        // Bold Double Width Total: ESC ! 0x20
        addBytes(0x1B, 0x45, 0x01) // Bold on
        addBytes(0x1B, 0x61, 0x02) // Right align
        addBytes(0x1B, 0x21, 0x10) // Double height
        addString("TOTAL: $${String.format(Locale.US, "%.2f", sale.total)}\n")
        addBytes(0x1B, 0x21, 0x00) // Normal text
        addBytes(0x1B, 0x45, 0x00) // Bold off

        if (grandTotalPrizes > 0.0) {
            addBytes(0x1B, 0x45, 0x01)
            addBytes(0x1B, 0x61, 0x02)
            addString("TOTAL PREMIADO: $${String.format(Locale.US, "%.2f", grandTotalPrizes)}\n")
            addBytes(0x1B, 0x45, 0x00)
        }

        // Center align: ESC a 1
        addBytes(0x1B, 0x61, 0x01)
        addString("================================\n")
        addString("           IMPORTANTE           \n")
        addString("Sin comprobante no se pagan     \n")
        addString("premios.                        \n")
        addString("Por favor verificar su ticket,  \n")
        addString("no se aceptan cambios luego del \n")
        addString("cierre.                         \n")
        addString("    ¡GRACIAS POR SU COMPRA!     \n")
        addString("================================\n")

        // Feed 4 lines & Cut: ESC d 4
        addBytes(0x1B, 0x64, 0x04)
        // Partial Cut: GS V 66 0
        addBytes(0x1D, 0x56, 0x42, 0x00)

        return output.toByteArray()
    }

    /**
     * Native Android PrintManager adapter specifically configured for 58mm thermal paper roll dimensions.
     */
    fun printTicket58mm(context: Context, saleWithItems: SaleWithItems, resultsMap: Map<String, DrawResult> = emptyMap()) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: run {
            Toast.makeText(context, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Ticket_POS_${saleWithItems.sale.ticketNumber}"

        val adapter = object : PrintDocumentAdapter() {
            private var pdfDocument: PdfDocument? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("$jobName.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()

                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    val groupedByDraw = saleWithItems.items.groupBy { it.drawId }
                    val pageWidth = 200
                    val pageHeight = (260 + (saleWithItems.items.size * 18) + (groupedByDraw.size * 45)).coerceAtLeast(360)

                    val doc = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas

                    // Background white for thermal
                    canvas.drawColor(Color.WHITE)

                    val paint = Paint().apply {
                        color = Color.BLACK
                        isAntiAlias = true
                        typeface = Typeface.MONOSPACE
                    }

                    var y = 18f

                    // Title
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("LOTERIA", (pageWidth / 2).toFloat(), y, paint)
                    y += 10f

                    paint.textSize = 7f
                    paint.isFakeBoldText = false
                    canvas.drawText("COMPROBANTE", (pageWidth / 2).toFloat(), y, paint)
                    y += 8f

                    // Divider
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, paint)
                    y += 9f

                    val sale = saleWithItems.sale
                    val dateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

                    paint.textSize = 6.5f
                    canvas.drawText("FECHA   : ${dateFormat.format(Date(sale.createdAt))}", 10f, y, paint)
                    y += 8f
                    canvas.drawText("HORA    : ${timeFormat.format(Date(sale.createdAt))}", 10f, y, paint)
                    y += 8f
                    canvas.drawText("CLIENTE : ${sale.customerName}", 10f, y, paint)
                    y += 8f
                    canvas.drawText("VENDEDOR: ${sale.userName}", 10f, y, paint)
                    y += 8f

                    // Divider
                    canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, paint)
                    y += 9f

                    var dIdx = 0
                    groupedByDraw.forEach { (drawId, drawItems) ->
                        val firstItem = drawItems.first()
                        val drawTx = getDrawTxCode(sale.id, drawId, dIdx)
                        val drawSubtotal = drawItems.sumOf { it.total }

                        paint.isFakeBoldText = true
                        paint.textSize = 7f
                        paint.textAlign = Paint.Align.CENTER
                        canvas.drawText(">> ${firstItem.drawName.removeEmojis()} <<", (pageWidth / 2).toFloat(), y, paint)
                        y += 8f
                        paint.textAlign = Paint.Align.LEFT

                        val res = resultsMap[drawId]
                        if (res != null && res.firstPrize.isNotBlank()) {
                            paint.textSize = 6f
                            paint.isFakeBoldText = false
                            canvas.drawText("Premios: 1ro:[${res.firstPrize}] 2do:[${res.secondPrize}] 3ro:[${res.thirdPrize}]", 10f, y, paint)
                            y += 7f
                        }

                        // Table Header
                        paint.isFakeBoldText = true
                        paint.textSize = 6f
                        canvas.drawText("NUMERO", 10f, y, paint)
                        canvas.drawText("TIPO", 65f, y, paint)
                        canvas.drawText("PZS", 110f, y, paint)
                        canvas.drawText("MONTO", 155f, y, paint)
                        y += 7f
                        paint.isFakeBoldText = false

                        drawItems.forEach { itm ->
                            val mod = if (itm.modality.uppercase().startsWith("CH")) "CH" else if (itm.modality.uppercase().startsWith("P")) "PL" else itm.modality
                            paint.isFakeBoldText = true
                            canvas.drawText(itm.number, 10f, y, paint)
                            paint.isFakeBoldText = false
                            canvas.drawText(mod, 65f, y, paint)
                            canvas.drawText("x${itm.quantity}", 110f, y, paint)
                            canvas.drawText("$${String.format(Locale.US, "%.2f", itm.total)}", 155f, y, paint)
                            y += 8f
                        }

                        paint.textSize = 5.5f
                        canvas.drawText("TX: $drawTx", 10f, y, paint)
                        paint.textAlign = Paint.Align.RIGHT
                        paint.isFakeBoldText = true
                        canvas.drawText("SUBTOTAL: $${String.format(Locale.US, "%.2f", drawSubtotal)}", (pageWidth - 10).toFloat(), y, paint)
                        paint.textAlign = Paint.Align.LEFT
                        y += 8f

                        canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, paint)
                        y += 8f
                        dIdx++
                    }

                    // Total
                    paint.isFakeBoldText = true
                    paint.textSize = 9f
                    canvas.drawText("TOTAL :", 10f, y, paint)
                    paint.textAlign = Paint.Align.RIGHT
                    canvas.drawText("$${String.format(Locale.US, "%.2f", sale.total)}", (pageWidth - 10).toFloat(), y, paint)
                    paint.textAlign = Paint.Align.LEFT
                    y += 12f

                    // Footer
                    canvas.drawLine(10f, y, (pageWidth - 10).toFloat(), y, paint)
                    y += 10f
                    paint.textSize = 5.5f
                    paint.isFakeBoldText = false
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("IMPORTANTE: Sin comprobante no se pagan premios.", (pageWidth / 2).toFloat(), y, paint)

                    doc.finishPage(page)

                    FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    doc.close()

                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMediaSize(PrintAttributes.MediaSize.ISO_A7)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, printAttributes)
    }

    /**
     * Direct Bluetooth printing for ESC/POS portable 58mm printers.
     */
    @SuppressLint("MissingPermission")
    suspend fun printToBluetoothPrinter(
        context: Context,
        device: BluetoothDevice,
        saleWithItems: SaleWithItems
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        var socket: BluetoothSocket? = null
        var outputStream: OutputStream? = null

        try {
            val bytes = generateEscPos58mm(saleWithItems)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            outputStream = socket.outputStream
            outputStream.write(bytes)
            outputStream.flush()
            Pair(true, "Ticket impreso correctamente en ${device.name ?: "Impresora 58mm"}")
        } catch (e: Exception) {
            Pair(false, "Error al imprimir en impresora Bluetooth: ${e.message}")
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
