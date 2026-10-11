# EndUpdate

Mithryl-Erze, neue End-City-Beute und Netherite-Upgrades für **Paper 26.3 / Java 25 oder neuer**.

## Installation

1. `EndUpdate-1.0.1.jar` in den `plugins`-Ordner legen.
2. Server vollständig neu starten.
3. Das beim Beitritt angebotene Ressourcenpaket akzeptieren.

Das Pack ist in der JAR enthalten. Das Plugin entpackt es automatisch, startet seinen eigenen HTTP-Server und bietet es beim Beitritt an. In `server.properties` muss kein Ressourcenpaket eingetragen werden. Andere Serverpacks werden durch `addResourcePack` ergänzt.

**Netzwerkgrenze:** Minecraft-Clients benötigen eine erreichbare HTTP-URL. Standardmäßig verwendet EndUpdate die Adresse, mit der der Spieler verbunden ist, und **Minecraft-Port + 1** (normalerweise **TCP 25566**). Bei einem Hoster muss dieser Port zugeteilt/freigegeben sein. Bei SRV-Einträgen, Proxy, NAT oder abweichender Portweiterleitung müssen gegebenenfalls `resource-pack.port` und `resource-pack.public-url` gesetzt werden. Das Plugin kann keine Firewall- oder Hoster-Freigaben vornehmen. Ein öffentliches Hosting ohne zusätzliche Portfreigabe ist mit einer eingebetteten ZIP allein technisch nicht möglich.

Spieler dürfen das Pack standardmäßig ablehnen; Spiellogik funktioniert dann weiterhin, benutzerdefinierte Modelle können jedoch fehlen. `resource-pack.required: true` macht die Annahme verpflichtend. Das ändert nicht die Download-Erreichbarkeit.

## Mithryl finden und abbauen

- Mithryl erscheint als **Prismarinblock**, ausschließlich auf freier Endstein-Oberfläche der äußeren Endinseln, ab 1.024 Blöcken Entfernung vom Ursprung.
- Standard: 32 % Chance pro Chunk, fünf Versuche für einzelne Erzblöcke. Keine Erzgenerierung in anderen Dimensionen oder unterirdisch.
- Bereits vorhandene End-Chunks werden beim nächsten Laden einmalig geprüft. `ore.generate-in-existing-chunks: false` beschränkt dies auf neue Chunks. Die Prüfung ersetzt nur obersten, nach oben freien Endstein; platzierter Endstein wird nicht von natürlichem unterschieden.
- Fundstellen und der einmalige Generierungsstatus liegen dauerhaft im Chunk-PDC. Erze wachsen nach Abbau oder Neustart nicht nach.
- Abbau nur mit **Netherite- oder Mithryl-Spitzhacke**. Andere Werkzeuge brechen das Erz nicht.
- Ein Erz ergibt einen **Mithryl Shard** (`PRISMARINE_CRYSTALS`). Fortune verwendet die Vanilla-Erzverteilung: Fortune III liefert 1–4 Shards, durchschnittlich 2,2.
- Silk Touch liefert ebenfalls Shards. Creative entfernt das Erz ohne Drop. Explosionen und Kolben können das Erz nicht entfernen oder verschieben.
- Vom Spieler platzierter Prismarin liefert keine Mithryl-Shards.

## Herstellung und Upgrades

**Ingot:** Vier Diamanten und vier echte Mithryl-Shards beliebig im Craftingfeld ergeben einen Mithryl Ingot. Technischer Grundgegenstand ist `PRISMARINE_SHARD`; die eigene Textur behält die Scherben-Silhouette.

**Lagerblock:** Neun Mithryl-Ingots im 3×3-Feld ergeben einen Mithrylblock; ein Block lässt sich wieder in neun Ingots zerlegen. Platzierte Mithrylblöcke werden dauerhaft im Chunk gespeichert und benötigen eine Netherite- oder Mithryl-Spitzhacke. Sie liefern genau einen Mithrylblock zurück; Fortune vervielfacht ihn nicht. Creative entfernt sie ohne Drop. Der technische Blocktyp ist `PRISMARINE_BRICKS`.

**Dreizack:** Drei echte Mithryl-Shards und zwei Stöcke ergeben an der Werkbank einen normalen, unbeschädigten Dreizack:

```text
Leer   Shard  Shard
Leer   Stock  Shard
Stock  Leer   Leer
```

Normale oder nur umbenannte Prismarinkristalle funktionieren nicht. Wie bei Vanilla-Werkbankrezepten wird auch die horizontal gespiegelte Anordnung akzeptiert.

**Schmiedetisch:** Mithryl Upgrade Template + Netherite-Gegenstand + Mithryl Ingot → Mithryl-Gegenstand. Je ein Template und Ingot werden verbraucht. Unterstützt: Schwert, Spitzhacke, Axt, Schaufel, Hacke, Helm, Brustplatte, Hose und Schuhe.

Verzauberungen, vorhandener Schaden, eigener Ambossname und fremde PDC-Daten bleiben erhalten. Der Materialgrundtyp bleibt Netherite; dessen Schutzwerte, Haltbarkeit und Feuerresistenz bleiben erhalten. Das Plugin verändert Itemname, Modell und die Mithryl-Boni. Bereits aufgewertete Ausrüstung lässt sich nicht erneut aufwerten.

**Template duplizieren:** Ein Template, sieben Diamanten und ein Endstein ergeben zwei Templates:

```text
Diamant  Template  Diamant
Diamant  Endstein  Diamant
Diamant  Diamant   Diamant
```

Normale Prismarinscherben, Prismarinkristalle und nur umbenannte Gegenstände ersetzen keine Mithryl-Zutaten. Eigene Rohstoffe können nicht versehentlich für normale Prismarin-Rezepte verbraucht werden. Mithryl-Zutaten mit nachträglich geändertem Namen/Lore können wegen der exakten Rezeptprüfung vom Rezept abweichen. Ausrüstung kann am Amboss repariert werden; das Vanilla-Reparaturmaterial bleibt Netherite.

## Boni

- Mithryl-Werkzeuge/Schwert: **30 % mehr Abbaugeschwindigkeit**, **20 % mehr Angriffsgeschwindigkeit** in der Haupthand. Daraus folgt etwa 16,7 % kürzerer Angriffscooldown. Kein zusätzlicher Angriffsschaden; bestehende Netherite-Attribute bleiben erhalten.
- **1–3 Mithryl-Rüstungsteile:** Speed I.
- **4 Mithryl-Rüstungsteile:** Speed II.
- **0 Teile:** kein Bonus.
- `/speed`, `/speed on`, `/speed off`: persönliche Einstellung, über Neustarts gespeichert. Aktualisierung spätestens nach zehn Ticks. Externe Speed-Effekte durch Tränke, Beacons oder andere Plugins haben Vorrang bis zu ihrem Ablauf und werden beim Ausschalten nicht gelöscht.

## End-City-Loot

Ergänzt die Vanilla-Tabelle `minecraft:chests/end_city_treasure`, auch in bereits generierten, aber noch nicht befüllten Truhen. Bereits befüllte Truhen bleiben unverändert.

| Zusätzliche Beute | Chance je Truhe |
| --- | --- |
| 1 Mithryl Upgrade Template | 30 % |
| 2–6 Mithryl Shards | 45 % |
| 1 Mithryl Ingot | 10 % |
| Ein Bonus: Shulker-Schalen, Drachenatem, Erfahrungsfläschchen, Diamanten oder Mending-Buch | 35 % |

Die vier Würfe sind unabhängig. Werte sind in der automatisch erstellten `plugins/EndUpdate/config.yml` einstellbar. Änderungen erfordern einen vollständigen Neustart.

## Texturen

Mit dem eingebauten Imagegen neu gestaltete **16×16-Inventar- und Blocktexturen**, 64×32-Rüstungstexturen und eine silber-türkise Rib-Variante. Vanilla-Silhouetten, dezente Metallfarben, einfache Pixel-Schattierung und neu überarbeitete Boots. Das Erz zeigt kleine Mithryl-Einschlüsse in Endstein; der Lagerblock eine schlichte Metallfläche. Kein OptiFine und keine Client-Mod nötig.

**Die Erz- und Lagerblocktexturen ersetzen Prismarin und Prismarinziegel global.** Auch normale Blöcke und Varianten, die diese Texturen verwenden, sehen damit anders aus; Mithryl-Drops erhalten ausschließlich vom Plugin markierte Blöcke.

Mithryl-Rüstung trägt ein Rib-Trim und ein eigenes Equipment-Modell. **Die Rib-Texturen werden im Ressourcenpaket ersetzt und wirken deshalb auch auf andere Rüstungen mit Rib-Trim.** Andere Trim-Muster werden nicht ersetzt. Der Mithryl-Trim kann am Schmiedetisch nicht überschrieben werden.

- `art/mithryl-all-preview.png`: Übersicht der zwölf Item-Sprites sowie Erz und Lagerblock.
- `art/mithryl-equipment-preview.png`: Rüstungs- und Trim-Atlanten.
- `art/generated/vanilla/`: gesicherte Originale des aktuellen Imagegen-Sets. Ältere Entwürfe liegen separat unter `art/generated/`.
- `art/VANILLA-PROMPTS.md`: vollständige Prompts des aktuellen Sets.
- `resourcepack/`: fertige Packquellen für Minecraft 26.3, Format 97.1.
- `art/reference/`: Minecraft-UV-Vorlagen als Referenz für die generierten Rüstungs-/Trimtexturen; nicht Teil der Plugin-JAR.

## Befehle

| Befehl | Berechtigung |
| --- | --- |
| `/speed [on\|off]` | `endupdate.speed` (standardmäßig alle) |
| `/endupdate` | alle |
| `/endupdate pack` | alle; Pack erneut anbieten |
| `/endupdate status` | alle; zusätzliche Hostinformationen für Admins |
| `/endupdate help` | Hilfe anzeigen; identisch zu `/endupdate` |
| `/endupdate give <Spieler> <Item> [1–64]` | `endupdate.admin` (standardmäßig OP) |
| `/mithryl give <Spieler> [Item] [1–64]` | `endupdate.admin` (standardmäßig OP) |
| `/mithryl <Spieler> [1–64]` | gibt Ingots; `endupdate.admin` |

Itemnamen: `shard`, `ingot`, `block`, `template`, `sword`, `pickaxe`, `axe`, `shovel`, `hoe`, `helmet`, `chestplate`, `leggings`, `boots`.

Beispiele: `/mithryl give Alex ingot 16`, `/mithryl give Alex block 8`, `/mithryl Alex 16`. Aliasse: `/mythril`, `/givemithryl`, `/givemythril`. Bei vollem Inventar wird der Überschuss am Spieler abgelegt.

Bei einem Konflikt mit dem `/speed`-Befehl eines anderen Plugins funktioniert `/endupdate:speed`.

## Bauen

JDK 25+ und Maven 3.9+:

```powershell
./build.ps1
```

Alternativ `mvn clean package`. Maven packt `resourcepack/` automatisch als `endupdate-pack.zip` in die JAR. Ausgabe: `target/EndUpdate-1.0.1.jar`. Für einen normalen Build ist keine Bildgenerierung und kein API-Schlüssel erforderlich. `tools/prepare_vanilla_assets.py` importiert bei Bedarf die gespeicherten Bildquellen; dafür wird Python mit Pillow benötigt. Die älteren Import-Einstiegspunkte leiten ebenfalls auf dieses aktuelle Set weiter.

## Prüfung

Am 11.10.2026 geprüft:

- Build gegen Paper API `26.3.build.166-beta`, Java-Releaseziel 25.
- Sieben JUnit-Tests für Fortune-Verteilung, Inselgrenze, Set-Stufen, Chunk-Koordinaten und Give-Befehlsargumente.
- 123 Integrationsprüfungen im isolierten Paper-26.3-Server (Build 145): echte Item-Metadaten, Rezeptregistrierung und Crafting-Zutaten, Upgrade-Erhalt, Loot, Oberflächenerz, Idempotenz, Boni, Lagerblock-Verhalten und Dreizack-Rezept einschließlich falscher Zutaten. Spieler und Schmiedetisch-Views werden im Prüfplugin simuliert; kein echter Client war verbunden.
- HTTP-Download mit SHA-1-Abgleich, Ressourcenverweise, PNG-Größen und Abgleich aller Packdateien mit der eingebetteten ZIP.

Ein visueller Ingame-Test, echtes Shift-Klick-Schmieden mit einem Spieler und externe Download-Erreichbarkeit wurden hier nicht geprüft. Zum manuellen Abnehmen: Pack laden, alle neun Upgrades schmieden, Rüstung an-/ausziehen, `/speed` prüfen, Fortune/Silk Touch testen und ein neues End-City-Inventar öffnen. Der isolierte Prüfcode liegt in `tools/integration/` und wird **nicht** in die fertige JAR eingebaut.

Technische Referenzen: [Paper-Projektsetup](https://docs.papermc.io/paper/dev/project-setup/), [Paper ItemMeta API](https://jd.papermc.io/paper/26.3/org/bukkit/inventory/meta/ItemMeta.html).

## Alle Tobias30 MidniteSMP plugins

- Claims: _[Paper 1.5.5](https://github.com/Tobias30yt/Claims/releases/tag/paper-1.5.5)_
- Claims Client Mod: _[1.31.6](https://github.com/Tobias30yt/Claims-Client/releases/tag/v1.31.6)_
- Reports: _[1.2.36](https://github.com/Tobias30yt/Reports/releases/tag/v1.2.36)_
- EndUpdate: _[1.0.1](https://github.com/Tobias30yt/EndUpdate/releases/tag/v1.0.1)_
- GrimAC: _[Modrinth](https://modrinth.com/plugin/grimac)_
