package org.tlab.voidDupe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class DupeCommand implements CommandExecutor {
    private static final String voiddupemain = ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE + "Void" + ChatColor.DARK_PURPLE + "Dupe" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY + "| ";

    private static final Map<Enchantment, Integer> ILLEGAL_ENCHANTMENTS;

    static {
        Map<Enchantment, Integer> map = new HashMap<>();
        putEnchantment(map, "sharpness", 6);
        putEnchantment(map, "efficiency", 8);
        putEnchantment(map, "unbreaking", 4);
        putEnchantment(map, "protection", 6);
        putEnchantment(map, "blast_protection", 6);
        putEnchantment(map, "depth_strider", 4);
        putEnchantment(map, "density", 6);
        putEnchantment(map, "breach", 5);
        ILLEGAL_ENCHANTMENTS = Collections.unmodifiableMap(map);
    }

    private static void putEnchantment(Map<Enchantment, Integer> map, String key, int minLevel) {
        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key));
        if (enchant != null) {
            map.put(enchant, minLevel);
        }
    }

    private static final Set<Material> DYES = Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("_DYE"))
            .collect(Collectors.toSet());

    private boolean containsBlacklistedItems(ItemStack container, List<Material> materialBlacklist) {
        if (container.getItemMeta() instanceof BlockStateMeta meta) {
            if (meta.getBlockState() instanceof ShulkerBox shulker) {
                for (ItemStack content : shulker.getInventory().getContents()) {
                    if (content != null && (materialBlacklist.contains(content.getType()) || hasIllegalEnchantments(content))) {
                        return true;
                    }
                }
            }
        } else if (container.getItemMeta() instanceof BundleMeta bundleMeta) {
            for (ItemStack content : bundleMeta.getItems()) {
                if (content != null && (materialBlacklist.contains(content.getType()) || hasIllegalEnchantments(content))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasIllegalEnchantments(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        Map<Enchantment, Integer> enchants = meta.getEnchants();

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (ILLEGAL_ENCHANTMENTS.containsKey(enchantment) && level >= ILLEGAL_ENCHANTMENTS.get(enchantment)) {
                return true;
            }
        }

        Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
        if (DYES.contains(item.getType()) && unbreaking != null &&
                enchants.getOrDefault(unbreaking, 0) == 1) {
            return true;
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

        int requestedAmount = 1;
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
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (blacklist.contains(itemType)) {
            player.sendMessage(voiddupemain + ChatColor.RED + "You are not allowed to duplicate the held item.");
            return true;
        }

        if (hasIllegalEnchantments(item)) {
            player.sendMessage(voiddupemain + ChatColor.RED + "This item contains illegal enchantments and cannot be duplicated.");
            return true;
        }

        if (containsBlacklistedItems(item, blacklist)) {
            player.sendMessage(voiddupemain + ChatColor.RED + "You can't duplicate this container: it contains blacklisted or illegal items.");
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
