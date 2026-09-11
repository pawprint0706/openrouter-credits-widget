package ai.openrouter.creditswidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import ai.openrouter.creditswidget.data.EncryptedKeyStore
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.domain.ApiKeyNormalizer
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import ai.openrouter.creditswidget.worker.CreditsScheduler
import ai.openrouter.creditswidget.widget.CreditsWidget
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(); MaterialTheme(colorScheme = colors) { SettingsScreen(this) } } }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(activity: SettingsActivity) {
    val scope = rememberCoroutineScope(); var key by remember { mutableStateOf("") }; var show by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }; var interval by remember { mutableStateOf(RefreshInterval.HOUR_1) }; var page by remember { mutableStateOf(StartPage.CREDITS) }; var intervalOpen by remember { mutableStateOf(false) }; var pageOpen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { val saved = WidgetStore(activity).state(); interval = saved.interval; page = saved.startPage }
    Scaffold(topBar = { TopAppBar(title = { Text("OpenRouter 위젯 설정") }) }) { pad -> Column(Modifier.padding(pad).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("OpenRouter API 키", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(), label = { Text("sk-or-…") }, visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "숨김" else "표시") } })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { val normalized = ApiKeyNormalizer.normalize(key); if (normalized == null) message = "올바른 sk-or- API 키를 입력하세요" else { EncryptedKeyStore(activity).save(normalized); key = ""; scope.launch { WidgetStore(activity).saveSettings(interval, page); CreditsScheduler.schedule(activity, interval); CreditsScheduler.refreshNow(activity); message = "저장하고 갱신을 시작했습니다" } } }) { Text("확인 및 저장") }; OutlinedButton(onClick = { EncryptedKeyStore(activity).delete(); key = ""; scope.launch { WidgetStore(activity).clearSnapshot(); CreditsWidget().updateAll(activity); message = "키를 삭제했습니다" } }) { Text("키 삭제") } }
        Text("자동 새로고침", style = MaterialTheme.typography.titleMedium)
        Box { OutlinedButton(onClick = { intervalOpen = true }) { Text(interval.label) }; DropdownMenu(expanded = intervalOpen, onDismissRequest = { intervalOpen = false }) { RefreshInterval.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { interval = option; intervalOpen = false; scope.launch { WidgetStore(activity).saveSettings(interval, page); CreditsScheduler.schedule(activity, interval) } }) } } }
        Text("앱 시작 페이지", style = MaterialTheme.typography.titleMedium)
        Box { OutlinedButton(onClick = { pageOpen = true }) { Text(page.label) }; DropdownMenu(expanded = pageOpen, onDismissRequest = { pageOpen = false }) { StartPage.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { page = option; pageOpen = false; scope.launch { WidgetStore(activity).saveSettings(interval, page) } }) } } }
        Text("절전 모드에서는 새로고침 시각이 지연될 수 있습니다.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { CreditsScheduler.refreshNow(activity); message = "지금 갱신을 요청했습니다" }) { Text("지금 새로고침") }
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
    } }
}
