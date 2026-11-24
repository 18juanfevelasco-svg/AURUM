package com.aurum.finanzas.data

import java.text.Normalizer
import java.util.Calendar

/** Tipo de movimiento */
enum class TxType { EXPENSE, INCOME }

/** De dónde salió el movimiento: leído de una notificación o agregado a mano */
enum class TxSource { AUTO, MANUAL }

/**
 * Un movimiento de dinero. Estos son todos los datos que la app guarda por cada uno.
 */
data class Transaction(
    val id: Long = 0,
    val amount: Double,                 // Monto en pesos
    val type: TxType,                   // Gasto o ingreso
    val categoryId: String,             // Categoría (mercado, comida, transporte...)
    val merchant: String,               // Comercio o descripción
    val note: String = "",              // Nota opcional
    val cardLast4: String = "",         // Últimos 4 dígitos de la tarjeta
    val bank: String = "",              // Banco que envió la notificación
    val paymentMethod: String = "",     // Débito, Crédito, Efectivo, Transferencia, PSE
    val dateMillis: Long,               // Fecha y hora
    val source: TxSource = TxSource.MANUAL,
    val rawText: String = "",           // Texto original de la notificación
)

/** Quita tildes y pasa a minúsculas para comparar textos */
fun String.norm(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

/** Año + mes (mes de 0 a 11) */
data class YM(val year: Int, val month: Int) {
    fun prev(): YM = if (month == 0) YM(year - 1, 11) else YM(year, month - 1)
    fun next(): YM = if (month == 11) YM(year + 1, 0) else YM(year, month + 1)
    fun contains(millis: Long): Boolean = of(millis) == this
    fun index(): Int = year * 12 + month

    companion object {
        fun now(): YM = of(System.currentTimeMillis())
        fun of(millis: Long): YM {
            val c = Calendar.getInstance()
            c.timeInMillis = millis
            return YM(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }
    }
}

fun startOfDay(millis: Long): Long {
    val c = Calendar.getInstance()
    c.timeInMillis = millis
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

fun List<Transaction>.inMonth(ym: YM): List<Transaction> = filter { ym.contains(it.dateMillis) }
fun List<Transaction>.totalExpense(): Double = filter { it.type == TxType.EXPENSE }.sumOf { it.amount }
fun List<Transaction>.totalIncome(): Double = filter { it.type == TxType.INCOME }.sumOf { it.amount }
