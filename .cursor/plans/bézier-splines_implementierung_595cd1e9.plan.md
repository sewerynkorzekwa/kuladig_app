---
name: Bézier-Splines Implementierung
overview: Implementierung von kubischen Bézier-Splines zur visuellen Glättung von Routen-Polylines für eine ansprechendere Darstellung der Navigation.
todos:
  - id: bezier_util
    content: Erstelle BezierSplineUtil.kt mit kubischer Bézier-Spline-Berechnung und smoothPolyline() Funktion
    status: pending
  - id: directions_service
    content: Füge smoothPolyline() Methode zu DirectionsService hinzu als Wrapper für BezierSplineUtil
    status: pending
    dependencies:
      - bezier_util
  - id: mapscreen_integration
    content: Integriere Glättung in MapScreen.kt an allen Stellen wo routePolylinePoints gesetzt wird (Zeilen 263, 325, 440)
    status: pending
    dependencies:
      - directions_service
  - id: testing
    content: Teste Implementation mit verschiedenen Routentypen (kurz/lang, Kurven/gerade, Tour-Routen)
    status: pending
    dependencies:
      - mapscreen_integration
---

# Bézier-Splines Implementierung

## Übersicht

Die Implementierung fügt kubische Bézier-Spline-Interpolation zur Routendarstellung hinzu, um die aktuell linearen Polylines zwischen Wegpunkten zu glätten. Dies verbessert die visuelle Qualität der Routen auf der Karte.

## Aktueller Stand

- Google Directions API liefert encoded polylines
- `PolyUtil.decode()` dekodiert diese zu `List<LatLng>` in [app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt](app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt:111-113)
- Polylines werden direkt aus den dekodierten Punkten gerendert in [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt:532-538)
- Mehrere Stellen erzeugen Routen: normale Route, Tour-Route, Route von Markern

## Implementierungsansatz

### 1. Bézier-Spline Utility-Klasse

**Neue Datei**: `app/src/main/java/com/example/kuladig_app/utils/BezierSplineUtil.kt`

- Implementiert kubische Bézier-Spline-Berechnung
- Methode: `smoothPolyline(points: List<LatLng>, segmentsPerCurve: Int = 10): List<LatLng>`
- Berechnet Kontrollpunkte automatisch basierend auf benachbarten Punkten
- Verwendet Catmull-Rom-Spline-Logik oder ähnliche Technik für natürliche Kurven

### 2. Integration in DirectionsService

**Datei**: [app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt](app/src/main/java/com/example/kuladig_app/data/service/DirectionsService.kt)

- Neue Methode: `smoothPolyline(points: List<LatLng>): List<LatLng>`
- Optional: Wrapper für `decodePolyline()` der automatisch glättet

### 3. Integration in MapScreen

**Datei**: [app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

- Alle Stellen, die `decodePolyline()` verwenden und `routePolylinePoints` setzen, müssen glätten:
  - Zeile 263: Route aus SearchScreen
  - Zeile 325: Tour-Route
  - Zeile 440: Route zu Destination

## Technische Details

### Bézier-Spline-Algorithmus

1. **Kontrollpunkt-Berechnung**: Für jeden Punkt werden Kontrollpunkte basierend auf vorherigen/nächsten Punkten berechnet
2. **Interpolation**: Zwischen jedem Punktpaar werden interpolierte Punkte mit kubischen Bézier-Kurven generiert
3. **Endpunkte**: Start- und Endpunkte bleiben unverändert
4. **Segmentanzahl**: Standard 10 Punkte pro Kurvensegment (konfigurierbar)

### Datenfluss

```
Google Directions API 
  → encoded polyline 
  → decodePolyline() → List<LatLng> 
  → smoothPolyline() → List<LatLng> (glatt) 
  → Polyline Rendering
```

## Dependencies

Keine zusätzlichen Dependencies erforderlich. Alle mathematischen Berechnungen werden in reiner Kotlin implementiert.

## Implementierungsschritte

1. Erstelle `BezierSplineUtil.kt` mit kubischer Bézier-Spline-Logik
2. Füge `smoothPolyline()` Methode zu `DirectionsService` hinzu
3. Aktualisiere alle `routePolylinePoints` Zuweisungen in `MapScreen.kt` zur Verwendung der Glättung
4. Teste mit verschiedenen Routentypen (kurze/lange Routen, viele/few Wegpunkte)

## Testfälle

- Kurze Routen (< 5 Wegpunkte)
- Lange Routen (> 50 Wegpunkte)
- Route mit vielen Kurven
- Route mit geraden Abschnitten
- Tour-Routen mit mehreren Legs

## Performance-Betrachtungen

- Glättung sollte nur einmal pro Route erfolgen (nicht bei jedem Render)
- Bei sehr langen Routen (> 1000 Punkte) könnte Segmentanzahl reduziert werden
- Caching der geglätteten Punkte (bereits implementiert durch `routePolylinePoints` state)