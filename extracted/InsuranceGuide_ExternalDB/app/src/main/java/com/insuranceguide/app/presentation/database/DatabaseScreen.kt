package com.insuranceguide.app.presentation.database

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.DatabaseInfo

@Composable
fun DatabaseScreen(info: DatabaseInfo?, onPick: () -> Unit, onClear: () -> Unit, onBack: () -> Unit, message: String? = null) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("قاعدة البيانات", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(18.dp))
        if (info == null) {
            Text("لم يتم تحديد قاعدة بيانات.")
            Spacer(Modifier.height(8.dp))
            Text("اختر Domw.zip أو ملف SQLite من الهاتف.")
        } else {
            Text("الملف: ${info.name}")
            Text("الحالة: ${if (info.isCompatible) "متوافقة ✅" else "غير متوافقة ⚠️"}")
            Text("سلامة SQLite: ${if (info.isValid) "سليمة ✅" else "غير سليمة ❌"}")
            Spacer(Modifier.height(10.dp))
            info.tableCounts.forEach { (table, count) -> Text("$table: $count") }
            if (info.missingTables.isNotEmpty()) Text("الجداول المفقودة: ${info.missingTables.joinToString()}")
            info.error?.let { Text("السبب: $it") }
        }
        message?.let { Text(it, modifier = Modifier.padding(top = 12.dp)) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text("📂 اختيار قاعدة بيانات") }
        if (info != null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text("إزالة القاعدة الحالية") }
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("رجوع") }
    }
}
