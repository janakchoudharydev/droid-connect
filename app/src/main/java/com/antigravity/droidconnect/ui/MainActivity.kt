package com.antigravity.droidconnect.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.antigravity.droidconnect.deliverer.LocalHttpDeliverer
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AntiGravityConfigScreen()
                }
            }
        }
    }
}

@Composable
fun AntiGravityConfigScreen() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(LocalHttpDeliverer.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isServiceEnabled by remember {
        mutableStateOf(prefs.getBoolean(LocalHttpDeliverer.KEY_FORWARDING_ACTIVE, true))
    }

    var serverEndpoint by remember {
        mutableStateOf(
            prefs.getString(
                LocalHttpDeliverer.KEY_SERVER_ENDPOINT,
                LocalHttpDeliverer.DEFAULT_ENDPOINT
            ) ?: LocalHttpDeliverer.DEFAULT_ENDPOINT
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(isNotificationListenerGranted(context))
    }

    // Refresh permission state and ensure listener is bound on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = isNotificationListenerGranted(context)
                if (hasNotificationPermission) {
                    try {
                        NotificationListenerService.requestRebind(
                            android.content.ComponentName(context, com.antigravity.droidconnect.service.AntiGravityNotificationService::class.java)
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed auto requestRebind: ${e.message}")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Title Header
            Column {
                Text(
                    text = "Anti Gravity",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Local Wi-Fi Notification Sync (macOS)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 1. Master Start/Stop Toggle Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isServiceEnabled) "Forwarding Active" else "Forwarding Paused",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isServiceEnabled) "Listening & sending to Mac" else "Notifications will not be forwarded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = isServiceEnabled,
                        onCheckedChange = { checked ->
                            isServiceEnabled = checked
                            prefs.edit().putBoolean(LocalHttpDeliverer.KEY_FORWARDING_ACTIVE, checked).apply()
                        }
                    )
                }
            }

            // 2. Notification Access Permission Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "System Permission",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasNotificationPermission)
                            "Notification Listener Access: GRANTED"
                        else
                            "Notification Listener Access: REQUIRED",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasNotificationPermission)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )

                    if (!hasNotificationPermission) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Notification Access")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    android.service.notification.NotificationListenerService.requestRebind(
                                        android.content.ComponentName(context, com.antigravity.droidconnect.service.AntiGravityNotificationService::class.java)
                                    )
                                    android.widget.Toast.makeText(context, "Listener service rebound successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(context, "Rebind error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reconnect Listener Service")
                        }
                    }
                }
            }

            // 3. Mac Local IP & Port Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Mac Receiver Endpoint",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your Mac's Wi-Fi IP and Node.js port",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = serverEndpoint,
                        onValueChange = { newValue ->
                            serverEndpoint = newValue
                            prefs.edit().putString(LocalHttpDeliverer.KEY_SERVER_ENDPOINT, newValue.trim()).apply()
                        },
                        label = { Text("IP:Port (e.g. 192.168.1.121:3000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                    Button(
                        onClick = {
                            prefs.edit().putString(LocalHttpDeliverer.KEY_SERVER_ENDPOINT, serverEndpoint.trim()).apply()
                            coroutineScope.launch {
                                val deliverer = LocalHttpDeliverer(context)
                                val result = deliverer.deliver(
                                    packageName = "com.whatsapp",
                                    appName = "WhatsApp",
                                    title = "Artolika Office",
                                    text = "Live Test from Anti Gravity App!"
                                )
                                if (result.isSuccess) {
                                    android.widget.Toast.makeText(context, "Notification sent to Mac!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    android.widget.Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Send Test Notification")
                    }
                }
            }
        }
    }
}

private fun isNotificationListenerGranted(context: Context): Boolean {
    return NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)
}
