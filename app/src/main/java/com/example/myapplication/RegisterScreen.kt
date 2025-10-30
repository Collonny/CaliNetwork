package com.example.myapplication

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                photoUri = uri
                context.contentResolver.openInputStream(uri)?.use { photoBitmap = BitmapFactory.decodeStream(it) }
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let {
                    photoUri = it
                    context.contentResolver.openInputStream(it)?.use { photoBitmap = BitmapFactory.decodeStream(it) }
                }
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registracija", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Ime i prezime") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = phoneNumber, onValueChange = { phoneNumber = it }, label = { Text("Broj telefona") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Lozinka") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Potvrdi lozinku") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))

        photoBitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = "Izabrana fotografija", modifier = Modifier.size(120.dp).clip(CircleShape))
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = { showImageSourceDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Izaberi fotografiju")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMessage = "Molimo popunite sva obavezna polja"
                    return@Button
                }
                if (password != confirmPassword) {
                    errorMessage = "Lozinke se ne poklapaju"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { authTask ->
                        if (authTask.isSuccessful) {
                            val user = authTask.result?.user
                            val userId = user?.uid ?: ""

                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName)
                                .build()

                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    if (photoUri != null) {
                                        CloudinaryService.uploadImage(photoUri!!) { imageUrl ->
                                            saveUserToFirestore(
                                                userId = userId, fullName = fullName, email = email, phoneNumber = phoneNumber, photoUrl = imageUrl,
                                                onSuccess = { isLoading = false; onRegisterSuccess() },
                                                onFailure = { e -> 
                                                    errorMessage = e.message
                                                    isLoading = false
                                                }
                                            )
                                        }
                                    } else {
                                        saveUserToFirestore(
                                            userId = userId, fullName = fullName, email = email, phoneNumber = phoneNumber, photoUrl = "",
                                            onSuccess = { isLoading = false; onRegisterSuccess() },
                                            onFailure = { e -> 
                                                errorMessage = e.message
                                                isLoading = false
                                            }
                                        )
                                    }
                                } else {
                                    errorMessage = updateTask.exception?.message
                                    isLoading = false
                                }
                            }
                        } else {
                            errorMessage = authTask.exception?.message
                            isLoading = false
                        }
                    }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registruj se")
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onCancel) { Text("Nazad na prijavu") }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Izvor slike") },
            text = { Text("Izaberite odakle želite da dodate sliku.") },
            confirmButton = {
                Button(onClick = {
                    showImageSourceDialog = false
                    if (cameraPermissionState.status.isGranted) {
                        tempImageUri = createImageUri(context)
                        cameraLauncher.launch(tempImageUri)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }) {
                    Text("Kamera")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galerija")
                }
            }
        )
    }
}

private fun createImageUri(context: Context): Uri {
    val imageFile = File.createTempFile("JPEG_${System.currentTimeMillis()}_", ".jpg", context.externalCacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

private fun saveUserToFirestore(
    userId: String,
    fullName: String,
    email: String,
    phoneNumber: String,
    photoUrl: String,
    onSuccess: () -> Unit,
    onFailure: (Exception) -> Unit
) {
    val userData = hashMapOf(
        "Ime" to fullName,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "photoUrl" to photoUrl,
        "points" to 0,
        "createdAt" to Timestamp.now()
    )

    FirebaseFirestore.getInstance().collection("users").document(userId)
        .set(userData)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { onFailure(it) }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen({}, {})
}
