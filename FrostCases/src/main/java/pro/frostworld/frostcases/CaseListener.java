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
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
        
        if (plugin.isCaseRunning(locStr)) {
            p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cЭтот кейс уже открывают! Подождите!"));
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
        if (e.getClickedInventory() == null) return;
        Inventory inv = e.getInventory();
        
        if (inv.getHolder() == null) return;
        String dataStr = inv.getHolder().toString();
        if (!dataStr.contains(";")) return;
        
        e.setCancelled(true); 

        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("cases");
        if (cs == null) return;

        String[] dataParts = dataStr.split(";");
        String caseID = dataParts[0];
        String locStr = dataParts[1];

        ConfigurationSection sec = cs.getConfigurationSection(caseID);
        if (sec == null) return;

        if (e.getSlot() == sec.getInt("button-slot", 13)) {
            Player p = (Player) e.getWhoClicked();

            String[] locParts = locStr.split(",");
            if (locParts.length < 4) { p.closeInventory(); return; }
            org.bukkit.World w = Bukkit.getWorld(locParts[0]);
            if (w == null) { p.closeInventory(); return; }
            Location blockLoc = new Location(w, Integer.parseInt(locParts[1]), Integer.parseInt(locParts[2]), Integer.parseInt(locParts[3]));

            String keyPath = "keys." + p.getUniqueId() + "." + caseID;
            int playerKeys = plugin.getDataConfig().getInt(keyPath, 0);

            if (playerKeys <= 0) {
                p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cКлючи закончились!"));
                p.closeInventory();
                return;
            }

            plugin.getDataConfig().set(keyPath, playerKeys - 1);
            plugin.saveDataConfig();

            p.closeInventory(); 
            plugin.start3DRoulette(p, caseID, sec, locStr, blockLoc);
        }
    }
}
