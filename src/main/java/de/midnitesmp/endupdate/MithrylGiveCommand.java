package de.midnitesmp.endupdate;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public final class MithrylGiveCommand implements TabExecutor {
    private final MithrylItems items;
    public MithrylGiveCommand(MithrylItems items) { this.items = items; }

    record Request(String player, String item, int amount) {}

    static Request parse(String[] input) {
        String[] args = input.length > 0 && input[0].equalsIgnoreCase("give")
            ? Arrays.copyOfRange(input, 1, input.length) : input;
        if (args.length < 1 || args.length > 3) throw new IllegalArgumentException();
        String item = "ingot";
        int amount = 1;
        if (args.length == 2 && args[1].matches("[+-]?\\d+")) amount = Integer.parseInt(args[1]);
        else if (args.length >= 2) item = args[1].toLowerCase(Locale.ROOT);
        if (args.length == 3) amount = Integer.parseInt(args[2]);
        if (amount < 1 || amount > 64) throw new IllegalArgumentException();
        return new Request(args[0], item, amount);
    }

    public boolean give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("endupdate.admin")) {
            sender.sendMessage("§cDafuer fehlt endupdate.admin.");
            return true;
        }
        try {
            Request request = parse(args);
            ItemStack item = items.create(request.item());
            Player target = Bukkit.getPlayerExact(request.player());
            if (target == null) {
                sender.sendMessage("§cSpieler ist nicht online.");
                return true;
            }
            int remaining = request.amount();
            while (remaining > 0) {
                int count = Math.min(remaining, item.getMaxStackSize());
                item.setAmount(count);
                target.getInventory().addItem(item.clone()).values().forEach(leftover ->
                    target.getWorld().dropItemNaturally(target.getLocation(), leftover));
                remaining -= count;
            }
            sender.sendMessage("§a" + target.getName() + " hat " + request.amount() + "× Mithryl " + request.item() + " erhalten.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§e/mithryl give <Spieler> [Item] [1–64] §7(Standard: ingot)");
            sender.sendMessage("§7Items: " + String.join(", ", itemNames()));
        }
        return true;
    }

    private List<String> itemNames() {
        List<String> names = new ArrayList<>(List.of("ingot", "shard", "template", "block", "ore"));
        names.addAll(MithrylItems.RELICS);
        names.addAll(MithrylWorkstations.MATERIALS.keySet());
        names.addAll(MithrylItems.GEAR.keySet());
        names.addAll(EndWood.MATERIALS.keySet());
        return names;
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return give(sender, args);
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] input) {
        if (!sender.hasPermission("endupdate.admin")) return List.of();
        boolean subcommand = input.length > 0 && input[0].equalsIgnoreCase("give");
        String[] args = subcommand ? Arrays.copyOfRange(input, 1, input.length) : input;
        List<String> choices = new ArrayList<>();
        if (args.length <= 1) {
            if (!subcommand) choices.add("give");
            Bukkit.getOnlinePlayers().forEach(p -> choices.add(p.getName()));
        } else if (args.length == 2) {
            choices.addAll(itemNames());
            choices.addAll(List.of("1", "4", "16", "64"));
        } else if (args.length == 3) choices.addAll(List.of("1", "4", "16", "64"));
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
