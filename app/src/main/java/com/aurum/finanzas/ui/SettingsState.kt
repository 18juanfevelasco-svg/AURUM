package com.aurum.finanzas.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aurum.finanzas.data.Prefs

/** Ajustes observables: si cambian, la pantalla se actualiza sola */
class SettingsState(private val prefs: Prefs) {
    private val _name = mutableStateOf(prefs.userName)
    var name: String
        get() = _name.value
        set(v) { _name.value = v; prefs.userName = v }

    private val _budget = mutableLongStateOf(prefs.monthlyBudget)
    var budget: Long
        get() = _budget.longValue
        set(v) { _budget.longValue = v; prefs.monthlyBudget = v }

    private val _auto = mutableStateOf(prefs.autoCapture)
    var autoCapture: Boolean
        get() = _auto.value
        set(v) { _auto.value = v; prefs.autoCapture = v }

    private val _notify = mutableStateOf(prefs.notifyOnCapture)
    var notifyOnCapture: Boolean
        get() = _notify.value
        set(v) { _notify.value = v; prefs.notifyOnCapture = v }

    private val _broad = mutableStateOf(prefs.broadMode)
    var broadMode: Boolean
        get() = _broad.value
        set(v) { _broad.value = v; prefs.broadMode = v }

    private val _onb = mutableStateOf(prefs.onboardingDone)
    var onboardingDone: Boolean
        get() = _onb.value
        set(v) { _onb.value = v; prefs.onboardingDone = v }

    private val _theme = mutableIntStateOf(prefs.themeMode)
    var themeMode: Int
        get() = _theme.intValue
        set(v) { _theme.intValue = v; prefs.themeMode = v }
}

/** Estado de los permisos: acceso a notificaciones y permiso para mostrar avisos */
data class PermState(val listener: Boolean, val post: Boolean)

fun readPerms(ctx: Context): PermState = PermState(
    listener = NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName),
    post = if (Build.VERSION.SDK_INT >= 33)
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    else true
)

/** Se vuelve a revisar cada vez que regresas a la app (por ejemplo, desde Ajustes de Android) */
@Composable
fun rememberPermState(): MutableState<PermState> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state = remember { mutableStateOf(readPerms(context)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.value = readPerms(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return state
}
