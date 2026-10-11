"""Validate resource references, dimensions, alpha and embedded ZIP equality."""
import json, zipfile, hashlib, io
from pathlib import Path
from PIL import Image
root=Path(__file__).resolve().parents[1]
pack=root/'resourcepack'
for path in pack.rglob('*.json'):
    data=json.loads(path.read_text('utf-8'))
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
for name,base in [('ore','prismarine'),('block','prismarine_bricks')]:
    own=pack/f'assets/endupdate/textures/block/mithryl_{name}.png'
    assert Image.open(own).size==(16,16)
    assert own.read_bytes()==(pack/f'assets/minecraft/textures/block/{base}.png').read_bytes()
assert json.loads((pack/'assets/minecraft/textures/block/prismarine.png.mcmeta').read_text())['animation']['frames']==[0]
assert json.loads((pack/'pack.mcmeta').read_text('utf-8'))['pack']['min_format']==[97,1]
jar=root/'target/EndUpdate-1.0.1.jar'
if jar.exists():
    with zipfile.ZipFile(jar) as j:
        with zipfile.ZipFile(io.BytesIO(j.read('endupdate-pack.zip'))) as z:
            for path in pack.rglob('*'):
                if path.is_file(): assert z.read(path.relative_to(pack).as_posix())==path.read_bytes(),path
    print('Embedded pack matches source files. JAR SHA256:',hashlib.sha256(jar.read_bytes()).hexdigest())
print('Pack references, PNGs, transparency and format 97.1 verified.')
