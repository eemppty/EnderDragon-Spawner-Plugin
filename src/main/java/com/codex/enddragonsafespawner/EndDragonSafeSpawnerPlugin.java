package com.codex.enddragonsafespawner;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class EndDragonSafeSpawnerPlugin extends JavaPlugin {
    static final String ADMIN_PERMISSION = "enddragonsafe.admin";

    private NamespacedKey dragonMarkerKey;
    private DragonService dragonService;
    private DragonScheduler dragonScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        this.dragonMarkerKey = new NamespacedKey(this, "scheduled_dragon");
        this.dragonService = new DragonService(this, this.dragonMarkerKey);
        this.dragonScheduler = new DragonScheduler(this, this.dragonService);

        Bukkit.getPluginManager().registerEvents(this.dragonService, this);

        DragonCommand dragonCommand = new DragonCommand(this, this.dragonService, this.dragonScheduler);
        PluginCommand command = getCommand("dragaoend");
        if (command == null) {
            getLogger().severe("Comando /dragaoend nao foi encontrado no plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(dragonCommand);
        command.setTabCompleter(dragonCommand);

        this.dragonScheduler.start();
        getLogger().info("EndDragonSafeSpawner ativado.");
    }

    @Override
    public void onDisable() {
        if (this.dragonScheduler != null) {
            this.dragonScheduler.stop();
        }
        if (this.dragonService != null) {
            this.dragonService.removeAllBossBars();
        }
    }
}
