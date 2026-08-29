package com.insuranceguide.app.presentation.pension

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.Pension
import com.insuranceguide.app.data.database.PensionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PensionScreen(repository: PensionRepository, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pension>>(emptyList()) }
    var selected by remember { mutableStateOf<Pension?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        loading = true
        results = withContext(Dispatchers.IO) { repository.search(query) }
        loading = false
    }

    selected?.let {
        PensionDetails(it) { selected = null }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بحث المتقاعدين والمعاشات") },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("الاسم أو رقم الربط أو رقم التأمين") }
            )
            Spacer(Modifier.height(12.dp))
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { pension ->
                    ElevatedCard(Modifier.fillMaxWidth().clickable { selected = pension }) {
                        Column(Modifier.padding(14.dp)) {
                            Text(pension.name, fontWeight = FontWeight.Bold)
                            Text("رقم الربط: ${pension.linkNo ?: "غير متوفر"}")
                            Text("رقم التأمين: ${pension.insuranceNo ?: "غير متوفر"}")
                            pension.branchName?.let { Text(it) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PensionDetails(p: Pension, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل المعاش") },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(p.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            listOf(
                "رقم الربط" to p.linkNo?.toString(), "رقم التأمين" to p.insuranceNo?.toString(),
                "الحساب" to p.fullAccount, "نوع المستفيد" to p.kindType,
                "سبب الاستحقاق" to p.reasonType, "الوكيل" to p.wakeelName,
                "القرض" to p.loan?.toString(), "إجمالي المعاش" to p.total?.toString(),
                "صافي المعاش" to p.net?.toString(), "جهة العمل" to p.company,
                "تاريخ الميلاد" to p.birthDate, "تاريخ الالتحاق" to p.joinDate,
                "تاريخ انتهاء الخدمة" to p.outDate, "تاريخ الربط" to p.linkDate,
                "رقم الفرع" to p.branchNo?.toString(), "اسم الفرع" to p.branchName,
                "الدرجة" to p.fullMark
            ).forEach { (label, value) ->
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(label)
                            Text(value ?: "غير متوفر")
                        }
                    }
                }
            }
        }
    }
}
