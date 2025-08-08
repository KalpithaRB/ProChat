package com.kalpi.prochat.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.IOException

/**
 * Retrieves the display name of the file from a given content URI.
 */
fun getFileNameFromUri(context: Context, uri: Uri): String? {
    var fileName: String? = null
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                fileName = it.getString(nameIndex)
            }
        }
    }
    return fileName
}

/**
 * Retrieves the MIME type of the file from a given content URI.
 */
fun getFileTypeFromUri(context: Context, uri: Uri): String? {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri)
    if (mimeType.isNullOrBlank()) {
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
    return mimeType
}

/**
 * Retrieves the size of the file in bytes from a given content URI.
 */
fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    val contentResolver = context.contentResolver
    try {
        contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            return pfd.statSize
        }
    } catch (e: SecurityException) {
        // Fallback to cursor if FileDescriptor access is restricted
        var cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !it.isNull(sizeIndex)) {
                    try {
                        return it.getLong(sizeIndex)
                    } catch (e: Exception) {
                        Log.e("GetFileSize", "Error getting size from OpenableColumns: ${e.message}")
                    }
                }
            }
        }
        Log.e("GetFileSize", "Could not get file size for URI: $uri. SecurityException or OpenableColumns failed.", e)
    } catch (e: IOException) {
        Log.e("GetFileSize", "Could not get file size for URI: $uri. IOException.", e)
    } catch (e: Exception) {
        Log.e("GetFileSize", "An unexpected error occurred while getting file size for URI: $uri.", e)
    }
    return -1L // Indicates failure
}