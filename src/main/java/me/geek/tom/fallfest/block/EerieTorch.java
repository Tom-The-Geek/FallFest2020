package me.geek.tom.fallfest.block;

import net.minecraft.block.TorchBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.particle.ParticleEffect;

public class EerieTorch {

    // Skip accesswidener and just use subclasses to expose the protected constructors - quicker and easier.
    public static class EerieTorchBlock extends TorchBlock {
        public EerieTorchBlock(Settings settings, ParticleEffect particle) {
            super(settings, particle);
        }
    }

    public static class EerieWallTorchBlock extends WallTorchBlock {
        public EerieWallTorchBlock(Settings settings, ParticleEffect particleEffect) {
            super(settings, particleEffect);
        }
    }
}
