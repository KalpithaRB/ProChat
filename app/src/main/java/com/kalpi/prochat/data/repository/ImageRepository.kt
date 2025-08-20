package com.kalpi.prochat.data.repository

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import com.cloudinary.android.callback.UploadCallback
import java.util.UUID
import kotlin.coroutines.resume

interface ImageRepository {
    suspend fun uploadImage(fileUri: Uri): Result<String>
}

class RealImageRepository : ImageRepository {

    companion object {
        private const val TAG = "ImageRepository"
        // Use your actual Cloudinary upload preset name here
        private const val UPLOAD_PRESET = "prochat_unsigned_images"
    }

    override suspend fun uploadImage(fileUri: Uri): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Starting Cloudinary upload for URI: $fileUri")

            MediaManager.get().upload(fileUri)
                .unsigned(UPLOAD_PRESET)
                .option("public_id", "group_avatars/${UUID.randomUUID()}")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.d(TAG, "Cloudinary upload started. Request ID: $requestId")
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        val progress = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                        Log.d(TAG, "Cloudinary upload progress: $progress%. Request ID: $requestId")
                        // In this repository layer, we don't have a direct way to communicate progress back to the UI,
                        // but logging it is good for debugging.
                    }

                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        val url = resultData?.get("secure_url") as? String
                        if (url != null) {
                            Log.d(TAG, "Cloudinary upload success. URL: $url")
                            if (continuation.isActive) {
                                continuation.resume(Result.success(url))
                            }
                        } else {
                            Log.e(TAG, "Cloudinary upload success but URL is null. ResultData: $resultData")
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(Exception("Cloudinary upload succeeded but URL was null.")))
                            }
                        }
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e(TAG, "Cloudinary upload error. Desc: ${error?.description}")
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Cloudinary upload failed: ${error?.description}")))
                        }
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.w(TAG, "Cloudinary upload rescheduled. Desc: ${error?.description}")
                        // For a simple implementation, we can just fail on reschedule
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("Cloudinary upload rescheduled, considered an error.")))
                        }
                    }
                })
                .dispatch()

            continuation.invokeOnCancellation {
                Log.d(TAG, "Cloudinary upload coroutine cancelled for URI: $fileUri.")
            }
        }
    }
}