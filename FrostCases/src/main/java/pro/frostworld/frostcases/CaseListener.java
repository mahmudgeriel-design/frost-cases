package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CaseListener implements Listener {
    private final FrostCases plugin;
    public CaseListener(FrostCases plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBlockClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = e.getClickedBlock();
        if (block == null) return;

        Location loc = block.getLocation();
        String locStr = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        if (!plugin.getDataConfig().contains("placed-cases." + locStr)) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        String caseID = plugin.getDataConfig().getString("placed-cases." + locStr);
        
        String keyPath = "keys." + p.getUniqueId() + "." + caseID;
        int playerKeys = plugin.getDataConfig().getInt(keyPath, 0);

        if (playerKeys <= 0) {
            p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cУ вас нет ключей для открытия кейса &b" + caseID + "&c!"));
            return;
        }

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("cases." + caseID);
        if (sec != null) {
            plugin.openMenu(p, caseID, sec);
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
            
            if (viewTitle.startsWith(title) && e.getSlot() == sec.getInt("button-slot", 13)) {
                p.closeInventory();
                
                String keyPath = "keys." + p.getUniqueId() + "." + id;
                int playerKeys = plugin.getDataConfig().getInt(keyPath, 0);

                if (playerKeys <= 0) {
                    p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &cКлючи закончились!"));
                    return;
                }

                plugin.getDataConfig().set(keyPath, playerKeys - 1);
                plugin.saveDataConfig();

                openCrate(p, sec);
                break;
            }
        }
    }

    private void openCrate(Player p, ConfigurationSection sec) {
        ConfigurationSection rew = sec.getConfigurationSection("rewards");
        if (rew == null) return;
        Set<String> keys = rew.getKeys(false);
        double total = 0;
        for (String k : keys) total += rew.getDouble(k + ".chance");
        double rand = ThreadLocalRandom.current().nextDouble() * total;
        double count = 0;
        for (String k : keys) {
            count += rew.getDouble(k + ".chance");
            if (rand <= count) {
                List<String> cmds = rew.getStringList(k + ".commands");
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                }
                p.sendRawMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &aВы получили " + rew.getString(k + ".name")));
                break;
            }
        }
    }
}
