"""Build namespaced display geometry for doors/trapdoors from saved vanilla UV layouts.

No raster image changes. A tiny model-space outset prevents coplanar flicker with
the functional backing block. Rotation and state selection live in HingedBlockModels.
"""
from pathlib import Path
import copy
import json

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'resourcepack/assets/endupdate'
OUTSET = .02  # model pixels, 1/800 of a block; do not scale around the block centre


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2)+'\n', 'utf-8')


def generate():
    layouts = json.loads((ROOT/'tools/fixtures/hinged-vanilla-models.json').read_text('utf-8'))
    for name, source in layouts['models'].items():
        model = copy.deepcopy(source)
        model['parent'] = 'minecraft:block/block'
        model['ambientocclusion'] = False
        if name.startswith('door_'):
            model['textures'] = {'bottom':'endupdate:block/end_door_bottom',
                                 'top':'endupdate:block/end_door_top',
                                 'particle':'endupdate:block/end_door_'+('top' if name.startswith('door_top') else 'bottom')}
            target = 'end_door_display_'+name.removeprefix('door_')
        else:
            model['textures'] = {'texture':'endupdate:block/end_trapdoor','particle':'endupdate:block/end_trapdoor'}
            target = 'end_trapdoor_display_'+name.removeprefix('template_orientable_trapdoor_')
        for element in model['elements']:
            element['from'] = [v-OUTSET for v in element['from']]
            element['to'] = [v+OUTSET for v in element['to']]
            for face in element['faces'].values():
                face.pop('cullface',None)
        write(ASSETS/f'models/block/{target}.json', model)
        write(ASSETS/f'items/{target}.json', {'model':{'type':'minecraft:model','model':'endupdate:block/'+target}})
    # Inventory geometry and compatibility aliases for saved displays from older versions.
    write(ASSETS/'models/block/end_trapdoor.json', {'parent':'endupdate:block/end_trapdoor_display_bottom'})
    for half in ('bottom','top'):
        write(ASSETS/f'models/block/end_door_display_{half}.json', {'parent':f'endupdate:block/end_door_display_{half}_left'})


if __name__ == '__main__':
    generate()
    print('Generated 8 door and 3 trapdoor display models with explicit UVs and anti-flicker outset.')
