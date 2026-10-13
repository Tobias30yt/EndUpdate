"""Import Imagegen's vanilla-style sources at native Minecraft texture sizes.

Only performs nearest-neighbor resizing and preview layout; no procedural art.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, shutil

ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'resourcepack'
ASSETS = PACK / 'assets/endupdate'
manifest = json.loads((ROOT / 'tools/vanilla-assets.json').read_text('utf-8'))
sprites, blocks, layers = [], [], []
for name, data in manifest.items():
    source = ROOT / 'art/generated/vanilla' / (name + '.png')
    source.parent.mkdir(parents=True, exist_ok=True)
    if not source.exists():
        shutil.copyfile(data['path'], source)
    img = Image.open(source).convert('RGBA')
    if name in ('ore', 'block'):
        small = img.convert('RGB').resize((16, 16), Image.Resampling.NEAREST)
        dest = ASSETS / f'textures/block/mithryl_{name}.png'
        base = 'mossy_stone_bricks' if name == 'ore' else 'polished_blackstone_bricks'
        (PACK / f'assets/minecraft/textures/block/{base}.png').unlink(missing_ok=True)
        (PACK / f'assets/minecraft/textures/block/prismarine.png').unlink(missing_ok=True)
        (PACK / f'assets/minecraft/textures/block/prismarine_bricks.png').unlink(missing_ok=True)
        (PACK / f'assets/minecraft/textures/block/prismarine.png.mcmeta').unlink(missing_ok=True)
        blocks.append((name, small))
    elif name.startswith('rib_'):
        # Keep ordinary Rib trims intact. Custom armour uses its own equipment model.
        (PACK / f'assets/minecraft/textures/trims/entity/{name[4:]}/rib.png').unlink(missing_ok=True)
        continue
    elif name.startswith('humanoid'):
        small = img.resize((64, 32), Image.Resampling.NEAREST)
        dest = ASSETS / f'textures/entity/equipment/{name}/mithryl.png'
        layers.append((name, small))
    else:
        small = img.resize((16, 16), Image.Resampling.NEAREST)
        dest = ASSETS / f'textures/item/mithryl_{name}.png'
        sprites.append((name, small))
    dest.parent.mkdir(parents=True, exist_ok=True)
    small.save(dest)

def sheet(entries, columns, cell, filename):
    result = Image.new('RGB', (columns * cell, ((len(entries)+columns-1)//columns)*cell), '#393939')
    draw = ImageDraw.Draw(result)
    for i, (name, img) in enumerate(entries):
        x, y = i % columns * cell, i // columns * cell
        scaled = img.resize((cell-48, cell-48), Image.Resampling.NEAREST)
        result.paste(scaled, (x+24, y+12), scaled if scaled.mode == 'RGBA' else None)
        draw.text((x+24,y+cell-24), name.upper(), fill='#e5e5e5')
    result.save(ROOT / 'art' / filename)

sheet(sprites, 4, 160, 'mithryl-preview.png')
sheet(blocks, 2, 288, 'mithryl-blocks-preview.png')
sheet(sprites+blocks, 4, 160, 'mithryl-all-preview.png')
Image.open(ASSETS / 'textures/item/mithryl_boots.png').resize((256,256), Image.Resampling.NEAREST).save(ROOT / 'art/mithryl-boots-preview.png')
Image.open(ASSETS / 'textures/item/mithryl_template.png').resize((128,128), Image.Resampling.NEAREST).save(PACK / 'pack.png')
atlas = Image.new('RGB', (768, len(layers)*224), '#393939')
draw = ImageDraw.Draw(atlas)
for i, (name, img) in enumerate(layers):
    scaled = img.resize((384,192), Image.Resampling.NEAREST)
    atlas.paste(scaled, (16, i*224), scaled)
    draw.text((420,i*224+80), name, fill='#e5e5e5')
atlas.save(ROOT / 'art/mithryl-equipment-preview.png')
prompts = '# Vanilla-style Mithryl art\n\nGenerated with built-in Imagegen using vanilla silhouettes and UV layouts as references. Final items and blocks: 16×16; equipment and trims: 64×32. Import uses nearest-neighbor resizing. Sources: `art/generated/vanilla/`.\n\n'
for name, data in manifest.items():
    prompts += '## ' + name + '\n\n' + data['prompt'] + '\n\n'
(ROOT / 'art/VANILLA-PROMPTS.md').write_text(prompts, 'utf-8')
print(f'Imported {len(manifest)} vanilla-style textures.')
