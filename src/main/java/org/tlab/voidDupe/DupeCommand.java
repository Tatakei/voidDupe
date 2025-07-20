package org.tlab.voidDupe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.block.ShulkerBox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DupeCommand implements CommandExecutor {
    private static final String voiddupemain = ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "Void" + ChatColor.DARK_PURPLE + "Dupe" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "| ";

    private boolean containsBlacklistedItems(ItemStack container, List<Material> blacklist) {
        if (container.getItemMeta() instanceof BlockStateMeta meta) {
            if (meta.getBlockState() instanceof ShulkerBox shulker) {
                for (ItemStack content : shulker.getInventory().getContents()) {
                    if (content != null && blacklist.contains(content.getType())) {
                        return true;
                    }
                }
            }
        } else if (container.getItemMeta() instanceof BundleMeta bundleMeta) {
            for (ItemStack content : bundleMeta.getItems()) {
                if (content != null && blacklist.contains(content.getType())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!player.hasPermission("dupe.use")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        int requestedAmount = 1; // default to 1
        if (args.length > 1) {
            player.sendMessage(voiddupemain + ChatColor.GREEN + "Usage: /dupe <amount>");
            return true;
        }
        if (args.length == 1) {
            try {
                requestedAmount = Integer.parseInt(args[0]);
                if (requestedAmount <= 0) {
                    player.sendMessage(voiddupemain + ChatColor.RED + "Amount must be a positive number.");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(voiddupemain + ChatColor.RED + "Invalid number.");
                return true;
            }
        }

        int maxDupeAmount = VoidDupe.getInstance().getConfig().getInt("max-dupe-amount", 5);
        if (requestedAmount > maxDupeAmount) {
            player.sendMessage(voiddupemain + ChatColor.LIGHT_PURPLE + "The max dupe you can do is " + ChatColor.DARK_PURPLE + maxDupeAmount + ChatColor.LIGHT_PURPLE + "!");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(voiddupemain + ChatColor.RED + "You must be holding an item.");
            return true;
        }

        Material itemType = item.getType();
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

        if (blacklist.contains(itemType)) {
            player.sendMessage(voiddupemain + ChatColor.RED + "You are not allowed to duplicate the held item.");
            return true;
        }

        if (containsBlacklistedItems(item, blacklist)) {
            player.sendMessage(ChatColor.RED + "You can't duplicate this container: it contains blacklisted items.");
            return true;
        }

        int originalAmount = item.getAmount();
        int maxDupedAmount = 384;

        int doublingFactor = (int) Math.pow(2, requestedAmount);
        int doubledAmount = originalAmount * doublingFactor;
        if (doubledAmount > maxDupedAmount) {
            doubledAmount = maxDupedAmount;
        }

        int toGive = doubledAmount - originalAmount;
        if (toGive <= 0) {
            player.sendMessage(voiddupemain + ChatColor.RED + "Nothing to duplicate.");
            return true;
        }

        int maxStackSize = item.getMaxStackSize();

        while (toGive > 0) {
            int giveAmount = Math.min(toGive, maxStackSize);
            ItemStack clone = item.clone();
            clone.setAmount(giveAmount);

            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(clone);
            if (!leftovers.isEmpty()) {
                player.sendMessage(voiddupemain + ChatColor.RED + "Not enough inventory space to dupe all items.");
                break;
            }
            toGive -= giveAmount;
        }

        player.sendMessage(voiddupemain + ChatColor.GREEN + "Your item has been duped " + ChatColor.WHITE + requestedAmount + ChatColor.GREEN + " times.");
        return true;
    }
}
