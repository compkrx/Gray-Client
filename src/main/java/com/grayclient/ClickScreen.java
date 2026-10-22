package com.grayclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** A deliberately vanilla-style screen: normal Minecraft widgets, with a quiet gray frame. */
public class ClickScreen extends Screen {
    private static Module.Category category;
    private static Module selected;
    private static String query = "";
    private static int page;
    private static int settingsPage;
    private static final int ROW = 22;

    private final List<ButtonWidget> contentButtons = new ArrayList<>();
    private final List<Caption> captions = new ArrayList<>();
    private int panelX, panelY, panelWidth, panelHeight;
    private int listX, listWidth, detailX, detailWidth, contentY, contentBottom, rows, settingsRows;
    private int resultCount;
    private TextFieldWidget search;

    private record Caption(int x, int y, int width, Supplier<String> text) {}

    public ClickScreen() { super(Text.literal("Gray Client")); }

    private ButtonWidget button(int x, int y, int width, String label, ButtonWidget.PressAction action) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label), action)
                .dimensions(x, y, Math.max(20, width), 20).build());
    }

    private ButtonWidget contentButton(int x, int y, int width, String label, ButtonWidget.PressAction action) {
        ButtonWidget button = button(x, y, width, label, action);
        contentButtons.add(button);
        return button;
    }

    private static String displayName(Module module) {
        return module.name.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    @Override
    protected void init() {
        contentButtons.clear();
        captions.clear();
        panelWidth = Math.min(720, width - 16);
        panelHeight = Math.min(420, height - 16);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        listX = panelX + 12;
        listWidth = Math.max(112, (panelWidth - 36) * 43 / 100);
        detailX = listX + listWidth + 12;
        detailWidth = panelX + panelWidth - 12 - detailX;
        contentY = panelY + 88;
        contentBottom = panelY + panelHeight - 56;
        rows = Math.max(1, (contentBottom - contentY) / ROW);
        settingsRows = Math.max(1, (contentBottom - contentY - 48) / ROW);

        search = addDrawableChild(new TextFieldWidget(textRenderer, listX, panelY + 32,
                panelWidth - 24, 20, Text.literal("Search modules")));
        search.setMaxLength(80);
        search.setText(query);
        search.setSuggestion(query.isEmpty() ? "Search modules..." : "");
        search.setChangedListener(value -> {
            query = value;
            search.setSuggestion(value.isEmpty() ? "Search modules..." : "");
            page = 0;
            buildContent();
        });

        int categoryCount = Module.Category.values().length + 1;
        int categoryWidth = (panelWidth - 24 - (categoryCount - 1) * 4) / categoryCount;
        ButtonWidget all = button(listX, panelY + 58, categoryWidth, "All", b -> changeCategory(null));
        all.active = category != null;
        Module.Category[] categories = Module.Category.values();
        for (int i = 0; i < categories.length; i++) {
            Module.Category value = categories[i];
            ButtonWidget tab = button(listX + (i + 1) * (categoryWidth + 4), panelY + 58,
                    categoryWidth, value.label, b -> changeCategory(value));
            tab.active = category != value;
        }
        button(listX, panelY + panelHeight - 26, 96, "Disable all", b -> {
            for (Module module : Modules.ALL) module.setEnabled(false);
            Config.save();
            buildContent();
        });
        button(panelX + panelWidth - 72, panelY + panelHeight - 26, 60, "Done", b -> close());
        buildContent();
    }

    private void changeCategory(Module.Category value) {
        category = value;
        page = 0;
        settingsPage = 0;
        selected = null;
        clearAndInit();
    }

    private void buildContent() {
        for (ButtonWidget button : contentButtons) remove(button);
        contentButtons.clear();
        captions.clear();
        String term = query.strip().toLowerCase(Locale.ROOT);
        List<Module> modules = Modules.ALL.stream()
                .filter(module -> category == null || module.category == category)
                .filter(module -> (module.name + " " + displayName(module) + " " + module.description)
                        .toLowerCase(Locale.ROOT).contains(term))
                .toList();
        resultCount = modules.size();
        int pages = Math.max(1, (modules.size() + rows - 1) / rows);
        page = Math.max(0, Math.min(page, pages - 1));
        if (selected == null || !modules.contains(selected)) {
            selected = modules.isEmpty() ? null : modules.get(page * rows);
            settingsPage = 0;
        }
        for (int i = 0; i < rows && page * rows + i < modules.size(); i++) {
            Module module = modules.get(page * rows + i);
            int y = contentY + i * ROW;
            ButtonWidget toggle = contentButton(listX, y, listWidth - 28, moduleLabel(module), b -> {
                module.toggle();
                Config.save();
                b.setMessage(Text.literal(moduleLabel(module)));
            });
            toggle.setTooltip(Tooltip.of(Text.literal(displayName(module) + "\n" + module.description)));
            contentButton(listX + listWidth - 24, y, 24, module == selected ? "<" : ">", b -> {
                selected = module;
                settingsPage = 0;
                buildContent();
            });
        }
        int navigationY = panelY + panelHeight - 50;
        ButtonWidget previous = contentButton(listX, navigationY, 24, "<", b -> { page--; buildContent(); });
        previous.active = page > 0;
        ButtonWidget next = contentButton(listX + listWidth - 24, navigationY, 24, ">", b -> { page++; buildContent(); });
        next.active = page + 1 < pages;
        captions.add(new Caption(listX + 28, navigationY + 6, listWidth - 56,
                () -> (page + 1) + " / " + pages));

        if (selected == null) return;
        Module module = selected;
        int settingPages = Math.max(1, (module.settings.size() + settingsRows - 1) / settingsRows);
        settingsPage = Math.max(0, Math.min(settingsPage, settingPages - 1));
        for (int i = 0; i < settingsRows && settingsPage * settingsRows + i < module.settings.size(); i++) {
            Setting<?> setting = module.settings.get(settingsPage * settingsRows + i);
            int y = contentY + 48 + i * ROW;
            if (setting instanceof Setting.Bool value) {
                contentButton(detailX, y, detailWidth, value.name + ": " + (value.on() ? "On" : "Off"), b -> {
                    value.toggle();
                    Config.save();
                    b.setMessage(Text.literal(value.name + ": " + (value.on() ? "On" : "Off")));
                });
            } else if (setting instanceof Setting.Num value) {
                contentButton(detailX, y, 22, "-", b -> {
                    value.dec();
                    Config.save();
                });
                contentButton(detailX + detailWidth - 22, y, 22, "+", b -> {
                    value.inc();
                    Config.save();
                });
                captions.add(new Caption(detailX + 26, y + 6, detailWidth - 52,
                        () -> value.name + ": " + value.display()));
            }
        }
        if (settingPages > 1) {
            ButtonWidget back = contentButton(detailX, navigationY, 24, "<", b -> { settingsPage--; buildContent(); });
            back.active = settingsPage > 0;
            ButtonWidget forward = contentButton(detailX + detailWidth - 24, navigationY, 24, ">", b -> { settingsPage++; buildContent(); });
            forward.active = settingsPage + 1 < settingPages;
            captions.add(new Caption(detailX + 28, navigationY + 6, detailWidth - 56,
                    () -> "Settings " + (settingsPage + 1) + " / " + settingPages));
        }
    }

    private String moduleLabel(Module module) {
        String suffix = module.enabled ? "  [On]" : "  [Off]";
        return textRenderer.trimToWidth(displayName(module), Math.max(20, listWidth - 40 - textRenderer.getWidth(suffix))) + suffix;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xB5101113);
        context.fill(panelX - 1, panelY - 1, panelX + panelWidth + 1, panelY + panelHeight + 1, 0xFF73767A);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF0212225);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + 26, 0xFF35373B);
        context.fill(detailX - 7, contentY - 4, detailX - 6, contentBottom, 0xFF515358);
        context.drawTextWithShadow(textRenderer, "GRAY CLIENT", listX, panelY + 9, 0xFFF2F2F2);
        String count = Modules.ALL.stream().filter(module -> module.enabled).count() + " enabled";
        context.drawTextWithShadow(textRenderer, count,
                panelX + panelWidth - 12 - textRenderer.getWidth(count), panelY + 9, 0xFFB7BBC2);
        if (resultCount == 0) {
            context.drawTextWithShadow(textRenderer, "No modules found", listX + 4, contentY + 8, 0xFFB7BBC2);
        }
        if (selected != null) {
            context.drawTextWithShadow(textRenderer, textRenderer.trimToWidth(displayName(selected), detailWidth),
                    detailX, contentY + 2, 0xFFF2F2F2);
            String status = selected.enabled ? "Enabled" : "Disabled";
            context.drawTextWithShadow(textRenderer, status, detailX, contentY + 14, 0xFFB7BBC2);
            List<net.minecraft.text.OrderedText> description = textRenderer.wrapLines(Text.literal(selected.description), detailWidth);
            for (int i = 0; i < Math.min(2, description.size()); i++) {
                context.drawTextWithShadow(textRenderer, description.get(i), detailX, contentY + 26 + i * 10, 0xFF9DA1A8);
            }
            if (selected.settings.isEmpty()) {
                context.drawTextWithShadow(textRenderer, "No settings", detailX, contentY + 50, 0xFF9DA1A8);
            }
        }
        super.render(context, mouseX, mouseY, delta);
        for (Caption caption : captions) {
            String label = textRenderer.trimToWidth(caption.text.get(), Math.max(1, caption.width));
            context.drawCenteredTextWithShadow(textRenderer, label, caption.x + caption.width / 2, caption.y, 0xFFD4D6DA);
        }
    }

    @Override
    public void removed() { Config.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
