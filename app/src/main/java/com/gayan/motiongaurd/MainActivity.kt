package com.gayan.motiongaurd

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private var serviceRunning = mutableStateOf(MotionGuardService.isRunning)
    private var overlayGranted = mutableStateOf(false)
    private var isBatteryOptimized = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            MaterialTheme {
                ControlPanel(
                    serviceRunning = serviceRunning.value,
                    overlayGranted = overlayGranted.value,
                    isBatteryOptimized = isBatteryOptimized.value,
                    onStart = { startService() },
                    onStop = { stopService() },
                    onGrantOverlay = { requestOverlayPermission() },
                    onRequestBatteryExemption = { requestBatteryOptimizationExemption() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
        // Keep UI synced with background service
        serviceRunning.value = MotionGuardService.isRunning
    }

    private fun checkPermissions() {
        overlayGranted.value = Settings.canDrawOverlays(this)
        
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        isBatteryOptimized.value = !pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun startService() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return
        }
        MotionGuardService.start(this)
        serviceRunning.value = true
    }

    private fun stopService() {
        MotionGuardService.stop(this)
        serviceRunning.value = false
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        ))
    }

    private fun requestBatteryOptimizationExemption() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

@Composable
fun ControlPanel(
    serviceRunning: Boolean,
    overlayGranted: Boolean,
    isBatteryOptimized: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onGrantOverlay: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MotionGuard", fontSize = 32.sp,
                style = MaterialTheme.typography.headlineLarge)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Motion sickness protection",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Overlay permission warning
            if (!overlayGranted) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Overlay Permission Required",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Required for edge dimming to work over other apps",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onGrantOverlay) {
                            Text("Grant Permission")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Battery Optimization warning
            if (isBatteryOptimized) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Battery Optimization Enabled",
                            style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Your phone may kill the sensors in the background. Disable optimizations for reliable protection.",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestBatteryExemption) {
                            Text("Disable Optimization")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Big start/stop button
            Button(
                onClick = if (serviceRunning) onStop else onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serviceRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (serviceRunning) "Stop Protection" else "Start Protection",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Status indicator
            Text(
                text = if (serviceRunning)
                    "✓ Running in background"
                else
                    "Not active",
                color = if (serviceRunning)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You can close this app — protection continues",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}