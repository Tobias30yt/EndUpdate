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
        try{runChecks();String result="PASS: "+assertions+" integration assertions on Paper "+Bukkit.getMinecraftVersion();getLogger().info(result);Files.writeString(Path.of("integration-result.txt"),result);}
        catch(Throwable ex){getLogger().log(java.util.logging.Level.SEVERE,"INTEGRATION FAILED",ex);try{Files.writeString(Path.of("integration-result.txt"),"FAIL: "+ex);}catch(Exception ignored){}}
    },30);}
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
        MithrylOre ore=new MithrylOre(plugin,items);Method generate=MithrylOre.class.getDeclaredMethod("generate",Chunk.class);generate.setAccessible(true);generate.invoke(ore,chunk);
        List<Block> ores=new ArrayList<>();for(int x=0;x<16;x++)for(int z=0;z<16;z++){Block b=end.getBlockAt(2048+x,100,2048+z);if(ore.isOre(b))ores.add(b);}
        check(!ores.isEmpty(),"outer surface ores generated");
        int[] before=chunk.getPersistentDataContainer().get(plugin.key("ore_locations"),PersistentDataType.INTEGER_ARRAY);generate.invoke(ore,chunk);check(Arrays.equals(before,chunk.getPersistentDataContainer().get(plugin.key("ore_locations"),PersistentDataType.INTEGER_ARRAY)),"generation idempotent");
        Block placed=end.getBlockAt(2048,101,2048);placed.setType(Material.PRISMARINE,false);check(!ore.isOre(placed),"placed prismarine not ore");
        ItemStack[] held={new ItemStack(Material.DIAMOND_PICKAXE)};ItemStack[][] armor={new ItemStack[4]};Map<PotionEffectType,PotionEffect> effects=new HashMap<>();
        PlayerInventory inventory=proxy(PlayerInventory.class,(p,m,a)->switch(m.getName()){case "getItemInMainHand"->held[0];case "getArmorContents"->armor[0];default->null;});
        var pdc=new ItemStack(Material.STONE).getItemMeta().getPersistentDataContainer();UUID uuid=UUID.randomUUID();
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()){
            case "getInventory"->inventory;case "getGameMode"->GameMode.SURVIVAL;case "getUniqueId"->uuid;case "isDead"->false;case "getPersistentDataContainer"->pdc;
            case "getPotionEffect"->effects.get(a[0]);case "addPotionEffect"->{var effect=(PotionEffect)a[0];effects.put(effect.getType(),effect);yield true;}
            case "removePotionEffect"->{effects.remove(a[0]);yield null;}case "toString"->"VerificationPlayer";default->null;});
        var mine=new BlockBreakEvent(ores.getFirst(),player);ore.breakOre(mine);check(mine.isCancelled(),"diamond cannot mine");
        held[0]=items.create("pickaxe");mine=new BlockBreakEvent(ores.getFirst(),player);ore.breakOre(mine);check(!mine.isCancelled()&&!mine.isDropItems(),"mithryl can mine, no vanilla drops");ore.dropOre(mine);check(!ore.isOre(ores.getFirst()),"ore marker consumed");
        MithrylBlocks storage=new MithrylBlocks(plugin,items);
        Block storageBlock=end.getBlockAt(2050,102,2050);var replaced=storageBlock.getState();storageBlock.setType(Material.PRISMARINE_BRICKS,false);
        check(!storage.isStorage(storageBlock),"unmarked bricks not Mithryl");
        var placement=new org.bukkit.event.block.BlockPlaceEvent(storageBlock,replaced,storageBlock.getRelative(org.bukkit.block.BlockFace.DOWN),items.create("block"),player,true,EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(placement);check(storage.isStorage(storageBlock),"placed custom block tracked");
        check(new MithrylBlocks(plugin,items).isStorage(storageBlock),"storage identity in persistent chunk data");
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
        plugin.reloadConfig();
    }
}
