package com.totembed;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotemBedMod implements ModInitializer {
    public static final String MOD_ID = "totembed";
    public static final Logger LOGGER = LoggerFactory.getLogger("TotemBed");

    @Override
    public void onInitialize() {
        com.totembed.data.TotemBedComponents.register();
        TotemStasisEnchantment.registerKeyOnly();
        BedBindingHandler.register();
        BedReturnManager.register();
        TotemTooltip.register();

        // Server-authoritative binding; vanilla bed behavior otherwise untouched.
        UseBlockCallback.EVENT.register(BedBindingHandler::onUseBlock);

        LOGGER.info("[TotemBed] initialized: Totem Stasis ready.");
    }
}
