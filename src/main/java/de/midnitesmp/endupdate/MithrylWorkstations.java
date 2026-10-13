package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Ordinary solid backing blocks; only marked positions receive a custom display. */
public final class MithrylWorkstations implements Listener {
    public static final Map<String, Material> MATERIALS = Map.of(
        "mithryl_forge", Material.POLISHED_DEEPSLATE,
        "grove_bench", Material.CHERRY_PLANKS,
        "relic_altar", Material.PURPUR_BLOCK);
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final CustomBlockDisplays displays;

    public MithrylWorkstations(EndUpdatePlugin plugin, MithrylItems items, CustomBlockDisplays displays) {
        this.plugin = plugin; this.items = items; this.displays = displays;
    }
    private NamespacedKey key(Block block, String prefix) {
        return plugin.key(prefix + Integer.toHexString(MithrylRules.encode(block.getX(), block.getY(), block.getZ(), block.getWorld().getMinHeight())));
    }
    public String id(Block block) {
        String id = block.getChunk().getPersistentDataContainer().getOrDefault(key(block, "station_"), PersistentDataType.STRING, "");
        return MATERIALS.get(id) == block.getType() ? id : "";
    }
    public void set(Block block, String id) {
        block.setType(Objects.requireNonNull(MATERIALS.get(id)), false);
        block.getChunk().getPersistentDataContainer().set(key(block, "station_"), PersistentDataType.STRING, id);
        displays.show(block, id, items.create(id));
    }
    public String owner(Block block) {
        return block.getChunk().getPersistentDataContainer().getOrDefault(key(block, "station_owner_"), PersistentDataType.STRING, "");
    }
    public void claim(Block block, UUID owner) {
        block.getChunk().getPersistentDataContainer().set(key(block, "station_owner_"), PersistentDataType.STRING, owner.toString());
    }
    public void release(Block block) { block.getChunk().getPersistentDataContainer().remove(key(block, "station_owner_")); }
    private void forget(Block block) {
        block.getChunk().getPersistentDataContainer().remove(key(block, "station_"));
        release(block); displays.remove(block);
    }
    public List<Block> nearby(Location origin, int radius) {
        List<Block> result = new ArrayList<>(); World world = origin.getWorld();
        for (int x = (origin.getBlockX()-radius)>>4; x <= (origin.getBlockX()+radius)>>4; x++)
            for (int z = (origin.getBlockZ()-radius)>>4; z <= (origin.getBlockZ()+radius)>>4; z++) {
                if (!world.isChunkLoaded(x,z)) continue;
                Chunk chunk = world.getChunkAt(x,z);
                for (NamespacedKey key : chunk.getPersistentDataContainer().getKeys()) {
                    if (!key.getNamespace().equals(plugin.getName().toLowerCase(Locale.ROOT)) || !key.getKey().startsWith("station_") || key.getKey().startsWith("station_owner_")) continue;
                    int encoded;
                    try { encoded = Integer.parseUnsignedInt(key.getKey().substring(8),16); } catch (NumberFormatException ex) { continue; }
                    Block block = chunk.getBlock(encoded & 15, (encoded >> 8)+world.getMinHeight(), (encoded >> 4)&15);
                    if (!id(block).isEmpty() && block.getLocation().distanceSquared(origin) <= radius*radius) result.add(block);
                }
            }
        result.sort(Comparator.comparingDouble(b -> b.getLocation().distanceSquared(origin))); return result;
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void place(BlockPlaceEvent event) {
        String id = items.id(event.getItemInHand()); Block block = event.getBlock();
        // Delay until vanilla has committed the placement, as in EndWood.
        if (MATERIALS.get(id) == block.getType()) Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() == MATERIALS.get(id)) set(block,id);
        });
        else if (block.getChunk().getPersistentDataContainer().has(key(block,"station_"))) forget(block);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void breaking(BlockBreakEvent event) { if (!id(event.getBlock()).isEmpty()) event.setDropItems(false); }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void broken(BlockBreakEvent event) {
        Block block = event.getBlock(); String id = id(block); if (id.isEmpty()) return;
        if (event.getPlayer().getGameMode()!=GameMode.CREATIVE) block.getWorld().dropItemNaturally(block.getLocation(),items.create(id));
        forget(block);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void inspect(PlayerInteractEvent event) {
        if (event.getAction()!=Action.RIGHT_CLICK_BLOCK || event.getHand()!=org.bukkit.inventory.EquipmentSlot.HAND || event.getPlayer().isSneaking()) return;
        String id = id(event.getClickedBlock()); if (id.isEmpty()) return;
        event.setCancelled(true); Player player = event.getPlayer();
        player.sendMessage("§d" + MithrylTraders.Job.forStation(id).title + " §7· " + (owner(event.getClickedBlock()).isEmpty() ? "Arbeitsplatz frei" : "Arbeitsplatz vergeben"));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void extend(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->!id(b).isEmpty()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->!id(b).isEmpty()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->!id(b).isEmpty());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->!id(b).isEmpty());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e){if(!id(e.getBlock()).isEmpty())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void change(EntityChangeBlockEvent e){if(!id(e.getBlock()).isEmpty())e.setCancelled(true);}
}
