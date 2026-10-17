package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.example.data.model.AppConfig

@Composable
fun UpdateDialog(config: AppConfig, isForce: Boolean, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = { if (!isForce) onDismiss() },
        title = { Text(if (isForce) "Mandatory Update Required" else "Update Available (${config.latestVersionName})") },
        text = {
            Text(if (isForce) (config.forceUpdateMessage ?: "Please update the app to continue.") else (config.changelog ?: "A new version is available."))
        },
        confirmButton = {
            Button(onClick = {
                if (!config.apkUrl.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(config.apkUrl))
                    context.startActivity(intent)
                }
            }) {
                Text("Update Now")
            }
        },
        dismissButton = {
            if (!isForce) {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        }
    )
}
