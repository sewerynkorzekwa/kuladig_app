---
name: Anforderungsanalyse Kuladig App
overview: Analyse des aktuellen Projektstands zur Identifikation erfüllter und noch umzusetzender Anforderungen für die Kuladig-App
todos: []
---

# Anforderungsanalyse: Kuladig App

## Zusammenfassung

Die Analyse zeigt, dass bereits grundlegende Kartierungs- und Routenfunktionen implementiert sind, aber mehrere erweiterte Features noch fehlen.

## Erfüllte Anforderungen ✅

### 1. Integration eines Kartendienstes

**Status: ✅ Erfüllt**

- **Implementierung**: Google Maps ist vollständig integriert
- **Dependencies**: `play-services-maps` (19.0.0), `maps-compose` (6.1.2), `maps-compose-utils` (6.1.2)
- **Dateien**: 
- [app/build.gradle.kts](app/build.gradle.kts) - Dependencies definiert
- [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) - API-Key konfiguriert
- [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt) - GoogleMap Composable verwendet

### 2. Exakte Darstellung und Markierung der wichtigsten Sehenswürdigkeiten

**Status: ✅ Teilweise erfüllt**

- **Implementierung**: KuladigObjects werden als Marker auf der Karte angezeigt
- **Dateien**:
- [app/src/main/java/com/example/kuladig_app/data/model/KuladigObject.kt](app/src/main/java/com/example/kuladig_app/data/model/KuladigObject.kt) - Datenmodell
- [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:292-322) - Marker-Rendering
- [app/src/main/java/com/example/kuladig_app/ui/components/MarkerInfoBottomSheet.kt](app/src/main/java/com/example/kuladig_app/ui/components/MarkerInfoBottomSheet.kt) - Detailansicht

**Hinweis**: Marker werden angezeigt, aber "exakte Darstellung" könnte erweiterte Funktionen wie 3D-Marker, Custom-Icons oder Clustering erfordern.

### 3. Routenführung (Basis)

**Status: ✅ Teilweise erfüllt**

- **Implementierung**: Google Directions API wird für Routenberechnung verwendet
- **Dateien**:
- [app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt](app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt) - Service-Implementierung
- [app/src/main/java/com/example/kuladig_app/data/service/DirectionsApi.kt](app/src/main/java/com/example/kuladig_app/data/service/DirectionsApi.kt) - API-Interface
- [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:324-331) - Polyline-Darstellung

**Hinweis**: Routen werden berechnet und angezeigt, aber nicht mit Bézier-Splines (siehe unten).

## Nicht erfüllte Anforderungen ❌

### 1. GPS-gestützte Navigation mit Echtzeit-Positionsbestimmung

**Status: ❌ Nicht erfüllt**

**Aktueller Stand**:

- Nur einmalige Positionsabfrage via `fusedLocationClient.lastLocation` 
- Keine kontinuierliche Positionsverfolgung
- Keine Echtzeit-Navigation während der Bewegung

**Fehlende Implementierung**:

- `LocationCallback` für kontinuierliche Updates
- `LocationRequest` mit Update-Intervallen
- `requestLocationUpdates()` für Echtzeit-Tracking
- Navigation während aktiver Route

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:119-139)

### 2. Einbindung digitaler Höhenmodelle (DEM)

**Status: ❌ Nicht erfüllt**

**Fehlende Implementierung**:

- Keine DEM-Datenquelle (z.B. SRTM, ASTER GDEM)
- Keine Höhendaten-API-Integration
- Keine Berechnung von Höhenprofilen
- Keine Streckenprofile mit Höheninformationen

**Benötigte Komponenten**:

- DEM-Datenquelle oder API (z.B. Google Elevation API, OpenElevation)
- Service für Höhendatenabfrage
- UI-Komponente für Höhen-/Streckenprofil-Darstellung

### 3. Routenführung auf Basis von Bézier-Splines

**Status: ❌ Nicht erfüllt**

**Aktueller Stand**:

- Routen werden direkt von Google Directions API übernommen
- Polylines werden linear zwischen Wegpunkten gezeichnet

**Fehlende Implementierung**:

- Bézier-Spline-Berechnung für glatte Kurven
- Interpolation zwischen Route-Wegpunkten
- Anpassung der Polyline-Darstellung mit Splines

**Datei**: [app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt](app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt:76-78) - Aktuell nur PolyUtil.decode()

### 4. Definition und Verwaltung von Tour-Stopps

**Status: ❌ Nicht erfüllt**

**Aktueller Stand**:

- Nur Punkt-zu-Punkt-Routen (Start → Ziel)
- Keine Multi-Stopp-Routen

**Fehlende Implementierung**:

- Datenmodell für Tour-Stopps
- UI für Stopp-Verwaltung (hinzufügen, entfernen, sortieren)
- Multi-Waypoint-Routenberechnung
- Tour-Verwaltung (speichern, laden, teilen)

**Benötigte Komponenten**:

- `Tour` Datenmodell
- `TourStop` Datenmodell
- Tour-Management-Screen
- Erweiterte DirectionsService-Methode für Waypoints

### 5. Präzise Geolokalisierung mit hoher Positionsgenauigkeit

**Status: ❌ Teilweise erfüllt**

**Aktueller Stand**:

- Standard Google Play Services Location
- `ACCESS_FINE_LOCATION` Permission vorhanden
- Keine spezifische Präzisionssteigerung

**Fehlende Implementierung**:

- `LocationRequest` mit hoher Genauigkeit (`PRIORITY_HIGH_ACCURACY`)
- GPS + Netzwerk + Passiv-Lokalisierung kombinieren
- Filterung von ungenauen Positionen
- Kalibrierung/Validierung der Positionsdaten

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:81-83) - Aktuell nur FusedLocationProviderClient ohne spezifische Konfiguration

## Technische Details

### Bereits vorhandene Dependencies

- Google Maps SDK ✅
- Google Play Services Location ✅
- Room Database ✅
- Retrofit für API-Calls ✅

### Benötigte zusätzliche Dependencies

- Für DEM: Elevation API Client oder DEM-Datenbibliothek
- Für Bézier-Splines: Mathematische Bibliothek (z.B. Apache Commons Math) oder eigene Implementierung
- Für erweiterte Navigation: Möglicherweise Navigation SDK (optional)

## Empfohlene Umsetzungsreihenfolge

1. **Echtzeit-GPS-Navigation** (höchste Priorität für Navigation)
2. **Tour-Stopps** (wichtig für Benutzerfunktionalität)
3. **Präzise Geolokalisierung** (Verbesserung der Navigation)
4. **Bézier-Splines** (visuelle Verbesserung)
5. **DEM/Höhenmodelle** (erweiterte Features)