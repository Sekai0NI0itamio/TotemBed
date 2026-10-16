package com.totembed.datagen;

import com.totembed.TotemStasisEnchantment;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;

import java.util.concurrent.CompletableFuture;

public class TotemBedDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        FabricDataGenerator.Pack pack = gen.createPack();
        pack.addProvider(EnchantmentGenerator::new);
        pack.addProvider(TotemBedLangProvider::new);
    }

    public static class EnchantmentGenerator extends FabricDynamicRegistryProvider {
        public EnchantmentGenerator(FabricDataOutput output,
                                    CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            RegistryEntryLookup<Item> items = registries.getWrapperOrThrow(RegistryKeys.ITEM);
            RegistryKey<Item> totemKey = RegistryKey.of(RegistryKeys.ITEM,
                Identifier.ofVanilla("totem_of_undying"));
            // The vanilla totem always resolves; fail loudly if it ever does not.
            RegistryEntryList<Item> supported = RegistryEntryList.of(items.getOrThrow(totemKey));

            entries.add(TotemStasisEnchantment.KEY,
                Enchantment.builder(
                    Enchantment.definition(
                        supported,
                        10, // weight
                        TotemStasisEnchantment.MAX_LEVEL,
                        Enchantment.constantCost(25),
                        Enchantment.constantCost(50),
                        4, // anvil cost
                        AttributeModifierSlot.HAND
                    )
                ).build(TotemStasisEnchantment.KEY.getValue()));
        }

        @Override
        public String getName() {
            return "TotemBedEnchantmentGenerator";
        }
    }
}
