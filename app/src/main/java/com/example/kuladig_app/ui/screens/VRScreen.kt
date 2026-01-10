package com.example.kuladig_app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

enum class VRTab(val title: String) {
    DESCRIPTION("Beschreibung"),
    VR("VR")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VRScreen(modifier: Modifier = Modifier) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            VRTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(tab.title) }
                )
            }
        }
        
        when (selectedTabIndex) {
            0 -> DescriptionTabContent()
            1 -> VRContent()
        }
    }
}

@Composable
fun DescriptionTabContent(modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Die Cäsar-Statue im Louvre zeigt Gaius Julius Caesar, den berühmten römischen Feldherrn und Staatsmann. Sie stammt aus der römischen Kaiserzeit (1. Jh. v. Chr./n. Chr.) und ist für ihren realistischen Stil bekannt: markante Gesichtszüge, hoher Haaransatz und ein ernsthafter Ausdruck. Die Statue betont Cäsars Macht, Autorität und politische Bedeutung und gilt als eines der bekanntesten Porträts des römischen Altertums."
        )
    }
}

@Composable
fun VRContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var modelInstance by remember { mutableStateOf<io.github.sceneview.model.ModelInstance?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            Log.d("VRScreen", "Loading model cesar.glb...")
            modelInstance = modelLoader.createModelInstance("cesar.glb")
            Log.d("VRScreen", "Model loaded: ${modelInstance != null}")
        } catch (e: Exception) {
            Log.e("VRScreen", "Error loading model", e)
        }
    }
    
    val cameraNode = rememberCameraNode(engine) {
        position = Float3(0f, 0f, 5f) // Weiter weg, um mehr zu sehen
    }
    
    // Kamera blickt auf das Zentrum
    LaunchedEffect(cameraNode) {
        cameraNode.lookAt(targetWorldPosition = Float3(0f, 0f, 0f), upDirection = Float3(0f, 1f, 0f))
    }
    
    val cameraManipulator = rememberCameraManipulator()
    
    // Hauptlicht für Beleuchtung - helleres Licht von oben
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 150_000.0f // Helleres Licht
        position = Float3(0f, 5f, 5f) // Von oben und vorne
    }
    
    // Licht blickt auf das Zentrum
    LaunchedEffect(mainLightNode) {
        mainLightNode.lookAt(targetWorldPosition = Float3(0f, 0f, 0f), upDirection = Float3(0f, 1f, 0f))
    }
    
    // Nodes werden neu erstellt, wenn modelInstance sich ändert
    val childNodes = rememberNodes {
        // Initial leer - wird durch LaunchedEffect gefüllt
    }
    
    // Füge das Modell hinzu, wenn es geladen ist
    LaunchedEffect(modelInstance) {
        childNodes.clear()
        modelInstance?.let { instance ->
            Log.d("VRScreen", "Adding ModelNode to scene")
            val modelNode = ModelNode(
                modelInstance = instance,
                scaleToUnits = 1.0f
            )
            // Modell zentrieren am Ursprung
            modelNode.position = Float3(0f, 0f, 0f)
            // Eventuell das Modell skalieren, falls es zu groß/klein ist
            // modelNode.scale = Float3(0.5f, 0.5f, 0.5f) // Falls nötig
            childNodes.add(modelNode)
            Log.d("VRScreen", "ModelNode added at position: ${modelNode.position}")
        } ?: run {
            Log.w("VRScreen", "Model instance is null, cannot add to scene")
        }
    }

    Scene(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader,
        cameraNode = cameraNode,
        cameraManipulator = cameraManipulator,
        mainLightNode = mainLightNode,
        childNodes = childNodes
    )
}