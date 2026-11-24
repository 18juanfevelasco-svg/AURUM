package com.aurum.finanzas.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.*
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format
import java.util.Calendar
import kotlin.math.roundToLong

private val METHODS = listOf("Débito", "Crédito", "Efectivo", "Transferencia", "PSE")

@Composable
fun EditScreen(existing: Transaction?, initialType: TxType, onClose: () -> Unit) {
    val c = Aurum.colors
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    var type by remember { mutableStateOf(existing?.type ?: initialType) }
    var amountText by remember { mutableStateOf(existing?.amount?.roundToLong()?.toString() ?: "") }
    var merchant by remember { mutableStateOf(existing?.merchant ?: "") }
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: Categories.defaultFor(initialType)) }
    var categoryTouched by remember { mutableStateOf(existing != null) }
    var method by remember { mutableStateOf(existing?.paymentMethod?.takeIf { it.isNotBlank() } ?: "Débito") }
    var date by remember { mutableLongStateOf(existing?.dateMillis ?: System.currentTimeMillis()) }
    var textFocused by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val amount = amountText.toLongOrNull() ?: 0L

    // Sugerir categoría a partir del comercio mientras escribes
    LaunchedEffect(merchant, type) {
        if (!categoryTouched && merchant.length >= 2) categoryId = Categories.guess(merchant, type)
    }
    LaunchedEffect(type) {
        if (Categories.byId(categoryId).type != type) categoryId = Categories.defaultFor(type)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Barra superior
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(Icons.Rounded.Close, onClose)
            Text(
                if (existing == null) "Nuevo movimiento" else "Editar movimiento",
                style = AType.heading, color = c.ink,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
            )
            if (existing != null) IconCircle(Icons.Rounded.DeleteOutline, { confirmDelete = true }, tint = c.expense)
            else Spacer(Modifier.size(42.dp))
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            SegmentedToggle(
                options = listOf("Gasto", "Ingreso"),
                selected = type.ordinal,
                onSelect = { type = TxType.entries[it] },
                activeColor = if (type == TxType.EXPENSE) c.ink else c.income,
                activeText = if (type == TxType.EXPENSE) c.bg else androidx.compose.ui.graphics.Color.White,
            )
            Spacer(Modifier.height(14.dp))
            // Monto grande; al tocarlo vuelve a aparecer el teclado numérico
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.clearFocus() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (type == TxType.EXPENSE) "¿CUÁNTO GASTASTE?" else "¿CUÁNTO RECIBISTE?",
                    style = AType.label, color = c.muted
                )
                Spacer(Modifier.height(4.dp))
                AnimatedContent(
                    targetState = Format.money(amount.toDouble()),
                    transitionSpec = {
                        (slideInVertically(tween(180)) { it / 3 } + fadeIn(tween(180))) togetherWith
                            (slideOutVertically(tween(140)) { -it / 3 } + fadeOut(tween(120))) using SizeTransform(clip = false)
                    },
                    label = "amount"
                ) { s ->
                    Text(
                        s,
                        style = AType.display.copy(fontSize = if (s.length > 12) 38.sp else 46.sp),
                        color = if (amount == 0L) c.faint else if (type == TxType.EXPENSE) c.ink else c.income,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Detalles (se pueden desplazar)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            AurumTextField(
                value = merchant, onChange = { merchant = it.take(40) },
                placeholder = "Comercio o descripción",
                leading = Icons.Rounded.Storefront,
                onFocus = { textFocused = it }
            )
            Spacer(Modifier.height(10.dp))
            AurumTextField(
                value = note, onChange = { note = it.take(120) },
                placeholder = "Nota (opcional)",
                leading = Icons.Rounded.EditNote,
                onFocus = { textFocused = it }
            )

            Spacer(Modifier.height(18.dp))
            Text("CATEGORÍA", style = AType.label, color = c.muted)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(Categories.forType(type), key = { it.id }) { cat ->
                    CategoryPick(cat, cat.id == categoryId) {
                        categoryId = cat.id
                        categoryTouched = true
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text("MÉTODO DE PAGO", style = AType.label, color = c.muted)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(METHODS) { m -> SelectChip(m, method == m, { method = m }) }
            }

            Spacer(Modifier.height(18.dp))
            AurumCard(
                Modifier.bounceClick {
                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            val n = Calendar.getInstance().apply { timeInMillis = date }
                            n.set(y, m, d)
                            date = n.timeInMillis
                        },
                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                padding = 16.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarToday, null, tint = c.gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fecha", style = AType.caption, color = c.muted)
                        Text(Format.longDate(date), style = AType.bodyStrong, color = c.ink)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = c.faint)
                }
            }

            if (existing != null && existing.source == TxSource.AUTO && existing.rawText.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                AurumCard(padding = 16.dp, background = c.goldSoft) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = c.gold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Registrado desde notificación", style = AType.bodyStrong.copy(fontSize = 13.sp), color = c.ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(existing.rawText, style = AType.caption, color = c.muted)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Teclado numérico propio (se oculta mientras escribes texto)
        AnimatedVisibility(
            visible = !textFocused,
            enter = expandVertically(spring(dampingRatio = 0.9f, stiffness = 400f)) + fadeIn(),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(150))
        ) {
            Keypad(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { k ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                amountText = when (k) {
                    "⌫" -> amountText.dropLast(1)
                    else -> {
                        if (amountText.isEmpty() && k.startsWith("0")) amountText
                        else (amountText + k).take(12)
                    }
                }
            }
        }

        PrimaryButton(
            text = if (existing == null) "Guardar" else "Guardar cambios",
            enabled = amount > 0,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
            onClick = {
                val tx = Transaction(
                    id = existing?.id ?: 0,
                    amount = amount.toDouble(),
                    type = type,
                    categoryId = categoryId,
                    merchant = merchant.trim().ifBlank { Categories.byId(categoryId).name },
                    note = note.trim(),
                    cardLast4 = existing?.cardLast4 ?: "",
                    bank = existing?.bank ?: "",
                    paymentMethod = method,
                    dateMillis = date,
                    source = existing?.source ?: TxSource.MANUAL,
                    rawText = existing?.rawText ?: "",
                )
                if (existing == null) Repo.add(tx) else Repo.update(tx)
                onClose()
            }
        )
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surface,
            titleContentColor = c.ink,
            textContentColor = c.muted,
            title = { Text("¿Eliminar movimiento?", style = AType.heading) },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repo.delete(existing.id)
                    onClose()
                }) { Text("Eliminar", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = c.muted) }
            }
        )
    }
}

@Composable
private fun Keypad(modifier: Modifier = Modifier, onKey: (String) -> Unit) {
    val c = Aurum.colors
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "⌫")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(52.dp)
                            .bounceClick { onKey(k) }
                            .clip(RoundedCornerShape(16.dp))
                            .background(c.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        if (k == "⌫") Icon(Icons.Rounded.Backspace, "Borrar", tint = c.ink, modifier = Modifier.size(22.dp))
                        else Text(k, style = AType.heading.copy(fontSize = 22.sp), color = c.ink)
                    }
                }
            }
        }
    }
}
