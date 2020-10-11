package me.geek.tom.fallfest.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistryV3;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import me.geek.tom.fallfest.component.impl.SpawnerPosComponent;
import net.minecraft.entity.mob.MobEntity;

import static me.geek.tom.fallfest.FallFest.modIdentifier;

public class ModComponents implements EntityComponentInitializer {
    public static final ComponentKey<SpawnerMobComponent> SPAWNER_MOB =
            ComponentRegistryV3.INSTANCE.getOrCreate(modIdentifier("spawner_mob"), SpawnerMobComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        // Use MobEntity as it is equivalent to LivingEntity, but excludes players and armor stands
        registry.registerFor(MobEntity.class, SPAWNER_MOB, e -> new SpawnerPosComponent(null, false));
    }
}
