package com.example.myapplication

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AuthGateScreen(navController: NavController) {
    var navigationPerformed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }

    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            if (!navigationPerformed) {
                if (firebaseAuth.currentUser != null) {

                    navController.navigate("map") { popUpTo(0) }
                } else {
                    navController.navigate("login") { popUpTo(0) }
                }
                navigationPerformed = true
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener(authListener)

        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        }
    }
}
