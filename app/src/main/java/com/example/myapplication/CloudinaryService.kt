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
            // 🔹 VAŽNO: Unesite vaše stvarne podatke ovde
            val config = mapOf(
                "cloud_name" to "dzh1lsvcm", // 🔹 ZAMENITE OVO
                "api_key" to "749362658272874",     // 🔹 ZAMENITE OVO
                "api_secret" to "ln6_wH9Ymv8BOS3rXgp-ueb-Pdo" // 🔹 ZAMENITE OVO
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

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    Log.d(TAG, "Upload progress for $requestId: $bytes/$totalBytes")
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val url = resultData?.get("url") as? String ?: ""
                    Log.d(TAG, "Upload successful for $requestId. URL: $url")
                    onResult(url)
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e(TAG, "Upload failed for $requestId. Error: ${error?.description}")
                    onResult("") // Vraćamo prazan string u slučaju greške
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.w(TAG, "Upload rescheduled for $requestId. Error: ${error?.description}")
                }
            })
            .dispatch()
    }
}
