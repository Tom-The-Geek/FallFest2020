package me.geek.tom.fallfest.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import me.geek.tom.fallfest.FallFest;
import me.geek.tom.fallfest.structure.DungeonStructure;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.storage.LevelStorage;
import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addStructuresToBiomes(Thread thread, DynamicRegistryManager.Impl impl,
                                              LevelStorage.Session session, SaveProperties saveProperties,
                                              ResourcePackManager resourcePackManager, Proxy proxy, DataFixer dataFixer,
                                              ServerResourceManager serverResourceManager,
                                              MinecraftSessionService minecraftSessionService,
                                              GameProfileRepository gameProfileRepository, UserCache userCache,
                                              WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory,
                                              CallbackInfo ci) {
        FallFest.log(Level.INFO, "Inject structures!");

        if (impl.getOptional(Registry.BIOME_KEY).isPresent()) {
            MutableRegistry<Biome> reg = impl.getOptional(Registry.BIOME_KEY).get();
            for (Biome biome : reg) {
                Identifier id = reg.getId(biome);
                if (id == null) continue; // WTF

                if (!DungeonStructure.BLACKLIST.contains(biome.getCategory())) {
                    FallFest.LOGGER.info("inject into biome: " + id);
                    FallFest.addStructureToBiome(biome, FallFest.CONFIGURED_DUNGEON);
                }
            }
        }
    }
}
