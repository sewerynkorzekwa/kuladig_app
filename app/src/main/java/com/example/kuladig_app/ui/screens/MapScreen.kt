package com.example.kuladig_app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
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
import com.example.kuladig_app.ui.components.MarkerInfoBottomSheet
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as KuladigApplication
    val repository = remember { application.repository }
    val scope = rememberCoroutineScope()
    
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

    // Lade KuladigObjects aus der Datenbank
    LaunchedEffect(Unit) {
        scope.launch {
            kuladigObjects = repository.getAllObjects()
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
                
                // Marker für alle KuladigObjects
                kuladigObjects.forEach { obj ->
                    Marker(
                        state = MarkerState(position = LatLng(obj.latitude, obj.longitude)),
                        title = obj.name,
                        snippet = obj.beschreibung,
                        onClick = {
                            selectedObject = obj
                            showBottomSheet = true
                            true
                        }
                    )
                }
            }
            
            // Bottom Sheet für Marker-Details
            if (showBottomSheet) {
                MarkerInfoBottomSheet(
                    kuladigObject = selectedObject,
                    onDismiss = {
                        showBottomSheet = false
                        selectedObject = null
                    }
                )
            }
        }
    }
}

