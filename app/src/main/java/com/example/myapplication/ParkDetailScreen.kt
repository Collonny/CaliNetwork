package com.example.myapplication

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkDetailScreen(
    park: WorkoutPark,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = park.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val parkLocation = LatLng(park.latituda, park.longituda)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(parkLocation, 15f)
                }
                GoogleMap(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    Marker(state = MarkerState(position = parkLocation))
                }
            }

            item {
                Text(park.opis, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text(String.format("Prosečna ocena: %.1f (od %d glasova)", park.rating.average, park.rating.brRatings), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                if (currentUserId != null) {
                    Text("Vaša ocena:")
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            val currentUserRating = (park.rating.userRatings[currentUserId] as? Number)?.toLong()
                            val isSelected = currentUserRating == star.toLong()
                            Button(
                                onClick = { 
                                    FirestoreService.ratePark(park.id, currentUserId, star, 
                                        onSuccess = { Toast.makeText(context, "Hvala na oceni!", Toast.LENGTH_SHORT).show() },
                                        onError = { Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show() }
                                    )
                                },
                                colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                            ) {
                                Text(star.toString())
                            }
                        }
                    }
                }
            }

            item {
                Text("Izazovi i Rekordi", style = MaterialTheme.typography.titleLarge)
            }

            if (park.challenges.isEmpty()) {
                item {
                    Text("Nema postavljenih izazova za ovaj park.")
                }
            } else {
                items(park.challenges.entries.toList()) { (type, challenge) ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(type.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Rekord: ${challenge.bestScore}")
                            Text("Postavio: ${challenge.bestUser}")
                        }
                    }
                }
            }
        }
    }
}
