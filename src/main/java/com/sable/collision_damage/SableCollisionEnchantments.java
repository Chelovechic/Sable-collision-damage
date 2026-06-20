package com.sable.collision_damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public final class SableCollisionEnchantments {
    public static final ResourceKey<Enchantment> DUROMETER = ResourceKey.create(
            Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(SableCollisionDamage.MODID, "durometer")
    );

    private SableCollisionEnchantments() {
    }
}
