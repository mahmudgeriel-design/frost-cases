package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.List;

public final class FrostCases extends JavaPlugin implements CommandExecutor {

    private File dataFile;
    private FileConfiguration dataConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createDataConfig();
        
        getCommand("frostcases").setExecutor(this);
        if (getCommand("frostworld") != null) {
            getCommand("frostworld").setExecutor(this);
        }
        
        getServer().getPluginManager().registerEvents(new CaseListener(this), this);
        Bukkit.getLogger().info("[FrostCases] Плагин успешно обновлен до версии с блоками и ключами!");
    }
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
                sendMessage(sender, "&a[FrostCases] Конфиг и база данных успешно перезагружены!");
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
                if (!getConfig().contains("cases." + caseID)) {
                    sendMessage(sender, "&cТакой кейс не настроен в config.yml!");
                    return true;
                }
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sendMessage(sender, "&cКоличество должно быть числом!");
                    return true;
                }

                String path = "keys." + target.getUniqueId() + "." + caseID;
                int currentKeys = getDataConfig().getInt(path, 0);

                if (args[0].equalsIgnoreCase("givekey")) {
                    getDataConfig().set(path, currentKeys + amount);
                    saveDataConfig();
                    target.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &eВам выдано &6" + amount + " &eключ(а/ей) от кейса &b" + caseID));
                    sendMessage(sender, "&aИгроку &e" + target.getName() + " &aвыдано &e" + amount + " &aключей.");
                } else {
                    int result = Math.max(0, currentKeys - amount);
                    getDataConfig().set(path, result);
                    saveDataConfig();
                    target.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cУ вас забрали &6" + amount + " &cключ(а/ей) от кейса &b" + caseID));
                    sendMessage(sender, "&aУ игрока &e" + target.getName() + " &aзабрано &e" + amount + " &aключей.");
                }
                return true;
            }
            sendMessage(sender, "&cИспользуй: /frostworld [reload/givekey/takekey]");
            return true;
        }

        if (!(sender instanceof Player player)) {
            Bukkit.getLogger().warning("Команды блоков только для игроков!");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("setcase")) {
            if (!player.hasPermission("frostcases.admin")) {
                player.sendRawMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            if (args.length < 2) {
                player.sendRawMessage(ChatColor.RED + "Пиши: /frostcases setcase [название_кейса]");
                return true;
            }
            String caseID = args[1].toLowerCase();
            if (!getConfig().contains("cases." + caseID)) {
                player.sendRawMessage(ChatColor.RED + "Этот кейс не найден в конфиге!");
                return true;
            }

            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                player.sendRawMessage(ChatColor.RED + "Ты должен смотреть на block (макс. 5 блоков)!");
                return true;
            }

            Location loc = targetBlock.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            
            getDataConfig().set("placed-cases." + locStr, caseID);
            saveDataConfig();
            
            createHologram(loc, getConfig().getString("cases." + caseID + ".menu-title", "&bКейс"));

            player.sendRawMessage(ChatColor.GREEN + "Кейс " + caseID + " успешно установлен на этот блок!");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("removecase")) {
            if (!player.hasPermission("frostcases.admin")) {
                player.sendRawMessage(ChatColor.RED + "Нет прав!");
                return true;
            }
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null) {
                player.sendRawMessage(ChatColor.RED + "Смотри на блок кейса!");
                return true;
            }
            Location loc = targetBlock.getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            if (!getDataConfig().contains("placed-cases." + locStr)) {
                player.sendRawMessage(ChatColor.RED + "На этом блоке нет нашего кейса!");
                return true;
            }

            getDataConfig().set("placed-cases." + locStr, null);
            saveDataConfig();
            removeHologram(loc);

            player.sendRawMessage(ChatColor.YELLOW + "Кейс успешно удален с этого блока!");
            return true;
        }

        String id = args.length > 0 ? args[0].toLowerCase() : "";
        ConfigurationSection sec = getConfig().getConfigurationSection("cases." + id);
        if (sec == null) {
            player.sendRawMessage(ChatColor.RED + "Пиши: /frostcases [название_кейса] или используй блоки!");
            return true;
        }
        openMenu(player, id, sec);
        return true;
    }

    public void openMenu(Player p, String id, ConfigurationSection sec) {
        String title = sec.getString("menu-title", "Кейс");
        int size = sec.getInt("menu-size", 27);
        String hidden = ChatColor.COLOR_CHAR + "x" + ChatColor.COLOR_CHAR + id.substring(0,1);
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title) + hidden);
        
        Material mat = Material.matchMaterial(sec.getString("button-material", "STRUCTURE_VOID"));
        if (mat == null) mat = Material.STRUCTURE_VOID;
        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.translateAlternateColorCodes('&', sec.getString("button-name")));
            List<String> lore = new ArrayList<>();
            for (String l : sec.getStringList("button-lore")) lore.add(ChatColor.translateAlternateColorCodes('&', l));
            m.setLore(lore);
            m.setCustomModelData(sec.getInt("button-custom-model-data", 10000));
            item.setItemMeta(m);
        }
        gui.setItem(sec.getInt("button-slot", 13), item);
        p.openInventory(gui);
    }

    private void sendMessage(CommandSender sender, String text) {
        String colored = ChatColor.translateAlternateColorCodes('&', text);
        if (sender instanceof Player p) p.sendRawMessage(colored);
        else Bukkit.getLogger().info(colored);
    }

    public FileConfiguration getDataConfig() { return this.dataConfig; }
    
    public void saveDataConfig() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void createDataConfig() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void reloadDataConfig() { dataConfig = YamlConfiguration.loadConfiguration(dataFile); }

    private void createHologram(Location loc, String text) {
        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        ArmorStand as = holoLoc.getWorld().spawn(holoLoc, ArmorStand.class);
        as.setVisible(false);
        as.setGravity(false);
        as.setCanPickupItems(false);
        as.setCustomName(ChatColor.translateAlternateColorCodes('&', text));
        as.setCustomNameVisible(true);
    }

    private void removeHologram(Location loc) {
        Location holoLoc = loc.clone().add(0.5, 1.2, 0.5);
        for (Entity entity : holoLoc.getWorld().getNearbyEntities(holoLoc, 0.5, 0.5, 0.5)) {
            if (entity instanceof ArmorStand) {
                entity.remove();
            }
        }
    }
}
