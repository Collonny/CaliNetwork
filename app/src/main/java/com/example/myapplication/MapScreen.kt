package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    navController: NavController,
    onParkClick: (WorkoutPark) -> Unit,
    initialLat: Double?,
    initialLng: Double?
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var workoutParks by remember { mutableStateOf(listOf<WorkoutPark>()) }
    var otherUsers by remember { mutableStateOf(listOf<UserLocation>()) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showAddParkDialog by remember { mutableStateOf(false) }
    val notifiedParks = remember { mutableStateListOf<String>() }
    val notifiedUsers = remember { mutableStateListOf<String>() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(43.3209, 21.8958), 13f)
    }

    // --- Dozvole ---
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else { null }

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
        notificationPermissionState?.launchPermissionRequest()
    }
    
    // --- Pomeranje kamere na prosleđenu lokaciju ---
    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != null && initialLng != null) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 15f)
            )
        }
    }

    // --- Praćenje i slanje lokacije ---
    DisposableEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        if (currentUserId != null) {
                            FirestoreService.updateUserLocation(currentUserId, it.latitude, it.longitude)
                        }
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else { onDispose { } }
    }

    // --- Provera blizine (Parkovi i Korisnici) ---
    LaunchedEffect(userLocation, workoutParks, otherUsers) {
        if (userLocation == null) return@LaunchedEffect
        val userAndroidLocation = Location("").apply {
            latitude = userLocation!!.latitude
            longitude = userLocation!!.longitude
        }

        workoutParks.forEach { park ->
            val parkLocation = Location("").apply { latitude = park.latituda; longitude = park.longituda }
            if (userAndroidLocation.distanceTo(parkLocation) < 200 && !notifiedParks.contains(park.name)) {
                NotificationHelper.showParkNotification(context, park)
                notifiedParks.add(park.name)
            }
        }

        otherUsers.forEach { otherUser ->
            val otherUserLocation = Location("").apply { latitude = otherUser.latitude; longitude = otherUser.longitude }
            if (userAndroidLocation.distanceTo(otherUserLocation) < 200 && !notifiedUsers.contains(otherUser.userId)) {
                NotificationHelper.showUserNotification(context, otherUser)
                notifiedUsers.add(otherUser.userId)
            }
        }
    }

    // --- Učitavanje podataka ---
    DisposableEffect(Unit) {
        val parksListener = FirestoreService.getWorkoutParks(
            onResult = { parks -> workoutParks = parks },
            onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        val usersListener = if (currentUserId != null) {
            FirestoreService.listenForOtherUsers(currentUserId,
                onResult = { users -> otherUsers = users },
                onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
            )
        } else { null }

        onDispose {
            parksListener.remove()
            usersListener?.remove()
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa Parkova") },
                actions = {
                    IconButton(onClick = { navController.navigate("parkList") }) {
                        Icon(Icons.Filled.List, contentDescription = "Lista parkova")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (userLocation != null) {
                    showAddParkDialog = true
                } else {
                    Toast.makeText(context, "Trenutna lokacija nije dostupna.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Dodaj park")
            }
        }
    ) { paddingValues ->
        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted)
        ) {
            // Markeri za parkove
            workoutParks.forEach { park ->
                MarkerInfoWindowContent(
                    state = MarkerState(position = LatLng(park.latituda, park.longituda)),
                    title = park.name,
                    snippet = "Klikni za detalje",
                    onInfoWindowClick = { onParkClick(park) }
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(text = park.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = park.opis)
                    }
                }
            }
            // Markeri za druge korisnike
            otherUsers.forEach { user ->
                Marker(
                    state = MarkerState(position = LatLng(user.latitude, user.longitude)),
                    title = user.username
                )
            }
        }

        if (showAddParkDialog) {
            AddParkDialog(
                onDismiss = { showAddParkDialog = false },
                onConfirm = { name, opis ->
                    if (currentUserId != null && userLocation != null) {
                        FirestoreService.addWorkoutPark(
                            name = name,
                            opis = opis,
                            lat = userLocation!!.latitude,
                            lng = userLocation!!.longitude,
                            creatorId = currentUserId,
                            onSuccess = { 
                                Toast.makeText(context, "Park uspešno dodat!", Toast.LENGTH_SHORT).show()
                                showAddParkDialog = false 
                            },
                            onError = { 
                                Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun AddParkDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var parkName by remember { mutableStateOf("") }
    var parkOpis by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj novi park") },
        text = {
            Column {
                OutlinedTextField(
                    value = parkName,
                    onValueChange = { parkName = it },
                    label = { Text("Ime parka") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = parkOpis,
                    onValueChange = { parkOpis = it },
                    label = { Text("Opis parka") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                if (parkName.isNotBlank()) {
                    onConfirm(parkName, parkOpis)
                } 
            }) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Odustani")
            }
        }
    )
}
