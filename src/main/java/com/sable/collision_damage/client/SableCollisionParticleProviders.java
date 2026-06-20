package com.sable.collision_damage.client;

import com.sable.collision_damage.particle.SableCollisionParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = "sablecollisiondamage", value = Dist.CLIENT)
public final class SableCollisionParticleProviders {

    private SableCollisionParticleProviders() {
    }

    @SubscribeEvent
    public static void registerProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SableCollisionParticles.UCK_1.get(), UckParticle.Provider::new);
        event.registerSpriteSet(SableCollisionParticles.UCK_2.get(), UckParticle.Provider::new);
        event.registerSpriteSet(SableCollisionParticles.UCK_3.get(), UckParticle.Provider::new);
        event.registerSpriteSet(SableCollisionParticles.UCK_4.get(), UckParticle.Provider::new);
    }
}
