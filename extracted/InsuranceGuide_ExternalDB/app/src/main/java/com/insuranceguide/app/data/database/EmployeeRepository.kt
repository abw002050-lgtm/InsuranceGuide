package com.insuranceguide.app.data.database

import android.content.Context
import android.database.Cursor

class EmployeeRepository(context: Context) {
    private val db = InsuranceDatabase.get(context).readableDatabase()

    fun search(query: String, limit: Int = 100): List<Employee> {
        val q = query.trim()
        if (q.isBlank()) return emptyList()
        val numeric = q.all { it.isDigit() }
        val sql = if (numeric) {
            "SELECT _id, EMP_NO, EMP_DATE_BIRTH, EMP_DATE_IN, EMP_DATE_OUT, EMP_NAME, EMP_MODAH, EMP_MARK, EMP_SALARY, EMP_LOCATION FROM EMP WHERE CAST(EMP_NO AS TEXT) LIKE ? ORDER BY EMP_NO LIMIT ?"
        } else {
            "SELECT _id, EMP_NO, EMP_DATE_BIRTH, EMP_DATE_IN, EMP_DATE_OUT, EMP_NAME, EMP_MODAH, EMP_MARK, EMP_SALARY, EMP_LOCATION FROM EMP WHERE EMP_NAME LIKE ? ORDER BY EMP_NAME LIMIT ?"
        }
        val args = arrayOf("%$q%", limit.toString())
        return db.rawQuery(sql, args).use { c -> buildList { while (c.moveToNext()) add(c.toEmployee()) } }
    }

    private fun Cursor.toEmployee() = Employee(
        id = getLong(0), employeeNo = getLongOrNull(1), birthDate = getStringOrNull(2),
        joinDate = getStringOrNull(3), outDate = getStringOrNull(4), name = getString(5).orEmpty(),
        duration = getStringOrNull(6), mark = getStringOrNull(7), salary = getLongOrNull(8), location = getStringOrNull(9)
    )
    private fun Cursor.getStringOrNull(i: Int) = if (isNull(i)) null else getString(i)
    private fun Cursor.getLongOrNull(i: Int) = if (isNull(i)) null else getLong(i)
}
