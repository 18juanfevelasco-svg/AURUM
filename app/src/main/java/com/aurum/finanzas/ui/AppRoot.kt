package com.aurum.finanzas.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurum.finanzas.data.Repo
import com.aurum.finanzas.data.Transaction
import com.aurum.finanzas.data.TxType
import com.aurum.finanzas.ui.components.AurumTextField
import com.aurum.finanzas.ui.components.bounceClick
import com.aurum.finanzas.ui.screens.EditScreen
import com.aurum.finanzas.ui.screens.HomeScreen
import com.aurum.finanzas.ui.screens.MovementsScreen
import com.aurum.finanzas.ui.screens.Onboarding
import com.aurum.finanzas.ui.screens.SettingsScreen
import com.aurum.finanzas.ui.screens.StatsScreen
import com.aurum.finanzas.ui.theme.AType
import com.aurum.finanzas.ui.theme.Aurum

enum class AppTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Rounded.Home),
    MOVES("Historial", Icons.Rounded.Receipt),
    STATS("Análisis", Icons.Rounded.PieChart),
    SETTINGS("Ajustes", Icons.Rounded.Tune),
}

sealed interface Route {
    data object Main : Route
    data class Edit(val tx: Transaction?, val type: TxType) : Route
}

@Composable
fun AppRoot(settings: SettingsState) {
    val c = Aurum.colors
    val context = LocalContext.current
    val txs by Repo.items.collectAsStateWithLifecycle()
    val perms = rememberPermState()

    var route by remember { mutableStateOf<Route>(Route.Main) }
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    var budgetDialog by remember { mutableStateOf(false) }

    val openListenerSettings: () -> Unit = {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    val openAppInfo: () -> Unit = {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        AnimatedContent(
            targetState = settings.onboardingDone,
            transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
            label = "onboarding"
        ) { done ->
            if (!done) {
                Onboarding(settings, perms, openListenerSettings, openAppInfo) { settings.onboardingDone = true }
            } else {
                AnimatedContent(
                    targetState = route,
                    transitionSpec = {
                        if (targetState is Route.Edit) {
                            (slideInVertically(spring(dampingRatio = 0.86f, stiffness = 380f)) { it / 5 } + fadeIn(tween(260))) togetherWith
                                (fadeOut(tween(220)) + scaleOut(tween(260), targetScale = 0.95f))
                        } else {
                            (fadeIn(tween(280)) + scaleIn(tween(320), initialScale = 0.95f)) togetherWith
                                (slideOutVertically(tween(260)) { it / 5 } + fadeOut(tween(220)))
                        }
                    },
                    label = "route"
                ) { r ->
                    when (r) {
                        Route.Main -> MainTabs(
                            tab = tab,
                            onTab = { tab = it },
                            txs = txs,
                            settings = settings,
                            perms = perms,
                            onAdd = { route = Route.Edit(null, TxType.EXPENSE) },
                            onOpenTx = { route = Route.Edit(it, it.type) },
                            openListenerSettings = openListenerSettings,
                            openAppInfo = openAppInfo,
                            onSetBudget = { budgetDialog = true },
                        )
                        is Route.Edit -> EditScreen(existing = r.tx, initialType = r.type, onClose = { route = Route.Main })
                    }
                }
            }
        }
    }

    BackHandler(enabled = route is Route.Edit) { route = Route.Main }
    BackHandler(enabled = route == Route.Main && tab != AppTab.HOME && settings.onboardingDone) { tab = AppTab.HOME }

    if (budgetDialog) {
        BudgetDialog(
            initial = settings.budget,
            onDismiss = { budgetDialog = false },
            onSave = { settings.budget = it; budgetDialog = false }
        )
    }
}

@Composable
private fun MainTabs(
    tab: AppTab,
    onTab: (AppTab) -> Unit,
    txs: List<Transaction>,
    settings: SettingsState,
    perms: MutableState<PermState>,
    onAdd: () -> Unit,
    onOpenTx: (Transaction) -> Unit,
    openListenerSettings: () -> Unit,
    openAppInfo: () -> Unit,
    onSetBudget: () -> Unit,
) {
    val c = Aurum.colors
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(spring(dampingRatio = 0.9f, stiffness = 320f)) { it / 7 * dir } + fadeIn(tween(280))) togetherWith
                    (slideOutHorizontally(tween(240)) { -it / 7 * dir } + fadeOut(tween(180)))
            },
            label = "tabs"
        ) { t ->
            when (t) {
                AppTab.HOME -> HomeScreen(
                    txs = txs,
                    settings = settings,
                    listenerEnabled = perms.value.listener,
                    onOpenTx = onOpenTx,
                    onSeeAll = { onTab(AppTab.MOVES) },
                    onEnableCapture = openListenerSettings,
                    onSetBudget = onSetBudget,
                    onAdd = onAdd,
                )
                AppTab.MOVES -> MovementsScreen(txs = txs, onOpenTx = onOpenTx)
                AppTab.STATS -> StatsScreen(txs = txs)
                AppTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    txs = txs,
                    perms = perms,
                    openListenerSettings = openListenerSettings,
                    openAppInfo = openAppInfo,
                    onSetBudget = onSetBudget,
                )
            }
        }

        // Degradado suave detrás de la barra inferior
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, c.bg.copy(alpha = 0.92f), c.bg)))
        )

        FloatingNavBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            current = tab,
            onTab = onTab,
            onAdd = onAdd
        )
    }
}

@Composable
private fun FloatingNavBar(modifier: Modifier, current: AppTab, onTab: (AppTab) -> Unit, onAdd: () -> Unit) {
    val c = Aurum.colors
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(70.dp)
            .shadow(28.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.22f))
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(AppTab.HOME, current == AppTab.HOME) { onTab(AppTab.HOME) }
        NavItem(AppTab.MOVES, current == AppTab.MOVES) { onTab(AppTab.MOVES) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(52.dp)
                    .bounceClick { onAdd() }
                    .shadow(12.dp, CircleShape, spotColor = c.gold, ambientColor = c.gold)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFFDDBF88), Color(0xFFA9824A)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, "Agregar", tint = Color(0xFF17191D), modifier = Modifier.size(28.dp))
            }
        }
        NavItem(AppTab.STATS, current == AppTab.STATS) { onTab(AppTab.STATS) }
        NavItem(AppTab.SETTINGS, current == AppTab.SETTINGS) { onTab(AppTab.SETTINGS) }
    }
}

@Composable
private fun RowScope.NavItem(tab: AppTab, selected: Boolean, onClick: () -> Unit) {
    val c = Aurum.colors
    val tint by animateColorAsState(if (selected) c.ink else c.faint, tween(250), label = "navTint")
    val dotW by animateDpAsState(if (selected) 18.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "navDot")
    val scale by animateFloatAsState(if (selected) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 500f), label = "navScale")
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            tab.icon, tab.label, tint = tint,
            modifier = Modifier.size(23.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        )
        Spacer(Modifier.height(3.dp))
        Text(tab.label, color = tint, style = AType.caption.copy(fontSize = 10.5.sp), maxLines = 1)
        Spacer(Modifier.height(3.dp))
        Box(Modifier.height(3.dp).width(dotW).clip(CircleShape).background(c.gold))
    }
}

@Composable
fun BudgetDialog(initial: Long, onDismiss: () -> Unit, onSave: (Long) -> Unit) {
    val c = Aurum.colors
    var text by remember { mutableStateOf(if (initial > 0) initial.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        titleContentColor = c.ink,
        textContentColor = c.muted,
        title = { Text("Presupuesto mensual", style = AType.heading) },
        text = {
            Column {
                Text("¿Cuánto quieres gastar como máximo cada mes?", style = AType.caption, color = c.muted)
                Spacer(Modifier.height(14.dp))
                AurumTextField(
                    value = text,
                    onChange = { v -> text = v.filter { it.isDigit() }.take(12) },
                    placeholder = "Ej: 2500000",
                    keyboardType = KeyboardType.Number,
                    leading = Icons.Rounded.AccountBalanceWallet
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text.toLongOrNull() ?: 0L) }) { Text("Guardar", color = c.gold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = c.muted) }
        }
    )
}
