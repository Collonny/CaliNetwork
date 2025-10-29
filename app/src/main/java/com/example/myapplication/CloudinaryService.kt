package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

object CloudinaryService {

    private var isInitialized = false
    private const val TAG = "CloudinaryService"

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            val config = mapOf(
                "cloud_name" to "dzh1lsvcm",
                "api_key" to "435948132225333",
                "api_secret" to "2T6T5UuG_Gv94AH25a2n5Jz3C-s"
            )
            MediaManager.init(context, config)
            isInitialized = true
            Log.d(TAG, "Cloudinary initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Cloudinary: ${e.message}", e)
        }
    }

    fun uploadImage(uri: Uri, onResult: (String) -> Unit) {
        if (!isInitialized) {
            Log.e(TAG, "Cloudinary not initialized. Cannot upload image.")
            onResult("")
            return
        }

        MediaManager.get().upload(uri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d(TAG, "Upload started for requestId: $requestId")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("secure_url") as? String ?: ""
                    Log.d(TAG, "Upload successful for $requestId. Secure URL: $url")
                    onResult(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e(TAG, "Upload failed for $requestId. Error: ${error?.description}")
                    onResult("")
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.w(TAG, "Upload rescheduled for $requestId. Error: ${error?.description}")
                }
            })
            .dispatch()
    }
}
