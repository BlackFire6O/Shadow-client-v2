package dev.shadow.client;

import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/** User settings persisted in .minecraft/config/shadowclient.properties. */
public final class ShadowConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("shadowclient.properties");
    public static boolean aura = false, trigger = false;
    public static boolean esp = false, boxes2d = true, tracers = false, health = true, nameTags = true, targetEsp = true;
    public static boolean trails = false, hud = true, middleClickPearl = false, projectileAim = false;
    public static boolean chams = false, radar = true, autoSprint = false, fullbright = false;
    public static boolean itemEsp = false, armorHud = true, autoTool = false, autoTotem = false, chestStealer = false;
    public static boolean performanceMode = false, compactHud = true;
    // 0=Balanced, 1=FPS Boost, 2=Quality
    public static int performanceProfile = 0;
    // 0=Purple, 1=Cyan, 2=Red, 3=Green
    public static int espColor = 0;
    public static double auraRange = 4.0;
    public static int attackDelayTicks = 10;

    private ShadowConfig() {}

    public static void load() {
        Properties p = new Properties();
        if (FILE.toFile().exists()) {
            try (InputStream in = new FileInputStream(FILE.toFile())) { p.load(in); }
            catch (IOException ignored) {}
        }
        aura = Boolean.parseBoolean(p.getProperty("combat.aura", "false"));
        trigger = Boolean.parseBoolean(p.getProperty("combat.trigger", "false"));
        esp = Boolean.parseBoolean(p.getProperty("visuals.esp", "false"));
        boxes2d = Boolean.parseBoolean(p.getProperty("visuals.boxes2d", "true"));
        tracers = Boolean.parseBoolean(p.getProperty("visuals.tracers", "false"));
        health = Boolean.parseBoolean(p.getProperty("visuals.health", "true"));
        nameTags = Boolean.parseBoolean(p.getProperty("visuals.nametags", "true"));
        targetEsp = Boolean.parseBoolean(p.getProperty("visuals.target_esp", "true"));
        chams = Boolean.parseBoolean(p.getProperty("visuals.chams", "false"));
        radar = Boolean.parseBoolean(p.getProperty("hud.radar", "true"));
        autoSprint = Boolean.parseBoolean(p.getProperty("utility.auto_sprint", "false"));
        fullbright = Boolean.parseBoolean(p.getProperty("utility.fullbright", "false"));
        itemEsp = Boolean.parseBoolean(p.getProperty("visuals.item_esp", "false"));
        armorHud = Boolean.parseBoolean(p.getProperty("hud.armor", "true"));
        autoTool = Boolean.parseBoolean(p.getProperty("utility.auto_tool", "false"));
        autoTotem = Boolean.parseBoolean(p.getProperty("utility.auto_totem", "false"));
        chestStealer = Boolean.parseBoolean(p.getProperty("utility.chest_stealer", "false"));
        trails = Boolean.parseBoolean(p.getProperty("visuals.trails", "false"));
        hud = Boolean.parseBoolean(p.getProperty("visuals.hud", "true"));
        middleClickPearl = Boolean.parseBoolean(p.getProperty("utility.middle_click_pearl", "false"));
        projectileAim = Boolean.parseBoolean(p.getProperty("combat.projectile_aim", "false"));
        performanceMode = Boolean.parseBoolean(p.getProperty("optimization.performance_mode", "false"));
        compactHud = Boolean.parseBoolean(p.getProperty("hud.compact", "true"));
        performanceProfile = (int) number(p.getProperty("optimization.profile"), 0, 0, 2);
        espColor = (int) number(p.getProperty("visuals.esp_color"), 0, 0, 3);
        auraRange = number(p.getProperty("combat.range"), 4.0, 2.0, 6.0);
        attackDelayTicks = (int) number(p.getProperty("combat.delay"), 10, 2, 20);
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("combat.aura", Boolean.toString(aura));
        p.setProperty("combat.trigger", Boolean.toString(trigger));
        p.setProperty("combat.range", Double.toString(auraRange));
        p.setProperty("combat.delay", Integer.toString(attackDelayTicks));
        p.setProperty("visuals.esp", Boolean.toString(esp));
        p.setProperty("visuals.boxes2d", Boolean.toString(boxes2d));
        p.setProperty("visuals.tracers", Boolean.toString(tracers));
        p.setProperty("visuals.health", Boolean.toString(health));
        p.setProperty("visuals.nametags", Boolean.toString(nameTags));
        p.setProperty("visuals.target_esp", Boolean.toString(targetEsp));
        p.setProperty("visuals.chams", Boolean.toString(chams));
        p.setProperty("hud.radar", Boolean.toString(radar));
        p.setProperty("utility.auto_sprint", Boolean.toString(autoSprint));
        p.setProperty("utility.fullbright", Boolean.toString(fullbright));
        p.setProperty("visuals.item_esp", Boolean.toString(itemEsp));
        p.setProperty("hud.armor", Boolean.toString(armorHud));
        p.setProperty("utility.auto_tool", Boolean.toString(autoTool));
        p.setProperty("utility.auto_totem", Boolean.toString(autoTotem));
        p.setProperty("utility.chest_stealer", Boolean.toString(chestStealer));
        p.setProperty("visuals.trails", Boolean.toString(trails));
        p.setProperty("visuals.hud", Boolean.toString(hud));
        p.setProperty("utility.middle_click_pearl", Boolean.toString(middleClickPearl));
        p.setProperty("combat.projectile_aim", Boolean.toString(projectileAim));
        p.setProperty("optimization.performance_mode", Boolean.toString(performanceMode));
        p.setProperty("hud.compact", Boolean.toString(compactHud));
        p.setProperty("optimization.profile", Integer.toString(performanceProfile));
        p.setProperty("visuals.esp_color", Integer.toString(espColor));
        try {
            FILE.toFile().getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(FILE.toFile())) {
                p.store(out, "Shadow Client 2.0 settings");
            }
        } catch (IOException ignored) {}
    }


    /** Saves the current settings as a named profile under config/shadowclient/profiles. */
    public static boolean saveProfile(String name) {
        if (!validProfileName(name)) return false;
        save();
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("shadowclient").resolve("profiles");
        Path target = dir.resolve(name + ".properties");
        try {
            Files.createDirectories(dir);
            Files.copy(FILE, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    /** Loads a named profile and immediately applies it to the live settings. */
    public static boolean loadProfile(String name) {
        if (!validProfileName(name)) return false;
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("shadowclient").resolve("profiles");
        Path source = dir.resolve(name + ".properties");
        if (!Files.isRegularFile(source)) return false;
        try {
            Files.copy(source, FILE, StandardCopyOption.REPLACE_EXISTING);
            load();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean validProfileName(String name) {
        return name != null && name.matches("[A-Za-z0-9_-]{1,24}");
    }

    private static double number(String value, double fallback, double min, double max) {
        try { return Math.max(min, Math.min(max, Double.parseDouble(value))); }
        catch (Exception ignored) { return fallback; }
    }
}
