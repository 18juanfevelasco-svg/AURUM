package com.aurum.finanzas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.*
import com.aurum.finanzas.ui.SettingsState
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format
import java.util.Calendar
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    txs: List<Transaction>,
    settings: SettingsState,
    listenerEnabled: Boolean,
    onOpenTx: (Transaction) -> Unit,
    onSeeAll: () -> Unit,
    onEnableCapture: () -> Unit,
    onSetBudget: () -> Unit,
    onAdd: () -> Unit,
) {
    val c = Aurum.colors
    val ym = remember { YM.now() }
    val month = remember(txs) { txs.inMonth(ym) }
    val spent = month.totalExpense()
    val income = month.totalIncome()

    val now = System.currentTimeMillis()
    val today0 = startOfDay(now)
    val todaySpent = month.filter { it.type == TxType.EXPENSE && it.dateMillis >= today0 }.sumOf { it.amount }
    val weekSpent = txs.filter { it.type == TxType.EXPENSE && it.dateMillis >= today0 - 6 * 86_400_000L }.sumOf { it.amount }
    val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val dailyAvg = spent / dayOfMonth

    val topCats = remember(month) {
        month.filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (id, l) -> Categories.byId(id) to l.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(4)
    }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 14.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth().fadeUpIn(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(Format.greeting(), style = AType.caption, color = c.muted)
                    Text(
                        settings.name.ifBlank { "Tus finanzas" },
                        style = AType.title, color = c.ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(c.goldSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        settings.name.trim().take(1).uppercase().ifBlank { "A" },
                        style = AType.displaySmall.copy(fontSize = 20.sp), color = c.gold
                    )
                }
            }
        }

        item(key = "hero") {
            Box(Modifier.fadeUpIn(1)) {
                HeroCard(
                    spent = spent, income = income, budget = settings.budget,
                    monthLabel = Format.monthName(ym.month), onSetBudget = onSetBudget
                )
            }
        }

        if (!listenerEnabled) {
            item(key = "banner") {
                Box(Modifier.fadeUpIn(2)) { CaptureBanner(onEnableCapture) }
            }
        }

        item(key = "mini") {
            Row(Modifier.fillMaxWidth().fadeUpIn(3), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("Hoy", Format.compact(todaySpent), Modifier.weight(1f))
                MiniStat("7 días", Format.compact(weekSpent), Modifier.weight(1f))
                MiniStat("Prom. diario", Format.compact(dailyAvg), Modifier.weight(1f))
            }
        }

        if (topCats.isNotEmpty()) {
            item(key = "cats") {
                AurumCard(Modifier.fadeUpIn(4)) {
                    SectionHeader("En qué se va tu dinero")
                    Spacer(Modifier.height(14.dp))
                    topCats.forEachIndexed { i, (cat, amount) ->
                        if (i > 0) Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBadge(cat, 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.name, style = AType.bodyStrong, color = c.ink, modifier = Modifier.weight(1f))
                                    Text(Format.money(amount), style = AType.amount, color = c.ink)
                                }
                                Spacer(Modifier.height(7.dp))
                                ProgressLine(
                                    fraction = if (spent > 0) (amount / spent).toFloat() else 0f,
                                    color = cat.color, track = c.surfaceAlt, height = 5.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "recentHeader") {
            SectionHeader("Recientes", if (txs.isNotEmpty()) "Ver todo" else null, onSeeAll)
        }

        if (txs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Aún no hay movimientos",
                    text = "Cuando pagues con tus tarjetas, tus compras aparecerán aquí solas. También puedes agregarlas a mano.",
                    action = "Agregar movimiento",
                    onAction = onAdd
                )
            }
        } else {
            items(txs.take(6), key = { it.id }) { tx ->
                TxRow(tx, onClick = { onOpenTx(tx) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun HeroCard(spent: Double, income: Double, budget: Long, monthLabel: String, onSetBudget: () -> Unit) {
    val c = Aurum.colors
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(22.dp, shape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.heroTop, c.heroBottom)))
    ) {
        // Anillos dorados decorativos
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 0.98f, size.height * 0.02f)
            val r = size.width * 0.44f
            drawCircle(
                Brush.radialGradient(listOf(c.gold.copy(alpha = 0.22f), Color.Transparent), center = center, radius = r),
                radius = r, center = center
            )
            drawCircle(c.gold.copy(alpha = 0.28f), radius = r * 0.92f, center = center, style = Stroke(1.dp.toPx()))
            drawCircle(c.gold.copy(alpha = 0.16f), radius = r * 0.66f, center = center, style = Stroke(1.dp.toPx()))
            drawCircle(c.gold.copy(alpha = 0.08f), radius = r * 1.2f, center = center, style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GASTADO EN ${monthLabel.uppercase()}", style = AType.label, color = c.heroMuted, modifier = Modifier.weight(1f))
                Box(
                    Modifier.clip(CircleShape).background(c.gold.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("COP", style = AType.label.copy(fontSize = 10.sp), color = c.gold) }
            }
            Spacer(Modifier.height(10.dp))
            AnimatedMoney(spent, AType.display, c.heroInk)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroStat("Ingresos", income, Color(0xFF8FD1B8), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(38.dp).background(Color.White.copy(alpha = 0.12f)))
                Spacer(Modifier.width(18.dp))
                HeroStat("Balance", income - spent, c.heroInk, Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            if (budget > 0) {
                val frac = (spent / budget).toFloat()
                val remaining = budget - spent
                ProgressLine(
                    fraction = frac,
                    color = if (frac > 1f) Color(0xFFE2867A) else c.gold,
                    track = Color.White.copy(alpha = 0.10f)
                )
                Spacer(Modifier.height(9.dp))
                Row {
                    Text("${(frac * 100).roundToInt()}% del presupuesto", style = AType.caption, color = c.heroMuted, modifier = Modifier.weight(1f))
                    Text(
                        if (remaining >= 0) "Quedan ${Format.money(remaining)}" else "Excedido ${Format.money(-remaining)}",
                        style = AType.caption.copy(fontSize = 12.5.sp),
                        color = if (remaining >= 0) c.heroInk else Color(0xFFE2867A)
                    )
                }
            } else {
                Row(
                    Modifier.bounceClick { onSetBudget() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Define tu presupuesto mensual", style = AType.bodyStrong.copy(fontSize = 13.5.sp), color = c.gold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.gold, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: Double, color: Color, modifier: Modifier) {
    val c = Aurum.colors
    Column(modifier) {
        Text(label, style = AType.caption, color = c.heroMuted)
        Spacer(Modifier.height(2.dp))
        AnimatedMoney(value, AType.bodyStrong.copy(fontSize = 17.sp), color)
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    val c = Aurum.colors
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(label.uppercase(), style = AType.label.copy(fontSize = 10.sp), color = c.muted, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Text(value, style = AType.bodyStrong.copy(fontSize = 16.sp), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CaptureBanner(onEnable: () -> Unit) {
    val c = Aurum.colors
    AurumCard(background = c.goldSoft) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(c.gold.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.NotificationsActive, null, tint = c.gold, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Activa la captura automática", style = AType.bodyStrong, color = c.ink)
                Text("Registra tus compras al instante desde las notificaciones de tu banco.", style = AType.caption, color = c.muted)
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton("Activar ahora", onEnable)
    }
}
