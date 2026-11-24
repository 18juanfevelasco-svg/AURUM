package com.aurum.finanzas.notif

import com.aurum.finanzas.data.Categories
import com.aurum.finanzas.data.TxType
import com.aurum.finanzas.data.norm

/** Resultado de leer una notificación */
data class ParsedTx(
    val amount: Double,
    val type: TxType,
    val merchant: String,
    val cardLast4: String,
    val method: String,
    val bank: String,
    val categoryId: String,
)

/**
 * Lee el texto de una notificación (o SMS) del banco y extrae: monto, si es gasto o ingreso,
 * comercio, tarjeta, método de pago, banco y categoría.
 * Pensado para los formatos habituales de bancos en Colombia.
 */
object NotificationParser {

    /** Apps que se escuchan por defecto: bancos, billeteras y apps de mensajes (SMS) */
    private val sourceHints = listOf(
        "bancolombia", "todo1", "davivienda", "daviplata", "nequi", "com.nu.", "nubank", "bbva",
        "bancodebogota", "bogota", "occidente", "bancopopular", "avvillas", "itau", "scotiabank",
        "colpatria", "falabella", "lulo", "rappipay", "rappicard", "movii", "cajasocial", "bancoomeva",
        "serfinanza", "finandina", "pichincha", "bancoagrario", "mibanco", "dale", "ualá", "uala",
        "messaging", "mms", "sms", "message",
    )

    private val bankNames = listOf(
        "bancolombia" to "Bancolombia", "todo1" to "Bancolombia",
        "daviplata" to "DaviPlata", "davivienda" to "Davivienda",
        "nequi" to "Nequi", "com.nu." to "Nu", "nubank" to "Nu", "nu colombia" to "Nu",
        "bbva" to "BBVA", "bancodebogota" to "Banco de Bogotá", "banco de bogota" to "Banco de Bogotá",
        "occidente" to "Banco de Occidente", "banco popular" to "Banco Popular", "bancopopular" to "Banco Popular",
        "avvillas" to "AV Villas", "av villas" to "AV Villas", "itau" to "Itaú",
        "colpatria" to "Scotiabank Colpatria", "scotiabank" to "Scotiabank Colpatria",
        "falabella" to "Banco Falabella", "lulo" to "Lulo Bank", "rappipay" to "RappiPay",
        "rappicard" to "RappiCard", "caja social" to "Caja Social", "cajasocial" to "Caja Social",
        "movii" to "Movii", "uala" to "Ualá",
    )

    /** Si aparece alguna de estas, NO es una compra real (rechazos, códigos, publicidad) */
    private val rejectWords = listOf(
        "rechazad", "declinad", "no aprobad", "no fue aprobad", "no fue exitos", "no exitos", "fallid",
        "clave dinamica", "codigo de verificacion", "codigo de seguridad", "otp", "token", "no compartas",
        "oferta", "promocion", "aprovecha", "sorteo", "descuento", "preaprobad", "cashback de hasta",
    )

    private val incomeWords = listOf(
        "recibiste", "te consignaron", "consignacion", "te abonaron", "abono de", "te transfirieron",
        "te enviaron", "transferencia recibida", "pago recibido", "reembolso", "devolucion", "nomina",
        "te llego", "te llegaron", "has recibido", "recibio un", "ingreso de",
    )

    private val expenseWords = listOf(
        "compra", "compraste", "pagaste", "pago", "retiro", "retiraste", "transferiste", "transferencia",
        "enviaste", "debito", "cargo", "avance", "cobro", "pse", "realizaste", "consumo",
    )

    private val amountWithSymbol = Regex("""(?:\$|cop|usd)\s?([0-9][0-9.,]*[0-9]|[0-9])""", RegexOption.IGNORE_CASE)
    private val amountWithWord = Regex("""([0-9][0-9.,]*[0-9])\s?(?:pesos|cop)\b""", RegexOption.IGNORE_CASE)

    private const val STOP =
        """(?=\s+con\s|\s+el\s+\d|\s+el\s+dia|\s+a\s+las\s|\s+\d{1,2}:\d{2}|\s+\d{1,2}/\d{1,2}|\s+t\.\s?(?:cred|deb)|\s+tarjeta|\s+desde|\s+por\s|\s+en\s|\s*[,;]|\.\s|\.$|\n|$)"""

    private val enRegex = Regex("""\ben\s+([^\n]+?)$STOP""", RegexOption.IGNORE_CASE)
    private val aRegex = Regex("""\ba\s+([^\n]+?)$STOP""", RegexOption.IGNORE_CASE)
    private val deRegex = Regex("""\bde\s+([^\n$]+?)$STOP""", RegexOption.IGNORE_CASE)

    private val cardRegex = Regex(
        """(?:\*+|\bterminad[ao]\s+en\s+|\btermina\s+en\s+|\bfinalizad[ao]\s+en\s+|x{2,})\s?(\d{4})\b""",
        RegexOption.IGNORE_CASE
    )

    private val badStarts = listOf(
        "tu ", "su ", "la ", "el ", "los ", "las ", "este", "esta", "linea", "cuota", "nuestra",
        "app", "cuenta", "tarjeta", "credito", "debito", "las ", "momento", "nombre", "valor",
    )

    fun isCandidateSource(pkg: String, broad: Boolean): Boolean {
        if (broad) return true
        val p = pkg.lowercase()
        return sourceHints.any { p.contains(it) }
    }

    /** Convierte "45.900,00", "45,900.00", "45900" o "1.234.567" en número */
    fun parseNumber(raw: String): Double? {
        val s = raw.trim().trimEnd('.', ',')
        if (s.isEmpty()) return null
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')
        val normalized = when {
            lastDot >= 0 && lastComma >= 0 ->
                if (lastComma > lastDot) s.replace(".", "").replace(',', '.') else s.replace(",", "")
            lastComma >= 0 -> {
                val decimals = s.length - lastComma - 1
                if (decimals in 1..2 && s.count { it == ',' } == 1) s.replace(',', '.') else s.replace(",", "")
            }
            lastDot >= 0 -> {
                val decimals = s.length - lastDot - 1
                if (decimals in 1..2 && s.count { it == '.' } == 1) s else s.replace(".", "")
            }
            else -> s
        }
        return normalized.toDoubleOrNull()
    }

    fun parse(text: String, pkg: String = ""): ParsedTx? {
        if (text.isBlank()) return null
        val n = text.norm()
        if (rejectWords.any { n.contains(it) }) return null

        val type = when {
            incomeWords.any { n.contains(it) } -> TxType.INCOME
            expenseWords.any { n.contains(it) } -> TxType.EXPENSE
            else -> return null
        }

        val m = amountWithSymbol.find(text) ?: amountWithWord.find(text) ?: return null
        val amount = parseNumber(m.groupValues[1]) ?: return null
        if (amount <= 0.0) return null

        val card = cardRegex.find(text)?.groupValues?.get(1).orEmpty()

        val method = when {
            Regex("""t\.?\s?cred|credito""").containsMatchIn(n) -> "Crédito"
            Regex("""t\.?\s?deb|debito""").containsMatchIn(n) -> "Débito"
            n.contains("pse") -> "PSE"
            n.contains("transfer") || n.contains("enviaste") || n.contains("te enviaron") -> "Transferencia"
            n.contains("retir") -> "Efectivo"
            else -> "Tarjeta"
        }

        val bank = bankNames.firstOrNull { (k, _) -> (pkg.lowercase() + " " + n).contains(k) }?.second.orEmpty()

        val merchant = extractMerchant(text, m.range.last, type)
        val fallback = when {
            type == TxType.INCOME -> "Ingreso"
            method == "Efectivo" -> "Retiro"
            method == "Transferencia" -> "Transferencia"
            else -> "Compra"
        }
        val finalMerchant = merchant.ifBlank { if (bank.isNotBlank()) "$fallback $bank" else fallback }
        val category = Categories.guess("$finalMerchant $n", type)

        return ParsedTx(amount, type, finalMerchant, card, method, bank, category)
    }

    private fun candidates(regex: Regex, text: String): List<Pair<Int, String>> =
        regex.findAll(text).mapNotNull { r ->
            val raw = r.groupValues[1].trim()
            val nn = raw.norm()
            when {
                raw.length < 2 -> null
                raw.first().isDigit() -> null
                raw.contains('$') -> null
                badStarts.any { nn.startsWith(it) } -> null
                else -> r.range.first to raw
            }
        }.toList()

    private fun extractMerchant(text: String, afterIndex: Int, type: TxType): String {
        val preferred = if (type == TxType.INCOME) listOf(deRegex, enRegex) else listOf(enRegex, aRegex)
        for (rx in preferred) {
            val c = candidates(rx, text)
            val pick = c.firstOrNull { it.first > afterIndex } ?: c.firstOrNull()
            if (pick != null) return clean(pick.second)
        }
        return ""
    }

    private fun clean(raw: String): String {
        var s = raw.trim()
        if (s.contains('*')) {
            val before = s.substringBefore('*').trim()
            s = if (before.length >= 3) before else s.replace('*', ' ')
        }
        s = s.replace(Regex("\\s+"), " ").trim(' ', '.', ',', '-', ':')
        if (s.length > 32) s = s.take(32).trim()
        return s.split(' ').joinToString(" ") { w ->
            if (w.length <= 3 && w.any { it.isDigit() }) w.uppercase()
            else w.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
