# Änderungen

## 1.0.4 – 18.10.2026

- EndUpdate prüft beim Start und alle 48 Stunden auf neue stabile GitHub-Releases und lädt Updates in Papers konfigurierten Plugin-Updateordner. Paper installiert die JAR beim nächsten Serverstart.
- Automatische Downloads lassen sich mit `update-checker.auto-install: false` deaktivieren; `update-checker.enabled: false` schaltet die Prüfung aus.

## 1.0.3 – 13.10.2026

- End-Falltür: Ungültigen Vanilla-Modellverweis durch eigene Modelle für unten, oben und geöffnet ersetzt.
- End-Tür: Darstellung an die tatsächliche Blockkante versetzt; eigene Modelle für beide Hälften, beide Anschlagseiten und den geöffneten Zustand mit Vanilla-UVs.
- Öffnen per Rechtsklick (auch mit leerer Hand), Redstone und Blockphysik aktualisiert die Displays im nächsten Tick. Änderungen durch andere Plugins werden zusätzlich regelmäßig abgeglichen.
- Leichter Modellüberstand verhindert koplanare Flächen am Trägerblock. Bestehende End-Türen und Falltüren verwenden nach dem Laden automatisch die neuen Modelle. Keine Vanilla-Texturen werden ersetzt.

Installation: Alte EndUpdate-JAR durch `EndUpdate-1.0.3.jar` ersetzen, Server vollständig neu starten und das neue Pack annehmen; bei Bedarf `/endupdate pack` verwenden.

Validierung: Neun JUnit-Tests, vier Atlas-Regressionsprüfungen, 52 Packmodelle und 525 Integrationsprüfungen auf Paper 26.3. Alle 32 Tür- und 16 Falltürzustände gegen Vanilla-Modellzustände sowie tatsächliche Paper-Kollisionsflächen geprüft. HTTP-Download und eingebettete ZIP verifiziert. Kein visueller Ingame-Test mit verbundenem Client.

## 1.0.2 – 13.10.2026

- Fehlerhafte Trader-Texturen behoben: Der eigene Bewohner-Skin wird als Blockatlas-Sprite geladen. Alle vier Berufsmodelle verwenden gemeinsam mit ihren Akzenten denselben Atlas.
- Packprüfung ergänzt: Nicht geladene Entity-Sprites, gemischte Item-/Blockatlanten und veraltete Dateien in der eingebetteten ZIP werden erkannt.
- End-Dörfer mit rosa End-Bäumen, eigener Holzpalette und `/locatevillage` für Operatoren.
- Mithryl-Bewohner mit ArmorStand-Hitbox, eigenen ItemDisplay-Modellen, drei Berufen, Arbeitsstationen und Vanilla-Handelsoberfläche. Berufsbindung und Vorräte werden gespeichert.
- Blütenstab, Federfall-Talisman und Kristallfrucht als neue Handelsgegenstände. Mithryl-Ingot mit Barren-Silhouette.
- Custom-Blöcke verwenden die positionsgebundene Display-Darstellung. Keine Vanilla-Texturen und keine globalen Rib-Trim-Texturen werden ersetzt.

Installation: Alte EndUpdate-JAR durch `EndUpdate-1.0.2.jar` ersetzen und den Server vollständig neu starten. Das neue Ressourcenpaket annehmen; bei Bedarf mit `/endupdate pack` erneut anfordern. Bestehende Dörfer und Händler bleiben erhalten.

Validierung: Maven-Build und neun JUnit-Tests; vier Atlas-Regressionsprüfungen; Prüfung der Packverweise und der eingebetteten ZIP. Ein visueller Test mit verbundenem Minecraft-Client ist nicht Bestandteil dieser Prüfung.
