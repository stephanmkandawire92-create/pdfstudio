import re

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "r") as f:
    content = f.read()

# Fix the QuickActionItem signature
content = content.replace("        Spacer(Modifier.height(6.dp))\n        Text(text = label,", """@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        modifier = androidx.compose.ui.Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .size(48.dp)
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label,""")

# Fix the DocumentGridCard signature
content = content.replace(") {\n    var showMenu by remember { mutableStateOf(false) }\n    val formattedDate", """@Composable
fun DocumentGridCard(
    document: com.example.data.DocumentEntity,
    thumbnail: android.graphics.Bitmap?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCompress: () -> Unit,
    onBackupToCloud: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate""", 1)

# Fix the DocumentListCard signature
content = content.replace(") {\n    var showMenu by remember { mutableStateOf(false) }\n    \n    val (fileIcon", """@Composable
fun DocumentListCard(
    document: com.example.data.DocumentEntity,
    thumbnail: android.graphics.Bitmap?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCompress: () -> Unit,
    onBackupToCloud: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val (fileIcon""", 1)

# Fix CloudStatusIndicator exhaustiveness
content = content.replace("is com.example.engine.SyncState.Error -> Triple(\"Sync Error\", androidx.compose.material3.MaterialTheme.colorScheme.error, androidx.compose.material.icons.Icons.Default.CloudOff)\n    }", "is com.example.engine.SyncState.Error -> Triple(\"Sync Error\", androidx.compose.material3.MaterialTheme.colorScheme.error, androidx.compose.material.icons.Icons.Default.CloudOff)\n        else -> Triple(\"Ready\", androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, androidx.compose.material.icons.Icons.Default.CloudDone)\n    }")

with open("app/src/main/java/com/example/ui/HomeScreen.kt", "w") as f:
    f.write(content)
