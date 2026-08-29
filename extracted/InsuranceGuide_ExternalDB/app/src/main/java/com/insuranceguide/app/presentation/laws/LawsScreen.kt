package com.insuranceguide.app.presentation.laws

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.insuranceguide.app.data.database.LawItem
import com.insuranceguide.app.data.database.LawRepository

@Composable fun LawsScreen(repo:LawRepository, procedures:Boolean, onBack:()->Unit){
 var selected by remember{mutableStateOf<LawItem?>(null)}
 val items=remember{if(procedures) repo.procedures() else repo.laws()}
 if(selected!=null){LawContent(selected!!,onBack={selected=null})} else Column(Modifier.fillMaxSize().padding(16.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(if(procedures)"الإجراءات والمعاملات" else "القوانين",style=MaterialTheme.typography.headlineSmall);TextButton(onClick=onBack){Text("الرئيسية")}}
   Spacer(Modifier.height(8.dp)); LazyColumn{items(items){item->Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{selected=item}){Text(item.title,Modifier.padding(16.dp))}}}
 }
}
@SuppressLint("SetJavaScriptEnabled") @Composable fun LawContent(item:LawItem,onBack:()->Unit){Column(Modifier.fillMaxSize()){Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(item.title,Modifier.weight(1f));TextButton(onClick=onBack){Text("رجوع")}};AndroidView(factory={ctx->WebView(ctx).apply{webViewClient=WebViewClient();settings.javaScriptEnabled=false;settings.allowFileAccess=true;val p=item.path.substringAfterLast("/android_asset/proc/");loadUrl("file:///android_asset/content/proc/$p")}},modifier=Modifier.fillMaxSize())}}
