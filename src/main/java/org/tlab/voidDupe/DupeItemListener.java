package org.tlab.voidDupe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.stream.Collectors;

public class DupeItemListener implements Listener {

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack item = event.getItem().getItemStack();

        List<String> blacklistRaw = VoidDupe.getInstance().getConfig().getStringList("blacklisted-items");
        List<Material> blacklist = blacklistRaw.stream()
                .map(String::toUpperCase)
                .map(name -> {
                    try {
                        return Material.valueOf(name);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(m -> m != null)
                .collect(Collectors.toList());

        if (blacklist.contains(item.getType()) || ItemCheckUtil.hasIllegalEnchantments(item)) {
            addUndupeableLore(item);
        }
    }

    private void addUndupeableLore(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = meta.hasLore() ? meta.getLore() : new java.util.ArrayList<>();
        String tag = ChatColor.RED + "UNDUPEABLE";
        if (!lore.contains(tag)) {
            lore.add(tag);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
    }
}
