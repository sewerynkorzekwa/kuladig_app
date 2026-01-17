---
name: Echtzeit-GPS-Navigation
overview: Implementierung einer kontinuierlichen GPS-Positionsverfolgung mit Echtzeit-Navigation während aktiver Routenführung, einschließlich LocationRequest, LocationCallback und automatischer Kamera-Nachführung.
todos:
  - id: location-request
    content: LocationRequest mit hoher Genauigkeit und Update-Intervallen erstellen
    status: completed
  - id: location-callback
    content: LocationCallback implementieren für kontinuierliche Positionsupdates
    status: completed
    dependencies:
      - location-request
  - id: lifecycle-management
    content: Lifecycle-Management für Location-Updates (starten/stoppen) implementieren
    status: completed
    dependencies:
      - location-callback
  - id: navigation-mode
    content: Navigation-Modus mit Kamera-Nachführung während aktiver Route implementieren
    status: completed
    dependencies:
      - location-callback
  - id: bearing-calculation
    content: Bewegungsrichtung (Bearing) berechnen und für Navigation-Ansicht verwenden
    status: completed
    dependencies:
      - navigation-mode
  - id: error-handling
    content: Fehlerbehandlung und Position-Filterung (Genauigkeit) implementieren
    status: completed
    dependencies:
      - location-callback
---

# Echtzeit-GPS-Navigation für Kuladig App

## Übersicht

Die App verwendet aktuell nur eine einmalige Positionsabfrage (`lastLocation`). Dieser Plan implementiert eine kontinuierliche GPS-Positionsverfolgung mit Echtzeit-Navigation während aktiver Routenführung.

## Aktueller Stand

- **Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:119-139)
- Nur einmalige Positionsabfrage via `fusedLocationClient.lastLocation`
- Keine kontinuierliche Positionsverfolgung
- Keine Navigation während aktiver Route
- Kamera bewegt sich nur einmalig zur Startposition

## Implementierungsplan

### 1. LocationRequest-Konfiguration

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Erstelle `LocationRequest` mit hoher Genauigkeit (`PRIORITY_HIGH_ACCURACY`)
- Konfiguriere Update-Intervalle:
  - `intervalMillis`: 2000ms (2 Sekunden) für Navigation
  - `fastestIntervalMillis`: 1000ms (1 Sekunde) für schnelle Updates
  - `maxWaitTime`: 5000ms (5 Sekunden) für Batch-Updates
- Setze `smallestDisplacement`: 5 Meter (optional, reduziert Updates bei Stillstand)

### 2. LocationCallback-Implementierung

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Erstelle `LocationCallback` als `remember`-Variable
- Im Callback:
  - Aktualisiere `userLocation` State mit neuer Position
  - Prüfe, ob eine Route aktiv ist (`currentRoute != null`)
  - Bei aktiver Route: Aktiviere Navigation-Modus (Kamera folgt Position)

### 3. Lifecycle-Management für Location-Updates

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Starte Updates mit `requestLocationUpdates()` wenn:
  - Location-Berechtigung vorhanden ist
  - Screen sichtbar ist
- Stoppe Updates mit `removeLocationUpdates()` wenn:
  - Screen nicht mehr sichtbar ist (DisposableEffect)
  - App in Hintergrund geht
- Verwende `DisposableEffect` für Cleanup bei Composable-Zerstörung

### 4. Navigation-Modus während aktiver Route

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Füge State-Variable `isNavigating` hinzu (aktiv wenn Route vorhanden)
- Bei neuer Position während Navigation:
  - Aktualisiere User-Location-Marker
  - Bewege Kamera zur neuen Position mit `CameraUpdateFactory.newCameraPosition()`
  - Verwende `CameraPosition` mit Bearing (Richtung) basierend auf Bewegungsrichtung
  - Optional: Tilt-Winkel für bessere Navigation-Ansicht
- Deaktiviere Navigation-Modus wenn Route gelöscht wird

### 5. Bewegungsrichtung berechnen

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Speichere vorherige Position (`previousLocation`)
- Berechne Bearing zwischen vorheriger und aktueller Position
- Verwende `Location.bearingTo()` oder manuelle Berechnung
- Setze Kamera-Bearing für Navigation-Ansicht

### 6. Optimierungen und Fehlerbehandlung

**Dateien**:

- [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)
- [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)

- Prüfe `Location.accuracy` und filtere ungenaue Positionen (< 20 Meter)
- Behandle `SecurityException` beim Starten der Updates
- Prüfe GPS-Status (`LocationSettingsRequest` optional)
- Für Android 10+: Prüfe `ACCESS_BACKGROUND_LOCATION` wenn nötig

## Technische Details

### Benötigte Imports

```kotlin
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.runtime.DisposableEffect
import android.location.Location
```

### LocationRequest-Konfiguration

```kotlin
val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
    .setMinUpdateIntervalMillis(1000)
    .setMaxUpdateDelayMillis(5000)
    .setSmallestDisplacement(5f) // Optional
    .build()
```

### Lifecycle-Integration

- Verwende `DisposableEffect` mit `LifecycleOwner` für automatisches Cleanup
- Oder manuelles Cleanup in `onDispose` des `DisposableEffect`

## Abhängigkeiten

- ✅ `play-services-location` bereits vorhanden
- Keine zusätzlichen Dependencies erforderlich

## Erweiterte Features (Optional, nicht im Scope)

- Abweichung von Route erkennen und neu berechnen
- Sprachansagen für Navigation
- Nächste Abbiegung anzeigen
- Distanz zum Ziel anzeigen