package dev.lofibox.util;

import dev.lofibox.LofiBox;
import dev.lofibox.stats.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderHook extends PlaceholderExpansion {

    private final LofiBox plugin;

    public PlaceholderHook(LofiBox plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "lofibox"; }
    @Override public @NotNull String getAuthor()     { return "Lord-Lofi"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        StatsManager stats = plugin.getStatsManager();

        // %lofibox_total_opened%
        if (params.equals("total_opened")) {
            return String.valueOf(stats.getTotalOpened(player.getUniqueId()));
        }

        // %lofibox_opened_<boxId>%
        if (params.startsWith("opened_")) {
            String boxId = params.substring(7);
            return String.valueOf(stats.getOpened(player.getUniqueId(), boxId));
        }

        return null;
    }
}
