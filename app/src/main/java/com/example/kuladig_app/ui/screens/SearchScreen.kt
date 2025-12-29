package com.example.kuladig_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.kuladig_app.KuladigApplication
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.TravelMode
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.PI

data class ObjectWithDistance(
    val kuladigObject: KuladigObject,
    val distance: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onRouteRequest: (KuladigObject, TravelMode) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val application = context.applicationContext as KuladigApplication
    val repository = application.repository

    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var allObjects by remember { mutableStateOf<List<KuladigObject>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Lade Objekte aus der Datenbank
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                allObjects = repository.getAllObjects()
                android.util.Log.d("SearchScreen", "Geladene Objekte: ${allObjects.size}")
                if (allObjects.isEmpty()) {
                    android.util.Log.w("SearchScreen", "Keine Objekte in der Datenbank gefunden!")
                    // Versuche Import zu erzwingen, falls Datenbank leer ist
                    val importService = application.jsonImportService
                    val importResult = importService.forceImport()
                    android.util.Log.d("SearchScreen", "Import-Ergebnis: $importResult")
                    // Lade Objekte erneut nach Import
                    allObjects = repository.getAllObjects()
                    android.util.Log.d("SearchScreen", "Objektanzahl nach Import: ${allObjects.size}")
                }
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("SearchScreen", "Fehler beim Laden der Objekte", e)
                isLoading = false
            }
        }
    }

    // Lade Standort
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                hasLocationPermission = false
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Berechne Entfernungen und sortiere
    val objectsWithDistance = remember(allObjects, userLocation, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            allObjects
        } else {
            allObjects.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

        if (userLocation != null) {
            filtered.map { obj ->
                val distance = calculateDistance(
                    userLocation!!.latitude,
                    userLocation!!.longitude,
                    obj.latitude,
                    obj.longitude
                )
                ObjectWithDistance(obj, distance)
            }.sortedBy { it.distance }
        } else {
            filtered.map { ObjectWithDistance(it, Double.MAX_VALUE) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Objekte suchen") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Nach Name suchen") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Objekte werden geladen...")
                }
            } else if (objectsWithDistance.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Keine Objekte gefunden" else "Keine Ergebnisse für \"$searchQuery\""
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    items(objectsWithDistance) { item ->
                        ObjectItemCard(
                            objectWithDistance = item,
                            userLocation = userLocation,
                            onRouteRequest = { mode ->
                                onRouteRequest(item.kuladigObject, mode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectItemCard(
    objectWithDistance: ObjectWithDistance,
    userLocation: LatLng?,
    onRouteRequest: (TravelMode) -> Unit = {}
) {
    val distanceText = if (userLocation != null && objectWithDistance.distance != Double.MAX_VALUE) {
        formatDistance(objectWithDistance.distance)
    } else {
        null
    }
    
    var showTravelModeDialog by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = {
            Text(
                text = objectWithDistance.kuladigObject.name,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            if (objectWithDistance.kuladigObject.beschreibung.isNotEmpty()) {
                Text(
                    text = objectWithDistance.kuladigObject.beschreibung,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null
            )
        },
        trailingContent = {
            Column {
                if (distanceText != null) {
                    Text(
                        text = distanceText,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { showTravelModeDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Route anzeigen"
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
    
    if (showTravelModeDialog) {
        TravelModeDialog(
            onDismiss = { showTravelModeDialog = false },
            onModeSelected = { mode ->
                showTravelModeDialog = false
                onRouteRequest(mode)
            }
        )
    }
}

/**
 * Berechnet die Entfernung zwischen zwei Koordinaten mit der Haversine-Formel
 * @return Entfernung in Metern
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // Erdradius in Metern

    val dLat = (lat2 - lat1) * PI / 180.0
    val dLon = (lon2 - lon1) * PI / 180.0

    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * PI / 180.0) * cos(lat2 * PI / 180.0) *
            sin(dLon / 2) * sin(dLon / 2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}

/**
 * Formatiert die Entfernung in eine lesbare Zeichenkette
 */
fun formatDistance(distanceInMeters: Double): String {
    return if (distanceInMeters < 1000) {
        "${distanceInMeters.toInt()} m"
    } else {
        val km = distanceInMeters / 1000.0
        "%.1f km".format(km)
    }
}

@Composable
private fun TravelModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transportmodus wählen") },
        text = {
            Column {
                Text("Wie möchten Sie reisen?")
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onModeSelected(TravelMode.WALKING) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zu Fuß")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onModeSelected(TravelMode.DRIVING) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

