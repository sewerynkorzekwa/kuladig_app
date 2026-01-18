package com.example.kuladig_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.kuladig_app.KuladigApplication
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.model.TravelMode
import com.example.kuladig_app.data.model.VRObject
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
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToMap: () -> Unit = {},
    onNavigateToVR: () -> Unit = {},
    onNavigateToTours: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onRouteRequest: (KuladigObject, TravelMode) -> Unit = { _, _ -> },
    onTourStart: (Tour, List<KuladigObject>) -> Unit = { _, _ -> },
    onVRObjectSelected: (VRObject) -> Unit = {},
    activeTour: Pair<Tour, List<KuladigObject>>? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as KuladigApplication
    val repository = remember { application.repository }
    
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
    var kuladigObjects by remember { mutableStateOf<List<KuladigObject>>(emptyList()) }
    var tours by remember { mutableStateOf<List<Tour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Lade Daten aus Repository
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                kuladigObjects = repository.getAllObjects()
                tours = repository.getAllTours()
                isLoading = false
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Fehler beim Laden der Daten", e)
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

    // Berechne nahe Objekte
    val nearbyObjects = remember(kuladigObjects, userLocation) {
        if (userLocation != null && kuladigObjects.isNotEmpty()) {
            kuladigObjects.map { obj ->
                val distance = calculateDistance(
                    userLocation!!.latitude,
                    userLocation!!.longitude,
                    obj.latitude,
                    obj.longitude
                )
                ObjectWithDistance(obj, distance)
            }.sortedBy { it.distance }.take(5)
        } else {
            emptyList()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Home") }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Daten werden geladen...")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Quick Access Section
                item {
                    QuickAccessSection(
                        onNavigateToMap = onNavigateToMap,
                        onNavigateToVR = onNavigateToVR,
                        onNavigateToTours = onNavigateToTours
                    )
                }

                // Statistics Section
                item {
                    StatisticsSection(
                        objectCount = kuladigObjects.size,
                        tourCount = tours.size,
                        vrObjectCount = VRObject.getAllObjects().size
                    )
                }

                // Active Tour Section
                activeTour?.let { (tour, stops) ->
                    item {
                        ActiveTourSection(
                            tour = tour,
                            stops = stops,
                            onTourStart = { onTourStart(tour, stops) }
                        )
                    }
                }

                // Nearby Objects Section
                if (nearbyObjects.isNotEmpty()) {
                    item {
                        NearbyObjectsSection(
                            nearbyObjects = nearbyObjects,
                            userLocation = userLocation,
                            onRouteRequest = onRouteRequest
                        )
                    }
                }

                // Recent Objects Section (Platzhalter - könnte später mit SharedPreferences implementiert werden)
                item {
                    RecentObjectsSection(
                        recentObjects = kuladigObjects.take(5),
                        onObjectClick = { obj ->
                            // Navigate to map with object selected
                            onRouteRequest(obj, TravelMode.WALKING)
                        }
                    )
                }

                // Quick Actions Section
                item {
                    QuickActionsSection(
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToTours = onNavigateToTours
                    )
                }

                // VR Objects Preview Section
                item {
                    VRObjectsPreviewSection(
                        vrObjects = VRObject.getAllObjects(),
                        onVRObjectSelected = onVRObjectSelected
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAccessSection(
    onNavigateToMap: () -> Unit,
    onNavigateToVR: () -> Unit,
    onNavigateToTours: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Schnellzugriff",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = onNavigateToMap,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Zur Karte")
            }
            FilledTonalButton(
                onClick = onNavigateToVR,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ViewInAr,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("VR/AR")
            }
            FilledTonalButton(
                onClick = onNavigateToTours,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Tours")
            }
        }
    }
}

@Composable
fun StatisticsSection(
    objectCount: Int,
    tourCount: Int,
    vrObjectCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Statistiken",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$objectCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Objekte",
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(
                        text = "$tourCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tours",
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text(
                        text = "$vrObjectCount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "VR-Objekte",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveTourSection(
    tour: Tour,
    stops: List<KuladigObject>,
    onTourStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aktive Tour",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = tour.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (!tour.description.isNullOrBlank()) {
                Text(
                    text = tour.description ?: "",
                    fontSize = 14.sp
                )
            }
            Text(
                text = "${stops.size} Stopps",
                fontSize = 14.sp
            )
            androidx.compose.material3.Button(
                onClick = onTourStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tour fortsetzen")
            }
        }
    }
}

@Composable
fun NearbyObjectsSection(
    nearbyObjects: List<ObjectWithDistance>,
    userLocation: LatLng?,
    onRouteRequest: (KuladigObject, TravelMode) -> Unit
) {
    var showTravelModeDialog by remember { mutableStateOf(false) }
    var selectedObject by remember { mutableStateOf<KuladigObject?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Objekte in der Nähe",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            nearbyObjects.forEach { objWithDistance ->
                val distanceText = if (userLocation != null && objWithDistance.distance != Double.MAX_VALUE) {
                    formatDistance(objWithDistance.distance)
                } else {
                    null
                }
                
                ListItem(
                    headlineContent = {
                        Text(
                            text = objWithDistance.kuladigObject.name,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supportingContent = {
                        if (distanceText != null) {
                            Text(text = distanceText)
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedObject = objWithDistance.kuladigObject
                            showTravelModeDialog = true
                        }
                )
            }
        }
    }

    if (showTravelModeDialog && selectedObject != null) {
        TravelModeDialog(
            onDismiss = {
                showTravelModeDialog = false
                selectedObject = null
            },
            onModeSelected = { mode ->
                selectedObject?.let { obj ->
                    onRouteRequest(obj, mode)
                }
                showTravelModeDialog = false
                selectedObject = null
            }
        )
    }
}

@Composable
fun RecentObjectsSection(
    recentObjects: List<KuladigObject>,
    onObjectClick: (KuladigObject) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Zuletzt besucht",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            recentObjects.forEach { obj ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = obj.name,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supportingContent = {
                        if (obj.beschreibung.isNotEmpty()) {
                            Text(
                                text = obj.beschreibung,
                                maxLines = 2
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onObjectClick(obj) }
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onNavigateToSearch: () -> Unit,
    onNavigateToTours: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Schnellaktionen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            OutlinedButton(
                onClick = onNavigateToSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Objekte suchen")
            }
            OutlinedButton(
                onClick = onNavigateToTours,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Neue Tour erstellen")
            }
        }
    }
}

@Composable
fun VRObjectsPreviewSection(
    vrObjects: List<VRObject>,
    onVRObjectSelected: (VRObject) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "VR-Objekte",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            vrObjects.forEach { vrObject ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = vrObject.title,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    supportingContent = {
                        Text(
                            text = vrObject.description.take(100) + if (vrObject.description.length > 100) "..." else "",
                            maxLines = 2
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVRObjectSelected(vrObject) }
                )
            }
        }
    }
}

@Composable
private fun TravelModeDialog(
    onDismiss: () -> Unit,
    onModeSelected: (TravelMode) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transportmodus wählen") },
        text = {
            Column {
                Text("Wie möchten Sie reisen?")
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = { onModeSelected(TravelMode.WALKING) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zu Fuß")
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.Button(
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
