package com.sable.collision_damage;

import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class DurometerHandler {
    private DurometerHandler() {
    }

    public static void onRightClickBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof final ServerPlayer player) || !(event.getLevel() instanceof final ServerLevel level)) {
            return;
        }

        final ItemStack heldStack = event.getItemStack();
        if (heldStack.isEmpty()) {
            return;
        }

        final Holder<Enchantment> durometer = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(SableCollisionEnchantments.DUROMETER);
        final ItemEnchantments enchantments = heldStack.getAllEnchantments(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT));
        if (enchantments.getLevel(durometer) <= 0) {
            return;
        }

        final BlockPos blockPos = event.getPos();
        final BlockState state = level.getBlockState(blockPos);
        if (state.isAir()) {
            return;
        }

        final SablePreSolverDamage.BlockDiagnostics diagnostics = SablePreSolverDamage.inspectBlock(level, blockPos, state);
        player.sendSystemMessage(buildMessage(state, diagnostics));
    }

    private static Component buildMessage(final BlockState state, final SablePreSolverDamage.BlockDiagnostics diagnostics) {
        final Object destroySpeed = diagnostics.destroySpeed() < 0.0F
                ? Component.translatable("message.sablecollisiondamage.durometer.value.unbreakable")
                : format(diagnostics.destroySpeed());
        final Object breakThreshold = diagnostics.breakThresholdAgainstEqualHardness() < 0.0D
                ? Component.translatable("message.sablecollisiondamage.durometer.value.no_break")
                : format(diagnostics.breakThresholdAgainstEqualHardness());
        final Object slowdown = diagnostics.penetrationSlowdown() < 0.0D
                ? Component.translatable("message.sablecollisiondamage.durometer.value.not_applicable")
                : format(diagnostics.penetrationSlowdown());
        final Object fragile = diagnostics.fragileTag()
                ? Component.translatable("message.sablecollisiondamage.durometer.value.yes")
                : Component.translatable("message.sablecollisiondamage.durometer.value.no");

        MutableComponent message = Component.empty()
                .append(Component.translatable("message.sablecollisiondamage.durometer.header", state.getBlock().getName()).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("\n"))
                .append(Component.translatable("message.sablecollisiondamage.durometer.destroy_speed", destroySpeed).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.translatable("message.sablecollisiondamage.durometer.fragile", fragile).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.translatable("message.sablecollisiondamage.durometer.break_threshold", breakThreshold).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.translatable("message.sablecollisiondamage.durometer.counterpart_factor", format(diagnostics.counterpartHardnessFactor())).withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Component.translatable("message.sablecollisiondamage.durometer.slowdown", slowdown).withStyle(ChatFormatting.GRAY));

        if (diagnostics.persistentLeavesImmune()) {
            message = message
                    .append(Component.literal("\n"))
                    .append(Component.translatable("message.sablecollisiondamage.durometer.persistent_leaves").withStyle(ChatFormatting.DARK_GREEN));
        }

        return message;
    }

    private static String format(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
