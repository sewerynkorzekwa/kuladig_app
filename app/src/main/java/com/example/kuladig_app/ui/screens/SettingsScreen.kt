package com.example.kuladig_app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kuladig_app.data.PreferencesManager
import com.example.kuladig_app.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onThemeModeChanged: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    
    var currentThemeMode by remember { mutableStateOf<ThemeMode>(ThemeMode.SYSTEM) }

    // Lade die aktuelle Theme-Präferenz
    LaunchedEffect(Unit) {
        currentThemeMode = preferencesManager.getThemeMode()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Erscheinungsbild Sektion
            AppearanceSection(
                currentThemeMode = currentThemeMode,
                onThemeModeSelected = { mode ->
                    currentThemeMode = mode
                    preferencesManager.setThemeMode(mode)
                    onThemeModeChanged(mode)
                }
            )
        }
    }
}

@Composable
fun AppearanceSection(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Erscheinungsbild",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            
            // Light Mode Option
            ThemeModeOption(
                title = "Hell",
                description = "Immer helles Design verwenden",
                icon = Icons.Default.LightMode,
                selected = currentThemeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeSelected(ThemeMode.LIGHT) }
            )
            
            // Dark Mode Option
            ThemeModeOption(
                title = "Dunkel",
                description = "Immer dunkles Design verwenden",
                icon = Icons.Default.DarkMode,
                selected = currentThemeMode == ThemeMode.DARK,
                onClick = { onThemeModeSelected(ThemeMode.DARK) }
            )
            
            // System Mode Option
            ThemeModeOption(
                title = "System-Standard",
                description = "Systemeinstellung verwenden",
                icon = Icons.Default.SettingsBrightness,
                selected = currentThemeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeSelected(ThemeMode.SYSTEM) }
            )
        }
    }
}

@Composable
fun ThemeModeOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 12.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            Text(
                text = description,
                fontSize = 14.sp
            )
        }
    }
}
