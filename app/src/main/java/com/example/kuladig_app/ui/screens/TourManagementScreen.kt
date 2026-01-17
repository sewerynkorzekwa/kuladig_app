package com.example.kuladig_app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kuladig_app.KuladigApplication
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.model.TourStop
import com.example.kuladig_app.ui.components.TourEditorBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TourWithStops(
    val tour: Tour,
    val stops: List<TourStop>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourManagementScreen(
    modifier: Modifier = Modifier,
    onTourStart: (Tour, List<KuladigObject>) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val application = context.applicationContext as KuladigApplication
    val repository = application.repository

    var tours by remember { mutableStateOf<List<TourWithStops>>(emptyList()) }
    var allObjects by remember { mutableStateOf<List<KuladigObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showEditor by remember { mutableStateOf(false) }
    var editingTour by remember { mutableStateOf<TourWithStops?>(null) }
    var tourToDelete by remember { mutableStateOf<Tour?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Lade Tours und Objekte
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val allTours = repository.getAllTours()
                val toursWithStops = allTours.map { tour ->
                    val (_, stops) = repository.getTourWithStops(tour.id)
                    TourWithStops(tour, stops)
                }
                tours = toursWithStops
                allObjects = repository.getAllObjects()
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("TourManagementScreen", "Fehler beim Laden", e)
                isLoading = false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTour = null
                    showEditor = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Neue Tour")
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Tours werden geladen...")
            }
        } else if (tours.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Keine Tours vorhanden",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tippen Sie auf + um eine neue Tour zu erstellen",
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(tours) { tourWithStops ->
                    TourCard(
                        tourWithStops = tourWithStops,
                        onEdit = {
                            editingTour = tourWithStops
                            showEditor = true
                        },
                        onDelete = {
                            tourToDelete = tourWithStops.tour
                        },
                        onStart = {
                            val objects = tourWithStops.stops.mapNotNull { stop ->
                                allObjects.find { it.id == stop.kuladigObjectId }
                            }
                            onTourStart(tourWithStops.tour, objects)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showEditor) {
        val tour = editingTour
        TourEditorBottomSheet(
            tourName = tour?.tour?.name ?: "",
            tourDescription = tour?.tour?.description ?: "",
            initialStops = tour?.let { t ->
                t.stops.sortedBy { it.stopOrder }
                    .mapNotNull { stop ->
                        allObjects.find { it.id == stop.kuladigObjectId }
                    }
            } ?: emptyList(),
            initialTravelMode = tour?.tour?.travelMode ?: com.example.kuladig_app.data.model.TravelMode.WALKING,
            allObjects = allObjects,
            onDismiss = {
                showEditor = false
                editingTour = null
            },
            onSave = { name, description, stops, travelMode ->
                val tourMode = travelMode
                
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        if (tour != null) {
                            // Tour aktualisieren
                            val updatedTour = tour.tour.copy(
                                name = name,
                                description = description,
                                travelMode = tourMode
                            )
                            val tourStops = stops.mapIndexed { index, obj ->
                                TourStop(
                                    id = 0,
                                    tourId = tour.tour.id,
                                    kuladigObjectId = obj.id,
                                    stopOrder = index,
                                    notes = null
                                )
                            }
                            repository.updateTour(updatedTour, tourStops)
                        } else {
                            // Neue Tour erstellen
                            val newTour = Tour(
                                id = 0,
                                name = name,
                                description = description,
                                createdAt = System.currentTimeMillis(),
                                travelMode = tourMode
                            )
                            val tourStops = stops.mapIndexed { index, obj ->
                                TourStop(
                                    id = 0,
                                    tourId = 0,
                                    kuladigObjectId = obj.id,
                                    stopOrder = index,
                                    notes = null
                                )
                            }
                            repository.insertTour(newTour, tourStops)
                        }
                        
                        // Lade Tours neu
                        val allTours = repository.getAllTours()
                        val toursWithStops = allTours.map { t ->
                            val (_, stops) = repository.getTourWithStops(t.id)
                            TourWithStops(t, stops)
                        }
                        withContext(Dispatchers.Main) {
                            tours = toursWithStops
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TourManagementScreen", "Fehler beim Speichern", e)
                    }
                }
                
                showEditor = false
                editingTour = null
            }
        )
    }

    if (tourToDelete != null) {
        AlertDialog(
            onDismissRequest = { tourToDelete = null },
            title = { Text("Tour löschen") },
            text = { Text("Möchten Sie die Tour \"${tourToDelete?.name}\" wirklich löschen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        tourToDelete?.let { tour ->
                            coroutineScope.launch(Dispatchers.IO) {
                                try {
                                    repository.deleteTour(tour.id)
                                    // Lade Tours neu
                                    val allTours = repository.getAllTours()
                                    val toursWithStops = allTours.map { t ->
                                        val (_, stops) = repository.getTourWithStops(t.id)
                                        TourWithStops(t, stops)
                                    }
                                    withContext(Dispatchers.Main) {
                                        tours = toursWithStops
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("TourManagementScreen", "Fehler beim Löschen", e)
                                }
                            }
                        }
                        tourToDelete = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { tourToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
fun TourCard(
    tourWithStops: TourWithStops,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tourWithStops.tour.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!tourWithStops.tour.description.isNullOrBlank()) {
                        Text(
                            text = tourWithStops.tour.description ?: "",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${tourWithStops.stops.size} Stopps • ${when (tourWithStops.tour.travelMode) {
                            com.example.kuladig_app.data.model.TravelMode.WALKING -> "Zu Fuß"
                            com.example.kuladig_app.data.model.TravelMode.DRIVING -> "Auto"
                        }}",
                        fontSize = 12.sp
                    )
                }
                Row {
                    IconButton(onClick = onStart) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Tour starten")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                }
            }
        }
    }
}
