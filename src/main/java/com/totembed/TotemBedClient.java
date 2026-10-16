package com.totembed;

import net.fabricmc.api.ClientModInitializer;

public class TotemBedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Tooltip + translations are handled server+client via shared code.
        // Client presence is optional for gameplay but needed for tooltip text.
    }
}
