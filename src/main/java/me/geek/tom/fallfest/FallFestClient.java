package me.geek.tom.fallfest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;


public class FallFestClient implements ClientModInitializer {

    public static final DefaultParticleType EERIE_FLAME_PARTICLE = Registration.modRegister(Registry.PARTICLE_TYPE, FabricParticleTypes.simple(), "eerie_flame");

    @Override
    public void onInitializeClient() {
        FallFest.LOGGER.info("Initialise client!");
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.EERIE_TORCH_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.EERIE_WALL_TORCH_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.CURSED_SPAWNER_BLOCK, RenderLayer.getCutout());

        // Register particles
        ParticleFactoryRegistry.getInstance().register(EERIE_FLAME_PARTICLE, FlameParticle.Factory::new);

        ClientSidePacketRegistry.INSTANCE.register(FallFest.SPAWNER_EFFECT_PACKET_ID, (ctx, data) -> {
            BlockPos pos = data.readBlockPos();
            int effect = data.readInt();
            ctx.getTaskQueue().execute(() -> {

                World world = ctx.getPlayer().getEntityWorld();

                switch (effect) {
                    case 0: // BEGIN CHARGING
                        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS,
                                1.0f, 1.0f, false);
                        for (int i = 0; i < 32; i++) {
                            world.addParticle(ParticleTypes.PORTAL, false,
                                    pos.getX() + 0.5,
                                    pos.getY() + world.random.nextDouble() * 2,
                                    pos.getZ() + 0.5,
                                    world.random.nextGaussian(), 0, world.random.nextGaussian());
                        }
                        break;
                    case 1: // BEGIN
                        break;
                    case 2: // SPAWN MOB
                        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE,
                                1.0f, 1.0f, false);
                        for (int i = 0; i < 32; i++) {
                            world.addParticle(ParticleTypes.PORTAL, false,
                                    pos.getX() + 0.5,
                                    pos.getY() + world.random.nextDouble() * 2,
                                    pos.getZ() + 0.5,
                                    world.random.nextGaussian(), 0, world.random.nextGaussian());
                        }
                        break;
                    case 3: // COMPLETE
                        world.playSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                                SoundCategory.MASTER, 1.0f, 1.0f, false);
                        for (int i = 0; i < 250; i++) {
                            world.addParticle(EERIE_FLAME_PARTICLE, false,
                                    pos.getX() + 0.5,
                                    pos.getY() + 1.5,
                                    pos.getZ() + 0.5,
                                    world.random.nextGaussian(), 0, world.random.nextGaussian());
                        }
                        break;
                }
            });
        });
    }
}
