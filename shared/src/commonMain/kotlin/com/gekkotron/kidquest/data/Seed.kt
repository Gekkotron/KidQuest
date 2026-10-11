package com.gekkotron.kidquest.data

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement

object SeedData {
    val quickAmounts = listOf(10, 20, 100, -10)

    fun actions(language: String): List<PointAction> {
        val fr = language.lowercase().startsWith("fr")
        val rows = listOf(
            Triple(if (fr) "Essuyer la table" else "Wipe the table", 50, "🧽"),
            Triple(if (fr) "Ranger ses chaussures" else "Put shoes away", 25, "👟"),
            Triple(if (fr) "Passer l'aspirateur" else "Vacuum", 100, "🧹"),
            Triple(if (fr) "Remplir/vider le lave-vaisselle" else "Load/unload dishwasher", 100, "🍽️"),
            Triple(if (fr) "Se brosser les dents sans qu'on le demande" else "Brush teeth without asking", 20, "🪥"),
        )
        return rows.mapIndexed { i, (label, points, icon) -> PointAction(label = label, points = points, icon = icon, sortOrder = i) }
    }
}

private inline fun SQLiteConnection.withStatement(sql: String, block: (SQLiteStatement) -> Unit) {
    val statement = prepare(sql)
    try {
        block(statement)
    } finally {
        statement.close()
    }
}

internal class SeedCallback(private val language: String) : RoomDatabase.Callback() {
    override fun onCreate(connection: SQLiteConnection) {
        connection.withStatement("INSERT INTO point_action (label, points, icon, sortOrder) VALUES (?, ?, ?, ?)") { st ->
            SeedData.actions(language).forEach { a ->
                st.bindText(1, a.label); st.bindLong(2, a.points.toLong()); st.bindText(3, a.icon); st.bindLong(4, a.sortOrder.toLong())
                st.step(); st.reset()
            }
        }
        connection.withStatement("INSERT INTO quick_amount (points, sortOrder) VALUES (?, ?)") { st ->
            SeedData.quickAmounts.forEachIndexed { i, points ->
                st.bindLong(1, points.toLong()); st.bindLong(2, i.toLong())
                st.step(); st.reset()
            }
        }
    }
}
