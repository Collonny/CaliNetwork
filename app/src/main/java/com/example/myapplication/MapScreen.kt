package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import kotlin.math.roundToInt

data class FilterState(val distance: Float, val minRating: Float)

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
    val auth = FirebaseAuth.getInstance()
    var currentUserId by remember { mutableStateOf(auth.currentUser?.uid) }

    var workoutParks by remember { mutableStateOf(listOf<WorkoutPark>()) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var nearbyPark by remember { mutableStateOf<WorkoutPark?>(null) }
    var showAddParkDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterState by remember { mutableStateOf(FilterState(distance = 10000f, minRating = 0f)) }
    val notifiedParks = remember { mutableStateListOf<String>() }
    var isMapInitialized by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(43.3209, 21.8958), 13f) }

    val filteredParks = remember(filterState, workoutParks, userLocation) {
        var parksToFilter = workoutParks
        parksToFilter = parksToFilter.filter { it.rating.average >= filterState.minRating }
        userLocation?.let { loc ->
            val userAndroidLocation = Location("").apply { latitude = loc.latitude; longitude = loc.longitude }
            parksToFilter = parksToFilter.filter {
                val parkLocation = Location("").apply { latitude = it.latituda; longitude = it.longituda }
                userAndroidLocation.distanceTo(parkLocation) <= filterState.distance
            }
        }
        parksToFilter
    }

    LaunchedEffect(userLocation) {
        if (userLocation != null && !isMapInitialized) {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f),
                durationMs = 1500
            )
            isMapInitialized = true
        }
    }

    LaunchedEffect(filteredParks) {
        if (filteredParks.isNotEmpty() && isMapInitialized) {
            val boundsBuilder = LatLngBounds.Builder()
            filteredParks.forEach { park ->
                boundsBuilder.include(LatLng(park.latituda, park.longituda))
            }
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100),
                durationMs = 1000
            )
        }
    }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { currentAuth -> currentUserId = currentAuth.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != null && initialLng != null) {
            cameraPositionState.animate(update = CameraUpdateFactory.newLatLngZoom(LatLng(initialLat, initialLng), 15f))
            isMapInitialized = true
        }
    }

    DisposableEffect(currentUserId) {
        val localUserId = currentUserId ?: return@DisposableEffect onDispose {}
        val parksListener = FirestoreService.getWorkoutParks(
            onResult = { parks -> workoutParks = parks },
            onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
        )
        onDispose {
            parksListener.remove()
        }
    }
    
    DisposableEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        currentUserId?.let { uid ->
                            FirestoreService.updateUserLocation(uid, it.latitude, it.longitude)
                        }
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
            onDispose { fusedLocationClient.removeLocationUpdates(locationCallback) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionState.launchPermissionRequest()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mapa Parkova") },
                actions = {
                    IconButton(onClick = { navController.navigate("parkList") }) {
                        Icon(Icons.Filled.List, contentDescription = "Lista parkova")
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = { navController.navigate("leaderboard") }) {
                        Icon(Icons.Filled.Leaderboard, contentDescription = "Rang Lista")
                    }
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Odjavi se")
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Start,
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

        if (showFilterDialog) {
            FilterDialog(
                initialState = filterState,
                onDismiss = { showFilterDialog = false },
                onApply = { newState ->
                    filterState = newState
                    showFilterDialog = false
                }
            )
        }

        if (showAddParkDialog) {
            AddParkDialog(
                onDismiss = { showAddParkDialog = false },
                onConfirm = { name, opis ->
                    val localUserId = currentUserId
                    if (localUserId != null && userLocation != null) {
                        FirestoreService.addWorkoutPark(
                            name = name, opis = opis, lat = userLocation!!.latitude, lng = userLocation!!.longitude, creatorId = localUserId,
                            onSuccess = { 
                                Toast.makeText(context, "Park uspešno dodat!", Toast.LENGTH_SHORT).show()
                                showAddParkDialog = false 
                            },
                            onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            )
        }

        if (showAddRecordDialog && nearbyPark != null) {
            val localUserId = currentUserId
            if (localUserId != null) {
                AddRecordDialog(
                    park = nearbyPark!!, userId = localUserId, onDismiss = { showAddRecordDialog = false },
                    onConfirm = { record ->
                        FirestoreService.addRecord(record, 
                            onSuccess = { Toast.makeText(context, "Rekord uspešno dodat!", Toast.LENGTH_SHORT).show() },
                            onError = { error -> Toast.makeText(context, "Greška: ${error.message}", Toast.LENGTH_SHORT).show() }
                        )
                        showAddRecordDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterDialog(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit
) {
    var sliderDistance by remember { mutableStateOf(initialState.distance) }
    var sliderRating by remember { mutableStateOf(initialState.minRating) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filteri") },
        text = {
            Column {
                Text(String.format("Maksimalna udaljenost: %.1f km", sliderDistance / 1000f))
                Slider(
                    value = sliderDistance,
                    onValueChange = { sliderDistance = it },
                    valueRange = 0f..10000f,
                    steps = 9
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(String.format("Minimalna ocena: %.1f", sliderRating))
                Slider(
                    value = sliderRating,
                    onValueChange = { sliderRating = it },
                    valueRange = 0f..5f,
                    steps = 9
                )
            }
        },
        confirmButton = { Button(onClick = { onApply(FilterState(distance = sliderDistance, minRating = sliderRating)) }) { Text("Primeni") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Odustani") } }
    )
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
                OutlinedTextField(value = parkName, onValueChange = { parkName = it }, label = { Text("Ime parka") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = parkOpis, onValueChange = { parkOpis = it }, label = { Text("Opis parka") })
            }
        },
        confirmButton = { Button(onClick = { if (parkName.isNotBlank()) { onConfirm(parkName, parkOpis) } }) { Text("Dodaj") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Odustani") } }
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
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedExercise, onValueChange = {},
                        readOnly = true, label = { Text("Vežba") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
                OutlinedTextField(value = score, onValueChange = { score = it }, label = { Text("Rezultat") })
            }
        },
        confirmButton = { Button(onClick = {
            val scoreLong = score.toLongOrNull()
            if (scoreLong != null && selectedExercise.isNotBlank()) {
                onConfirm(UserRecord(userId, park.id, selectedExercise, scoreLong, com.google.firebase.Timestamp.now()))
            }
        }) { Text("Dodaj") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Odustani") } }
    )
}
