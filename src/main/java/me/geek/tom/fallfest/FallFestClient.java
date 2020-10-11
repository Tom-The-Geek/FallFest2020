package me.geek.tom.fallfest;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;


public class FallFestClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        FallFest.LOGGER.info("Initialise client!");
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.EERIE_TORCH_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.EERIE_WALL_TORCH_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(Registration.CURSED_SPAWNER_BLOCK, RenderLayer.getCutout());
    }
}
