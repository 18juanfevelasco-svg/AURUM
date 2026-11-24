package com.aurum.finanzas.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurum.finanzas.ui.PermState
import com.aurum.finanzas.ui.SettingsState
import com.aurum.finanzas.ui.components.*
import com.aurum.finanzas.ui.readPerms
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum

@Composable
fun Onboarding(
    settings: SettingsState,
    perms: MutableState<PermState>,
    openListenerSettings: () -> Unit,
    openAppInfo: () -> Unit,
    onFinish: () -> Unit,
) {
    val c = Aurum.colors
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(settings.name) }
    var budget by rememberSaveable { mutableStateOf(if (settings.budget > 0) settings.budget.toString() else "") }

    val postLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        perms.value = readPerms(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp)
    ) {
        // Indicador de pasos
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(3) { i ->
                val w by animateDpAsState(if (i == step) 28.dp else 8.dp, spring(dampingRatio = 0.7f, stiffness = 400f), label = "dot")
                val col by animateColorAsState(if (i <= step) c.gold else c.line, label = "dotColor")
                Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(col))
                Spacer(Modifier.width(6.dp))
            }
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val fwd = targetState > initialState
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 300f)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(350))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(200)))
            },
            label = "onbStep"
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                when (s) {
                    0 -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AurumMark(104.dp) }
                        Spacer(Modifier.height(28.dp))
                        Text("Aurum", style = AType.display.copy(fontSize = 52.sp), color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text("Tus finanzas, con calma y claridad.", style = AType.body, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(36.dp))
                        Feature(Icons.Rounded.AutoAwesome, "Registro automático", "Cada compra con tus tarjetas se anota sola.")
                        Feature(Icons.Rounded.PieChart, "Claridad total", "Mira en qué se va tu dinero, mes a mes.")
                        Feature(Icons.Rounded.Lock, "Privado", "Todo se guarda solo en tu celular.")
                    }
                    1 -> {
                        Text("Hagámoslo tuyo", style = AType.title, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text("Puedes cambiar esto cuando quieras en Ajustes.", style = AType.caption, color = c.muted)
                        Spacer(Modifier.height(24.dp))
                        Text("¿CÓMO TE LLAMAS?", style = AType.label, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        AurumTextField(name, { name = it.take(24) }, "Tu nombre", leading = Icons.Rounded.Person)
                        Spacer(Modifier.height(20.dp))
                        Text("PRESUPUESTO MENSUAL (OPCIONAL)", style = AType.label, color = c.muted)
                        Spacer(Modifier.height(8.dp))
                        AurumTextField(
                            budget, { v -> budget = v.filter { it.isDigit() }.take(12) }, "Ej: 2500000",
                            keyboardType = KeyboardType.Number, leading = Icons.Rounded.AccountBalanceWallet
                        )
                    }
                    else -> {
                        Text("Activa la magia", style = AType.title, color = c.ink)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Para registrar tus compras solas, Aurum necesita leer las notificaciones (y SMS) de tu banco. Solo procesa avisos de compras e ingresos.",
                            style = AType.caption, color = c.muted
                        )
                        Spacer(Modifier.height(22.dp))
                        PermissionCard(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "Acceso a notificaciones",
                            text = "Busca \"Aurum\" en la lista y actívalo.",
                            granted = perms.value.listener,
                            action = "Activar",
                            onAction = openListenerSettings
                        )
                        if (Build.VERSION.SDK_INT >= 33) {
                            Spacer(Modifier.height(12.dp))
                            PermissionCard(
                                icon = Icons.Rounded.Notifications,
                                title = "Avisos de Aurum",
                                text = "Te confirma cada gasto registrado.",
                                granted = perms.value.post,
                                action = "Permitir",
                                onAction = { postLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        AurumCard(background = c.goldSoft, padding = 16.dp) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Rounded.Info, null, tint = c.gold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Si Android dice que el ajuste está restringido: abre la información de la app, toca ⋮ arriba a la derecha y elige \"Permitir ajustes restringidos\". Luego vuelve a intentarlo.",
                                    style = AType.caption, color = c.ink
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            SecondaryButton("Abrir información de la app", openAppInfo, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = when (step) {
                0 -> "Comenzar"
                1 -> "Continuar"
                else -> if (perms.value.listener) "Entrar a Aurum" else "Lo haré después"
            },
            onClick = {
                when (step) {
                    0 -> step = 1
                    1 -> {
                        settings.name = name.trim()
                        settings.budget = budget.toLongOrNull() ?: 0L
                        step = 2
                    }
                    else -> onFinish()
                }
            }
        )
        if (step > 0) {
            Text(
                "Atrás",
                style = AType.bodyStrong.copy(fontSize = 14.sp),
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).bounceClick { step -= 1 }
            )
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    val c = Aurum.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(c.goldSoft),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = c.gold, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = AType.bodyStrong, color = c.ink)
            Text(text, style = AType.caption, color = c.muted)
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    text: String,
    granted: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    val c = Aurum.colors
    AurumCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(if (granted) c.income.copy(alpha = 0.15f) else c.goldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (granted) Icons.Rounded.CheckCircle else icon, null, tint = if (granted) c.income else c.gold, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = AType.bodyStrong, color = c.ink)
                Text(if (granted) "Listo" else text, style = AType.caption, color = c.muted)
            }
            AnimatedVisibility(visible = !granted, enter = fadeIn(), exit = fadeOut() + shrinkHorizontally()) {
                SecondaryButton(action, onAction, color = c.gold)
            }
        }
    }
}
