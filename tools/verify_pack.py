"""Validate resource references, dimensions, alpha and embedded ZIP equality."""
import json, zipfile, hashlib, io
from pathlib import Path
from PIL import Image
from model_atlas import validate_pack_models
root=Path(__file__).resolve().parents[1]
pack=root/'resourcepack'
print(f'{validate_pack_models(pack)} model sprite atlases verified (no missing entity sprites or mixed atlases).')
for path in (pack/'assets/endupdate/items').glob('*.json'):
    data=json.loads(path.read_text('utf-8'))
    model=data.get('model')
    assert isinstance(model,dict) and model.get('type')=='minecraft:model',path
    ref=model.get('model')
    assert isinstance(ref,str) and ref.startswith('endupdate:'),path
    target=pack/'assets/endupdate/models'/(ref.split(':',1)[1]+'.json')
    assert target.exists(),target
for path in pack.rglob('*.json'):
    data=json.loads(path.read_text('utf-8'))
    for ref in data.get('textures',{}).values():
        if isinstance(ref,str) and ref.startswith('endupdate:'):
            target=pack/'assets/endupdate/textures'/(ref.split(':',1)[1]+'.png')
            assert target.exists(),target
    def visit(value):
        if isinstance(value,dict):
            for k,v in value.items():
                if k in ('model','layer0','all') and isinstance(v,str) and v.startswith('endupdate:'):
                    ref=v.split(':',1)[1]
                    target=pack/'assets/endupdate'/('models' if k=='model' else 'textures')/(ref+('.json' if k=='model' else '.png'))
                    assert target.exists(),target
                visit(v)
        elif isinstance(value,list):
            for v in value: visit(v)
    visit(data)
for path in (pack/'assets/endupdate/textures/item').glob('*.png'):
    im=Image.open(path); assert im.size==(16,16); assert im.mode=='RGBA'
    lo,hi=im.getchannel('A').getextrema(); assert lo==0 and hi>=240,path
for layer in ('humanoid','humanoid_leggings'):
    assert Image.open(pack/f'assets/endupdate/textures/entity/equipment/{layer}/mithryl.png').size==(64,32)
for name in ('ore','block'):
    own=pack/f'assets/endupdate/textures/block/mithryl_{name}.png'
    assert Image.open(own).size==(16,16)
assert Image.open(pack/'assets/endupdate/textures/block/mithryl_villager.png').size==(32,32)
assert not (pack/'assets/endupdate/textures/entity/mithryl_villager.png').exists()
for job in ('none','smith','gardener','relic'):
    model=json.loads((pack/f'assets/endupdate/models/entity/villager_{job}.json').read_text('utf-8'))
    assert len(model['elements'])>=8
    for element in model['elements']:
        assert all(-16<=v<=32 for corner in ('from','to') for v in element[corner])
        for face in element['faces'].values():assert face['texture'][1:] in model['textures']
assert not any((pack/'assets/minecraft/textures/block').glob('*.png'))
assert not any((pack/'assets/minecraft/textures/item').glob('*.png'))
assert not any(p.is_file() for p in (pack/'assets/minecraft').rglob('*')), 'No vanilla asset overrides allowed'
assert json.loads((pack/'pack.mcmeta').read_text('utf-8'))['pack']['min_format']==[97,1]
jar=root/'target/EndUpdate-1.0.2.jar'
if jar.exists():
    with zipfile.ZipFile(jar) as j:
        with zipfile.ZipFile(io.BytesIO(j.read('endupdate-pack.zip'))) as z:
            actual={n for n in z.namelist() if not n.endswith('/')}
            expected={p.relative_to(pack).as_posix() for p in pack.rglob('*') if p.is_file()}
            assert actual==expected, 'Embedded pack contains stale or missing files'
            for path in pack.rglob('*'):
                if path.is_file(): assert z.read(path.relative_to(pack).as_posix())==path.read_bytes(),path
    print('Embedded pack matches source files. JAR SHA256:',hashlib.sha256(jar.read_bytes()).hexdigest())
print('Pack references, PNGs, transparency and format 97.1 verified.')
