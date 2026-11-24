package com.aurum.finanzas.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val type: TxType,
    val keywords: List<String> = emptyList(),
) {
    /** Patrones para reconocer el comercio como palabra completa */
    val patterns: List<Regex> by lazy {
        keywords.map { Regex("(?<![a-z0-9])" + Regex.escape(it) + "(?![a-z0-9])") }
    }
}

object Categories {
    private val E = TxType.EXPENSE
    private val I = TxType.INCOME

    val all: List<Category> = listOf(
        Category("mercado", "Mercado", Icons.Rounded.ShoppingCart, Color(0xFF7C9A6E), E, listOf(
            "exito", "carulla", "d1", "ara", "jumbo", "olimpica", "makro", "oxxo", "colsubsidio",
            "surtimax", "justo y bueno", "isimo", "euro", "supermercado", "fruver", "mercado", "pricesmart",
        )),
        Category("comida", "Comida", Icons.Rounded.Restaurant, Color(0xFFC07A4F), E, listOf(
            "rappi", "ifood", "restaurante", "frisby", "kfc", "mcdonalds", "mcdonald", "burger", "el corral",
            "juan valdez", "starbucks", "tostao", "crepes", "wok", "presto", "pizza", "dominos", "subway",
            "qbano", "cafe", "panaderia", "asadero", "piqueteadero", "bar", "sushi", "helados",
        )),
        Category("transporte", "Transporte", Icons.Rounded.DirectionsCar, Color(0xFF5B7C99), E, listOf(
            "uber", "didi", "cabify", "indriver", "picap", "terpel", "primax", "texaco", "esso", "biomax",
            "mobil", "gasolina", "eds", "transmilenio", "tullave", "parqueadero", "parking", "peaje", "sitp", "taxi",
        )),
        Category("suscripciones", "Suscripciones", Icons.Rounded.Subscriptions, Color(0xFF8A6FA8), E, listOf(
            "netflix", "spotify", "disney", "hbo", "prime video", "amazon prime", "youtube", "apple.com",
            "icloud", "google one", "google play", "chatgpt", "openai", "claude", "anthropic", "xbox",
            "playstation", "psn", "steam", "crunchyroll", "paramount", "deezer", "canva",
        )),
        Category("servicios", "Servicios", Icons.Rounded.Bolt, Color(0xFFC9A24D), E, listOf(
            "claro", "movistar", "tigo", "wom", "etb", "enel", "codensa", "vanti", "gas natural",
            "acueducto", "epm", "emcali", "internet",
        )),
        Category("salud", "Salud", Icons.Rounded.LocalHospital, Color(0xFFC46D7E), E, listOf(
            "drogueria", "farmatodo", "cruz verde", "cafam", "la rebaja", "colsanitas", "sura", "compensar",
            "clinica", "hospital", "laboratorio", "odontologia", "optica", "medico", "farmacia",
        )),
        Category("compras", "Compras", Icons.Rounded.ShoppingBag, Color(0xFF9C7B5B), E, listOf(
            "amazon", "mercadolibre", "mercado libre", "falabella", "zara", "h&m", "adidas", "nike", "koaj",
            "arturo calle", "studio f", "ktronix", "alkosto", "dafiti", "shein", "temu", "aliexpress",
            "tennis", "pat primo", "flamingo", "panamericana",
        )),
        Category("ocio", "Ocio", Icons.Rounded.Movie, Color(0xFF4F8A8B), E, listOf(
            "cine colombia", "cinemark", "procinal", "tuboleta", "eticket", "taquilla", "teatro",
            "concierto", "bolera",
        )),
        Category("educacion", "Educación", Icons.Rounded.School, Color(0xFF6E7FB0), E, listOf(
            "universidad", "umb", "udemy", "platzi", "coursera", "colegio", "matricula", "libreria",
        )),
        Category("hogar", "Hogar", Icons.Rounded.Home, Color(0xFF8C8C5A), E, listOf(
            "arriendo", "administracion", "homecenter", "easy", "ferreteria", "muebles", "ikea", "tugo",
        )),
        Category("viajes", "Viajes", Icons.Rounded.Flight, Color(0xFF5E8FB8), E, listOf(
            "avianca", "latam", "wingo", "jetsmart", "satena", "booking", "airbnb", "despegar", "hotel", "expedia",
        )),
        Category("mascotas", "Mascotas", Icons.Rounded.Pets, Color(0xFFA88A5E), E, listOf(
            "veterinaria", "petco", "laika", "mascota", "mascotas",
        )),
        Category("efectivo", "Efectivo", Icons.Rounded.LocalAtm, Color(0xFF7D8288), E, listOf(
            "retiro", "cajero", "atm",
        )),
        Category("otros", "Otros", Icons.Rounded.MoreHoriz, Color(0xFF8E8A82), E),

        Category("salario", "Salario", Icons.Rounded.Work, Color(0xFF3F7A66), I, listOf(
            "nomina", "salario", "sueldo", "honorarios", "prima",
        )),
        Category("reembolso", "Reembolsos", Icons.Rounded.Replay, Color(0xFFC9A24D), I, listOf(
            "reembolso", "devolucion", "reverso", "cashback",
        )),
        Category("transfer_in", "Transferencias", Icons.Rounded.SwapHoriz, Color(0xFF4F8A8B), I, listOf(
            "transferencia", "te enviaron", "nequi", "daviplata", "consignacion", "te consignaron",
        )),
        Category("otros_ing", "Otros ingresos", Icons.Rounded.Savings, Color(0xFF8E8A82), I),
    )

    fun byId(id: String): Category = all.firstOrNull { it.id == id } ?: all.first { it.id == "otros" }

    fun forType(type: TxType): List<Category> = all.filter { it.type == type }

    fun defaultFor(type: TxType): String = if (type == TxType.EXPENSE) "otros" else "otros_ing"

    /** Adivina la categoría a partir del nombre del comercio o del texto */
    fun guess(text: String, type: TxType): String {
        val t = " " + text.norm() + " "
        for (c in forType(type)) {
            if (c.patterns.any { it.containsMatchIn(t) }) return c.id
        }
        return defaultFor(type)
    }
}
