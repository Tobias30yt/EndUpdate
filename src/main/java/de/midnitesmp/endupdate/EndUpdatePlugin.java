package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class EndUpdatePlugin extends JavaPlugin implements Listener, TabExecutor {
    private MithrylItems items; private MithrylRecipes recipes; private MithrylSpeed speed; private ResourcePackHost pack;
    private MithrylGiveCommand give;
    private MithrylTraders traders;
    private ReleaseUpdateChecker updateChecker;
    public NamespacedKey key(String name){return new NamespacedKey(this,name);}
    @Override public void onEnable(){
        saveDefaultConfig();updateChecker=new ReleaseUpdateChecker(this);updateChecker.start();items=new MithrylItems(this);recipes=new MithrylRecipes(this,items);recipes.register();
        speed=new MithrylSpeed(this,items);CustomBlockDisplays displays=new CustomBlockDisplays(this,items);
        MithrylOre ore=new MithrylOre(this,items,displays);MithrylBlocks storage=new MithrylBlocks(this,items,displays);pack=new ResourcePackHost(this);
        EndWood wood=new EndWood(this,items,displays);EndVillages villages=new EndVillages(this,items,wood);
        MithrylWorkstations stations=new MithrylWorkstations(this,items,displays);
        traders=new MithrylTraders(this,items,stations);villages.setTraders(traders);
        for(Listener listener:List.of(this,recipes,speed,ore,pack,wood,villages,displays,new EndCityLoot(this,items),storage,stations,traders,new MithrylRelics(this,items,wood)))getServer().getPluginManager().registerEvents(listener,this);
        Objects.requireNonNull(getCommand("locatevillage")).setExecutor(villages);
        Bukkit.getWorlds().forEach(w->Arrays.stream(w.getLoadedChunks()).forEach(c->{
            var loaded=new org.bukkit.event.world.ChunkLoadEvent(c,false);
            wood.load(loaded);ore.load(loaded);storage.load(loaded);traders.load(c);
            if(getConfig().getBoolean("villages.generate-in-existing-chunks",false))villages.enqueue(c);
        }));
        try{pack.start();}catch(Exception ex){pack.close();getLogger().log(java.util.logging.Level.SEVERE,"Cannot host resource pack. Gameplay remains enabled.",ex);}
        Objects.requireNonNull(getCommand("speed")).setExecutor(this);
        Objects.requireNonNull(getCommand("endupdate")).setExecutor(this);
        Objects.requireNonNull(getCommand("endupdate")).setTabCompleter(this);
        give = new MithrylGiveCommand(items);
        Objects.requireNonNull(getCommand("mithryl")).setExecutor(give);
        Objects.requireNonNull(getCommand("mithryl")).setTabCompleter(give);
        if(getConfig().getBoolean("ore.generate-in-existing-chunks",true))Bukkit.getWorlds().forEach(w->Arrays.stream(w.getLoadedChunks()).forEach(ore::enqueue));
        Bukkit.getOnlinePlayers().forEach(p->{p.discoverRecipes(recipes.keys());if(pack.ready())pack.offer(p);});
        getLogger().info("EndUpdate enabled: 9 Mithryl upgrades, End City loot, surface ore and resource pack.");
    }
    @Override public void onDisable(){if(updateChecker!=null)updateChecker.shutdown();if(traders!=null)traders.shutdown();if(speed!=null)Bukkit.getOnlinePlayers().forEach(speed::clear);if(pack!=null)pack.close();if(recipes!=null)recipes.unregister();}
    @EventHandler public void join(PlayerJoinEvent e){e.getPlayer().discoverRecipes(recipes.keys());}
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("speed")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Nur Spieler koennen ihren Speedbonus umschalten.");
                return true;
            }
            if (args.length > 1 || (args.length == 1
                    && !Set.of("on", "off").contains(args[0].toLowerCase(Locale.ROOT)))) {
                return false;
            }
            speed.toggle(player, args.length == 0 ? null : args[0].equalsIgnoreCase("on"));
            return true;
        }

        if (args.length == 0 || (args.length == 1
                && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")))) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "pack" -> {
                if (args.length != 1) {
                    sender.sendMessage("§eVerwendung: /endupdate pack");
                } else if (sender instanceof Player player) {
                    pack.offer(player);
                } else {
                    sender.sendMessage("§cNur Spieler koennen das Ressourcenpaket erneut anfordern.");
                }
                return true;
            }
            case "status" -> {
                if (args.length != 1) {
                    sender.sendMessage("§eVerwendung: /endupdate status");
                    return true;
                }
                sender.sendMessage("§bEndUpdate " + getPluginMeta().getVersion() + " §7| Paper "
                        + Bukkit.getMinecraftVersion() + " | Pack: " + (pack.ready() ? "aktiv" : "inaktiv"));
                if (sender instanceof Player player) {
                    sender.sendMessage("§7Dein Pack: " + pack.status(player));
                }
                if (sender.hasPermission("endupdate.admin")) {
                    sender.sendMessage("§7Pack-Test: " + pack.localUrl());
                }
                return true;
            }
            case "give" -> {
                return give.give(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            default -> {
                return false;
            }
        }
    }
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§bEndUpdate §7• Mithryl-Erze und Upgrades aus dem Äußeren End");
        sender.sendMessage("§74 Mithryl-Shards + 4 Diamanten → 1 Ingot; Template + Netherite + Ingot → Mithryl.");
        sender.sendMessage("§7Template kopieren: 7 Diamanten + Template + Endstein. /speed schaltet den Rüstungsbonus um.");
        sender.sendMessage("§7/endupdate pack | /endupdate status | /endupdate help");
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        List<String> choices=new ArrayList<>();
        if(command.getName().equals("speed"))choices.addAll(List.of("on","off"));
        else if(args.length==1){choices.addAll(List.of("pack","status","help"));if(sender.hasPermission("endupdate.admin"))choices.add("give");}
        else if(args[0].equalsIgnoreCase("give")&&sender.hasPermission("endupdate.admin")){
            if(args.length==2)Bukkit.getOnlinePlayers().forEach(p->choices.add(p.getName()));
            else if(args.length==3){choices.addAll(List.of("shard","ingot","template","block","ore"));choices.addAll(MithrylItems.GEAR.keySet());choices.addAll(EndWood.MATERIALS.keySet());choices.addAll(MithrylItems.RELICS);choices.addAll(MithrylWorkstations.MATERIALS.keySet());}
            else if(args.length==4)choices.addAll(List.of("1","4","16","64"));
        }
        String prefix=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
