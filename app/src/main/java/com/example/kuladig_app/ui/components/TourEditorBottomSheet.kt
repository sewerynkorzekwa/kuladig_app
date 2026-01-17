package com.example.kuladig_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.TravelMode

data class TourStopItem(
    val kuladigObject: KuladigObject,
    val order: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourEditorBottomSheet(
    tourName: String = "",
    tourDescription: String = "",
    initialStops: List<KuladigObject> = emptyList(),
    initialTravelMode: TravelMode = TravelMode.WALKING,
    allObjects: List<KuladigObject> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (String, String, List<KuladigObject>, TravelMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var name by remember { mutableStateOf(tourName) }
    var description by remember { mutableStateOf(tourDescription) }
    var stops by remember { mutableStateOf(initialStops.mapIndexed { index, obj -> TourStopItem(obj, index) }) }
    var travelMode by remember { mutableStateOf(initialTravelMode) }
    
    var showObjectSelection by remember { mutableStateOf(false) }
    var showTravelModeDialog by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (tourName.isEmpty()) "Neue Tour" else "Tour bearbeiten",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Schließen")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tour-Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transportmodus:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(onClick = { showTravelModeDialog = true }) {
                    Text(when (travelMode) {
                        TravelMode.WALKING -> "Zu Fuß"
                        TravelMode.DRIVING -> "Auto"
                    })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stopps (${stops.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedButton(
                    onClick = { showObjectSelection = true },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hinzufügen")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (stops.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Keine Stopps hinzugefügt",
                        modifier = Modifier.padding(16.dp),
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = androidx.compose.ui.graphics.Color.Gray
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height((stops.size * 80).coerceAtMost(400).dp)
                ) {
                    itemsIndexed(stops) { index, stop ->
                        StopItemCard(
                            stop = stop,
                            index = index,
                            totalStops = stops.size,
                            onMoveUp = if (index > 0) {
                                {
                                    val newStops = stops.toMutableList()
                                    val temp = newStops[index]
                                    newStops[index] = newStops[index - 1].copy(order = index)
                                    newStops[index - 1] = temp.copy(order = index - 1)
                                    stops = newStops
                                }
                            } else null,
                            onMoveDown = if (index < stops.size - 1) {
                                {
                                    val newStops = stops.toMutableList()
                                    val temp = newStops[index]
                                    newStops[index] = newStops[index + 1].copy(order = index)
                                    newStops[index + 1] = temp.copy(order = index + 1)
                                    stops = newStops
                                }
                            } else null,
                            onDelete = {
                                stops = stops.filterIndexed { i, _ -> i != index }
                                    .mapIndexed { i, item -> item.copy(order = i) }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Abbrechen")
                }
                Button(
                    onClick = {
                        if (name.isNotBlank() && stops.isNotEmpty()) {
                            onSave(name, description, stops.map { it.kuladigObject }, travelMode)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && stops.isNotEmpty()
                ) {
                    Text("Speichern")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showObjectSelection) {
        ObjectSelectionDialog(
            allObjects = allObjects.filter { obj -> stops.none { it.kuladigObject.id == obj.id } },
            onDismiss = { showObjectSelection = false },
            onObjectSelected = { obj ->
                stops = stops + TourStopItem(obj, stops.size)
                showObjectSelection = false
            }
        )
    }

    if (showTravelModeDialog) {
        TravelModeDialog(
            onDismiss = { showTravelModeDialog = false },
            onModeSelected = { mode ->
                travelMode = mode
                showTravelModeDialog = false
            }
        )
    }
}

@Composable
fun StopItemCard(
    stop: TourStopItem,
    index: Int,
    totalStops: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}.",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stop.kuladigObject.name,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (stop.kuladigObject.beschreibung.isNotEmpty()) {
                        Text(
                            text = stop.kuladigObject.beschreibung,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Row {
                if (onMoveUp != null) {
                    IconButton(onClick = onMoveUp) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Nach oben")
                    }
                }
                if (onMoveDown != null) {
                    IconButton(onClick = onMoveDown) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Nach unten")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
        }
    }
}

@Composable
fun ObjectSelectionDialog(
    allObjects: List<KuladigObject>,
    onDismiss: () -> Unit,
    onObjectSelected: (KuladigObject) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Objekt hinzufügen") },
        text = {
            if (allObjects.isEmpty()) {
                Text("Keine weiteren Objekte verfügbar")
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(allObjects) { obj ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onObjectSelected(obj) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = obj.name,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (obj.beschreibung.isNotEmpty()) {
                                        Text(
                                            text = obj.beschreibung,
                                            fontSize = 12.sp,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
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
