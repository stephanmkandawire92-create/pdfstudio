@Composable
fun CloudStatusIndicator(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    syncState: com.example.engine.SyncState,
    lastBackupTime: Long?,
    isAutoBackupEnabled: Boolean
) {
    if (currentUser == null) return

    val timeString = androidx.compose.runtime.remember(lastBackupTime) {
        lastBackupTime?.let {
            java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: "Never"
    }

    val statusData = when (syncState) {
        is com.example.engine.SyncState.Idle -> Triple("Ready", androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, androidx.compose.material.icons.Icons.Default.CloudDone)
        is com.example.engine.SyncState.Syncing -> Triple("Syncing...", androidx.compose.material3.MaterialTheme.colorScheme.primary, androidx.compose.material.icons.Icons.Default.CloudSync)
        is com.example.engine.SyncState.Success -> Triple("Last synced: $timeString", androidx.compose.material3.MaterialTheme.colorScheme.primary, androidx.compose.material.icons.Icons.Default.CloudDone)
        is com.example.engine.SyncState.Error -> Triple("Sync Error", androidx.compose.material3.MaterialTheme.colorScheme.error, androidx.compose.material.icons.Icons.Default.CloudOff)
    }

    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .androidx.compose.foundation.layout.fillMaxWidth()
            .androidx.compose.foundation.layout.padding(horizontal = 24.dp, vertical = 2.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                imageVector = statusData.third,
                contentDescription = "Cloud Status",
                tint = statusData.second,
                modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.size(16.dp)
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.width(6.dp))
            androidx.compose.foundation.layout.Column {
                androidx.compose.material3.Text(
                    text = "PDFStudio Cloud Active",
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.material3.Text(
                    text = statusData.first + if (isAutoBackupEnabled) " • Auto-backup ON" else "",
                    fontSize = 11.sp,
                    color = statusData.second
                )
            }
        }
    }
}
