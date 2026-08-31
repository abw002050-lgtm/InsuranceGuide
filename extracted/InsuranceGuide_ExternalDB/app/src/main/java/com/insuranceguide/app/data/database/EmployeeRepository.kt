package com.insuranceguide.app.data.database

import android.content.Context
import android.database.Cursor

class EmployeeRepository(context: Context) {

    private val db = InsuranceDatabase.get(context).readableDatabase()

    // =========================================================
    // البحث العادي
    // =========================================================

    fun search(query: String, limit: Int = 100): List<Employee> {
        val q = query.trim()

        if (q.isBlank()) return emptyList()

        val numeric = q.all { it.isDigit() }

        val sql = if (numeric) {
            """
            SELECT _id,
                   EMP_NO,
                   EMP_DATE_BIRTH,
                   EMP_DATE_IN,
                   EMP_DATE_OUT,
                   EMP_NAME,
                   EMP_MODAH,
                   EMP_MARK,
                   EMP_SALARY,
                   EMP_LOCATION
            FROM EMP
            WHERE CAST(EMP_NO AS TEXT) LIKE ?
            ORDER BY EMP_NO
            LIMIT ?
            """.trimIndent()
        } else {
            """
            SELECT _id,
                   EMP_NO,
                   EMP_DATE_BIRTH,
                   EMP_DATE_IN,
                   EMP_DATE_OUT,
                   EMP_NAME,
                   EMP_MODAH,
                   EMP_MARK,
                   EMP_SALARY,
                   EMP_LOCATION
            FROM EMP
            WHERE EMP_NAME LIKE ?
            ORDER BY EMP_NAME
            LIMIT ?
            """.trimIndent()
        }

        val args = arrayOf(
            "%$q%",
            limit.toString()
        )

        return db.rawQuery(sql, args).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(c.toEmployee())
                }
            }
        }
    }

    // =========================================================
    // المحافظات
    // =========================================================

    /**
     * يستخرج المحافظات الموجودة فعليًا في قاعدة البيانات.
     */
    fun getGovernorates(): List<String> {

        val govs = runCatching {
            db.rawQuery(
                """
                SELECT GOV_NAME, GOV_FULL_NAME
                FROM GOVS
                ORDER BY GOV_NAME
                """.trimIndent(),
                emptyArray()
            ).use { c ->

                buildList {

                    while (c.moveToNext()) {

                        val name =
                            c.getString(0)
                                .orEmpty()
                                .trim()

                        val full =
                            if (c.isNull(1)) {
                                ""
                            } else {
                                c.getString(1)
                                    .trim()
                            }

                        if (name.isNotBlank()) {
                            add(name)
                        }

                        if (full.isNotBlank()) {
                            add(full)
                        }
                    }
                }
            }
        }.getOrDefault(emptyList())

        val locations = distinctLocations()

        return govs
            .distinct()
            .filter { gov ->
                locations.any { location ->
                    containsNormalized(location, gov)
                }
            }
            .sorted()
    }

    // =========================================================
    // المديريات
    // =========================================================

    /**
     * يستخرج المديريات من EMP_LOCATION.
     *
     * يدعم:
     * مديرية
     * مديريه
     *
     * وكذلك اختلافات الكتابة العربية مثل:
     * التعزية / التعزيه
     */
    fun getDistricts(governorate: String): List<String> {

        if (governorate.isBlank()) {
            return emptyList()
        }

        return distinctLocations()
            .asSequence()

            // أولاً المحافظة
            .filter {
                containsNormalized(it, governorate)
            }

            // استخراج اسم المديرية
            .mapNotNull {
                extractDistrict(it)
            }

            .map {
                normalize(it)
            }

            .filter {
                it.isNotBlank()
            }

            .distinct()
            .sorted()
            .toList()
    }

    // =========================================================
    // جهات العمل / المواقع
    // =========================================================

    /**
     * يعرض جهات العمل الموجودة فعليًا في EMP_LOCATION
     * بعد تطبيق المحافظة والمديرية.
     */
    fun getLocations(
        governorate: String,
        district: String
    ): List<String> {

        return distinctLocations()
            .asSequence()

            // فلتر المحافظة
            .filter {
                governorate.isBlank() ||
                        containsNormalized(it, governorate)
            }

            // فلتر المديرية
            .filter {

                if (district.isBlank()) {
                    true
                } else {

                    val extractedDistrict =
                        extractDistrict(it)

                    extractedDistrict != null &&
                            containsNormalized(
                                extractedDistrict,
                                district
                            )
                }
            }

            .sorted()
            .toList()
    }

    // =========================================================
    // البحث المتقدم
    // =========================================================

    /**
     * البحث المتقدم في SQLite.
     *
     * الفلاتر:
     * الاسم أو رقم الموظف
     * المحافظة
     * المديرية
     * جهة العمل
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

        // -----------------------------------------------------
        // الاسم أو رقم الموظف
        // -----------------------------------------------------

        if (q.isNotBlank()) {

            if (q.all { it.isDigit() }) {

                conditions +=
                    "CAST(EMP_NO AS TEXT) LIKE ?"

                args += "%$q%"

            } else {

                conditions +=
                    "EMP_NAME LIKE ?"

                args += "%$q%"
            }
        }

        // -----------------------------------------------------
        // المحافظة
        // -----------------------------------------------------

        if (governorate.isNotBlank()) {

            val govRaw = governorate.trim()
            val govNormalized = normalize(governorate)

            if (govRaw == govNormalized) {

                conditions +=
                    "EMP_LOCATION LIKE ?"

                args += "%$govRaw%"

            } else {

                conditions +=
                    "(EMP_LOCATION LIKE ? OR EMP_LOCATION LIKE ?)"

                args += "%$govRaw%"
                args += "%$govNormalized%"
            }
        }

        // -----------------------------------------------------
        // المديرية
        // -----------------------------------------------------

        if (district.isNotBlank()) {

            val districtRaw =
                district.trim()

            val districtNormalized =
                normalize(district)

            /*
             * نبحث في أكثر من صيغة:
             *
             * مديرية التعزية
             * مديريه التعزية
             * مديرية التعزيه
             * مديريه التعزيه
             *
             * وذلك حتى لا يعتمد البحث على طريقة واحدة
             * لكتابة البيانات.
             */

            val patterns = linkedSetOf(
                "%مديرية $districtRaw%",
                "%مديريه $districtRaw%",
                "%مديرية $districtNormalized%",
                "%مديريه $districtNormalized%"
            )

            val districtConditions =
                patterns.joinToString(" OR ") {
                    "EMP_LOCATION LIKE ?"
                }

            conditions += "($districtConditions)"

            args.addAll(patterns)
        }

        // -----------------------------------------------------
        // جهة العمل / الموقع
        // -----------------------------------------------------

        if (location.isNotBlank()) {

            /*
             * الموقع المختار يأتي مباشرة من القائمة
             * ولذلك نستخدم المطابقة الدقيقة.
             */
            conditions +=
                "TRIM(EMP_LOCATION) = TRIM(?)"

            args += location.trim()
        }

        // -----------------------------------------------------
        // لا توجد فلاتر
        // -----------------------------------------------------

        if (conditions.isEmpty()) {
            return emptyList()
        }

        // -----------------------------------------------------
        // SQL
        // -----------------------------------------------------

        val sql = """
            SELECT _id,
                   EMP_NO,
                   EMP_DATE_BIRTH,
                   EMP_DATE_IN,
                   EMP_DATE_OUT,
                   EMP_NAME,
                   EMP_MODAH,
                   EMP_MARK,
                   EMP_SALARY,
                   EMP_LOCATION
            FROM EMP
            WHERE ${conditions.joinToString(" AND ")}
            ORDER BY EMP_NAME
            LIMIT ?
        """.trimIndent()

        args += limit.toString()

        return db.rawQuery(
            sql,
            args.toTypedArray()
        ).use { c ->

            buildList {

                while (c.moveToNext()) {
                    add(c.toEmployee())
                }
            }
        }
    }

    // =========================================================
    // حساب عدد النتائج
    // =========================================================

    /**
     * يحسب العدد الحقيقي للنتائج.
     *
     * مهم جدًا:
     * يستخدم نفس منطق searchAdvanced()
     * حتى لا يظهر عدد مختلف عن النتائج.
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

        // -----------------------------------------------------
        // الاسم أو رقم الموظف
        // -----------------------------------------------------

        if (q.isNotBlank()) {

            if (q.all { it.isDigit() }) {

                conditions +=
                    "CAST(EMP_NO AS TEXT) LIKE ?"

                args += "%$q%"

            } else {

                conditions +=
                    "EMP_NAME LIKE ?"

                args += "%$q%"
            }
        }

        // -----------------------------------------------------
        // المحافظة
        // -----------------------------------------------------

        if (governorate.isNotBlank()) {

            val govRaw =
                governorate.trim()

            val govNormalized =
                normalize(governorate)

            if (govRaw == govNormalized) {

                conditions +=
                    "EMP_LOCATION LIKE ?"

                args += "%$govRaw%"

            } else {

                conditions +=
                    "(EMP_LOCATION LIKE ? OR EMP_LOCATION LIKE ?)"

                args += "%$govRaw%"
                args += "%$govNormalized%"
            }
        }

        // -----------------------------------------------------
        // المديرية
        // -----------------------------------------------------

        if (district.isNotBlank()) {

            val districtRaw =
                district.trim()

            val districtNormalized =
                normalize(district)

            val patterns = linkedSetOf(
                "%مديرية $districtRaw%",
                "%مديريه $districtRaw%",
                "%مديرية $districtNormalized%",
                "%مديريه $districtNormalized%"
            )

            val districtConditions =
                patterns.joinToString(" OR ") {
                    "EMP_LOCATION LIKE ?"
                }

            conditions += "($districtConditions)"

            args.addAll(patterns)
        }

        // -----------------------------------------------------
        // جهة العمل
        // -----------------------------------------------------

        if (location.isNotBlank()) {

            conditions +=
                "TRIM(EMP_LOCATION) = TRIM(?)"

            args += location.trim()
        }

        // -----------------------------------------------------
        // لا توجد فلاتر
        // -----------------------------------------------------

        if (conditions.isEmpty()) {
            return 0
        }

        // -----------------------------------------------------
        // COUNT
        // -----------------------------------------------------

        val sql =
            "SELECT COUNT(*) FROM EMP WHERE " +
                    conditions.joinToString(" AND ")

        return db.rawQuery(
            sql,
            args.toTypedArray()
        ).use { c ->

            if (c.moveToFirst()) {
                c.getInt(0)
            } else {
                0
            }
        }
    }

    // =========================================================
    // استخراج المواقع الفعلية
    // =========================================================

    private fun distinctLocations(): List<String> {

        return db.rawQuery(
            """
            SELECT DISTINCT EMP_LOCATION
            FROM EMP
            WHERE EMP_LOCATION IS NOT NULL
              AND TRIM(EMP_LOCATION) <> ''
            ORDER BY EMP_LOCATION
            """.trimIndent(),
            emptyArray()
        ).use { c ->

            buildList {

                while (c.moveToNext()) {

                    val value =
                        c.getString(0)
                            .orEmpty()
                            .trim()

                    if (value.isNotBlank()) {
                        add(value)
                    }
                }
            }
        }
    }

    // =========================================================
    // استخراج اسم المديرية
    // =========================================================

    private fun extractDistrict(
        location: String
    ): String? {

        val normalized =
            normalize(location)

        /*
         * بعد normalize():
         *
         * مديرية
         *
         * تصبح:
         *
         * مديريـه
         *
         * لذلك نبحث عن الشكلين احتياطًا.
         */

        val markers =
            listOf(
                "مديريه",
                "مديرية"
            )

        val marker =
            markers.firstOrNull {
                normalized.contains(it)
            }
                ?: return null

        val index =
            normalized.indexOf(marker)

        if (index < 0) {
            return null
        }

        val result =
            normalized
                .substring(
                    index + marker.length
                )
                .trim()

        return result
            .split(Regex("\\s+"))
            .joinToString(" ")
            .ifBlank {
                null
            }
    }

    // =========================================================
    // مقارنة عربية مرنة
    // =========================================================

    private fun containsNormalized(
        text: String,
        part: String
    ): Boolean {

        return normalize(text)
            .contains(
                normalize(part)
            )
    }

    // =========================================================
    // تطبيع النص العربي
    // =========================================================

    private fun normalize(
        value: String
    ): String {

        return value
            .trim()

            // الهمزات
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")

            // الياء والألف المقصورة
            .replace("ى", "ي")

            // التاء المربوطة
            .replace("ة", "ه")

            // مسافات متعددة
            .replace(
                Regex("\\s+"),
                " "
            )
    }

    // =========================================================
    // تحويل Cursor إلى Employee
    // =========================================================

    private fun Cursor.toEmployee() =
        Employee(

            id = getLong(0),

            employeeNo =
                getLongOrNull(1),

            birthDate =
                getStringOrNull(2),

            joinDate =
                getStringOrNull(3),

            outDate =
                getStringOrNull(4),

            name =
                getString(5).orEmpty(),

            duration =
                getStringOrNull(6),

            mark =
                getStringOrNull(7),

            salary =
                getLongOrNull(8),

            location =
                getStringOrNull(9)
        )

    // =========================================================
    // أدوات Cursor
    // =========================================================

    private fun Cursor.getStringOrNull(
        index: Int
    ): String? =
        if (isNull(index)) {
            null
        } else {
            getString(index)
        }

    private fun Cursor.getLongOrNull(
        index: Int
    ): Long? =
        if (isNull(index)) {
            null
        } else {
            getLong(index)
        }
}
