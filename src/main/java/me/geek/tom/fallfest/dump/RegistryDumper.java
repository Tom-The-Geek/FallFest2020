package me.geek.tom.fallfest.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import me.geek.tom.fallfest.FallFest;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.function.Consumer;

public class RegistryDumper {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <T> File dumpRegistry(File dir, String prefix, Registry<T> reg, Codec<T> codec, Consumer<String> logger) {
        File file = new File(dir, prefix + "." + reg.getKey().getValue().getPath() + ".reg.json");
        File parent = file.getParentFile();
        if (!parent.exists() || !parent.isDirectory()) {
            try {
                Files.createDirectories(parent.toPath());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        String msg = "Dumping registry: " + reg.toString() + " to " + file.toString();
        FallFest.log(Level.INFO, msg);
        logger.accept(msg);
        JsonObject output = new JsonObject();

        for (Map.Entry<RegistryKey<T>, T> entry : reg.getEntries()) {
            T t = entry.getValue();
            output.add(entry.getKey().getValue().toString(), codec.encodeStart(JsonOps.INSTANCE, t).get().orThrow());
        }

        try (OutputStream s = new FileOutputStream(file); OutputStreamWriter out = new OutputStreamWriter(s)) {
            out.write(GSON.toJson(output));
        } catch (IOException e) {
            e.printStackTrace();
            logger.accept("Failed to dump registry, check server output for more info: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return file;
    }
}
