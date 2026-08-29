package com.insuranceguide.app.presentation.directory
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.insuranceguide.app.data.database.*
@Composable fun DirectoryScreen(section:String,title:String,repo:DirectoryRepository,onBack:()->Unit){var q by rememberSaveable{mutableStateOf("")};var list by remember{mutableStateOf(emptyList<DirectoryItem>())};var selected by remember{mutableStateOf<DirectoryItem?>(null)};LaunchedEffect(q,section){list=repo.search(section,q)};selected?.let{Details(title,it){selected=null};return};Scaffold(topBar={TopAppBar(title={Text(title)},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})}){p->Column(Modifier.padding(p).padding(12.dp)){OutlinedTextField(q,{q=it},label={Text("بحث")},modifier=Modifier.fillMaxWidth());LazyColumn{items(list){x->ListItem(headlineContent={Text(x.title)},supportingContent={Text(x.subtitle?:"")},modifier=Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{selected=x});}}}}}
@Composable private fun Details(title:String,x:DirectoryItem,onBack:()->Unit){Scaffold(topBar={TopAppBar(title={Text(title)},navigationIcon={TextButton(onClick=onBack){Text("رجوع")}})}){p->LazyColumn(Modifier.padding(p).padding(16.dp)){item{Text(x.title,style=MaterialTheme.typography.headlineSmall)};items(x.details){d->ElevatedCard(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(d.first);Text(d.second?:"غير متوفر")}}}}}}
