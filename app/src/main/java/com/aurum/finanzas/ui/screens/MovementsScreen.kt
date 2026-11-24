package com.aurum.finanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.*
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format

@Composable
fun MovementsScreen(txs: List<Transaction>, onOpenTx: (Transaction) -> Unit) {
    val c = Aurum.colors
    var ym by remember { mutableStateOf(YM.now()) }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val filters = listOf("Todos", "Gastos", "Ingresos", "Automáticos")

    val monthList = remember(txs, ym) { txs.inMonth(ym) }
    val filtered = remember(monthList, filter, query) {
        val q = query.trim().norm()
        monthList.filter {
            when (filter) {
                1 -> it.type == TxType.EXPENSE
                2 -> it.type == TxType.INCOME
                3 -> it.source == TxSource.AUTO
                else -> true
            }
        }.filter {
            q.isEmpty() ||
                it.merchant.norm().contains(q) ||
                Categories.byId(it.categoryId).name.norm().contains(q) ||
                it.note.norm().contains(q) ||
                it.bank.norm().contains(q)
        }
    }
    val groups = remember(filtered) { filtered.groupBy { startOfDay(it.dateMillis) }.toList() }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 14.dp, bottom = 140.dp),
    ) {
        item(key = "title") {
            Text("Historial", style = AType.title, color = c.ink, modifier = Modifier.fadeUpIn(0))
            Spacer(Modifier.height(16.dp))
        }
        item(key = "search") {
            AurumTextField(
                value = query, onChange = { query = it },
                placeholder = "Buscar comercio, categoría o nota",
                leading = Icons.Rounded.Search,
                modifier = Modifier.fadeUpIn(1)
            )
            Spacer(Modifier.height(14.dp))
        }
        item(key = "filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fadeUpIn(2)) {
                items(filters.size) { i ->
                    SelectChip(filters[i], filter == i, { filter = i })
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        item(key = "month") {
            AurumCard(Modifier.fadeUpIn(3)) {
                MonthSelector(ym) { ym = it }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("GASTOS", style = AType.label, color = c.muted)
                        AnimatedMoney(monthList.totalExpense(), AType.displaySmall.copy(fontSize = 22.sp), c.ink)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("INGRESOS", style = AType.label, color = c.muted)
                        AnimatedMoney(monthList.totalIncome(), AType.displaySmall.copy(fontSize = 22.sp), c.income)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.Receipt,
                    title = if (query.isBlank()) "Sin movimientos este mes" else "Sin resultados",
                    text = if (query.isBlank()) "Cambia de mes o agrega un movimiento con el botón +." else "Prueba con otra palabra."
                )
            }
        }

        groups.forEach { (day, list) ->
            item(key = "h$day") {
                val net = list.sumOf { if (it.type == TxType.EXPENSE) -it.amount else it.amount }
                Row(
                    Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp).animateItem(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Format.dayHeader(day).uppercase(), style = AType.label, color = c.muted, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(CircleShape).background(c.surfaceAlt).padding(horizontal = 10.dp, vertical = 3.dp)) {
                        Text(Format.money(net), style = AType.caption, color = c.muted)
                    }
                }
            }
            items(list, key = { it.id }) { tx ->
                TxRow(tx, onClick = { onOpenTx(tx) }, modifier = Modifier.animateItem())
            }
        }
    }
}

