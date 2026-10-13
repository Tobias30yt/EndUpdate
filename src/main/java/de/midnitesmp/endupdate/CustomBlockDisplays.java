package de.midnitesmp.endupdate;

import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Shows namespaced custom block models only at plugin-marked block locations. */
public final class CustomBlockDisplays implements Listener {
    private static final int VIEW_RADIUS_CHUNKS = 4;
    private final NamespacedKey marker;
    private final NamespacedKey position;
    private final NamespacedKey blockId;
    private final MithrylItems items;
    private final EndUpdatePlugin plugin;
    private final Set<Location> pendingChanges = new HashSet<>();
    private final Set<ChunkRef> activeChunks = new HashSet<>();
    private boolean initialized;

    private record ChunkRef(World world, int x, int z) {}

    public CustomBlockDisplays(EndUpdatePlugin plugin, MithrylItems items) {
        this.plugin = plugin;
        marker = plugin.key("custom_block_display");
        position = plugin.key("custom_block_position");
        blockId = plugin.key("custom_block_id");
        this.items = items;
        Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 20L, 20L);
    }

    private int encode(Block block) {
        return MithrylRules.encode(block.getX(), block.getY(), block.getZ(), block.getWorld().getMinHeight());
    }

    public void show(Block block, String id, ItemStack stack) {
        if (!isWithinRange(block.getChunk())) {
            remove(block);
            return;
        }
        int encoded = encode(block);
        for (Entity entity : block.getChunk().getEntities()) {
            Integer at = entity.getPersistentDataContainer().get(position, PersistentDataType.INTEGER);
            if (entity.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)
                    && at != null && at == encoded) {
                if (entity instanceof ItemDisplay display) {
                    ItemStack wanted = displayStack(block, stack);
                    if (!display.getItemStack().equals(wanted)) display.setItemStack(wanted);
                    if (display.getItemDisplayTransform()!=ItemDisplay.ItemDisplayTransform.NONE) display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    if (!display.getLocation().equals(displayLocation(block))) display.teleport(displayLocation(block));
                    var transform = transformation(block);
                    if (!display.getTransformation().equals(transform)) display.setTransformation(transform);
                }
                return;
            }
        }
        block.getWorld().spawn(displayLocation(block), ItemDisplay.class, display -> {
            display.setItemStack(displayStack(block, stack));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            display.setPersistent(true);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setSilent(true);
            display.setTransformation(transformation(block));
            var data = display.getPersistentDataContainer();
            data.set(marker, PersistentDataType.BYTE, (byte) 1);
            data.set(position, PersistentDataType.INTEGER, encoded);
            data.set(blockId, PersistentDataType.STRING, id);
        });
    }

    private ItemStack displayStack(Block block, ItemStack stack) {
        var state = HingedBlockModels.of(block.getBlockData());
        if (state == null) return stack;
        ItemStack displayItem = stack.clone();
        ItemMeta meta = displayItem.getItemMeta();
        meta.setItemModel(new NamespacedKey(marker.getNamespace(), state.model()));
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private org.bukkit.Location displayLocation(Block block) {
        return block.getLocation().add(.5, .5, .5);
    }

    private org.bukkit.util.Transformation transformation(Block block) {
        var state = HingedBlockModels.of(block.getBlockData());
        if (state != null) return state.transformation();
        var rotation = new org.joml.Quaternionf();
        float scale = 1.05f;
        if (block.getBlockData() instanceof Orientable orientable) {
            switch (orientable.getAxis()) {
                case X -> rotation.rotationZ((float) (Math.PI / 2));
                case Z -> rotation.rotationX((float) (Math.PI / 2));
                default -> { }
            }
        }
        return new org.bukkit.util.Transformation(new org.joml.Vector3f(), rotation,
            new org.joml.Vector3f(scale, scale, scale), new org.joml.Quaternionf());
    }

    public void remove(Block block) {
        int encoded = encode(block);
        Arrays.stream(block.getChunk().getEntities()).forEach(entity -> {
            Integer at = entity.getPersistentDataContainer().get(position, PersistentDataType.INTEGER);
            if (entity.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)
                    && Objects.equals(at, encoded)) entity.remove();
        });
    }

    private void removeDisplays(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    public boolean hasDisplay(Block block) {
        int encoded = encode(block);
        return Arrays.stream(block.getChunk().getEntities()).anyMatch(entity -> {
            Integer at = entity.getPersistentDataContainer().get(position, PersistentDataType.INTEGER);
            return entity.getPersistentDataContainer().has(marker, PersistentDataType.BYTE)
                && Objects.equals(at, encoded);
        });
    }

    public void restore(Block block, String id) {
        if (id.isEmpty()) return;
        show(block, id, items.create(id));
    }

    private static boolean hinged(String id) { return "end_door".equals(id) || "end_trapdoor".equals(id); }

    private void refreshHinged(Block block) {
        String id = block.getChunk().getPersistentDataContainer()
            .get(pluginKey("wood_"+Integer.toHexString(encode(block))), PersistentDataType.STRING);
        if (hinged(id) && EndWood.MATERIALS.get(id) == block.getType()) restore(block,id);
        else remove(block);
    }

    private void queueHinged(Block block) {
        if (block.getType()!=org.bukkit.Material.CHERRY_DOOR && block.getType()!=org.bukkit.Material.CHERRY_TRAPDOOR) return;
        String id = block.getChunk().getPersistentDataContainer()
            .get(pluginKey("wood_"+Integer.toHexString(encode(block))), PersistentDataType.STRING);
        if (!hinged(id)) return;
        queueLocation(block.getLocation());
        if (block.getBlockData() instanceof Door door)
            queueLocation(block.getLocation().add(0,door.getHalf()==Door.Half.TOP?-1:1,0));
    }

    private void queueLocation(Location location) {
        if (!pendingChanges.add(location)) return;
        // Read the committed state, after vanilla/other plugins have processed the click or power.
        Bukkit.getScheduler().runTask(plugin,()->{
            pendingChanges.remove(location);
            if (location.isWorldLoaded() && location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4))
                refreshHinged(location.getBlock());
        });
    }

    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void interact(PlayerInteractEvent event) {
        if (event.getAction()==Action.RIGHT_CLICK_BLOCK && event.getClickedBlock()!=null && event.useInteractedBlock()!=Event.Result.DENY)
            queueHinged(event.getClickedBlock());
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void redstone(BlockRedstoneEvent event) { queueHinged(event.getBlock()); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void physics(BlockPhysicsEvent event) { queueHinged(event.getBlock()); }

    private void refreshHingedDisplays(Chunk chunk) {
        // Catch state changes made directly by other plugins without a click/redstone event.
        for (Entity entity : chunk.getEntities()) {
            var data = entity.getPersistentDataContainer();
            if (!data.has(marker,PersistentDataType.BYTE) || !hinged(data.get(blockId,PersistentDataType.STRING))) continue;
            Integer encoded = data.get(position,PersistentDataType.INTEGER);
            if (encoded != null) refreshHinged(blockAt(chunk,encoded));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (isWithinRange(event.getChunk())) {
            restoreChunk(event.getChunk());
        } else {
            removeDisplays(event.getChunk());
        }
    }

    private boolean isWithinRange(Chunk chunk) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != chunk.getWorld()) continue;
            long dx = (player.getLocation().getBlockX() >> 4) - (long) chunk.getX();
            long dz = (player.getLocation().getBlockZ() >> 4) - (long) chunk.getZ();
            if (dx * dx + dz * dz <= VIEW_RADIUS_CHUNKS * VIEW_RADIUS_CHUNKS) return true;
        }
        return false;
    }

    private void refresh() {
        Set<ChunkRef> desired = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            int centerX = player.getLocation().getBlockX() >> 4;
            int centerZ = player.getLocation().getBlockZ() >> 4;
            for (int dx = -VIEW_RADIUS_CHUNKS; dx <= VIEW_RADIUS_CHUNKS; dx++) {
                for (int dz = -VIEW_RADIUS_CHUNKS; dz <= VIEW_RADIUS_CHUNKS; dz++) {
                    if (dx * dx + dz * dz <= VIEW_RADIUS_CHUNKS * VIEW_RADIUS_CHUNKS) {
                        desired.add(new ChunkRef(player.getWorld(), centerX + dx, centerZ + dz));
                    }
                }
            }
        }

        if (!initialized) {
            for (World world : Bukkit.getWorlds()) {
                for (Chunk chunk : world.getLoadedChunks()) {
                    if (!desired.contains(new ChunkRef(world, chunk.getX(), chunk.getZ()))) {
                        removeDisplays(chunk);
                    }
                }
            }
            initialized = true;
        }

        for (ChunkRef previous : activeChunks) {
            if (!desired.contains(previous) && previous.world().isChunkLoaded(previous.x(), previous.z())) {
                removeDisplays(previous.world().getChunkAt(previous.x(), previous.z()));
            }
        }
        for (ChunkRef current : desired) {
            if (current.world().isChunkLoaded(current.x(), current.z())) {
                Chunk chunk = current.world().getChunkAt(current.x(), current.z());
                if (!activeChunks.contains(current)) restoreChunk(chunk);
                else refreshHingedDisplays(chunk);
            }
        }
        activeChunks.clear();
        activeChunks.addAll(desired);
    }

    private void restoreChunk(Chunk chunk) {
        var data = chunk.getPersistentDataContainer();
        for (NamespacedKey key : data.getKeys()) {
            if (key.getNamespace().equals(marker.getNamespace()) && key.getKey().startsWith("station_") && !key.getKey().startsWith("station_owner_")) {
                String station = data.get(key, PersistentDataType.STRING);
                if (station != null && MithrylWorkstations.MATERIALS.containsKey(station)) {
                    int at = Integer.parseUnsignedInt(key.getKey().substring(8), 16);
                    Block block = blockAt(chunk, at);
                    if (block.getType() == MithrylWorkstations.MATERIALS.get(station)) restore(block, station);
                }
            }
            if (!key.getNamespace().equals(marker.getNamespace()) || !key.getKey().startsWith("wood_")) continue;
            String id = data.get(key, PersistentDataType.STRING);
            if (id == null || !EndWood.MATERIALS.containsKey(id)) continue;
            int encoded = Integer.parseUnsignedInt(key.getKey().substring("wood_".length()), 16);
            Block block = blockAt(chunk, encoded);
            if (block.getType() == EndWood.MATERIALS.get(id)) restore(block, id);
        }
        restoreLocations(chunk, "ore_locations", MithrylOre.ORE_BLOCK, "ore");
        restoreLocations(chunk, "storage_blocks", MithrylBlocks.STORAGE_BLOCK, "block");
    }

    private void restoreLocations(Chunk chunk, String keyName, org.bukkit.Material material, String id) {
        int[] locations = chunk.getPersistentDataContainer()
            .get(pluginKey(keyName), PersistentDataType.INTEGER_ARRAY);
        if (locations == null) return;
        for (int encoded : locations) {
            Block block = blockAt(chunk, encoded);
            if (block.getType() == material) restore(block, id);
        }
    }

    private NamespacedKey pluginKey(String key) {
        return new NamespacedKey(marker.getNamespace(), key);
    }

    private Block blockAt(Chunk chunk, int encoded) {
        return chunk.getBlock(encoded & 15, (encoded >> 8) + chunk.getWorld().getMinHeight(),
            (encoded >> 4) & 15);
    }
}
