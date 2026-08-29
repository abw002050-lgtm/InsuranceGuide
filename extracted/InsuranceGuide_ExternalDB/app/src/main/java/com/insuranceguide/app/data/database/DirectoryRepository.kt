package com.insuranceguide.app.data.database

import android.content.Context

data class DirectoryItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val details: List<Pair<String, String?>>
)

class DirectoryRepository(context: Context) {
    private val db = InsuranceDatabase.get(context).readableDatabase()

    fun search(section: String, query: String): List<DirectoryItem> {
        val (table, titleColumn, columns) = when (section.uppercase()) {
            "BANKS" -> Triple("BANKS", "B_BANK_NAME", listOf("B_BANK_ADDRESS", "B_BANK_PHONE", "B_BANK_FAX", "B_BANK_SITE", "B_BANK_EMAIL"))
            "BRANCHES" -> Triple("BRANCHES", "BRA_NAME", listOf("BRA_NO", "BRA_TEL", "BRA_TEL1", "BRA_TEL2", "BRA_FAX", "BRA_FAX2", "BRA_SITE", "BRA_EMAIL", "BRA_ADDRESS", "BRA_MAN_NAME"))
            "GOVS" -> Triple("GOVS", "GOV_NAME", listOf("GOV_MAN", "GOV_FULL_NAME", "GOV_PHONE", "GOV_FAX", "GOV_KEY", "GOV_POST", "GOV_EMAIL", "GOV_SITE"))
            "POST" -> Triple("POST", "POS_NAME", listOf("POS_NO", "POS_KEY", "POS_ADD", "POS_TEL1", "POS_FAX", "POS_GOV", "POS_CODE"))
            else -> Triple("PHONE", "PHONE_DESC", listOf("PHONE_NUM", "PHONE_FAX"))
        }
        val searchColumns = listOf(titleColumn) + columns
        val where = if (query.isBlank()) "1=1" else searchColumns.joinToString(" OR ") { "$it LIKE ?" }
        val args = if (query.isBlank()) emptyArray() else Array(searchColumns.size) { "%$query%" }
        val sql = "SELECT _id,$titleColumn,${columns.joinToString(",")} FROM $table WHERE $where ORDER BY $titleColumn LIMIT 200"

        return db.rawQuery(sql, args).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val details = columns.mapIndexed { index, name ->
                        name to if (cursor.isNull(index + 2)) null else cursor.getString(index + 2)
                    }
                    add(DirectoryItem(cursor.getLong(0), cursor.getString(1).orEmpty(), details.firstOrNull()?.second, details))
                }
            }
        }
    }
}
