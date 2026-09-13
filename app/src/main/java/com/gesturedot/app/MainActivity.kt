package com.gesturedot.app

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gesturedot.app.accessibility.GestureAccessibilityService
import com.gesturedot.app.data.UserPreferences
import com.gesturedot.app.ui.theme.GestureDotTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GestureDotTheme {
                GestureDotApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GestureDotApp() {
    val context = LocalContext.current
    val preferences = remember { UserPreferences(context.applicationContext) }
    val bubbleEnabled by preferences.bubbleEnabled.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    var serviceEnabled by remember { mutableStateOf(isGestureServiceEnabled(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        serviceEnabled = isGestureServiceEnabled(context)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("复刻球") }) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "一次录制，一键复现",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "你主动点击悬浮球时，复刻球才会执行已经配置好的手势。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ServiceCard(
                enabled = serviceEnabled,
                onOpenSettings = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                },
            )

            SettingCard(
                title = "显示悬浮球",
                description = if (serviceEnabled) "单击执行默认动作，拖动调整位置" else "启用手势服务后显示",
                checked = bubbleEnabled,
                enabled = serviceEnabled,
                onCheckedChange = { checked ->
                    scope.launch { preferences.setBubbleEnabled(checked) }
                },
            )

            ActionCard()

            Button(
                onClick = { /* Recording screen is milestone M2. */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            ) {
                Text("录制新动作（下一阶段）")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServiceCard(enabled: Boolean, onOpenSettings: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("手势服务", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (enabled) "已启用，可以执行手势" else "未启用，无法显示悬浮球",
                        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
                FilledTonalButton(onClick = onOpenSettings) {
                    Text(if (enabled) "查看" else "去启用")
                }
            }
            Text(
                "服务只负责执行你主动触发的动作；当前版本不读取页面文字和密码。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun ActionCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("默认动作", style = MaterialTheme.typography.labelLarge)
            Text("向右滑动", style = MaterialTheme.typography.titleLarge)
            Text(
                "从屏幕 22% 位置滑至 78%，持续 360 毫秒",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "单击悬浮球执行；连续三击暂时执行同一动作。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun isGestureServiceEnabled(context: android.content.Context): Boolean {
    val expected = ComponentName(context, GestureAccessibilityService::class.java).flattenToString()
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty()
    return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
}
