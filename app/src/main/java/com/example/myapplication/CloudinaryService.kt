package com.example.myapplication.services

import android.net.Uri
import android.os.AsyncTask
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils

object CloudinaryService {
    private val cloudinary = Cloudinary(
        ObjectUtils.asMap(
            "cloud_name", "dzh1lsvcm",
            "api_key", "749362658272874",
            "api_secret", "ln6_wH9Ymv8BOS3rXgp-ueb-Pdo"
        )
    )

    fun uploadImage(uri: Uri, onResult: (String) -> Unit) {
        AsyncTask.execute {
            val filePath = uri.path ?: ""
            val uploadResult = cloudinary.uploader().upload(filePath, ObjectUtils.emptyMap())
            val url = uploadResult["secure_url"] as? String ?: ""
            onResult(url)
        }
    }
}
