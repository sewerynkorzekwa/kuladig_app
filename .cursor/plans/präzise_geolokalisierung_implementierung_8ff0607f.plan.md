---
name: Präzise Geolokalisierung Implementierung
overview: Implementierung erweiterter Geolokalisierungs-Features zur Steigerung der Positionsgenauigkeit durch intelligente Filterung, Validierung und adaptive Genauigkeitsanforderungen je nach Navigationskontext.
todos:
  - id: create-location-validation-model
    content: Datenmodell LocationValidation.kt erstellen mit ValidationResult, SignalQuality und LocationMetadata Data Classes
    status: pending
  - id: create-location-validator-service
    content: LocationValidator Service-Klasse implementieren mit Multi-Kriterien-Filterung, Outlier-Erkennung und Signalqualitätsbewertung
    status: pending
    dependencies:
      - create-location-validation-model
  - id: extend-location-request-config
    content: Erweiterte LocationRequest-Konfiguration in MapScreen mit adaptiven Profilen (Navigation/Normal/Batterie) und setWaitForAccurateLocation
    status: pending
  - id: integrate-validator-in-callback
    content: LocationValidator in LocationCallback von MapScreen integrieren mit adaptiver Genauigkeitsfilterung und erweiterten Validierungschecks
    status: pending
    dependencies:
      - create-location-validator-service
  - id: add-location-quality-ui
    content: "Optional: UI-Komponente für Positionsqualitäts-Feedback in MapScreen hinzufügen (Signalqualitäts-Icon/Status)"
    status: pending
    dependencies:
      - integrate-validator-in-callback
---

# Präzise Geolokalisierung: Implementierungsplan

## Überblick

Die aktuelle Implementierung hat bereits grundlegende Features wie `PRIORITY_HIGH_ACCURACY`, kontinuierliche Location-Updates und einfache Genauigkeitsfilterung. Dieser Plan erweitert die Geolokalisierung um intelligente Filterung, Validierung und adaptive Genauigkeitsanforderungen.

## Aktueller Stand

In [`app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt`](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt) ist bereits implementiert:

- `LocationRequest` mit `PRIORITY_HIGH_ACCURACY` (Zeile 135-140)
- `LocationCallback` für kontinuierliche Updates (Zeile 144-187)
- Basis-Genauigkeitsfilterung (20m Schwellwert, Zeile 151)
- Bearing-Berechnung zwischen Updates (Zeile 159-161)

## Geplante Verbesserungen

### 1. LocationValidator Service-Klasse

**Datei**: `app/src/main/java/com/example/kuladig_app/data/service/LocationValidator.kt` (neu)

Erstellt einen zentralen Location-Validierungs-Service mit:

- Multi-Kriterien-Filterung (Genauigkeit, Geschwindigkeit, Outlier-Erkennung)
- Bearing-Konsistenz-Check
- Distanz-Validierung zwischen aufeinanderfolgenden Positionen
- Zeitbasierte Filterung (zu alte Positionen ignorieren)
- Signalqualitätsbewertung

**Methoden**:

- `validateLocation(location: Location, previousLocation: Location?): ValidationResult`
- `isOutlier(location: Location, previousLocation: Location?): Boolean`
- `calculateSignalQuality(location: Location): SignalQuality`

### 2. Erweiterte LocationRequest-Konfiguration

**Datei**: [`app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt`](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

**Änderungen**:

- Adaptive `LocationRequest`-Konfiguration je nach Kontext (Navigation vs. normal)
- Dynamische Update-Intervalle basierend auf Bewegungsgeschwindigkeit
- Zusätzliche Parameter für bessere Genauigkeit:
- `setWaitForAccurateLocation(true)` für initiale Position
- `setMaxUpdateDelayMillis()` Optimierung
- Konfigurierbare Genauigkeitsanforderungen

### 3. Intelligente Location-Filterung im LocationCallback

**Datei**: [`app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt`](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

**Erweiterungen**:

- Integration des `LocationValidator` in den `LocationCallback`
- Adaptive Genauigkeitsfilterung (strenger während Navigation)
- Geschwindigkeitsbasierte Filterung (Spring-Erkennung)
- Bearing-Konsistenz-Check für realistische Bewegungsrichtungen
- Historische Positionsvalidierung (vergleicht mit letzten N Positionen)

### 4. Datenmodell für Validierungsergebnisse

**Datei**: `app/src/main/java/com/example/kuladig_app/data/model/LocationValidation.kt` (neu)

Definiert Data Classes für:

- `ValidationResult`: Ergebnis der Validierung mit Status und Metadaten
- `SignalQuality`: Signalqualitätsbewertung (EXCELLENT, GOOD, FAIR, POOR)
- `LocationMetadata`: Erweiterte Metadaten zu einer Position (Genauigkeit, Quelle, Zeitstempel)

### 5. Adaptive Genauigkeitsanforderungen

**Datei**: [`app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt`](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

**Feature**:

- Verschiedene Genauigkeitsprofile:
- **Navigation-Modus**: Sehr hohe Genauigkeit (< 10m), häufige Updates
- **Normale Nutzung**: Moderate Genauigkeit (< 20m), weniger häufige Updates
- **Batterie-Sparmodus**: Niedrigere Genauigkeit (< 50m), seltene Updates
- Automatisches Wechseln zwischen Profilen basierend auf Kontext (`isNavigating` State)

### 6. Positionsqualitäts-Anzeige (Optional)

**Datei**: [`app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt`](app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt)

**UI-Komponente**:

- Kleines Status-Icon/Widget, das die aktuelle Signalqualität anzeigt
- Visuelles Feedback für Nutzer über Positionsgenauigkeit
- Warnung bei schlechter Signalqualität

## Technische Details

### Validierungsalgorithmen

1. **Outlier-Erkennung**:

- Berechnet erwartete Position basierend auf vorheriger Position, Geschwindigkeit und Bearing
- Filtert Positionen, die mehr als X Meter von erwarteter Position abweichen

2. **Geschwindigkeitsbasierte Filterung**:

- Erkennt unrealistische Geschwindigkeiten (> 200 km/h)
- Ignoriert Positionen bei unmöglichen Bewegungsmustern

3. **Bearing-Konsistenz**:

- Vergleicht berechnetes Bearing mit GPS-Bearing (wenn verfügbar)
- Erkennt Sprünge in der Bewegungsrichtung

4. **Zeitbasierte Filterung**:

- Ignoriert Positionen, die älter als X Sekunden sind
- Priorisiert aktuellste Positionen

### Konfigurationsparameter

- **Navigation-Genauigkeit**: 10 Meter
- **Normale Genauigkeit**: 20 Meter
- **Batterie-Spar-Genauigkeit**: 50 Meter
- **Max. Outlier-Abweichung**: 100 Meter
- **Max. Geschwindigkeit**: 200 km/h
- **Max. Positionsalter**: 5 Sekunden

## Abhängigkeiten

Keine zusätzlichen Dependencies erforderlich - nutzt bereits vorhandene:

- `play-services-location` (bereits vorhanden, Version 21.3.0)
- Standard Android Location APIs

## Umsetzungsreihenfolge

1. **Datenmodell** (`LocationValidation.kt`) - Grundlage für Validierung
2. **LocationValidator Service** - Kern-Logik der Validierung
3. **Erweiterte LocationRequest-Konfiguration** - Adaptive Requests
4. **Integration in LocationCallback** - Filterung in MapScreen
5. **UI-Feedback** (Optional) - Positionsqualitäts-Anzeige

## Dateien

**Neu zu erstellen**:

- `app/src/main/java/com/example/kuladig_app/data/service/LocationValidator.kt`
- `app/src/main/java/com/example/kuladig_app/data/model/LocationValidation.kt`

**Zu ändern**:

- `app/src/main/java/com/example/kuladig_app/ui/screens/MapScreen.kt` (Zeilen 134-187, erweiterte Location-Logik)

## Erwartete Verbesserungen

- **Genauigkeitssteigerung**: Reduzierung von Fehlpositionierungen durch intelligente Filterung
- **Konsistenz**: Glattere Bewegungsverfolgung ohne Spring-Erkennungen
- **Adaptivität**: Optimale Balance zwischen Genauigkeit und Batterieverbrauch je nach Kontext
- **Zuverlässigkeit**: Bessere Positionsqualität auch bei schwierigen GPS-Bedingungen