package com.insuranceguide.app.data.database

import android.content.Context
import android.database.Cursor

class PensionRepository(context: Context) {
    private val db = InsuranceDatabase.get(context).readableDatabase()
    private val columns = "_id, EMP_FULL_ACC, EMP_NAME, EMP_LINK_NO, EMP_TYPE_CODE, EMP_OFFICE_NAME, EMP_KIND_TYPE, EMP_REASON_TYPE, EMP_WAKEEL_NAME, EMP_LOAN, EMP_TOTAL, EMP_NET, EMP_COMPANY, EMP_BIRTHDATE, EMP_JOIN_DATE, EMP_OUT_DATE, EMP_LINK_DATE, EMP_BRANCH_NO, EMP_BRANCH_NAME, EMP_INSURANCE_NO, EMP_FULL_MARK"
    fun search(query: String, limit: Int = 100): List<Pension> {
        val q=query.trim(); if(q.isBlank()) return emptyList()
        val numeric=q.all { it.isDigit() }
        val where=if(numeric) "CAST(EMP_LINK_NO AS TEXT) LIKE ? OR CAST(EMP_INSURANCE_NO AS TEXT) LIKE ?" else "EMP_NAME LIKE ?"
        val args=if(numeric) arrayOf("%$q%","%$q%") else arrayOf("%$q%")
        return db.rawQuery("SELECT $columns FROM Pension WHERE $where ORDER BY EMP_NAME LIMIT $limit", args).use { c -> buildList { while(c.moveToNext()) add(c.toPension()) } }
    }
    private fun Cursor.s(i:Int)=if(isNull(i)) null else getString(i)
    private fun Cursor.l(i:Int)=if(isNull(i)) null else getLong(i)
    private fun Cursor.toPension()=Pension(getLong(0),s(2).orEmpty(),l(3),l(19),s(1),s(4),s(5),s(6),s(7),s(8),l(9),l(10),l(11),s(12),s(13),s(14),s(15),s(16),l(17),s(18),s(20))
}
