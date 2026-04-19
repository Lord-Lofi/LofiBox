package dev.lofibox.editor;

import dev.lofibox.LofiBox;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class ChatInputManager implements Listener {

    private final LofiBox plugin;
    private final Map<UUID, PendingInput> pending = new HashMap<>();

    public ChatInputManager(LofiBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Closes the player's inventory, shows a prompt, and awaits their next chat message.
     * The callback runs on the main thread. If the player types "cancel", the callback
     * is skipped and cancelInventory (if non-null) is reopened instead.
     */
    public void await(Player player, String prompt, Inventory cancelInventory, Consumer<String> callback) {
        pending.put(player.getUniqueId(), new PendingInput(callback, cancelInventory));
        player.closeInventory();
        player.sendMessage(plugin.getMessageConfig().get("editor-input-prompt", "prompt", prompt));
        player.sendMessage(plugin.getMessageConfig().get("editor-input-cancel"));
    }

    public boolean hasPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent e) {
        PendingInput input = pending.remove(e.getPlayer().getUniqueId());
        if (input == null) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Player player = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.equalsIgnoreCase("cancel")) {
                if (input.cancelInventory() != null) player.openInventory(input.cancelInventory());
            } else {
                input.callback().accept(text);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        pending.remove(e.getPlayer().getUniqueId());
    }

    private record PendingInput(Consumer<String> callback, Inventory cancelInventory) {}
}
