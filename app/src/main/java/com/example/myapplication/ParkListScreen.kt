package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkListScreen(
    onParkSelected: (park: WorkoutPark) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var parkList by remember { mutableStateOf(listOf<WorkoutPark>()) }

    // Učitavanje parkova iz Firestore-a
    DisposableEffect(Unit) {
        val listener = FirestoreService.getWorkoutParks(
            onResult = { parks -> parkList = parks },
            onError = { error ->
                Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
        onDispose { listener.remove() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Parkova") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(parkList) { park ->
                ListItem(
                    headlineContent = { Text(park.name) },
                    supportingContent = { Text(park.opis) },
                    modifier = Modifier.clickable { onParkSelected(park) }
                )
            }
        }
    }
}
