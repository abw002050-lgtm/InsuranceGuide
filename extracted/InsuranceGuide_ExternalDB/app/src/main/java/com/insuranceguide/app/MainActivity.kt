package com.insuranceguide.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.*
import com.insuranceguide.app.presentation.calculators.CalculatorsScreen
import com.insuranceguide.app.presentation.database.DatabaseScreen
import com.insuranceguide.app.presentation.directory.DirectoryScreen
import com.insuranceguide.app.presentation.employees.EmployeesScreen
import com.insuranceguide.app.presentation.laws.LawsScreen
import com.insuranceguide.app.presentation.pension.PensionScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InsuranceGuideApp() }
    }

    @Composable
    private fun InsuranceGuideApp() {
        val manager = remember { DatabaseManager(this@MainActivity) }
        val scope = rememberCoroutineScope()
        var databaseInfo by remember { mutableStateOf(manager.currentInfo()) }
        var databaseError by remember { mutableStateOf<String?>(null) }
        var databaseVersion by remember { mutableIntStateOf(0) }
        var screen by rememberSaveable { mutableStateOf("home") }
        var directoryTitle by rememberSaveable { mutableStateOf("") }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch(Dispatchers.IO) {
                val result = runCatching {
                    contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    manager.import(uri)
                }
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    result.onSuccess { info ->
                        InsuranceDatabase.reset(this@MainActivity)
                        databaseInfo = info
                        databaseError = null
                        databaseVersion++
                        screen = "home"
                    }.onFailure { e -> databaseError = e.message ?: "تعذر استيراد قاعدة البيانات" }
                }
            }
        }

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                if (screen == "database") {
                    BackHandler { screen = "home" }
                    DatabaseScreen(databaseInfo, onPick = { picker.launch(arrayOf("*/*")) }, onClear = {
                        scope.launch(Dispatchers.IO) { manager.clear(); InsuranceDatabase.reset(this@MainActivity); kotlinx.coroutines.withContext(Dispatchers.Main) { databaseInfo = null; databaseVersion++; screen = "database" } }
                    }, onBack = { screen = "home" }, message = databaseError)
                } else if (databaseInfo == null || !databaseInfo!!.isValid || !databaseInfo!!.isCompatible) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Text("الدليل التأميني", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(12.dp))
                            Text(databaseError ?: "لم يتم تحديد قاعدة بيانات متوافقة.")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { screen = "database" }) { Text("📂 اختيار قاعدة البيانات") }
                        }
                    }
                } else {
                    if (screen != "home") BackHandler { screen = "home" }
                    when {
                        screen == "employees" -> { val repo = remember(databaseVersion) { EmployeeRepository(this@MainActivity) }; EmployeesScreen(repo) { screen = "home" } }
                        screen == "pension" -> { val repo = remember(databaseVersion) { PensionRepository(this@MainActivity) }; PensionScreen(repo) { screen = "home" } }
                        screen == "laws" -> { val repo = remember(databaseVersion) { LawRepository(this@MainActivity) }; LawsScreen(repo, false) { screen = "home" } }
                        screen == "procedures" -> { val repo = remember(databaseVersion) { LawRepository(this@MainActivity) }; LawsScreen(repo, true) { screen = "home" } }
                        screen == "calculators" -> CalculatorsScreen { screen = "home" }
                        screen.startsWith("dir:") -> { val repo = remember(databaseVersion) { DirectoryRepository(this@MainActivity) }; DirectoryScreen(screen.removePrefix("dir:"), directoryTitle, repo) { screen = "home" } }
                        else -> HomeScreen(
                            onEmployees = { screen = "employees" }, onPension = { screen = "pension" },
                            onDirectory = { section, title -> directoryTitle = title; screen = "dir:$section" },
                            onLaws = { screen = "laws" }, onProcedures = { screen = "procedures" },
                            onCalculators = { screen = "calculators" }, onDatabase = { screen = "database" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(onEmployees: () -> Unit, onPension: () -> Unit, onDirectory: (String, String) -> Unit, onLaws: () -> Unit, onProcedures: () -> Unit, onCalculators: () -> Unit, onDatabase: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("الدليل التأميني", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(18.dp))
        listOf("بحث الموظفين" to onEmployees, "بحث المتقاعدين والمعاشات" to onPension).forEach { (title, action) ->
            ElevatedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(title) }; Spacer(Modifier.height(10.dp))
        }
        listOf("BANKS" to "البنوك", "BRANCHES" to "الفروع", "GOVS" to "المحافظات", "POST" to "مكاتب البريد", "PHONE" to "دليل الهاتف").forEach { (section, title) ->
            ElevatedButton(onClick = { onDirectory(section, title) }, modifier = Modifier.fillMaxWidth()) { Text(title) }; Spacer(Modifier.height(8.dp))
        }
        ElevatedButton(onClick = onLaws, modifier = Modifier.fillMaxWidth()) { Text("القوانين") }
        Spacer(Modifier.height(8.dp)); ElevatedButton(onClick = onProcedures, modifier = Modifier.fillMaxWidth()) { Text("الإجراءات والمعاملات") }
        Spacer(Modifier.height(8.dp)); ElevatedButton(onClick = onCalculators, modifier = Modifier.fillMaxWidth()) { Text("الحاسبات") }
        Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = onDatabase, modifier = Modifier.fillMaxWidth()) { Text("⚙️ إدارة قاعدة البيانات") }
        Text("البيانات محلية ولا يستخدم التطبيق GPS أو الخرائط.", Modifier.padding(top = 12.dp))
    }
}
