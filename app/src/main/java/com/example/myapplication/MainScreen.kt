package com.example.myapplication

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.parcelize.Parcelize
import java.net.URLDecoder
import java.net.URLEncoder

@Parcelize
data class RatingParcelable(val average: Double, val brRatings: Long, val userRatings: Map<String, Long>) : Parcelable

@Parcelize
data class WorkoutParkParcelable(
    val id: String, val name: String, val opis: String, val latituda: Double, 
    val longituda: Double, val rating: RatingParcelable, val createdBy: String, 
    val challenges: Map<String, ChallengeParcelable>
) : Parcelable

@Parcelize
data class ChallengeParcelable(val bestUser: String, val bestScore: Long) : Parcelable


fun Rating.toParcelable(): RatingParcelable {
    val longUserRatings = userRatings.mapValues { (it.value as? Number)?.toLong() ?: 0L }
    return RatingParcelable(average, brRatings, longUserRatings)
}
fun WorkoutPark.toParcelable(): WorkoutParkParcelable {
    return WorkoutParkParcelable(id, name, opis, latituda, longituda, rating.toParcelable(), createdBy, challenges.mapValues { it.value.toParcelable() })
}
fun Challenge.toParcelable(): ChallengeParcelable {
    return ChallengeParcelable(bestUser, bestScore)
}
fun RatingParcelable.toOriginal(): Rating {
    return Rating(average, brRatings, userRatings)
}
fun WorkoutParkParcelable.toOriginal(): WorkoutPark {
    return WorkoutPark(id, name, opis, latituda, longituda, rating.toOriginal(), createdBy, challenges = challenges.mapValues { it.value.toOriginal() })
}
fun ChallengeParcelable.toOriginal(): Challenge {
    return Challenge(bestUser, bestScore)
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "authGate") {

        composable("authGate") {
            AuthGateScreen(navController = navController)
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("map") {
                        popUpTo(0)
                    }
                },
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable("parkList") {
            ParkListScreen(
                onBack = { navController.popBackStack() },
                onParkSelected = { park ->
                    navController.navigate("map?lat=${park.latituda}&lng=${park.longituda}")
                }
            )
        }

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
                onParkClick = { park ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("park", park.toParcelable())
                    navController.navigate("parkDetail")
                }
            )
        }

        composable("parkDetail") {
            val parkParcelable = navController.previousBackStackEntry?.savedStateHandle?.get<WorkoutParkParcelable>("park")
            if (parkParcelable != null) {
                ParkDetailScreen(
                    park = parkParcelable.toOriginal(),
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable("leaderboard") {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
    }
}
