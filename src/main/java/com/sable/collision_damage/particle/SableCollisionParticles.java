package com.sable.collision_damage.particle;

import com.sable.collision_damage.SableCollisionDamage;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SableCollisionParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, SableCollisionDamage.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UCK_1 = PARTICLES.register("uck_1", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UCK_2 = PARTICLES.register("uck_2", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UCK_3 = PARTICLES.register("uck_3", () -> new SimpleParticleType(true));
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> UCK_4 = PARTICLES.register("uck_4", () -> new SimpleParticleType(true));

    private SableCollisionParticles() {
    }
}
