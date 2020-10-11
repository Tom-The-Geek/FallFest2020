package me.geek.tom.fallfest.dump;

import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;

import java.io.File;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class DumpCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("ttg_fallfest_dump")
                        .requires(s -> s.hasPermissionLevel(4))
                        .then(literal("biomes").then(argument("file_prefix", string()).executes(ctx -> {
                            ServerWorld world = ctx.getSource().getWorld();
                            DynamicRegistryManager registryManager = world.getRegistryManager();
                            MutableRegistry<Biome> reg = registryManager.get(Registry.BIOME_KEY);
                            File output = RegistryDumper.dumpRegistry(
                                    new File(world.getServer().getRunDirectory(), "regdump"),
                                    getString(ctx, "file_prefix"), reg, Biome.CODEC,
                                    msg -> ctx.getSource().sendFeedback(new LiteralText(msg), false)
                            );

                            if (output == null) {
                                ctx.getSource().sendError(new LiteralText("Failed to dump!"));
                            } else {
                                ctx.getSource().sendFeedback(new LiteralText("Dumped biome registry to: " + output), false);
                            }

                            return 0;
                        })))
                )
        );
    }
}
