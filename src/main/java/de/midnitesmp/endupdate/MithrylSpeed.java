package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.*;
import java.util.*;

public final class MithrylSpeed implements Listener {
    private final MithrylItems items; private final NamespacedKey disabled;
    private final Set<UUID> owned=new HashSet<>(); private boolean changing;
    public MithrylSpeed(EndUpdatePlugin plugin,MithrylItems items){this.items=items;disabled=plugin.key("speed_disabled"); Bukkit.getScheduler().runTaskTimer(plugin,()->Bukkit.getOnlinePlayers().forEach(this::update),1,10);}
    public boolean enabled(Player p){return !p.getPersistentDataContainer().has(disabled);}
    public void toggle(Player p,Boolean enable){boolean state=enable==null?!enabled(p):enable; if(state)p.getPersistentDataContainer().remove(disabled);else p.getPersistentDataContainer().set(disabled,PersistentDataType.BYTE,(byte)1);update(p);p.sendMessage("§bMithryl-Speed: §f"+(state?"AN":"AUS"));}
    public void update(Player p){
        int count=(int)Arrays.stream(p.getInventory().getArmorContents()).filter(items::isGear).count();
        int amp=enabled(p)&&!p.isDead()?MithrylRules.speedAmplifier(count):-1;
        PotionEffect current=p.getPotionEffect(PotionEffectType.SPEED);
        if(amp<0){clear(p);return;}
        // Respect speed supplied by potions, beacons and other plugins until it expires.
        if(current!=null&&!owned.contains(p.getUniqueId()))return;
        changing=true;
        try{
            if(current!=null&&current.getAmplifier()!=amp)p.removePotionEffect(PotionEffectType.SPEED);
            if(p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,30,amp,true,false,true)))owned.add(p.getUniqueId());
        }finally{changing=false;}
    }
    public void clear(Player p){if(owned.remove(p.getUniqueId())){changing=true;try{p.removePotionEffect(PotionEffectType.SPEED);}finally{changing=false;}}}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void effect(EntityPotionEffectEvent e){if(!changing&&e.getEntity() instanceof Player p&&e.getModifiedType()==PotionEffectType.SPEED)owned.remove(p.getUniqueId());}
    @EventHandler public void quit(PlayerQuitEvent e){clear(e.getPlayer());}
}
