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
        return db.rawQuery(sql, args).use { c ->
            buildList { while (c.moveToNext()) add(c.toEmployee()) }
        }
    }

    /**
     * يستخرج المحافظات من قيم EMP_LOCATION بمطابقتها مع جدول GOVS.
     * لا يضيف محافظات غير موجودة في قاعدة البيانات.
     */
    fun getGovernorates(): List<String> {
        val govs = runCatching {
            db.rawQuery(
                "SELECT GOV_NAME, GOV_FULL_NAME FROM GOVS ORDER BY GOV_NAME",
                emptyArray()
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val name = c.getString(0).orEmpty().trim()
                        val full = if (c.isNull(1)) "" else c.getString(1).trim()
                        if (name.isNotBlank()) add(name)
                        if (full.isNotBlank()) add(full)
                    }
                }
            }
        }.getOrDefault(emptyList())

        val locations = distinctLocations()
        return govs
            .distinct()
            .filter { gov -> locations.any { location -> containsNormalized(location, gov) } }
            .sorted()
    }

    /**
     * يستخرج المديريات من النص الواقع بعد كلمة "مديرية/مديريه"
     * داخل EMP_LOCATION، مع ربطها بالمحافظة المختارة.
     */
    fun getDistricts(governorate: String): List<String> {
        if (governorate.isBlank()) return emptyList()
        return distinctLocations()
            .asSequence()
            .filter { containsNormalized(it, governorate) }
            .mapNotNull { extractDistrict(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    /**
     * يعرض جهات العمل الفعلية الموجودة في EMP_LOCATION بعد تطبيق الفلاتر.
     */
    fun getLocations(governorate: String, district: String): List<String> {
        return distinctLocations()
            .asSequence()
            .filter { governorate.isBlank() || containsNormalized(it, governorate) }
            .filter { district.isBlank() || extractDistrict(it)?.let { d -> containsNormalized(d, district) } == true }
            .sorted()
            .toList()
    }

    /**
     * بحث SQLite مباشر مع الفلاتر. لا يتم تحميل سجلات الموظفين كلها إلى الذاكرة.
     */
    fun searchAdvanced(
        query: String,
        governorate: String,
        district: String,
        location: String,
        limit: Int = 100
    ): List<Employee> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        val q = query.trim()
        if (q.isNotBlank()) {
            if (q.all { it.isDigit() }) {
                conditions += "CAST(EMP_NO AS TEXT) LIKE ?"
                args += "%$q%"
            } else {
                conditions += "EMP_NAME LIKE ?"
                args += "%$q%"
            }
        }

        if (governorate.isNotBlank()) {
            conditions += "EMP_LOCATION LIKE ?"
            args += "%$governorate%"
        }

        if (district.isNotBlank()) {
            // مديرية قد ترد بالياء أو بدونها في البيانات.
            conditions += "(EMP_LOCATION LIKE ? OR EMP_LOCATION LIKE ?)"
            args += "%مديرية$district%"
            args += "%مديريه$district%"
        }

        if (location.isNotBlank()) {
            conditions += "EMP_LOCATION = ?"
            args += location
        }

        if (conditions.isEmpty()) return emptyList()

        val sql = """
            SELECT _id, EMP_NO, EMP_DATE_BIRTH, EMP_DATE_IN, EMP_DATE_OUT,
                   EMP_NAME, EMP_MODAH, EMP_MARK, EMP_SALARY, EMP_LOCATION
            FROM EMP
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY EMP_NAME
            LIMIT ?
        """.trimIndent()

        args += limit.toString()

        return db.rawQuery(sql, args.toTypedArray()).use { c ->
            buildList {
                while (c.moveToNext()) add(c.toEmployee())
            }
        }
    }

    /**
     * يحسب العدد الحقيقي للنتائج من SQLite دون تحميل السجلات.
     */
    fun countAdvanced(
        query: String,
        governorate: String,
        district: String,
        location: String
    ): Int {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        val q = query.trim()
        if (q.isNotBlank()) {
            if (q.all { it.isDigit() }) {
                conditions += "CAST(EMP_NO AS TEXT) LIKE ?"
                args += "%$q%"
            } else {
                conditions += "EMP_NAME LIKE ?"
                args += "%$q%"
            }
        }

        if (governorate.isNotBlank()) {
            conditions += "EMP_LOCATION LIKE ?"
            args += "%$governorate%"
        }

        if (district.isNotBlank()) {
            conditions += "(EMP_LOCATION LIKE ? OR EMP_LOCATION LIKE ?)"
            args += "%مديرية$district%"
            args += "%مديريه$district%"
        }

        if (location.isNotBlank()) {
            conditions += "EMP_LOCATION = ?"
            args += location
        }

        if (conditions.isEmpty()) return 0

        val sql = "SELECT COUNT(*) FROM EMP WHERE ${conditions.joinToString(" AND ")}"
        return db.rawQuery(sql, args.toTypedArray()).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun distinctLocations(): List<String> {
        return db.rawQuery(
            """
            SELECT DISTINCT EMP_LOCATION
            FROM EMP
            WHERE EMP_LOCATION IS NOT NULL AND TRIM(EMP_LOCATION) <> ''
            ORDER BY EMP_LOCATION
            """.trimIndent(),
            emptyArray()
        ).use { c ->
            buildList {
                while (c.moveToNext()) {
                    val value = c.getString(0).orEmpty().trim()
                    if (value.isNotBlank()) add(value)
                }
            }
        }
    }

    private fun extractDistrict(location: String): String? {
        val normalized = normalize(location)
        val markers = listOf("مديرية", "مديريه")
        val marker = markers.firstOrNull { normalized.contains(it) } ?: return null
        val index = normalized.indexOf(marker)
        if (index < 0) return null
        return normalized.substring(index + marker.length)
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ")
            .ifBlank { null }
    }

    private fun containsNormalized(text: String, part: String): Boolean {
        return normalize(text).contains(normalize(part))
    }

    private fun normalize(value: String): String {
        return value
            .trim()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ى", "ي")
            .replace("ة", "ه")
            .replace(Regex("\\s+"), " ")
    }

    private fun Cursor.toEmployee() = Employee(
        id = getLong(0),
        employeeNo = getLongOrNull(1),
        birthDate = getStringOrNull(2),
        joinDate = getStringOrNull(3),
        outDate = getStringOrNull(4),
        name = getString(5).orEmpty(),
        duration = getStringOrNull(6),
        mark = getStringOrNull(7),
        salary = getLongOrNull(8),
        location = getStringOrNull(9)
    )

    private fun Cursor.getStringOrNull(i: Int) =
        if (isNull(i)) null else getString(i)

    private fun Cursor.getLongOrNull(i: Int) =
        if (isNull(i)) null else getLong(i)
}
