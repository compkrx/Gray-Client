package com.grayclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;

/** Saves settings only; active modules are deliberately not restored between sessions. */
public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("grayclient.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : Modules.ALL) {
                JsonObject o = new JsonObject();
                for (Setting<?> s : m.settings) o.add(s.name, s.toJson());
                root.add(m.name, o);
            }
            Path target = file();
            Files.createDirectories(target.getParent());
            Path temporary = Files.createTempFile(target.getParent(), "grayclient-", ".tmp");
            try {
                Files.writeString(temporary, GSON.toJson(root));
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (Exception e) {
            System.err.println("[Gray Client] could not save config: " + e);
        }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) f = f.resolveSibling("farmclient.json");
            if (!Files.exists(f)) return;
            JsonObject root = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            for (Module m : Modules.ALL) {
                if (!root.has(m.name) || !root.get(m.name).isJsonObject()) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                for (Setting<?> s : m.settings) {
                    try { if (o.has(s.name)) s.fromJson(o.get(s.name)); } catch (RuntimeException ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("[Gray Client] could not load config: " + e);
        }
    }
}
