package de.midnitesmp.endupdate;

import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private final Set<ChunkRef> activeChunks = new HashSet<>();
    private boolean initialized;

    private record ChunkRef(World world, int x, int z) {}

    public CustomBlockDisplays(EndUpdatePlugin plugin, MithrylItems items) {
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
                    display.setItemStack(displayStack(block, stack));
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    display.teleport(displayLocation(block));
                    display.setTransformation(transformation(block));
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
        if (!(block.getBlockData() instanceof Door door)) return stack;
        ItemStack displayItem = stack.clone();
        ItemMeta meta = displayItem.getItemMeta();
        meta.setItemModel(new NamespacedKey(marker.getNamespace(),
            door.getHalf() == Door.Half.TOP ? "end_door_display_top" : "end_door_display_bottom"));
        displayItem.setItemMeta(meta);
        return displayItem;
    }

    private org.bukkit.Location displayLocation(Block block) {
        return block.getLocation().add(.5, .5, .5);
    }

    private org.bukkit.util.Transformation transformation(Block block) {
        var rotation = new org.joml.Quaternionf();
        float scale = 1.05f;
        if (block.getBlockData() instanceof Door door) {
            float yaw = switch (door.getFacing()) {
                case SOUTH -> 0;
                case WEST -> (float) (Math.PI / 2);
                case NORTH -> (float) Math.PI;
                case EAST -> (float) (Math.PI * 1.5);
                default -> 0;
            };
            if (door.isOpen()) {
                yaw += door.getHinge() == Door.Hinge.RIGHT
                    ? (float) (Math.PI / 2) : (float) (-Math.PI / 2);
            }
            rotation.rotationY(yaw);
            scale = 1.0f;
        } else if (block.getBlockData() instanceof Orientable orientable) {
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
            if (!activeChunks.contains(current) && current.world().isChunkLoaded(current.x(), current.z())) {
                restoreChunk(current.world().getChunkAt(current.x(), current.z()));
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
