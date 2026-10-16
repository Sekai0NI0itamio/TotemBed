package com.totembed;

import com.totembed.data.TotemBedComponents;
import com.totembed.data.TotemStasisData;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Post-totem bed-return sequence.
 *
 * Flow: totem mixin captures the activating stack's binding at damage time,
 * then schedules this manager 1 server tick later (safe point, outside the
 * damage callback). We validate everything, teleport across dimensions if
 * needed, attempt vanilla trySleep + wakeUp, and let vanilla choose the exit
 * position via BedBlock.findWakeUpPosition. Fallback uses the same vanilla
 * exit calculation; we never hardcode an east-side offset.
 */
public final class BedReturnManager {
    private BedReturnManager() {}

    private record Pending(ServerPlayerEntity player, TotemStasisData data, int delay) {}

    private static final Queue<Pending> QUEUE = new ArrayDeque<>();
    private static final Set<UUID> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    /** Guards against the same activation scheduling twice. */
    private static final Map<UUID, Long> LAST_SCHEDULE_TICK = new ConcurrentHashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BedReturnManager::onEndTick);
    }

    public static void schedule(ServerPlayerEntity player, TotemStasisData data) {
        if (player == null || data == null || !data.isWellFormed()) return;
        MinecraftServer server = player.getServer();
        if (server == null) return;
        long tick = server.getTicks();
        Long last = LAST_SCHEDULE_TICK.get(player.getUuid());
        if (last != null && tick - last < 20) {
            TotemBedMod.LOGGER.info("[TotemBed] skipping duplicate schedule for {}", player.getGameProfile().getName());
            return;
        }
        LAST_SCHEDULE_TICK.put(player.getUuid(), tick);
        if (!IN_FLIGHT.add(player.getUuid())) return;
        synchronized (QUEUE) {
            QUEUE.add(new Pending(player, data, 1));
        }
        TotemBedMod.LOGGER.info("[TotemBed] scheduled bed return for {} -> {} @ {}",
            player.getGameProfile().getName(), data.dimension(), data.pos());
    }

    private static void onEndTick(MinecraftServer server) {
        Pending next;
        synchronized (QUEUE) {
            next = QUEUE.poll();
        }
        if (next == null) return;
        if (next.delay > 0) {
            synchronized (QUEUE) {
                QUEUE.add(new Pending(next.player, next.data, next.delay - 1));
            }
            return;
        }
        try {
            execute(server, next.player, next.data);
        } catch (Exception e) {
            TotemBedMod.LOGGER.error("[TotemBed] bed return failed safely: {}", e.toString());
            try {
                if (next.player != null && !next.player.isRemoved()) {
                    next.player.sendMessage(Text.translatable("message.totembed.return_failed"), true);
                }
            } catch (Exception ignored) {}
        } finally {
            if (next.player != null) IN_FLIGHT.remove(next.player.getUuid());
        }
    }

    static void execute(MinecraftServer server, ServerPlayerEntity player, TotemStasisData data) {
        if (player == null || player.isRemoved()) return;
        if (player.isDead()) {
            TotemBedMod.LOGGER.warn("[TotemBed] player dead at bed-return time; aborting (totem effect preserved).");
            return;
        }

        Identifier dimId = Identifier.tryParse(data.dimension());
        if (dimId == null) {
            TotemBedMod.LOGGER.warn("[TotemBed] malformed dimension '{}'; totem effect preserved.", data.dimension());
            return;
        }
        RegistryKey<World> dimKey = RegistryKey.of(RegistryKeys.WORLD, dimId);
        ServerWorld dest = server.getWorld(dimKey);
        if (dest == null) {
            TotemBedMod.LOGGER.warn("[TotemBed] destination dimension {} unavailable.", dimId);
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }

        BlockPos stored = data.pos();
        if (!dest.isInBuildLimit(stored)) {
            TotemBedMod.LOGGER.warn("[TotemBed] stored pos {} out of bounds.", stored);
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }

        // Ensure the chunk is available before touching the bed.
        try {
            dest.getChunk(stored);
        } catch (Exception e) {
            TotemBedMod.LOGGER.warn("[TotemBed] destination chunk unavailable: {}", e.toString());
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }

        BlockState state = dest.getBlockState(stored);
        if (!(state.getBlock() instanceof BedBlock)) {
            TotemBedMod.LOGGER.info("[TotemBed] no bed at {}; totem effect preserved.", stored);
            player.sendMessage(Text.translatable("message.totembed.bed_gone"), true);
            return;
        }

        // Normalize to the head half and validate the pair.
        BlockPos head = BedBindingHandler.resolveCanonicalBedPos(state, stored);
        if (head == null) {
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }
        BlockState headState = dest.getBlockState(head);
        if (!(headState.getBlock() instanceof BedBlock)
            || headState.get(BedBlock.PART) != BedPart.HEAD) {
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }
        Direction bedDir = headState.get(BedBlock.FACING);
        BlockPos foot = head.offset(bedDir.getOpposite());
        BlockState footState = dest.getBlockState(foot);
        if (!(footState.getBlock() instanceof BedBlock)) {
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }

        // Nether/End safety: never call anything that can explode a bed.
        if (!BedBlock.isBedWorking(dest)) {
            TotemBedMod.LOGGER.info("[TotemBed] bed in non-sleep dimension {}; using safe exit positioning (no sleep attempt).", dimId);
            Optional<Vec3d> safe = BedBlock.findWakeUpPosition(
                EntityType.PLAYER, dest, head, bedDir, player.getYaw());
            if (safe.isPresent()) {
                movePlayerTo(server, player, dest, safe.get(), bedDir);
                player.sendMessage(Text.translatable("message.totembed.returned_safe"), true);
            } else {
                player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            }
            return;
        }

        // Cross-dimension travel if needed.
        if (player.getWorld() != dest) {
            movePlayerTo(server, player, dest,
                new Vec3d(head.getX() + 0.5, head.getY(), head.getZ() + 0.5), bedDir);
        }

        // Vanilla exit position, computed by vanilla from orientation + surroundings.
        Optional<Vec3d> vanillaExit = BedBlock.findWakeUpPosition(
            EntityType.PLAYER, player.getWorld(), head, bedDir, player.getYaw());

        // Primary path: real vanilla sleep + wake.
        boolean slept = false;
        try {
            var result = player.trySleep(head);
            slept = result.isRight();
            if (!slept) {
                TotemBedMod.LOGGER.info("[TotemBed] trySleep rejected: {}",
                    result.left().map(r -> r.toString()).orElse("unknown"));
            }
        } catch (Exception e) {
            TotemBedMod.LOGGER.warn("[TotemBed] trySleep threw safely: {}", e.toString());
            slept = false;
        }

        try {
            if (player.isSleeping()) {
                player.wakeUp(false, true);
            }
        } catch (Exception e) {
            TotemBedMod.LOGGER.warn("[TotemBed] wakeUp threw safely: {}", e.toString());
            try { player.stopSleeping(); } catch (Exception ignored) {}
        }

        // Let vanilla's choice win: move to the vanilla exit if we have one and
        // the player is not already standing there awake.
        if (vanillaExit.isPresent()) {
            Vec3d exit = vanillaExit.get();
            double dist = player.getPos().distanceTo(exit);
            if (player.isSleeping() || dist > 0.6) {
                movePlayerTo(server, player, (ServerWorld) player.getWorld(), exit, bedDir);
            }
        } else if (!slept) {
            TotemBedMod.LOGGER.info("[TotemBed] no vanilla exit and sleep rejected; staying put (totem effect preserved).");
            player.sendMessage(Text.translatable("message.totembed.invalid_bed"), true);
            return;
        }

        // Final safety: never leave the player sleeping.
        if (player.isSleeping()) {
            try { player.wakeUp(true, true); } catch (Exception ignored) {}
            try { if (player.isSleeping()) player.stopSleeping(); } catch (Exception ignored) {}
        }

        TotemBedMod.LOGGER.info("[TotemBed] bed return complete for {} at {}",
            player.getGameProfile().getName(), player.getBlockPos());
        player.sendMessage(Text.translatable("message.totembed.returned"), true);
    }

    private static void movePlayerTo(MinecraftServer server, ServerPlayerEntity player,
                                     ServerWorld world, Vec3d pos, Direction bedDir) {
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        if (player.getWorld() != world) {
            player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
        } else {
            player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch);
        }
    }

    /** Pure helper for unit tests: dimension ids that we accept. */
    public static boolean isAcceptableDimensionId(String id) {
        if (id == null) return false;
        TotemStasisData probe;
        try {
            probe = new TotemStasisData(id, new BlockPos(0, 64, 0));
        } catch (Exception e) {
            return false;
        }
        return probe.isWellFormed();
    }
}
