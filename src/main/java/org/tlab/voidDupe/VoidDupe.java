package org.tlab.voidDupe;

import org.bukkit.plugin.java.JavaPlugin;

public final class VoidDupe extends JavaPlugin {
    private static VoidDupe instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getCommand("dupe").setExecutor(new DupeCommand());
        getServer().getPluginManager().registerEvents(new DupeItemListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static VoidDupe getInstance() {
        return instance;
    }
}
