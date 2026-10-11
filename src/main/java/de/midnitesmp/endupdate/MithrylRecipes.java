package de.midnitesmp.endupdate;

import org.bukkit.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

public final class MithrylRecipes implements Listener {
    private final EndUpdatePlugin plugin;
    private final MithrylItems items;
    private final Set<NamespacedKey> keys = new HashSet<>();
    public MithrylRecipes(EndUpdatePlugin plugin, MithrylItems items) { this.plugin = plugin; this.items = items; }
    public void register() {
        ShapelessRecipe ingot = new ShapelessRecipe(key("mithryl_ingot"), items.create("ingot"));
        for (int i = 0; i < 4; i++) { ingot.addIngredient(Material.DIAMOND); ingot.addIngredient(exact("shard")); }
        Bukkit.addRecipe(ingot);
        ShapedRecipe trident = new ShapedRecipe(key("mithryl_trident"), new ItemStack(Material.TRIDENT))
            .shape(" SS", " TS", "T  ")
            .setIngredient('S', exact("shard"))
            .setIngredient('T', Material.STICK);
        Bukkit.addRecipe(trident);
        ShapedRecipe block = new ShapedRecipe(key("mithryl_block"), items.create("block"))
            .shape("III", "III", "III").setIngredient('I', exact("ingot"));
        Bukkit.addRecipe(block);
        ItemStack unpacked = items.create("ingot"); unpacked.setAmount(9);
        Bukkit.addRecipe(new ShapelessRecipe(key("unpack_mithryl_block"), unpacked)
            .addIngredient(exact("block")));
        ItemStack doubled = items.create("template"); doubled.setAmount(2);
        ShapedRecipe template = new ShapedRecipe(key("template_copy"), doubled).shape("DTD", "DED", "DDD")
            .setIngredient('D', Material.DIAMOND).setIngredient('T', exact("template")).setIngredient('E', Material.END_STONE);
        Bukkit.addRecipe(template);
        MithrylItems.GEAR.forEach((kind, material) -> Bukkit.addRecipe(new SmithingTransformRecipe(key("upgrade_" + kind), items.create(kind),
            exact("template"), new RecipeChoice.MaterialChoice(material), exact("ingot"))));
    }
    private RecipeChoice.ExactChoice exact(String id) { return new RecipeChoice.ExactChoice(List.of(items.create(id))); }
    private NamespacedKey key(String id) { NamespacedKey key = plugin.key(id); keys.add(key); Bukkit.removeRecipe(key); return key; }
    public Set<NamespacedKey> keys() { return Set.copyOf(keys); }
    public void unregister() { keys.forEach(Bukkit::removeRecipe); }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void smith(PrepareSmithingEvent event) {
        SmithingInventory inv = event.getInventory();
        ItemStack base = inv.getInputEquipment(), template = inv.getInputTemplate(), addition = inv.getInputMineral();
        if (items.is(template, "template") || items.is(addition, "ingot")) {
            if (items.is(template, "template") && items.is(addition, "ingot") && base != null
                && MithrylItems.GEAR.containsValue(base.getType()) && !items.isGear(base)) event.setResult(items.upgrade(base));
            else event.setResult(null);
        } else if (items.isGear(base)) {
            // The signature rib trim is part of the Mithryl appearance.
            event.setResult(null);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void craft(PrepareItemCraftEvent event) {
        boolean custom = Arrays.stream(event.getInventory().getMatrix()).anyMatch(i -> !items.id(i).isEmpty());
        if (custom && (!(event.getRecipe() instanceof Keyed keyed) || !keys.contains(keyed.getKey()))) event.getInventory().setResult(null);
    }
}
