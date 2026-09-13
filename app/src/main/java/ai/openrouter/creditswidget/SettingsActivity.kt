package ai.openrouter.creditswidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ai.openrouter.creditswidget.data.EncryptedKeyStore
import ai.openrouter.creditswidget.data.WidgetStore
import ai.openrouter.creditswidget.domain.ApiKeyMasker
import ai.openrouter.creditswidget.domain.ApiKeyNormalizer
import ai.openrouter.creditswidget.domain.RefreshInterval
import ai.openrouter.creditswidget.domain.StartPage
import ai.openrouter.creditswidget.worker.CreditsScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : ComponentActivity() {
    private var permissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionResult?.invoke(granted)
        permissionResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colors) { SettingsScreen(this) }
        }
    }

    fun notificationsAllowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Android suppresses toasts from apps whose notifications are disabled, and that state is per app, not per toast. */
    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (notificationsAllowed()) {
            onResult(true)
            return
        }
        permissionResult = onResult
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(activity: SettingsActivity) {
    val scope = rememberCoroutineScope()
    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var maskedSavedKey by remember { mutableStateOf<String?>(null) }
    var editingKey by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var interval by remember { mutableStateOf(RefreshInterval.HOUR_1) }
    var page by remember { mutableStateOf(StartPage.CREDITS) }
    var intervalOpen by remember { mutableStateOf(false) }
    var pageOpen by remember { mutableStateOf(false) }
    var notifyToast by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val saved = WidgetStore(activity).state()
        interval = saved.interval
        page = saved.startPage
        notifyToast = saved.notifyOnRefresh && activity.notificationsAllowed()
        val storedKey = withContext(Dispatchers.IO) { EncryptedKeyStore(activity).read() }
        maskedSavedKey = storedKey?.let(ApiKeyMasker::mask)
        editingKey = storedKey == null
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenRouter 위젯 설정") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("OpenRouter API 키", style = MaterialTheme.typography.titleMedium)

            if (maskedSavedKey != null && !editingKey) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("API 키 저장됨", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = maskedSavedKey.orEmpty(),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            label = { Text("현재 저장된 키") },
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { activity.finish() }, modifier = Modifier.weight(1f)) { Text("현재 키 유지") }
                            OutlinedButton(
                                onClick = {
                                    keyInput = ""
                                    showKey = false
                                    editingKey = true
                                    message = "새 API 키를 입력하세요"
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("키 변경하기") }
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("sk-or-…") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "숨김" else "표시") }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val normalized = ApiKeyNormalizer.normalize(keyInput)
                            if (normalized == null) {
                                message = "올바른 sk-or- API 키를 입력하세요"
                            } else {
                                scope.launch {
                                    withContext(Dispatchers.IO) { EncryptedKeyStore(activity).save(normalized) }
                                    WidgetStore(activity).saveSettings(interval, page)
                                    CreditsScheduler.schedule(activity, interval)
                                    CreditsScheduler.refreshNow(activity)
                                    maskedSavedKey = ApiKeyMasker.mask(normalized)
                                    keyInput = ""
                                    showKey = false
                                    editingKey = false
                                    message = "API 키를 저장하고 갱신을 시작했습니다"
                                }
                            }
                        },
                    ) { Text("확인 및 저장") }
                    if (maskedSavedKey != null) {
                        OutlinedButton(
                            onClick = {
                                keyInput = ""
                                showKey = false
                                editingKey = false
                                message = "기존 API 키를 유지합니다"
                            },
                        ) { Text("취소") }
                    }
                }
            }

            Text("자동 새로고침", style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(onClick = { intervalOpen = true }) { Text(interval.label) }
                DropdownMenu(expanded = intervalOpen, onDismissRequest = { intervalOpen = false }) {
                    RefreshInterval.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                interval = option
                                intervalOpen = false
                                scope.launch {
                                    WidgetStore(activity).saveSettings(interval, page)
                                    CreditsScheduler.schedule(activity, interval)
                                }
                            },
                        )
                    }
                }
            }

            Text("앱 시작 페이지", style = MaterialTheme.typography.titleMedium)
            Box {
                OutlinedButton(onClick = { pageOpen = true }) { Text(page.label) }
                DropdownMenu(expanded = pageOpen, onDismissRequest = { pageOpen = false }) {
                    StartPage.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                page = option
                                pageOpen = false
                                scope.launch { WidgetStore(activity).saveSettings(interval, page) }
                            },
                        )
                    }
                }
            }

            Text("절전 모드에서는 새로고침 시각이 지연될 수 있습니다.", style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = notifyToast,
                    onCheckedChange = { checked ->
                        if (checked) {
                            activity.requestNotificationPermission { granted ->
                                notifyToast = granted
                                scope.launch { WidgetStore(activity).saveNotifyOnRefresh(granted) }
                                if (!granted) message = "알림 권한이 없어 토스트를 표시할 수 없습니다"
                            }
                        } else {
                            notifyToast = false
                            scope.launch { WidgetStore(activity).saveNotifyOnRefresh(false) }
                        }
                    },
                )
                Text("새로고침 완료를 토스트로 표시", style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = { CreditsScheduler.refreshNow(activity); message = "지금 갱신을 요청했습니다" }) {
                Text("지금 새로고침")
            }
            if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
        }
    }
}
