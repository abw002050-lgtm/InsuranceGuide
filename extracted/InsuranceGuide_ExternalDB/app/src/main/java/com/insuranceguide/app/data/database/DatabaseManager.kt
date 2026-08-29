package com.insuranceguide.app.data.database

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DatabaseManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("database_preferences", Context.MODE_PRIVATE)
    private val dbFile = File(appContext.filesDir, "databases/Domw.db")

    fun currentFile(): File? = dbFile.takeIf { it.exists() }

    fun currentInfo(): DatabaseInfo? = currentFile()?.let { DatabaseValidator.validate(it) }

    fun import(uri: Uri): DatabaseInfo {
        val tempSource = File(appContext.cacheDir, "database_source.tmp")
        val tempDb = File(appContext.cacheDir, "database_candidate.db")
        tempSource.delete(); tempDb.delete()
        try {
            appContext.contentResolver.openInputStream(uri)?.use { input -> FileOutputStream(tempSource).use { input.copyTo(it) } }
                ?: error("تعذر قراءة الملف المحدد")
            val candidate = if (isZip(tempSource)) extractSqlite(tempSource, tempDb) else {
                tempSource.copyTo(tempDb, overwrite = true); tempDb
            }
            val info = DatabaseValidator.validate(candidate)
            check(info.isValid) { info.error ?: "قاعدة البيانات غير صالحة" }
            check(info.isCompatible) { "قاعدة البيانات غير متوافقة. الجداول المفقودة: ${info.missingTables.joinToString()}" }
            dbFile.parentFile?.mkdirs()
            val replacement = File(dbFile.parentFile, "Domw.db.new")
            replacement.delete(); candidate.copyTo(replacement, overwrite = true)
            ensureIndexes(replacement)
            val finalInfo = DatabaseValidator.validate(replacement)
            check(finalInfo.isValid && finalInfo.isCompatible) { "فشل التحقق بعد تجهيز قاعدة البيانات" }
            if (dbFile.exists() && !dbFile.delete()) error("تعذر استبدال قاعدة البيانات الحالية")
            check(replacement.renameTo(dbFile)) { "تعذر تثبيت قاعدة البيانات الجديدة" }
            prefs.edit().putString("source_uri", uri.toString()).putString("display_name", displayName(uri)).apply()
            return DatabaseValidator.validate(dbFile) ?: error("تعذر قراءة القاعدة")
        } finally { tempSource.delete(); tempDb.delete() }
    }

    fun clear() {
        dbFile.delete()
        prefs.edit().clear().apply()
    }

    fun openReadOnly(): SQLiteDatabase = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)

    private fun isZip(file: File): Boolean = file.inputStream().use { i ->
        val h = ByteArray(4); i.read(h) == 4 && h[0] == 'P'.code.toByte() && h[1] == 'K'.code.toByte()
    }

    private fun extractSqlite(zipFile: File, output: File): File {
        ZipInputStream(zipFile.inputStream()).use { zip ->
            var found = false
            while (true) {
                val entry = zip.nextEntry ?: break
                val safeName = entry.name.replace('\\', '/')
                if (entry.isDirectory || safeName.startsWith("__MACOSX/") || safeName.contains("../") || safeName.startsWith("/")) { zip.closeEntry(); continue }
                val name = safeName.substringAfterLast('/').lowercase()
                if (!name.endsWith(".db") && !name.endsWith(".sqlite") && !name.endsWith(".sqlite3") && name != "domw") { zip.closeEntry(); continue }
                FileOutputStream(output).use { zip.copyTo(it) }
                found = true; zip.closeEntry(); break
            }
            check(found) { "ملف ZIP لا يحتوي على قاعدة SQLite" }
        }
        return output
    }

    private fun displayName(uri: Uri): String = runCatching {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else uri.lastPathSegment ?: "قاعدة البيانات"
        } ?: (uri.lastPathSegment ?: "قاعدة البيانات")
    }.getOrDefault("قاعدة البيانات")

    private fun ensureIndexes(file: File) {
        SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            createIndexIfColumnExists(db, "EMP", "EMP_NO", "idx_emp_no_external")
            createIndexIfColumnExists(db, "EMP", "EMP_NAME", "idx_emp_name_external")
            createIndexIfColumnExists(db, "Pension", "EMP_NAME", "idx_pension_name_external")
            createIndexIfColumnExists(db, "Pension", "EMP_INSURANCE_NO", "idx_pension_insurance_external")
            createIndexIfColumnExists(db, "Pension", "EMP_LINK_NO", "idx_pension_link_external")
        }
    }

    private fun createIndexIfColumnExists(db: SQLiteDatabase, table: String, column: String, index: String) {
        val exists = db.rawQuery("PRAGMA table_info($table)", null).use { c ->
            val nameIndex = c.getColumnIndex("name")
            var found = false
            while (c.moveToNext()) if (nameIndex >= 0 && c.getString(nameIndex).equals(column, true)) { found = true; break }
            found
        }
        if (exists) db.execSQL("CREATE INDEX IF NOT EXISTS $index ON $table($column)")
    }
}
