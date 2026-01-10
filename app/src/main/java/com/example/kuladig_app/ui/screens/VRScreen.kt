package com.example.kuladig_app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

@Composable
fun VRScreen(modifier: Modifier = Modifier) {
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