package com.cypy.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = viewModel.settings.value
    val logList = viewModel.translationLog
    val logListState = rememberLazyListState()
    val yoloReady = viewModel.yoloReady.value
    val yoloError = viewModel.yoloError.value

    // Auto-scroll log
    LaunchedEffect(logList.size) {
        if (logList.isNotEmpty()) {
            logListState.animateScrollToItem(logList.size - 1)
        }
    }

    // Initialize YOLO on first composition
    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val paths = uris.mapNotNull { FileUtils.getPathFromUri(context, it) }
            if (paths.isNotEmpty()) {
                viewModel.addFiles(paths)
            } else {
                // Fallback: copy to cache
                val copied = uris.mapNotNull { FileUtils.copyUriToCache(context, it) }
                viewModel.addFiles(copied)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cypy Translator") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // ── Status ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (yoloReady) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("YOLO ready", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                } else if (yoloError != null) {
                    Icon(Icons.Default.Error, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(yoloError, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Loading YOLO...", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Quick settings row ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Provider
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "LLM: ${settings.llmProvider.uppercase().replaceFirstChar { it }}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                // Language
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "→ ${settings.targetLanguage}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Selected files ──
            if (viewModel.selectedFiles.isNotEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "${viewModel.selectedFiles.size} file(s) selected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        viewModel.selectedFiles.forEach { path ->
                            Text(
                                path.substringAfterLast('/'),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Action buttons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Pick file (images + PDF)
                OutlinedButton(
                    onClick = { filePickerLauncher.launch(arrayOf("image/*", "application/pdf")) },
                    enabled = !viewModel.translationActive.value,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Pick Image")
                }

                // Start / Cancel
                if (viewModel.translationActive.value) {
                    Button(
                        onClick = { viewModel.cancelTranslation() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = { viewModel.startTranslation() },
                        enabled = viewModel.selectedFiles.isNotEmpty() && yoloReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Translate")
                    }
                }
            }

            // ── Progress bar ──
            if (viewModel.translationActive.value) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { viewModel.translationProgress.value },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "${viewModel.translationDone.value} / ${viewModel.translationTotal.value}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── Log output ──
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                if (logList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            "Select an image and press Translate to start.\n\nSettings gear icon → configure providers, API keys, and tweak params.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        state = logListState,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(logList.toList()) { msg ->
                            val isError = msg.contains("[!]") || msg.contains("[Cancelled]")
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isError) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // ── Result Preview & Action Bar ──
            val previewPath = viewModel.currentPreviewPath.value
            val resultList = viewModel.resultPaths

            if (previewPath != null || resultList.isNotEmpty()) {
                var showFullscreenViewer by remember { mutableStateOf(false) }
                val currentPath = previewPath ?: resultList.last()

                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Hasil Terjemahan (${resultList.size} Halaman)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                currentPath.substringAfterLast('/'),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // In-App Fullscreen Viewer
                            Button(
                                onClick = { showFullscreenViewer = true },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Lihat di App", fontSize = 12.sp)
                            }

                            // Open in System Gallery
                            OutlinedButton(
                                onClick = { FileUtils.openFileInSystemViewer(context, currentPath) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Galeri HP", fontSize = 12.sp)
                            }

                            // Share
                            IconButton(
                                onClick = { FileUtils.shareFile(context, currentPath) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Fullscreen In-App Image Viewer Dialog
                if (showFullscreenViewer) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showFullscreenViewer = false },
                        properties = androidx.compose.ui.window.DialogProperties(
                            usePlatformDefaultWidth = false
                        )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = androidx.compose.ui.graphics.Color.Black
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (currentPath.endsWith(".pdf", ignoreCase = true)) {
                                    // PDF: no image to zoom — show a document card instead
                                    Column(
                                        modifier = Modifier.align(Alignment.Center),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.PictureAsPdf,
                                            contentDescription = "PDF",
                                            tint = androidx.compose.ui.graphics.Color(0xFFE53935),
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            currentPath.substringAfterLast('/'),
                                            color = androidx.compose.ui.graphics.Color.White,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Button(
                                                onClick = { FileUtils.openFileInSystemViewer(context, currentPath) }
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Buka PDF")
                                            }
                                            OutlinedButton(
                                                onClick = { FileUtils.shareFile(context, currentPath) },
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = androidx.compose.ui.graphics.Color.White
                                                )
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Bagikan")
                                            }
                                        }
                                    }
                                } else {
                                    var scale by remember { mutableStateOf(1f) }
                                    var offsetX by remember { mutableStateOf(0f) }
                                    var offsetY by remember { mutableStateOf(0f) }

                                    val state = androidx.compose.foundation.gestures.rememberTransformableState { zoomChange, panChange, _ ->
                                        scale = (scale * zoomChange).coerceIn(0.8f, 5f)
                                        offsetX += panChange.x
                                        offsetY += panChange.y
                                    }

                                    coil.compose.AsyncImage(
                                        model = java.io.File(currentPath),
                                        contentDescription = "Translated Manga Page",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            )
                                            .transformable(state = state)
                                    )
                                }


                                // Top bar with close and share button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .align(Alignment.TopCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showFullscreenViewer = false },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.White)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { FileUtils.shareFile(context, currentPath) },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = androidx.compose.ui.graphics.Color.White)
                                        }

                                        IconButton(
                                            onClick = { FileUtils.openFileInSystemViewer(context, currentPath) },
                                            colors = IconButtonDefaults.iconButtonColors(
                                                containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f)
                                            )
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = "Open in Gallery", tint = androidx.compose.ui.graphics.Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

