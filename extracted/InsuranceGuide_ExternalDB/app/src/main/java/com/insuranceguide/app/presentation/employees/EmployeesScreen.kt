package com.insuranceguide.app.presentation.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.Employee
import com.insuranceguide.app.data.database.EmployeeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesScreen(repository: EmployeeRepository, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var selected by remember { mutableStateOf<Employee?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.trim().length < 2) { results = emptyList(); loading = false; return@LaunchedEffect }
        delay(300)
        loading = true
        results = withContext(Dispatchers.IO) { repository.search(query) }
        loading = false
    }

    selected?.let { employee -> EmployeeDetails(employee) { selected = null }; return }

    Scaffold(topBar = { TopAppBar(title = { Text("بحث الموظفين") }, navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("الاسم أو رقم الموظف") }, placeholder = { Text("اكتب حرفين على الأقل للبحث بالاسم") })
            Spacer(Modifier.height(12.dp))
            when {
                loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
                query.trim().length in 1..1 -> Text("اكتب حرفًا إضافيًا لبدء البحث")
                query.trim().length >= 2 && results.isEmpty() -> Text("لا توجد نتائج مطابقة")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { employee ->
                        ElevatedCard(Modifier.fillMaxWidth().clickable { selected = employee }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(employee.name, fontWeight = FontWeight.Bold)
                                Text("رقم الموظف: ${employee.employeeNo ?: "غير متوفر"}")
                                employee.location?.takeIf { it.isNotBlank() }?.let { Text(it) }
                            }
                        }
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmployeeDetails(e: Employee, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("تفاصيل الموظف") }, navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(e.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            item { Detail("رقم الموظف", e.employeeNo?.toString()) }
            item { Detail("تاريخ الميلاد", e.birthDate) }; item { Detail("تاريخ الالتحاق", e.joinDate) }
            item { Detail("تاريخ الخروج", e.outDate) }; item { Detail("المدة", e.duration) }
            item { Detail("الدرجة/العلامة", e.mark) }; item { Detail("الراتب", e.salary?.toString()) }
            item { Detail("جهة العمل", e.location) }
        }
    }
}
@Composable private fun Detail(label: String, value: String?) { ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) { Text(label, style=MaterialTheme.typography.labelMedium); Text(value ?: "غير متوفر") } } }
