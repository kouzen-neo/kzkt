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
        contract = ActivityResultContracts.GetMultipleContents()
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
                // Pick file
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("image/*") },
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

            // ── Result preview ──
            viewModel.currentPreviewPath.value?.let { previewPath ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                        // Show last rendered result as a thumbnail note
                        Text(
                            "Output: ${previewPath.substringAfterLast('/')}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}
