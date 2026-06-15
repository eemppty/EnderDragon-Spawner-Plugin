package com.codex.enddragonsafespawner;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

final class DragonScheduler {
    private static final long MIN_INTERVAL_MILLIS = 60_000L;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final EndDragonSafeSpawnerPlugin plugin;
    private final DragonService dragonService;
    private BukkitTask task;

    DragonScheduler(EndDragonSafeSpawnerPlugin plugin, DragonService dragonService) {
        this.plugin = plugin;
        this.dragonService = dragonService;
    }

    void start() {
        stop();
        ensureNextSpawn();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    void stop() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    void reload() {
        plugin.reloadConfig();
        start();
    }

    void setEnabled(boolean enabled) {
        plugin.getConfig().set("enabled", enabled);
        if (enabled && getNextSpawnAt() <= 0L) {
            setNextSpawnAt(System.currentTimeMillis() + getIntervalMillis());
        }
        plugin.saveConfig();
    }

    void setIntervalMillis(long intervalMillis) {
        double hours = intervalMillis / 3_600_000.0;
        plugin.getConfig().set("interval-hours", round(hours));
        resetTimer();
    }

    void resetTimer() {
        setNextSpawnAt(System.currentTimeMillis() + getIntervalMillis());
        plugin.saveConfig();
    }

    long getNextSpawnAt() {
        return plugin.getConfig().getLong("scheduler.next-spawn-at", 0L);
    }

    long getIntervalMillis() {
        double hours = plugin.getConfig().getDouble("interval-hours", 3.0);
        long millis = (long) (hours * 3_600_000.0);
        return Math.max(MIN_INTERVAL_MILLIS, millis);
    }

    String formatNextSpawn() {
        long next = getNextSpawnAt();
        if (next <= 0L) {
            return "não agendado";
        }
        return DATE_FORMAT.format(Instant.ofEpochMilli(next)) + " (" + formatDuration(Math.max(0L, next - System.currentTimeMillis())) + ")";
    }

    String formatInterval() {
        return formatDuration(getIntervalMillis());
    }

    private void tick() {
        dragonService.tickPluginDragons();

        if (!plugin.getConfig().getBoolean("enabled", true)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (getNextSpawnAt() > now) {
            return;
        }

        DragonService.SpawnOutcome outcome = dragonService.spawnConfiguredDragon(false);
        if (outcome.spawned()) {
            plugin.getLogger().info(outcome.message());
        } else if (outcome.skipped()) {
            plugin.getLogger().info(outcome.message());
        } else {
            plugin.getLogger().warning(outcome.message());
        }

        setNextSpawnAt(now + getIntervalMillis());
        plugin.saveConfig();
    }

    private void ensureNextSpawn() {
        if (getNextSpawnAt() <= 0L) {
            setNextSpawnAt(System.currentTimeMillis() + getIntervalMillis());
            plugin.saveConfig();
        }
    }

    private void setNextSpawnAt(long epochMillis) {
        plugin.getConfig().set("scheduler.next-spawn-at", epochMillis);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (days > 0L) {
            return String.format(Locale.US, "%dd %02dh %02dm", days, hours, minutes);
        }
        if (hours > 0L) {
            return String.format(Locale.US, "%dh %02dm", hours, minutes);
        }
        if (minutes > 0L) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
