package com.codex.enddragonsafespawner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

final class DragonCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "help", "status", "nascer", "matar", "spawn", "kill", "setinterval", "setcoords", "sethere",
            "setkilltime", "tempomatar", "setreward", "clearreward", "recompensa", "bossbar", "reset",
            "npc", "setnpc", "reload", "enable", "disable"
    );

    private final EndDragonSafeSpawnerPlugin plugin;
    private final DragonService dragonService;
    private final DragonScheduler dragonScheduler;

    DragonCommand(
            EndDragonSafeSpawnerPlugin plugin,
            DragonService dragonService,
            DragonScheduler dragonScheduler
    ) {
        this.plugin = plugin;
        this.dragonService = dragonService;
        this.dragonScheduler = dragonScheduler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(EndDragonSafeSpawnerPlugin.ADMIN_PERMISSION)) {
            sender.sendMessage(dragonService.prefix() + "Voce nao tem permissao.");
            return true;
        }

        if (args.length == 0 || matches(args[0], "help", "ajuda")) {
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "status" -> sendStatus(sender);
            case "spawn", "spawnar", "nascer" -> spawn(sender, args);
            case "kill", "matar" -> kill(sender, args);
            case "setinterval", "intervalo" -> setInterval(sender, args);
            case "setkilltime", "tempomatar", "setlifetime" -> setKillTime(sender, args);
            case "setreward", "setrecompensa" -> setTopOneReward(sender, args);
            case "clearreward", "clearrecompensa" -> clearTopOneReward(sender, args);
            case "recompensa", "reward" -> reward(sender, args);
            case "bossbar" -> setBossBar(sender, args);
            case "npc", "campeao" -> championNpc(sender, args);
            case "setnpc", "setcampeao" -> setChampionNpc(sender);
            case "setcoords", "coords" -> setCoords(sender, args);
            case "sethere", "aqui" -> setHere(sender);
            case "reset" -> reset(sender);
            case "reload", "recarregar" -> reload(sender);
            case "enable", "ativar" -> setEnabled(sender, true);
            case "disable", "desativar" -> setEnabled(sender, false);
            default -> {
                sender.sendMessage(dragonService.prefix() + "Subcomando desconhecido. Use /" + label + " help.");
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(EndDragonSafeSpawnerPlugin.ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && matches(subcommand, "spawn", "nascer")) {
            return filter(List.of("force"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "kill", "matar")) {
            return filter(List.of("plugin", "todos", "all"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "setinterval", "intervalo")) {
            return filter(List.of("10m", "30m", "90m", "3h", "5h", "12h"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "setkilltime", "tempomatar", "setlifetime")) {
            return filter(List.of("5m", "10m", "15m", "30m", "1h"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "setreward", "setrecompensa", "clearreward", "clearrecompensa")) {
            return filter(List.of("top1"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "recompensa", "reward")) {
            return filter(List.of("set", "clear", "status"), args[1]);
        }
        if (args.length == 3 && matches(subcommand, "recompensa", "reward") && matches(args[1], "set", "clear")) {
            return filter(List.of("top1"), args[2]);
        }
        if (args.length == 2 && subcommand.equals("bossbar")) {
            return filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && matches(subcommand, "npc", "campeao")) {
            return filter(List.of("set", "status", "remove", "on", "off"), args[1]);
        }
        if (args.length >= 2 && args.length <= 4 && matches(subcommand, "setcoords", "coords")) {
            return filter(List.of("0", "~"), args[args.length - 1]);
        }
        if (args.length == 5 && matches(subcommand, "setcoords", "coords")) {
            List<String> endWorlds = Bukkit.getWorlds().stream()
                    .filter(dragonService::isValidEndWorld)
                    .map(World::getName)
                    .collect(Collectors.toList());
            return filter(endWorlds, args[4]);
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender, String label) {
        String prefix = dragonService.prefix();
        sender.sendMessage(prefix + "Comandos:");
        sender.sendMessage("/" + label + " status");
        sender.sendMessage("/" + label + " setinterval <10m|30m|90m|3h|5h>");
        sender.sendMessage("/" + label + " setkilltime <5m|15m|30m|1h>");
        sender.sendMessage("/" + label + " setcoords <x> <y> <z> [mundo]");
        sender.sendMessage("/" + label + " sethere");
        sender.sendMessage("/" + label + " nascer [force]");
        sender.sendMessage("/" + label + " matar [plugin|todos]");
        sender.sendMessage("/" + label + " bossbar <on|off>");
        sender.sendMessage("/" + label + " npc set | status | remove | on | off");
        sender.sendMessage("/" + label + " setreward top1");
        sender.sendMessage("/" + label + " clearreward top1");
        sender.sendMessage("/" + label + " reset | reload | enable | disable");
    }

    private void sendStatus(CommandSender sender) {
        Location location = dragonService.getConfiguredLocation();
        World world = location == null ? null : location.getWorld();
        int dragons = world == null ? 0 : dragonService.countLivingDragons(world);
        int pluginDragons = world == null ? 0 : dragonService.countPluginDragons(world);

        sender.sendMessage(dragonService.prefix() + "Status:");
        sender.sendMessage("Ativo: " + plugin.getConfig().getBoolean("enabled", true));
        sender.sendMessage("Spawn: " + (location == null ? "invalido" : dragonService.formatLocation(location)));
        sender.sendMessage("Intervalo: " + dragonScheduler.formatInterval());
        sender.sendMessage("Tempo para matar: " + dragonService.formatKillTime());
        sender.sendMessage("Proximo spawn: " + dragonScheduler.formatNextSpawn());
        sender.sendMessage("Boss bar: " + dragonService.isBossBarEnabled());
        sender.sendMessage("Dragoes vivos nesse mundo: " + dragons);
        sender.sendMessage("Dragoes do plugin nesse mundo: " + pluginDragons);
        sender.sendMessage("Protecao de blocos: " + dragonService.isBlockProtectionEnabled());
        sender.sendMessage("Recompensa top 1: " + dragonService.describeTopOneReward());
        sender.sendMessage("NPC campeao: " + dragonService.describeChampionNpc());
    }

    private void spawn(CommandSender sender, String[] args) {
        boolean force = args.length >= 2 && args[1].equalsIgnoreCase("force");
        DragonService.SpawnOutcome outcome = dragonService.spawnConfiguredDragon(force);
        sender.sendMessage(dragonService.prefix() + outcome.message());
    }

    private void kill(CommandSender sender, String[] args) {
        Location location = dragonService.getConfiguredLocation();
        if (location == null || location.getWorld() == null) {
            sender.sendMessage(dragonService.prefix() + "Mundo configurado nao existe ou nao e um mundo do End.");
            return;
        }

        boolean includeVanillaDragons = args.length >= 2 && matches(args[1], "todos", "all", "tudo");
        int removed = dragonService.removeDragons(location.getWorld(), includeVanillaDragons);
        if (includeVanillaDragons) {
            sender.sendMessage(dragonService.prefix() + "Dragoes removidos do mundo configurado: " + removed + ".");
        } else {
            sender.sendMessage(dragonService.prefix() + "Dragoes criados pelo plugin removidos: " + removed
                    + ". Use /dragaoend matar todos para remover qualquer Ender Dragon vivo nesse mundo.");
        }
    }

    private void setInterval(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend setinterval <10m|30m|90m|3h|5h>");
            return;
        }

        Long millis = parseDurationMillis(args[1]);
        if (millis == null || millis < 60_000L) {
            sender.sendMessage(dragonService.prefix() + "Tempo invalido. Use algo como 10m, 30m, 90m, 3h ou 5h.");
            return;
        }

        dragonScheduler.setIntervalMillis(millis);
        sender.sendMessage(dragonService.prefix() + "Intervalo configurado para " + dragonScheduler.formatInterval()
                + ". Proximo spawn: " + dragonScheduler.formatNextSpawn() + ".");
    }

    private void setKillTime(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend setkilltime <5m|15m|30m|1h>");
            return;
        }

        Long millis = parseDurationMillis(args[1]);
        if (millis == null || millis < 10_000L) {
            sender.sendMessage(dragonService.prefix() + "Tempo invalido. Use algo como 5m, 15m, 30m ou 1h.");
            return;
        }

        dragonService.setKillTimeMillis(millis);
        sender.sendMessage(dragonService.prefix() + "Tempo para matar configurado para "
                + dragonService.formatKillTime()
                + ". Dragoes ja vivos continuam com o prazo atual; os proximos usam esse novo tempo.");
    }

    private void reward(CommandSender sender, String[] args) {
        if (args.length < 2 || matches(args[1], "status")) {
            sender.sendMessage(dragonService.prefix() + "Recompensa top 1: " + dragonService.describeTopOneReward() + ".");
            return;
        }

        if (matches(args[1], "set")) {
            setTopOneReward(sender, new String[] {"setreward", args.length >= 3 ? args[2] : "top1"});
            return;
        }

        if (matches(args[1], "clear", "remove")) {
            clearTopOneReward(sender, new String[] {"clearreward", args.length >= 3 ? args[2] : "top1"});
            return;
        }

        sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend recompensa <set|clear|status> top1");
    }

    private void setTopOneReward(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(dragonService.prefix() + "Apenas jogadores podem setar recompensa segurando item.");
            return;
        }
        if (args.length >= 2 && !args[1].equalsIgnoreCase("top1")) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend setreward top1");
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() == Material.AIR || handItem.getAmount() <= 0) {
            sender.sendMessage(dragonService.prefix() + "Segure o item da recompensa na mao principal.");
            return;
        }

        dragonService.saveTopOneReward(handItem);
        sender.sendMessage(dragonService.prefix() + "Recompensa do top 1 configurada para "
                + dragonService.describeTopOneReward() + ".");
    }

    private void clearTopOneReward(CommandSender sender, String[] args) {
        if (args.length >= 2 && !args[1].equalsIgnoreCase("top1")) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend clearreward top1");
            return;
        }

        dragonService.clearTopOneReward();
        sender.sendMessage(dragonService.prefix() + "Recompensa do top 1 removida.");
    }

    private void setBossBar(CommandSender sender, String[] args) {
        if (args.length < 2 || !matches(args[1], "on", "off", "true", "false", "ligar", "desligar")) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend bossbar <on|off>");
            return;
        }

        boolean enabled = matches(args[1], "on", "true", "ligar");
        dragonService.setBossBarEnabled(enabled);
        sender.sendMessage(dragonService.prefix() + "Boss bar " + (enabled ? "ativada" : "desativada") + ".");
    }

    private void championNpc(CommandSender sender, String[] args) {
        if (args.length < 2 || matches(args[1], "status")) {
            sender.sendMessage(dragonService.prefix() + "NPC campeao: " + dragonService.describeChampionNpc() + ".");
            return;
        }

        if (matches(args[1], "set", "setar", "aqui", "here")) {
            setChampionNpc(sender);
            return;
        }

        if (matches(args[1], "remove", "clear", "remover")) {
            int removed = dragonService.removeChampionNpcDisplays();
            sender.sendMessage(dragonService.prefix() + "NPCs campeoes removidos do mundo: " + removed
                    + ". O local continua salvo; no proximo dragao morto ele volta atualizado.");
            return;
        }

        if (matches(args[1], "on", "enable", "ativar", "ligar")) {
            dragonService.setChampionNpcEnabled(true);
            sender.sendMessage(dragonService.prefix() + "NPC campeao ativado. Status: "
                    + dragonService.describeChampionNpc() + ".");
            return;
        }

        if (matches(args[1], "off", "disable", "desativar", "desligar")) {
            dragonService.setChampionNpcEnabled(false);
            sender.sendMessage(dragonService.prefix() + "NPC campeao desativado e removido.");
            return;
        }

        sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend npc <set|status|remove|on|off>");
    }

    private void setChampionNpc(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(dragonService.prefix() + "Apenas jogadores podem usar /dragaoend npc set.");
            return;
        }

        Location location = dragonService.saveChampionNpcLocation(player.getLocation(), player);
        if (location == null) {
            sender.sendMessage(dragonService.prefix() + "Nao foi possivel salvar o local do NPC campeao.");
            return;
        }
        sender.sendMessage(dragonService.prefix() + "NPC campeao configurado em "
                + dragonService.formatLocation(location)
                + " no centro do bloco e olhando para a mesma direcao que voce.");
    }

    private void setCoords(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(dragonService.prefix() + "Uso: /dragaoend setcoords <x> <y> <z> [mundo]");
            return;
        }

        World world = resolveWorld(sender, args.length >= 5 ? args[4] : null);
        if (!dragonService.isValidEndWorld(world)) {
            sender.sendMessage(dragonService.prefix() + "O mundo precisa existir e ser do tipo THE_END.");
            return;
        }

        Double x = parseCoordinate(sender, args[1], 'x');
        Double y = parseCoordinate(sender, args[2], 'y');
        Double z = parseCoordinate(sender, args[3], 'z');
        if (x == null || y == null || z == null) {
            sender.sendMessage(dragonService.prefix() + "Coordenadas invalidas.");
            return;
        }

        Location current = sender instanceof Player player ? player.getLocation() : new Location(world, x, y, z);
        Location location = new Location(world, x, y, z, current.getYaw(), current.getPitch());
        dragonService.saveSpawn(location);
        dragonScheduler.resetTimer();
        sender.sendMessage(dragonService.prefix() + "Spawn configurado para " + dragonService.formatLocation(location) + ".");
    }

    private void setHere(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(dragonService.prefix() + "Apenas jogadores podem usar /dragaoend sethere.");
            return;
        }

        Location location = player.getLocation();
        if (!dragonService.isValidEndWorld(location.getWorld())) {
            sender.sendMessage(dragonService.prefix() + "Voce precisa estar em um mundo do End.");
            return;
        }

        dragonService.saveSpawn(location);
        dragonScheduler.resetTimer();
        sender.sendMessage(dragonService.prefix() + "Spawn configurado para sua posicao: "
                + dragonService.formatLocation(location) + ".");
    }

    private void reset(CommandSender sender) {
        dragonScheduler.resetTimer();
        sender.sendMessage(dragonService.prefix() + "Contador reiniciado. Proximo spawn: "
                + dragonScheduler.formatNextSpawn() + ".");
    }

    private void reload(CommandSender sender) {
        dragonScheduler.reload();
        dragonService.refreshChampionNpcFromConfig();
        sender.sendMessage(dragonService.prefix() + "Config recarregada. Proximo spawn: "
                + dragonScheduler.formatNextSpawn() + ".");
    }

    private void setEnabled(CommandSender sender, boolean enabled) {
        dragonScheduler.setEnabled(enabled);
        sender.sendMessage(dragonService.prefix() + "Agendamento " + (enabled ? "ativado" : "desativado") + ".");
    }

    private World resolveWorld(CommandSender sender, String worldName) {
        if (worldName != null && !worldName.isBlank()) {
            return Bukkit.getWorld(worldName);
        }
        if (sender instanceof Player player) {
            return player.getWorld();
        }
        String configuredWorld = plugin.getConfig().getString("world", "world_the_end");
        return Bukkit.getWorld(configuredWorld == null ? "world_the_end" : configuredWorld);
    }

    private Double parseCoordinate(CommandSender sender, String raw, char axis) {
        if (raw.equals("~")) {
            if (sender instanceof Player player) {
                return switch (axis) {
                    case 'x' -> player.getLocation().getX();
                    case 'y' -> player.getLocation().getY();
                    case 'z' -> player.getLocation().getZ();
                    default -> null;
                };
            }
            return null;
        }

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Long parseDurationMillis(String raw) {
        String value = raw.toLowerCase(Locale.ROOT).trim().replace(',', '.');
        double multiplier = 3_600_000.0;

        if (value.endsWith("h")) {
            value = value.substring(0, value.length() - 1);
            multiplier = 3_600_000.0;
        } else if (value.endsWith("m")) {
            value = value.substring(0, value.length() - 1);
            multiplier = 60_000.0;
        } else if (value.endsWith("s")) {
            value = value.substring(0, value.length() - 1);
            multiplier = 1000.0;
        }

        try {
            double amount = Double.parseDouble(value);
            if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
                return null;
            }
            return (long) (amount * multiplier);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean matches(String value, String... candidates) {
        return Arrays.stream(candidates).anyMatch(candidate -> candidate.equalsIgnoreCase(value));
    }

    private List<String> filter(List<String> options, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
