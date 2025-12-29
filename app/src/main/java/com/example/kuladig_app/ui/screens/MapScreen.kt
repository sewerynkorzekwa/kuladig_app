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
import com.example.kuladig_app.data.service.DirectionsService
import com.example.kuladig_app.ui.components.MarkerInfoBottomSheet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
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
    onRouteRequestHandled: () -> Unit = {}
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
    
    val coroutineScope = rememberCoroutineScope()
    

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            } catch (e: SecurityException) {
                // Permission wurde widerrufen
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

    // Memoize Marker-Positionen um unnötige Recompositionen zu vermeiden
    val markerPositions = remember(kuladigObjects) {
        kuladigObjects.associate { obj ->
            obj.id to LatLng(obj.latitude, obj.longitude)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (!hasLocationPermission) {
            Text(
                text = "Standortberechtigung wird angefordert...",
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val cameraPositionState = rememberCameraPositionState {
                position = userLocation?.let {
                    CameraPosition.fromLatLngZoom(it, 15f)
                } ?: CameraPosition.fromLatLngZoom(
                    LatLng(52.5200, 13.4050), // Berlin als Fallback
                    10f
                )
            }

            LaunchedEffect(userLocation) {
                userLocation?.let {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(it, 15f)
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
                
                // Route-Polyline anzeigen
                if (routePolylinePoints.isNotEmpty()) {
                    Polyline(
                        points = routePolylinePoints,
                        color = Color.Blue,
                        width = 8f
                    )
                }
            }
            
            // Route-Info-Card anzeigen
            currentRoute?.let { route ->
                if (route.legs.isNotEmpty()) {
                    val leg = route.legs.first()
                    Card(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row {
                                Text(
                                    text = "Route: ${leg.distance.text} • ${leg.duration.text}",
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = {
                                    currentRoute = null
                                    routePolylinePoints = emptyList()
                                    routeError = null
                                    routeStartMarker = null
                                    routeTravelMode = null
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
                    hasActiveRoute = currentRoute != null
                )
            }
        }
    }
}

