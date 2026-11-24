package com.aurum.finanzas.data

import java.util.Calendar
import java.util.Random
import kotlin.math.roundToLong

/** Datos de ejemplo para ver la app "viva" antes de tener movimientos reales */
object DemoData {
    fun seed() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val rnd = Random(7)
        val shops = listOf(
            Triple("Éxito", "mercado", 165_000.0),
            Triple("D1", "mercado", 48_000.0),
            Triple("Carulla", "mercado", 92_000.0),
            Triple("Rappi", "comida", 43_900.0),
            Triple("Juan Valdez", "comida", 14_500.0),
            Triple("Crepes & Waffles", "comida", 68_000.0),
            Triple("Uber", "transporte", 18_700.0),
            Triple("Terpel", "transporte", 120_000.0),
            Triple("Netflix", "suscripciones", 38_900.0),
            Triple("Spotify", "suscripciones", 16_900.0),
            Triple("Claro", "servicios", 89_900.0),
            Triple("Farmatodo", "salud", 37_400.0),
            Triple("Falabella", "compras", 219_000.0),
            Triple("Cine Colombia", "ocio", 52_000.0),
        )
        val list = ArrayList<Transaction>()
        repeat(70) {
            val (name, cat, base) = shops[rnd.nextInt(shops.size)]
            val t = now - (rnd.nextDouble() * 88 * day).toLong()
            val amount = ((base * (0.65 + rnd.nextDouble() * 0.7)) / 100.0).roundToLong() * 100.0
            list += Transaction(
                amount = amount, type = TxType.EXPENSE, categoryId = cat, merchant = name,
                paymentMethod = if (rnd.nextBoolean()) "Crédito" else "Débito",
                cardLast4 = if (rnd.nextBoolean()) "4821" else "7730",
                bank = "Bancolombia", dateMillis = t,
                source = if (rnd.nextInt(4) > 0) TxSource.AUTO else TxSource.MANUAL,
            )
        }
        for (m in 0..2) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -m)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 8)
            if (c.timeInMillis <= now) {
                list += Transaction(
                    amount = 3_200_000.0, type = TxType.INCOME, categoryId = "salario",
                    merchant = "Nómina", paymentMethod = "Transferencia", bank = "Bancolombia",
                    dateMillis = c.timeInMillis, source = TxSource.AUTO,
                )
            }
            c.set(Calendar.DAY_OF_MONTH, 12)
            if (c.timeInMillis <= now) {
                list += Transaction(
                    amount = 150_000.0, type = TxType.INCOME, categoryId = "transfer_in",
                    merchant = "Transferencia Nequi", paymentMethod = "Transferencia", bank = "Nequi",
                    dateMillis = c.timeInMillis, source = TxSource.AUTO,
                )
            }
        }
        Repo.addAll(list)
    }
}
