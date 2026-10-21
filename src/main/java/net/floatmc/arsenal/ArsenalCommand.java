package net.floatmc.arsenal;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** /floatarsenal give <jugador> <arma|all> [cantidad] | list | reload */
public final class ArsenalCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "&6[FloatArsenal] &7";

    private final FloatArsenal plugin;

    public ArsenalCommand(FloatArsenal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender, label);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "list" -> list(sender);
            case "reload" -> {
                plugin.reloadAll();
                sender.sendMessage(Txt.msg(PREFIX + "Configuración y recetas recargadas."));
            }
            case "give" -> give(sender, label, args);
            default -> help(sender, label);
        }
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(Txt.msg(PREFIX + "Comandos:"));
        sender.sendMessage(Txt.msg("&e/" + label + " give <jugador> <arma|all> [cantidad] &8- &7Entrega armas"));
        sender.sendMessage(Txt.msg("&e/" + label + " list &8- &7Lista las armas"));
        sender.sendMessage(Txt.msg("&e/" + label + " reload &8- &7Recarga config y recetas"));
    }

    private void list(CommandSender sender) {
        sender.sendMessage(Txt.msg(PREFIX + "Armas disponibles:"));
        for (WeaponType t : WeaponType.values()) {
            sender.sendMessage(Txt.msg("&8- &e" + t.id() + " &7(" + t.activeName() + ")"));
        }
    }

    private void give(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Txt.msg(PREFIX + "&cUso: /" + label + " give <jugador> <arma|all> [cantidad]"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Txt.msg(PREFIX + "&cEl jugador '" + args[1] + "' no está conectado."));
            return;
        }
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(Txt.msg(PREFIX + "&cLa cantidad debe ser un número."));
                return;
            }
            if (amount < 1 || amount > 10) {
                sender.sendMessage(Txt.msg(PREFIX + "&cLa cantidad debe estar entre 1 y 10."));
                return;
            }
        }

        List<WeaponType> toGive = new ArrayList<>();
        if (args[2].equalsIgnoreCase("all")) {
            for (WeaponType t : WeaponType.values()) {
                toGive.add(t);
            }
        } else {
            WeaponType t = WeaponType.byId(args[2]);
            if (t == null) {
                sender.sendMessage(Txt.msg(PREFIX + "&cArma desconocida. Usa /" + label + " list"));
                return;
            }
            toGive.add(t);
        }

        for (WeaponType t : toGive) {
            for (int i = 0; i < amount; i++) {
                ItemStack item = t.create(plugin);
                Map<Integer, ItemStack> leftover = target.getInventory().addItem(item);
                for (ItemStack extra : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), extra);
                }
            }
        }
        sender.sendMessage(Txt.msg(PREFIX + "Entregado a &e" + target.getName() + "&7: &e"
                + (toGive.size() == 1 ? toGive.get(0).id() : "todas las armas") + " &7x" + amount));
        if (!sender.equals(target)) {
            target.sendMessage(Txt.msg(PREFIX + "¡Has recibido un arma legendaria!"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatches(out, args[0], List.of("give", "list", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            addMatches(out, args[1], names);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            List<String> ids = new ArrayList<>();
            ids.add("all");
            for (WeaponType t : WeaponType.values()) {
                ids.add(t.id());
            }
            addMatches(out, args[2], ids);
        } else if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            addMatches(out, args[3], List.of("1", "2", "5", "10"));
        }
        return out;
    }

    private static void addMatches(List<String> out, String typed, List<String> options) {
        String lower = typed.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(option);
            }
        }
    }
}
