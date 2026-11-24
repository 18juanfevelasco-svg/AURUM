package com.aurum.finanzas.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.data.*
import com.aurum.finanzas.notif.NotificationParser
import com.aurum.finanzas.notif.ParsedTx
import com.aurum.finanzas.ui.PermState
import com.aurum.finanzas.ui.SettingsState
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.readPerms
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum
import com.aurum.finanzas.util.Format

@Composable
fun SettingsScreen(
    settings: SettingsState,
    txs: List<Transaction>,
    perms: MutableState<PermState>,
    openListenerSettings: () -> Unit,
    openAppInfo: () -> Unit,
    onSetBudget: () -> Unit,
) {
    val c = Aurum.colors
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    var testText by remember { mutableStateOf("") }
    var tested by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ParsedTx?>(null) }

    val postLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        perms.value = readPerms(context)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(Format.csv(txs).toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) "Archivo exportado" else "No se pudo exportar", Toast.LENGTH_SHORT).show()
        }
    }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = top + 14.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "title") { Text("Ajustes", style = AType.title, color = c.ink, modifier = Modifier.fadeUpIn(0)) }

        // Perfil
        item(key = "profile") {
            AurumCard(Modifier.fadeUpIn(1)) {
                SectionLabel("PERFIL")
                AurumTextField(settings.name, { settings.name = it.take(24) }, "Tu nombre", leading = Icons.Rounded.Person)
                Spacer(Modifier.height(12.dp))
                SettingRow(
                    icon = Icons.Rounded.AccountBalanceWallet,
                    title = "Presupuesto mensual",
                    subtitle = if (settings.budget > 0) Format.money(settings.budget.toDouble()) else "Sin definir",
                    onClick = onSetBudget
                )
            }
        }

        // Captura automática
        item(key = "capture") {
            AurumCard(Modifier.fadeUpIn(2)) {
                SectionLabel("CAPTURA AUTOMÁTICA")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(perms.value.listener)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Acceso a notificaciones", style = AType.bodyStrong, color = c.ink)
                        Text(
                            if (perms.value.listener) "Activo: Aurum lee los avisos de tu banco" else "Inactivo: tus compras no se registrarán solas",
                            style = AType.caption, color = c.muted
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                SecondaryButton(
                    if (perms.value.listener) "Revisar acceso" else "Activar acceso",
                    openListenerSettings, icon = Icons.Rounded.NotificationsActive, color = c.gold,
                    modifier = Modifier.fillMaxWidth()
                )
                if (Build.VERSION.SDK_INT >= 33) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(perms.value.post)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Avisos de Aurum", style = AType.bodyStrong, color = c.ink)
                            Text("Para confirmarte cada gasto registrado", style = AType.caption, color = c.muted)
                        }
                        if (!perms.value.post) {
                            TextButton(onClick = { postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Permitir", color = c.gold)
                            }
                        }
                    }
                }
                Divider()
                ToggleRow("Registrar compras automáticamente", "Lee montos, comercio y tarjeta", settings.autoCapture) { settings.autoCapture = it }
                ToggleRow("Avisarme cada registro", "Notificación \"Gasto registrado\"", settings.notifyOnCapture) { settings.notifyOnCapture = it }
                ToggleRow("Escuchar todas las apps", "Útil si tu banco no es detectado", settings.broadMode) { settings.broadMode = it }
                Divider()
                Text(
                    "¿No te deja activar el acceso? En Android 13 o superior, abre la información de la app, toca ⋮ (arriba a la derecha) y elige \"Permitir ajustes restringidos\".",
                    style = AType.caption, color = c.muted
                )
                Spacer(Modifier.height(10.dp))
                SecondaryButton("Abrir información de la app", openAppInfo, icon = Icons.Rounded.Info, modifier = Modifier.fillMaxWidth())
            }
        }

        // Probador del detector
        item(key = "tester") {
            AurumCard {
                SectionLabel("PROBAR DETECTOR")
                Text(
                    "Pega el texto de una notificación o SMS de tu banco para ver qué entiende Aurum.",
                    style = AType.caption, color = c.muted
                )
                Spacer(Modifier.height(12.dp))
                AurumTextField(testText, { testText = it; tested = false }, "Ej: Compraste \$45.900 en RAPPI con tu T.Cred *1234", singleLine = false, minLines = 3)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryButton("Analizar", {
                        testResult = NotificationParser.parse(testText)
                        tested = true
                    }, icon = Icons.Rounded.Science, color = c.gold, modifier = Modifier.weight(1f))
                    SecondaryButton("Ejemplo", {
                        testText = "Bancolombia: Compraste \$45.900,00 en RAPPI*RAPPI COLOMBIA con tu T.Cred *1234, el 21/11/2026 a las 20:15."
                        tested = false
                    }, modifier = Modifier.weight(1f))
                }
                AnimatedVisibility(
                    visible = tested,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(Modifier.padding(top = 14.dp)) {
                        val r = testResult
                        if (r == null) {
                            Text("No se detectó una compra ni un ingreso en ese texto.", style = AType.bodyStrong, color = c.expense)
                        } else {
                            ResultLine("Tipo", if (r.type == TxType.EXPENSE) "Gasto" else "Ingreso")
                            ResultLine("Monto", Format.money(r.amount))
                            ResultLine("Comercio", r.merchant)
                            ResultLine("Categoría", Categories.byId(r.categoryId).name)
                            ResultLine("Método", r.method)
                            if (r.cardLast4.isNotBlank()) ResultLine("Tarjeta", "•${r.cardLast4}")
                            if (r.bank.isNotBlank()) ResultLine("Banco", r.bank)
                            Spacer(Modifier.height(10.dp))
                            SecondaryButton("Guardar como movimiento", {
                                Repo.add(
                                    Transaction(
                                        amount = r.amount, type = r.type, categoryId = r.categoryId,
                                        merchant = r.merchant, cardLast4 = r.cardLast4, bank = r.bank,
                                        paymentMethod = r.method, dateMillis = System.currentTimeMillis(),
                                        source = TxSource.AUTO, rawText = testText,
                                    )
                                )
                                Toast.makeText(context, "Movimiento guardado", Toast.LENGTH_SHORT).show()
                                tested = false
                                testText = ""
                            }, icon = Icons.Rounded.Add, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        // Apariencia
        item(key = "theme") {
            AurumCard {
                SectionLabel("APARIENCIA")
                SegmentedToggle(
                    options = listOf("Sistema", "Claro", "Oscuro"),
                    selected = settings.themeMode,
                    onSelect = { settings.themeMode = it }
                )
            }
        }

        // Datos
        item(key = "data") {
            AurumCard {
                SectionLabel("TUS DATOS")
                SettingRow(Icons.Rounded.FileDownload, "Exportar a Excel (CSV)", "${txs.size} movimientos") {
                    exportLauncher.launch("aurum_movimientos.csv")
                }
                Spacer(Modifier.height(6.dp))
                SettingRow(Icons.Rounded.AutoAwesome, "Cargar datos de ejemplo", "Para ver cómo luce la app con información") {
                    DemoData.seed()
                    Toast.makeText(context, "Datos de ejemplo cargados", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(6.dp))
                SettingRow(Icons.Rounded.DeleteForever, "Borrar todos los movimientos", "No se puede deshacer", danger = true) {
                    confirmClear = true
                }
            }
        }

        item(key = "footer") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                AurumMark(44.dp)
                Spacer(Modifier.height(6.dp))
                Text("Aurum · versión 1.0", style = AType.caption, color = c.faint)
                Text("Tus datos se guardan solo en este celular.", style = AType.caption, color = c.faint)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = c.surface,
            titleContentColor = c.ink,
            textContentColor = c.muted,
            title = { Text("¿Borrar todo?", style = AType.heading) },
            text = { Text("Se eliminarán todos tus movimientos de este celular.") },
            confirmButton = {
                TextButton(onClick = { Repo.clear(); confirmClear = false }) { Text("Borrar", color = c.expense) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancelar", color = c.muted) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = AType.label, color = Aurum.colors.muted)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Divider() {
    Spacer(Modifier.height(14.dp))
    Box(Modifier.fillMaxWidth().height(1.dp).background(Aurum.colors.line))
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun StatusDot(ok: Boolean) {
    val c = Aurum.colors
    Box(
        Modifier.size(10.dp).clip(CircleShape).background(if (ok) c.income else c.expense)
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = Aurum.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = AType.bodyStrong, color = c.ink)
            Text(subtitle, style = AType.caption, color = c.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.surface,
                checkedTrackColor = c.gold,
                checkedBorderColor = c.gold,
                uncheckedThumbColor = c.faint,
                uncheckedTrackColor = c.surfaceAlt,
                uncheckedBorderColor = c.line,
            )
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    val c = Aurum.colors
    val tint = if (danger) c.expense else c.gold
    Row(
        Modifier.fillMaxWidth().bounceClick { onClick() }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AType.bodyStrong, color = if (danger) c.expense else c.ink)
            Text(subtitle, style = AType.caption, color = c.muted)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = c.faint)
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    val c = Aurum.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = AType.caption.copy(fontSize = 13.sp), color = c.muted, modifier = Modifier.width(90.dp))
        Text(value, style = AType.bodyStrong.copy(fontSize = 14.sp), color = c.ink)
    }
}
