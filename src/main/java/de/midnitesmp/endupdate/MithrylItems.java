package de.midnitesmp.endupdate;

import com.google.common.collect.ArrayListMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.trim.*;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

public final class MithrylItems {
    public static final List<String> RELICS = List.of("bloom_wand", "feather_charm", "crystal_fruit");
    public static final Map<String, Material> GEAR = new LinkedHashMap<>();
    static {
        for (String kind : List.of("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"))
            GEAR.put(kind, Material.valueOf("NETHERITE_" + kind.toUpperCase(Locale.ROOT)));
    }
    private final EndUpdatePlugin plugin;
    private final NamespacedKey identity;
    public MithrylItems(EndUpdatePlugin plugin) { this.plugin = plugin; identity = plugin.key("item"); }
    public String id(ItemStack item) {
        return item == null || !item.hasItemMeta() ? "" : Objects.toString(item.getItemMeta().getPersistentDataContainer().get(identity, PersistentDataType.STRING), "");
    }
    public boolean is(ItemStack item, String id) { return id.equals(id(item)); }
    public boolean isGear(ItemStack item) { return GEAR.containsKey(id(item)) && GEAR.get(id(item)) == item.getType(); }
    public ItemStack create(String id) {
        if (EndWood.MATERIALS.containsKey(id)) return EndWood.item(plugin,id);
        if (MithrylWorkstations.MATERIALS.containsKey(id)) {
            ItemStack item = new ItemStack(MithrylWorkstations.MATERIALS.get(id));
            var meta = item.getItemMeta();
            mark(meta,id,switch(id){case "mithryl_forge" -> "Mithrylschmiede"; case "grove_bench" -> "Hainwerkbank"; default -> "Reliktaltar";});
            meta.setItemModel(plugin.key(id));
            meta.lore(List.of(line("Arbeitsplatz: " + MithrylTraders.Job.forStation(id).title)));
            item.setItemMeta(meta); return item;
        }
        if (RELICS.contains(id)) {
            ItemStack item = new ItemStack(id.equals("bloom_wand") ? Material.CARROT_ON_A_STICK : Material.AMETHYST_SHARD);
            var meta = item.getItemMeta();
            mark(meta,id,switch(id){case "bloom_wand" -> "Bluetenstab"; case "feather_charm" -> "Federfall-Talisman"; default -> "Kristallfrucht";});
            meta.setMaxStackSize(id.equals("crystal_fruit") ? 16 : 1);
            meta.lore(List.of(line(switch(id){
                case "bloom_wand" -> "Rechtsklick auf End-Setzling · 32 Anwendungen";
                case "feather_charm" -> "Rechtsklick: 20 s sanfter Fall · 60 s Abklingzeit";
                default -> "Rechtsklick: 3 Hungerkeulen + Regeneration II (5 s)";
            })));
            if (meta instanceof Damageable damage) damage.setMaxDamage(32);
            var cooldown = meta.getUseCooldown(); cooldown.setCooldownGroup(plugin.key(id));
            cooldown.setCooldownSeconds(id.equals("feather_charm") ? 60 : 1); meta.setUseCooldown(cooldown);
            item.setItemMeta(meta); return item;
        }
        if (GEAR.containsKey(id)) return upgrade(new ItemStack(GEAR.get(id)));
        Material material = switch (id) {
            case "shard" -> Material.PRISMARINE_CRYSTALS;
            case "ingot" -> Material.PRISMARINE_SHARD;
            case "block" -> MithrylBlocks.STORAGE_BLOCK;
            case "ore" -> MithrylOre.ORE_BLOCK;
            case "template" -> Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE;
            default -> throw new IllegalArgumentException("Unknown item: " + id);
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        mark(meta, id, switch (id) { case "shard" -> "Mithryl Shard"; case "ingot" -> "Mithryl Ingot"; case "block" -> "Mithryl Block"; case "ore" -> "Mithryl Ore"; default -> "Mithryl Upgrade Template"; });
        meta.lore(List.of(line(switch (id) {
            case "shard" -> "4 Shards + 4 Diamanten = 1 Mithryl Ingot";
            case "ingot" -> "Veredelt Netherite am Schmiedetisch.";
            case "block" -> "9 Mithryl Ingots · platzierbar und wieder zerlegbar";
            case "ore" -> "Mithryl-Erz";
            default -> "Netherite + Mithryl Ingot → Mithryl";
        })));
        item.setItemMeta(meta);
        return item;
    }
    public ItemStack upgrade(ItemStack original) {
        String kind = GEAR.entrySet().stream().filter(e -> e.getValue() == original.getType()).map(Map.Entry::getKey).findFirst().orElseThrow();
        ItemStack result = original.clone(); result.setAmount(1);
        ItemMeta meta = result.getItemMeta();
        mark(meta, kind, "Mithryl " + Character.toUpperCase(kind.charAt(0)) + kind.substring(1));
        List<Component> lore = new ArrayList<>(Optional.ofNullable(meta.lore()).orElse(List.of()));
        if (meta instanceof ArmorMeta armor) {
            armor.setTrim(new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.RIB));
            var equipment = meta.getEquippable();
            equipment.setSlot(switch(kind) { case "helmet" -> EquipmentSlot.HEAD; case "chestplate" -> EquipmentSlot.CHEST; case "leggings" -> EquipmentSlot.LEGS; default -> EquipmentSlot.FEET; });
            equipment.setModel(plugin.key("mithryl"));
            meta.setEquippable(equipment);
            lore.add(line("1–3 Teile: Speed I · 4 Teile: Speed II"));
            lore.add(line("Bonus umschalten: /speed"));
        } else {
            // An explicit attribute component replaces defaults: copy them before adding bonuses.
            if (!meta.hasAttributeModifiers()) meta.setAttributeModifiers(ArrayListMultimap.create(original.getType().getDefaultAttributeModifiers(EquipmentSlot.HAND)));
            meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(plugin.key("mining_speed"),
                Math.max(0, plugin.getConfig().getDouble("tools.mining-speed-multiplier", 1.3) - 1), AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.MAINHAND));
            meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(plugin.key("attack_speed"),
                Math.max(0, plugin.getConfig().getDouble("tools.attack-speed-multiplier", 1.2) - 1), AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.MAINHAND));
            lore.add(line("Schnellerer Abbau und kuerzerer Angriffscooldown"));
        }
        meta.lore(lore); result.setItemMeta(meta); return result;
    }
    private void mark(ItemMeta meta, String id, String name) {
        meta.getPersistentDataContainer().set(identity, PersistentDataType.STRING, id);
        meta.itemName(Component.text(name, NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        meta.setItemModel(plugin.key("mithryl_" + id));
    }
    private static Component line(String value) { return Component.text(value, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false); }
}
