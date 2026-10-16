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
    private TotemStasisData totembed$pending;

    @Inject(method = "tryUseTotem", at = @At("HEAD"))
    private void totembed$captureBinding(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        totembed$pending = null;
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity)) return;
        try {
            // Match vanilla hand order (main first) so we capture the stack
            // vanilla will actually consume.
            for (Hand hand : Hand.values()) {
                ItemStack stack = self.getStackInHand(hand);
                if (stack == null || stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) continue;
                if (TotemBedComponents.TOTEM_STASIS == null) break;
                TotemStasisData data = stack.get(TotemBedComponents.TOTEM_STASIS);
                if (data != null && data.isWellFormed()) {
                    totembed$pending = new TotemStasisData(data.dimension(), data.pos().toImmutable());
                    break;
                }
                // First totem found without binding: vanilla will use an unbound
                // totem, so there is nothing to schedule.
                break;
            }
        } catch (Exception e) {
            totembed$pending = null;
        }
    }

    @Inject(method = "tryUseTotem", at = @At("RETURN"))
    private void totembed$scheduleReturn(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        TotemStasisData pending = totembed$pending;
        totembed$pending = null;
        try {
            if (pending == null) return;
            if (!Boolean.TRUE.equals(cir.getReturnValue())) return;
            LivingEntity self = (LivingEntity) (Object) this;
            if (!(self instanceof ServerPlayerEntity player)) return;
            if (player.isDead()) return;
            BedReturnManager.schedule(player, pending);
        } catch (Exception e) {
            TotemBedMod.LOGGER.warn("[TotemBed] totem mixin post-hook failed safely: {}", e.toString());
        }
    }
}
