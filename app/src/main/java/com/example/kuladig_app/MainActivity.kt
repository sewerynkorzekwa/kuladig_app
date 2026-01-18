package com.example.kuladig_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.kuladig_app.ui.theme.Kuladig_appTheme
import com.example.kuladig_app.ui.screens.HomeScreen
import com.example.kuladig_app.ui.screens.MapScreen
import com.example.kuladig_app.ui.screens.SearchScreen
import com.example.kuladig_app.ui.screens.SettingsScreen
import com.example.kuladig_app.ui.screens.TourManagementScreen
import com.example.kuladig_app.ui.screens.VRScreen
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.model.TravelMode
import com.example.kuladig_app.data.PreferencesManager
import com.example.kuladig_app.data.ThemeMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencesManager = PreferencesManager(this)
        setContent {
            var themeMode by remember { mutableStateOf<ThemeMode>(ThemeMode.SYSTEM) }
            
            LaunchedEffect(Unit) {
                themeMode = preferencesManager.getThemeMode()
            }
            
            Kuladig_appTheme(themeMode = themeMode) {
                Kuladig_appApp(
                    onThemeModeChanged = { mode ->
                        themeMode = mode
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun Kuladig_appApp(
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var routeRequest by rememberSaveable { mutableStateOf<Pair<KuladigObject, TravelMode>?>(null) }
    var tourRequest by rememberSaveable { mutableStateOf<Pair<Tour, List<KuladigObject>>?>(null) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            imageVector = it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (currentDestination != AppDestinations.VR && 
                    currentDestination != AppDestinations.HOME &&
                    currentDestination != AppDestinations.SETTINGS) {
                    TopAppBar(
                        title = { Text(currentDestination.label) },
                        actions = {
                            if (currentDestination == AppDestinations.KARTE) {
                                IconButton(onClick = { showSearchScreen = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            if (showSearchScreen) {
                SearchScreen(
                    modifier = Modifier.padding(innerPadding),
                    onBackClick = { showSearchScreen = false },
                    onRouteRequest = { obj, mode ->
                        showSearchScreen = false
                        currentDestination = AppDestinations.KARTE
                        routeRequest = Pair(obj, mode)
                    }
                )
            } else {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateToMap = { currentDestination = AppDestinations.KARTE },
                            onNavigateToVR = { currentDestination = AppDestinations.VR },
                            onNavigateToTours = { currentDestination = AppDestinations.PROFILE },
                            onNavigateToSearch = { showSearchScreen = true },
                            onRouteRequest = { obj, mode ->
                                routeRequest = Pair(obj, mode)
                                currentDestination = AppDestinations.KARTE
                            },
                            onTourStart = { tour, stops ->
                                tourRequest = Pair(tour, stops)
                                currentDestination = AppDestinations.KARTE
                            },
                            onVRObjectSelected = { vrObject ->
                                // TODO: Navigate to VR screen with selected object
                                currentDestination = AppDestinations.VR
                            },
                            activeTour = tourRequest
                        )
                    }
                    AppDestinations.KARTE -> {
                        MapScreen(
                            modifier = Modifier.padding(innerPadding),
                            initialRouteRequest = routeRequest,
                            onRouteRequestHandled = { routeRequest = null },
                            initialTour = tourRequest
                        )
                    }
                    AppDestinations.VR -> {
                        VRScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    AppDestinations.PROFILE -> {
                        TourManagementScreen(
                            modifier = Modifier.padding(innerPadding),
                            onTourStart = { tour, stops ->
                                tourRequest = Pair(tour, stops)
                                currentDestination = AppDestinations.KARTE
                            }
                        )
                    }
                    AppDestinations.SETTINGS -> {
                        SettingsScreen(
                            modifier = Modifier.padding(innerPadding),
                            onThemeModeChanged = onThemeModeChanged
                        )
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    KARTE("Map", Icons.Default.LocationOn),
    VR("AR", Icons.Default.ViewInAr),
    PROFILE("Tours", Icons.Default.AccountBox),
    SETTINGS("Settings", Icons.Default.Settings),
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Kuladig_appTheme(themeMode = ThemeMode.SYSTEM) {
        Greeting("Android")
    }
}