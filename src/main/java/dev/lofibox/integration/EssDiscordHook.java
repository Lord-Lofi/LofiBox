package dev.lofibox.integration;

import net.essentialsx.api.v2.services.discord.DiscordService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Hooks into EssentialsXDiscord without importing the channel type directly.
 * Reflection is used so the hook compiles and works regardless of which EssX
 * version is installed (the MessageChannel type changed in 2.20.x).
 */
public final class EssDiscordHook {

    private DiscordService discord;
    private Object channel;       // type varies by EssX version, held as Object
    private Method sendMethod;    // discord.sendMessage(channel, String, boolean)

    /**
     * Attempts to hook into EssentialsXDiscord. Returns true if fully set up.
     * channelDefinition is the channel name from EssX Discord's channels config (e.g. "chat").
     */
    public boolean setup(JavaPlugin plugin, String channelDefinition) {
        RegisteredServiceProvider<DiscordService> rsp =
                Bukkit.getServicesManager().getRegistration(DiscordService.class);
        if (rsp == null) return false;
        discord = rsp.getProvider();
        if (discord == null) return false;

        try {
            // Resolve the channel object via the interface, not the concrete class
            Method getChannel = null;
            for (Method m : DiscordService.class.getMethods()) {
                if (m.getName().equals("getDefinedChannel") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    getChannel = m;
                    break;
                }
            }
            if (getChannel == null) {
                plugin.getLogger().warning("EssDiscord: getDefinedChannel not found on DiscordService. Discord announcements disabled.");
                return false;
            }
            channel = getChannel.invoke(discord, channelDefinition);
            if (channel == null) {
                plugin.getLogger().warning("EssDiscord: channel '" + channelDefinition + "' returned null. Check your EssentialsXDiscord config.");
                return false;
            }

            // Find sendMessage(channelType, String, boolean) on the DiscordService interface
            for (Method m : DiscordService.class.getMethods()) {
                if (m.getName().equals("sendMessage") && m.getParameterCount() == 3) {
                    Class<?>[] p = m.getParameterTypes();
                    if (p[0].isInstance(channel) && p[1] == String.class && p[2] == boolean.class) {
                        sendMethod = m;
                        break;
                    }
                }
            }

            if (sendMethod == null) {
                plugin.getLogger().warning("EssDiscord: could not find a compatible sendMessage method. Discord announcements disabled.");
                return false;
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("EssDiscord: setup failed — " + e.getMessage());
            return false;
        }
    }

    public boolean isAvailable() {
        return discord != null && channel != null && sendMethod != null;
    }

    /** Sends a plain-text message to the configured EssX Discord channel. */
    public void sendMessage(String message) {
        if (!isAvailable()) return;
        try {
            sendMethod.invoke(discord, channel, message, false);
        } catch (Exception ignored) {}
    }
}
