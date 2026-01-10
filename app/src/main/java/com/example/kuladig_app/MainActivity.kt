package com.example.kuladig_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
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
import com.example.kuladig_app.ui.screens.MapScreen
import com.example.kuladig_app.ui.screens.SearchScreen
import com.example.kuladig_app.ui.screens.VRScreen
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.TravelMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Kuladig_appTheme {
                Kuladig_appApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewScreenSizes
@Composable
fun Kuladig_appApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.KARTE) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var routeRequest by rememberSaveable { mutableStateOf<Pair<KuladigObject, TravelMode>?>(null) }

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
                TopAppBar(
                    title = { Text(currentDestination.label) },
                    actions = {
                        if (currentDestination == AppDestinations.KARTE) {
                            IconButton(onClick = { showSearchScreen = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Suchen"
                                )
                            }
                        }
                    }
                )
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
                    AppDestinations.KARTE -> {
                        MapScreen(
                            modifier = Modifier.padding(innerPadding),
                            initialRouteRequest = routeRequest,
                            onRouteRequestHandled = { routeRequest = null }
                        )
                    }
                    AppDestinations.FAVORITES -> {
                        VRScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    AppDestinations.PROFILE -> {
                        Greeting(
                            name = "Profile",
                            modifier = Modifier.padding(innerPadding)
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
    KARTE("Karten", Icons.Default.LocationOn),
    FAVORITES("VR", Icons.Default.ViewInAr),
    PROFILE("Profile", Icons.Default.AccountBox),
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
    Kuladig_appTheme {
        Greeting("Android")
    }
}