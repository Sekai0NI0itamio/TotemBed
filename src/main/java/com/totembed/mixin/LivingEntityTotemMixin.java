package com.totembed.mixin;

import com.totembed.BedReturnManager;
import com.totembed.TotemBedMod;
import com.totembed.data.TotemBedComponents;
import com.totembed.data.TotemStasisData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Capture the activating totem's binding BEFORE vanilla consumes it (HEAD),
 * then schedule the bed return AFTER vanilla survival effects (RETURN true).
 * We never replace vanilla activation; we only observe it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityTotemMixin {
    @Unique
    private ItemStack totembed$mainSnap;
    @Unique
    private ItemStack totembed$offSnap;

    @Inject(method = "tryUseTotem", at = @At("HEAD"))
    private void totembed$captureBinding(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        totembed$mainSnap = null;
        totembed$offSnap = null;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity)) return;
        try {
            totembed$mainSnap = self.getStackInHand(Hand.MAIN_HAND).copy();
            totembed$offSnap = self.getStackInHand(Hand.OFF_HAND).copy();
        } catch (Exception e) {
            totembed$mainSnap = null;
            totembed$offSnap = null;
        }
    }

    @Inject(method = "tryUseTotem", at = @At("RETURN"))
    private void totembed$scheduleReturn(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        ItemStack mainSnap = totembed$mainSnap;
        ItemStack offSnap = totembed$offSnap;
        totembed$mainSnap = null;
        totembed$offSnap = null;
        try {
            if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof ServerPlayerEntity player)) return;
            if (player.isDead()) return;

            TotemStasisData data = pickActivatingBinding(self, mainSnap, offSnap);
            if (data == null) return; // unbound totem: vanilla behavior only
            BedReturnManager.schedule(player, data);
        } catch (Exception e) {
            TotemBedMod.LOGGER.warn("[TotemBed] totem mixin post-hook failed safely: {}", e.toString());
        }
    }

    /**
     * Figure out which hand vanilla actually consumed: the snapshot whose
     * stack shrank (or vanished). Falls back to main-first bound data
     * (matching vanilla's Hand.values() order) when nothing shrank
     * (e.g. creative mode).
     */
    @Unique
    private static TotemStasisData pickActivatingBinding(LivingEntity self,
                                                         ItemStack mainSnap, ItemStack offSnap) {
        ItemStack mainNow = self.getStackInHand(Hand.MAIN_HAND);
        ItemStack offNow = self.getStackInHand(Hand.OFF_HAND);

        TotemStasisData mainData = boundData(mainSnap);
        TotemStasisData offData = boundData(offSnap);

        boolean mainShrank = shrank(mainSnap, mainNow);
        boolean offShrank = shrank(offSnap, offNow);
        if (mainShrank && mainData != null) return mainData;
        if (offShrank && offData != null) return offData;
        // No detectable consumption (creative) or only one side bound:
        // mirror vanilla's main-first preference.
        if (mainData != null) return mainData;
        return offData;
    }

    @Unique
    private static boolean shrank(ItemStack before, ItemStack after) {
        try {
            if (before == null || before.isEmpty() || !before.isOf(Items.TOTEM_OF_UNDYING)) return false;
            if (after == null || after.isEmpty()) return true;
            if (!after.isOf(Items.TOTEM_OF_UNDYING)) return true;
            return after.getCount() < before.getCount();
        } catch (Exception e) {
            return false;
        }
    }

    @Unique
    private static TotemStasisData boundData(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) return null;
            if (TotemBedComponents.TOTEM_STASIS == null) return null;
            TotemStasisData data = stack.get(TotemBedComponents.TOTEM_STASIS);
            return (data != null && data.isWellFormed()) ? data : null;
        } catch (Exception e) {
            return null;
        }
    }
}
