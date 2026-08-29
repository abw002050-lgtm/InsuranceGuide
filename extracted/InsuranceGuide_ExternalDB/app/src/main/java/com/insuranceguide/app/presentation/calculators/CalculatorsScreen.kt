package com.insuranceguide.app.presentation.calculators

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Period

@Composable fun CalculatorsScreen(onBack:()->Unit){var tab by remember{mutableIntStateOf(0)}; Column(Modifier.fillMaxSize().padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("الحاسبات",style=MaterialTheme.typography.headlineSmall);TextButton(onClick=onBack){Text("الرئيسية")}};TabRow(selectedTabIndex=tab){listOf("العمر","المدة","سنوات الخدمة").forEachIndexed{i,t->Tab(selected=i==tab,onClick={tab=i},text={Text(t)})}};Spacer(Modifier.height(20.dp));when(tab){0->AgeCalculator();1->DurationCalculator();else->ServiceCalculator()}}}
@Composable private fun DateFields(labels:List<String>):Pair<List<String>,List<(String)->Unit>>{val values=labels.map{remember{mutableStateOf("")}};labels.forEachIndexed{i,l->OutlinedTextField(values[i].value,{values[i].value=it},label={Text(l+" YYYY-MM-DD")},modifier=Modifier.fillMaxWidth())};return values.map{it.value} to values.map{s->{v:String->s.value=v}}}
@Composable private fun AgeCalculator(){var birth by remember{mutableStateOf("")};var result by remember{mutableStateOf("")};OutlinedTextField(birth,{birth=it},label={Text("تاريخ الميلاد YYYY-MM-DD")},modifier=Modifier.fillMaxWidth());Button(onClick={result=runCatching{val p=Period.between(LocalDate.parse(birth),LocalDate.now());"${p.years} سنة، ${p.months} شهر، ${p.days} يوم"}.getOrElse{"أدخل تاريخًا صحيحًا"}},modifier=Modifier.padding(top=12.dp)){Text("احسب العمر")};Text(result,Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleMedium)}
@Composable private fun DurationCalculator(){var start by remember{mutableStateOf("")};var end by remember{mutableStateOf("")};var result by remember{mutableStateOf("")};OutlinedTextField(start,{start=it},label={Text("تاريخ البداية YYYY-MM-DD")},modifier=Modifier.fillMaxWidth());OutlinedTextField(end,{end=it},label={Text("تاريخ النهاية YYYY-MM-DD")},modifier=Modifier.fillMaxWidth());Button(onClick={result=runCatching{val p=Period.between(LocalDate.parse(start),LocalDate.parse(end));"${p.years} سنة، ${p.months} شهر، ${p.days} يوم"}.getOrElse{"أدخل تاريخين صحيحين"}},modifier=Modifier.padding(top=12.dp)){Text("احسب المدة")};Text(result,Modifier.padding(top=16.dp),style=MaterialTheme.typography.titleMedium)}
@Composable private fun ServiceCalculator(){DurationCalculator()}
