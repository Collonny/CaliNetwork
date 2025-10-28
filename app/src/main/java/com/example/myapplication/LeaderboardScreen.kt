package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    var leaderboardData by remember { mutableStateOf<List<LeaderboardEntry>?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            leaderboardData = FirestoreService.getLeaderboardData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rang Lista") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (leaderboardData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues), contentPadding = PaddingValues(8.dp)) {
                itemsIndexed(leaderboardData!!) { index, entry ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}.", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.userName, fontWeight = FontWeight.Bold)
                                Text("Zgibovi: ${entry.bestScores["zgibovi"]}")
                                Text("Sklekovi: ${entry.bestScores["sklekovi"]}")
                                Text("Propadanja: ${entry.bestScores["propadanja"]}")
                            }
                            Text("Ukupno: ${entry.totalScore}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
