package com.insuranceguide.app

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import com.insuranceguide.app.data.database.*
import com.insuranceguide.app.presentation.advanced.AdvancedSearchScreen
import com.insuranceguide.app.presentation.calculators.CalculatorsScreen
import com.insuranceguide.app.presentation.database.DatabaseScreen
import com.insuranceguide.app.presentation.directory.DirectoryScreen
import com.insuranceguide.app.presentation.employees.EmployeesScreen
import com.insuranceguide.app.presentation.laws.LawsScreen
import com.insuranceguide.app.presentation.pension.PensionScreen

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            InsuranceGuideApp()
        }
    }

    /*
     * منع إعادة إنشاء Activity عند تدوير الشاشة،
     * إذا كان AndroidManifest مضبوطًا على configChanges.
     *
     * لا يغيّر هذا أي شيء في قاعدة البيانات.
     */
    override fun onConfigurationChanged(
        newConfig: Configuration
    ) {
        super.onConfigurationChanged(newConfig)
    }


    @Composable
    private fun InsuranceGuideApp() {

        /*
         * مدير قاعدة البيانات
         */
        val manager = remember {
            DatabaseManager(this@MainActivity)
        }

        val scope = rememberCoroutineScope()

        /*
         * معلومات قاعدة البيانات
         */
        var databaseInfo by remember {
            mutableStateOf(
                manager.currentInfo()
            )
        }

        /*
         * أخطاء قاعدة البيانات
         */
        var databaseError by remember {
            mutableStateOf<String?>(null)
        }

        /*
         * رقم نسخة الاتصال بقاعدة البيانات.
         *
         * يتغير عند الاستيراد أو الحذف حتى يتم
         * إنشاء Repository جديد بالاتصال الصحيح.
         */
        var databaseVersion by remember {
            mutableIntStateOf(0)
        }

        /*
         * الشاشة الحالية
         */
        var screen by rememberSaveable {
            mutableStateOf("home")
        }

        /*
         * عنوان شاشة الأدلة
         */
        var directoryTitle by rememberSaveable {
            mutableStateOf("")
        }


        /*
         * ========================================================
         * اختيار واستيراد قاعدة البيانات
         * ========================================================
         */

        val picker =
            rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->

                if (uri == null) {
                    return@rememberLauncherForActivityResult
                }

                scope.launch(
                    Dispatchers.IO
                ) {

                    val result = runCatching {

                        contentResolver
                            .takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )

                        manager.import(uri)
                    }

                    withContext(
                        Dispatchers.Main
                    ) {

                        result
                            .onSuccess { info ->

                                /*
                                 * إعادة ضبط اتصال قاعدة البيانات
                                 */
                                InsuranceDatabase.reset(
                                    this@MainActivity
                                )

                                databaseInfo = info
                                databaseError = null

                                databaseVersion++

                                screen = "home"
                            }

                            .onFailure { e ->

                                databaseError =
                                    e.message
                                        ?: "تعذر استيراد قاعدة البيانات"
                            }
                    }
                }
            }


        /*
         * ========================================================
         * واجهة التطبيق
         * ========================================================
         */

        MaterialTheme {

            Surface(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * ==================================================
                 * إدارة قاعدة البيانات
                 * ==================================================
                 */

                if (screen == "database") {

                    BackHandler {

                        screen = "home"
                    }

                    DatabaseScreen(
                        databaseInfo,

                        onPick = {

                            picker.launch(
                                arrayOf("*/*")
                            )
                        },

                        onClear = {

                            scope.launch(
                                Dispatchers.IO
                            ) {

                                manager.clear()

                                InsuranceDatabase.reset(
                                    this@MainActivity
                                )

                                withContext(
                                    Dispatchers.Main
                                ) {

                                    databaseInfo = null

                                    databaseVersion++

                                    screen = "database"
                                }
                            }
                        },

                        onBack = {

                            screen = "home"
                        },

                        message = databaseError
                    )
                }


                /*
                 * ==================================================
                 * لا توجد قاعدة بيانات متوافقة
                 * ==================================================
                 */

                else if (
                    databaseInfo == null ||
                    !databaseInfo!!.isValid ||
                    !databaseInfo!!.isCompatible
                ) {

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,

                            modifier =
                                Modifier.padding(24.dp)
                        ) {

                            Text(
                                "الدليل التأميني",

                                style =
                                    MaterialTheme
                                        .typography
                                        .headlineMedium
                            )

                            Spacer(
                                Modifier.height(12.dp)
                            )

                            Text(
                                databaseError
                                    ?: "لم يتم تحديد قاعدة بيانات متوافقة."
                            )

                            Spacer(
                                Modifier.height(16.dp)
                            )

                            Button(
                                onClick = {

                                    screen = "database"
                                }
                            ) {

                                Text(
                                    "📂 اختيار قاعدة البيانات"
                                )
                            }
                        }
                    }
                }


                /*
                 * ==================================================
                 * التطبيق الرئيسي
                 * ==================================================
                 */

                else {

                    /*
                     * زر الرجوع للنظام
                     */
                    if (screen != "home") {

                        BackHandler {

                            screen = "home"
                        }
                    }


                    when {

                        /*
                         * ==================================================
                         * البحث المتقدم
                         * ==================================================
                         */

                        screen == "advancedSearch" -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                EmployeeRepository(
                                    this@MainActivity
                                )
                            }

                            AdvancedSearchScreen(
                                repository = repo,

                                onBack = {

                                    screen = "home"
                                }
                            )
                        }


                        /*
                         * ==================================================
                         * الموظفون
                         * ==================================================
                         */

                        screen == "employees" -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                EmployeeRepository(
                                    this@MainActivity
                                )
                            }

                            EmployeesScreen(repo) {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * المتقاعدون والمعاشات
                         * ==================================================
                         */

                        screen == "pension" -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                PensionRepository(
                                    this@MainActivity
                                )
                            }

                            PensionScreen(repo) {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * القوانين
                         * ==================================================
                         */

                        screen == "laws" -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                LawRepository(
                                    this@MainActivity
                                )
                            }

                            LawsScreen(
                                repo,
                                false
                            ) {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * الإجراءات والمعاملات
                         * ==================================================
                         */

                        screen == "procedures" -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                LawRepository(
                                    this@MainActivity
                                )
                            }

                            LawsScreen(
                                repo,
                                true
                            ) {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * الحاسبات
                         * ==================================================
                         */

                        screen == "calculators" -> {

                            CalculatorsScreen {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * الأدلة
                         * ==================================================
                         */

                        screen.startsWith("dir:") -> {

                            val repo = remember(
                                databaseVersion
                            ) {

                                DirectoryRepository(
                                    this@MainActivity
                                )
                            }

                            DirectoryScreen(

                                screen.removePrefix(
                                    "dir:"
                                ),

                                directoryTitle,

                                repo
                            ) {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * حول التطبيق
                         * ==================================================
                         */

                        screen == "about" -> {

                            AboutScreen {

                                screen = "home"
                            }
                        }


                        /*
                         * ==================================================
                         * الصفحة الرئيسية
                         * ==================================================
                         */

                        else -> {

                            HomeScreen(

                                onEmployees = {

                                    screen = "employees"
                                },

                                onPension = {

                                    screen = "pension"
                                },

                                onAdvancedSearch = {

                                    screen = "advancedSearch"
                                },

                                onDirectory = {
                                    section,
                                    title ->

                                    directoryTitle = title

                                    screen =
                                        "dir:$section"
                                },

                                onLaws = {

                                    screen = "laws"
                                },

                                onProcedures = {

                                    screen = "procedures"
                                },

                                onCalculators = {

                                    screen = "calculators"
                                },

                                onDatabase = {

                                    screen = "database"
                                },

                                onAbout = {

                                    screen = "about"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


/*
 * ============================================================
 * الشاشة الرئيسية
 * ============================================================
 */

@Composable
fun HomeScreen(

    onEmployees: () -> Unit,

    onPension: () -> Unit,

    onAdvancedSearch: () -> Unit,

    onDirectory: (
        String,
        String
    ) -> Unit,

    onLaws: () -> Unit,

    onProcedures: () -> Unit,

    onCalculators: () -> Unit,

    onDatabase: () -> Unit,

    onAbout: () -> Unit

) {

    /*
     * ========================================================
     * حل مشكلة عدم إمكانية النزول في الصفحة
     * ========================================================
     *
     * الصفحة أصبحت قابلة للتمرير عموديًا.
     */

    val scrollState =
        rememberScrollState()


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    scrollState
                )
                .padding(24.dp)

    ) {

        /*
         * عنوان التطبيق
         */

        Text(

            "الدليل التأميني",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            Modifier.height(18.dp)
        )


        /*
         * ========================================================
         * بحث الموظفين والمتقاعدين
         * ========================================================
         */

        listOf(

            "بحث الموظفين" to onEmployees,

            "بحث المتقاعدين والمعاشات" to onPension

        ).forEach { (title, action) ->

            ElevatedButton(

                onClick = action,

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(title)
            }


            Spacer(
                Modifier.height(10.dp)
            )
        }


        /*
         * ========================================================
         * البحث المتقدم
         * ========================================================
         */

        OutlinedButton(

            onClick =
                onAdvancedSearch,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "🔎 البحث المتقدم"
            )
        }


        Spacer(
            Modifier.height(10.dp)
        )


        /*
         * ========================================================
         * الأدلة
         * ========================================================
         */

        listOf(

            "BANKS" to "البنوك",

            "BRANCHES" to "الفروع",

            "GOVS" to "المحافظات",

            "POST" to "مكاتب البريد",

            "PHONE" to "دليل الهاتف"

        ).forEach { (section, title) ->

            ElevatedButton(

                onClick = {

                    onDirectory(
                        section,
                        title
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(title)
            }


            Spacer(
                Modifier.height(8.dp)
            )
        }


        /*
         * ========================================================
         * القوانين
         * ========================================================
         */

        ElevatedButton(

            onClick = onLaws,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "القوانين"
            )
        }


        Spacer(
            Modifier.height(8.dp)
        )


        /*
         * ========================================================
         * الإجراءات والمعاملات
         * ========================================================
         */

        ElevatedButton(

            onClick =
                onProcedures,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "الإجراءات والمعاملات"
            )
        }


        Spacer(
            Modifier.height(8.dp)
        )


        /*
         * ========================================================
         * الحاسبات
         * ========================================================
         */

        ElevatedButton(

            onClick =
                onCalculators,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "الحاسبات"
            )
        }


        Spacer(
            Modifier.height(8.dp)
        )


        /*
         * ========================================================
         * إدارة قاعدة البيانات
         * ========================================================
         */

        OutlinedButton(

            onClick =
                onDatabase,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "⚙️ إدارة قاعدة البيانات"
            )
        }


        Spacer(
            Modifier.height(8.dp)
        )


        /*
         * ========================================================
         * حول التطبيق
         * ========================================================
         */

        OutlinedButton(

            onClick =
                onAbout,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "ℹ️ حول التطبيق"
            )
        }


        Spacer(
            Modifier.height(12.dp)
        )


        /*
         * ========================================================
         * معلومات التشغيل
         * ========================================================
         */

        Text(

            "البيانات محلية ولا يستخدم التطبيق GPS أو الخرائط.",

            modifier =
                Modifier.padding(
                    top = 4.dp
                )
        )


        /*
         * مساحة إضافية أسفل الصفحة
         * حتى لا يلتصق آخر عنصر بحافة الشاشة.
         */

        Spacer(
            Modifier.height(24.dp)
        )
    }
}


/*
 * ============================================================
 * شاشة حول التطبيق
 * ============================================================
 */

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {

    BackHandler {

        onBack()
    }


    val context =
        LocalContext.current


    val versionName =
        remember {

            runCatching {

                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )
                    .versionName
                    ?: "غير محدد"

            }.getOrDefault(
                "غير محدد"
            )
        }


    /*
     * جعل شاشة حول التطبيق قابلة للتمرير
     * أيضًا على الشاشات الصغيرة.
     */

    val scrollState =
        rememberScrollState()


    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    scrollState
                )
                .padding(24.dp),

        horizontalAlignment =
            Alignment.CenterHorizontally

    ) {

        Spacer(
            Modifier.height(32.dp)
        )


        /*
         * عنوان الصفحة
         */

        Text(

            "حول التطبيق",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium
        )


        Spacer(
            Modifier.height(24.dp)
        )


        /*
         * ========================================================
         * بطاقة معلومات التطبيق
         * ========================================================
         */

        Card(

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Column(

                modifier =
                    Modifier.padding(24.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally

            ) {

                /*
                 * اسم التطبيق
                 */

                Text(

                    "الدليل التأميني",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                /*
                 * وصف التطبيق
                 */

                Text(

                    "دليل تأميني شامل للبحث والوصول إلى المعلومات " +
                            "والبيانات والخدمات التأمينية.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )


                Spacer(
                    Modifier.height(24.dp)
                )


                HorizontalDivider()


                Spacer(
                    Modifier.height(20.dp)
                )


                /*
                 * حقوق التطوير
                 */

                Text(

                    "تم تطوير التطبيق وتحديثه ليتوافق مع " +
                            "متطلبات التشغيل الحديثة من قبل",

                    style =
                        MaterialTheme
                            .typography
                            .bodyLarge
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                Text(

                    "أبو عبدالرحمن عاصم محمد",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )


                Spacer(
                    Modifier.height(20.dp)
                )


                /*
                 * سنة التطوير
                 */

                Text(

                    "سنة التطوير والتحديث: 2026",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )


                Spacer(
                    Modifier.height(10.dp)
                )


                /*
                 * إصدار التطبيق
                 */

                Text(

                    "إصدار التطبيق: $versionName",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )


                Spacer(
                    Modifier.height(24.dp)
                )


                HorizontalDivider()


                Spacer(
                    Modifier.height(20.dp)
                )


                /*
                 * حقوق الملكية
                 */

                Text(

                    "© 2026 أبو عبدالرحمن عاصم محمد",

                    style =
                        MaterialTheme
                            .typography
                            .titleSmall
                )


                Spacer(
                    Modifier.height(8.dp)
                )


                Text(

                    "جميع الحقوق محفوظة.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )


                Spacer(
                    Modifier.height(16.dp)
                )


                /*
                 * التشغيل المحلي
                 */

                Text(

                    "البيانات محلية ولا يعتمد التطبيق " +
                            "على GPS أو الخرائط.",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }


        Spacer(
            Modifier.height(24.dp)
        )


        /*
         * زر العودة
         */

        Button(

            onClick = onBack,

            modifier =
                Modifier.fillMaxWidth()

        ) {

            Text(
                "العودة"
            )
        }


        Spacer(
            Modifier.height(24.dp)
        )
    }
}
