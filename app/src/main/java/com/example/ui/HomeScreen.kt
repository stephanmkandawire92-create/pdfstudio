package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.DocumentEntity
import com.example.engine.PdfEngine
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.PdfRed
import com.example.ui.theme.PdfRedDark
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getFileTypeData(title: String, filePath: String): Pair<ImageVector, Color> {
    val extension = filePath.substringAfterLast('.', "").lowercase()
    val nameExt = title.substringAfterLast('.', "").lowercase()
    val ext = if (extension.isNotEmpty()) extension else nameExt
    
    return when (ext) {
        "pdf" -> Pair(Icons.Default.PictureAsPdf, PdfRed)
        "jpg", "jpeg", "png", "gif", "webp" -> Pair(Icons.Default.Image, Color(0xFF4CAF50)) // Green
        "doc", "docx" -> Pair(Icons.Default.Description, Color(0xFF2196F3)) // Blue
        "xls", "xlsx", "csv" -> Pair(Icons.Default.TableChart, Color(0xFF009688)) // Teal
        else -> Pair(Icons.Default.InsertDriveFile, Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PdfAppViewModel,
    onOpenDocument: (DocumentEntity) -> Unit,
    onNavigateScanner: () -> Unit,
    onNavigateBatchStudio: () -> Unit,
    onNavigateTools: () -> Unit = {}
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val isDarkMode by viewModel.isAppDarkMode.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedDocForUnlock by remember { mutableStateOf<DocumentEntity?>(null) }
    var selectedDocForCompression by remember { mutableStateOf<DocumentEntity?>(null) }
    var docToRename by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showCloudSyncDialog by remember { mutableStateOf(false) }

    // PDF Import Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Imported_${System.currentTimeMillis() % 10000}.pdf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.importPdf(uri, fileName)
        }
    }

    // Thumbnail cache
    val thumbnailMap = remember { mutableStateMapOf<Long, android.graphics.Bitmap>() }

    LaunchedEffect(documents) {
        for (doc in documents) {
            if (!thumbnailMap.containsKey(doc.id)) {
                val f = File(doc.filePath)
                val bmp = PdfEngine.renderPage(f, 0, renderScale = 0.4f)
                if (bmp != null) {
                    thumbnailMap[doc.id] = bmp
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search files, tags, content...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("home_search_input"),
                            trailingIcon = {
                                IconButton(onClick = {
                                    viewModel.setSearchQuery("")
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(PdfRed, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("PDF Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Pro Document Suite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = { showCloudSyncDialog = true }, modifier = Modifier.testTag("cloud_sync_button")) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync & Backup",
                            tint = PdfRed
                        )
                    }
                    IconButton(onClick = { viewModel.toggleGridView() }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateScanner,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("Scan Document", fontWeight = FontWeight.Bold) },
                containerColor = PdfRed,
                contentColor = Color.White,
                modifier = Modifier.testTag("scan_fab_button")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Quick Actions Hub Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(listOf(PdfRedDark, PdfRed)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Smart Document Hub",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${documents.size} Documents • Ready to read & edit",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // 4 Quick Action Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickActionItem(
                                icon = Icons.Default.CameraAlt,
                                label = "Scan Doc",
                                onClick = onNavigateScanner
                            )
                            QuickActionItem(
                                icon = Icons.Default.FolderOpen,
                                label = "Import PDF",
                                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                            )
                            QuickActionItem(
                                icon = Icons.Default.AutoAwesomeMotion,
                                label = "Batch Studio",
                                onClick = onNavigateBatchStudio
                            )
                            QuickActionItem(
                                icon = Icons.Default.BuildCircle,
                                label = "PDF Tools",
                                onClick = onNavigateTools
                            )
                        }
                    }
                }
            }

            // Category / Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                divider = {}
            ) {
                val tabs = listOf(
                    HomeTab.ALL to "All Files",
                    HomeTab.RECENTS to "Recents",
                    HomeTab.STARRED to "Starred",
                    HomeTab.SCANS to "Scans",
                    HomeTab.VAULT to "Vault 🔐"
                )
                tabs.forEachIndexed { index, (tab, title) ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == tab) PdfRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Document List / Grid
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching documents found" else "No documents in this tab",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Scan Document' or 'Import PDF' to add documents.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentGridCard(
                            document = doc,
                            thumbnail = thumbnailMap[doc.id],
                            onOpen = {
                                if (doc.isEncrypted) {
                                    selectedDocForUnlock = doc
                                } else {
                                    onOpenDocument(doc)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onDelete = { viewModel.deleteDocument(doc) },
                            onRename = {
                                docToRename = doc
                                renameInput = doc.title
                            },
                            onShare = { shareDocument(context, doc) },
                            onCompress = { selectedDocForCompression = doc },
                            onBackupToCloud = {
                                viewModel.backupSingleDocumentToCloud(doc) { success ->
                                    val msg = if (success) "Backed up '${doc.title}' to Cloud" else "Cloud backup failed (check sign-in)"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentListCard(
                            document = doc,
                            thumbnail = thumbnailMap[doc.id],
                            onOpen = {
                                if (doc.isEncrypted) {
                                    selectedDocForUnlock = doc
                                } else {
                                    onOpenDocument(doc)
                                }
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onDelete = { viewModel.deleteDocument(doc) },
                            onRename = {
                                docToRename = doc
                                renameInput = doc.title
                            },
                            onShare = { shareDocument(context, doc) },
                            onCompress = { selectedDocForCompression = doc },
                            onBackupToCloud = {
                                viewModel.backupSingleDocumentToCloud(doc) { success ->
                                    val msg = if (success) "Backed up '${doc.title}' to Cloud" else "Cloud backup failed (check sign-in)"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Cloud Sync & Backup Dialog
    if (showCloudSyncDialog) {
        CloudSyncDialog(
            viewModel = viewModel,
            onDismiss = { showCloudSyncDialog = false }
        )
    }

    // PDF Compression Dialog
    selectedDocForCompression?.let { doc ->
        PdfCompressionDialog(
            document = doc,
            viewModel = viewModel,
            onDismiss = { selectedDocForCompression = null },
            onOpenDocument = { compressedDoc ->
                selectedDocForCompression = null
                onOpenDocument(compressedDoc)
            }
        )
    }

    // Unlock Password Dialog
    selectedDocForUnlock?.let { doc ->
        UnlockPasswordPromptDialog(
            document = doc,
            onDismiss = { selectedDocForUnlock = null },
            onVerify = { pass -> viewModel.verifyPassword(doc, pass) },
            onSuccess = {
                val toOpen = selectedDocForUnlock
                selectedDocForUnlock = null
                if (toOpen != null) {
                    onOpenDocument(toOpen)
                }
            }
        )
    }

    // Rename Dialog
    docToRename?.let { doc ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { docToRename = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Rename Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        androidx.compose.material3.TextButton(onClick = { docToRename = null }) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                if (renameInput.isNotBlank()) {
                                    viewModel.renameDocument(doc, renameInput.trim())
                                }
                                docToRename = null
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = PdfRed)
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DocumentListCard(
    document: DocumentEntity,
    thumbnail: android.graphics.Bitmap?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit = {},
    onBackupToCloud: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(document.lastOpenedTimestamp))
    val sizeKb = document.fileSize / 1024
    
    val (fileIcon, fileColor) = getFileTypeData(document.title, document.filePath)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("document_item_${document.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(50.dp, 64.dp)
                    .background(Color.White, RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null && !thumbnail.isRecycled) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp))
                    )
                } else {
                    Icon(
                        fileIcon,
                        contentDescription = null,
                        tint = fileColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (document.isEncrypted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = document.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${document.pageCount} Pages • $sizeKb KB • $formattedDate",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (document.tags.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "🏷️ ${document.tags}",
                        fontSize = 10.sp,
                        color = PdfRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (document.isFavorite) PdfRed else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("📖 Open Document") },
                        onClick = { showMenu = false; onOpen() },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("🗜️ Compress PDF") },
                        onClick = { showMenu = false; onCompress() },
                        leadingIcon = { Icon(Icons.Default.Compress, contentDescription = null, tint = AccentEmerald) }
                    )
                    DropdownMenuItem(
                        text = { Text("☁️ Backup to Cloud") },
                        onClick = { showMenu = false; onBackupToCloud() },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PdfRed) }
                    )
                    DropdownMenuItem(
                        text = { Text("✏️ Rename") },
                        onClick = { showMenu = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("📤 Share File") },
                        onClick = { showMenu = false; onShare() },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("🗑️ Delete") },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentGridCard(
    document: DocumentEntity,
    thumbnail: android.graphics.Bitmap?,
    onOpen: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onCompress: () -> Unit = {},
    onBackupToCloud: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    val (fileIcon, fileColor) = getFileTypeData(document.title, document.filePath)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .testTag("document_grid_item_${document.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Thumbnail preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnail != null && !thumbnail.isRecycled) {
                    Image(
                        bitmap = thumbnail.asImageBitmap(),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        fileIcon,
                        contentDescription = null,
                        tint = fileColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                if (document.isEncrypted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = document.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${document.pageCount} Pages • ${(document.fileSize / 1024)} KB",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (document.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (document.isFavorite) PdfRed else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("📖 Open") },
                            onClick = { showMenu = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text("🗜️ Compress") },
                            onClick = { showMenu = false; onCompress() }
                        )
                        DropdownMenuItem(
                            text = { Text("☁️ Backup to Cloud") },
                            onClick = { showMenu = false; onBackupToCloud() }
                        )
                        DropdownMenuItem(
                            text = { Text("✏️ Rename") },
                            onClick = { showMenu = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("📤 Share") },
                            onClick = { showMenu = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text("🗑️ Delete") },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

private fun shareDocument(context: Context, doc: DocumentEntity) {
    try {
        val file = File(doc.filePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share ${doc.title}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
