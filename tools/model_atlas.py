"""Validate the vanilla directory-atlas contract used by EndUpdate model sprites.

Since Java 1.21.11 a model must use either block/ or item/ sprites, never both.
Entity equipment textures are loaded separately and are intentionally not model sprites.
EndUpdate supplies no custom atlas definitions, so an arbitrary entity/ PNG is not
discoverable by a baked ItemDisplay model even when that file exists in the pack.
"""
import json


def validate_model_atlas(model, label='model'):
    atlases = set()
    for variable, reference in model.get('textures', {}).items():
        if reference.startswith('#'):
            continue  # Texture aliases do not introduce another sprite.
        namespace, path = reference.split(':', 1) if ':' in reference else ('minecraft', reference)
        directory = path.split('/', 1)[0]
        if directory not in ('block', 'item'):
            raise AssertionError(f'{label}: {variable}={reference} is not in the block/item sprite atlases')
        atlases.add(directory)
    if len(atlases) > 1:
        raise AssertionError(f'{label}: mixed block and item sprite atlases')


def validate_pack_models(pack):
    paths = sorted((pack / 'assets/endupdate/models').rglob('*.json'))
    for path in paths:
        validate_model_atlas(json.loads(path.read_text('utf-8')), path.relative_to(pack))
    return len(paths)
