package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var nearbyPark by remember { mutableStateOf<WorkoutPark?>(null) }
    var showAddParkDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    val notifiedParks = remember { mutableStateListOf<String>() }
    val notifiedUsers = remember { mutableStateListOf<String>() }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf<Int?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(43.3209, 21.8958), 13f)
    }

    val filteredParks = remember(selectedDistance, workoutParks, userLocation) {
        if (selectedDistance == null || userLocation == null) {
            workoutParks
        } else {
            val userAndroidLocation = Location("").apply {
                latitude = userLocation!!.latitude
                longitude = userLocation!!.longitude
            }
            workoutParks.filter {
                val parkLocation = Location("").apply { latitude = it.latituda; longitude = it.longituda }
                userAndroidLocation.distanceTo(parkLocation) <= selectedDistance!!
            }
        }
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

        nearbyPark = workoutParks.firstOrNull { park ->
            val parkLocation = Location("").apply { latitude = park.latituda; longitude = park.longituda }
            userAndroidLocation.distanceTo(parkLocation) < 200
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
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(text = { Text("Prikaži sve") }, onClick = { selectedDistance = null; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Do 1 km") }, onClick = { selectedDistance = 1000; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Do 5 km") }, onClick = { selectedDistance = 5000; showFilterMenu = false })
                        }
                    }
                    IconButton(onClick = { navController.navigate("leaderboard") }) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = "Rang Lista")
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
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)){
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted)
            ) {
                filteredParks.forEach { park ->
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
                otherUsers.forEach { user ->
                    Marker(
                        state = MarkerState(position = LatLng(user.latitude, user.longitude)),
                        title = user.username
                    )
                }
            }

            if (nearbyPark != null) {
                Button(
                    onClick = { showAddRecordDialog = true },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    Text("Dodaj Rekord za ${nearbyPark!!.name}")
                }
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

        if (showAddRecordDialog && nearbyPark != null && currentUserId != null) {
            AddRecordDialog(
                park = nearbyPark!!,
                userId = currentUserId,
                onDismiss = { showAddRecordDialog = false },
                onConfirm = { record ->
                    FirestoreService.addRecord(record, 
                        onSuccess = { Toast.makeText(context, "Rekord uspešno dodat!", Toast.LENGTH_SHORT).show() },
                        onError = { Toast.makeText(context, "Greška: ${it.message}", Toast.LENGTH_SHORT).show() }
                    )
                    showAddRecordDialog = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordDialog(
    park: WorkoutPark,
    userId: String,
    onDismiss: () -> Unit,
    onConfirm: (UserRecord) -> Unit
) {
    val exercises = park.challenges.keys.toList()
    var expanded by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf(exercises.firstOrNull() ?: "") }
    var score by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj novi rekord") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedExercise,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vežba") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        exercises.forEach { exercise ->
                            DropdownMenuItem(
                                text = { Text(exercise) },
                                onClick = {
                                    selectedExercise = exercise
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = score,
                    onValueChange = { score = it },
                    label = { Text("Rezultat") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val scoreLong = score.toLongOrNull()
                if (scoreLong != null && selectedExercise.isNotBlank()) {
                    onConfirm(UserRecord(userId, park.id, selectedExercise, scoreLong, com.google.firebase.Timestamp.now()))
                }
            }) { Text("Dodaj") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Odustani") } }
    )
}
