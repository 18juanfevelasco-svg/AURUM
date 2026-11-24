package com.aurum.finanzas.notif

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aurum.finanzas.MainActivity
import com.aurum.finanzas.R
import com.aurum.finanzas.data.Categories
import com.aurum.finanzas.data.Transaction
import com.aurum.finanzas.data.TxType
import com.aurum.finanzas.util.Format

/** Muestra el aviso "Gasto registrado" cuando se captura una compra */
object Notifier {
    private const val CHANNEL = "capturas"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val ch = NotificationChannel(CHANNEL, "Movimientos registrados", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "Aviso cuando Aurum registra una compra o ingreso automáticamente"
        nm.createNotificationChannel(ch)
    }

    @SuppressLint("MissingPermission")
    fun captured(ctx: Context, tx: Transaction) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)

        val open = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val cat = Categories.byId(tx.categoryId)
        val title = if (tx.type == TxType.EXPENSE) "Gasto registrado · ${Format.money(tx.amount)}"
        else "Ingreso registrado · ${Format.money(tx.amount)}"

        val notification = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_aurum)
            .setContentTitle(title)
            .setContentText("${tx.merchant} · ${cat.name}")
            .setColor(0xFFB08A4E.toInt())
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()

        NotificationManagerCompat.from(ctx).notify((tx.id % Int.MAX_VALUE).toInt(), notification)
    }
}
