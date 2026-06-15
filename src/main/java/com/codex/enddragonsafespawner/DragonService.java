package com.codex.enddragonsafespawner;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.EulerAngle;

import io.papermc.paper.event.block.DragonEggFormEvent;

final class DragonService implements Listener {
    private static final long DEFAULT_KILL_TIME_MILLIS = 15L * 60_000L;
    private static final long MIN_KILL_TIME_MILLIS = 10_000L;
    private static final long EXPIRE_WHEN_DISPLAY_WOULD_BE_ONE_SECOND_MILLIS = 1_999L;
    private static final long TIME_EXPIRED_DEATH_GUARD_MILLIS = 60_000L;

    private final EndDragonSafeSpawnerPlugin plugin;
    private final NamespacedKey dragonMarkerKey;
    private final NamespacedKey dragonExpiresAtKey;
    private final NamespacedKey championNpcKey;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, DamageEntry>> damageByDragon = new ConcurrentHashMap<>();
    private final Map<UUID, Long> timeExpiredDeathDragonIds = new ConcurrentHashMap<>();
    private final Map<UUID, Long> timeExpiredDeathWorldIds = new ConcurrentHashMap<>();

    DragonService(EndDragonSafeSpawnerPlugin plugin, NamespacedKey dragonMarkerKey) {
        this.plugin = plugin;
        this.dragonMarkerKey = dragonMarkerKey;
        this.dragonExpiresAtKey = new NamespacedKey(plugin, "expires_at");
        this.championNpcKey = new NamespacedKey(plugin, "champion_npc");
    }

    SpawnOutcome spawnConfiguredDragon(boolean force) {
        Location location = getConfiguredLocation();
        if (location == null) {
            return SpawnOutcome.failure("Mundo configurado nao existe ou nao e um mundo do End.");
        }

        World world = location.getWorld();
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return SpawnOutcome.failure("O mundo configurado precisa ser do tipo THE_END.");
        }

        if (!force && plugin.getConfig().getBoolean("skip-if-dragon-alive", true) && hasLivingDragon(world)) {
            return SpawnOutcome.skipped("Ja existe um Ender Dragon vivo em " + world.getName() + ".");
        }

        location.getChunk().load(true);
        long now = System.currentTimeMillis();
        long expiresAt = now + getKillTimeMillis();
        EnderDragon dragon = world.spawn(location, EnderDragon.class, spawned -> {
            spawned.getPersistentDataContainer().set(this.dragonMarkerKey, PersistentDataType.BYTE, (byte) 1);
            spawned.getPersistentDataContainer().set(this.dragonExpiresAtKey, PersistentDataType.LONG, expiresAt);
            spawned.setPersistent(true);
            spawned.setRemoveWhenFarAway(false);
            spawned.setPhase(EnderDragon.Phase.CIRCLING);
            updateDragonName(spawned, expiresAt, now);
        });

        if (plugin.getConfig().getBoolean("announce-spawn", true)) {
            Bukkit.broadcastMessage(formatSpawnMessage(location));
        }

        return SpawnOutcome.spawned("Dragao spawnado em " + formatLocation(location) + ".", dragon);
    }

    Location getConfiguredLocation() {
        String worldName = plugin.getConfig().getString("world", "world_the_end");
        World world = Bukkit.getWorld(Objects.requireNonNullElse(worldName, "world_the_end"));
        if (world == null || world.getEnvironment() != World.Environment.THE_END) {
            return null;
        }

        double x = plugin.getConfig().getDouble("spawn.x", 0.0);
        double y = plugin.getConfig().getDouble("spawn.y", 80.0);
        double z = plugin.getConfig().getDouble("spawn.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("spawn.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("spawn.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    boolean isValidEndWorld(World world) {
        return world != null && world.getEnvironment() == World.Environment.THE_END;
    }

    boolean hasLivingDragon(World world) {
        return !getLivingDragons(world).isEmpty();
    }

    int countLivingDragons(World world) {
        return getLivingDragons(world).size();
    }

    int countPluginDragons(World world) {
        return (int) getLivingDragons(world).stream()
                .filter(this::isPluginDragon)
                .count();
    }

    int removeDragons(World world, boolean includeVanillaDragons) {
        List<EnderDragon> dragons = getLivingDragons(world).stream()
                .filter(dragon -> includeVanillaDragons || isPluginDragon(dragon))
                .toList();

        dragons.forEach(dragon -> {
            removeBossBar(dragon.getUniqueId());
            clearCombatData(dragon.getUniqueId());
            dragon.remove();
        });
        return dragons.size();
    }

    void tickPluginDragons() {
        long now = System.currentTimeMillis();
        cleanupTimeExpiredDeathGuards(now);
        Set<UUID> activeDragonIds = ConcurrentHashMap.newKeySet();
        for (World world : Bukkit.getWorlds()) {
            for (EnderDragon dragon : getLivingDragons(world)) {
                if (!isPluginDragon(dragon)) {
                    continue;
                }

                activeDragonIds.add(dragon.getUniqueId());
                if (isTimeExpiredDeath(dragon.getUniqueId(), now)) {
                    continue;
                }

                long expiresAt = getOrCreateExpiresAt(dragon, now);
                long remainingMillis = expiresAt - now;
                if (remainingMillis <= EXPIRE_WHEN_DISPLAY_WOULD_BE_ONE_SECOND_MILLIS) {
                    expireDragon(dragon);
                    continue;
                }

                updateDragonName(dragon, expiresAt, now);
                updateBossBar(dragon, expiresAt, now);
            }
        }
        cleanupMissingBossBars(activeDragonIds);
    }

    void removeAllBossBars() {
        this.bossBars.values().forEach(BossBar::removeAll);
        this.bossBars.clear();
    }

    void setBossBarEnabled(boolean enabled) {
        plugin.getConfig().set("bossbar.enabled", enabled);
        plugin.saveConfig();
        if (!enabled) {
            removeAllBossBars();
        }
    }

    boolean isBossBarEnabled() {
        return plugin.getConfig().getBoolean("bossbar.enabled", true);
    }

    boolean isBlockProtectionEnabled() {
        if (plugin.getConfig().contains("block-protection.enabled")) {
            return plugin.getConfig().getBoolean("block-protection.enabled", true);
        }
        return plugin.getConfig().getBoolean("protect-end-towers", true);
    }

    boolean hasTopOneReward() {
        return plugin.getConfig().getBoolean("rewards.top1.enabled", false)
                && plugin.getConfig().getItemStack("rewards.top1.item") != null;
    }

    void saveTopOneReward(ItemStack itemStack) {
        ItemStack reward = itemStack.clone();
        plugin.getConfig().set("rewards.top1.enabled", true);
        plugin.getConfig().set("rewards.top1.item", reward);
        plugin.saveConfig();
    }

    void clearTopOneReward() {
        plugin.getConfig().set("rewards.top1.enabled", false);
        plugin.getConfig().set("rewards.top1.item", null);
        plugin.saveConfig();
    }

    String describeTopOneReward() {
        ItemStack reward = plugin.getConfig().getItemStack("rewards.top1.item");
        if (!plugin.getConfig().getBoolean("rewards.top1.enabled", false) || reward == null) {
            return "desativada";
        }
        return describeItem(reward);
    }

    boolean isChampionNpcEnabled() {
        return plugin.getConfig().getBoolean("champion-npc.enabled", false);
    }

    Location saveChampionNpcLocation(Location location, Player previewPlayer) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }

        Location centeredLocation = centerChampionNpcLocation(location);
        plugin.getConfig().set("champion-npc.enabled", true);
        plugin.getConfig().set("champion-npc.world", world.getName());
        plugin.getConfig().set("champion-npc.x", round(centeredLocation.getX()));
        plugin.getConfig().set("champion-npc.y", round(centeredLocation.getY()));
        plugin.getConfig().set("champion-npc.z", round(centeredLocation.getZ()));
        plugin.getConfig().set("champion-npc.yaw", round(centeredLocation.getYaw()));
        plugin.getConfig().set("champion-npc.pitch", round(centeredLocation.getPitch()));

        ChampionPlayer champion = readStoredChampion();
        if (champion == null) {
            champion = new ChampionPlayer(previewPlayer.getUniqueId(), previewPlayer.getName());
        }
        saveLastChampion(champion);
        plugin.saveConfig();
        spawnChampionNpc(champion);
        return centeredLocation;
    }

    void setChampionNpcEnabled(boolean enabled) {
        plugin.getConfig().set("champion-npc.enabled", enabled);
        plugin.saveConfig();
        if (enabled) {
            refreshChampionNpcFromConfig();
        } else {
            removeChampionNpcs();
        }
    }

    void refreshChampionNpcFromConfig() {
        if (!isChampionNpcEnabled()) {
            removeChampionNpcs();
            return;
        }

        ChampionPlayer champion = readStoredChampion();
        if (champion != null) {
            spawnChampionNpc(champion);
        } else {
            removeChampionNpcs();
        }
    }

    int removeChampionNpcDisplays() {
        return removeChampionNpcs();
    }

    String describeChampionNpc() {
        if (!isChampionNpcEnabled()) {
            return "desativado";
        }

        Location location = readChampionNpcLocation();
        ChampionPlayer champion = readStoredChampion();
        String player = champion == null ? "nenhum jogador salvo" : champion.playerName();
        return (location == null ? "local invalido" : formatLocation(location)) + " | jogador: " + player;
    }

    void setKillTimeMillis(long millis) {
        double minutes = Math.max(MIN_KILL_TIME_MILLIS, millis) / 60_000.0;
        plugin.getConfig().set("dragon.kill-time-minutes", round(minutes));
        plugin.saveConfig();
    }

    long getKillTimeMillis() {
        double minutes = plugin.getConfig().getDouble("dragon.kill-time-minutes", 15.0);
        long millis = (long) (minutes * 60_000.0);
        return Math.max(MIN_KILL_TIME_MILLIS, millis <= 0L ? DEFAULT_KILL_TIME_MILLIS : millis);
    }

    String formatKillTime() {
        return formatDuration(getKillTimeMillis());
    }

    void saveSpawn(Location location) {
        World world = location.getWorld();
        if (world != null) {
            plugin.getConfig().set("world", world.getName());
        }
        plugin.getConfig().set("spawn.x", round(location.getX()));
        plugin.getConfig().set("spawn.y", round(location.getY()));
        plugin.getConfig().set("spawn.z", round(location.getZ()));
        plugin.getConfig().set("spawn.yaw", round(location.getYaw()));
        plugin.getConfig().set("spawn.pitch", round(location.getPitch()));
        plugin.saveConfig();
    }

    String prefix() {
        return color(plugin.getConfig().getString("messages.prefix", "&5[DragaoEnd]&r "));
    }

    String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    String formatLocation(Location location) {
        World world = location.getWorld();
        return String.format(
                Locale.US,
                "%s (%.2f, %.2f, %.2f)",
                world == null ? "mundo-desconhecido" : world.getName(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    private void expireDragon(EnderDragon dragon) {
        UUID dragonId = dragon.getUniqueId();
        if (isTimeExpiredDeath(dragonId, System.currentTimeMillis())) {
            return;
        }

        markTimeExpiredDeath(dragon);
        removeBossBar(dragonId);
        clearCombatData(dragonId);
        dragon.setInvulnerable(false);
        dragon.setGlowing(false);
        dragon.setCustomNameVisible(false);

        if (dragon.getHealth() > 0.0) {
            dragon.setHealth(0.0);
        } else {
            dragon.remove();
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity entity = Bukkit.getEntity(dragonId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }, 20L * 15L);
    }

    private void markTimeExpiredDeath(EnderDragon dragon) {
        long guardUntil = System.currentTimeMillis() + TIME_EXPIRED_DEATH_GUARD_MILLIS;
        this.timeExpiredDeathDragonIds.put(dragon.getUniqueId(), guardUntil);
        this.timeExpiredDeathWorldIds.put(dragon.getWorld().getUID(), guardUntil);
    }

    private boolean isTimeExpiredDeath(UUID dragonId, long now) {
        Long guardUntil = this.timeExpiredDeathDragonIds.get(dragonId);
        return guardUntil != null && guardUntil >= now;
    }

    private boolean isRecentTimeExpiredWorld(World world) {
        if (world == null) {
            return false;
        }
        Long guardUntil = this.timeExpiredDeathWorldIds.get(world.getUID());
        return guardUntil != null && guardUntil >= System.currentTimeMillis();
    }

    private void cleanupTimeExpiredDeathGuards(long now) {
        this.timeExpiredDeathDragonIds.entrySet().removeIf(entry -> entry.getValue() < now);
        this.timeExpiredDeathWorldIds.entrySet().removeIf(entry -> entry.getValue() < now);
    }

    private long getOrCreateExpiresAt(EnderDragon dragon, long now) {
        Long expiresAt = dragon.getPersistentDataContainer().get(this.dragonExpiresAtKey, PersistentDataType.LONG);
        if (expiresAt == null || expiresAt <= 0L) {
            expiresAt = now + getKillTimeMillis();
            dragon.getPersistentDataContainer().set(this.dragonExpiresAtKey, PersistentDataType.LONG, expiresAt);
        }
        return expiresAt;
    }

    private void updateDragonName(EnderDragon dragon, long expiresAt, long now) {
        if (!plugin.getConfig().getBoolean("dragon.countdown-name", true)) {
            return;
        }

        String template = plugin.getConfig().getString("dragon.custom-name", "&5Dragao do End &7- &e{time}");
        if (template == null || template.isBlank()) {
            template = "&5Dragao do End &7- &e{time}";
        }
        if (!template.contains("{time}")) {
            template = template + " &7- &e{time}";
        }

        dragon.setCustomName(color(template.replace("{time}", formatDuration(Math.max(0L, expiresAt - now)))));
        dragon.setCustomNameVisible(plugin.getConfig().getBoolean("dragon.custom-name-visible", true));
    }

    private void updateBossBar(EnderDragon dragon, long expiresAt, long now) {
        if (!isBossBarEnabled()) {
            removeBossBar(dragon.getUniqueId());
            return;
        }

        long remaining = Math.max(0L, expiresAt - now);
        double maxHealth = Math.max(1.0, dragon.getMaxHealth());
        double progress = Math.max(0.0, Math.min(1.0, dragon.getHealth() / maxHealth));

        BossBar bossBar = this.bossBars.computeIfAbsent(
                dragon.getUniqueId(),
                ignored -> Bukkit.createBossBar(
                        formatBossBarTitle(remaining),
                        readBossBarColor(),
                        readBossBarStyle()
                )
        );
        bossBar.setTitle(formatBossBarTitle(remaining));
        bossBar.setColor(readBossBarColor());
        bossBar.setStyle(readBossBarStyle());
        bossBar.setProgress(progress);
        bossBar.setVisible(true);
        syncBossBarPlayers(bossBar, dragon.getWorld());
    }

    private void syncBossBarPlayers(BossBar bossBar, World dragonWorld) {
        bossBar.removeAll();
        boolean showToAll = plugin.getConfig().getBoolean("bossbar.show-to-all-online", false);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (showToAll || player.getWorld().equals(dragonWorld)) {
                bossBar.addPlayer(player);
            }
        }
    }

    private String formatBossBarTitle(long remainingMillis) {
        String template = plugin.getConfig().getString(
                "bossbar.title",
                "&5Dragao do End &7- &e{time} &7para matar"
        );
        if (template == null || template.isBlank()) {
            template = "&5Dragao do End &7- &e{time} &7para matar";
        }
        return color(template.replace("{time}", formatDuration(remainingMillis)));
    }

    private BarColor readBossBarColor() {
        String raw = plugin.getConfig().getString("bossbar.color", "PURPLE");
        try {
            return BarColor.valueOf(Objects.requireNonNullElse(raw, "PURPLE").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarColor.PURPLE;
        }
    }

    private BarStyle readBossBarStyle() {
        String raw = plugin.getConfig().getString("bossbar.style", "SOLID");
        try {
            return BarStyle.valueOf(Objects.requireNonNullElse(raw, "SOLID").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BarStyle.SOLID;
        }
    }

    private void cleanupMissingBossBars(Set<UUID> activeDragonIds) {
        Set<UUID> staleIds = this.bossBars.keySet().stream()
                .filter(id -> !activeDragonIds.contains(id))
                .collect(Collectors.toSet());
        staleIds.forEach(this::removeBossBar);
    }

    private void removeBossBar(UUID dragonId) {
        BossBar bossBar = this.bossBars.remove(dragonId);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    private List<EnderDragon> getLivingDragons(World world) {
        return world.getEntitiesByClass(EnderDragon.class).stream()
                .filter(dragon -> !dragon.isDead())
                .toList();
    }

    private boolean isPluginDragon(EnderDragon dragon) {
        return dragon.getPersistentDataContainer().has(this.dragonMarkerKey, PersistentDataType.BYTE);
    }

    private boolean shouldProtect(Entity entity) {
        if (!isBlockProtectionEnabled()) {
            return false;
        }
        if (entity == null || entity.getType() != EntityType.ENDER_DRAGON) {
            return false;
        }
        if (!protectOnlyPluginDragons()) {
            return true;
        }
        return entity.getPersistentDataContainer().has(this.dragonMarkerKey, PersistentDataType.BYTE);
    }

    private boolean protectOnlyPluginDragons() {
        if (plugin.getConfig().contains("block-protection.only-plugin-dragons")) {
            return plugin.getConfig().getBoolean("block-protection.only-plugin-dragons", true);
        }
        return plugin.getConfig().getBoolean("protect-only-plugin-dragons", true);
    }

    private EnderDragon resolveDragon(Entity entity) {
        if (entity instanceof EnderDragon dragon) {
            return dragon;
        }
        if (entity instanceof ComplexEntityPart part && part.getParent() instanceof EnderDragon dragon) {
            return dragon;
        }
        return null;
    }

    private Player resolveDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private void recordDamage(EnderDragon dragon, Player player, double damage) {
        if (damage <= 0.0) {
            return;
        }

        Map<UUID, DamageEntry> damageByPlayer = this.damageByDragon.computeIfAbsent(
                dragon.getUniqueId(),
                ignored -> new ConcurrentHashMap<>()
        );
        damageByPlayer.compute(player.getUniqueId(), (ignored, entry) -> {
            if (entry == null) {
                return new DamageEntry(player.getUniqueId(), player.getName(), damage);
            }
            entry.addDamage(damage);
            entry.setPlayerName(player.getName());
            return entry;
        });
    }

    private void announceDefeat(EnderDragon dragon) {
        if (!plugin.getConfig().getBoolean("messages.defeat.enabled", true)) {
            return;
        }

        List<DamageEntry> topDamage = getTopDamage(dragon.getUniqueId());
        String killer = getKillerName(dragon, topDamage);
        for (String line : formatDefeatLines(killer, topDamage)) {
            Bukkit.broadcastMessage(line);
        }
    }

    private void giveTopOneReward(List<DamageEntry> topDamage) {
        if (topDamage.isEmpty() || !hasTopOneReward()) {
            return;
        }

        ItemStack reward = plugin.getConfig().getItemStack("rewards.top1.item");
        if (reward == null) {
            return;
        }

        DamageEntry winnerEntry = topDamage.get(0);
        Player winner = Bukkit.getPlayer(winnerEntry.playerId());
        if (winner == null || !winner.isOnline()) {
            return;
        }

        ItemStack rewardToGive = reward.clone();
        Map<Integer, ItemStack> leftovers = winner.getInventory().addItem(rewardToGive);
        leftovers.values().forEach(leftover -> winner.getWorld().dropItemNaturally(winner.getLocation(), leftover));

        String message = plugin.getConfig().getString(
                "rewards.top1.message",
                "&d{player} &7recebeu a recompensa de top 1: &f{item}&7 x{amount}."
        );
        if (message != null && !message.isBlank()) {
            Bukkit.broadcastMessage(prefix() + color(message)
                    .replace("{player}", winner.getName())
                    .replace("{item}", getItemName(reward))
                    .replace("{amount}", Integer.toString(reward.getAmount())));
        }
    }

    private List<DamageEntry> getTopDamage(UUID dragonId) {
        Map<UUID, DamageEntry> damageByPlayer = this.damageByDragon.getOrDefault(dragonId, Map.of());
        return damageByPlayer.values().stream()
                .sorted(Comparator.comparingDouble(DamageEntry::damage).reversed())
                .limit(3)
                .toList();
    }

    private String getKillerName(EnderDragon dragon, List<DamageEntry> topDamage) {
        Player killer = dragon.getKiller();
        if (killer != null) {
            return killer.getName();
        }
        if (!topDamage.isEmpty()) {
            return topDamage.get(0).playerName();
        }
        return "Um jogador";
    }

    private List<String> formatDefeatLines(String killer, List<DamageEntry> topDamage) {
        List<String> lines = new java.util.ArrayList<>();
        String separator = plugin.getConfig().getString("messages.defeat.separator", "&8&m--------------------------");
        String title = plugin.getConfig().getString("messages.defeat.title", "&5&lO DRAGAO ANCESTRAL CAIU");
        String killerLine = plugin.getConfig().getString(
                "messages.defeat.killer-line",
                "&d{killer} &7matou o Dragao do End."
        );
        String topHeader = plugin.getConfig().getString("messages.defeat.top-header", "&7Top dano causado:");
        String topLine = plugin.getConfig().getString(
                "messages.defeat.top-line",
                "&5#{position} &f{player} &7- &c{damage} dano"
        );
        String noDamageLine = plugin.getConfig().getString(
                "messages.defeat.no-damage-line",
                "&7Nenhum dano de player foi registrado."
        );

        if (separator != null && !separator.isBlank()) {
            lines.add(color(separator));
        }
        lines.add(color(Objects.requireNonNullElse(title, "&5&lO DRAGAO ANCESTRAL CAIU")));
        lines.add(color(Objects.requireNonNullElse(killerLine, "&d{killer} &7matou o Dragao do End.")
                .replace("{killer}", killer)));
        lines.add(color(Objects.requireNonNullElse(topHeader, "&7Top dano causado:")));

        if (topDamage.isEmpty()) {
            lines.add(color(Objects.requireNonNullElse(noDamageLine, "&7Nenhum dano de player foi registrado.")));
        } else {
            for (int i = 0; i < topDamage.size(); i++) {
                DamageEntry entry = topDamage.get(i);
                lines.add(color(Objects.requireNonNullElse(topLine, "&5#{position} &f{player} &7- &c{damage} dano")
                        .replace("{position}", Integer.toString(i + 1))
                        .replace("{player}", entry.playerName())
                        .replace("{damage}", formatDamage(entry.damage()))));
            }
        }
        if (separator != null && !separator.isBlank()) {
            lines.add(color(separator));
        }
        return lines;
    }

    private String formatDamage(double damage) {
        return String.format(Locale.US, "%.1f", damage);
    }

    private String describeItem(ItemStack itemStack) {
        return getItemName(itemStack) + " x" + itemStack.getAmount();
    }

    private String getItemName(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(color(meta.getDisplayName()));
        }
        return itemStack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void updateChampionNpcOnDefeat(EnderDragon dragon, List<DamageEntry> topDamage) {
        if (!isChampionNpcEnabled() || !plugin.getConfig().getBoolean("champion-npc.update-on-defeat", true)) {
            return;
        }

        ChampionPlayer champion = resolveChampionPlayer(dragon, topDamage);
        if (champion == null) {
            return;
        }

        saveLastChampion(champion);
        plugin.saveConfig();
        spawnChampionNpc(champion);
    }

    private ChampionPlayer resolveChampionPlayer(EnderDragon dragon, List<DamageEntry> topDamage) {
        Player killer = dragon.getKiller();
        if (killer != null) {
            return new ChampionPlayer(killer.getUniqueId(), killer.getName());
        }
        if (!topDamage.isEmpty()) {
            DamageEntry topOne = topDamage.get(0);
            return new ChampionPlayer(topOne.playerId(), topOne.playerName());
        }
        return null;
    }

    private void saveLastChampion(ChampionPlayer champion) {
        plugin.getConfig().set("champion-npc.last-player.uuid",
                champion.playerId() == null ? "" : champion.playerId().toString());
        plugin.getConfig().set("champion-npc.last-player.name", champion.playerName());
    }

    private ChampionPlayer readStoredChampion() {
        String playerName = plugin.getConfig().getString("champion-npc.last-player.name", "");
        String rawUuid = plugin.getConfig().getString("champion-npc.last-player.uuid", "");
        if ((playerName == null || playerName.isBlank()) && (rawUuid == null || rawUuid.isBlank())) {
            return null;
        }

        UUID playerId = null;
        if (rawUuid != null && !rawUuid.isBlank()) {
            try {
                playerId = UUID.fromString(rawUuid);
            } catch (IllegalArgumentException ignored) {
                playerId = null;
            }
        }

        String fallbackName = playerId == null ? "Jogador" : Bukkit.getOfflinePlayer(playerId).getName();
        return new ChampionPlayer(playerId, playerName == null || playerName.isBlank()
                ? Objects.requireNonNullElse(fallbackName, "Jogador")
                : playerName);
    }

    private Location readChampionNpcLocation() {
        String worldName = plugin.getConfig().getString("champion-npc.world", "");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }

        double x = plugin.getConfig().getDouble("champion-npc.x", 0.0);
        double y = plugin.getConfig().getDouble("champion-npc.y", 80.0);
        double z = plugin.getConfig().getDouble("champion-npc.z", 0.0);
        float yaw = (float) plugin.getConfig().getDouble("champion-npc.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("champion-npc.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private boolean spawnChampionNpc(ChampionPlayer champion) {
        Location location = readChampionNpcLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }

        location.getChunk().load(true);
        removeChampionNpcs();
        location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
            armorStand.getPersistentDataContainer().set(this.championNpcKey, PersistentDataType.BYTE, (byte) 1);
            armorStand.setPersistent(true);
            armorStand.setRemoveWhenFarAway(false);
            armorStand.setGravity(plugin.getConfig().getBoolean("champion-npc.gravity", false));
            armorStand.setInvulnerable(plugin.getConfig().getBoolean("champion-npc.invulnerable", true));
            armorStand.setCollidable(false);
            armorStand.setCanPickupItems(false);
            armorStand.setArms(true);
            armorStand.setBasePlate(false);
            armorStand.setSmall(false);
            armorStand.setMarker(false);
            armorStand.setCustomName(formatChampionNpcName(champion));
            armorStand.setCustomNameVisible(true);
            armorStand.setRotation(location.getYaw(), location.getPitch());
            applyChampionNpcPose(armorStand);
            applyChampionNpcEquipment(armorStand, champion);
        });
        return true;
    }

    private String formatChampionNpcName(ChampionPlayer champion) {
        String template = plugin.getConfig().getString("champion-npc.name", "{player}");
        if (template == null || template.isBlank() || template.equalsIgnoreCase("&5Campeao do Dragao: &f{player}")) {
            template = "{player}";
        }
        return color(template.replace("{player}", champion.playerName()));
    }

    private Location centerChampionNpcLocation(Location location) {
        return new Location(
                location.getWorld(),
                Math.floor(location.getX()) + 0.5,
                Math.floor(location.getY()),
                Math.floor(location.getZ()) + 0.5,
                location.getYaw(),
                location.getPitch()
        );
    }

    private void applyChampionNpcPose(ArmorStand armorStand) {
        armorStand.setHeadPose(new EulerAngle(Math.toRadians(0.0), Math.toRadians(0.0), Math.toRadians(0.0)));
        armorStand.setBodyPose(new EulerAngle(Math.toRadians(0.0), Math.toRadians(8.0), Math.toRadians(0.0)));
        armorStand.setRightArmPose(new EulerAngle(Math.toRadians(-72.0), Math.toRadians(18.0), Math.toRadians(8.0)));
        armorStand.setLeftArmPose(new EulerAngle(Math.toRadians(12.0), Math.toRadians(0.0), Math.toRadians(-10.0)));
        armorStand.setRightLegPose(new EulerAngle(Math.toRadians(4.0), Math.toRadians(0.0), Math.toRadians(3.0)));
        armorStand.setLeftLegPose(new EulerAngle(Math.toRadians(-4.0), Math.toRadians(0.0), Math.toRadians(-3.0)));
    }

    private void applyChampionNpcEquipment(ArmorStand armorStand, ChampionPlayer champion) {
        EntityEquipment equipment = armorStand.getEquipment();
        if (equipment == null) {
            return;
        }

        equipment.setHelmet(createChampionHead(champion));
        equipment.setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        equipment.setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        equipment.setBoots(new ItemStack(Material.NETHERITE_BOOTS));
        equipment.setItemInMainHand(new ItemStack(Material.NETHERITE_SWORD));
    }

    @SuppressWarnings("deprecation")
    private ItemStack createChampionHead(ChampionPlayer champion) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = head.getItemMeta();
        if (rawMeta instanceof SkullMeta skullMeta) {
            OfflinePlayer owner = champion.playerId() == null
                    ? Bukkit.getOfflinePlayer(champion.playerName())
                    : Bukkit.getOfflinePlayer(champion.playerId());
            skullMeta.setOwningPlayer(owner);
            skullMeta.setDisplayName(color("&5Cabeca de &f" + champion.playerName()));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private int removeChampionNpcs() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (ArmorStand armorStand : world.getEntitiesByClass(ArmorStand.class)) {
                if (isChampionNpc(armorStand)) {
                    armorStand.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private boolean isChampionNpc(ArmorStand armorStand) {
        return armorStand.getPersistentDataContainer().has(this.championNpcKey, PersistentDataType.BYTE);
    }

    private void clearCombatData(UUID dragonId) {
        this.damageByDragon.remove(dragonId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityExplode(EntityExplodeEvent event) {
        if (!shouldProtect(event.getEntity())) {
            return;
        }
        event.blockList().clear();
        event.setYield(0.0F);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (shouldProtect(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityCreatePortal(EntityCreatePortalEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon
                && isTimeExpiredDeath(dragon.getUniqueId(), System.currentTimeMillis())) {
            event.getBlocks().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPortalCreate(PortalCreateEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon
                && isTimeExpiredDeath(dragon.getUniqueId(), System.currentTimeMillis())) {
            event.getBlocks().clear();
            event.setCancelled(true);
            return;
        }

        if (isRecentTimeExpiredWorld(event.getWorld()) && event.getReason() != PortalCreateEvent.CreateReason.FIRE) {
            event.getBlocks().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        EnderDragon dragon = resolveDragon(event.getEntity());
        Player player = resolveDamagingPlayer(event.getDamager());
        if (dragon == null || player == null || !isPluginDragon(dragon)) {
            return;
        }

        double damage = Math.min(event.getFinalDamage(), Math.max(0.0, dragon.getHealth()));
        recordDamage(dragon, player, damage);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof EnderDragon dragon && isPluginDragon(dragon)) {
            removeBossBar(dragon.getUniqueId());
            if (isTimeExpiredDeath(dragon.getUniqueId(), System.currentTimeMillis())) {
                event.getDrops().clear();
                event.setDroppedExp(0);
                clearCombatData(dragon.getUniqueId());
                if (plugin.getConfig().getBoolean("announce-expire", true)) {
                    Bukkit.broadcastMessage(formatExpireMessage(dragon.getLocation()));
                }
                return;
            }

            List<DamageEntry> topDamage = getTopDamage(dragon.getUniqueId());
            updateChampionNpcOnDefeat(dragon, topDamage);
            giveTopOneReward(topDamage);
            announceDefeat(dragon);
            clearCombatData(dragon.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onDragonEggForm(DragonEggFormEvent event) {
        EnderDragon dragon = event.getDragonBattle() == null ? null : event.getDragonBattle().getEnderDragon();
        if ((dragon != null && isTimeExpiredDeath(dragon.getUniqueId(), System.currentTimeMillis()))
                || isRecentTimeExpiredWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    private String formatSpawnMessage(Location location) {
        String raw = plugin.getConfig().getString("messages.spawn", "&dUm Dragao do End nasceu.");
        return prefix() + color(raw)
                .replace("{world}", location.getWorld() == null ? "mundo-desconhecido" : location.getWorld().getName())
                .replace("{x}", String.format(Locale.US, "%.1f", location.getX()))
                .replace("{y}", String.format(Locale.US, "%.1f", location.getY()))
                .replace("{z}", String.format(Locale.US, "%.1f", location.getZ()));
    }

    private String formatExpireMessage(Location location) {
        String raw = plugin.getConfig().getString(
                "messages.expire",
                "&cO Dragao do End desapareceu porque o tempo de &f{time}&c acabou."
        );
        return prefix() + color(raw)
                .replace("{time}", formatKillTime())
                .replace("{world}", location.getWorld() == null ? "mundo-desconhecido" : location.getWorld().getName())
                .replace("{x}", String.format(Locale.US, "%.1f", location.getX()))
                .replace("{y}", String.format(Locale.US, "%.1f", location.getY()))
                .replace("{z}", String.format(Locale.US, "%.1f", location.getZ()));
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3_600L;
        long minutes = (totalSeconds % 3_600L) / 60L;
        long seconds = totalSeconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.US, "%dh %02dm %02ds", hours, minutes, seconds);
        }
        if (minutes > 0L) {
            return String.format(Locale.US, "%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    record SpawnOutcome(boolean spawned, boolean skipped, String message, EnderDragon dragon) {
        static SpawnOutcome spawned(String message, EnderDragon dragon) {
            return new SpawnOutcome(true, false, message, dragon);
        }

        static SpawnOutcome skipped(String message) {
            return new SpawnOutcome(false, true, message, null);
        }

        static SpawnOutcome failure(String message) {
            return new SpawnOutcome(false, false, message, null);
        }
    }

    private record ChampionPlayer(UUID playerId, String playerName) {
    }

    private static final class DamageEntry {
        private final UUID playerId;
        private String playerName;
        private double damage;

        private DamageEntry(UUID playerId, String playerName, double damage) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.damage = damage;
        }

        private UUID playerId() {
            return this.playerId;
        }

        private String playerName() {
            return this.playerName;
        }

        private void setPlayerName(String playerName) {
            this.playerName = playerName;
        }

        private double damage() {
            return this.damage;
        }

        private void addDamage(double damage) {
            this.damage += damage;
        }
    }
}
