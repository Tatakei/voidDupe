package org.tlab.voidDupe;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class ItemCheckUtil {

    private static final Map<Enchantment, Integer> ILLEGAL_ENCHANTMENTS;

    static {
        Map<Enchantment, Integer> map = new HashMap<>();
        put(map, "sharpness", 6);
        put(map, "efficiency", 8);
        put(map, "unbreaking", 4);
        put(map, "protection", 6);
        put(map, "blast_protection", 6);
        put(map, "depth_strider", 4);
        put(map, "density", 6);
        put(map, "breach", 5);
        ILLEGAL_ENCHANTMENTS = Collections.unmodifiableMap(map);
    }

    private static void put(Map<Enchantment, Integer> map, String key, int level) {
        Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key));
        if (enchant != null) {
            map.put(enchant, level);
        }
    }

    private static final Set<Material> DYES = Arrays.stream(Material.values())
            .filter(m -> m.name().endsWith("_DYE"))
            .collect(Collectors.toSet());

    public static boolean hasIllegalEnchantments(ItemStack item) {
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
        return DYES.contains(item.getType()) && unbreaking != null &&
                enchants.getOrDefault(unbreaking, 0) == 1;
    }
}
