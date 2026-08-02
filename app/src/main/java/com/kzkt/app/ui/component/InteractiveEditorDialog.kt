package com.kzkt.app.ui.component

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kzkt.app.core.TextRenderer

/**
 * Interactive Touch-up Editor: tap any bubble on the preview image to tweak its translated text.
 */
@Composable
fun InteractiveEditorDialog(
    originalBitmap: Bitmap,
    translations: Map<String, String>,
    coordinateMap: Map<String, IntArray>,
    textRenderer: TextRenderer,
    targetLanguage: String,
    onDismiss: () -> Unit,
    onSave: (Bitmap, Map<String, String>) -> Unit,
) {
    var currentTranslations by remember { mutableStateOf(translations.toMutableMap()) }
    var selectedBubbleId by remember { mutableStateOf<String?>(null) }
    var editingText by remember { mutableStateOf("") }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Re-render bitmap on translation edit
    val editedBitmap by remember(currentTranslations) {
        derivedStateOf {
            val resultBmp = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBmp)
            for ((id, text) in currentTranslations) {
                val box = coordinateMap[id] ?: continue
                if (text.isBlank() || text.uppercase() == "SKIP") continue
                textRenderer.renderTextInBubble(
                    canvas = canvas,
                    bubbleRect = box,
                    text = text,
                    backgroundPatch = true,
                    targetLanguage = targetLanguage
                )
            }
            resultBmp
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Interactive Touch-up Editor",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Tap any bubble on the page to edit its translated text live.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Interactive Image View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { containerSize = it.size }
                        .pointerInput(containerSize, coordinateMap) {
                            detectTapGestures { offset ->
                                if (containerSize.width == 0 || containerSize.height == 0) return@detectTapGestures
                                val scaleX = originalBitmap.width.toFloat() / containerSize.width
                                val scaleY = originalBitmap.height.toFloat() / containerSize.height
                                val imgX = (offset.x * scaleX).toInt()
                                val imgY = (offset.y * scaleY).toInt()

                                // Find tapped bubble
                                for ((id, box) in coordinateMap) {
                                    val (x1, y1, x2, y2) = box
                                    if (imgX in x1..x2 && imgY in y1..y2) {
                                        selectedBubbleId = id
                                        editingText = currentTranslations[id] ?: ""
                                        break
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = editedBitmap.asImageBitmap(),
                        contentDescription = "Preview Page",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Edit section if bubble selected
                selectedBubbleId?.let { id ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Editing Bubble #$id", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = editingText,
                                onValueChange = {
                                    editingText = it
                                    val updated = currentTranslations.toMutableMap()
                                    updated[id] = it
                                    currentTranslations = updated
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                singleLine = false,
                                maxLines = 3
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(editedBitmap, currentTranslations) }) { Text("Save & Apply") }
                }
            }
        }
    }
}
