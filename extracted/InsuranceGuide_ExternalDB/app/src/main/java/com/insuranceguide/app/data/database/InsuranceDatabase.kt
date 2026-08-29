package com.insuranceguide.app.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase

class InsuranceDatabase private constructor(context: Context) {
    private val manager = DatabaseManager(context)
    private var db: SQLiteDatabase? = null

    @Synchronized fun readableDatabase(): SQLiteDatabase {
        if (db?.isOpen != true) db = manager.openReadOnly()
        return db!!
    }

    @Synchronized fun close() { db?.close(); db = null }

    companion object {
        @Volatile private var instance: InsuranceDatabase? = null
        fun get(context: Context): InsuranceDatabase = instance ?: synchronized(this) {
            instance ?: InsuranceDatabase(context.applicationContext).also { instance = it }
        }
        fun reset(context: Context) { instance?.close(); instance = null }
    }
}
