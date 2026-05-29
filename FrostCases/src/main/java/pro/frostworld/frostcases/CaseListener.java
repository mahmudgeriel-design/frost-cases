package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

public class CaseListener implements Listener {
    private final FrostCases plugin;
    public CaseListener(FrostCases plugin) { this.plugin = plugin; }

    @EventHandler
    public void onEntityClick(PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked() instanceof ArmorStand) {
            Location loc = e.getRightClicked().getLocation().getBlock().getLocation();
            String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            if (plugin.getDataConfig().contains("placed-cases." + locStr)) {
                e.setCancelled(true);
                handleCaseOpening(e.getPlayer(), locStr, loc);
            }
        }
    }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        if (!plugin.getDataConfig().contains("placed-cases." + locStr)) return;
        e.setCancelled(true);

        handleCaseOpening(e.getPlayer(), locStr, loc);
    }

    private void handleCaseOpening(Player p, String locStr, Location blockLoc) {
        String caseID = plugin.getDataConfig().getString("placed-cases." + locStr);
        
        // Проверяем, не крутится ли этот кейс прямо сейчас кем-то другим
        if (plugin.isCaseRunning(locStr)) {
            p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cЭтот кейс уже кто-то открывает! Подождите!"));
            return;
        }

        String keyPath = "keys." + p.getUniqueId() + "." + caseID;
        int playerKeys = plugin.getDataConfig().getInt(keyPath, 0);

        if (playerKeys <= 0) {
            p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cУ вас нет ключей для кейса &b" + caseID + "&c!"));
            return;
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("cases." + caseID);
        if (sec != null) {
            plugin.openMenu(p, caseID, sec, locStr, blockLoc);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String viewTitle = e.getView().getTitle();
        if (!viewTitle.contains(String.valueOf(ChatColor.COLOR_CHAR) + "x")) return;
        e.setCancelled(true);

        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();

        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("cases");
        if (cs == null) return;

        for (String id : cs.getKeys(false)) {
            ConfigurationSection sec = cs.getConfigurationSection(id);
            String title = ChatColor.translateAlternateColorCodes('&', sec.getString("menu-title", ""));
            
            // Проверяем нажатие на кнопку "Открыть" (Слот 13)
            if (viewTitle.startsWith(title) && e.getSlot() == sec.getInt("button-slot", 13)) {
                // Извлекаем скрытые данные локации из заголовка
                String[] titleParts = viewTitle.split(String.valueOf(ChatColor.COLOR_CHAR) + "z");
                if (titleParts.length < 2) {
                    p.closeInventory();
                    return;
                }
                String locStr = titleParts[1];
                String[] locParts = locStr.split(",");
                Location blockLoc = new Location(Bukkit.getWorld(locParts[0]), Integer.parseInt(locParts[1]), Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));

                String keyPath = "keys." + p.getUniqueId() + "." + id;
                int playerKeys = plugin.getDataConfig().getInt(keyPath, 0);

                if (playerKeys <= 0) {
                    p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cКлючи закончились!"));
                    p.closeInventory();
                    return;
                }

                // Списываем ключ
                plugin.getDataConfig().set(keyPath, playerKeys - 1);
                plugin.saveDataConfig();

                p.closeInventory(); // Мгновенно закрываем GUI, чтобы смотреть на шалкер!
                
                // Запускаем безумную 3D крутилку вокруг блока!
                plugin.start3DRoulette(p, id, sec, locStr, blockLoc);
                break;
            }
        }
    }
}
