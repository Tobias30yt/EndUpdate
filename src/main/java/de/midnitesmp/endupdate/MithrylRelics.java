package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;

public final class MithrylRelics implements Listener {
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final EndWood wood;
    public MithrylRelics(EndUpdatePlugin plugin, MithrylItems items, EndWood wood) { this.plugin=plugin; this.items=items; this.wood=wood; }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void use(PlayerInteractEvent event) {
        if (event.getHand()!=EquipmentSlot.HAND || (event.getAction()!=Action.RIGHT_CLICK_AIR && event.getAction()!=Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack item=event.getItem(); String id=items.id(item); if (!MithrylItems.RELICS.contains(id)) return;
        // Air interactions can be pre-cancelled by vanilla because these backing items do nothing.
        if (event.getAction()==Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock()==Event.Result.DENY) return;
        if (event.useItemInHand()==Event.Result.DENY) return;
        event.setCancelled(true); Player player=event.getPlayer();
        if (player.getGameMode()==GameMode.SPECTATOR || player.hasCooldown(item)) return;
        var key=plugin.key("relic_ready_"+id); long now=System.currentTimeMillis();
        long ready=player.getPersistentDataContainer().getOrDefault(key,PersistentDataType.LONG,0L);
        if (ready>now) { player.sendActionBar(net.kyori.adventure.text.Component.text("Noch "+((ready-now+999)/1000)+" s Abklingzeit")); return; }
        boolean success=switch(id) {
            case "bloom_wand" -> event.getClickedBlock()!=null && wood.grow(event.getClickedBlock(),true,player);
            case "feather_charm" -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,400,0));
            case "crystal_fruit" -> {
                player.setFoodLevel(Math.min(20,player.getFoodLevel()+6));
                player.setSaturation(Math.min(player.getFoodLevel(),player.getSaturation()+4.8f));
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION,100,1)); yield true;
            }
            default -> false;
        };
        if (!success) return;
        int ticks=id.equals("feather_charm")?1200:20;
        player.setCooldown(item,ticks); player.getPersistentDataContainer().set(key,PersistentDataType.LONG,now+ticks*50L);
        player.getWorld().playSound(player.getLocation(),Sound.BLOCK_AMETHYST_BLOCK_CHIME,.6f,1.2f);
        if (player.getGameMode()!=GameMode.CREATIVE) {
            if (id.equals("crystal_fruit")) item.subtract();
            else if (id.equals("bloom_wand")) {
                // Explicit charges, so Unbreaking cannot turn a trade reward into unlimited growth.
                Damageable meta=(Damageable)item.getItemMeta(); int damage=meta.getDamage()+1;
                if (damage>=32) { item.subtract(); player.playSound(player.getLocation(),Sound.ENTITY_ITEM_BREAK,1,1); }
                else { meta.setDamage(damage); item.setItemMeta(meta); }
            }
        }
    }
}
