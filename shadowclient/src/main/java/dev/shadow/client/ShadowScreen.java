package dev.shadow.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public final class ShadowScreen extends Screen {
    private enum Page { COMBAT, VISUALS, UTILITY, OPTIMIZE, PROFILES }
    private Page page = Page.COMBAT;
    private float openProgress = 0.0f;
    private int profileSlot = 1;
    private String profileStatus = "Select a slot";
    private int left, top;

    public ShadowScreen() { super(Component.literal("Shadow Client")); }

    @Override
    protected void init() {
        left = this.width / 2 - 140;
        top = this.height / 2 - 112;
        rebuildPage();
    }

    private void rebuildPage() {
        clearWidgets();
        int tabY = top - 28;
        addTab("Combat", Page.COMBAT, left, tabY);
        addTab("Visuals", Page.VISUALS, left + 56, tabY);
        addTab("Utility", Page.UTILITY, left + 112, tabY);
        addTab("Optimize", Page.OPTIMIZE, left + 168, tabY);
        addTab("Profiles", Page.PROFILES, left + 280, tabY);

        int y = top;
        switch (page) {
            case COMBAT -> {
                toggle("KillAura", () -> ShadowConfig.aura, v -> ShadowConfig.aura = v, y); y += 25;
                toggle("TriggerBot", () -> ShadowConfig.trigger, v -> ShadowConfig.trigger = v, y); y += 25;
                cycle("Aura range", () -> String.format("%.1f blocks", ShadowConfig.auraRange),
                    () -> { ShadowConfig.auraRange += .5; if (ShadowConfig.auraRange > 6) ShadowConfig.auraRange = 2; }, y); y += 25;
                cycle("Attack delay", () -> ShadowConfig.attackDelayTicks + " ticks",
                    () -> { ShadowConfig.attackDelayTicks += 2; if (ShadowConfig.attackDelayTicks > 20) ShadowConfig.attackDelayTicks = 2; }, y); y += 25;
                toggle("Trident / crossbow aim", () -> ShadowConfig.projectileAim,
                    v -> ShadowConfig.projectileAim = v, y);
            }
            case VISUALS -> {
                toggle("Player ESP", () -> ShadowConfig.esp, v -> ShadowConfig.esp = v, y); y += 17;
                toggle("Player boxes", () -> ShadowConfig.boxes2d, v -> ShadowConfig.boxes2d = v, y); y += 17;
                toggle("Tracers", () -> ShadowConfig.tracers, v -> ShadowConfig.tracers = v, y); y += 17;
                toggle("Health", () -> ShadowConfig.health, v -> ShadowConfig.health = v, y); y += 17;
                toggle("NameTags", () -> ShadowConfig.nameTags, v -> ShadowConfig.nameTags = v, y); y += 17;
                toggle("Target ESP (aimed entity)", () -> ShadowConfig.targetEsp, v -> ShadowConfig.targetEsp = v, y); y += 17;
                toggle("Trails", () -> ShadowConfig.trails, v -> ShadowConfig.trails = v, y); y += 17;
                toggle("Chams (glowing outline)", () -> ShadowConfig.chams, v -> ShadowConfig.chams = v, y); y += 17;
                toggle("Item ESP", () -> ShadowConfig.itemEsp, v -> ShadowConfig.itemEsp = v, y); y += 17;
                cycle("ESP color", () -> colorName(), () -> ShadowConfig.espColor = (ShadowConfig.espColor + 1) % 4, y);
            }
            case UTILITY -> {
                toggle("HUD", () -> ShadowConfig.hud, v -> ShadowConfig.hud = v, y); y += 18;
                toggle("Compact HUD", () -> ShadowConfig.compactHud, v -> ShadowConfig.compactHud = v, y); y += 18;
                toggle("Player radar", () -> ShadowConfig.radar, v -> ShadowConfig.radar = v, y); y += 18;
                toggle("Middle-click pearl", () -> ShadowConfig.middleClickPearl,
                    v -> ShadowConfig.middleClickPearl = v, y); y += 18;
                toggle("Auto sprint", () -> ShadowConfig.autoSprint, v -> ShadowConfig.autoSprint = v, y); y += 18;
                toggle("Fullbright", () -> ShadowConfig.fullbright, v -> ShadowConfig.fullbright = v, y); y += 18;
                toggle("Auto Tool", () -> ShadowConfig.autoTool, v -> ShadowConfig.autoTool = v, y); y += 18;
                toggle("Auto Totem", () -> ShadowConfig.autoTotem, v -> ShadowConfig.autoTotem = v, y); y += 18;
                toggle("Chest Stealer", () -> ShadowConfig.chestStealer, v -> ShadowConfig.chestStealer = v, y); y += 18;
                toggle("Armor HUD", () -> ShadowConfig.armorHud, v -> ShadowConfig.armorHud = v, y);
            }
            case OPTIMIZE -> {
                cycle("Performance profile", () -> profileName(), () -> {
                    ShadowConfig.performanceProfile = (ShadowConfig.performanceProfile + 1) % 3;
                    ShadowConfig.performanceMode = ShadowConfig.performanceProfile == 1;
                }, y); y += 25;
                addRenderableWidget(Button.builder(Component.literal("Apply profile"), b -> {
                    ShadowClient.applyPerformanceProfileNow(this.minecraft);
                    ShadowConfig.save();
                    b.setMessage(Component.literal("§aProfile applied"));
                }).bounds(left, y, 280, 20).build()); y += 25;
                addRenderableWidget(Button.builder(Component.literal("Restore vanilla defaults"), b -> {
                    ShadowClient.restorePerformanceDefaults(this.minecraft);
                    ShadowConfig.performanceProfile = 0;
                    ShadowConfig.performanceMode = false;
                    ShadowConfig.save();
                    rebuildPage();
                }).bounds(left, y, 280, 20).build());
            }
            case PROFILES -> {
                addRenderableWidget(Button.builder(Component.literal("Profile slot: " + profileSlot), b -> {
                    profileSlot = profileSlot % 5 + 1;
                    b.setMessage(Component.literal("Profile slot: " + profileSlot));
                }).bounds(left, y, 280, 20).build()); y += 28;
                addRenderableWidget(Button.builder(Component.literal("Create / overwrite profile"), b -> {
                    boolean ok = ShadowConfig.saveProfile("profile" + profileSlot);
                    profileStatus = ok ? "Saved profile" + profileSlot : "Save failed";
                    b.setMessage(Component.literal(ok ? "§aSaved profile" + profileSlot : "§cSave failed"));
                }).bounds(left, y, 280, 20).build()); y += 25;
                addRenderableWidget(Button.builder(Component.literal("Load selected profile"), b -> {
                    boolean ok = ShadowConfig.loadProfile("profile" + profileSlot);
                    profileStatus = ok ? "Loaded profile" + profileSlot : "Profile not found";
                    if (ok) {
                        ShadowClient.applyPerformanceProfileNow(this.minecraft);
                        rebuildPage();
                    } else {
                        b.setMessage(Component.literal("§cProfile not found"));
                    }
                }).bounds(left, y, 280, 20).build()); y += 27;
                addRenderableWidget(Button.builder(Component.literal("Save current as profile1"), b -> {
                    boolean ok = ShadowConfig.saveProfile("profile1");
                    profileStatus = ok ? "Saved profile1" : "Save failed";
                    b.setMessage(Component.literal(ok ? "§aSaved profile1" : "§cSave failed"));
                }).bounds(left, y, 280, 20).build()); y += 25;
                addRenderableWidget(Button.builder(Component.literal("Load profile1"), b -> {
                    boolean ok = ShadowConfig.loadProfile("profile1");
                    profileStatus = ok ? "Loaded profile1" : "Profile not found";
                    if (ok) { ShadowClient.applyPerformanceProfileNow(this.minecraft); rebuildPage(); }
                    else b.setMessage(Component.literal("§cProfile not found"));
                }).bounds(left, y, 280, 20).build()); y += 28;
                addRenderableWidget(Button.builder(Component.literal("§7" + profileStatus), b -> {})
                    .bounds(left, y, 280, 20).build());
            }
        }

        addRenderableWidget(Button.builder(Component.literal("Save & close"), b -> {
            ShadowConfig.save();
            this.minecraft.setScreen(null);
        }).bounds(left, top + 178, 280, 20).build());
    }

    private void addTab(String label, Page target, int x, int y) {
        String prefix = page == target ? "§d» " : "§7";
        addRenderableWidget(Button.builder(Component.literal(prefix + label), b -> {
            page = target;
            rebuildPage();
        }).bounds(x, y, 54, 20).build());
    }

    private void toggle(String label, java.util.function.BooleanSupplier get,
                        java.util.function.Consumer<Boolean> set, int y) {
        addRenderableWidget(Button.builder(toggleLabel(label, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(toggleLabel(label, get.getAsBoolean()));
            ShadowConfig.save();
        }).bounds(left, y, 280, 20).build());
    }

    private void cycle(String label, java.util.function.Supplier<String> value, Runnable action, int y) {
        addRenderableWidget(Button.builder(Component.literal("§5" + label + ": §f" + value.get()), b -> {
            action.run();
            b.setMessage(Component.literal("§5" + label + ": §f" + value.get()));
            ShadowConfig.save();
        }).bounds(left, y, 280, 20).build());
    }

    private Component toggleLabel(String label, boolean enabled) {
        return Component.literal("§5" + label + ": " + (enabled ? "§aON" : "§cOFF"));
    }

    private String colorName() {
        return switch (ShadowConfig.espColor) {
            case 1 -> "Cyan";
            case 2 -> "Red";
            case 3 -> "Green";
            default -> "Purple";
        };
    }

    private String profileName() {
        return switch (ShadowConfig.performanceProfile) {
            case 1 -> "FPS Boost";
            case 2 -> "Quality";
            default -> "Balanced";
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        openProgress = Math.min(1.0f, openProgress + delta * 0.12f);
        this.renderBackground(graphics, mouseX, mouseY, delta);
        int slide = (int)((1.0f - openProgress) * 14);
        int x = left - 9;
        int y = top - 8 + slide;
        graphics.fill(x, y, x + 300, y + 205, 0xE8100B19);
        graphics.fill(x, y, x + 300, y + 2, 0xFF9B55FF);
        graphics.drawCenteredString(this.font, Component.literal("§5SHADOW CLIENT §8/ §dCLICKGUI §8v2.0"),
            this.width / 2, top - 43 + slide, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
