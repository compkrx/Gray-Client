package com.grayclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GrayClient implements ClientModInitializer {
    public static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("grayclient", "main"));

    @Override
    public void onInitializeClient() {
        Config.load();
        System.out.println("[Gray Client] Initialized " + Modules.ALL.size() + " modules; Right Shift opens the menu.");

        KeyBinding menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.grayclient.menu", GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        Map<Module, KeyBinding> keys = new LinkedHashMap<>();
        for (Module m : Modules.ALL)
            keys.put(m, KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.grayclient." + m.name, InputUtil.UNKNOWN_KEY.getCode(), CATEGORY)));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
            while (menuKey.wasPressed()) if (mc.currentScreen == null) mc.setScreen(new ClickScreen());
            keys.forEach((m, k) -> {
                while (k.wasPressed()) if (mc.currentScreen == null) m.toggle();
            });
            for (Module m : Modules.ALL) {
                if (!m.enabled) continue;
                if (mc.currentScreen != null && m.category != Module.Category.VISUAL
                        && m != Modules.AUTO_EAT && m != Modules.AUTO_RESPAWN) {
                    // Keep toggles, release temporary input/slots while a menu is open.
                    try { m.onDisable(); } catch (RuntimeException ex) { m.setEnabled(false); }
                    continue;
                }
                try {
                    m.onTick();
                } catch (RuntimeException ex) {
                    System.err.println("[Gray Client] " + m.name + " crashed and was disabled: " + ex);
                    m.setEnabled(false);
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            for (Module module : Modules.ALL) {
                if (module != Modules.HUD) module.setEnabled(false);
            }
            Config.save();
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            for (Module module : Modules.ALL) if (module != Modules.HUD) module.setEnabled(false);
            Config.save();
        });

        HudElementRegistry.addLast(Identifier.of("grayclient", "hud"), (ctx, tickCounter) -> renderHud(ctx));
    }

    private static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Modules.INFO_HUD.render(ctx);
        if (!Modules.HUD.enabled || mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        if (Modules.HUD.watermark.on()) ctx.drawTextWithShadow(tr, "Gray Client", 4, 4, 0xFFD8DADE);
        if (!Modules.HUD.list.on()) return;
        List<String> lines = Modules.ALL.stream()
                .filter(m -> m.enabled && m != Modules.HUD)
                .map(m -> m.name)
                .sorted(Comparator.comparingInt((String s) -> tr.getWidth(s)).reversed())
                .toList();
        int w = ctx.getScaledWindowWidth();
        int y = 4;
        for (String s : lines) {
            ctx.drawTextWithShadow(tr, s, w - tr.getWidth(s) - 4, y, 0xFFFFFFFF);
            y += 10;
        }
    }
}
