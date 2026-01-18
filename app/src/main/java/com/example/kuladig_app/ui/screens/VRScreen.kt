package com.example.kuladig_app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.example.kuladig_app.ui.ar.ARRenderer
import com.example.kuladig_app.data.model.VRObject
import com.example.kuladig_app.ui.components.AudioPlayer
import com.example.kuladig_app.ui.components.VideoPlayer
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
    VR("VR"),
    AR("AR")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VRScreen(modifier: Modifier = Modifier) {
    var selectedObject by rememberSaveable { mutableStateOf<VRObject?>(null) }
    
    if (selectedObject == null) {
        VRListScreen(
            modifier = modifier,
            onObjectSelected = { obj -> selectedObject = obj }
        )
    } else {
        VRDetailScreen(
            modifier = modifier,
            vrObject = selectedObject!!,
            onBackClick = { selectedObject = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VRListScreen(
    modifier: Modifier = Modifier,
    onObjectSelected: (VRObject) -> Unit
) {
    val allObjects = remember { VRObject.getAllObjects() }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredObjects = remember(allObjects, searchQuery) {
        if (searchQuery.isBlank()) {
            allObjects
        } else {
            allObjects.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("VR") }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Suchen...") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Suchen"
                    )
                }
            )
            
            if (filteredObjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "Keine Objekte gefunden" else "Keine Ergebnisse für \"$searchQuery\""
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 16.dp)
                ) {
                    items(filteredObjects) { obj ->
                        VRObjectListItem(
                            vrObject = obj,
                            onClick = { onObjectSelected(obj) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VRObjectListItem(
    vrObject: VRObject,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = vrObject.title,
                fontWeight = FontWeight.Bold
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.ViewInAr,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VRDetailScreen(
    modifier: Modifier = Modifier,
    vrObject: VRObject,
    onBackClick: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(vrObject.title) },
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
                0 -> DescriptionTabContent(
                    description = vrObject.description,
                    audioFileName = vrObject.audioFileName,
                    videoFileName = vrObject.videoFileName,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> VRContent(
                    glbFileName = vrObject.glbFileName,
                    modifier = Modifier.fillMaxSize()
                )
                2 -> ARContent(
                    glbFileName = vrObject.glbFileName,
                    vrObject = vrObject,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DescriptionTabContent(
    description: String,
    audioFileName: String? = null,
    videoFileName: String? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(text = description, modifier = Modifier.padding(bottom = 16.dp))
        
        if (audioFileName != null) {
            Text(
                text = "Audio:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            AudioPlayer(
                audioFileName = audioFileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp)
            )
        }
        
        if (videoFileName != null) {
            Text(
                text = "Video:",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            VideoPlayer(
                videoFileName = videoFileName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}

@Composable
fun VRContent(
    glbFileName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var modelInstance by remember { mutableStateOf<io.github.sceneview.model.ModelInstance?>(null) }
    
    LaunchedEffect(glbFileName) {
        try {
            Log.d("VRScreen", "Loading model $glbFileName...")
            modelInstance = modelLoader.createModelInstance(glbFileName)
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

@Composable
fun ARContent(
    glbFileName: String,
    vrObject: VRObject? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }
    var arAvailable by remember { mutableStateOf<Boolean?>(null) }
    var arSession by remember { mutableStateOf<Session?>(null) }
    var arRenderer by remember { mutableStateOf<ARRenderer?>(null) }
    var selectedAnchor by remember { mutableStateOf<Anchor?>(null) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    
    // Prüfe ARCore Verfügbarkeit
    LaunchedEffect(context) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            arAvailable = when {
                availability.isSupported -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e("ARContent", "Error checking ARCore availability", e)
            arAvailable = false
        }
    }
    
    // Fordere Kamera-Permission an falls nicht vorhanden
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    // Initialisiere AR Session wenn Permission vorhanden
    LaunchedEffect(hasCameraPermission, arAvailable) {
        if (hasCameraPermission && arAvailable == true && arSession == null) {
            try {
                val session = Session(context)
                val config = Config(session)
                config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                
                // Enable Geospatial API
                config.geospatialMode = Config.GeospatialMode.ENABLED
                
                session.configure(config)
                arSession = session
                
                // Erstelle AR Renderer
                val renderer = ARRenderer(context, glbFileName) { anchor ->
                    Log.d("ARContent", "Anchor created: ${anchor.trackingState}")
                    selectedAnchor = anchor // Markiere Anchor als ausgewählt für Info-Overlay
                }
                renderer.setSession(session)
                arRenderer = renderer
                
                Log.d("ARContent", "AR Session initialized with Geospatial API")
            } catch (e: Exception) {
                Log.e("ARContent", "Error initializing AR Session", e)
            }
        }
    }
    
    // Erstelle Geospatial-Anker wenn Earth-Tracking verfügbar ist
    LaunchedEffect(arSession, vrObject, arRenderer) {
        if (arSession != null && arRenderer != null && vrObject != null) {
            val obj = vrObject
            if (obj.latitude != null && obj.longitude != null) {
                // Warte auf Earth Tracking (max. 10 Sekunden)
                var attempts = 0
                while (attempts < 100 && arSession != null && arRenderer != null) {
                    val earth = arSession?.earth
                    if (earth?.trackingState == com.google.ar.core.TrackingState.TRACKING) {
                        arRenderer?.createGeospatialAnchor(obj.latitude, obj.longitude)
                        Log.d("ARContent", "Geospatial anchor created for ${obj.title} at (${obj.latitude}, ${obj.longitude})")
                        break
                    }
                    kotlinx.coroutines.delay(100) // Warte 100ms zwischen Versuchen
                    attempts++
                }
            }
        }
    }
    
    // Cleanup AR Session
    DisposableEffect(arSession) {
        onDispose {
            arRenderer?.clearAnchors()
            arSession?.close()
            arSession = null
            arRenderer = null
        }
    }
    
    if (!hasCameraPermission) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Kamera-Berechtigung erforderlich für AR")
        }
    } else if (arAvailable == false) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ARCore nicht verfügbar")
                Text("Bitte installieren Sie ARCore vom Play Store", modifier = Modifier.padding(top = 8.dp))
            }
        }
    } else if (arAvailable == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Prüfe ARCore Verfügbarkeit...")
        }
    } else {
        // AR View mit vollständiger Implementierung und Info-Overlay
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(2)
                    preserveEGLContextOnPause = true
                    
                    arRenderer?.let { renderer ->
                        setRenderer(renderer)
                        renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        
                        // Touch-Handler für Anchor-Platzierung
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                val renderer = arRenderer
                                if (renderer != null) {
                                    val x = event.x
                                    val y = event.y
                                    renderer.handleTap(x, y)
                                    true
                                } else {
                                    false
                                }
                            } else {
                                false
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                // Update renderer session if needed
                arRenderer?.let { renderer ->
                    if (view.renderer == null) {
                        view.setRenderer(renderer)
                        renderer.setSession(arSession)
                    }
                }
            }
            )
            
            // Info-Overlay für ausgewähltes AR-Objekt
            if (selectedAnchor != null && vrObject != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = vrObject.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = vrObject.description.take(150) + if (vrObject.description.length > 150) "..." else "",
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        androidx.compose.material3.Button(
                            onClick = { selectedAnchor = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Schließen")
                        }
                    }
                }
            }
        }
    }
}