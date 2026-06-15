package com.codex.enddragonsafespawner;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class EndDragonSafeSpawnerPlugin extends JavaPlugin {
    static final String ADMIN_PERMISSION = "enddragonsafe.admin";

    private NamespacedKey dragonMarkerKey;
    private DragonService dragonService;
    private DragonScheduler dragonScheduler;

    @Override
    public void onEnable() {
        migrateLegacyConfig();
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

        this.dragonService.refreshChampionNpcFromConfig();
        this.dragonScheduler.start();
        getLogger().info("EnderDragon Spawner Plugin ativado.");
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

    private void migrateLegacyConfig() {
        File dataFolder = getDataFolder();
        File pluginsFolder = dataFolder.getParentFile();
        if (pluginsFolder == null || dataFolder.exists()) {
            return;
        }

        File legacyConfig = new File(new File(pluginsFolder, "EndDragonSafeSpawner"), "config.yml");
        File newConfig = new File(dataFolder, "config.yml");
        if (!legacyConfig.isFile() || newConfig.exists()) {
            return;
        }

        try {
            Files.createDirectories(dataFolder.toPath());
            Files.copy(legacyConfig.toPath(), newConfig.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            getLogger().info("Config antiga de EndDragonSafeSpawner migrada para EnderDragonSpawnerPlugin.");
        } catch (IOException exception) {
            getLogger().warning("Nao foi possivel migrar a config antiga: " + exception.getMessage());
        }
    }
}
