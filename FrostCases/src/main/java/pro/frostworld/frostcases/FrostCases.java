package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class FrostCases extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getCommand("frostcases").setExecutor(this);
        getServer().getPluginManager().registerEvents(new CaseListener(this), this);
        getLogger().info("Плагин FrostCases на много кейсов успешно запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("frostcases.admin")) {
                sender.sendMessage(ChatColor.RED + "У тебя нет прав!");
                return true;
            }
            reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Конфиг FrostCases успешно перезагружен!");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Используй: /frostcases [donate/frostik/titles]");
            return true;
        }

        String caseID = args[0].toLowerCase();
        ConfigurationSection caseSection = getConfig().getConfigurationSection("cases." + caseID);

        if (caseSection == null) {
            player.sendMessage(ChatColor.RED + "Такой кейс не найден в конфиге!");
            return true;
        }

        openCaseMenu(player, caseID, caseSection);
        return true;
    }

    public void openCaseMenu(Player player, String caseID, ConfigurationSection section) {
        String title = section.getString("menu-title", "Кейс");
        int size = section.getInt("menu-size", 27);
        
        // Вшиваем ID кейса в заголовок в виде невидимого цветового кода, чтобы Listener понимал, какой именно кейс открыт!
        String hiddenId = ChatColor.COLOR_CHAR + "x" + ChatColor.COLOR_CHAR + caseID.substring(0,1); 
        Inventory gui = Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title) + hiddenId);

        Material mat = Material.matchMaterial(section.getString("button-material", "STRUCTURE_VOID"));
        if (mat == null) mat = Material.STRUCTURE_VOID;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', section.getString("button-name")));
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("button-lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(lore);
            meta.setCustomModelData(section.getInt("button-custom-model-data", 10000));
            item.setItemMeta(meta);
        }

        gui.setItem(section.getInt("button-slot", 13), item);
        player.openInventory(gui);
    }
}