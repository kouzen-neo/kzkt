package com.kzkt.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

/**
 * File I/O utilities using Android ContentResolver (Scoped Storage safe).
 */
object FileUtils {

    /**
     * Resolve a content URI to a file path on the filesystem.
     * Works for most file providers and MediaStore URIs.
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
        // Try direct path
        if (uri.scheme == "file") return uri.path

        // Try content provider
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, "_data")
        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    // Try _data column first
                    val dataIdx = cursor.getColumnIndex("_data")
                    if (dataIdx >= 0) {
                        val path = cursor.getString(dataIdx)
                        if (path != null) return path
                    }
                    // Fallback: use display name
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (name != null) {
                            return copyUriToCache(context, uri, name)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // Last resort: copy to cache with a generated name
        return copyUriToCache(context, uri)
    }

    /**
     * Copy a content URI to app cache and return the file path.
     */
    fun copyUriToCache(context: Context, uri: Uri, customName: String? = null): String? {
        val name = customName ?: getFileName(context, uri) ?: "image_${System.currentTimeMillis()}.png"
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val cacheFile = File(context.cacheDir, "input/$name")
            cacheFile.parentFile?.mkdirs()
            FileOutputStream(cacheFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            cacheFile.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Open a translated image file using System Gallery / Media Viewer Intent.
     */
    fun openFileInSystemViewer(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mime = if (filePath.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/*"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("KZKT", "Failed to open file in system viewer: ${e.message}")
        }
    }

    /**
     * Share a translated image file to WhatsApp / Social Media.
     */
    fun shareFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share Translated Image"))
        } catch (e: Exception) {
            android.util.Log.e("KZKT", "Failed to share file: ${e.message}")
        }
    }

    /**
     * Copy translated file to public MediaStore Download/Pictures gallery.
     */
    fun saveToMediaStore(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val fileName = file.name
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true)

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, if (isPdf) "application/pdf" else "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOWNLOADS}/KZKT")
                }
            }

            val targetUri = if (isPdf || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            context.contentResolver.insert(targetUri, values)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("KZKT", "Failed to copy to MediaStore: ${e.message}")
        }
    }

    /**
     * Save bitmap directly to public MediaStore Download/KZKT directory.
     */
    fun saveBitmapToMediaStore(context: Context, bitmap: android.graphics.Bitmap, fileName: String, subDirName: String = "KZKT"): Uri? {
        return try {
            val isPdf = fileName.endsWith(".pdf", ignoreCase = true)
            val relPath = if (subDirName.isNotBlank() && subDirName != "KZKT") {
                "${android.os.Environment.DIRECTORY_DOWNLOADS}/KZKT/$subDirName"
            } else {
                "${android.os.Environment.DIRECTORY_DOWNLOADS}/KZKT"
            }

            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, if (isPdf) "application/pdf" else "image/png")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relPath)
                }
            }

            val targetUri = if (isPdf || android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = context.contentResolver.insert(targetUri, values) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }
            uri
        } catch (e: Exception) {
            android.util.Log.w("KZKT", "Failed to save bitmap to MediaStore: ${e.message}")
            null
        }
    }
}

