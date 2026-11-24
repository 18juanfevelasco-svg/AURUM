package com.aurum.finanzas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.*
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StatsScreen(txs: List<Transaction>) {
    val c = Aurum.colors
    var ym by remember { mutableStateOf(YM.now()) }
    val month = remember(txs, ym) { txs.inMonth(ym) }
    val expenses = remember(month) { month.filter { it.type == TxType.EXPENSE } }
    val total = expenses.sumOf { it.amount }
    val income = month.totalIncome()

    val byCat = remember(expenses) {
        expenses.groupBy { it.categoryId }
            .map { (id, l) -> Categories.byId(id) to l.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val months6 = remember(ym) {
        (5 downTo 0).map { i ->
            var m = ym
            repeat(i) { m = m.prev() }
            m
        }
    }
    val values6 = remember(txs, months6) { months6.map { m -> txs.inMonth(m).totalExpense() } }
    val prevTotal = values6.getOrElse(4) { 0.0 }

    val biggest = expenses.maxByOrNull { it.amount }
    val avgTicket = if (expenses.isNotEmpty()) total / expenses.size else 0.0
    val topDay = expenses
        .groupBy { Calendar.getInstance().apply { timeInMillis = it.dateMillis }.get(Calendar.DAY_OF_WEEK) }
        .maxByOrNull { (_, l) -> l.sumOf { it.amount } }?.key
    val autoCount = month.count { it.source == TxSource.AUTO }
    val topMerchant = expenses.groupBy { it.merchant }.maxByOrNull { it.value.size }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 14.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") {
            Text("Análisis", style = AType.title, color = c.ink, modifier = Modifier.fadeUpIn(0))
        }
        item(key = "month") {
            AurumCard(Modifier.fadeUpIn(1), padding = 12.dp) { MonthSelector(ym) { ym = it } }
        }

        item(key = "donut") {
            AurumCard(Modifier.fadeUpIn(2)) {
                Box(Modifier.fillMaxWidth().height(232.dp), contentAlignment = Alignment.Center) {
                    DonutChart(
                        values = byCat.map { it.second.toFloat() },
                        colors = byCat.map { it.first.color },
                        track = c.surfaceAlt,
                        modifier = Modifier.size(214.dp),
                        stroke = 18.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL GASTADO", style = AType.label.copy(fontSize = 10.sp), color = c.muted)
                        Spacer(Modifier.height(4.dp))
                        AnimatedMoney(total, AType.displaySmall.copy(fontSize = 24.sp), c.ink)
                        Spacer(Modifier.height(2.dp))
                        Text("${expenses.size} movimientos", style = AType.caption, color = c.muted)
                    }
                }
                if (byCat.isEmpty()) {
                    Text(
                        "No hay gastos en este mes.", style = AType.caption, color = c.muted,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    Spacer(Modifier.height(18.dp))
                    byCat.forEachIndexed { i, (cat, amount) ->
                        if (i > 0) Spacer(Modifier.height(14.dp))
                        val pct = if (total > 0) amount / total else 0.0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(cat, 34.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.name, style = AType.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
                                    Text("${(pct * 100).roundToInt()}%", style = AType.caption, color = c.muted)
                                    Spacer(Modifier.width(10.dp))
                                    Text(Format.money(amount), style = AType.amount, color = c.ink)
                                }
                                Spacer(Modifier.height(6.dp))
                                ProgressLine(pct.toFloat(), cat.color, c.surfaceAlt, height = 4.dp)
                            }
                        }
                    }
                }
            }
        }

        item(key = "bars") {
            AurumCard {
                SectionHeader("Últimos 6 meses")
                Spacer(Modifier.height(4.dp))
                if (prevTotal > 0) {
                    val diff = (total - prevTotal) / prevTotal * 100
                    val up = diff >= 0
                    Text(
                        "${if (up) "+" else "−"}${abs(diff).roundToInt()}% frente a ${Format.monthName(ym.prev().month).lowercase()}",
                        style = AType.caption,
                        color = if (up) c.expense else c.income
                    )
                }
                Spacer(Modifier.height(16.dp))
                BarChart(
                    values = values6,
                    labels = months6.map { Format.monthShort(it) },
                    highlight = 5,
                    barColor = c.surfaceAlt,
                    highlightColor = c.gold,
                    labelColor = c.muted,
                    highlightLabelColor = c.ink
                )
            }
        }

        item(key = "insights") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Insight(Icons.Rounded.TrendingUp, "Mayor gasto",
                        biggest?.let { Format.compact(it.amount) } ?: "—",
                        biggest?.merchant ?: "Sin datos", Modifier.weight(1f))
                    Insight(Icons.Rounded.Receipt, "Ticket promedio",
                        Format.compact(avgTicket), "${expenses.size} compras", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Insight(Icons.Rounded.CalendarToday, "Día más costoso",
                        topDay?.let { Format.weekday(it) } ?: "—", "según este mes", Modifier.weight(1f))
                    Insight(Icons.Rounded.Storefront, "Comercio frecuente",
                        topMerchant?.key ?: "—",
                        topMerchant?.let { "${it.value.size} visitas" } ?: "Sin datos", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Insight(Icons.Rounded.AutoAwesome, "Automáticos",
                        "$autoCount", "registrados solos", Modifier.weight(1f))
                    Insight(Icons.Rounded.Savings, "Ahorro del mes",
                        Format.compact(income - total),
                        if (income > 0) "${((income - total) / income * 100).roundToInt()}% de tus ingresos" else "sin ingresos",
                        Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun Insight(icon: ImageVector, label: String, value: String, sub: String, modifier: Modifier) {
    val c = Aurum.colors
    AurumCard(modifier, padding = 16.dp) {
        Icon(icon, null, tint = c.gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(10.dp))
        Text(label.uppercase(), style = AType.label.copy(fontSize = 9.5.sp), color = c.muted, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = AType.bodyStrong.copy(fontSize = 17.sp), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = AType.caption.copy(fontSize = 11.5.sp), color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
