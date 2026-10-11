package de.midnitesmp.endupdate;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;

/** Tracks storage blocks separately from naturally generated ore and ordinary prismarine. */
public final class MithrylBlocks implements Listener {
    private final NamespacedKey locations;
    private final MithrylItems items;

    public MithrylBlocks(EndUpdatePlugin plugin, MithrylItems items) {
        locations = plugin.key("storage_blocks");
        this.items = items;
    }

    private int encode(Block block) {
        return MithrylRules.encode(block.getX(), block.getY(), block.getZ(), block.getWorld().getMinHeight());
    }

    private int[] locations(Block block) {
        return block.getChunk().getPersistentDataContainer()
            .getOrDefault(locations, PersistentDataType.INTEGER_ARRAY, new int[0]);
    }

    public boolean isStorage(Block block) {
        return block.getType() == Material.PRISMARINE_BRICKS
            && Arrays.stream(locations(block)).anyMatch(value -> value == encode(block));
    }

    private void forget(Block block) {
        int[] values = locations(block);
        if (values.length == 0) return;
        block.getChunk().getPersistentDataContainer().set(locations, PersistentDataType.INTEGER_ARRAY,
            Arrays.stream(values).filter(value -> value != encode(block)).toArray());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        forget(block);
        if (block.getType() != Material.PRISMARINE_BRICKS || !items.is(event.getItemInHand(), "block")) return;
        int[] values = locations(block);
        int[] extended = Arrays.copyOf(values, values.length + 1);
        extended[values.length] = encode(block);
        block.getChunk().getPersistentDataContainer().set(locations, PersistentDataType.INTEGER_ARRAY, extended);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void breakBlock(BlockBreakEvent event) {
        if (!isStorage(event.getBlock())) return;
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE
            && event.getPlayer().getInventory().getItemInMainHand().getType() != Material.NETHERITE_PICKAXE) {
            event.setCancelled(true);
            event.getPlayer().sendActionBar(Component.text("Mithryl braucht eine Netherite- oder Mithryl-Spitzhacke."));
            return;
        }
        event.setDropItems(false);
        event.setExpToDrop(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drop(BlockBreakEvent event) {
        if (!isStorage(event.getBlock())) return;
        forget(event.getBlock());
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation().add(.5, .5, .5), items.create("block"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonExtendEvent event) { if (event.getBlocks().stream().anyMatch(this::isStorage)) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void piston(BlockPistonRetractEvent event) { if (event.getBlocks().stream().anyMatch(this::isStorage)) event.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(EntityExplodeEvent event) { event.blockList().removeIf(this::isStorage); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void explosion(BlockExplodeEvent event) { event.blockList().removeIf(this::isStorage); }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void entity(EntityChangeBlockEvent event) { if (isStorage(event.getBlock())) event.setCancelled(true); }
}
