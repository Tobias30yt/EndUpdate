package de.midnitesmp.endupdate;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;

/** Tracks storage blocks separately from ordinary prismarine bricks. */
public final class MithrylBlocks implements Listener {
    public static final Material STORAGE_BLOCK = Material.PRISMARINE_BRICKS;
    private final EndUpdatePlugin plugin;
    private final NamespacedKey locations;
    private final MithrylItems items;
    private final CustomBlockDisplays displays;

    public MithrylBlocks(EndUpdatePlugin plugin, MithrylItems items, CustomBlockDisplays displays) {
        this.plugin = plugin;
        locations = plugin.key("storage_blocks");
        this.items = items;
        this.displays = displays;
    }

    private int encode(Block block) {
        return MithrylRules.encode(block.getX(), block.getY(), block.getZ(), block.getWorld().getMinHeight());
    }

    private int[] locations(Block block) {
        return block.getChunk().getPersistentDataContainer()
            .getOrDefault(locations, PersistentDataType.INTEGER_ARRAY, new int[0]);
    }

    @EventHandler
    public void load(ChunkLoadEvent event) {
        for (int encoded : event.getChunk().getPersistentDataContainer()
                .getOrDefault(locations, PersistentDataType.INTEGER_ARRAY, new int[0])) {
            Block block = event.getChunk().getBlock(encoded & 15,
                (encoded >> 8) + event.getWorld().getMinHeight(), (encoded >> 4) & 15);
            if (block.getType() == STORAGE_BLOCK) displays.restore(block, "block");
        }
    }

    public boolean isStorage(Block block) {
        return block.getType() == STORAGE_BLOCK
            && Arrays.stream(locations(block)).anyMatch(value -> value == encode(block));
    }

    private void forget(Block block) {
        int[] values = locations(block);
        if (values.length != 0) {
            block.getChunk().getPersistentDataContainer().set(locations, PersistentDataType.INTEGER_ARRAY,
                Arrays.stream(values).filter(value -> value != encode(block)).toArray());
        }
        displays.remove(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        boolean customBlock = block.getType() == STORAGE_BLOCK && items.is(event.getItemInHand(), "block");
        forget(block);
        if (!customBlock) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType() != STORAGE_BLOCK) return;
            int[] values = locations(block);
            int[] extended = Arrays.copyOf(values, values.length + 1);
            extended[values.length] = encode(block);
            block.getChunk().getPersistentDataContainer().set(locations, PersistentDataType.INTEGER_ARRAY, extended);
            displays.show(block, "block", items.create("block"));
        });
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
