package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng

private enum class SortOption {
    DISTANCE, RATING
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ParkListScreen(
    onParkSelected: (park: WorkoutPark) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var parkList by remember { mutableStateOf(listOf<WorkoutPark>()) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var sortOption by remember { mutableStateOf(SortOption.DISTANCE) }
    var showSortMenu by remember { mutableStateOf(false) }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    DisposableEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(Unit) {
        val listener = FirestoreService.getWorkoutParks(
            onResult = { parks -> parkList = parks },
            onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        onDispose { listener.remove() }
    }

    val sortedParks = remember(parkList, sortOption, userLocation) {
        when (sortOption) {
            SortOption.RATING -> parkList.sortedByDescending { it.rating.average }
            SortOption.DISTANCE -> {
                userLocation?.let { loc ->
                    parkList.sortedBy {
                        val parkLocation = Location("").apply { latitude = it.latituda; longitude = it.longituda }
                        val userAndroidLocation = Location("").apply { latitude = loc.latitude; longitude = loc.longitude }
                        userAndroidLocation.distanceTo(parkLocation)
                    }
                } ?: parkList // Ako lokacija nije dostupna, vrati originalnu listu
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista Parkova") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sortiraj")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Sortiraj po udaljenosti") }, onClick = { sortOption = SortOption.DISTANCE; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Sortiraj po oceni") }, onClick = { sortOption = SortOption.RATING; showSortMenu = false })
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(sortedParks) { park ->
                ListItem(
                    headlineContent = { Text(park.name) },
                    supportingContent = { Text(park.opis) },
                    modifier = Modifier.clickable { onParkSelected(park) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            if (sortOption == SortOption.RATING) {
                                Text(String.format("%.1f ★", park.rating.average), fontWeight = FontWeight.Bold)
                            } else {
                                userLocation?.let {
                                    val parkLocation = Location("").apply { latitude = park.latituda; longitude = park.longituda }
                                    val userAndroidLocation = Location("").apply { latitude = it.latitude; longitude = it.longitude }
                                    val distance = userAndroidLocation.distanceTo(parkLocation) / 1000f // u km
                                    Text(String.format("%.1f km", distance), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
