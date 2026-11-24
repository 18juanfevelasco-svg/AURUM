package com.aurum.finanzas.data

import android.content.Context

/** Ajustes del usuario guardados en el celular */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("aurum_prefs", Context.MODE_PRIVATE)

    var userName: String
        get() = sp.getString("name", "") ?: ""
        set(v) = sp.edit().putString("name", v).apply()

    /** Presupuesto mensual en pesos (0 = sin presupuesto) */
    var monthlyBudget: Long
        get() = sp.getLong("budget", 0L)
        set(v) = sp.edit().putLong("budget", v).apply()

    /** Registrar compras automáticamente desde notificaciones */
    var autoCapture: Boolean
        get() = sp.getBoolean("auto", true)
        set(v) = sp.edit().putBoolean("auto", v).apply()

    /** Mostrar aviso "Gasto registrado" */
    var notifyOnCapture: Boolean
        get() = sp.getBoolean("notify", true)
        set(v) = sp.edit().putBoolean("notify", v).apply()

    /** Escuchar notificaciones de cualquier app (no solo bancos y SMS) */
    var broadMode: Boolean
        get() = sp.getBoolean("broad", false)
        set(v) = sp.edit().putBoolean("broad", v).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean("onb", false)
        set(v) = sp.edit().putBoolean("onb", v).apply()

    /** 0 = según el sistema, 1 = claro, 2 = oscuro */
    var themeMode: Int
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()
}
