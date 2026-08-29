package com.insuranceguide.app.data.database

import android.database.sqlite.SQLiteDatabase
import java.io.File

object DatabaseValidator {
    val requiredTables = listOf("EMP", "Pension", "BANKS", "BRANCHES", "GOVS", "POST", "PHONE", "LAWS", "LAWP")

    fun validate(file: File): DatabaseInfo {
        if (!file.exists() || file.length() < 100L) return DatabaseInfo(file.name, file.length(), false, false, error = "الملف غير موجود أو فارغ")
        return runCatching {
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val headerOk = file.inputStream().use { input ->
                    val h = ByteArray(16); input.read(h) == 16 && String(h, Charsets.US_ASCII) == "SQLite format 3\u0000"
                }
                if (!headerOk) return DatabaseInfo(file.name, file.length(), false, false, error = "الملف ليس SQLite صالحًا")
                val integrity = db.rawQuery("PRAGMA integrity_check", null).use { c -> c.moveToFirst() && c.getString(0).equals("ok", true) }
                if (!integrity) return DatabaseInfo(file.name, file.length(), false, false, error = "فشل فحص سلامة SQLite")
                val tables = mutableSetOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c -> while (c.moveToNext()) tables += c.getString(0) }
                val missing = requiredTables.filterNot { it in tables }
                val counts = requiredTables.filter { it in tables }.associateWith { table ->
                    runCatching { db.rawQuery("SELECT COUNT(*) FROM $table", null).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L } }.getOrDefault(0L)
                }
                DatabaseInfo(file.name, file.length(), true, missing.isEmpty(), missing, counts)
            }
        }.getOrElse { DatabaseInfo(file.name, file.length(), false, false, error = it.message ?: "تعذر فحص قاعدة البيانات") }
    }
}
