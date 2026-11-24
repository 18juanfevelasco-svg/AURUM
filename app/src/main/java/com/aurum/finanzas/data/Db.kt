package com.aurum.finanzas.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** Base de datos local (SQLite) dentro del celular. Nada sale del teléfono. */
class Db(context: Context) : SQLiteOpenHelper(context, "aurum.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tx (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                merchant TEXT NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                card TEXT NOT NULL DEFAULT '',
                bank TEXT NOT NULL DEFAULT '',
                method TEXT NOT NULL DEFAULT '',
                date INTEGER NOT NULL,
                source TEXT NOT NULL,
                raw TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tx_date ON tx(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun all(): List<Transaction> {
        val out = ArrayList<Transaction>()
        readableDatabase.rawQuery(
            "SELECT id, amount, type, category, merchant, note, card, bank, method, date, source, raw " +
                "FROM tx ORDER BY date DESC, id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out += Transaction(
                    id = c.getLong(0),
                    amount = c.getDouble(1),
                    type = runCatching { TxType.valueOf(c.getString(2)) }.getOrDefault(TxType.EXPENSE),
                    categoryId = c.getString(3),
                    merchant = c.getString(4),
                    note = c.getString(5),
                    cardLast4 = c.getString(6),
                    bank = c.getString(7),
                    paymentMethod = c.getString(8),
                    dateMillis = c.getLong(9),
                    source = runCatching { TxSource.valueOf(c.getString(10)) }.getOrDefault(TxSource.MANUAL),
                    rawText = c.getString(11),
                )
            }
        }
        return out
    }

    private fun values(t: Transaction) = ContentValues().apply {
        put("amount", t.amount)
        put("type", t.type.name)
        put("category", t.categoryId)
        put("merchant", t.merchant)
        put("note", t.note)
        put("card", t.cardLast4)
        put("bank", t.bank)
        put("method", t.paymentMethod)
        put("date", t.dateMillis)
        put("source", t.source.name)
        put("raw", t.rawText)
    }

    fun insert(t: Transaction): Long = writableDatabase.insert("tx", null, values(t))

    fun insertAll(list: List<Transaction>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            list.forEach { db.insert("tx", null, values(it)) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun update(t: Transaction) {
        writableDatabase.update("tx", values(t), "id = ?", arrayOf(t.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("tx", "id = ?", arrayOf(id.toString()))
    }

    fun clear() {
        writableDatabase.delete("tx", null, null)
    }
}

/**
 * Punto único de acceso a los movimientos. La pantalla y el servicio de notificaciones
 * usan este mismo objeto, así que cuando llega una compra la pantalla se actualiza sola.
 */
object Repo {
    private var db: Db? = null
    private val _items = MutableStateFlow<List<Transaction>>(emptyList())
    val items: StateFlow<List<Transaction>> = _items.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (db != null) return
        val d = Db(context.applicationContext)
        db = d
        _items.value = d.all()
    }

    private fun d(): Db = db ?: error("Repo no inicializado")

    @Synchronized
    fun add(t: Transaction): Transaction {
        val id = d().insert(t)
        _items.value = d().all()
        return t.copy(id = id)
    }

    @Synchronized
    fun addAll(list: List<Transaction>) {
        d().insertAll(list)
        _items.value = d().all()
    }

    @Synchronized
    fun update(t: Transaction) {
        d().update(t)
        _items.value = d().all()
    }

    @Synchronized
    fun delete(id: Long) {
        d().delete(id)
        _items.value = d().all()
    }

    @Synchronized
    fun clear() {
        d().clear()
        _items.value = emptyList()
    }

    /**
     * Evita duplicados: muchos bancos avisan dos veces (notificación de la app + SMS).
     * Si ya existe un movimiento automático con el mismo monto en los últimos 3 minutos, se ignora.
     */
    fun isDuplicate(amount: Double, time: Long): Boolean =
        _items.value.any {
            it.source == TxSource.AUTO &&
                abs(it.amount - amount) < 0.5 &&
                abs(it.dateMillis - time) < 180_000L
        }
}
