package com.sable.collision_damage;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class SableCollisionCreativeTabs {
    private SableCollisionCreativeTabs() {
    }

    public static void onBuildContents(final BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) {
            return;
        }

        final Holder<Enchantment> durometer = event.getParameters().holders()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(SableCollisionEnchantments.DUROMETER);
        final ItemStack durometerBook = EnchantedBookItem.createForEnchantment(
                new EnchantmentInstance(durometer, 1)
        );
        event.accept(durometerBook, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
