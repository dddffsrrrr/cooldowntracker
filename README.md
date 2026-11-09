# Cooldown Tracker (Fabric 1.21.11)

Zeigt über den Köpfen gegnerischer Spieler, wie lange ihre Cooldowns noch laufen:

| Cooldown | Standard | Erkannt durch |
|---|---|---|
| Wind Charge | 5 s | neu geworfene Wind Charge des Spielers |
| Enderperle | 12 s | neu geworfene Enderperle des Spielers |
| Speer | 10 s | Angriff mit Speer (einstellbar: Angriff / Aufladen / Beides) |
| Verz. goldener Apfel | 2 min | Spieler isst einen Apfel komplett auf |

## Einstellungen
* **Mod Menu → Cooldown Tracker → Konfigurieren**: jeden Cooldown einzeln an/aus, Dauer mit `-`/`+` ändern, Icons, Größe, Reichweite, „Nur in Sicht“.
* **Optionen → Steuerung → Tastenbelegung → Cooldown Tracker**:
  * `K` – ganze Anzeige an/aus
  * Wind Charge / Enderperle / Speer / Gapple einzeln an/aus (standardmäßig nicht belegt)
  * Einstellungen öffnen (nicht belegt)
* Config-Datei: `.minecraft/config/cooldowntracker.json`

## Bauen
Benötigt **Java 21** (JDK).

```
gradlew build        (Windows: gradlew.bat build)
```
Die fertige Mod liegt danach in `build/libs/cooldowntracker-1.0.0.jar`.

Alternativ: Ordner als GitHub-Repo hochladen – der Workflow unter `.github/workflows` baut die `.jar` automatisch (Tab „Actions“ → Artifact).

## Installieren
In `.minecraft/mods` legen: `cooldowntracker-1.0.0.jar`, **Fabric API** (1.21.11) und **Mod Menu 17.x**.
Nur clientseitig – der Server braucht nichts.

## Hinweis
Der Client bekommt die Cooldowns anderer Spieler nicht vom Server. Die Mod erkennt deshalb die Aktionen
(Wurf, Angriff, Essen) und rechnet die Zeit selbst. Stimmt die Dauer auf deinem Server nicht, im Menü anpassen.
