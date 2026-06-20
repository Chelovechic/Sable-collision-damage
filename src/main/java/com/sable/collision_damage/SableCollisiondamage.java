package com.sable.collision_damage;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import com.sable.collision_damage.particle.SableCollisionParticles;
import com.sable.collision_damage.particle.SableCollisionNetwork;

@Mod(SableCollisiondamage.MODID)
public final class SableCollisiondamage {
    public static final String MODID = "sablecollisiondamage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SableCollisiondamage(final IEventBus modEventBus, final ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        SableCollisionParticles.PARTICLES.register(modEventBus);
        modEventBus.addListener(SableCollisionNetwork::registerPayloads);
        NeoForge.EVENT_BUS.addListener(SablePreSolverDamage::onPostPhysicsTick);
        NeoForge.EVENT_BUS.addListener(DurometerHandler::onRightClickBlock);
        LOGGER.info("SCD-loaded");
    }
}
