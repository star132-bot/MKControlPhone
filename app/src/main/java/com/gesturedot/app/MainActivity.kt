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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val disclosureAccepted by preferences.hasAcceptedAccessibilityDisclosure
        .collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var serviceEnabled by remember { mutableStateOf(isGestureServiceEnabled(context)) }
    var showPermissionDisclosure by remember { mutableStateOf(false) }

    fun openAccessibilitySettings() {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

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
                    if (serviceEnabled || disclosureAccepted) {
                        openAccessibilitySettings()
                    } else {
                        showPermissionDisclosure = true
                    }
                },
            )

            PermissionSummaryCard()

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

    if (showPermissionDisclosure) {
        PermissionDisclosureDialog(
            onDismiss = { showPermissionDisclosure = false },
            onConfirm = {
                showPermissionDisclosure = false
                scope.launch {
                    preferences.acceptAccessibilityDisclosure()
                    openAccessibilitySettings()
                }
            },
        )
    }
}

@Composable
private fun PermissionDisclosureDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("启用手势服务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("复刻球需要 Android 无障碍服务，才能在你点击悬浮球后执行已配置的点击和滑动。")
                Text("服务会：")
                Text("• 在其他 App 上方显示复刻球\n• 执行你主动触发的确定性手势\n• 检测服务是否已开启")
                Text("服务不会：")
                Text("• 读取页面文字、通知或密码\n• 截取或上传屏幕内容\n• 在你未触发时自行操作")
                Text(
                    "授权将在 Android 系统设置中完成，你可以随时关闭。",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("同意并前往设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暂不启用")
            }
        },
    )
}

@Composable
private fun PermissionSummaryCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("权限与隐私", style = MaterialTheme.typography.titleMedium)
            Text("需要：手势服务", color = MaterialTheme.colorScheme.primary)
            Text(
                "不需要网络权限，也不额外申请普通悬浮窗权限。悬浮球由无障碍服务提供的安全覆盖层显示。",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
