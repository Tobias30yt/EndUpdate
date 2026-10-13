package de.midnitesmp.endupdate;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Cherry-backed blocks retain their custom identity in chunk PDC across restarts. */
public final class EndWood implements Listener {
    public static final Map<String, Material> MATERIALS = new LinkedHashMap<>();
    static {
        for (String kind : List.of("log","wood","stripped_log","stripped_wood","planks","stairs","slab","fence","fence_gate","door","trapdoor","button","pressure_plate","leaves","sapling")) {
            String material = kind.equals("sapling") ? "CHERRY_SAPLING"
                : kind.startsWith("stripped_") ? "STRIPPED_CHERRY_"+kind.substring(9).toUpperCase(Locale.ROOT)
                : "CHERRY_"+kind.toUpperCase(Locale.ROOT);
            MATERIALS.put("end_"+kind, Material.valueOf(material));
        }
    }
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final CustomBlockDisplays displays;
    private boolean dispatchingGrowth;
    private final Set<Location> saplings = new HashSet<>();
    public EndWood(EndUpdatePlugin plugin, MithrylItems items, CustomBlockDisplays displays) {
        this.plugin=plugin; this.items=items; this.displays=displays;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Location location : List.copyOf(saplings)) {
                if (!location.isWorldLoaded() || !location.getWorld().isChunkLoaded(location.getBlockX()>>4,location.getBlockZ()>>4)) { saplings.remove(location); continue; }
                if (!id(location.getBlock()).equals("end_sapling")) { saplings.remove(location); continue; }
                if (ThreadLocalRandom.current().nextInt(3)==0) grow(location.getBlock(),false,null);
            }
        },600,600);
    }
    public static ItemStack item(EndUpdatePlugin plugin,String id) {
        ItemStack item=new ItemStack(MATERIALS.get(id)); var meta=item.getItemMeta();
        meta.getPersistentDataContainer().set(plugin.key("item"),PersistentDataType.STRING,id);
        meta.itemName(Component.text(Arrays.stream(id.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).reduce((a,b)->a+" "+b).orElse(id),NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC,false));
        meta.setItemModel(plugin.key(id)); item.setItemMeta(meta); return item;
    }
    private NamespacedKey key(Block b) { return plugin.key("wood_"+Integer.toHexString(MithrylRules.encode(b.getX(),b.getY(),b.getZ(),b.getWorld().getMinHeight()))); }
    public String id(Block b) {
        String id=b.getChunk().getPersistentDataContainer().get(key(b),PersistentDataType.STRING);
        return id!=null && MATERIALS.get(id)==b.getType() ? id : "";
    }
    public void mark(Block b,String id) {
        b.getChunk().getPersistentDataContainer().set(key(b),PersistentDataType.STRING,id);
        displays.show(b,id,item(plugin,id));
        if(id.equals("end_sapling"))saplings.add(b.getLocation());
    }
    private void forget(Block b) { b.getChunk().getPersistentDataContainer().remove(key(b)); displays.remove(b); saplings.remove(b.getLocation()); }
    public void set(Block b,String id) { b.setType(MATERIALS.get(id),false); mark(b,id); }
    @EventHandler public void load(org.bukkit.event.world.ChunkLoadEvent e) {
        // Only marked saplings need a timer; trees/wood remain entirely in the chunk data.
        var pdc=e.getChunk().getPersistentDataContainer();
        for(NamespacedKey key:pdc.getKeys()) if(key.getNamespace().equals(plugin.getName().toLowerCase(Locale.ROOT))&&key.getKey().startsWith("wood_")) {
            String id=pdc.get(key,PersistentDataType.STRING);
            int encoded=Integer.parseUnsignedInt(key.getKey().substring(5),16);
            Block b=e.getChunk().getBlock(encoded&15,(encoded>>8)+e.getWorld().getMinHeight(),(encoded>>4)&15);
            if(id!=null&&MATERIALS.get(id)==b.getType())displays.restore(b,id);
            if("end_sapling".equals(id)&&id(b).equals("end_sapling"))saplings.add(b.getLocation());
        }
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void place(BlockPlaceEvent e) {
        Block block=e.getBlock();
        String id=items.id(e.getItemInHand());
        if(MATERIALS.get(id)!=block.getType()) { forget(block); return; }
        Bukkit.getScheduler().runTask(plugin,()->{
            if(MATERIALS.get(id)!=block.getType())return;
            mark(block,id);
            if(block.getBlockData() instanceof Door door)
                mark(block.getRelative(door.getHalf()==Bisected.Half.TOP?BlockFace.DOWN:BlockFace.UP),id);
        });
    }
    private List<ItemStack> drops(Block b,ItemStack tool) {
        String id=id(b); if(id.isEmpty())return List.of();
        if(id.equals("end_leaves") && tool.getType()!=Material.SHEARS && !tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            int fortune=Math.clamp(tool.getEnchantmentLevel(Enchantment.FORTUNE),0,3);
            if(ThreadLocalRandom.current().nextDouble()<new double[]{.05,.0625,.0833,.10}[fortune]) return List.of(items.create("end_sapling"));
            if(ThreadLocalRandom.current().nextDouble()<.02) return List.of(new ItemStack(Material.STICK,1+ThreadLocalRandom.current().nextInt(2)));
            return List.of();
        }
        ItemStack result=items.create(id);
        if(b.getBlockData() instanceof Slab slab && slab.getType()==Slab.Type.DOUBLE)result.setAmount(2);
        return List.of(result);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breaking(BlockBreakEvent e) { if(!id(e.getBlock()).isEmpty())e.setDropItems(false); }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void broken(BlockBreakEvent e) {
        Block b=e.getBlock(); if(id(b).isEmpty())return;
        if(e.getPlayer().getGameMode()!=GameMode.CREATIVE) drops(b,e.getPlayer().getInventory().getItemInMainHand()).forEach(i->b.getWorld().dropItemNaturally(b.getLocation(),i));
        if(b.getBlockData() instanceof Door door)forget(b.getRelative(door.getHalf()==Bisected.Half.TOP?BlockFace.DOWN:BlockFace.UP));
        forget(b);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void decay(LeavesDecayEvent e) {
        if(!id(e.getBlock()).equals("end_leaves"))return;
        e.setCancelled(true); Block b=e.getBlock();
        drops(b,new ItemStack(Material.AIR)).forEach(i->b.getWorld().dropItemNaturally(b.getLocation(),i));
        forget(b);b.setType(Material.AIR,true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void physics(BlockPhysicsEvent e) {
        Block b=e.getBlock();
        if(!id(b).equals("end_sapling"))return;
        Material below=b.getRelative(BlockFace.DOWN).getType();
        if(below==Material.END_STONE)e.setCancelled(true);
        else if(!Tag.DIRT.isTagged(below)&&below!=Material.GRASS_BLOCK) {
            e.setCancelled(true);forget(b);b.setType(Material.AIR,false);b.getWorld().dropItemNaturally(b.getLocation(),items.create("end_sapling"));
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void interact(PlayerInteractEvent e) {
        if(e.getAction()!=Action.RIGHT_CLICK_BLOCK||e.getClickedBlock()==null||e.useInteractedBlock()==Event.Result.DENY)return;
        Block b=e.getClickedBlock(); ItemStack held=e.getItem(); if(held==null)return;
        String id=id(b);
        if(id.equals("end_sapling")&&held.getType()==Material.BONE_MEAL) {
            e.setCancelled(true);
            if(grow(b,true,e.getPlayer())&&e.getPlayer().getGameMode()!=GameMode.CREATIVE)held.subtract();
        } else if((id.equals("end_log")||id.equals("end_wood"))&&held.getType().name().endsWith("_AXE")) {
            // Vanilla performs stripping and tool wear; update identity only if it actually happened.
            Bukkit.getScheduler().runTask(plugin,()->{String next=id.replace("end_","end_stripped_");if(b.getType()==MATERIALS.get(next))mark(b,next);});
        } else if(items.is(held,"end_sapling")&&b.getType()==Material.END_STONE&&e.getBlockFace()==BlockFace.UP) {
            Block target=b.getRelative(BlockFace.UP); if(!target.getType().isAir())return;
            e.setCancelled(true); BlockState previous=target.getState(); target.setType(Material.CHERRY_SAPLING,false);
            BlockPlaceEvent place=new BlockPlaceEvent(target,previous,b,held,e.getPlayer(),true,e.getHand());
            Bukkit.getPluginManager().callEvent(place);
            if(place.isCancelled()||!place.canBuild()){previous.update(true,false);forget(target);return;}
            mark(target,"end_sapling"); if(e.getPlayer().getGameMode()!=GameMode.CREATIVE)held.subtract();
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void vanillaGrow(StructureGrowEvent e) {
        if(!dispatchingGrowth&&id(e.getLocation().getBlock()).equals("end_sapling")) {
            e.setCancelled(true);Bukkit.getScheduler().runTask(plugin,()->grow(e.getLocation().getBlock(),e.isFromBonemeal(),e.getPlayer()));
        }
    }
    public boolean grow(Block root,boolean bonemeal,Player player) {
        if(!id(root).equals("end_sapling"))return false;
        Map<Block,String> plan=treePlan(root);if(!treeFits(plan,root))return false;
        List<BlockState> states=new ArrayList<>();
        plan.forEach((b,kind)->{BlockState state=b.getState();state.setType(MATERIALS.get(kind));states.add(state);});
        StructureGrowEvent event=new StructureGrowEvent(root.getLocation(),TreeType.CHERRY,bonemeal,player,states);
        dispatchingGrowth=true;try{Bukkit.getPluginManager().callEvent(event);}finally{dispatchingGrowth=false;}
        if(event.isCancelled())return false;
        placeTree(plan);saplings.remove(root.getLocation());return true;
    }
    public boolean tree(Block root) { Map<Block,String> plan=treePlan(root);if(!treeFits(plan,root))return false;placeTree(plan);return true; }
    private Map<Block,String> treePlan(Block root) {
        Map<Block,String> plan=new LinkedHashMap<>();
        for(int y=3;y<=6;y++){int radius=y==6?2:3;for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)
            if(Math.abs(x)+Math.abs(z)<=radius+1)plan.put(root.getRelative(x,y,z),"end_leaves");}
        for(int y=0;y<=5;y++)plan.put(root.getRelative(0,y,0),"end_log");
        for(int sign:new int[]{-1,1}){plan.put(root.getRelative(sign,4,0),"end_log");plan.put(root.getRelative(0,4,sign),"end_log");}
        return plan;
    }
    private boolean treeFits(Map<Block,String> plan,Block root) {
        if(root.getY()<root.getWorld().getMinHeight()+1||root.getY()+7>=root.getWorld().getMaxHeight())return false;
        Material soil=root.getRelative(BlockFace.DOWN).getType();
        if(soil!=Material.END_STONE&&!Tag.DIRT.isTagged(soil)&&soil!=Material.GRASS_BLOCK)return false;
        return plan.keySet().stream().allMatch(b->b.getType().isAir()||(b.equals(root)&&id(b).equals("end_sapling")));
    }
    private void placeTree(Map<Block,String> plan) {
        List<Block> logs=plan.entrySet().stream().filter(e->e.getValue().equals("end_log")).map(Map.Entry::getKey).toList();
        plan.forEach((b,kind)->{set(b,kind);if(b.getBlockData() instanceof Leaves leaves){
            leaves.setPersistent(false);leaves.setDistance(Math.min(7,logs.stream().mapToInt(l->Math.abs(l.getX()-b.getX())+Math.abs(l.getY()-b.getY())+Math.abs(l.getZ()-b.getZ())).min().orElse(7)));b.setBlockData(leaves,false);
        }});
    }
    // Block identities must not be separated from their positions by physics.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(e.getBlocks().stream().anyMatch(b->!id(b).isEmpty()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonRetractEvent e){if(e.getBlocks().stream().anyMatch(b->!id(b).isEmpty()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(EntityExplodeEvent e){e.blockList().removeIf(b->!id(b).isEmpty());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(BlockExplodeEvent e){e.blockList().removeIf(b->!id(b).isEmpty());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e){if(!id(e.getBlock()).isEmpty())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entity(EntityChangeBlockEvent e){if(!id(e.getBlock()).isEmpty())e.setCancelled(true);}
}
