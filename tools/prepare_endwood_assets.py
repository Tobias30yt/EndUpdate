"""Import generated 16x16 End wood art and reuse vanilla block geometry."""
from pathlib import Path
from PIL import Image, ImageDraw
import json, shutil
root=Path(__file__).resolve().parents[1]
pack=root/'resourcepack'
manifest=json.loads((root/'tools/endwood-assets.json').read_text('utf-8'))
mapping={'log':'mangrove_log','log_top':'mangrove_log_top','stripped_log':'stripped_mangrove_log','stripped_log_top':'stripped_mangrove_log_top','planks':'mangrove_planks','leaves':'mangrove_leaves','sapling':'mangrove_propagule','trapdoor':'mangrove_trapdoor','door_bottom':'mangrove_door_bottom','door_top':'mangrove_door_top','door_item':'mangrove_door'}
old_mapping={'log':'cherry_log','log_top':'cherry_log_top','stripped_log':'stripped_cherry_log','stripped_log_top':'stripped_cherry_log_top','planks':'cherry_planks','leaves':'cherry_leaves','sapling':'cherry_sapling','trapdoor':'cherry_trapdoor','door_bottom':'cherry_door_bottom','door_top':'cherry_door_top','door_item':'cherry_door'}
sheet=Image.new('RGB',(640,480),'#302735');draw=ImageDraw.Draw(sheet)
for index,(name,entry) in enumerate(manifest.items()):
    source=root/'art/generated/endwood'/f'{name}.png';source.parent.mkdir(parents=True,exist_ok=True)
    if not source.exists():shutil.copyfile(entry['path'],source)
    img=Image.open(source).convert('RGBA').resize((16,16),Image.Resampling.NEAREST)
    category='item' if name=='door_item' else 'block'
    target=pack/f'assets/endupdate/textures/{category}/end_{name}.png'
    target.parent.mkdir(parents=True,exist_ok=True);img.save(target)
    (pack/f'assets/minecraft/textures/{category}/{old_mapping[name]}.png').unlink(missing_ok=True)
    (pack/f'assets/minecraft/textures/{category}/{mapping[name]}.png').unlink(missing_ok=True)
    x,y=index%4*160,index//4*160
    icon=img.resize((112,112),Image.Resampling.NEAREST);sheet.paste(icon,(x+24,y+12),icon);draw.text((x+16,y+134),name.upper(),fill='#f1d5e7')
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n','utf-8')
for name in ['log','wood','stripped_log','stripped_wood','planks','stairs','slab','fence','fence_gate','door','trapdoor','button','pressure_plate','leaves','sapling']:
    write(pack/f'assets/endupdate/items/end_{name}.json',{'model':{'type':'minecraft:model','model':f'endupdate:block/end_{name}'}})
sheet.save(root/'art/endwood-preview.png')
(root/'art/ENDWOOD-PROMPTS.md').write_text('# End wood textures\n\nBuilt-in Imagegen with vanilla texture references. Originals: `art/generated/endwood/`. Nearest-neighbor import at 16×16.\n\n'+''.join('## '+n+'\n\n'+e['prompt']+'\n\n' for n,e in manifest.items()),'utf-8')
print('Imported eleven End wood textures and fifteen custom item definitions.')
