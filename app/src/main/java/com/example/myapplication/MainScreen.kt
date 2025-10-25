package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // 🔹 VRAĆENA RUTA ZA LOGIN
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // 🔹 VRAĆENA RUTA ZA REGISTER
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        // RUTA ZA LISTU PARKOVA
        composable("parkList") {
            ParkListScreen(
                onBack = { navController.popBackStack() },
                onParkSelected = { park ->
                    navController.navigate("map?lat=${park.latituda}&lng=${park.longituda}")
                }
            )
        }

        // RUTA ZA MAPU
        composable(
            route = "map?lat={lat}&lng={lng}",
            arguments = listOf(
                navArgument("lat") { nullable = true; type = NavType.StringType },
                navArgument("lng") { nullable = true; type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()

            MapScreen(
                navController = navController,
                initialLat = lat,
                initialLng = lng,
                onParkClick = { park: WorkoutPark ->
                    val encodedName = URLEncoder.encode(park.name, "UTF-8")
                    val encodedOpis = URLEncoder.encode(park.opis, "UTF-8")
                    val route = "parkDetail/$encodedName/${park.latituda}/${park.longituda}/${park.rating}/$encodedOpis"
                    navController.navigate(route)
                }
            )
        }

        // RUTA ZA DETALJE PARKA
        composable(
            route = "parkDetail/{name}/{lat}/{lng}/{rating}/{opis}",
            arguments = listOf(
                navArgument("name") { type = NavType.StringType },
                navArgument("lat") { type = NavType.StringType },
                navArgument("lng") { type = NavType.StringType },
                navArgument("rating") { type = NavType.StringType },
                navArgument("opis") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
            val opis = URLDecoder.decode(backStackEntry.arguments?.getString("opis") ?: "", "UTF-8")
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
            val rating = backStackEntry.arguments?.getString("rating")?.toFloatOrNull() ?: 0f

            ParkDetailScreen(
                parkName = name,
                parkDescription = opis,
                parkRating = rating,
                parkLat = lat,
                parkLng = lng,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
