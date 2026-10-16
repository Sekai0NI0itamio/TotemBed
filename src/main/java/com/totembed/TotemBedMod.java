package com.totembed;

import com.totembed.data.TotemBedComponents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotemBedMod implements ModInitializer {
    public static final String MOD_ID = "totembed";
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemBed");

    @Override
    public void onInitialize() {
        TotemBedComponents.register();
        TotemStasisEnchantment.registerKeyOnly();
        BedBindingHandler.register();
        BedReturnManager.register();
        TotemTooltip.register();

        // Ensure totems spawned without components still serialize fine; no default needed.
        // Keep the enchantment visible only through binding (not enchanting table/loot).
        UseBlockCallback.EVENT.register(BedBindingHandler::onUseBlock);

        LOGGER.info("[TotemBed] initialized: Totem Stasis ready.");
    }
}
