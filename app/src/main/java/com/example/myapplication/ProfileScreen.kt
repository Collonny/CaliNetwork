package com.example.myapplication

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    var profileData by remember { mutableStateOf<UserProfileData?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    LaunchedEffect(userId) {
        if (userId != null) {
            coroutineScope.launch {
                profileData = FirestoreService.getUserProfileData(userId)
                Log.d("ProfileScreen", "URL slike iz baze: ${profileData?.user?.photoUrl}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (profileData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    val imageUrl = profileData!!.user.photoUrl.replace("http://", "https://")
                    val painter = rememberAsyncImagePainter(
                        model = imageUrl,
                        error = rememberAsyncImagePainter(R.drawable.ic_launcher_foreground),
                        placeholder = rememberAsyncImagePainter(R.drawable.ic_launcher_foreground)
                    )
                    Image(
                        painter = painter,
                        contentDescription = "Profilna slika",
                        modifier = Modifier.size(120.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(profileData!!.user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(profileData!!.user.email, style = MaterialTheme.typography.bodyMedium)
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                }

                item {
                    Text("Moji Parkovi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (profileData!!.createdParks.isEmpty()) {
                    item { Text("Niste kreirali nijedan park.") }
                } else {
                    items(profileData!!.createdParks) {
                        Text(it.name)
                    }
                }

                item { Divider(modifier = Modifier.padding(vertical = 16.dp)) }

                item {
                    Text("Moji Rekordi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (profileData!!.userRecords.isEmpty()) {
                    item { Text("Nema unetih rekorda.") }
                } else {
                    items(profileData!!.userRecords) {
                        Text("${it.type.replaceFirstChar { c -> c.uppercase() }}: ${it.score}")
                    }
                }
            }
        }
    }
}
