"""Render data for the interactive structure viewer from real Paper block output."""
from pathlib import Path
import json,base64
root=Path(__file__).resolve().parents[1]
blocks=[]
for line in (root/'.test-server/village-preview.csv').read_text().splitlines():
    x,y,z,state=line.split(',',3);material=state.split('[')[0].removeprefix('minecraft:')
    blocks.append([int(x)-5128,int(y)-121,int(z)-5128,material,state])
textures={}
custom=root/'resourcepack/assets/endupdate/textures/block'
mapping={'cherry_log':'end_log','cherry_log_top':'end_log_top','stripped_cherry_log':'end_stripped_log',
         'stripped_cherry_log_top':'end_stripped_log_top','cherry_planks':'end_planks',
         'cherry_leaves':'end_leaves','cherry_door_bottom':'end_door_bottom',
         'cherry_door_top':'end_door_top'}
for material,filename in mapping.items():
    path=custom/f'{filename}.png'
    textures[material]='data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()
source=(root/'tools/village-preview-template.html').read_text('utf-8')
(root/'art/end-village-preview.html').write_text(source.replace('__BLOCKS__',json.dumps(blocks)).replace('__TEXTURES__',json.dumps(textures)),'utf-8')
print('Wrote interactive preview from',len(blocks),'actual server blocks.')
