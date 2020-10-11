package me.geek.tom.fallfest;

import me.geek.tom.fallfest.block.EerieTorch;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.block.TorchBlock;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.WallStandingBlockItem;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static me.geek.tom.fallfest.FallFest.MOD_ID;

@SuppressWarnings("unused")
public class Registration {

    public static final DefaultParticleType EERIE_FLAME_PARTICLE = modRegister(Registry.PARTICLE_TYPE,
            FabricParticleTypes.simple(), "eerie_flame");

    public static final TorchBlock EERIE_TORCH_BLOCK = modRegister(Registry.BLOCK,
            new EerieTorch.EerieTorchBlock(
                    FabricBlockSettings.of(Material.SUPPORTED)
                            .noCollision()
                            .breakInstantly()
                            .luminance(Registration::torchBrightness)
                            .sounds(BlockSoundGroup.WOOD), EERIE_FLAME_PARTICLE),
            "eerie_torch");
    public static final TorchBlock EERIE_WALL_TORCH_BLOCK = modRegister(Registry.BLOCK,
            new EerieTorch.EerieWallTorchBlock(
                    FabricBlockSettings.of(Material.SUPPORTED)
                            .noCollision()
                            .breakInstantly()
                            .luminance(Registration::torchBrightness)
                            .sounds(BlockSoundGroup.WOOD)
                            .dropsLike(EERIE_TORCH_BLOCK), EERIE_FLAME_PARTICLE),
            "eerie_wall_torch");
    public static final WallStandingBlockItem EERIE_TORCH_ITEM = modRegister(Registry.ITEM,
            new WallStandingBlockItem(EERIE_TORCH_BLOCK, EERIE_WALL_TORCH_BLOCK, new FabricItemSettings().group(ItemGroup.DECORATIONS)),
            "eerie_torch");

    public static void init() {
        // Trigger classload - registers stuff.
        ParticleFactoryRegistry.getInstance().register(EERIE_FLAME_PARTICLE, FlameParticle.Factory::new);
        BlockRenderLayerMap.INSTANCE.putBlock(EERIE_TORCH_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(EERIE_WALL_TORCH_BLOCK, RenderLayer.getCutout());
    }

    private static <T> T modRegister(Registry<? super T> reg, T object, String name) {
        return Registry.register(reg, new Identifier(MOD_ID, name), object);
    }

    @SuppressWarnings("SameReturnValue")
    private static int torchBrightness(BlockState state) {
        return 6;
    }
}
