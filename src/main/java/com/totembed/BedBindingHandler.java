package com.totembed;

import com.totembed.data.TotemBedComponents;
import com.totembed.data.TotemStasisData;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Right-click a bed while holding a Totem of Undying to bind that single stack.
 * Server-authoritative, runs exactly once (server side only).
 */
public final class BedBindingHandler {
    private BedBindingHandler() {}

    public static void register() {
        // Registration itself happens in TotemBedMod via UseBlockCallback.EVENT.
    }

    public static ActionResult onUseBlock(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;

        ItemStack stack = player.getStackInHand(hand);
        if (stack == null || stack.isEmpty() || !stack.isOf(Items.TOTEM_OF_UNDYING)) {
            return ActionResult.PASS; // not holding a totem: preserve vanilla bed behavior
        }

        BlockPos clicked = hit.getBlockPos();
        BlockState state = world.getBlockState(clicked);
        if (!(state.getBlock() instanceof BedBlock)) {
            return ActionResult.PASS; // non-bed block: do nothing special
        }

        BlockPos canonical = resolveCanonicalBedPos(state, clicked);
        if (canonical == null) return ActionResult.PASS;
        if (!isCompleteBed(world, state, clicked, canonical)) return ActionResult.PASS;

        String dimId = world.getRegistryKey().getValue().toString();
        TotemStasisData data = new TotemStasisData(dimId, canonical.toImmutable());
        if (!data.isWellFormed()) return ActionResult.PASS;

        // Store data on THIS stack only; never touch the rest of the inventory.
        stack.set(TotemBedComponents.TOTEM_STASIS, data);
        try {
            TotemStasisEnchantment.applyStasis(stack, world.getRegistryManager());
        } catch (Exception e) {
            // Enchantment registry entry missing (e.g. datapack stripped it):
            // keep the data component but warn; binding still recorded.
            TotemBedMod.LOGGER.warn("[TotemBed] bound data without enchantment entry: {}", e.toString());
        }

        ServerWorld sw = (ServerWorld) world;
        sw.playSound(null, canonical, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE,
            SoundCategory.BLOCKS, 1.0f, 1.2f);
        sw.spawnParticles(ParticleTypes.ENCHANT,
            canonical.getX() + 0.5, canonical.getY() + 1.1, canonical.getZ() + 0.5,
            24, 0.4, 0.4, 0.4, 0.6);

        serverPlayer.sendMessage(
            Text.translatable("message.totembed.bound",
                canonical.getX(), canonical.getY(), canonical.getZ(), dimId),
            true);
        TotemBedMod.LOGGER.info("[TotemBed] bound totem for {} to {} @ {}",
            serverPlayer.getGameProfile().getName(), dimId, canonical);

        // SUCCESS consumes the interaction so vanilla bed GUI/sleep does not also fire
        // for this binding click. Normal bed use (no totem) still returns PASS above.
        return ActionResult.SUCCESS;
    }

    /** Resolve the head-half position regardless of which half was clicked. */
    public static BlockPos resolveCanonicalBedPos(BlockState state, BlockPos clicked) {
        try {
            if (!(state.getBlock() instanceof BedBlock)) return null;
            return resolveHeadPos(state.get(BedBlock.PART), state.get(BedBlock.FACING), clicked);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pure head-resolution math (no block lookups): testable without a
     * bootstrapped registry.
     */
    public static BlockPos resolveHeadPos(BedPart part, Direction facing, BlockPos clicked) {
        if (part == null || facing == null || clicked == null) return null;
        if (part == BedPart.HEAD) return clicked;
        return clicked.offset(facing); // foot -> head
    }

    /** Both halves must exist and belong to the same bed. */
    public static boolean isCompleteBed(World world, BlockState clickedState, BlockPos clicked, BlockPos head) {
        try {
            BlockState headState = world.getBlockState(head);
            if (!(headState.getBlock() instanceof BedBlock)) return false;
            if (headState.get(BedBlock.PART) != BedPart.HEAD) return false;
            Direction facing = headState.get(BedBlock.FACING);
            BlockPos foot = head.offset(facing.getOpposite());
            BlockState footState = world.getBlockState(foot);
            if (!(footState.getBlock() instanceof BedBlock)) return false;
            if (footState.get(BedBlock.PART) != BedPart.FOOT) return false;
            return footState.get(BedBlock.FACING) == facing;
        } catch (Exception e) {
            return false;
        }
    }
}
