package com.totembed.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

/**
 * Bound-bed data stored on a single totem ItemStack.
 * Dimension is stored as a string identifier (e.g. "minecraft:overworld")
 * so beds in different dimensions are never confused.
 */
public record TotemStasisData(String dimension, BlockPos pos) {
    public static final Codec<TotemStasisData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("dimension").forGetter(TotemStasisData::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(TotemStasisData::pos)
        ).apply(instance, TotemStasisData::new)
    );

    public static final PacketCodec<RegistryByteBuf, TotemStasisData> PACKET_CODEC =
        PacketCodec.tuple(
            PacketCodecs.STRING, TotemStasisData::dimension,
            BlockPos.PACKET_CODEC, TotemStasisData::pos,
            TotemStasisData::new
        );

    /** Lenient validity check that never throws on malformed data. */
    public boolean isWellFormed() {
        if (dimension == null || dimension.isBlank()) return false;
        if (pos == null) return false;
        // Identifier shape: [namespace:]path with safe chars.
        String id = dimension.trim();
        if (id.length() > 128) return false;
        String[] parts = id.split(":", -1);
        String path = parts.length == 2 ? parts[1] : parts[0];
        if (path.isEmpty()) return false;
        if (parts.length == 1) {
            return path.matches("[a-z0-9._\\-/]+");
        }
        if (parts.length != 2) return false;
        return parts[0].matches("[a-z0-9._\\-]+") && path.matches("[a-z0-9._\\-/]+");
    }
}
