package com.example.myapplication

import android.net.Uri
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
// import com.example.myapplication.services.CloudinaryService // Pretpostavljam da je ovo putanja

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
            val inputStream = context.contentResolver.openInputStream(uri)
            photoBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
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

        Button(onClick = { launcher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
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
                            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            // Privremeno rešenje bez Cloudinary-ja
                            saveUserToFirestore(
                                userId = userId,
                                fullName = fullName,
                                email = email,
                                phoneNumber = phoneNumber,
                                photoUrl = "", // Privremeno prazno
                                onSuccess = {
                                    isLoading = false
                                    onRegisterSuccess()
                                },
                                onFailure = {
                                    errorMessage = it.message
                                    isLoading = false
                                }
                            )
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
