package com.kalpi.prochat.data


import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.IOException

fun getFileSizeFromUri(context: Context, uri: Uri): Long {
    val contentResolver = context.contentResolver
    try {
        contentResolver.openFileDescriptor(uri, "r").use { pfd ->
            return pfd?.statSize ?: -1L // Returns size in bytes, or -1 if failed
        }
    } catch (e: SecurityException) {
        // Fallback for some URIs (especially from external providers)
        // where direct FileDescriptor access might be restricted.
        // This is less reliable for exact size but can work for some cases.
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
    return -1L // Indicates failure to get size
}

