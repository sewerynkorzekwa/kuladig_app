---
name: AR-Implementierung mit ARCore
overview: Erweitert VRScreen um AR-Funktionalität mit ARCore Plane Detection - Nutzer können 3D-Modelle auf erkannten Oberflächen platzieren. AR wird als neuer Tab in VRDetailScreen integriert.
todos: []
---

# AR-Implementierung mit ARCore Plane Detection

## Übersicht

Erweiterung des bestehenden `VRScreen` um AR-Funktionalität mit ARCore. Nutzer können 3D-Modelle (`cesar.glb`, `munze.glb`) auf erkannten Oberflächen (Boden, Tisch) platzieren und interagieren. AR wird als neuer Tab neben "Beschreibung" und "VR" in `VRDetailScreen` hinzugefügt.

## Architektur

```
VRScreen (bestehend)
  └── VRDetailScreen
      └── TabRow: [Beschreibung | VR | AR]  ← Neuer AR-Tab
          └── ARContent (neu)
              └── ArSceneView / ArFragment
                  └── Plane Detection + 3D-Model-Platzierung
```

## Implementierungs-Schritte

### 1. ARCore Dependencies hinzufügen

**Datei:** `app/build.gradle.kts`

- ARCore Dependency hinzufügen: `implementation("com.google.ar:core:1.40.0")` oder neueste Version
- Optional: Sceneform AR Core (`sceneform.ux` oder `sceneview-ar`) falls SceneView AR-Unterstützung bietet
- Prüfen: Kompatibilität von `io.github.sceneview:sceneview:2.2.0` mit ARCore

**Wichtig:** `minSdk = 24` ist ausreichend für ARCore (benötigt mind. API 24)

### 2. AndroidManifest Permissions und Features

**Datei:** `app/src/main/AndroidManifest.xml`

- `CAMERA` Permission hinzufügen (erforderlich für ARCore)
- ARCore Feature Declaration: `<uses-feature android:name="android.hardware.camera.ar" android:required="false" />`
- Optional: AR Required Flag setzen, falls AR Pflichtfeature ist

### 3. VRTab Enum erweitern

**Datei:** `app/src/main/java/com/example/kuladig_app/ui/screens/VRScreen.kt`

- Neuer `AR` Eintrag in `VRTab` enum hinzufügen: `AR("AR")`
- Reihenfolge: `DESCRIPTION("Beschreibung"), VR("VR"), AR("AR")`

### 4. ARContent Composable erstellen

**Datei:** `app/src/main/java/com/example/kuladig_app/ui/screens/VRScreen.kt`

Neue Funktion `ARContent` implementieren:

- ARCore Session initialisieren und verwalten
- Plane Detection aktivieren (Horizontal/Vertical Planes)
- Kamera-View integrieren (ArFragment oder ArSceneView)
- 3D-Model-Loading aus Assets (`glbFileName`)
- Touch-Interaktion: Plane-Tapping zum Platzieren von Modellen
- Anker-Verwaltung: Modelle an erkannten Planes ankern

**Technische Details:**

- Verwendung von `ArFragment` (AndroidX Fragment) oder `ArSceneView` (Compose-kompatibel)
- Falls SceneView AR unterstützt: Integration mit bestehender `Scene`-Komponente
- Falls nicht: Native ARCore API mit `Session`, `Frame`, `Plane` Klassen
- Model-Rendering: GLB-Modelle in AR-Scene rendern (möglicherweise Konvertierung zu `.sfb` oder GLB-Support in ARCore prüfen)

### 5. VRDetailScreen erweitern

**Datei:** `app/src/main/java/com/example/kuladig_app/ui/screens/VRScreen.kt`

- `VRDetailScreen` Tab-Logik anpassen für 3 Tabs (aktuell: 2 Tabs)
- `when (selectedTabIndex)` um `AR`-Case erweitern: `2 -> ARContent(...)`
- `ARContent` mit `glbFileName` von `vrObject` aufrufen

### 6. AR-Session Lifecycle Management

**Datei:** `app/src/main/java/com/example/kuladig_app/ui/screens/VRScreen.kt`

- Session Lifecycle: Pause/Resume bei Tab-Wechsel
- Kamera-Permissions prüfen und anfordern
- ARCore Verfügbarkeit prüfen (device support, installation)

### 7. Model-Platzierung und Interaktion

- Plane-Detection-Overlay (optional): Visuelles Feedback für erkannte Ebenen
- Touch-Handler: Tap auf Plane → Modell platzieren
- Transform-Manipulation: Rotation/Scale per Gesten (optional für später)

## Technische Überlegungen

### ARCore vs. SceneView AR

**Option A: Native ARCore**

- Vollständige Kontrolle über Plane Detection
- Komplexere Integration in Compose (Fragment-basiert)
- Standard ARCore API

**Option B: SceneView AR Support**

- Prüfen ob `io.github.sceneview:sceneview:2.2.0` AR-Features bietet
- Einfachere Integration (falls vorhanden)

**Empfehlung:** Start mit Option A (Native ARCore), da SceneView primär für 3D-Viewer ist.

### GLB-Modell-Support in ARCore

ARCore unterstützt standardmäßig `.sfb` (Sceneform). Für GLB-Modelle:

- GLB zu SFB konvertieren, ODER
- GLB direkt mit ARCore Rendering Engine laden (falls unterstützt), ODER
- Sceneform Asset Definitions nutzen

## Dateien die geändert werden

1. `app/build.gradle.kts` - ARCore Dependency
2. `app/src/main/AndroidManifest.xml` - Camera Permission + AR Feature
3. `app/src/main/java/com/example/kuladig_app/ui/screens/VRScreen.kt` - AR Tab + ARContent Composable

## Erfolgskriterien

- [ ] AR-Tab erscheint in VRDetailScreen
- [ ] Kamera-View wird im AR-Tab angezeigt
- [ ] Planes werden erkannt und visualisiert (optional)
- [ ] 3D-Modelle können auf Planes platziert werden (Tap)
- [ ] Modelle bleiben verankert wenn Gerät bewegt wird
- [ ] Session-Lifecycle funktioniert korrekt (Pause/Resume)