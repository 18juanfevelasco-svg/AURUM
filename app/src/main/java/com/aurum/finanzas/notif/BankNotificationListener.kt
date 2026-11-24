package com.aurum.finanzas.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.aurum.finanzas.data.Prefs
import com.aurum.finanzas.data.Repo
import com.aurum.finanzas.data.Transaction
import com.aurum.finanzas.data.TxSource

/**
 * Servicio que Android activa cada vez que llega una notificación al celular.
 * Si parece una compra o un ingreso de tu banco, la guarda automáticamente.
 */
class BankNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Repo.init(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try {
            handle(sbn)
        } catch (e: Exception) {
            Log.w("Aurum", "No se pudo procesar la notificación", e)
        }
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val prefs = Prefs(applicationContext)
        if (!prefs.autoCapture) return
        if (!NotificationParser.isCandidateSource(sbn.packageName, prefs.broadMode)) return

        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.joinToString("\n") { it.toString() }.orEmpty()

        val body = listOf(title, if (big.length > text.length) big else text, lines)
            .filter { it.isNotBlank() }
            .joinToString("\n")

        val parsed = NotificationParser.parse(body, sbn.packageName) ?: return

        Repo.init(applicationContext)
        val time = sbn.postTime
        if (Repo.isDuplicate(parsed.amount, time)) return

        val saved = Repo.add(
            Transaction(
                amount = parsed.amount,
                type = parsed.type,
                categoryId = parsed.categoryId,
                merchant = parsed.merchant,
                cardLast4 = parsed.cardLast4,
                bank = parsed.bank,
                paymentMethod = parsed.method,
                dateMillis = time,
                source = TxSource.AUTO,
                rawText = body,
            )
        )
        if (prefs.notifyOnCapture) Notifier.captured(applicationContext, saved)
    }
}
