package com.totembed.data;

import com.totembed.TotemBedMod;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class TotemBedComponents {
    public static ComponentType<TotemStasisData> TOTEM_STASIS;

    private TotemBedComponents() {}

    public static void register() {
        if (TOTEM_STASIS != null) return;
        TOTEM_STASIS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(TotemBedMod.MOD_ID, "totem_stasis"),
            ComponentType.<TotemStasisData>builder()
                .codec(TotemStasisData.CODEC)
                .packetCodec(TotemStasisData.PACKET_CODEC)
                .build()
        );
        TotemBedMod.LOGGER.info("[TotemBed] registered data component totembed:totem_stasis");
    }
}
