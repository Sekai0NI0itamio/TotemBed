package com.totembed;

import com.totembed.data.TotemBedComponents;
import com.totembed.data.TotemStasisData;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Tooltip: enchantment name is rendered by vanilla; we add the linked
 * dimension + coordinates lines. Crash-proof against malformed data.
 */
public final class TotemTooltip {
    private TotemTooltip() {}

    public static void register() {
        try {
            ItemTooltipCallback.EVENT.register(TotemTooltip::append);
        } catch (Throwable t) {
            // Older/newer Fabric API mismatch: tooltip is cosmetic, never fatal.
            TotemBedMod.LOGGER.warn("[TotemBed] tooltip callback unavailable: {}", t.toString());
        }
    }

    private static void append(ItemStack stack, Item.TooltipContext ctx, TooltipType type,
                               List<Text> lines) {
        appendLines(stack, lines);
    }

    /** Testable core: never throws. */
    public static void appendLines(ItemStack stack, List<Text> lines) {
        try {
            if (stack == null || stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) return;
            if (TotemBedComponents.TOTEM_STASIS == null) return;
            TotemStasisData data = stack.get(TotemBedComponents.TOTEM_STASIS);
            if (data == null || !data.isWellFormed()) return;
            lines.add(Text.translatable("tooltip.totembed.stasis_linked",
                data.dimension(), data.pos().getX(), data.pos().getY(), data.pos().getZ()));
        } catch (Exception ignored) {}
    }
}
