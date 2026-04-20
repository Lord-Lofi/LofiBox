package dev.lofibox.commands;

import dev.lofibox.LofiBox;
import dev.lofibox.box.MysteryBox;
import dev.lofibox.gui.PreviewGui;
import dev.lofibox.gui.StatsGui;
import dev.lofibox.key.KeyTier;
import dev.lofibox.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LofiBoxCommand implements CommandExecutor, TabCompleter {

    private final LofiBox plugin;

    public LofiBoxCommand(LofiBox plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"  -> handleReload(sender);
            case "list"    -> handleList(sender);
            case "give"    -> handleGive(sender, args);
            case "givekey" -> handleGiveKey(sender, args);
            case "open"    -> handleOpen(sender, args);
            case "preview" -> handlePreview(sender, args);
            case "stats"   -> handleStats(sender, args);
            case "editor"  -> handleEditor(sender);
            default        -> sendHelp(sender);
        }
        return true;
    }

    // ── Subcommands ───────────────────────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("lofibox.reload")) {
            msg(sender, "no-permission");
            return;
        }
        plugin.reload();
        sender.sendMessage(plugin.getMessageConfig().get("reload"));
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(plugin.getMessageConfig().get("list-header"));
        for (MysteryBox box : plugin.getBoxManager().getAllBoxes()) {
            sender.sendMessage(plugin.getMessageConfig().get("list-line",
                "box", box.getDisplayName(), "id", box.getId()));
        }
        sender.sendMessage(plugin.getMessageConfig().get("list-footer"));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofibox.give")) {
            msg(sender, "no-permission"); return;
        }
        if (args.length < 3) { sender.sendMessage("§cUsage: /lofibox give <box> <player> [amount]"); return; }

        String boxId    = args[1];
        String target   = args[2];
        int amount      = args.length >= 4 ? parseInt(args[3], 1) : 1;

        Player targetPlayer = Bukkit.getPlayerExact(target);
        if (targetPlayer == null) { msg(sender, "unknown-player", "player", target); return; }

        MysteryBox box = plugin.getBoxManager().getBox(boxId);
        if (box == null) { msg(sender, "unknown-box", "box", boxId); return; }

        ItemStack item = plugin.getBoxManager().createBoxItem(boxId, amount);
        Map<Integer, ItemStack> leftover = targetPlayer.getInventory().addItem(item);
        leftover.values().forEach(it -> targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), it));

        msg(sender, "give-success", "box", box.getDisplayName(), "player", targetPlayer.getName(), "amount", String.valueOf(amount));
        plugin.getMessageConfig().send(targetPlayer, "give-received", "box", box.getDisplayName(), "amount", String.valueOf(amount));
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; }
        if (!player.hasPermission("lofibox.use")) { msg(sender, "no-permission"); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /lofibox open <box>"); return; }

        String boxId = args[1];
        MysteryBox box = plugin.getBoxManager().getBox(boxId);
        if (box == null) { msg(sender, "unknown-box", "box", boxId); return; }

        if (plugin.getBoxManager().isSpinning(player.getUniqueId())) {
            msg(sender, "already-spinning"); return;
        }

        if (!player.hasPermission("lofibox.admin")) {
            // Non-admins must have the box item in hand
            ItemStack item = player.getInventory().getItemInMainHand();
            String heldBoxId = plugin.getBoxManager().getBoxId(item);
            if (!boxId.equals(heldBoxId)) {
                msg(sender, "no-box-in-hand"); return;
            }

            // Vault cost check
            if (box.hasCost()) {
                var vault = plugin.getVaultHook();
                if (!vault.isAvailable()) {
                    msg(sender, "vault-unavailable"); return;
                }
                if (!vault.has(player, box.getOpenCost())) {
                    msg(sender, "vault-insufficient-funds",
                        "cost", vault.format(box.getOpenCost()),
                        "balance", vault.format(vault.getBalance(player)));
                    return;
                }
            }

            // Key check
            if (box.requiresKey()) {
                if (!plugin.getKeyManager().hasKey(player, box.getRequiredKey())) {
                    msg(sender, "key-required",
                        "key", box.getRequiredKey().getDisplayName(),
                        "box", box.getDisplayName());
                    return;
                }
            }

            // Consume box item
            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else player.getInventory().setItemInMainHand(null);

            // Consume key and deduct cost
            if (box.requiresKey()) {
                plugin.getKeyManager().consumeKey(player, box.getRequiredKey());
                msg(sender, "key-consumed", "key", box.getRequiredKey().getDisplayName());
            }
            if (box.hasCost()) {
                var vault = plugin.getVaultHook();
                vault.withdraw(player, box.getOpenCost());
                msg(sender, "vault-charged", "cost", vault.format(box.getOpenCost()));
            }
        }

        msg(sender, "box-opened", "box", box.getDisplayName());
        plugin.getBoxManager().openBox(player, box);
    }

    private void handleGiveKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("lofibox.give")) { msg(sender, "no-permission"); return; }
        if (args.length < 3) { sender.sendMessage("§cUsage: /lofibox givekey <tier> <player> [amount]"); return; }

        KeyTier tier;
        try {
            tier = KeyTier.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUnknown key tier. Valid tiers: wooden, stone, copper, iron, golden, diamond, netherite");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { msg(sender, "unknown-player", "player", args[2]); return; }

        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;
        ItemStack key = plugin.getKeyManager().createKey(tier, amount);
        Map<Integer, ItemStack> leftover = target.getInventory().addItem(key);
        leftover.values().forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));

        sender.sendMessage(plugin.getMessageConfig().get("give-success",
            "box", tier.getDisplayName(), "player", target.getName(), "amount", String.valueOf(amount)));
        target.sendMessage(plugin.getMessageConfig().get("give-received",
            "box", tier.getDisplayName(), "amount", String.valueOf(amount)));
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; }
        if (!player.hasPermission("lofibox.use")) { msg(sender, "no-permission"); return; }
        if (args.length < 2) { sender.sendMessage("§cUsage: /lofibox preview <box>"); return; }

        MysteryBox box = plugin.getBoxManager().getBox(args[1]);
        if (box == null) { msg(sender, "unknown-box", "box", args[1]); return; }

        new PreviewGui(plugin, box).open(player);
    }

    private void handleStats(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("lofibox.stats.others")) { msg(sender, "no-permission"); return; }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { msg(sender, "unknown-player", "player", args[1]); return; }
        } else {
            if (!(sender instanceof Player p)) { sender.sendMessage("§cSpecify a player."); return; }
            if (!p.hasPermission("lofibox.stats") && !p.hasPermission("lofibox.admin")) {
                msg(sender, "no-permission"); return;
            }
            target = p;
        }

        // Console/NPC with a target: open GUI on the target's screen
        if (!(sender instanceof Player)) {
            new StatsGui(plugin, target, target, 0).open();
            return;
        }

        // Player sender always gets the GUI
        Player viewer = (Player) sender;
        new StatsGui(plugin, viewer, target, 0).open();
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("give", "givekey", "open", "preview", "list", "stats", "reload", "editor"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("open") || sub.equals("preview")) {
                completions.addAll(plugin.getBoxManager().getBoxIds());
            } else if (sub.equals("givekey")) {
                for (KeyTier t : KeyTier.values()) completions.add(t.name().toLowerCase());
            } else if (sub.equals("stats")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("give") || sub.equals("givekey")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("givekey"))) {
            completions.addAll(List.of("1", "5", "10", "64"));
        }
        String partial = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(partial));
        return completions;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void handleEditor(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cPlayers only."); return; }
        if (!player.hasPermission("lofibox.editor")) { msg(sender, "no-permission"); return; }
        plugin.getEditorManager().openMainEditor(player);
    }

    private void msg(CommandSender sender, String key, String... replacements) {
        sender.sendMessage(plugin.getMessageConfig().get(key, replacements));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageConfig().get("list-header"));
        sender.sendMessage("§7/lofibox give §f<box> <player> [amount]");
        sender.sendMessage("§7/lofibox givekey §f<tier> <player> [amount]");
        sender.sendMessage("§7/lofibox open §f<box>");
        sender.sendMessage("§7/lofibox preview §f<box>");
        sender.sendMessage("§7/lofibox list");
        sender.sendMessage("§7/lofibox stats §f[player]");
        sender.sendMessage("§7/lofibox reload");
        sender.sendMessage("§7/lofibox editor");
    }

    private int parseInt(String s, int fallback) {
        try { return Math.max(1, Integer.parseInt(s)); }
        catch (NumberFormatException e) { return fallback; }
    }
}
