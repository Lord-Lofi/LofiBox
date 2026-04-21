package dev.lofibox.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Sends mail via EssentialsX using reflection.
 * Avoids a hard dependency on EssX API internals beyond what's available at compile time.
 */
public final class EssMailHook {

    private Object essentials;   // IEssentials instance
    private Method getUser;      // IEssentials.getUser(String)
    private Method addMail;      // User.addMail(String)

    public boolean setup(JavaPlugin plugin) {
        Object ess = Bukkit.getPluginManager().getPlugin("Essentials");
        if (ess == null) return false;
        try {
            getUser = ess.getClass().getMethod("getUser", String.class);
            // Get a dummy user to find addMail method on the User class
            // We look it up by iterating — avoids importing User type
            for (Method m : getUser.getReturnType().getMethods()) {
                if (m.getName().equals("addMail") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    addMail = m;
                    break;
                }
            }
            if (addMail == null) return false;
            essentials = ess;
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("EssentialsMail: hook failed — " + e.getMessage());
            return false;
        }
    }

    public boolean isAvailable() { return essentials != null && getUser != null && addMail != null; }

    public void sendMail(String playerName, String message) {
        if (!isAvailable() || message == null || message.isBlank()) return;
        try {
            Object user = getUser.invoke(essentials, playerName);
            if (user != null) addMail.invoke(user, message);
        } catch (Exception ignored) {}
    }
}
