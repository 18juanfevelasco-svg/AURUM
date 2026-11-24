package com.aurum.finanzas.util

import com.aurum.finanzas.data.Transaction
import com.aurum.finanzas.data.TxSource
import com.aurum.finanzas.data.TxType
import com.aurum.finanzas.data.YM
import com.aurum.finanzas.data.Categories
import com.aurum.finanzas.data.startOfDay
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Formatos en español (Colombia): $45.900, "Hoy", "sábado 21 de noviembre"... */
object Format {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val intFmt = DecimalFormat("#,##0", symbols)
    private val decFmt = DecimalFormat("#,##0.00", symbols)

    private val months = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    private val days = listOf("domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado")

    fun money(v: Double): String {
        val a = abs(v)
        val body = if (abs(a - a.roundToLong()) < 0.005) intFmt.format(a.roundToLong()) else decFmt.format(a)
        return (if (v < -0.004) "-" else "") + "$" + body
    }

    fun compact(v: Double): String {
        val a = abs(v)
        val sign = if (v < 0) "-" else ""
        return sign + when {
            a >= 1_000_000 -> "$" + oneDecimal(a / 1_000_000) + " M"
            a >= 10_000 -> "$" + (a / 1000).roundToLong() + " mil"
            else -> money(a)
        }
    }

    private fun oneDecimal(x: Double): String {
        val r = (x * 10).roundToLong() / 10.0
        return if (r == r.roundToLong().toDouble()) r.roundToLong().toString()
        else r.toString().replace('.', ',')
    }

    fun monthName(m: Int): String = months[m].replaceFirstChar { it.uppercase() }
    fun monthYear(ym: YM): String = "${monthName(ym.month)} ${ym.year}"
    fun monthShort(ym: YM): String = months[ym.month].take(3).replaceFirstChar { it.uppercase() }
    fun weekday(calendarDow: Int): String = days[calendarDow - 1].replaceFirstChar { it.uppercase() }

    fun dayHeader(millis: Long): String {
        val today = startOfDay(System.currentTimeMillis())
        val d = startOfDay(millis)
        return when (d) {
            today -> "Hoy"
            today - 86_400_000L -> "Ayer"
            else -> {
                val c = Calendar.getInstance().apply { timeInMillis = millis }
                "${weekday(c.get(Calendar.DAY_OF_WEEK))} ${c.get(Calendar.DAY_OF_MONTH)} de ${months[c.get(Calendar.MONTH)]}"
            }
        }
    }

    fun time(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        var h = c.get(Calendar.HOUR)
        if (h == 0) h = 12
        val mm = c.get(Calendar.MINUTE).toString().padStart(2, '0')
        val ap = if (c.get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m."
        return "$h:$mm $ap"
    }

    fun longDate(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        val today = startOfDay(System.currentTimeMillis())
        val prefix = when (startOfDay(millis)) {
            today -> "Hoy, "
            today - 86_400_000L -> "Ayer, "
            else -> ""
        }
        return prefix + "${c.get(Calendar.DAY_OF_MONTH)} de ${months[c.get(Calendar.MONTH)]} de ${c.get(Calendar.YEAR)}"
    }

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    /** Archivo CSV para abrir en Excel o Google Sheets */
    fun csv(list: List<Transaction>): String {
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        val sb = StringBuilder("﻿")
        sb.append("fecha,hora,tipo,monto,categoria,comercio,metodo,tarjeta,banco,origen,nota\n")
        list.forEach { t ->
            val c = Calendar.getInstance().apply { timeInMillis = t.dateMillis }
            val date = "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
            val hour = "%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
            sb.append(listOf(
                date, hour,
                if (t.type == TxType.EXPENSE) "Gasto" else "Ingreso",
                t.amount.roundToLong().toString(),
                esc(Categories.byId(t.categoryId).name), esc(t.merchant), esc(t.paymentMethod),
                esc(t.cardLast4), esc(t.bank),
                if (t.source == TxSource.AUTO) "Automático" else "Manual",
                esc(t.note),
            ).joinToString(","))
            sb.append('\n')
        }
        return sb.toString()
    }
}
