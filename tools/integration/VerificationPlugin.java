package de.midnitesmp.endupdate.verify;

import de.midnitesmp.endupdate.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.loot.*;
import org.bukkit.persistence.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Isolated server probe, never packaged into the production plugin. */
public final class VerificationPlugin extends JavaPlugin {
    int assertions;
    void check(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    @Override public void onEnable(){Bukkit.getScheduler().runTaskLater(this,()->{
        try{runChecks();}
        catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"INTEGRATION FAILED",ex);try{Files.writeString(Path.of("integration-result.txt"),"FAIL: "+ex);}catch(Exception ignored){}}
    },30);}
    void naturalChecks(int attempt) {
        EndUpdatePlugin plugin=(EndUpdatePlugin)Bukkit.getPluginManager().getPlugin("EndUpdate");
        World end=Bukkit.getWorlds().stream().filter(w->w.getEnvironment()==World.Environment.THE_END).findFirst().orElseThrow();
        EndVillages villages=(EndVillages)Bukkit.getPluginCommand("locatevillage").getExecutor();
        if(attempt>=49){try{Files.writeString(Path.of("integration-result.txt"),"FAIL: no natural village in 49 candidate regions");}catch(Exception ignored){}getLogger().severe("No natural village in 49 candidate regions");return;}
        var c=EndVillages.candidate(end.getSeed(),17+attempt%7,17+attempt/7,16);
        villages.ensure(end,c).whenComplete((location,error)->{
            try{
                if(error!=null)throw new AssertionError("async village generation",error);
                if(location==null){Bukkit.getScheduler().runTaskLater(this,()->naturalChecks(attempt+1),2);return;}
                check(location.getWorld()==end,"asynchronous generation succeeds on unmodified natural End terrain");
                check(villages.nearest(location).getBlockX()==location.getBlockX(),"natural village appears in locate index");
                Files.writeString(Path.of("natural-village.txt"),"Natural End village: "+location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ()+" after "+(attempt+1)+" candidates");
                String result="PASS: "+assertions+" integration assertions on Paper "+Bukkit.getMinecraftVersion();getLogger().info(result);Files.writeString(Path.of("integration-result.txt"),result);
            }catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"NATURAL GENERATION FAILED",ex);try{Files.writeString(Path.of("integration-result.txt"),"FAIL: "+ex);}catch(Exception ignored){}}
        });
    }
    @SuppressWarnings("unchecked")
    static <T> T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
    void runChecks() throws Exception {
        EndUpdatePlugin plugin=(EndUpdatePlugin)Bukkit.getPluginManager().getPlugin("EndUpdate");
        check(plugin!=null&&plugin.isEnabled(),"plugin enabled");
        MithrylItems items=new MithrylItems(plugin);MithrylRecipes recipes=new MithrylRecipes(plugin,items);
        for(var entry:MithrylItems.GEAR.entrySet()){
            ItemStack original=new ItemStack(entry.getValue());
            var meta=original.getItemMeta();meta.addEnchant(Enchantment.UNBREAKING,3,true);((Damageable)meta).setDamage(77);
            meta.getPersistentDataContainer().set(new NamespacedKey("verification","foreign"),PersistentDataType.STRING,"preserved");original.setItemMeta(meta);
            ItemStack upgraded=items.upgrade(original);
            check(items.isGear(upgraded),"identity "+entry.getKey());
            check(upgraded.getEnchantmentLevel(Enchantment.UNBREAKING)==3,"enchant preserved");
            check(((Damageable)upgraded.getItemMeta()).getDamage()==77,"damage preserved");
            check("preserved".equals(upgraded.getItemMeta().getPersistentDataContainer().get(new NamespacedKey("verification","foreign"),PersistentDataType.STRING)),"foreign PDC preserved");
            check(!items.isGear(original),"input not mutated");
            if(upgraded.getItemMeta() instanceof ArmorMeta armor){check(armor.hasTrim(),"rib trim");check(armor.getEquippable().getModel().equals(plugin.key("mithryl")),"equipment model");}
            else {check(upgraded.getItemMeta().getAttributeModifiers(Attribute.ATTACK_DAMAGE)!=null,"default damage kept");check(upgraded.getItemMeta().getAttributeModifiers(Attribute.BLOCK_BREAK_SPEED)!=null,"mining speed bonus");}
            SmithingTransformRecipe registered=(SmithingTransformRecipe)Bukkit.getRecipe(plugin.key("upgrade_"+entry.getKey()));
            check(registered!=null&&registered.getBase().test(original),"smithing recipe registered");
            check(!registered.getAddition().test(new ItemStack(Material.PRISMARINE_SHARD)),"vanilla shard rejected");
        }
        World world=Bukkit.getWorlds().getFirst();
        ItemStack[] matrix=new ItemStack[9];for(int i=0;i<4;i++){matrix[i]=items.create("shard");matrix[i+4]=new ItemStack(Material.DIAMOND);}
        Recipe recipe=Bukkit.getCraftingRecipe(matrix,world);check(recipe!=null&&items.is(recipe.getResult(),"ingot"),"4+4 ingot recipe");
        matrix[0]=new ItemStack(Material.PRISMARINE_CRYSTALS);recipe=Bukkit.getCraftingRecipe(matrix,world);check(recipe==null||!items.is(recipe.getResult(),"ingot"),"fake crystal rejected");
        ItemStack[] trident={null,items.create("shard"),items.create("shard"),null,new ItemStack(Material.STICK),items.create("shard"),new ItemStack(Material.STICK),null,null};
        recipe=Bukkit.getCraftingRecipe(trident,world);
        check(recipe instanceof Keyed keyed&&keyed.getKey().equals(plugin.key("mithryl_trident"))&&recipe.getResult().getType()==Material.TRIDENT&&recipe.getResult().getAmount()==1,"diagonal two sticks and three Mithryl shards craft one trident");
        for(int slot:new int[]{1,2,5}){
            ItemStack[] fake=trident.clone();fake[slot]=new ItemStack(Material.PRISMARINE_CRYSTALS);
            check(Bukkit.getCraftingRecipe(fake,world)==null,"ordinary crystal rejected at trident slot "+slot);
        }
        ItemStack[] renamed=trident.clone();renamed[1]=new ItemStack(Material.PRISMARINE_CRYSTALS);
        var fakeMeta=renamed[1].getItemMeta();fakeMeta.itemName(items.create("shard").getItemMeta().itemName());renamed[1].setItemMeta(fakeMeta);
        check(Bukkit.getCraftingRecipe(renamed,world)==null,"renamed ordinary crystal cannot craft trident");
        ItemStack[] misplaced=trident.clone();misplaced[8]=misplaced[6];misplaced[6]=null;
        check(Bukkit.getCraftingRecipe(misplaced,world)==null,"misplaced trident handle rejected");
        ItemStack[] copy={new ItemStack(Material.DIAMOND),items.create("template"),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.END_STONE),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND),new ItemStack(Material.DIAMOND)};
        recipe=Bukkit.getCraftingRecipe(copy,world);check(recipe!=null&&recipe.getResult().getAmount()==2&&items.is(recipe.getResult(),"template"),"template duplication");
        ItemStack[] ingots=new ItemStack[9];Arrays.setAll(ingots,i->items.create("ingot"));
        recipe=Bukkit.getCraftingRecipe(ingots,world);check(recipe!=null&&items.is(recipe.getResult(),"block"),"nine ingots craft block");
        ingots[4]=new ItemStack(Material.PRISMARINE_SHARD);recipe=Bukkit.getCraftingRecipe(ingots,world);check(recipe==null||!items.is(recipe.getResult(),"block"),"ordinary shard cannot craft storage block");
        ItemStack[] packed=new ItemStack[9];packed[0]=items.create("block");recipe=Bukkit.getCraftingRecipe(packed,world);
        check(recipe!=null&&items.is(recipe.getResult(),"ingot")&&recipe.getResult().getAmount()==9,"block unpacks to nine ingots");
        packed[0]=new ItemStack(Material.PRISMARINE_BRICKS);recipe=Bukkit.getCraftingRecipe(packed,world);check(recipe==null||!items.is(recipe.getResult(),"ingot"),"ordinary bricks cannot unpack");
        check(Bukkit.getPluginCommand("mithryl")!=null,"give command registered");
        check("endupdate.admin".equals(Bukkit.getPluginCommand("mithryl").getPermission()),"give command permission");
        check(Bukkit.getPluginCommand("mythril")==Bukkit.getPluginCommand("mithryl"),"mythril spelling alias");

        ItemStack[] smithSlots=new ItemStack[4];
        SmithingInventory smith=proxy(SmithingInventory.class,(p,m,a)->switch(m.getName()){
            case "setItem"->{smithSlots[(int)a[0]]=(ItemStack)a[1];yield null;}
            case "getItem"->smithSlots[(int)a[0]];case "getInputTemplate"->smithSlots[0];case "getInputEquipment"->smithSlots[1];case "getInputMineral"->smithSlots[2];default->null;});
        InventoryView view=proxy(InventoryView.class,(p,m,a)->switch(m.getName()){case "getTopInventory"->smith;case "getType"->InventoryType.SMITHING;default->null;});
        smith.setItem(0,items.create("template"));smith.setItem(1,new ItemStack(Material.NETHERITE_PICKAXE));smith.setItem(2,items.create("ingot"));
        PrepareSmithingEvent prepare=new PrepareSmithingEvent(view,null);recipes.smith(prepare);check(items.is(prepare.getResult(),"pickaxe"),"smithing preview");
        smith.setItem(1,items.create("pickaxe"));prepare=new PrepareSmithingEvent(view,null);recipes.smith(prepare);check(prepare.getResult()==null,"repeat upgrade blocked");
        smith.setItem(1,new ItemStack(Material.NETHERITE_PICKAXE));smith.setItem(2,new ItemStack(Material.PRISMARINE_SHARD));prepare=new PrepareSmithingEvent(view,null);recipes.smith(prepare);check(prepare.getResult()==null,"fake ingot blocked");

        plugin.getConfig().set("loot.template-chance",1.0);plugin.getConfig().set("loot.shards-chance",1.0);plugin.getConfig().set("loot.ingot-chance",1.0);plugin.getConfig().set("loot.bonus-chance",1.0);
        var table=Bukkit.getLootTable(NamespacedKey.minecraft("chests/end_city_treasure"));
        var context=new LootContext.Builder(new Location(world,0,100,0)).build();
        var loot=new ArrayList<ItemStack>();loot.add(new ItemStack(Material.DIAMOND));
        var event=new LootGenerateEvent(world,null,null,table,context,loot,false);new EndCityLoot(plugin,items).loot(event);
        check(event.getLoot().size()==5,"bonus loot appended and original retained");check(event.getLoot().stream().anyMatch(i->items.is(i,"template")),"template in End City loot");
        var other=new LootGenerateEvent(world,null,null,Bukkit.getLootTable(NamespacedKey.minecraft("chests/simple_dungeon")),context,new ArrayList<>(),false);new EndCityLoot(plugin,items).loot(other);check(other.getLoot().isEmpty(),"other loot unaffected");

        World end=Bukkit.getWorlds().stream().filter(w->w.getEnvironment()==World.Environment.THE_END).findFirst().orElseThrow();
        Chunk chunk=end.getChunkAt(128,128);chunk.getPersistentDataContainer().remove(plugin.key("ore_generated_v1"));
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)end.getBlockAt(2048+x,100,2048+z).setType(Material.END_STONE,false);
        plugin.getConfig().set("ore.chunk-chance",1.0);plugin.getConfig().set("ore.attempts-per-chunk",64);
        CustomBlockDisplays displays=new CustomBlockDisplays(plugin,items);
        MithrylOre ore=new MithrylOre(plugin,items,displays);Method generate=MithrylOre.class.getDeclaredMethod("generate",Chunk.class);generate.setAccessible(true);generate.invoke(ore,chunk);
        List<Block> ores=new ArrayList<>();for(int x=0;x<16;x++)for(int z=0;z<16;z++){Block b=end.getBlockAt(2048+x,100,2048+z);if(ore.isOre(b))ores.add(b);}
        check(!ores.isEmpty(),"outer surface ores generated");
        check(!displays.hasDisplay(ores.getFirst()),"ore display is culled with no player in range");
        int[] before=chunk.getPersistentDataContainer().get(plugin.key("ore_locations"),PersistentDataType.INTEGER_ARRAY);generate.invoke(ore,chunk);check(Arrays.equals(before,chunk.getPersistentDataContainer().get(plugin.key("ore_locations"),PersistentDataType.INTEGER_ARRAY)),"generation idempotent");
        Block placed=end.getBlockAt(2048,101,2048);placed.setType(Material.PRISMARINE,false);check(!ore.isOre(placed),"ordinary prismarine not ore");
        Block oldOre=end.getBlockAt(2049,101,2048);oldOre.setType(Material.PRISMARINE,false);
        int encodedOre=MithrylRules.encode(oldOre.getX(),oldOre.getY(),oldOre.getZ(),end.getMinHeight());
        int[] migrationOres=Arrays.copyOf(before,before.length+1);migrationOres[before.length]=encodedOre;
        chunk.getPersistentDataContainer().set(plugin.key("ore_locations"),PersistentDataType.INTEGER_ARRAY,migrationOres);
        ore.load(new org.bukkit.event.world.ChunkLoadEvent(chunk,false));
        check(oldOre.getType()==MithrylOre.ORE_BLOCK&&ore.isOre(oldOre),"previously generated ore migrates to its custom carrier");
        ItemStack[] held={new ItemStack(Material.DIAMOND_PICKAXE)};ItemStack[][] armor={new ItemStack[4]};Map<PotionEffectType,PotionEffect> effects=new HashMap<>();
        PlayerInventory inventory=proxy(PlayerInventory.class,(p,m,a)->switch(m.getName()){case "getItemInMainHand"->held[0];case "getArmorContents"->armor[0];default->null;});
        var pdc=new ItemStack(Material.STONE).getItemMeta().getPersistentDataContainer();UUID uuid=UUID.randomUUID();
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()){
            case "getInventory"->inventory;case "getGameMode"->GameMode.SURVIVAL;case "getUniqueId"->uuid;case "isDead"->false;case "getPersistentDataContainer"->pdc;
            case "getPotionEffect"->effects.get(a[0]);case "addPotionEffect"->{var effect=(PotionEffect)a[0];effects.put(effect.getType(),effect);yield true;}
            case "removePotionEffect"->{effects.remove(a[0]);yield null;}case "toString"->"VerificationPlayer";default->null;});
        var mine=new BlockBreakEvent(ores.getFirst(),player);ore.breakOre(mine);check(mine.isCancelled(),"diamond cannot mine");
        held[0]=items.create("pickaxe");mine=new BlockBreakEvent(ores.getFirst(),player);ore.breakOre(mine);check(!mine.isCancelled()&&!mine.isDropItems(),"mithryl can mine, no vanilla drops");ore.dropOre(mine);check(!ore.isOre(ores.getFirst()),"ore marker consumed");
        MithrylBlocks storage=new MithrylBlocks(plugin,items,displays);
        chunk.getPersistentDataContainer().remove(plugin.key("storage_blocks"));
        Block storageBlock=end.getBlockAt(2050,102,2050);var replaced=storageBlock.getState();storageBlock.setType(MithrylBlocks.STORAGE_BLOCK,false);
        check(!storage.isStorage(storageBlock),"unmarked bricks not Mithryl");
        var placement=new org.bukkit.event.block.BlockPlaceEvent(storageBlock,replaced,storageBlock.getRelative(org.bukkit.block.BlockFace.DOWN),items.create("block"),player,true,EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(placement);
        Bukkit.getScheduler().runTaskLater(this,()->{try{
        check(storage.isStorage(storageBlock),"placed custom block tracked after placement commit");
        check(!displays.hasDisplay(storageBlock),"storage display is culled with no player in range");
        check(new MithrylBlocks(plugin,items,displays).isStorage(storageBlock),"storage identity in persistent chunk data");
        Block oldStorage=end.getBlockAt(2051,102,2050);oldStorage.setType(Material.PRISMARINE_BRICKS,false);
        int encodedStorage=MithrylRules.encode(oldStorage.getX(),oldStorage.getY(),oldStorage.getZ(),end.getMinHeight());
        int encodedPlacedStorage=MithrylRules.encode(storageBlock.getX(),storageBlock.getY(),storageBlock.getZ(),end.getMinHeight());
        chunk.getPersistentDataContainer().set(plugin.key("storage_blocks"),PersistentDataType.INTEGER_ARRAY,new int[]{encodedPlacedStorage,encodedStorage});
        storage.load(new org.bukkit.event.world.ChunkLoadEvent(chunk,false));
        check(oldStorage.getType()==MithrylBlocks.STORAGE_BLOCK&&storage.isStorage(oldStorage),"previously placed storage blocks migrate to their custom carrier");
        held[0]=new ItemStack(Material.DIAMOND_PICKAXE);var storageMine=new BlockBreakEvent(storageBlock,player);Bukkit.getPluginManager().callEvent(storageMine);check(storageMine.isCancelled()&&storage.isStorage(storageBlock),"wrong tool preserves storage block");
        held[0]=items.create("pickaxe");storageMine=new BlockBreakEvent(storageBlock,player);storageMine.setCancelled(true);Bukkit.getPluginManager().callEvent(storageMine);check(storage.isStorage(storageBlock),"protected break preserves storage marker");
        var piston=new org.bukkit.event.block.BlockPistonExtendEvent(storageBlock,List.of(storageBlock),org.bukkit.block.BlockFace.UP);Bukkit.getPluginManager().callEvent(piston);check(piston.isCancelled(),"piston cannot move storage");
        held[0].addUnsafeEnchantment(Enchantment.FORTUNE,3);
        long droppedBefore=end.getEntitiesByClass(org.bukkit.entity.Item.class).stream().filter(i->items.is(i.getItemStack(),"block")).mapToInt(i->i.getItemStack().getAmount()).sum();
        storageMine=new BlockBreakEvent(storageBlock,player);Bukkit.getPluginManager().callEvent(storageMine);check(!storageMine.isCancelled()&&!storageMine.isDropItems()&&!storage.isStorage(storageBlock),"storage break consumes marker and disables vanilla drops");
        long droppedAfter=end.getEntitiesByClass(org.bukkit.entity.Item.class).stream().filter(i->items.is(i.getItemStack(),"block")).mapToInt(i->i.getItemStack().getAmount()).sum();check(droppedAfter==droppedBefore+1,"Fortune does not multiply storage blocks");
        MithrylSpeed speed=new MithrylSpeed(plugin,items);
        armor[0][0]=items.create("boots");speed.update(player);check(effects.get(PotionEffectType.SPEED).getAmplifier()==0,"partial set Speed I");
        armor[0]=new ItemStack[]{items.create("boots"),items.create("leggings"),items.create("chestplate"),items.create("helmet")};speed.update(player);check(effects.get(PotionEffectType.SPEED).getAmplifier()==1,"full set Speed II");
        speed.toggle(player,false);check(!effects.containsKey(PotionEffectType.SPEED)&&!speed.enabled(player),"toggle removes own speed and persists");
        effects.put(PotionEffectType.SPEED,new PotionEffect(PotionEffectType.SPEED,1000,2));speed.toggle(player,true);check(effects.get(PotionEffectType.SPEED).getAmplifier()==2,"strong external speed preserved");speed.toggle(player,false);check(effects.containsKey(PotionEffectType.SPEED),"toggle preserves external speed");
        checkEndVillage(plugin,items,end,player);
        checkHingedModels(plugin,items,end,player);
        plugin.reloadConfig();
        naturalChecks(0);
        }catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"INTEGRATION FAILED",ex);try{Files.writeString(Path.of("integration-result.txt"),"FAIL: "+ex);}catch(Exception ignored){}}},2);
    }
    @SuppressWarnings("unchecked")
    void checkHingedModels(EndUpdatePlugin plugin,MithrylItems items,World end,Player player) throws Exception {
        var fixture=com.google.gson.JsonParser.parseString(Files.readString(Path.of("../tools/fixtures/hinged-vanilla-models.json"))).getAsJsonObject();
        Map<String,com.google.gson.JsonObject> models=new HashMap<>();
        try(var zip=new java.util.zip.ZipInputStream(plugin.getResource("endupdate-pack.zip"))){
            for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry())
                if(entry.getName().startsWith("assets/endupdate/models/block/")&&entry.getName().endsWith(".json"))
                    models.put(entry.getName(),com.google.gson.JsonParser.parseString(new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        }
        CustomBlockDisplays displays=new CustomBlockDisplays(plugin,items);
        Method stackMethod=CustomBlockDisplays.class.getDeclaredMethod("displayStack",Block.class,ItemStack.class);stackMethod.setAccessible(true);
        Block block=end.getBlockAt(2050,180,2050);
        for(String kind:List.of("door","trapdoor")){
            for(var variant:fixture.getAsJsonObject("states").getAsJsonObject(kind).entrySet()){
                var expected=variant.getValue().getAsJsonObject();
                var data=Bukkit.createBlockData("minecraft:cherry_"+kind+"["+variant.getKey()+"]");
                block.setBlockData(data,false);
                var state=HingedBlockModels.of(data);
                String expectedModel=expected.get("model").getAsString().replace("minecraft:block/cherry_"+kind+"_","end_"+kind+"_display_");
                check(state.model().equals(expectedModel),"model for "+variant.getKey());
                check(state.x()==(expected.has("x")?expected.get("x").getAsInt():0)&&state.y()==(expected.has("y")?expected.get("y").getAsInt():0),"vanilla UV rotation for "+variant.getKey());
                ItemStack selected=(ItemStack)stackMethod.invoke(displays,block,items.create("end_"+kind));
                check(selected.getItemMeta().getItemModel().equals(plugin.key(expectedModel)),"display item selects state model");
                var element=models.get("assets/endupdate/models/block/"+state.model()+".json").getAsJsonArray("elements").get(0).getAsJsonObject();
                double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY}, max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
                for(int corner=0;corner<8;corner++){
                    float[] v=new float[3];for(int axis=0;axis<3;axis++)v[axis]=element.getAsJsonArray((corner&(1<<axis))==0?"from":"to").get(axis).getAsFloat()/16f-.5f;
                    var point=new org.joml.Vector3f(v[0],v[1],v[2]).rotate(state.transformation().getLeftRotation()).add(.5f,.5f,.5f);
                    for(int axis=0;axis<3;axis++){min[axis]=Math.min(min[axis],point.get(axis));max[axis]=Math.max(max[axis],point.get(axis));}
                }
                var bounds=block.getBoundingBox();double[] physicalMin={bounds.getMinX()-block.getX(),bounds.getMinY()-block.getY(),bounds.getMinZ()-block.getZ()},physicalMax={bounds.getMaxX()-block.getX(),bounds.getMaxY()-block.getY(),bounds.getMaxZ()-block.getZ()};
                for(int axis=0;axis<3;axis++)check(Math.abs(min[axis]-(physicalMin[axis]-.02/16))<.00001&&Math.abs(max[axis]-(physicalMax[axis]+.02/16))<.00001,"display geometry matches Paper collision: "+kind+" "+variant.getKey()+" axis "+axis);
            }
        }
        Field pendingField=CustomBlockDisplays.class.getDeclaredField("pendingChanges");pendingField.setAccessible(true);
        Set<Location> pending=(Set<Location>)pendingField.get(displays);
        block.setBlockData(Bukkit.createBlockData("minecraft:cherry_door[half=lower]"),false);
        int at=MithrylRules.encode(block.getX(),block.getY(),block.getZ(),end.getMinHeight());
        var key=plugin.key("wood_"+Integer.toHexString(at));
        block.getChunk().getPersistentDataContainer().set(key,PersistentDataType.STRING,"end_door");
        var click=new org.bukkit.event.player.PlayerInteractEvent(player,org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,null,block,org.bukkit.block.BlockFace.NORTH);
        click.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);
        displays.interact(click);
        check(pending.contains(block.getLocation())&&pending.contains(block.getLocation().add(0,1,0)),"empty-hand click refreshes both door halves");
        displays.redstone(new org.bukkit.event.block.BlockRedstoneEvent(block,0,15));
        check(pending.size()==2,"redstone and click updates deduplicated");
        pending.clear();click.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);displays.interact(click);
        check(pending.isEmpty(),"denied block interaction does not schedule display update");
        block.getChunk().getPersistentDataContainer().remove(key);
        displays.redstone(new org.bukkit.event.block.BlockRedstoneEvent(block,0,15));
        check(pending.isEmpty(),"ordinary vanilla door ignored");
        block.setBlockData(Bukkit.createBlockData("minecraft:cherry_trapdoor[half=top,open=true]"),false);
        block.getChunk().getPersistentDataContainer().set(key,PersistentDataType.STRING,"end_trapdoor");
        displays.physics(new org.bukkit.event.block.BlockPhysicsEvent(block,block.getBlockData()));
        check(pending.equals(Set.of(block.getLocation())),"trapdoor physics refreshes one block");
        block.getChunk().getPersistentDataContainer().remove(key);block.setType(Material.AIR,false);
    }

    void checkEndVillage(EndUpdatePlugin plugin,MithrylItems items,World end,Player player) throws Exception {
        CustomBlockDisplays displays=new CustomBlockDisplays(plugin,items);
        EndWood wood=Arrays.stream(org.bukkit.event.world.StructureGrowEvent.getHandlerList().getRegisteredListeners()).map(r->r.getListener()).filter(l->l instanceof EndWood).map(l->(EndWood)l).findFirst().orElseThrow();
        EndVillages villages=(EndVillages)Bukkit.getPluginCommand("locatevillage").getExecutor();
        check(villages!=null,"village command registered");
        check("endupdate.locatevillage".equals(Bukkit.getPluginCommand("locatevillage").getPermission()),"locate permission registered");
        List<String> messages=new ArrayList<>();
        org.bukkit.command.CommandSender nonOp=proxy(org.bukkit.command.CommandSender.class,(p,m,a)->switch(m.getName()){case "isOp"->false;case "hasPermission"->true;case "sendMessage"->{if(a[0] instanceof String s)messages.add(s);yield null;}default->null;});
        villages.onCommand(nonOp,Bukkit.getPluginCommand("locatevillage"),"locatevillage",new String[0]);
        check(messages.stream().anyMatch(s->s.contains("Operatoren")),"non-OP denied even with a permission grant");
        for(String kind:EndWood.MATERIALS.keySet())check(items.is(items.create(kind),kind),"custom wood item "+kind);
        ItemStack[] grid=new ItemStack[9];grid[0]=items.create("end_log");Recipe recipe=Bukkit.getCraftingRecipe(grid,end);
        check(recipe!=null&&items.is(recipe.getResult(),"end_planks")&&recipe.getResult().getAmount()==4,"End log crafts four End planks");
        grid[0]=new ItemStack(Material.CHERRY_LOG);recipe=Bukkit.getCraftingRecipe(grid,end);
        check(recipe==null||!items.is(recipe.getResult(),"end_planks"),"ordinary cherry log cannot create custom planks");
        Arrays.fill(grid,null);grid[0]=grid[1]=grid[3]=grid[4]=items.create("end_log");recipe=Bukkit.getCraftingRecipe(grid,end);
        check(recipe!=null&&items.is(recipe.getResult(),"end_wood")&&recipe.getResult().getAmount()==3,"four End logs craft three bark blocks");
        check(villages.build(end,0,0)==null,"no villages on central island");
        check(villages.build(Bukkit.getWorlds().getFirst(),5128,5128)==null,"no villages in overworld");
        int cx=5128,cz=5128,ground=120;
        checkTraderRestart(plugin,end);
        // Reset only this disposable probe village, including its saved per-house spawn markers.
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            Chunk c=end.getChunkAt((cx>>4)+dx,(cz>>4)+dz);
            for(var k:Set.copyOf(c.getPersistentDataContainer().getKeys()))if(k.getKey().startsWith("trader_house_5128_5128_")||k.getKey().startsWith("station_"))c.getPersistentDataContainer().remove(k);
            for(var entity:c.getEntities())if(entity.getPersistentDataContainer().has(plugin.key("trader_resident"))||entity.getPersistentDataContainer().has(plugin.key("trader_visual")))entity.remove();
        }
        // A controlled End-stone island provides reproducible terrain and obstruction tests.
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++){
            end.getBlockAt(cx+x,ground,cz+z).setType(Material.END_STONE,false);
            for(int y=ground+1;y<=ground+15;y++)end.getBlockAt(cx+x,y,cz+z).setType(Material.AIR,false);
        }
        Block obstruction=end.getBlockAt(cx,ground+1,cz);obstruction.setType(Material.DIAMOND_BLOCK,false);
        check(villages.build(end,cx,cz)==null&&obstruction.getType()==Material.DIAMOND_BLOCK,"village refuses to overwrite existing build");
        obstruction.setType(Material.AIR,false);
        Location center=villages.build(end,cx,cz);check(center!=null,"village fits outer End island");
        check(villages.nearest(new Location(end,cx,ground,cz)).getBlockX()==cx,"locate reads generated village index");
        check(end.getChunkAt(cx>>4,cz>>4).getPersistentDataContainer().has(plugin.key("end_village_center")),"village saved in chunk PDC");
        for(int x:new int[]{-8,8})for(int z:new int[]{-8,8}){
            int front=z<0?1:-1;Block chestBlock=end.getBlockAt(cx+x-2,ground+2,cz+z-2*front);
            check(chestBlock.getState() instanceof org.bukkit.block.Chest,"house has a loot chest");
            var chest=(org.bukkit.block.Chest)chestBlock.getState();
            check(items.is(chest.getBlockInventory().getItem(11),"shard")&&items.is(chest.getBlockInventory().getItem(15),"ingot"),"house contains real Mithryl loot");
            check(wood.id(end.getBlockAt(cx+x-3,ground+2,cz+z-3)).equals("end_log"),"house uses custom End log frame");
        }
        check(villages.build(end,cx,cz)==null,"existing village cannot regenerate loot");
        check(wood.id(end.getBlockAt(cx-14,ground+1,cz)).equals("end_log"),"village has a pink End tree");
        checkTraders(plugin,items,wood,center,player);
        Block sapling=end.getBlockAt(cx+24,ground+1,cz);sapling.getRelative(org.bukkit.block.BlockFace.DOWN).setType(Material.END_STONE,false);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<8;y++)sapling.getRelative(x,y,z).setType(Material.AIR,false);
        wood.set(sapling,"end_sapling");
        var physics=new org.bukkit.event.block.BlockPhysicsEvent(sapling,Material.END_STONE.createBlockData());wood.physics(physics);check(physics.isCancelled(),"End sapling survives on End stone");
        Block blocked=sapling.getRelative(0,4,0);blocked.setType(Material.DIAMOND_BLOCK,false);
        check(!wood.grow(sapling,true,null)&&wood.id(sapling).equals("end_sapling"),"blocked growth preserves sapling and obstacle");blocked.setType(Material.AIR,false);
        var protection=new org.bukkit.event.Listener(){};
        Bukkit.getPluginManager().registerEvent(org.bukkit.event.world.StructureGrowEvent.class,protection,org.bukkit.event.EventPriority.HIGHEST,(l,e)->((org.bukkit.event.world.StructureGrowEvent)e).setCancelled(true),this);
        check(!wood.grow(sapling,true,null)&&wood.id(sapling).equals("end_sapling"),"cancelled growth does not overwrite world");org.bukkit.event.HandlerList.unregisterAll(protection);
        check(wood.grow(sapling,true,null)&&wood.id(sapling).equals("end_log"),"End sapling grows custom tree");
        Block leaf=sapling.getRelative(2,4,0);check(wood.id(leaf).equals("end_leaves"),"tree carries custom leaves");
        var leaves=(org.bukkit.block.data.type.Leaves)leaf.getBlockData();check(!leaves.isPersistent()&&leaves.getDistance()<7,"natural leaves connected to logs and able to decay");
        Block ordinary=sapling.getRelative(4,0,0);ordinary.setType(Material.CHERRY_LOG,false);wood.mark(ordinary,"end_log");
        wood.load(new org.bukkit.event.world.ChunkLoadEvent(ordinary.getChunk(),false));
        check(ordinary.getType()==Material.CHERRY_LOG&&wood.id(ordinary).equals("end_log"),"previously placed custom End wood keeps its vanilla carrier");
        check(!displays.hasDisplay(ordinary),"End wood display is culled with no player in range");
        Block vanillaCherry=ordinary.getRelative(1,0,0);vanillaCherry.setType(Material.CHERRY_LOG,false);
        check(wood.id(vanillaCherry).isEmpty()&&vanillaCherry.getType()==Material.CHERRY_LOG,"ordinary cherry remains vanilla and is never tagged as End wood");
        check(EndWood.MATERIALS.get("end_log")==Material.CHERRY_LOG,"custom End wood uses its vanilla carrier with a separate display model");
        check(MithrylOre.ORE_BLOCK==Material.PRISMARINE&&MithrylBlocks.STORAGE_BLOCK==Material.PRISMARINE_BRICKS,
                "Mithryl uses its original carriers with separate display models");
        // Export actual server blocks for a schematic-style preview, not a generated concept.
        List<String> exported=new ArrayList<>();
        for(int x=-18;x<=18;x++)for(int z=-18;z<=18;z++)for(int y=ground;y<=ground+10;y++){
            Block b=end.getBlockAt(cx+x,y,cz+z);if(!b.getType().isAir())exported.add((cx+x)+","+y+","+(cz+z)+","+b.getBlockData().getAsString());
        }
        Files.write(Path.of("village-preview.csv"),exported);
    }
    void checkTraderRestart(EndUpdatePlugin plugin,World end) throws Exception {
        Path file=Path.of("trader-restart.txt");if(!Files.exists(file))return;
        String[] saved=Files.readString(file).split(",");UUID id=UUID.fromString(saved[0]);
        var stand=Arrays.stream(end.getChunkAt(Integer.parseInt(saved[1]),Integer.parseInt(saved[2])).getEntities()).filter(e->e.getUniqueId().equals(id)).findFirst().orElseThrow();
        check(stand instanceof org.bukkit.entity.ArmorStand,"trader entity survives a real server restart");
        check(stand.getPersistentDataContainer().has(plugin.key("trader_locked")),"traded profession survives a real restart");
        int[] used=stand.getPersistentDataContainer().get(plugin.key("trader_uses_smith"),PersistentDataType.INTEGER_ARRAY);
        check(used!=null&&used[0]==3,"merchant stock survives a real restart");
    }
    void checkTraders(EndUpdatePlugin plugin,MithrylItems items,EndWood wood,Location center,Player player) throws Exception {
        MithrylTraders traders=Arrays.stream(org.bukkit.event.player.PlayerInteractEntityEvent.getHandlerList().getRegisteredListeners()).map(r->r.getListener()).filter(l->l instanceof MithrylTraders).map(l->(MithrylTraders)l).findFirst().orElseThrow();
        MithrylWorkstations stations=Arrays.stream(org.bukkit.event.block.BlockPlaceEvent.getHandlerList().getRegisteredListeners()).map(r->r.getListener()).filter(l->l instanceof MithrylWorkstations).map(l->(MithrylWorkstations)l).findFirst().orElseThrow();
        List<org.bukkit.entity.ArmorStand> residents=center.getWorld().getNearbyEntities(center,15,4,15).stream().filter(traders::isTrader).map(e->(org.bukkit.entity.ArmorStand)e).toList();
        check(residents.size()==4,"four custom village residents spawn naturally with village generation");
        traders.populateVillage(center);check(center.getWorld().getNearbyEntities(center,15,4,15).stream().filter(traders::isTrader).count()==4,"village population is idempotent");
        for(var resident:residents){
            check(!resident.isVisible()&&!resident.isMarker()&&resident.isInvulnerable(),"invisible ArmorStand retains protected hitbox");
            check(center.getWorld().getNearbyEntities(resident.getLocation(),2,3,2).stream().anyMatch(e->e instanceof org.bukkit.entity.ItemDisplay&&resident.getUniqueId().toString().equals(e.getPersistentDataContainer().get(plugin.key("trader_parent"),PersistentDataType.STRING))),"resident has namespaced ItemDisplay model");
        }
        check(residents.stream().map(traders::job).collect(java.util.stream.Collectors.toSet()).size()==4,"smith, gardener, relic trader and unemployed resident coexist: "+residents.stream().map(traders::job).toList());
        for(var job:MithrylTraders.Job.values())if(job!=MithrylTraders.Job.NONE){
            var resident=residents.stream().filter(r->traders.job(r)==job).findFirst().orElseThrow();
            Merchant merchant=traders.merchant(resident);check(merchant.getRecipeCount()==4,"four native merchant offers for "+job);
            check(merchant.getRecipes().stream().allMatch(r->r.getMaxUses()>0&&r.shouldIgnoreDiscounts()),"bounded stock and stable pricing");
            var recipe=merchant.getRecipes().get(1);var cost=recipe.getIngredients();
            check(MithrylTraders.matches(cost,cost.getFirst(),cost.size()>1?cost.get(1):null),"real ingredients accepted");
        }
        var fake=new ItemStack(Material.PRISMARINE_SHARD,8);var cost=List.of(items.create("ingot"));
        check(!MithrylTraders.matches(cost,fake,null),"plain prismarine cannot buy custom trades");
        var meta=fake.getItemMeta();meta.itemName(items.create("ingot").getItemMeta().itemName());fake.setItemMeta(meta);
        check(!MithrylTraders.matches(cost,fake,null),"renamed imitation cannot buy custom trades");
        var free=residents.stream().filter(r->traders.job(r)==MithrylTraders.Job.NONE).findFirst().orElseThrow();
        Block station=free.getLocation().getBlock().getRelative(1,0,1);stations.set(station,"grove_bench");traders.refreshJob(free);
        check(traders.job(free)==MithrylTraders.Job.GARDENER,"unemployed resident adopts player workstation");
        check(stations.owner(station).equals(free.getUniqueId().toString()),"workplace records exclusive owner");
        stations.set(station,"relic_altar");traders.refreshJob(free);
        check(traders.job(free)==MithrylTraders.Job.RELIC,"untraded resident changes profession");
        free.getPersistentDataContainer().set(plugin.key("trader_locked"),PersistentDataType.BYTE,(byte)1);
        stations.set(station,"mithryl_forge");traders.refreshJob(free);
        check(traders.job(free)==MithrylTraders.Job.RELIC&&stations.owner(station).isEmpty(),"traded profession does not reroll at different workstation");
        stations.set(station,"relic_altar");traders.refreshJob(free);
        check(stations.owner(station).equals(free.getUniqueId().toString()),"locked profession reacquires matching workstation");
        var protectedBreak=new BlockBreakEvent(station,player);protectedBreak.setCancelled(true);Bukkit.getPluginManager().callEvent(protectedBreak);
        check(stations.id(station).equals("relic_altar"),"cancelled station break preserves identity");
        var piston=new org.bukkit.event.block.BlockPistonExtendEvent(station,List.of(station),org.bukkit.block.BlockFace.UP);Bukkit.getPluginManager().callEvent(piston);check(piston.isCancelled(),"workstations cannot be moved by piston");
        Block ordinary=station.getRelative(0,0,1);ordinary.setType(Material.PURPUR_BLOCK,false);check(stations.id(ordinary).isEmpty(),"ordinary workstation carrier remains vanilla");
        for(String id:MithrylWorkstations.MATERIALS.keySet()){
            check(Bukkit.getRecipe(plugin.key(id))!=null,"workstation recipe registered "+id);
            check(items.create(id).getItemMeta().getItemModel().equals(plugin.key(id)),"workstation custom item model "+id);
        }
        for(String id:MithrylItems.RELICS)check(items.is(items.create(id),id)&&items.create(id).getItemMeta().getItemModel().equals(plugin.key("mithryl_"+id)),"custom relic metadata "+id);
        check(((Damageable)items.create("bloom_wand").getItemMeta()).getMaxDamage()==32,"wand has 32 charges");
        checkRelics(plugin,items,wood,center);
        var smith=residents.stream().filter(r->traders.job(r)==MithrylTraders.Job.SMITH).findFirst().orElseThrow();
        Merchant merchant=traders.merchant(smith);var offer=merchant.getRecipe(0);offer.setUses(offer.getMaxUses());merchant.setRecipe(0,offer);
        smith.getPersistentDataContainer().set(plugin.key("trader_restock_smith"),PersistentDataType.LONG,System.currentTimeMillis()-1);traders.refreshJob(smith);
        check(traders.merchant(smith).getRecipe(0).getUses()==0,"due restock resets stock at valid workstation");
        merchant=traders.merchant(smith);offer=merchant.getRecipe(0);offer.setUses(3);merchant.setRecipe(0,offer);
        smith.getPersistentDataContainer().set(plugin.key("trader_locked"),PersistentDataType.BYTE,(byte)1);
        Method save=MithrylTraders.class.getDeclaredMethod("save",org.bukkit.entity.ArmorStand.class);save.setAccessible(true);save.invoke(traders,smith);
        check(smith.getPersistentDataContainer().get(plugin.key("trader_uses_smith"),PersistentDataType.INTEGER_ARRAY)[0]==3,"trade use counts saved to entity PDC");
        Files.writeString(Path.of("trader-restart.txt"),smith.getUniqueId()+","+smith.getChunk().getX()+","+smith.getChunk().getZ());
    }
    void checkRelics(EndUpdatePlugin plugin,MithrylItems items,EndWood wood,Location center){
        MithrylRelics relics=new MithrylRelics(plugin,items,wood);
        var pdc=new ItemStack(Material.STONE).getItemMeta().getPersistentDataContainer();
        Map<PotionEffectType,PotionEffect> effects=new HashMap<>();int[] food={10};float[] saturation={0};
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()){
            case "getPersistentDataContainer"->pdc;case "getGameMode"->GameMode.SURVIVAL;case "hasCooldown"->false;
            case "getLocation"->center;case "getWorld"->center.getWorld();case "getFoodLevel"->food[0];case "getSaturation"->saturation[0];
            case "setFoodLevel"->{food[0]=(int)a[0];yield null;}case "setSaturation"->{saturation[0]=(float)a[0];yield null;}
            case "addPotionEffect"->{var effect=(PotionEffect)a[0];effects.put(effect.getType(),effect);yield true;}default->null;
        });
        ItemStack charm=items.create("feather_charm");
        var use=new org.bukkit.event.player.PlayerInteractEvent(player,org.bukkit.event.block.Action.RIGHT_CLICK_AIR,charm,null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.HAND);
        use.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);relics.use(use);
        check(effects.get(PotionEffectType.SLOW_FALLING).getDuration()==400&&charm.getAmount()==1,"charm grants twenty seconds slow falling without consumption");
        effects.clear();use.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);relics.use(use);
        check(effects.isEmpty()&&pdc.get(plugin.key("relic_ready_feather_charm"),PersistentDataType.LONG)>System.currentTimeMillis(),"persistent charm cooldown prevents reuse");
        ItemStack fruit=items.create("crystal_fruit");fruit.setAmount(3);
        use=new org.bukkit.event.player.PlayerInteractEvent(player,org.bukkit.event.block.Action.RIGHT_CLICK_AIR,fruit,null,org.bukkit.block.BlockFace.SELF,EquipmentSlot.HAND);
        use.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);relics.use(use);
        check(fruit.getAmount()==2&&food[0]==16&&saturation[0]>4,"crystal fruit consumes one and restores hunger/saturation");
        check(effects.get(PotionEffectType.REGENERATION).getAmplifier()==1&&effects.get(PotionEffectType.REGENERATION).getDuration()==100,"crystal fruit grants five seconds regeneration II");
        Block sapling=center.clone().add(35,0,0).getBlock();
        sapling.getRelative(0,-1,0).setType(Material.END_STONE,false);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=0;y<8;y++)sapling.getRelative(x,y,z).setType(Material.AIR,false);
        wood.set(sapling,"end_sapling");ItemStack wand=items.create("bloom_wand");
        var obstruction=sapling.getRelative(0,3,0);obstruction.setType(Material.DIAMOND_BLOCK,false);
        use=new org.bukkit.event.player.PlayerInteractEvent(player,org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,wand,sapling,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND);
        use.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);use.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);relics.use(use);
        check(((Damageable)wand.getItemMeta()).getDamage()==0&&wood.id(sapling).equals("end_sapling"),"failed wand growth consumes no charge");
        obstruction.setType(Material.AIR,false);use.setUseInteractedBlock(org.bukkit.event.Event.Result.ALLOW);use.setUseItemInHand(org.bukkit.event.Event.Result.ALLOW);relics.use(use);
        check(((Damageable)wand.getItemMeta()).getDamage()==1&&wood.id(sapling).equals("end_log"),"successful wand growth consumes exactly one charge");
    }
}
