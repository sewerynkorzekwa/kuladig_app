package com.example.kuladig_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.kuladig_app.KuladigApplication
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.Route
import com.example.kuladig_app.data.model.TravelMode
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.service.DirectionsService
import com.example.kuladig_app.ui.components.MarkerInfoBottomSheet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import androidx.compose.runtime.DisposableEffect
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    initialRouteRequest: Pair<KuladigObject, TravelMode>? = null,
    onRouteRequestHandled: () -> Unit = {},
    initialTour: Pair<Tour, List<KuladigObject>>? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as KuladigApplication
    val repository = remember { application.repository }
    
    // API-Key aus Manifest lesen
    val apiKey = remember {
        val appInfo = context.packageManager.getApplicationInfo(
            context.packageName,
            android.content.pm.PackageManager.GET_META_DATA
        )
        appInfo.metaData?.getString("com.google.android.geo.API_KEY") ?: ""
    }
    
    val directionsService = remember(apiKey) {
        if (apiKey.isNotEmpty()) DirectionsService(apiKey) else null
    }
    
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
    var selectedObject by remember { mutableStateOf<KuladigObject?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentRoute by remember { mutableStateOf<Route?>(null) }
    var routePolylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var routeError by remember { mutableStateOf<String?>(null) }
    var routeStartMarker by remember { mutableStateOf<KuladigObject?>(null) }
    var routeTravelMode by remember { mutableStateOf<TravelMode?>(null) }
    var isNavigating by remember { mutableStateOf(false) }
    var currentTour by remember { mutableStateOf<Pair<Tour, List<KuladigObject>>?>(null) }
    var currentTourStopIndex by remember { mutableStateOf(0) }
    var previousLocation by remember { mutableStateOf<Location?>(null) }
    var currentBearing by remember { mutableStateOf<Float?>(null) }
    
    // Kamera-State außerhalb des else-Blocks definieren, damit es überall verfügbar ist
    val cameraPositionState = rememberCameraPositionState {
        position = userLocation?.let {
            CameraPosition.fromLatLngZoom(it, 15f)
        } ?: CameraPosition.fromLatLngZoom(
            LatLng(52.5200, 13.4050), // Berlin als Fallback
            10f
        )
    }
    
    // LocationRequest mit hoher Genauigkeit für Echtzeit-Navigation
    val locationRequest = remember {
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMaxUpdateDelayMillis(5000L)
            .build()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // LocationCallback für kontinuierliche Positionsupdates
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                
                // Filtere ungenaue Positionen (Genauigkeit < 20 Meter)
                if (location.accuracy > 20f) {
                    Log.d("MapScreen", "Position zu ungenau: ${location.accuracy}m")
                    return
                }
                
                val newLatLng = LatLng(location.latitude, location.longitude)
                
                // Berechne Bearing wenn vorherige Position vorhanden
                previousLocation?.let { prev ->
                    val bearing = prev.bearingTo(location)
                    currentBearing = bearing
                }
                
                // Aktualisiere User-Location
                userLocation = newLatLng
                previousLocation = location
                
                // Navigation-Modus: Kamera folgt Position
                if (isNavigating && currentRoute != null) {
                    val bearing = currentBearing ?: 0f
                    val cameraPosition = CameraPosition.Builder()
                        .target(newLatLng)
                        .zoom(17f)
                        .bearing(bearing)
                        .tilt(45f) // Tilt für bessere Navigation-Ansicht
                        .build()
                    
                    // animate() ist eine suspend-Funktion, muss in Coroutine aufgerufen werden
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(cameraPosition)
                        )
                    }
                }
            }
        }
    }
    

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Lifecycle-Management für Location-Updates
    DisposableEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                // Starte kontinuierliche Location-Updates
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
                
                // Hole auch einmalige letzte Position für sofortige Anzeige
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        if (it.accuracy <= 20f) {
                            userLocation = LatLng(it.latitude, it.longitude)
                            previousLocation = it
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MapScreen", "SecurityException beim Starten der Location-Updates", e)
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
        
        onDispose {
            // Stoppe Location-Updates wenn Composable zerstört wird
            if (hasLocationPermission) {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                } catch (e: SecurityException) {
                    Log.e("MapScreen", "SecurityException beim Stoppen der Location-Updates", e)
                }
            }
        }
    }

    // Lade KuladigObjects aus der Datenbank - nur einmal beim ersten Render
    LaunchedEffect(Unit) {
        kuladigObjects = withContext(Dispatchers.IO) {
            repository.getAllObjects()
        }
    }
    
    // Verarbeite initialRouteRequest von SearchScreen
    LaunchedEffect(initialRouteRequest) {
        initialRouteRequest?.let { (obj, mode) ->
            if (directionsService != null && userLocation != null) {
                isLoadingRoute = true
                routeError = null
                
                val destinationLatLng = LatLng(obj.latitude, obj.longitude)
                val origin = userLocation!!
                
                val result = directionsService.getRoute(origin, destinationLatLng, mode)
                
                result.fold(
                    onSuccess = { route ->
                        val polylinePoints = directionsService.decodePolyline(route.overview_polyline.points)
                        currentRoute = route
                        routePolylinePoints = polylinePoints
                        isLoadingRoute = false
                        
                        // Kamera auf Route zentrieren
                        if (route.legs.isNotEmpty()) {
                            val start = route.legs.first().start_location
                            val end = route.legs.first().end_location
                            val center = LatLng(
                                (start.lat + end.lat) / 2,
                                (start.lng + end.lng) / 2
                            )
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(center, 13f)
                                )
                            )
                        }
                        
                        onRouteRequestHandled()
                    },
                    onFailure = { error ->
                        Log.e("MapScreen", "Fehler beim Berechnen der Route", error)
                        routeError = error.message ?: "Unbekannter Fehler"
                        isLoadingRoute = false
                        onRouteRequestHandled()
                    }
                )
            }
        }
    }

    // Verarbeite initialTour
    LaunchedEffect(initialTour) {
        initialTour?.let { (tour, stops) ->
            currentTour = initialTour
            currentTourStopIndex = 0
            if (directionsService != null && userLocation != null && stops.isNotEmpty()) {
                calculateTourRoute(directionsService, userLocation!!, stops, tour.travelMode)
            }
        }
    }

    // Hilfsfunktion zum Berechnen einer Tour-Route
    fun calculateTourRoute(
        service: DirectionsService,
        origin: LatLng,
        stops: List<KuladigObject>,
        mode: TravelMode
    ) {
        if (stops.isEmpty()) return
        
        isLoadingRoute = true
        routeError = null
        
        coroutineScope.launch {
            try {
                val waypoints = if (stops.size > 1) {
                    stops.dropLast(1).map { LatLng(it.latitude, it.longitude) }
                } else {
                    emptyList()
                }
                val destination = LatLng(stops.last().latitude, stops.last().longitude)
                
                val result = if (waypoints.isNotEmpty()) {
                    service.getRouteWithWaypoints(origin, waypoints, destination, mode)
                } else {
                    service.getRoute(origin, destination, mode)
                }
                
                result.fold(
                    onSuccess = { route ->
                        val polylinePoints = service.decodePolyline(route.overview_polyline.points)
                        currentRoute = route
                        routePolylinePoints = polylinePoints
                        isLoadingRoute = false
                        
                        // Kamera auf Route zentrieren
                        if (route.legs.isNotEmpty()) {
                            val firstLeg = route.legs.first()
                            val lastLeg = route.legs.last()
                            val start = firstLeg.start_location
                            val end = lastLeg.end_location
                            val center = LatLng(
                                (start.lat + end.lat) / 2,
                                (start.lng + end.lng) / 2
                            )
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.fromLatLngZoom(center, 13f)
                                )
                            )
                        }
                    },
                    onFailure = { error ->
                        Log.e("MapScreen", "Fehler beim Berechnen der Tour-Route", error)
                        routeError = error.message ?: "Unbekannter Fehler"
                        isLoadingRoute = false
                    }
                )
            } catch (e: Exception) {
                Log.e("MapScreen", "Fehler beim Berechnen der Tour-Route", e)
                routeError = e.message ?: "Unbekannter Fehler"
                isLoadingRoute = false
            }
        }
    }

    // Memoize Marker-Positionen um unnötige Recompositionen zu vermeiden
    val markerPositions = remember(kuladigObjects) {
        kuladigObjects.associate { obj ->
            obj.id to LatLng(obj.latitude, obj.longitude)
        }
    }
    
    // Tour-Stopp-Positionen
    val tourStopPositions = remember(currentTour) {
        currentTour?.let { (_, stops) ->
            stops.mapIndexed { index, obj -> index to LatLng(obj.latitude, obj.longitude) }
        } ?: emptyList()
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            Text(
                text = "Standortberechtigung wird angefordert...",
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            // Aktiviere/Deaktiviere Navigation-Modus basierend auf aktiver Route
            LaunchedEffect(currentRoute) {
                isNavigating = currentRoute != null
                if (!isNavigating) {
                    // Zurück zur normalen Ansicht wenn keine Route aktiv
                    userLocation?.let {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.fromLatLngZoom(it, 15f)
                            )
                        )
                    }
                }
            }
            
            // Initiale Kamera-Position nur wenn nicht im Navigation-Modus
            LaunchedEffect(userLocation) {
                if (!isNavigating && userLocation != null) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(userLocation!!, 15f)
                        )
                    )
                }
            }
            
            // Hilfsfunktion zum Berechnen einer Route
            fun calculateRouteToDestination(
                origin: LatLng,
                destination: KuladigObject,
                mode: TravelMode
            ) {
                if (directionsService == null) {
                    routeError = "Directions Service nicht verfügbar"
                    return
                }
                
                isLoadingRoute = true
                routeError = null
                
                coroutineScope.launch {
                    val destinationLatLng = LatLng(destination.latitude, destination.longitude)
                    val result = directionsService.getRoute(origin, destinationLatLng, mode)
                    
                    result.fold(
                        onSuccess = { route ->
                            val polylinePoints = directionsService.decodePolyline(route.overview_polyline.points)
                            currentRoute = route
                            routePolylinePoints = polylinePoints
                            routeStartMarker = null // Reset nach erfolgreicher Berechnung
                            isLoadingRoute = false
                            
                            // Kamera auf Route zentrieren
                            if (route.legs.isNotEmpty()) {
                                val start = route.legs.first().start_location
                                val end = route.legs.first().end_location
                                val center = LatLng(
                                    (start.lat + end.lat) / 2,
                                    (start.lng + end.lng) / 2
                                )
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newCameraPosition(
                                        CameraPosition.fromLatLngZoom(center, 13f)
                                    )
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.e("MapScreen", "Fehler beim Berechnen der Route", error)
                            routeError = error.message ?: "Unbekannter Fehler"
                            isLoadingRoute = false
                        }
                    )
                }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                userLocation?.let { location ->
                    Marker(
                        state = MarkerState(position = location),
                        title = "Ihr Standort"
                    )
                }
                
                // Marker mit memoized Positionen - nur bei Änderung der kuladigObjects neu erstellt
                kuladigObjects.forEach { obj ->
                    val position = markerPositions[obj.id] ?: return@forEach
                    
                    Marker(
                        state = remember(obj.id, position) { 
                            MarkerState(position = position) 
                        },
                        title = obj.name,
                        snippet = obj.beschreibung,
                        onClick = {
                            // Wenn eine Route von einem Marker gestartet wurde, verwende diesen Marker als Ziel
                            routeStartMarker?.let { startObj ->
                                if (startObj.id != obj.id) {
                                    val startLatLng = LatLng(startObj.latitude, startObj.longitude)
                                    routeTravelMode?.let { mode ->
                                        calculateRouteToDestination(startLatLng, obj, mode)
                                    }
                                    routeStartMarker = null
                                    routeTravelMode = null
                                } else {
                                    selectedObject = obj
                                    showBottomSheet = true
                                }
                            } ?: run {
                                selectedObject = obj
                                showBottomSheet = true
                            }
                            true
                        }
                    )
                }
                
                // Tour-Stopp-Marker mit Nummern
                currentTour?.let { (tour, stops) ->
                    stops.forEachIndexed { index, obj ->
                        val position = LatLng(obj.latitude, obj.longitude)
                        val isCurrentStop = index == currentTourStopIndex
                        
                        Marker(
                            state = remember(obj.id, position, isCurrentStop) {
                                MarkerState(position = position)
                            },
                            title = "${index + 1}. ${obj.name}",
                            snippet = if (isCurrentStop) "Aktueller Stopp" else obj.beschreibung,
                            alpha = if (isCurrentStop) 1f else 0.7f
                        )
                    }
                }
                
                // Route-Polyline anzeigen
                if (routePolylinePoints.isNotEmpty()) {
                    Polyline(
                        points = routePolylinePoints,
                        color = if (currentTour != null) Color.Green else Color.Blue,
                        width = 8f
                    )
                }
            }
            
            // Route-Info-Card anzeigen
            currentRoute?.let { route ->
                if (route.legs.isNotEmpty()) {
                    val leg = route.legs.first()
                    val totalDistance = route.legs.sumOf { it.distance.value }
                    val totalDuration = route.legs.sumOf { it.duration.value }
                    val distanceText = if (totalDistance < 1000) "${totalDistance}m" else "${totalDistance / 1000.0}km"
                    val durationText = "${totalDuration / 60} Min"
                    
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (currentTour != null) {
                                val (tour, stops) = currentTour!!
                                Text(
                                    text = tour.name,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Stopp ${currentTourStopIndex + 1} von ${stops.size}",
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (currentTourStopIndex > 0) {
                                        OutlinedButton(
                                            onClick = {
                                                currentTourStopIndex--
                                                // Berechne Route zum vorherigen Stopp
                                                currentTour?.let { (tour, stops) ->
                                                    if (directionsService != null && userLocation != null) {
                                                        val targetStops = stops.subList(0, currentTourStopIndex + 1)
                                                        calculateTourRoute(directionsService, userLocation!!, targetStops, tour.travelMode)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Vorheriger")
                                        }
                                    }
                                    if (currentTourStopIndex < stops.size - 1) {
                                        Button(
                                            onClick = {
                                                currentTourStopIndex++
                                                // Berechne Route zum nächsten Stopp
                                                currentTour?.let { (tour, stops) ->
                                                    if (directionsService != null && userLocation != null) {
                                                        val targetStops = stops.subList(0, currentTourStopIndex + 1)
                                                        calculateTourRoute(directionsService, userLocation!!, targetStops, tour.travelMode)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Nächster")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row {
                                Text(
                                    text = if (currentTour != null) {
                                        "Gesamt: $distanceText • $durationText"
                                    } else {
                                        "Route: ${leg.distance.text} • ${leg.duration.text}"
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    currentRoute = null
                                    routePolylinePoints = emptyList()
                                    routeError = null
                                    routeStartMarker = null
                                    routeTravelMode = null
                                    isNavigating = false
                                    currentBearing = null
                                    currentTour = null
                                    currentTourStopIndex = 0
                                }) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Route schließen"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Loading-Indikator
            if (isLoadingRoute) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Text(
                            text = "Route wird berechnet...",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Fehler anzeigen
            routeError?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("Fehler: $error")
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { routeError = null }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
            
            // Anzeige wenn auf Zielauswahl gewartet wird
            if (routeStartMarker != null) {
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Wählen Sie einen Ziel-Marker aus",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "Start: ${routeStartMarker?.name ?: ""}",
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            routeStartMarker = null
                            routeTravelMode = null
                        }) {
                            Text("Abbrechen")
                        }
                    }
                }
            }
            
            // Bottom Sheet für Marker-Details
            if (showBottomSheet) {
                MarkerInfoBottomSheet(
                    kuladigObject = selectedObject,
                    onDismiss = {
                        showBottomSheet = false
                        selectedObject = null
                    },
                    onRouteRequest = { obj, mode ->
                        showBottomSheet = false
                        val origin = userLocation ?: run {
                            routeError = "Aktuelle Position nicht verfügbar"
                            return@MarkerInfoBottomSheet
                        }
                        routeTravelMode = mode
                        calculateRouteToDestination(origin, obj, mode)
                    },
                    onRouteFromHere = { startObj, mode ->
                        showBottomSheet = false
                        routeStartMarker = startObj
                        routeTravelMode = mode
                        // Warte auf Zielauswahl
                    },
                    hasActiveRoute = currentRoute != null,
                    onAddToTour = { obj ->
                        showBottomSheet = false
                        // TODO: Navigate to Tour Management or show dialog
                        // For now, just show a message
                        routeError = "Zu Tour hinzufügen: ${obj.name} (Feature in Entwicklung)"
                    }
                )
            }
        }
    }
}

