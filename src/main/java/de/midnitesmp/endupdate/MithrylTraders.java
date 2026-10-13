package de.midnitesmp.endupdate;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Stationary village residents: an ArmorStand hitbox with one custom ItemDisplay model. */
public final class MithrylTraders implements Listener {
    public enum Job {
        NONE("Bewohner", ""), SMITH("Mithrylschmied", "mithryl_forge"),
        GARDENER("Hainhueter", "grove_bench"), RELIC("Relikthaendler", "relic_altar");
        public final String title, station;
        Job(String title,String station){this.title=title;this.station=station;}
        public static Job forStation(String id){return Arrays.stream(values()).filter(j->j.station.equals(id)).findFirst().orElse(NONE);}
    }
    private static final long RESTOCK_MS=20*60*1000L;
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final MithrylWorkstations stations;
    private final Map<UUID,ArmorStand> residents=new HashMap<>();
    private final Map<UUID,Merchant> merchants=new HashMap<>();

    public MithrylTraders(EndUpdatePlugin plugin,MithrylItems items,MithrylWorkstations stations){
        this.plugin=plugin;this.items=items;this.stations=stations;
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            for(ArmorStand stand:List.copyOf(residents.values())) {
                if(!stand.isValid()){residents.remove(stand.getUniqueId());continue;}
                refreshJob(stand); updateModel(stand);
                Merchant merchant=merchants.get(stand.getUniqueId());
                if(merchant!=null && merchant.getTrader() instanceof Player player && !canTrade(player,stand)) player.closeInventory();
            }
        },40,40);
    }
    private NamespacedKey key(String name){return plugin.key("trader_"+name);}
    public boolean isTrader(Entity entity){return entity instanceof ArmorStand && entity.getPersistentDataContainer().has(key("resident"),PersistentDataType.BYTE);}
    private boolean isPart(Entity entity){return isTrader(entity)||entity.getPersistentDataContainer().has(key("visual"),PersistentDataType.BYTE);}
    public Job job(ArmorStand stand){
        try{return Job.valueOf(stand.getPersistentDataContainer().getOrDefault(key("job"),PersistentDataType.STRING,"NONE"));}
        catch(IllegalArgumentException ex){return Job.NONE;}
    }
    public ArmorStand spawn(Location location){
        ArmorStand stand=location.getWorld().spawn(location,ArmorStand.class,e->{
            e.setVisible(false);e.setMarker(false);e.setGravity(false);e.setInvulnerable(true);
            e.setSilent(true);e.setPersistent(true);e.setRemoveWhenFarAway(false);e.setBasePlate(false);e.setCanMove(false);
            e.setDisabledSlots(EquipmentSlot.values());
            e.getPersistentDataContainer().set(key("resident"),PersistentDataType.BYTE,(byte)1);
        });
        residents.put(stand.getUniqueId(),stand);refreshJob(stand);updateModel(stand);return stand;
    }
    public void load(Chunk chunk){
        if(chunk.getWorld().getEnvironment()!=World.Environment.THE_END)return;
        for(Entity entity:chunk.getEntities())if(isTrader(entity))residents.put(entity.getUniqueId(),(ArmorStand)entity);
        populate(chunk);
    }
    @EventHandler public void loaded(EntitiesLoadEvent e){load(e.getChunk());}
    @EventHandler public void chunkLoaded(ChunkLoadEvent e){
        Bukkit.getScheduler().runTask(plugin,()->{if(e.getWorld().isChunkLoaded(e.getChunk().getX(),e.getChunk().getZ()))load(e.getChunk());});
    }
    @EventHandler public void unloaded(EntitiesUnloadEvent e){
        for(Entity entity:e.getEntities())if(isTrader(entity)){close((ArmorStand)entity);residents.remove(entity.getUniqueId());}
    }
    public void populateVillage(Location center){
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            int x=(center.getBlockX()>>4)+dx,z=(center.getBlockZ()>>4)+dz;
            if(center.getWorld().isChunkLoaded(x,z))populate(center.getWorld().getChunkAt(x,z));
        }
    }
    /** Per-house markers prevent respawns or new free stations after a restart or block removal. */
    private void populate(Chunk chunk){
        int[] villages=chunk.getWorld().getPersistentDataContainer().getOrDefault(plugin.key("end_villages_v1"),PersistentDataType.INTEGER_ARRAY,new int[0]);
        int[][] houses={{-8,-8},{8,-8},{-8,8},{8,8}};
        Job[] jobs={Job.SMITH,Job.GARDENER,Job.RELIC,Job.NONE};
        for(int i=0;i+2<villages.length;i+=3)for(int slot=0;slot<4;slot++){
            int x=villages[i]+houses[slot][0],y=villages[i+1],z=villages[i+2]+houses[slot][1];
            if((x>>4)!=chunk.getX()||(z>>4)!=chunk.getZ())continue;
            var done=key("house_"+villages[i]+"_"+villages[i+2]+"_"+slot);
            if(chunk.getPersistentDataContainer().has(done))continue;
            World world=chunk.getWorld();Block feet=world.getBlockAt(x,y,z);
            if(!feet.getType().isAir()||!feet.getRelative(0,1,0).getType().isAir()||!feet.getRelative(0,-1,0).getType().isSolid())continue;
            int front=houses[slot][1]<0?1:-1;
            Block station=world.getBlockAt(x+2,y,z-2*front);
            // Existing villages are upgraded only at the original, untouched crafting table.
            if(jobs[slot]!=Job.NONE&&station.getType()==Material.CRAFTING_TABLE)stations.set(station,jobs[slot].station);
            spawn(new Location(world,x+.5,y,z+.5,front>0?0:180,0));
            chunk.getPersistentDataContainer().set(done,PersistentDataType.BYTE,(byte)1);
        }
    }
    private Block workplace(ArmorStand stand){
        int[] pos=stand.getPersistentDataContainer().get(key("workplace"),PersistentDataType.INTEGER_ARRAY);
        if(pos==null||pos.length!=3||!stand.getWorld().isChunkLoaded(pos[0]>>4,pos[2]>>4))return null;
        return stand.getWorld().getBlockAt(pos[0],pos[1],pos[2]);
    }
    private boolean validWorkplace(ArmorStand stand){
        Block block=workplace(stand);
        return block!=null&&job(stand)!=Job.NONE&&stations.id(block).equals(job(stand).station)
            &&stations.owner(block).equals(stand.getUniqueId().toString())&&block.getLocation().distanceSquared(stand.getLocation())<=64;
    }
    public void refreshJob(ArmorStand stand){
        if(validWorkplace(stand)){restock(stand);return;}
        // A temporarily unloaded neighbour chunk is not a removed workstation.
        int[] previous=stand.getPersistentDataContainer().get(key("workplace"),PersistentDataType.INTEGER_ARRAY);
        if(previous!=null&&previous.length==3&&!stand.getWorld().isChunkLoaded(previous[0]>>4,previous[2]>>4))return;
        Block old=workplace(stand);if(old!=null&&stations.owner(old).equals(stand.getUniqueId().toString()))stations.release(old);
        close(stand);stand.getPersistentDataContainer().remove(key("workplace"));
        boolean locked=stand.getPersistentDataContainer().has(key("locked"));Job required=locked?job(stand):Job.NONE;
        if(!locked)stand.getPersistentDataContainer().set(key("job"),PersistentDataType.STRING,Job.NONE.name());
        for(Block candidate:stations.nearby(stand.getLocation(),8)){
            if(!stations.owner(candidate).isEmpty())continue;
            Job next=Job.forStation(stations.id(candidate));if(required!=Job.NONE&&next!=required)continue;
            stations.claim(candidate,stand.getUniqueId());
            stand.getPersistentDataContainer().set(key("job"),PersistentDataType.STRING,next.name());
            stand.getPersistentDataContainer().set(key("workplace"),PersistentDataType.INTEGER_ARRAY,new int[]{candidate.getX(),candidate.getY(),candidate.getZ()});
            restock(stand);break;
        }
    }
    private void restock(ArmorStand stand){
        var pdc=stand.getPersistentDataContainer();long now=System.currentTimeMillis();
        NamespacedKey next=key("restock_"+job(stand).name().toLowerCase(Locale.ROOT));
        long due=pdc.getOrDefault(next,PersistentDataType.LONG,0L);
        if(due==0){pdc.set(next,PersistentDataType.LONG,now+RESTOCK_MS);return;}
        Merchant merchant=merchants.get(stand.getUniqueId());
        if(now<due || (merchant!=null&&merchant.isTrading()))return;
        pdc.remove(stockKey(stand));pdc.set(next,PersistentDataType.LONG,now+RESTOCK_MS);merchants.remove(stand.getUniqueId());
    }
    private NamespacedKey stockKey(ArmorStand stand){return key("uses_"+job(stand).name().toLowerCase(Locale.ROOT));}
    private ItemStack stack(String id,int count){ItemStack out=items.create(id);out.setAmount(count);return out;}
    private MerchantRecipe offer(ItemStack result,int max,ItemStack... cost){
        MerchantRecipe recipe=new MerchantRecipe(result,0,max,true,2,0f);recipe.setIgnoreDiscounts(true);recipe.setIngredients(List.of(cost));return recipe;
    }
    public List<MerchantRecipe> offers(Job job){
        return switch(job){
            case SMITH -> List.of(
                offer(stack("shard",1),12,new ItemStack(Material.DIAMOND,2)),
                offer(stack("ingot",1),8,stack("shard",4),new ItemStack(Material.DIAMOND,4)),
                offer(stack("template",1),2,stack("ingot",6),new ItemStack(Material.DIAMOND,12)),
                offer(new ItemStack(Material.EXPERIENCE_BOTTLE,8),6,stack("shard",3)));
            case GARDENER -> List.of(
                offer(stack("shard",1),8,stack("end_log",16)),
                offer(stack("end_sapling",3),12,stack("shard",2)),
                offer(stack("bloom_wand",1),2,stack("ingot",4),stack("end_sapling",4)),
                offer(stack("crystal_fruit",3),8,stack("shard",3),new ItemStack(Material.CHORUS_FRUIT,6)));
            case RELIC -> List.of(
                offer(stack("feather_charm",1),2,stack("ingot",8),new ItemStack(Material.PHANTOM_MEMBRANE,4)),
                offer(new ItemStack(Material.SHULKER_SHELL,1),4,stack("ingot",3),new ItemStack(Material.ENDER_PEARL,8)),
                offer(new ItemStack(Material.DRAGON_BREATH,2),4,stack("shard",6),new ItemStack(Material.GLASS_BOTTLE,2)),
                offer(stack("crystal_fruit",2),8,stack("shard",4)));
            default -> List.of();
        };
    }
    public Merchant merchant(ArmorStand stand){
        return merchants.computeIfAbsent(stand.getUniqueId(),ignored->{
            Merchant merchant=Bukkit.createMerchant(Component.text(job(stand).title));
            List<MerchantRecipe> offers=offers(job(stand));
            int[] used=stand.getPersistentDataContainer().getOrDefault(stockKey(stand),PersistentDataType.INTEGER_ARRAY,new int[0]);
            for(int i=0;i<Math.min(used.length,offers.size());i++)offers.get(i).setUses(Math.clamp(used[i],0,offers.get(i).getMaxUses()));
            merchant.setRecipes(offers);return merchant;
        });
    }
    private void save(ArmorStand stand){
        Merchant merchant=merchants.get(stand.getUniqueId());if(merchant==null)return;
        stand.getPersistentDataContainer().set(stockKey(stand),PersistentDataType.INTEGER_ARRAY,merchant.getRecipes().stream().mapToInt(MerchantRecipe::getUses).toArray());
    }
    private void close(ArmorStand stand){
        Merchant merchant=merchants.get(stand.getUniqueId());
        if(merchant!=null&&merchant.getTrader()!=null)merchant.getTrader().closeInventory();
        save(stand);merchants.remove(stand.getUniqueId());
    }
    public void shutdown(){for(ArmorStand stand:List.copyOf(residents.values()))close(stand);}
    private void updateModel(ArmorStand stand){
        String model="villager_"+job(stand).name().toLowerCase(Locale.ROOT);
        stand.customName(Component.text("Mithryl-"+job(stand).title));stand.setCustomNameVisible(false);
        ItemDisplay display=Arrays.stream(stand.getChunk().getEntities())
            .filter(e->e instanceof ItemDisplay && stand.getUniqueId().toString().equals(e.getPersistentDataContainer().get(key("parent"),PersistentDataType.STRING)))
            .map(e->(ItemDisplay)e).findFirst().orElse(null);
        if(display==null){
            display=stand.getWorld().spawn(stand.getLocation().add(0,.5,0),ItemDisplay.class,e->{
                e.setPersistent(true);e.setInvulnerable(true);e.setGravity(false);e.setSilent(true);
                e.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                e.setDisplayWidth(1.3f);e.setDisplayHeight(2.2f);e.setViewRange(.75f);
                e.getPersistentDataContainer().set(key("visual"),PersistentDataType.BYTE,(byte)1);
                e.getPersistentDataContainer().set(key("parent"),PersistentDataType.STRING,stand.getUniqueId().toString());
            });
        }
        display.teleport(stand.getLocation().add(0,.5,0));
        if(display.getItemStack().getItemMeta()!=null && plugin.key(model).equals(display.getItemStack().getItemMeta().getItemModel()))return;
        ItemStack item=new ItemStack(Material.PAPER);var meta=item.getItemMeta();meta.setItemModel(plugin.key(model));item.setItemMeta(meta);display.setItemStack(item);
    }
    private boolean canTrade(Player player,ArmorStand stand){return stand.isValid()&&player.getWorld()==stand.getWorld()&&player.getLocation().distanceSquared(stand.getLocation())<=64&&validWorkplace(stand)&&player.getGameMode()!=GameMode.SPECTATOR;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interact(PlayerInteractEntityEvent event){
        if(!isTrader(event.getRightClicked()))return;event.setCancelled(true);
        if(event.getHand()!=EquipmentSlot.HAND)return;
        ArmorStand stand=(ArmorStand)event.getRightClicked();Player player=event.getPlayer();refreshJob(stand);updateModel(stand);
        if(!canTrade(player,stand)){player.sendMessage("§dDieser Bewohner braucht einen freien Arbeitsplatz in seiner Naehe.");return;}
        Merchant merchant=merchant(stand);
        if(merchant.isTrading()){if(merchant.getTrader()!=player)player.sendMessage("§7Dieser Haendler handelt gerade. Einen Moment bitte.");return;}
        player.openMerchant(merchant,false);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void interactAt(PlayerInteractAtEntityEvent event){interact(event);}
    @EventHandler(priority=EventPriority.HIGHEST) public void manipulate(PlayerArmorStandManipulateEvent e){if(isTrader(e.getRightClicked()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageEvent e){if(isPart(e.getEntity()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void portal(EntityPortalEvent e){if(isPart(e.getEntity()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST)
    public void purchase(PlayerPurchaseEvent event){
        ArmorStand stand=owner(event.getMerchant());if(stand==null)return;
        if(!canTrade(event.getPlayer(),stand)||!(event.getPlayer().getOpenInventory().getTopInventory() instanceof MerchantInventory inventory)) {event.setCancelled(true);return;}
        // Verify exact metadata as well as material. Plain prismarine or renamed imitations never pay.
        List<ItemStack> cost=event.getTrade().getIngredients();
        if(!matches(cost,inventory.getItem(0),inventory.getItem(1))&&!matches(cost,inventory.getItem(1),inventory.getItem(0)))event.setCancelled(true);
        if(event.getTrade().getUses()>=event.getTrade().getMaxUses())event.setCancelled(true);
        event.setIncreaseTradeUses(true);
    }
    public static boolean matches(List<ItemStack> cost,ItemStack first,ItemStack second){
        return cost.size()>=1&&matches(cost.getFirst(),first)&&(cost.size()==1||matches(cost.get(1),second));
    }
    private static boolean matches(ItemStack cost,ItemStack actual){return actual!=null&&cost.isSimilar(actual)&&actual.getAmount()>=cost.getAmount();}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void purchased(PlayerPurchaseEvent event){
        ArmorStand stand=owner(event.getMerchant());if(stand==null)return;
        stand.getPersistentDataContainer().set(key("locked"),PersistentDataType.BYTE,(byte)1);
        Bukkit.getScheduler().runTask(plugin,()->save(stand));
    }
    private ArmorStand owner(Merchant merchant){
        for(var entry:merchants.entrySet())if(entry.getValue()==merchant)return residents.get(entry.getKey());return null;
    }
    @EventHandler public void closed(InventoryCloseEvent event){
        if(event.getInventory() instanceof MerchantInventory inventory){ArmorStand stand=owner(inventory.getMerchant());if(stand!=null)save(stand);}
    }
}
