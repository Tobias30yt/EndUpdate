# Änderungen

## 1.0.2 – 13.10.2026

- Fehlerhafte Trader-Texturen behoben: Der eigene Bewohner-Skin wird als Blockatlas-Sprite geladen. Alle vier Berufsmodelle verwenden gemeinsam mit ihren Akzenten denselben Atlas.
- Packprüfung ergänzt: Nicht geladene Entity-Sprites, gemischte Item-/Blockatlanten und veraltete Dateien in der eingebetteten ZIP werden erkannt.
- End-Dörfer mit rosa End-Bäumen, eigener Holzpalette und `/locatevillage` für Operatoren.
- Mithryl-Bewohner mit ArmorStand-Hitbox, eigenen ItemDisplay-Modellen, drei Berufen, Arbeitsstationen und Vanilla-Handelsoberfläche. Berufsbindung und Vorräte werden gespeichert.
- Blütenstab, Federfall-Talisman und Kristallfrucht als neue Handelsgegenstände. Mithryl-Ingot mit Barren-Silhouette.
- Custom-Blöcke verwenden die positionsgebundene Display-Darstellung. Keine Vanilla-Texturen und keine globalen Rib-Trim-Texturen werden ersetzt.

Installation: Alte EndUpdate-JAR durch `EndUpdate-1.0.2.jar` ersetzen und den Server vollständig neu starten. Das neue Ressourcenpaket annehmen; bei Bedarf mit `/endupdate pack` erneut anfordern. Bestehende Dörfer und Händler bleiben erhalten.

Validierung: Maven-Build und neun JUnit-Tests; vier Atlas-Regressionsprüfungen; Prüfung der Packverweise und der eingebetteten ZIP. Ein visueller Test mit verbundenem Minecraft-Client ist nicht Bestandteil dieser Prüfung.
