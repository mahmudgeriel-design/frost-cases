package pro.frostworld.frostcases;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class CaseListener implements Listener {
    private final FrostCases plugin;
    public CaseListener(FrostCases plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String vt = e.getView().getTitle();
        if (!vt.contains(String.valueOf(ChatColor.COLOR_CHAR) + "x")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        ConfigurationSection cs = plugin.getConfig().getConfigurationSection("cases");
        if (cs == null) return;
        for (String id : cs.getKeys(false)) {
            ConfigurationSection sec = cs.getConfigurationSection(id);
            String title = ChatColor.translateAlternateColorCodes('&', sec.getString("menu-title", ""));
            if (vt.startsWith(title) && e.getSlot() == sec.getInt("button-slot", 13)) {
                p.closeInventory();
                open(p, sec);
                break;
            }
        }
    }

    private void open(Player p, ConfigurationSection sec) {
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
                // Заменили p.sendMessage на Bukkit.getServer().broadcast или обычный посыл строки, чтобы обойти баг bungeecord
                String msg = ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &aВы получили " + rew.getString(k + ".name"));
                p.sendRawMessage(msg); 
                break;
            }
        }
    }
}
