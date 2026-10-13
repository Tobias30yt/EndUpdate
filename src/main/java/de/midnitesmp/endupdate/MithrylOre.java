package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MithrylOre implements Listener {
    public static final Material ORE_BLOCK = Material.PRISMARINE;
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final CustomBlockDisplays displays;
    private final NamespacedKey generated, locations;
    private record Pending(World world, int x, int z) {}
    private final Set<Pending> queue = new LinkedHashSet<>();
    public MithrylOre(EndUpdatePlugin plugin, MithrylItems items, CustomBlockDisplays displays) {
        this.plugin = plugin; this.items = items; this.displays = displays; generated = plugin.key("ore_generated_v1"); locations = plugin.key("ore_locations");
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (int i=0; i<2 && !queue.isEmpty(); i++) {
                var it = queue.iterator(); Pending p = it.next(); it.remove();
                if (p.world.isChunkLoaded(p.x, p.z)) generate(p.world.getChunkAt(p.x, p.z));
            }
        }, 1L, 1L);
    }
    public void enqueue(Chunk chunk) {
        if (chunk.getWorld().getEnvironment() == World.Environment.THE_END && !chunk.getPersistentDataContainer().has(generated))
            queue.add(new Pending(chunk.getWorld(), chunk.getX(), chunk.getZ()));
    }
    @EventHandler public void load(ChunkLoadEvent e) {
        var pdc = e.getChunk().getPersistentDataContainer();
        int[] savedLocations = pdc.get(locations, PersistentDataType.INTEGER_ARRAY);
        if (savedLocations != null) {
            for (int encoded : savedLocations) {
                Block block = e.getChunk().getBlock(encoded & 15,
                    (encoded >> 8) + e.getWorld().getMinHeight(), (encoded >> 4) & 15);
                if (block.getType() == ORE_BLOCK) displays.restore(block, "ore");
            }
        }
        if (e.isNewChunk() || plugin.getConfig().getBoolean("ore.generate-in-existing-chunks", true)) enqueue(e.getChunk());
    }
    private void generate(Chunk chunk) {
        var pdc = chunk.getPersistentDataContainer(); if (pdc.has(generated)) return;
        pdc.set(generated, PersistentDataType.BYTE, (byte)1);
        Random random = new Random(chunk.getWorld().getSeed() ^ (chunk.getX()*341873128712L) ^ (chunk.getZ()*132897987541L) ^ 0x4D49544852594CL);
        if (random.nextDouble() >= plugin.getConfig().getDouble("ore.chunk-chance", .32)) return;
        Set<Integer> found = new HashSet<>(); World world = chunk.getWorld();
        int radius = Math.max(1024, plugin.getConfig().getInt("ore.outer-island-radius", 1024));
        int attempts = Math.clamp(plugin.getConfig().getInt("ore.attempts-per-chunk", 5), 1, 64);
        for (int i=0; i<attempts; i++) {
            int x = (chunk.getX()<<4)+random.nextInt(16), z = (chunk.getZ()<<4)+random.nextInt(16);
            if (!MithrylRules.outerIsland(x,z,radius)) continue;
            int y = world.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (y < world.getMinHeight() || y >= world.getMaxHeight()-1) continue;
            Block block = world.getBlockAt(x,y,z);
            if (block.getType() != Material.END_STONE || !block.getRelative(BlockFace.UP).getType().isAir()) continue;
            block.setType(ORE_BLOCK, false); displays.show(block, "ore", items.create("ore")); found.add(encode(block));
        }
        pdc.set(locations, PersistentDataType.INTEGER_ARRAY, found.stream().mapToInt(Integer::intValue).toArray());
    }
    private int encode(Block b) { return MithrylRules.encode(b.getX(),b.getY(),b.getZ(),b.getWorld().getMinHeight()); }
    public boolean isOre(Block block) {
        if (block.getType() != ORE_BLOCK || block.getWorld().getEnvironment() != World.Environment.THE_END) return false;
        int[] entries = block.getChunk().getPersistentDataContainer().get(locations,PersistentDataType.INTEGER_ARRAY);
        return entries != null && Arrays.stream(entries).anyMatch(v -> v == encode(block));
    }
    private void forget(Block b) {
        var pdc=b.getChunk().getPersistentDataContainer(); int[] entries=pdc.get(locations,PersistentDataType.INTEGER_ARRAY);
        if(entries!=null) pdc.set(locations,PersistentDataType.INTEGER_ARRAY,Arrays.stream(entries).filter(v -> v!=encode(b)).toArray());
        displays.remove(b);
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void breakOre(BlockBreakEvent e) {
        if (!isOre(e.getBlock())) return;
        ItemStack tool=e.getPlayer().getInventory().getItemInMainHand();
        if (e.getPlayer().getGameMode()!=GameMode.CREATIVE && tool.getType()!=Material.NETHERITE_PICKAXE) {
            e.setCancelled(true); e.getPlayer().sendActionBar(net.kyori.adventure.text.Component.text("Mithryl braucht eine Netherite- oder Mithryl-Spitzhacke.")); return;
        }
        e.setDropItems(false); e.setExpToDrop(0);
    }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void dropOre(BlockBreakEvent e) {
        if(!isOre(e.getBlock()))return;
        forget(e.getBlock());
        if(e.getPlayer().getGameMode()==GameMode.CREATIVE)return;
        ItemStack shard=items.create("shard");
        int level=e.getPlayer().getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.FORTUNE);
        int amount=MithrylRules.fortuneDrops(level,ThreadLocalRandom.current());
        // Silk Touch deliberately still drops shards; ores cannot become a renewable resource.
        while(amount>0) { shard.setAmount(Math.min(amount,64)); e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(.5,.5,.5),shard); amount-=shard.getAmount(); }
    }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void piston(BlockPistonExtendEvent e) { if(e.getBlocks().stream().anyMatch(this::isOre))e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void piston(BlockPistonRetractEvent e) { if(e.getBlocks().stream().anyMatch(this::isOre))e.setCancelled(true); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void explode(EntityExplodeEvent e) { e.blockList().removeIf(this::isOre); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void explode(BlockExplodeEvent e) { e.blockList().removeIf(this::isOre); }
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true) public void entityChange(EntityChangeBlockEvent e) { if(isOre(e.getBlock()))e.setCancelled(true); }
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true) public void place(BlockPlaceEvent e) { forget(e.getBlock()); }
}
