import unittest
from model_atlas import validate_model_atlas


class ModelAtlasTests(unittest.TestCase):
    def test_old_trader_entity_sprite_is_rejected(self):
        with self.assertRaisesRegex(AssertionError, 'not in the block/item'):
            validate_model_atlas({'textures': {'skin': 'endupdate:entity/mithryl_villager',
                                              'accent': 'endupdate:block/mithryl_block'}})

    def test_moving_skin_to_item_would_still_mix_atlases(self):
        with self.assertRaisesRegex(AssertionError, 'mixed block and item'):
            validate_model_atlas({'textures': {'skin': 'endupdate:item/mithryl_villager',
                                              'accent': 'endupdate:block/mithryl_block'}})

    def test_trader_with_all_block_sprites(self):
        validate_model_atlas({'textures': {'skin': 'endupdate:block/mithryl_villager',
                                          'accent': 'endupdate:block/mithryl_block', 'particle': '#skin'}})

    def test_inventory_item_sprite(self):
        validate_model_atlas({'textures': {'layer0': 'endupdate:item/mithryl_ingot'}})


if __name__ == '__main__':
    unittest.main()
