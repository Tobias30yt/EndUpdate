package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import java.util.concurrent.ThreadLocalRandom;

public final class EndCityLoot implements Listener {
    private final EndUpdatePlugin plugin; private final MithrylItems items;
    public EndCityLoot(EndUpdatePlugin plugin,MithrylItems items){this.plugin=plugin;this.items=items;}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void loot(LootGenerateEvent e) {
        if(e.isPlugin() || !e.getLootTable().getKey().equals(NamespacedKey.minecraft("chests/end_city_treasure")))return;
        var r=ThreadLocalRandom.current();
        if(r.nextDouble()<plugin.getConfig().getDouble("loot.template-chance",.30))e.getLoot().add(items.create("template"));
        if(r.nextDouble()<plugin.getConfig().getDouble("loot.shards-chance",.45)){ItemStack shards=items.create("shard"); shards.setAmount(r.nextInt(2,7)); e.getLoot().add(shards);}
        if(r.nextDouble()<plugin.getConfig().getDouble("loot.ingot-chance",.10))e.getLoot().add(items.create("ingot"));
        if(r.nextDouble()<plugin.getConfig().getDouble("loot.bonus-chance",.35)) {
            ItemStack bonus=switch(r.nextInt(5)) {
                case 0 -> new ItemStack(Material.SHULKER_SHELL,r.nextInt(1,4));
                case 1 -> new ItemStack(Material.DRAGON_BREATH,r.nextInt(2,6));
                case 2 -> new ItemStack(Material.EXPERIENCE_BOTTLE,r.nextInt(8,17));
                case 3 -> new ItemStack(Material.DIAMOND,r.nextInt(2,6));
                default -> { ItemStack book=new ItemStack(Material.ENCHANTED_BOOK); var meta=(EnchantmentStorageMeta)book.getItemMeta(); meta.addStoredEnchant(Enchantment.MENDING,1,true); book.setItemMeta(meta); yield book; }
            }; e.getLoot().add(bonus);
        }
    }
}
