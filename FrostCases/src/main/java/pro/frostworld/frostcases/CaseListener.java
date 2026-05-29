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
        String viewTitle = e.getView().getTitle();
        
        // Проверяем наш скрытый маркер ID кейса
        if (!viewTitle.contains(String.valueOf(ChatColor.COLOR_CHAR) + "x")) return;
        e.setCancelled(true); // Защита предметов от кражи ххаха

        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();

        // Определяем, какой кейс открыт, сканируя конфиг
        ConfigurationSection cases = plugin.getConfig().getConfigurationSection("cases");
        if (cases == null) return;

        for (String caseID : cases.getKeys(false)) {
            ConfigurationSection sec = cases.getConfigurationSection(caseID);
            String title = ChatColor.translateAlternateColorCodes('&', sec.getString("menu-title", ""));
            
            if (viewTitle.startsWith(title) && e.getSlot() == sec.getInt("button-slot", 13)) {
                p.closeInventory();
                openCrate(p, sec);
                break;
            }
        }
    }

    private void openCrate(Player p, ConfigurationSection caseSection) {
        ConfigurationSection rewardsSection = caseSection.getConfigurationSection("rewards");
        if (rewardsSection == null) return;
        Set<String> keys = rewardsSection.getKeys(false);
        
        double total = 0;
        for (String k : keys) total += rewardsSection.getDouble(k + ".chance");
        double rand = ThreadLocalRandom.current().nextDouble() * total;
        
        double count = 0;
        for (String k : keys) {
            count += rewardsSection.getDouble(k + ".chance");
            if (rand <= count) {
                List<String> cmds = rewardsSection.getStringList(k + ".commands");
                for (String cmd : cmds) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", p.getName()));
                }
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFrostCases &8» &aВы успешно выиграли " + rewardsSection.getString(k + ".name")));
                break;
            }
        }
    }
}