package com.example.kuladig_app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.TravelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerInfoBottomSheet(
    kuladigObject: KuladigObject?,
    onDismiss: () -> Unit,
    onRouteRequest: (KuladigObject, TravelMode) -> Unit = { _, _ -> },
    onRouteFromHere: (KuladigObject, TravelMode) -> Unit = { _, _ -> },
    hasActiveRoute: Boolean = false,
    onAddToTour: (KuladigObject) -> Unit = { },
    modifier: Modifier = Modifier
) {
    if (kuladigObject == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTravelModeDialog by remember { mutableStateOf(false) }
    var showRouteFromHereDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = kuladigObject.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (kuladigObject.objekttyp.isNotEmpty()) {
                Text(
                    text = "Objekttyp:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = kuladigObject.objekttyp,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (kuladigObject.beschreibung.isNotEmpty()) {
                Text(
                    text = "Beschreibung:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = kuladigObject.beschreibung,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Text(
                text = "Koordinaten:",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Lat: ${kuladigObject.latitude}, Lng: ${kuladigObject.longitude}",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { onAddToTour(kuladigObject) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("Zu Tour hinzufügen")
            }

            if (hasActiveRoute) {
                OutlinedButton(
                    onClick = { showRouteFromHereDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Von hier starten")
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { showTravelModeDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (hasActiveRoute) "Neue Route" else "Route anzeigen")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text("Schließen")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    if (showTravelModeDialog) {
        TravelModeDialog(
            onDismiss = { showTravelModeDialog = false },
            onModeSelected = { mode ->
                showTravelModeDialog = false
                onRouteRequest(kuladigObject, mode)
            }
        )
    }
    
    if (showRouteFromHereDialog) {
        TravelModeDialog(
            onDismiss = { showRouteFromHereDialog = false },
            onModeSelected = { mode ->
                showRouteFromHereDialog = false
                onRouteFromHere(kuladigObject, mode)
            }
        )
    }
}

@Composable
fun TravelModeDialog(
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

