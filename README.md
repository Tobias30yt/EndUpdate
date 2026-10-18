# EndUpdate

Mithryl-Erze, neue End-City-Beute und Netherite-Upgrades für **Paper 26.3 / Java 25 oder neuer**.

## Installation

1. `EndUpdate-1.0.3.jar` in den `plugins`-Ordner legen.
2. Server vollständig neu starten.
3. Das beim Beitritt angebotene Ressourcenpaket akzeptieren.

Das Pack ist in der JAR enthalten. Das Plugin entpackt es automatisch, startet seinen eigenen HTTP-Server und bietet es beim Beitritt an. In `server.properties` muss kein Ressourcenpaket eingetragen werden. Andere Serverpacks werden durch `addResourcePack` ergänzt.

**Netzwerkgrenze:** Minecraft-Clients benötigen eine erreichbare HTTP-URL. Standardmäßig verwendet EndUpdate die Adresse, mit der der Spieler verbunden ist, und **Minecraft-Port + 1** (normalerweise **TCP 25566**). Bei einem Hoster muss dieser Port zugeteilt/freigegeben sein. Bei SRV-Einträgen, Proxy, NAT oder abweichender Portweiterleitung müssen gegebenenfalls `resource-pack.port` und `resource-pack.public-url` gesetzt werden. Das Plugin kann keine Firewall- oder Hoster-Freigaben vornehmen. Ein öffentliches Hosting ohne zusätzliche Portfreigabe ist mit einer eingebetteten ZIP allein technisch nicht möglich.

Spieler dürfen das Pack standardmäßig ablehnen; Spiellogik funktioniert dann weiterhin, benutzerdefinierte Modelle können jedoch fehlen. `resource-pack.required: true` macht die Annahme verpflichtend. Das ändert nicht die Download-Erreichbarkeit.

## Mithryl finden und abbauen

- Mithryl erscheint mit eigenem Erzmodell über einem Prismarin-Trägerblock, ausschließlich auf freier Endstein-Oberfläche der äußeren Endinseln, ab 1.024 Blöcken Entfernung vom Ursprung.
- Standard: 32 % Chance pro Chunk, fünf Versuche für einzelne Erzblöcke. Keine Erzgenerierung in anderen Dimensionen oder unterirdisch.
- Bereits vorhandene End-Chunks werden beim nächsten Laden einmalig geprüft. `ore.generate-in-existing-chunks: false` beschränkt dies auf neue Chunks. Die Prüfung ersetzt nur obersten, nach oben freien Endstein; platzierter Endstein wird nicht von natürlichem unterschieden.
- Fundstellen und der einmalige Generierungsstatus liegen dauerhaft im Chunk-PDC. Erze wachsen nach Abbau oder Neustart nicht nach.
- Abbau nur mit **Netherite- oder Mithryl-Spitzhacke**. Andere Werkzeuge brechen das Erz nicht.
- Ein Erz ergibt einen **Mithryl Shard** (`PRISMARINE_CRYSTALS`). Fortune verwendet die Vanilla-Erzverteilung: Fortune III liefert 1–4 Shards, durchschnittlich 2,2.
- Silk Touch liefert ebenfalls Shards. Creative entfernt das Erz ohne Drop. Explosionen und Kolben können das Erz nicht entfernen oder verschieben.
- Vom Spieler platzierter Prismarin liefert keine Mithryl-Shards.

## Herstellung und Upgrades

**Ingot:** Vier Diamanten und vier echte Mithryl-Shards beliebig im Craftingfeld ergeben einen Mithryl Ingot. Technischer Grundgegenstand ist `PRISMARINE_SHARD`; die eigene Textur zeigt jetzt einen Vanilla-artigen Metallbarren in den bisherigen silber-türkisen Farben.

**Lagerblock:** Neun Mithryl-Ingots im 3×3-Feld ergeben einen Mithrylblock; ein Block lässt sich wieder in neun Ingots zerlegen. Platzierte Mithrylblöcke werden dauerhaft im Chunk gespeichert und benötigen eine Netherite- oder Mithryl-Spitzhacke. Sie liefern genau einen Mithrylblock zurück; Fortune vervielfacht ihn nicht. Creative entfernt sie ohne Drop. Der technische Blocktyp ist `PRISMARINE_BRICKS`; das Pack ersetzt dessen Vanilla-Textur nicht.

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

Mit dem eingebauten Imagegen neu gestaltete **16×16-Inventar- und Blocktexturen**, 64×32-Rüstungstexturen und ein 32×32-Atlas für die Mithryl-Bewohner. Vanilla-Silhouetten, dezente Metallfarben, einfache Pixel-Schattierung und neu überarbeitete Boots. Das Erz zeigt kleine Mithryl-Einschlüsse in Endstein; der Lagerblock eine schlichte Metallfläche. Kein OptiFine und keine Client-Mod nötig.

**Alle EndUpdate-Texturen und -Modelle liegen im eigenen `endupdate`-Namespace.** Auf EndUpdate-Blöcken zeigt ein zusätzliches Item-Display das Custom-Modell; der darunterliegende Block behält sein unverändertes Vanilla-Modell. Displays werden nur in einem Radius von vier Chunks um mindestens einen Spieler gespawnt und außerhalb dieses Bereichs entfernt; beim Betreten werden sie aus den Chunk-Daten wiederhergestellt. Normale Cherry-, Mangrove-, Prismarin-, Moossteinziegel- und Schwarzsteinziegel-Blöcke werden nicht vom Pack überschrieben.

Mithryl-Rüstung trägt ein Rib-Trim und ein eigenes Equipment-Modell. Auch die globalen Rib-Texturersetzungen wurden entfernt: normale Rüstungen und sämtliche Vanilla-Items behalten ihre ursprünglichen Texturen. Der Mithryl-Trim kann am Schmiedetisch nicht überschrieben werden.

**Trader-Texturfix in 1.0.3:** Der Bewohner-Skin liegt jetzt als `endupdate:block/mithryl_villager` im Block-Texturatlas, zusammen mit den Hut- und Kleidungsakzenten. Der vorherige Pfad unter `textures/entity/` wurde für das ItemDisplay-Modell nicht als Sprite geladen. Minecraft verlangt außerdem, dass alle Modelltexturen denselben Atlas verwenden ([Mojang-Dokumentation](https://www.minecraft.net/en-us/article/minecraft-java-edition-1-21-11)). Bestehende Händler erhalten den Fix über das neue Pack; nach Austausch der JAR den Server neu starten und neu verbinden oder `/endupdate pack` ausführen. Es ist keine Neuerzeugung der Dörfer nötig.

## End-Dörfer und Mithryl-Bewohner

Auf passenden äußeren Endinseln entstehen Dörfer mit vier Häusern, rosa End-Holz, Bäumen und Mithryl-Beute. `/locatevillage` findet ein tatsächlich erzeugtes Dorf; nur Operatoren dürfen den Befehl verwenden. Neue Dorfgebiete werden standardmäßig nur in neuen Chunks erzeugt. Bereits bestehende EndUpdate-Dörfer erhalten beim Laden ihre Bewohner; bearbeitete Häuser werden dabei nicht überschrieben.

Jedes Dorf erhält vier stationäre Bewohner: einen Mithrylschmied, einen Hainhüter, einen Relikthändler und einen Bewohner ohne Beruf. Ein unsichtbarer ArmorStand stellt die Hitbox bereit, ein ItemDisplay das eigene Modell. Die Bewohner lassen sich per Rechtsklick in der normalen Villager-Handelsoberfläche ansprechen. Ihre Modelle und die Arbeitsstationen haben eigene Texturen; kein Villager-Skin und kein anderer Vanilla-Block wird ersetzt.

| Beruf | Arbeitsplatz | Angebote |
| --- | --- | --- |
| Mithrylschmied | `mithryl_forge` | 2 Diamanten → 1 Shard; 4 Shards + 4 Diamanten → 1 Ingot; 6 Ingots + 12 Diamanten → 1 Template; 3 Shards → 8 Erfahrungsfläschchen |
| Hainhüter | `grove_bench` | 16 End-Logs → 1 Shard; 2 Shards → 3 End-Setzlinge; 4 Ingots + 4 End-Setzlinge → 1 Blütenstab; 3 Shards + 6 Chorusfrüchte → 3 Kristallfrüchte |
| Relikthändler | `relic_altar` | 8 Ingots + 4 Phantomhäute → 1 Federfall-Talisman; 3 Ingots + 8 Enderperlen → 1 Shulker-Schale; 6 Shards + 2 Glasflaschen → 2 Drachenatem; 4 Shards → 2 Kristallfrüchte |

Unbeschäftigte Bewohner nehmen einen freien Arbeitsplatz innerhalb von acht Blöcken an. Jeder Arbeitsplatz gehört höchstens einem Bewohner. Vor dem ersten Handel kann der Beruf wechseln. Nach dem ersten Handel bleibt er fest; ein abgebauter Arbeitsplatz muss durch einen passenden ersetzt werden. Ohne Arbeitsplatz ist kein Handel möglich. Angebote haben begrenzte Vorräte; mit gültigem Arbeitsplatz werden sie frühestens alle 20 Minuten aufgefüllt, auch nach Neustarts nicht öfter. Ein Händler bedient jeweils einen Spieler. Original-Mithryl-Zutaten werden anhand ihrer Metadaten geprüft; normale oder nur umbenannte Prismaringegenstände werden abgelehnt.

Die drei Arbeitsplätze sind abbaubar, platzierbar und craftbar. Ihre Rezepte erscheinen im Rezeptbuch:

```text
Mithrylschmiede       Hainwerkbank         Reliktaltar
I I I                S I S                 · A ·
D F D                P P P                 I U I
P P P                P · P                 U U U

I = Mithryl Ingot   P = End-Planks   S = End-Setzling
D = polierter Tiefenschiefer   F = Ofen   A = Amethystscherbe   U = Purpurblock
```

## Neue Relikte

- **Blütenstab** (`bloom_wand`): Rechtsklick auf einen End-Setzling lässt einen End-Baum wachsen. 32 erfolgreiche Anwendungen; blockiertes oder durch Schutzplugins abgebrochenes Wachstum verbraucht keine Anwendung.
- **Federfall-Talisman** (`feather_charm`): Rechtsklick gewährt 20 Sekunden sanften Fall. Wiederverwendbar, 60 Sekunden persönliche Abklingzeit, über Neustarts gespeichert.
- **Kristallfrucht** (`crystal_fruit`): Rechtsklick verbraucht eine Frucht, füllt drei Hungerkeulen und etwas Sättigung auf und gewährt fünf Sekunden Regeneration II. Eine Sekunde Abklingzeit.

Die Relikte werden in der Haupthand verwendet. Stärkere bestehende Trankeffekte werden von den normalen Bukkit-Effektregeln geschützt. Neue Texturvorschau: `art/traders-preview.png`; 3D-Modellvorschau aus den Packdaten: `art/trader-models-preview.png` (kein Ingame-Screenshot); generierte Originale: `art/generated/traders/`; vollständige Prompts: `art/TRADER-PROMPTS.md`.

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
| `/locatevillage` | ausschließlich Operatoren, im End |
| `/endupdate give <Spieler> <Item> [1–64]` | `endupdate.admin` (standardmäßig OP) |
| `/mithryl give <Spieler> [Item] [1–64]` | `endupdate.admin` (standardmäßig OP) |
| `/mithryl <Spieler> [1–64]` | gibt Ingots; `endupdate.admin` |

Itemnamen: `shard`, `ingot`, `block`, `ore`, `template`, `sword`, `pickaxe`, `axe`, `shovel`, `hoe`, `helmet`, `chestplate`, `leggings`, `boots`, alle `end_…`-Holzblöcke sowie `mithryl_forge`, `grove_bench`, `relic_altar`, `bloom_wand`, `feather_charm`, `crystal_fruit`.

Beispiele: `/mithryl give Alex ingot 16`, `/mithryl give Alex block 8`, `/mithryl Alex 16`. Aliasse: `/mythril`, `/givemithryl`, `/givemythril`. Bei vollem Inventar wird der Überschuss am Spieler abgelegt.

Bei einem Konflikt mit dem `/speed`-Befehl eines anderen Plugins funktioniert `/endupdate:speed`.

## Bauen

JDK 25+ und Maven 3.9+:

```powershell
./build.ps1
```

Alternativ `mvn clean package`. Maven packt `resourcepack/` automatisch als `endupdate-pack.zip` in die JAR. Ausgabe: `target/EndUpdate-1.0.3.jar`. Für einen normalen Build ist keine Bildgenerierung und kein API-Schlüssel erforderlich. `tools/prepare_vanilla_assets.py`, `tools/prepare_endwood_assets.py` und `tools/prepare_trader_assets.py` importieren bei Bedarf die gespeicherten Bildquellen; dafür wird Python mit Pillow benötigt. `tools/render_traders.py` rendert die JSON-Modelle als Vorschau. Alle fertigen Packdateien sind bereits vorhanden.

## Prüfung

Am 13.10.2026 geprüft:

- Build gegen Paper API `26.3.build.166-beta`, Java-Releaseziel 25.
- Neun JUnit-Tests für Fortune-Verteilung, Inselgrenze, Set-Stufen, Chunk-Koordinaten, Dorfgrundriss und Give-Befehlsargumente.
- Vier Python-Regressionsprüfungen für nicht geladene Entity-Sprites, gemischte Atlanten und gültige Block-/Item-Sprites. Alle 52 Packmodelle bestehen die Atlasprüfung; die vorherigen Trader-Modelle schlagen dabei reproduzierbar fehl. Die elf Tür-/Falltürmodelle werden zusätzlich auf UVs, Geometrie und Modellverweise geprüft.
- 525 Integrationsprüfungen im isolierten Paper-26.3-Server (Build 145): Item-Metadaten, Rezepte, Upgrades, Loot, Erze, Boni, Lagerblöcke, Dreizack, natürliche Dörfer, Bäume, vier Bewohner pro Dorf, Arbeitsplatzvergabe, Berufswechsel und Berufssperre, Handelsangebote, falsche Zutaten, Vorratsauffüllung sowie alle drei Reliktfunktionen. Dazu alle 32 Tür- und 16 Falltürzustände gegen Vanilla-UV-Zustände und tatsächliche Paper-Kollisionsflächen sowie Aktualisierung bei Interaktion, Redstone und Physik.
- Ein vollständiger Testserver-Neustart bestätigt gespeicherte Bewohner-UUIDs, feste Berufe und verbrauchte Handelsvorräte. Spieler und Inventar-Views werden im Prüfplugin simuliert; kein echter Client war verbunden.
- HTTP-Download mit SHA-1-Abgleich, Modell-/Texturreferenzen, PNG-Größen, vollständiger Abgleich mit der eingebetteten ZIP und Prüfung auf **null Vanilla-Dateiüberschreibungen**.

Ein visueller Ingame-Test, echte Shift-Klick-Handelstransaktionen/Schmiedevorgänge mit einem Spieler und externe Download-Erreichbarkeit wurden hier nicht geprüft. Zum manuellen Abnehmen: Pack laden, `/locatevillage` als OP ausführen, Bewohner rechtsklicken, normale und Shift-Klick-Käufe testen und dem freien Bewohner einen Arbeitsplatz geben. Der isolierte Prüfcode liegt in `tools/integration/` und wird **nicht** in die fertige JAR eingebaut.

Technische Referenzen: [Paper-Projektsetup](https://docs.papermc.io/paper/dev/project-setup/), [Paper ItemMeta API](https://jd.papermc.io/paper/26.3/org/bukkit/inventory/meta/ItemMeta.html).

## Alle Tobias30 MidniteSMP plugins

- Claims: _[Paper 1.5.6](https://github.com/Tobias30yt/Claims/releases/tag/paper-1.5.6)_
- Claims Client Mod: _[1.31.6](https://github.com/Tobias30yt/Claims-Client/releases/tag/v1.31.6)_
- Reports: _[1.2.36](https://github.com/Tobias30yt/Reports/releases/tag/v1.2.36)_
- EndUpdate: _[1.0.3](https://github.com/Tobias30yt/EndUpdate/releases/tag/v1.0.3)_
- GrimAC: _[Modrinth](https://modrinth.com/plugin/grimac)_
