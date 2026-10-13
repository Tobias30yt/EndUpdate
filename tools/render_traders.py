"""Orthographic preview of the actual JSON cuboids and UV textures, not an in-game capture."""
from pathlib import Path
from PIL import Image, ImageDraw
import json

root=Path(__file__).resolve().parents[1]
assets=root/'resourcepack/assets/endupdate'
canvas=Image.new('RGB',(960,400),'#302735');draw=ImageDraw.Draw(canvas)

for index,job in enumerate(('none','smith','gardener','relic')):
    model=json.loads((assets/f'models/entity/villager_{job}.json').read_text())
    textures={k:Image.open(assets/'textures'/(v.split(':')[1]+'.png')).convert('RGBA') for k,v in model['textures'].items()}
    faces=[]
    for element in model['elements']:
        x0,y0,z0=element['from'];x1,y1,z1=element['to']
        corners={
            'south':[(x0,y1,z1),(x1,y1,z1),(x1,y0,z1),(x0,y0,z1)],
            'east':[(x1,y1,z1),(x1,y1,z0),(x1,y0,z0),(x1,y0,z1)],
            'up':[(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)]}
        for side,vertices in corners.items():
            depth=sum(.5*x+.35*y+.85*z for x,y,z in vertices)/4
            faces.append((depth,side,vertices,element['faces'][side]))
    def point(p):
        x,y,z=p
        return (index*240+104+(x*.85-z*.52)*8,320+(-y*.94+x*.2+z*.3)*8)
    def interpolate(v,u,w):
        return tuple(v[0][i]+u*(v[1][i]-v[0][i])+w*(v[3][i]-v[0][i]) for i in range(3))
    for _,side,vertices,face in sorted(faces,key=lambda f:f[0]):
        texture=textures[face['texture'][1:]];u0,v0,u1,v1=face['uv']
        tile=texture.crop((u0*texture.width/16,v0*texture.height/16,u1*texture.width/16,v1*texture.height/16))
        light={'south':1,'east':.74,'up':1.05}[side]
        for y in range(tile.height):
            for x in range(tile.width):
                color=tile.getpixel((x,y))
                if color[3]<128:continue
                poly=[point(interpolate(vertices,u/tile.width,v/tile.height)) for u,v in ((x,y),(x+1,y),(x+1,y+1),(x,y+1))]
                draw.polygon(poly,fill=tuple(min(255,int(c*light)) for c in color[:3]))
    draw.text((index*240+65,375),{'none':'Bewohner','smith':'Mithrylschmied','gardener':'Hainhueter','relic':'Relikthaendler'}[job],fill='#f1d5e7')
draw.text((20,14),'Modellvorschau aus Packdaten (kein Ingame-Screenshot)',fill='#d1bccc')
canvas.save(root/'art/trader-models-preview.png')
