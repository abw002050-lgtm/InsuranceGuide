package com.insuranceguide.app.presentation.directory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.DirectoryItem
import com.insuranceguide.app.data.database.DirectoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    section: String,
    title: String,
    repo: DirectoryRepository,
    onBack: () -> Unit
) {
    var q by rememberSaveable { mutableStateOf("") }
    var list by remember { mutableStateOf<List<DirectoryItem>>(emptyList()) }
    var selected by remember { mutableStateOf<DirectoryItem?>(null) }

    LaunchedEffect(q, section) {
        list = repo.search(section, q)
    }

    selected?.let { item ->
        Details(title, item) {
            selected = null
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("رجوع")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize()
        ) {

            OutlinedTextField(
                value = q,
                onValueChange = { newValue ->
                    q = newValue
                },
                label = {
                    Text("بحث")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = list,
                    key = { it.id }
                ) { item ->

                    ListItem(
                        headlineContent = {
                            Text(item.title)
                        },
                        supportingContent = {
                            Text(item.subtitle ?: "")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = item
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Details(
    title: String,
    item: DirectoryItem,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title)
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("رجوع")
                    }
                }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                Text(
                    item.title,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            items(item.details) { detail ->

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(detail.first)
                        Text(detail.second ?: "غير متوفر")
                    }
                }
            }
        }
    }
}
