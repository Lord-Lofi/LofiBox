package dev.lofibox.integration;

import net.essentialsx.api.v2.services.discord.DiscordService;
import net.essentialsx.api.v2.services.discord.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EssDiscordHook {

    private DiscordService discord;
    private MessageType messageType;

    public boolean setup(JavaPlugin plugin, String channelKey) {
        RegisteredServiceProvider<DiscordService> rsp =
                Bukkit.getServicesManager().getRegistration(DiscordService.class);
        if (rsp == null) return false;
        discord = rsp.getProvider();
        if (discord == null) return false;

        messageType = new MessageType(channelKey);
        discord.registerMessageType(plugin, messageType);
        return true;
    }

    public boolean isAvailable() {
        return discord != null && messageType != null;
    }

    public void sendMessage(String message) {
        if (!isAvailable()) return;
        try {
            discord.sendMessage(messageType, message, false);
        } catch (Exception ignored) {}
    }
}
