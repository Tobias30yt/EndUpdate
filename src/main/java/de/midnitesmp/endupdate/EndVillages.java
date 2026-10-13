package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import java.util.concurrent.*;

public final class EndVillages implements Listener, CommandExecutor {
    public record Candidate(int x,int z) {}
    private record Pending(World world,int x,int z) {}
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final EndWood wood;
    private final Set<Pending> queue=new LinkedHashSet<>();
    private final Set<Pending> pending=new HashSet<>();
    private boolean locating;
    private MithrylTraders traders;
    public void setTraders(MithrylTraders traders){this.traders=traders;}
    public EndVillages(EndUpdatePlugin plugin,MithrylItems items,EndWood wood) {
        this.plugin=plugin;this.items=items;this.wood=wood;
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            if(queue.isEmpty())return;var it=queue.iterator();Pending p=it.next();it.remove();
            if(p.world.isChunkLoaded(p.x,p.z))process(p.world.getChunkAt(p.x,p.z));
        },20,5);
    }
    public static Candidate candidate(long seed,int rx,int rz,int spacing) {
        Random r=new Random(seed^(rx*341873128712L)^(rz*132897987541L)^0x454E4456494CL);
        return new Candidate(rx*spacing+2+r.nextInt(spacing-4),rz*spacing+2+r.nextInt(spacing-4));
    }
    private int spacing(){return Math.clamp(plugin.getConfig().getInt("villages.spacing-chunks",16),8,64);}
    private Candidate candidate(World w,int x,int z){int s=spacing();return candidate(w.getSeed(),Math.floorDiv(x,s),Math.floorDiv(z,s),s);}
    public void enqueue(Chunk c) {if(c.getWorld().getEnvironment()==World.Environment.THE_END)queue.add(new Pending(c.getWorld(),c.getX(),c.getZ()));}
    @EventHandler public void load(ChunkLoadEvent e) {
        if(e.isNewChunk()||plugin.getConfig().getBoolean("villages.generate-in-existing-chunks",false))enqueue(e.getChunk());
    }
    private boolean outside(World w,int x,int z) {return MithrylRules.outerIsland(x,z,1024)&&w.getWorldBorder().isInside(new Location(w,x,64,z));}
    private void process(Chunk chunk) {
        World w=chunk.getWorld();int x=chunk.getX(),z=chunk.getZ();if(!outside(w,x*16+8,z*16+8))return;
        Candidate c=candidate(w,x,z);
        if(plugin.getConfig().getBoolean("villages.enabled",true)&&x==c.x&&z==c.z)ensure(w,c);
        if(plugin.getConfig().getBoolean("trees.enabled",true)&&!(Math.abs(x-c.x)<=1&&Math.abs(z-c.z)<=1))trees(chunk);
    }
    private void trees(Chunk chunk) {
        var key=plugin.key("end_trees_v1");var pdc=chunk.getPersistentDataContainer();if(pdc.has(key))return;pdc.set(key,PersistentDataType.BYTE,(byte)1);
        Random random=new Random(chunk.getWorld().getSeed()^(chunk.getX()*341873128712L)^(chunk.getZ()*132897987541L)^0x50494E4BL);
        if(random.nextDouble()>=plugin.getConfig().getDouble("trees.chunk-chance",.22))return;
        int x=chunk.getX()*16+4+random.nextInt(8),z=chunk.getZ()*16+4+random.nextInt(8);
        int y=chunk.getWorld().getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if(chunk.getWorld().getBlockAt(x,y,z).getType()==Material.END_STONE)wood.tree(chunk.getWorld().getBlockAt(x,y+1,z));
    }
    /** Loads a bounded 3x3 footprint asynchronously; all reads/writes resume on the server thread. */
    public CompletableFuture<Location> ensure(World world,Candidate c) {
        Pending id=new Pending(world,c.x,c.z);
        if(!outside(world,c.x*16+8,c.z*16+8))return CompletableFuture.completedFuture(null);
        if(!pending.add(id))return CompletableFuture.completedFuture(null);
        CompletableFuture<Location> result=new CompletableFuture<>();
        List<CompletableFuture<Chunk>> futures=new ArrayList<>();
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)futures.add(world.getChunkAtAsync(c.x+dx,c.z+dz,true));
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((v,error)->{
            if(!plugin.isEnabled()){pending.remove(id);result.complete(null);return;}
            Bukkit.getScheduler().runTask(plugin,()->{
                List<Chunk> chunks=new ArrayList<>();
                try {
                    if(error!=null){result.completeExceptionally(error);return;}
                    for(var future:futures){Chunk chunk=future.join();chunk.addPluginChunkTicket(plugin);chunks.add(chunk);}
                    Chunk center=world.getChunkAt(c.x,c.z);
                    var pdc=center.getPersistentDataContainer();int[] saved=pdc.get(plugin.key("end_village_center"),PersistentDataType.INTEGER_ARRAY);
                    if(saved!=null){result.complete(new Location(world,saved[0],saved[1],saved[2]));return;}
                    if(pdc.has(plugin.key("end_village_checked_v1"))){result.complete(null);return;}
                    pdc.set(plugin.key("end_village_checked_v1"),PersistentDataType.BYTE,(byte)1);
                    Location built=build(world,c.x*16+8,c.z*16+8);
                    result.complete(built);
                }catch(Throwable ex){result.completeExceptionally(ex);plugin.getLogger().log(java.util.logging.Level.WARNING,"End village generation failed",ex);}
                finally{chunks.forEach(ch->ch.removePluginChunkTicket(plugin));pending.remove(id);}
            });
        });
        return result;
    }
    private boolean natural(Material m){return m.isAir()||m==Material.CHORUS_PLANT||m==Material.CHORUS_FLOWER;}
    private int surface(World w,int x,int z) {
        int y=w.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES);
        while(y>w.getMinHeight()&&(w.getBlockAt(x,y,z).getType()==Material.CHORUS_PLANT||w.getBlockAt(x,y,z).getType()==Material.CHORUS_FLOWER))y--;
        Material m=w.getBlockAt(x,y,z).getType();return m==Material.END_STONE||m==MithrylOre.ORE_BLOCK?y:Integer.MIN_VALUE;
    }
    public Location build(World w,int cx,int cz) {
        if(w.getEnvironment()!=World.Environment.THE_END||!outside(w,cx,cz))return null;
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        // Inspect the complete footprint first; never clear player structures or End Cities.
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++) {
            if(!outside(w,cx+x,cz+z))return null;
            int y=surface(w,cx+x,cz+z);if(y==Integer.MIN_VALUE)return null;low=Math.min(low,y);high=Math.max(high,y);
        }
        if(high-low>8||high+11>=w.getMaxHeight())return null;
        int floor=high+1;var plan=EndVillagePlan.create();
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)for(int y=surface(w,cx+x,cz+z)+1;y<=floor+9;y++)if(!natural(w.getBlockAt(cx+x,y,cz+z).getType()))return null;
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)for(int y=surface(w,cx+x,cz+z)+1;y<=floor+9;y++) {
            Block b=w.getBlockAt(cx+x,y,cz+z);if(!b.getType().isAir())b.setType(Material.AIR,false);
        }
        // Foundations follow the terrain, avoiding floating houses and unsupported walkways.
        for(var entry:plan.blocks().entrySet()) {
            var p=entry.getKey();if(p.y()!=0)continue;
            int ground=surface(w,cx+p.x(),cz+p.z());
            for(int y=ground+1;y<floor;y++)w.getBlockAt(cx+p.x(),y,cz+p.z()).setType(Material.END_STONE_BRICKS,false);
        }
        Random random=new Random(w.getSeed()^((long)cx<<32)^cz);
        for(var entry:plan.blocks().entrySet()) {
            var p=entry.getKey();var cell=entry.getValue();Block b=w.getBlockAt(cx+p.x(),floor+p.y(),cz+p.z());
            if(!cell.wood().isEmpty())wood.set(b,cell.wood());else b.setType(Material.valueOf(cell.material()),false);
            if(!cell.states().isEmpty()) {
                b.setBlockData(Bukkit.createBlockData(b.getType(),"["+cell.states()+"]"),false);
                String woodId=wood.id(b);
                if(!woodId.isEmpty())wood.mark(b,woodId);
            }
            if(b.getState() instanceof Chest chest) {
                chest.getBlockInventory().setItem(11,stack("shard",3+random.nextInt(5)));
                chest.getBlockInventory().setItem(15,stack("ingot",1+random.nextInt(2)));
                chest.getBlockInventory().setItem(22,stack("end_sapling",1+random.nextInt(3)));
                if(random.nextDouble()<.35)chest.getBlockInventory().setItem(4,items.create("template"));
                chest.getBlockInventory().setItem(19,new ItemStack(Material.POPPED_CHORUS_FRUIT,3+random.nextInt(6)));
            }
        }
        for(int x:new int[]{-14,14}) {int y=surface(w,cx+x,cz);if(y!=Integer.MIN_VALUE)wood.tree(w.getBlockAt(cx+x,y+1,cz));}
        Location location=new Location(w,cx,floor+1,cz);
        w.getChunkAt(cx>>4,cz>>4).getPersistentDataContainer().set(plugin.key("end_village_center"),PersistentDataType.INTEGER_ARRAY,new int[]{cx,floor+1,cz});
        record(location);if(traders!=null)traders.populateVillage(location);return location;
    }
    private ItemStack stack(String id,int count){ItemStack item=items.create(id);item.setAmount(count);return item;}
    private void record(Location l) {
        var pdc=l.getWorld().getPersistentDataContainer();var key=plugin.key("end_villages_v1");int[] old=pdc.getOrDefault(key,PersistentDataType.INTEGER_ARRAY,new int[0]);
        for(int i=0;i<old.length;i+=3)if(old[i]==l.getBlockX()&&old[i+2]==l.getBlockZ())return;
        int[] next=Arrays.copyOf(old,old.length+3);next[old.length]=l.getBlockX();next[old.length+1]=l.getBlockY();next[old.length+2]=l.getBlockZ();pdc.set(key,PersistentDataType.INTEGER_ARRAY,next);
    }
    public Location nearest(Location origin) {
        int[] data=origin.getWorld().getPersistentDataContainer().getOrDefault(plugin.key("end_villages_v1"),PersistentDataType.INTEGER_ARRAY,new int[0]);Location nearest=null;double distance=Double.MAX_VALUE;
        for(int i=0;i<data.length;i+=3){Location candidate=new Location(origin.getWorld(),data[i],data[i+1],data[i+2]);double d=horizontal(origin,candidate);if(d<distance){nearest=candidate;distance=d;}}
        return nearest;
    }
    private static double horizontal(Location a,Location b){double x=a.getX()-b.getX(),z=a.getZ()-b.getZ();return x*x+z*z;}
    private void found(Player player,Location location) {
        player.sendMessage("§dEnd-Dorf: §f"+location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ()+" §7("+Math.round(Math.sqrt(horizontal(player.getLocation(),location)))+" Bloecke)");
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!sender.isOp()){sender.sendMessage("§cNur Operatoren duerfen End-Doerfer suchen.");return true;}
        if(!(sender instanceof Player player)){sender.sendMessage("Bitte als Spieler im End ausfuehren.");return true;}
        if(args.length!=0)return false;
        if(player.getWorld().getEnvironment()!=World.Environment.THE_END){player.sendMessage("§eBitte /locatevillage im End ausfuehren.");return true;}
        Location known=nearest(player.getLocation());if(known!=null){found(player,known);return true;}
        if(!plugin.getConfig().getBoolean("villages.enabled",true)){player.sendMessage("§eEnd-Doerfer sind deaktiviert.");return true;}
        if(locating){player.sendMessage("§eEine Dorfsuche laeuft bereits. Bitte kurz warten.");return true;}
        locating=true;player.sendMessage("§dSuche ein End-Dorf; passende neue Gebiete werden schrittweise geladen ...");
        Location origin=player.getLocation().clone();World world=origin.getWorld();int spacing=spacing(),rx=Math.floorDiv(origin.getBlockX()>>4,spacing),rz=Math.floorDiv(origin.getBlockZ()>>4,spacing);
        List<Candidate> candidates=new ArrayList<>();int radius=Math.clamp(plugin.getConfig().getInt("villages.locate-region-radius",6),1,12);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){Candidate c=candidate(world.getSeed(),rx+dx,rz+dz,spacing);if(outside(world,c.x*16+8,c.z*16+8))candidates.add(c);}
        candidates.sort(java.util.Comparator.comparingDouble(c->horizontal(origin,new Location(world,c.x*16+8,0,c.z*16+8))));
        new BukkitRunnable(){int index;boolean waiting;long started=System.currentTimeMillis();
            @Override public void run(){
                if(!player.isOnline()||player.getWorld()!=world||System.currentTimeMillis()-started>120000){locating=false;cancel();if(player.isOnline())player.sendMessage("§eSuche beendet. Erneut ausfuehren oder weiter ins aeussere End reisen.");return;}
                if(waiting)return;
                if(index>=candidates.size()){locating=false;cancel();player.sendMessage("§eKein geeignetes Dorf im Suchgebiet gefunden. Weiter entfernte Endinseln versuchen.");return;}
                Candidate c=candidates.get(index++);
                // Existing unmarked terrain is left alone unless retro-generation was explicitly enabled.
                if(world.isChunkGenerated(c.x,c.z)&&!plugin.getConfig().getBoolean("villages.generate-in-existing-chunks",false))return;
                waiting=true;ensure(world,c).whenComplete((location,error)->{
                    waiting=false;if(isCancelled())return;
                    if(error!=null){locating=false;cancel();player.sendMessage("§cDorfsuche fehlgeschlagen; siehe Serverlog.");}
                    else if(location!=null){locating=false;cancel();found(player,location);}
                });
            }
        }.runTaskTimer(plugin,1,5);
        return true;
    }
}
