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
        registerWood();
        Bukkit.addRecipe(new ShapedRecipe(key("mithryl_forge"),items.create("mithryl_forge")).shape("III","DED","PPP")
            .setIngredient('I',exact("ingot")).setIngredient('D',Material.POLISHED_DEEPSLATE).setIngredient('E',Material.FURNACE).setIngredient('P',exact("end_planks")));
        Bukkit.addRecipe(new ShapedRecipe(key("grove_bench"),items.create("grove_bench")).shape("SIS","PPP","P P")
            .setIngredient('S',exact("end_sapling")).setIngredient('I',exact("ingot")).setIngredient('P',exact("end_planks")));
        Bukkit.addRecipe(new ShapedRecipe(key("relic_altar"),items.create("relic_altar")).shape(" S ","IPI","PPP")
            .setIngredient('S',Material.AMETHYST_SHARD).setIngredient('I',exact("ingot")).setIngredient('P',Material.PURPUR_BLOCK));
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
    private void registerWood() {
        for(String source:List.of("log","wood","stripped_log","stripped_wood")) {
            ItemStack planks=items.create("end_planks");planks.setAmount(4);
            Bukkit.addRecipe(new ShapelessRecipe(key("planks_from_"+source),planks).addIngredient(new RecipeChoice.ExactChoice(items.create("end_"+source))));
        }
        woodRecipe("wood",3,"log","PP","PP");
        woodRecipe("stripped_wood",3,"stripped_log","PP","PP");
        woodRecipe("stairs",4,"planks","P  ","PP ","PPP");
        woodRecipe("slab",6,"planks","PPP");
        woodRecipe("fence",3,"planks","PSP","PSP");
        woodRecipe("fence_gate",1,"planks","SPS","SPS");
        woodRecipe("door",3,"planks","PP","PP","PP");
        woodRecipe("trapdoor",2,"planks","PPP","PPP");
        woodRecipe("pressure_plate",1,"planks","PP");
        woodRecipe("button",1,"planks","P");
        Bukkit.addRecipe(new ShapedRecipe(key("end_sticks"),new ItemStack(Material.STICK,4)).shape("P","P").setIngredient('P',new RecipeChoice.ExactChoice(items.create("end_planks"))));
        Bukkit.addRecipe(new ShapedRecipe(key("end_crafting_table"),new ItemStack(Material.CRAFTING_TABLE)).shape("PP","PP").setIngredient('P',new RecipeChoice.ExactChoice(items.create("end_planks"))));
    }
    private void woodRecipe(String result,int count,String ingredient,String... shape) {
        ItemStack out=items.create("end_"+result);out.setAmount(count);
        ShapedRecipe recipe=new ShapedRecipe(key("end_"+result),out).shape(shape).setIngredient('P',new RecipeChoice.ExactChoice(items.create("end_"+ingredient)));
        if(Arrays.stream(shape).anyMatch(s->s.contains("S")))recipe.setIngredient('S',Material.STICK);
        Bukkit.addRecipe(recipe);
    }
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
