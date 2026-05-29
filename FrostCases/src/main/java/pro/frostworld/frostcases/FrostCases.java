package pro.frostworld.frostcases;

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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("frostcases.admin")) {
                if (sender instanceof Player p) {
                    p.sendRawMessage(ChatColor.RED + "Нет прав!");
                } else {
                    sender.sendMessage(ChatColor.RED + "Нет прав!");
                }
                return true;
            }
            reloadConfig();
            if (sender instanceof Player p) {
                p.sendRawMessage(ChatColor.GREEN + "Конфиг перезагружен!");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Конфиг перезагружен!");
            }
            return true;
        }
        
        if (!(sender instanceof Player player)) return true;
        
        if (args.length < 1) {
            player.sendRawMessage(ChatColor.RED + "Пиши: /frostcases [donate/frostik/titles]");
            return true;
        }
        
        String id = args[0].toLowerCase();
        ConfigurationSection sec = getConfig().getConfigurationSection("cases." + id);
        if (sec == null) {
            player.sendRawMessage(ChatColor.RED + "Кейс не найден!");
            return true;
        }
        openMenu(player, id, sec);
        return true;
    }

    private void openMenu(Player p, String id, ConfigurationSection sec) {
        String title = sec.getString("menu-title", "Кейс");
        int size = sec.getInt("menu-size", 27);
        String hidden = ChatColor.COLOR_CHAR + "x" + ChatColor.COLOR_CHAR + id.substring(0,1);
        Inventory gui = org.bukkit.Bukkit.createInventory(null, size, ChatColor.translateAlternateColorCodes('&', title) + hidden);
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
}
