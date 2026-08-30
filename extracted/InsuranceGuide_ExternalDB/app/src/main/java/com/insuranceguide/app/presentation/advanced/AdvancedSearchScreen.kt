@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.insuranceguide.app.presentation.advanced

import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdvancedSearchScreen(
    repository: EmployeeRepository,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    var mode by rememberSaveable { mutableStateOf("menu") }

    when (mode) {
        "employees" -> EmployeeAdvancedSearchScreen(
            repository = repository,
            onBack = { mode = "menu" }
        )
        else -> AdvancedSearchMenu(
            onEmployees = { mode = "employees" },
            onPension = {
                // المرحلة الأولى: الموظفون. قسم المتقاعدين سيضاف في المرحلة التالية
                mode = "employees"
            },
            onBack = onBack
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedSearchMenu(
    onEmployees: () -> Unit,
    onPension: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البحث المتقدم") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "اختر نوع البحث",
                style = MaterialTheme.typography.titleLarge
            )

            ElevatedButton(
                onClick = onEmployees,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👨‍💼 البحث المتقدم للموظفين")
            }

            OutlinedButton(
                onClick = onPension,
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👴 البحث المتقدم للمتقاعدين والمعاشات — قريبًا")
            }

            Text(
                "تم إعداد البحث المتقدم ليعمل مباشرة على قاعدة البيانات المحلية.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun EmployeeAdvancedSearchScreen(
    repository: EmployeeRepository,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var query by rememberSaveable { mutableStateOf("") }
    var governorate by rememberSaveable { mutableStateOf("") }
    var district by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }

    var governorates by remember { mutableStateOf<List<String>>(emptyList()) }
    var districts by remember { mutableStateOf<List<String>>(emptyList()) }
    var locations by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var resultCount by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        governorates = withContext(Dispatchers.IO) {
            repository.getGovernorates()
        }
    }

    LaunchedEffect(governorate) {
        district = ""
        location = ""
        districts = if (governorate.isBlank()) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                repository.getDistricts(governorate)
            }
        }
        locations = emptyList()
    }

    LaunchedEffect(governorate, district) {
        if (governorate.isNotBlank()) {
            locations = withContext(Dispatchers.IO) {
                repository.getLocations(governorate, district)
            }
        } else {
            locations = emptyList()
        }
    }

    fun doSearch() {
        if (query.trim().isBlank() &&
            governorate.isBlank() &&
            district.isBlank() &&
            location.isBlank()
        ) {
            results = emptyList()
            resultCount = 0
            searched = false
            return
        }

        scope.launch {
            loading = true
            searched = true

            val filters = arrayOf(
                query.trim(),
                governorate,
                district,
                location
            )

            resultCount = withContext(Dispatchers.IO) {
                repository.countAdvanced(
                    filters[0],
                    filters[1],
                    filters[2],
                    filters[3]
                )
            }

            results = withContext(Dispatchers.IO) {
                repository.searchAdvanced(
                    filters[0],
                    filters[1],
                    filters[2],
                    filters[3],
                    100
                )
            }

            loading = false
        }
    }

    fun clearFilters() {
        query = ""
        governorate = ""
        district = ""
        location = ""
        districts = emptyList()
        locations = emptyList()
        results = emptyList()
        resultCount = 0
        searched = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بحث الموظفين المتقدم") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("الاسم أو رقم الموظف") },
                placeholder = { Text("يمكن تركه فارغًا واستخدام الفلاتر") }
            )

            Spacer(Modifier.height(10.dp))

            FilterDropdown(
                label = "المحافظة",
                value = governorate.ifBlank { "جميع المحافظات" },
                options = governorates,
                onSelected = { governorate = it }
            )

            Spacer(Modifier.height(8.dp))

            FilterDropdown(
                label = "المديرية",
                value = district.ifBlank { "جميع المديريات" },
                options = districts,
                enabled = governorate.isNotBlank(),
                onSelected = { district = it }
            )

            Spacer(Modifier.height(8.dp))

            FilterDropdown(
                label = "جهة العمل / الموقع",
                value = location.ifBlank { "جميع الجهات" },
                options = locations,
                enabled = governorate.isNotBlank(),
                onSelected = { location = it }
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { doSearch() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("🔎 بحث")
                }

                OutlinedButton(
                    onClick = { clearFilters() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("♻️ مسح")
                }
            }

            Spacer(Modifier.height(10.dp))

            if (searched) {
                Text(
                    "عدد النتائج: $resultCount" +
                            if (resultCount > 100) " (يتم عرض أول 100)" else "",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(Modifier.height(8.dp))

            if (loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (searched && !loading && results.isEmpty()) {
                Text("لا توجد نتائج مطابقة.")
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.id }) { employee ->
                    EmployeeResultCard(employee)
                }
            }
        }
    }
}

@Composable
private fun EmployeeResultCard(employee: Employee) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                employee.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text("رقم الموظف: ${employee.employeeNo ?: "غير متوفر"}")

            employee.location
                ?.takeIf { it.isNotBlank() }
                ?.let { Text("جهة العمل: $it") }
        }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = { if (enabled && options.isNotEmpty()) expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$label: $value")
                Text("▼")
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = { Text("الكل") },
                onClick = {
                    onSelected("")
                    expanded = false
                }
            )

            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

