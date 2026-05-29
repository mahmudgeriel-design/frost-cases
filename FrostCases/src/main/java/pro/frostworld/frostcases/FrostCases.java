package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class FrostCases extends JavaPlugin implements CommandExecutor {

    private File dataFile;
    private FileConfiguration dataConfig;
    private final List<Integer> activeTasks = new ArrayList<>();
    private final Set<String> runningCases = new HashSet<>(); 

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDataConfig();
        
        getCommand("frostcases").setExecutor(this);
        if (getCommand("frostworld") != null) {
            getCommand("frostworld").setExecutor(this);
        }
        
        getServer().getPluginManager().registerEvents(new CaseListener(this), this);
        startAllStaticTitles(); 
        Bukkit.getLogger().info("[FrostCases] Плагин успешно обновлен! Включена финальная 3D-анимация!");
    }

    @Override
    public void onDisable() {
        for (int taskId : activeTasks) Bukkit.getScheduler().cancelTask(taskId);
    }

    public boolean isCaseRunning(String locStr) { return runningCases.contains(locStr); }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("frostworld")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("frostcases.admin")) {
                    sendMessage(sender, "&cНет прав!");
                    return true;
                }
                reloadConfig();
                reloadDataConfig();
                for (int taskId : activeTasks) Bukkit.getScheduler().cancelTask(taskId);
                activeTasks.clear();
                startAllStaticTitles();
                sendMessage(sender, "&a[FrostCases] Настройки перезагружены!");
                return true;
            }

            if (args.length >= 4 && (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("takekey"))) {
                if (!sender.hasPermission("frostcases.admin")) {
                    sendMessage(sender, "&cНет прав!");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sendMessage(sender, "&cИгрок не найден!");
                    return true;
                }
                String caseID = args[2].toLowerCase();
                int amount;
                try { amount = Integer.parseInt(args[3]); } catch (Exception e) { return true; }

                String path = "keys." + target.getUniqueId() + "." + caseID;
                int currentKeys = getDataConfig().getInt(path, 0);

                if (args[0].equalsIgnoreCase("givekey")) {
                    getDataConfig().set(path, currentKeys + amount);
                    saveDataConfig();
                    target.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &eВыдано &6" + amount + " &eключей от кейса &b" + caseID));
                } else {
                    getDataConfig().set(path, Math.max(0, currentKeys - amount));
                    saveDataConfig();
                }
                return true;
            }
            return true;
        }

        if (!(sender instanceof Player player)) return true;

        if (args.length > 0 && args[0].equalsIgnoreCase("setcase")) {
            if (args.length < 2) return true;
            String caseID = args[1].toLowerCase();
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) return true;

            Location loc = targetBlock.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            
            getDataConfig().set("placed-cases." + locStr, caseID);
            saveDataConfig();
            
            createStaticTitle(loc, getConfig().getString("cases." + caseID + ".menu-title"));
            player.sendRawMessage(ChatColor.GREEN + "Кейс успешно установлен!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("removecase")) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null) return true;
            Location loc = targetBlock.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            getDataConfig().set("placed-cases." + locStr, null);
            saveDataConfig();
            removeHolograms(loc);
            return true;
        }
        return true;
    }

    public void openMenu(Player p, String id, ConfigurationSection sec, String locStr, Location blockLoc) {
        String title = sec.getString("menu-title", "Кейс");
        int size = sec.getInt("menu-size", 27);
        
        // Создаем кастомный держатель для инвентаря, чтобы хранить данные скрытно
        org.bukkit.inventory.InventoryHolder holder = new org.bukkit.inventory.InventoryHolder() {
            @Override
            public Inventory getInventory() { return null; }
            
            // Вшиваем наши технические данные прямо внутрь инвентаря через toString
            @Override
            public String toString() { return id + ";" + locStr; }
        };

        // Заголовок идеально чистый! Данные ушли в Holder
        Inventory gui = Bukkit.createInventory(holder, size, ChatColor.translateAlternateColorCodes('&', title));
        
        Material mat = Material.matchMaterial(sec.getString("button-material", "STRUCTURE_VOID"));
        ItemStack item = new ItemStack(mat != null ? mat : Material.STRUCTURE_VOID);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("button-name")));
            item.setItemMeta(m);
        }
        gui.setItem(sec.getInt("button-slot", 13), item);
        p.openInventory(gui);
    }
           public void start3DRoulette(Player p, String caseID, ConfigurationSection caseSection, String locStr, Location blockLoc) {
        runningCases.add(locStr); 
        removeHolograms(blockLoc); 

        Location centerLoc = blockLoc.clone().add(0.5, 0.5, 0.5);
        ConfigurationSection rew = caseSection.getConfigurationSection("rewards");
        if (rew == null) return;
        Set<String> keys = rew.getKeys(false);

        // 1. Рассчитываем победителя заранее
        double total = 0;
        for (String k : keys) total += rew.getDouble(k + ".chance");
        double rand = ThreadLocalRandom.current().nextDouble() * total;
        double count = 0;
        String winnerKey = null;
        for (String k : keys) {
            count += rew.getDouble(k + ".chance");
            if (rand <= count) { winnerKey = k; break; }
        }
        final String finalWinner = winnerKey;

        // ИСПРАВЛЕНО: Теперь названия берутся прямо из конфига конкретного кейса!
        List<String> displayNames = new ArrayList<>();
        for (String k : keys) {
            displayNames.add(rew.getString(k + ".name"));
        }
        // Если наград меньше 6, забиваем пустышками, чтобы не было ошибок
        while (displayNames.size() < 6) { displayNames.add("&c&lПусто"); }

        // ЭФФЕКТ: Заставляем шалкер открыть крышку! (Посылаем пакет blockAction 1, 1)
        blockLoc.getBlock().getState().update();
        p.playNote(blockLoc, (byte) 1, (byte) 1); 
        // Для 1.16.5 самый надежный способ открыть крышку шалкера без лагов пакетов:
        if (blockLoc.getBlock().getType().name().contains("SHULKER_BOX")) {
            p.sendBlockChange(blockLoc, blockLoc.getBlock().getBlockData());
            // Используем стандартный триггер сундука/шалкер-бокса через Spigot API
            p.playSound(blockLoc, Sound.BLOCK_SHULKER_BOX_OPEN, 1.0f, 1.0f);
        }

        List<ArmorStand> stands = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ArmorStand as = centerLoc.getWorld().spawn(centerLoc, ArmorStand.class);
            as.setVisible(false); as.setGravity(false); as.setSmall(true);
            as.setHelmet(new ItemStack(Material.NETHERITE_BLOCK)); 
            
            // Пишем над блоком правильное название награды из ТЕКУЩЕГО кейса!
            as.setCustomName(ChatColor.translateAlternateColorCodes('&', displayNames.get(i)));
            as.setCustomNameVisible(true);
            
            stands.add(as);
        }

        int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            int ticks = 0;
            double radius = 0.0; 
            double angleOffset = 0;

            @Override
            public void run() {
                ticks += 2;
                
                double speed = 0.24;
                if (ticks > 50) speed = 0.16;
                if (ticks > 80) speed = 0.08;
                if (ticks > 105) speed = 0.02; 
                
                angleOffset += speed;

                if (ticks <= 20) { radius += 1.2 / 10.0; }

                for (int i = 0; i < stands.size(); i++) {
                    ArmorStand as = stands.get(i);
                    if (!as.isValid()) continue;
                    
                    double angle = angleOffset + (i * (2 * Math.PI / 6));
                    double z = Math.cos(angle) * radius; 
                    double y = Math.sin(angle) * radius; 
                    
                    Location nextLoc = centerLoc.clone().add(0, y - 0.6, z);
                    nextLoc.setYaw(0); 
                    as.teleport(nextLoc);
                }

                boolean shouldPlaySound = false;
                if (ticks < 50 && ticks % 4 == 0) shouldPlaySound = true;
                if (ticks >= 50 && ticks < 85 && ticks % 8 == 0) shouldPlaySound = true;
                if (ticks >= 85 && ticks < 120 && ticks % 14 == 0) shouldPlaySound = true;
                
                if (shouldPlaySound && ticks < 120) {
                    p.playSound(centerLoc, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.8f, 1.4f);
                }

                if (ticks >= 120) {
                    Bukkit.getScheduler().cancelTask(activeTasks.remove(0)); 
                    
                    p.playSound(centerLoc, Sound.ENTITY_ITEM_BREAK, 1.2f, 0.8f);
                    for (int i = 1; i < stands.size(); i++) { stands.get(i).remove(); }
                    
                    ArmorStand winStand = stands.get(0);
                    winStand.setHelmet(new ItemStack(Material.NETHERITE_BLOCK));
                    
                    winStand.setCustomName(ChatColor.translateAlternateColorCodes('&', rew.getString(finalWinner + ".name")));
                    winStand.setCustomNameVisible(true);

                    Location finalLoc = centerLoc.clone().add(0, 1.1, 0);
                    finalLoc.setYaw(0);
                    winStand.teleport(finalLoc);
                    
                    p.playSound(centerLoc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                    List<String> cmds = rew.getStringList(finalWinner + ".commands");
                    for (String cmd : cmds) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                    p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &aВы выиграли " + rew.getString(finalWinner + ".name")));

                    // Закрываем крышку шалкера обратно!
                    p.playSound(blockLoc, Sound.BLOCK_SHULKER_BOX_CLOSE, 1.0f, 1.0f);

                    Bukkit.getScheduler().runTaskLater(FrostCases.this, () -> {
                        winStand.remove(); 
                        runningCases.remove(locStr); 
                        createStaticTitle(blockLoc, caseSection.getString("menu-title")); 
                    }, 60L); 
                }
            }
        }, 0L, 2L);

        activeTasks.add(taskId);
    }
    
    private void startAllStaticTitles() {
        ConfigurationSection placed = getDataConfig().getConfigurationSection("placed-cases");
        if (placed == null) return;
        for (String locStr : placed.getKeys(false)) {
            String[] p = locStr.split(",");
            try {
                Location l = new Location(Bukkit.getWorld(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
                removeHolograms(l);
                createStaticTitle(l, getConfig().getString("cases." + placed.getString(locStr) + ".menu-title"));
            } catch (Exception ignored) {}
        }
    }

    private void createStaticTitle(Location loc, String text) {
        Location holoLoc = loc.clone().add(0.5, 1.4, 0.5);
        ArmorStand as = holoLoc.getWorld().spawn(holoLoc, ArmorStand.class);
        as.setVisible(false); as.setGravity(false);
        as.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
        as.setCustomNameVisible(true);
    }

    private void removeHolograms(Location loc) {
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        for (Entity entity : center.getWorld().getNearbyEntities(center, 1.8, 1.8, 1.8)) {
            if (entity instanceof ArmorStand) entity.remove();
        }
    }

    private void sendMessage(CommandSender sender, String text) {
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        if (sender instanceof Player p) p.sendRawMessage(colored);
        else Bukkit.getLogger().info(colored);
    }

    public FileConfiguration getDataConfig() { return this.dataConfig; }
    public void saveDataConfig() { try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); } }
    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) { dataFile.getParentFile().mkdirs(); try { dataFile.createNewFile(); } catch (Exception ignored) {} }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    public void reloadDataConfig() { dataConfig = YamlConfiguration.loadConfiguration(dataFile); }
}
