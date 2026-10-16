package com.totembed;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

/**
 * Totem Stasis custom enchantment (max level I).
 * The enchantment itself is data-driven (see data/totembed/enchantment/totem_stasis.json
 * and the datagen provider). This class holds the registry key and helpers that work
 * against the 1.21.1 enchantment/component APIs.
 */
public final class TotemStasisEnchantment {
    public static final Identifier ID = Identifier.of(TotemBedMod.MOD_ID, "totem_stasis");
    public static final RegistryKey<Enchantment> KEY = RegistryKey.of(RegistryKeys.ENCHANTMENT, ID);
    public static final int MAX_LEVEL = 1;

    private TotemStasisEnchantment() {}

    /** No runtime Registry.register call: 1.21.1 enchantments live in the data registry. */
    public static void registerKeyOnly() {
        TotemBedMod.LOGGER.info("[TotemBed] using data-driven enchantment {}", ID);
    }

    public static RegistryEntry<Enchantment> entry(DynamicRegistryManager drm) {
        Registry<Enchantment> enchants = drm.get(RegistryKeys.ENCHANTMENT);
        return enchants.entryOf(KEY);
    }

    public static boolean hasStasis(ItemStack stack, DynamicRegistryManager drm) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            return net.minecraft.enchantment.EnchantmentHelper.getLevel(entry(drm), stack) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static void applyStasis(ItemStack stack, DynamicRegistryManager drm) {
        stack.addEnchantment(entry(drm), MAX_LEVEL);
    }
}
