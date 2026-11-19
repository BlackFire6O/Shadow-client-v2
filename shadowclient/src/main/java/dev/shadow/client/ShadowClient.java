package dev.shadow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;

public final class ShadowClient implements ClientModInitializer {
    private static KeyMapping menuKey, auraKey, triggerKey, espKey, pearlKey;
    private static int cooldown;
    private static float trailTick;
    private static double previousGamma = 0.5;
    private static boolean gammaCaptured = false;

    @Override
    public void onInitializeClient() {
        ShadowConfig.load();
        ShadowWorldRenderer.register();
        menuKey = register("menu", GLFW.GLFW_KEY_RIGHT_SHIFT);
        auraKey = register("aura", GLFW.GLFW_KEY_R);
        triggerKey = register("trigger", GLFW.GLFW_KEY_T);
        espKey = register("esp", GLFW.GLFW_KEY_G);
        pearlKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.shadowclient.middle_click_pearl",
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE,
            KeyMapping.Category.MISC
        ));

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            if (ShadowConfig.radar) drawRadar(graphics, mc);
            if (ShadowConfig.armorHud) drawArmorHud(graphics, mc);
            if (!ShadowConfig.hud) return;
            int x = 8, y = 8;
            int height = ShadowConfig.compactHud ? 57 : 70;
            graphics.fill(x - 4, y - 4, x + 154, y + height, 0xB5100B18);
            graphics.drawString(mc.font, Component.literal("§5SHADOW CLIENT §d2.0"), x, y, 0xFFFFFF);
            graphics.drawString(mc.font, Component.literal("§7Aura: " + onOff(ShadowConfig.aura)
                + "  §7Trigger: " + onOff(ShadowConfig.trigger)), x, y + 13, 0xFFFFFF);
            graphics.drawString(mc.font, Component.literal("§7Trails: " + onOff(ShadowConfig.trails)
                + "  §7Aim: " + onOff(ShadowConfig.projectileAim) + "  §7Target: " + onOff(ShadowConfig.targetEsp)), x, y + 26, 0xFFFFFF);
            graphics.drawString(mc.font, Component.literal(String.format("§7XYZ: §f%.1f / %.1f / %.1f",
                mc.player.getX(), mc.player.getY(), mc.player.getZ())), x, y + 39, 0xFFFFFF);
            if (!ShadowConfig.compactHud) {
                graphics.drawString(mc.font, Component.literal("§7Performance: " + onOff(ShadowConfig.performanceMode)), x, y + 52, 0xFFFFFF);
                graphics.drawString(mc.font, Component.literal("§8Right Shift: settings"), x, y + 65, 0xFFFFFF);
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.consumeClick()) client.setScreen(new ShadowScreen());
            while (auraKey.consumeClick()) { ShadowConfig.aura = !ShadowConfig.aura; ShadowConfig.save(); }
            while (triggerKey.consumeClick()) { ShadowConfig.trigger = !ShadowConfig.trigger; ShadowConfig.save(); }
            while (espKey.consumeClick()) { ShadowConfig.esp = !ShadowConfig.esp; ShadowConfig.save(); }

            if (client.player == null || client.level == null) return;

            applyPerformanceProfile(client);
            applyQualityOfLife(client);
            applyAutoTool(client);
            applyChestStealer(client);
            applyAutoTotem(client);

            // Middle-click pearl: selects an ender pearl, uses it, then restores the prior hotbar slot.
            // Uses normal inventory/action packets; server-side rules still apply.
            if (ShadowConfig.middleClickPearl && pearlKey.consumeClick()) {
                int pearlSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (client.player.getInventory().getItem(i).is(Items.ENDER_PEARL)) {
                        pearlSlot = i; break;
                    }
                }
                if (pearlSlot >= 0 && client.gameMode != null) {
                    int oldSlot = client.player.getInventory().getSelectedSlot();
                    client.player.getInventory().setSelectedSlot(pearlSlot);
                    client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    client.player.getInventory().setSelectedSlot(oldSlot);
                }
            }

            // Cosmetic movement trail. This does not alter movement or other players.
            if (ShadowConfig.trails && client.player.tickCount % 2 == 0) {
                Vec3 pos = client.player.position();
                client.level.addParticle(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    pos.x, pos.y + 0.08, pos.z, 0.0, 0.015, 0.0);
            }

            if (ShadowConfig.projectileAim && isHoldingProjectileWeapon(client)) {
                Player target = client.level.getEntitiesOfClass(
                    Player.class, client.player.getBoundingBox().inflate(24.0),
                    p -> p != client.player && p.isAlive()
                ).stream().min(Comparator.comparingDouble(client.player::distanceToSqr)).orElse(null);
                if (target != null) {
                    Vec3 aimAt = target.getEyePosition().subtract(client.player.getEyePosition());
                    double horizontal = Math.sqrt(aimAt.x * aimAt.x + aimAt.z * aimAt.z);
                    float yaw = (float)(Math.toDegrees(Math.atan2(aimAt.z, aimAt.x)) - 90.0);
                    float pitch = (float)-Math.toDegrees(Math.atan2(aimAt.y, horizontal));
                    client.player.setYRot(yaw);
                    client.player.setXRot(pitch);
                }
            }

            if (!ShadowConfig.aura && !ShadowConfig.trigger) return;
            if (cooldown > 0) cooldown--;

            if (ShadowConfig.aura) {
                Player target = client.level.getEntitiesOfClass(
                    Player.class, client.player.getBoundingBox().inflate(ShadowConfig.auraRange),
                    p -> p != client.player && p.isAlive() && !p.isSpectator()
                ).stream().min(Comparator.comparingDouble(client.player::distanceToSqr)).orElse(null);
                if (target != null && client.player.distanceToSqr(target) <= ShadowConfig.auraRange * ShadowConfig.auraRange) {
                    // Gradual camera correction instead of an instantaneous snap.
                    smoothAimAt(client.player, target, 0.28f);
                    if (cooldown == 0) {
                        client.gameMode.attack(client.player, target);
                        client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        cooldown = ShadowConfig.attackDelayTicks + java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 3);
                    }
                }
            }

            if (ShadowConfig.trigger && cooldown == 0
                && client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() instanceof Player target
                && target != client.player && target.isAlive()) {
                client.gameMode.attack(client.player, target);
                client.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                cooldown = ShadowConfig.attackDelayTicks;
            }
        });
    }


    private static void smoothAimAt(Player player, Player target, float smoothing) {
        Vec3 delta = target.getEyePosition().subtract(player.getEyePosition());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float wantedYaw = (float)(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float wantedPitch = (float)-Math.toDegrees(Math.atan2(delta.y, horizontal));
        float yawDiff = net.minecraft.util.Mth.wrapDegrees(wantedYaw - player.getYRot());
        float pitchDiff = wantedPitch - player.getXRot();
        player.setYRot(player.getYRot() + yawDiff * smoothing);
        player.setXRot(net.minecraft.util.Mth.clamp(player.getXRot() + pitchDiff * smoothing, -90.0f, 90.0f));
    }

    private static void applyAutoTool(Minecraft mc) {
        if (!ShadowConfig.autoTool || !(mc.hitResult instanceof BlockHitResult hit) || mc.gameMode == null) return;
        var state = mc.level.getBlockState(hit.getBlockPos());
        if (state.isAir()) return;
        int bestSlot = mc.player.getInventory().getSelectedSlot();
        float bestSpeed = mc.player.getInventory().getItem(bestSlot).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            float speed = mc.player.getInventory().getItem(i).getDestroySpeed(state);
            if (speed > bestSpeed) { bestSpeed = speed; bestSlot = i; }
        }
        mc.player.getInventory().setSelectedSlot(bestSlot);
    }

    private static void applyChestStealer(Minecraft mc) {
        if (!ShadowConfig.chestStealer
            || !(mc.screen instanceof AbstractContainerScreen<?> screen) || mc.player == null || mc.gameMode == null) return;
        var menu = screen.getMenu();
        int playerStart = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < playerStart; i++) {
            var slot = menu.getSlot(i);
            if (slot.hasItem()) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                break; // one transfer per tick to avoid flooding inventory actions
            }
        }
    }

    private static void applyAutoTotem(Minecraft mc) {
        if (!ShadowConfig.autoTotem || mc.player == null || mc.gameMode == null) return;
        // Player inventory menu slot 45 is the offhand; SWAP button 0..8 maps to hotbar slots.
        var inventory = mc.player.getInventory();
        if (mc.player.getItemBySlot(EquipmentSlot.OFFHAND).is(Items.TOTEM_OF_UNDYING)) return;
        for (int i = 0; i < 9; i++) {
            if (inventory.getItem(i).is(Items.TOTEM_OF_UNDYING)) {
                mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, 45, i, ClickType.SWAP, mc.player);
                break;
            }
        }
    }

    private static void drawArmorHud(net.minecraft.client.gui.GuiGraphics g, Minecraft mc) {
        if (mc.player == null) return;
        int x = mc.getWindow().getGuiScaledWidth() / 2 - 90;
        int y = mc.getWindow().getGuiScaledHeight() - 58;
        int index = 0;
        for (var stack : java.util.List.of(
            mc.player.getItemBySlot(EquipmentSlot.FEET),
            mc.player.getItemBySlot(EquipmentSlot.LEGS),
            mc.player.getItemBySlot(EquipmentSlot.CHEST),
            mc.player.getItemBySlot(EquipmentSlot.HEAD))) {
            int px = x + index * 20;
            g.fill(px - 2, y - 2, px + 18, y + 18, 0x880D0914);
            if (!stack.isEmpty()) {
                g.renderItem(stack, px, y);
                if (stack.isDamageableItem()) {
                    int remaining = stack.getMaxDamage() - stack.getDamageValue();
                    int pct = Math.max(0, Math.min(100, remaining * 100 / stack.getMaxDamage()));
                    g.drawString(mc.font, pct + "%", px - 1, y + 19, pct > 50 ? 0xFF75E879 : 0xFFFF6868, false);
                }
            }
            index++;
        }
    }

    private static void applyQualityOfLife(Minecraft mc) {
        if (mc.player == null) return;
        if (ShadowConfig.autoSprint) {
            boolean movingForward = mc.options.keyUp.isDown();
            mc.player.setSprinting(movingForward && !mc.player.isCrouching() && mc.player.getFoodData().getFoodLevel() > 6);
        }
        if (ShadowConfig.fullbright) {
            if (!gammaCaptured) {
                previousGamma = mc.options.gamma().get();
                gammaCaptured = true;
            }
            mc.options.gamma().set(1.0);
        } else if (gammaCaptured) {
            mc.options.gamma().set(previousGamma);
            gammaCaptured = false;
        }
    }

    private static void drawRadar(net.minecraft.client.gui.GuiGraphics g, Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        int cx = mc.getWindow().getGuiScaledWidth() - 54;
        int cy = 53;
        int radius = 38;
        g.fill(cx - 43, cy - 43, cx + 43, cy + 43, 0xA9100B19);
        g.fill(cx - 1, cy - radius, cx + 1, cy + radius, 0x665F526D);
        g.fill(cx - radius, cy - 1, cx + radius, cy + 1, 0x665F526D);
        g.fill(cx - 2, cy - 2, cx + 2, cy + 2, 0xFFFFD65A);
        double yaw = Math.toRadians(mc.player.getYRot());
        double scale = radius / 32.0;
        for (Player other : mc.level.players()) {
            if (other == mc.player || !other.isAlive() || other.isSpectator()) continue;
            double dx = other.getX() - mc.player.getX();
            double dz = other.getZ() - mc.player.getZ();
            double right = dx * Math.cos(yaw) - dz * Math.sin(yaw);
            double forward = dx * Math.sin(yaw) + dz * Math.cos(yaw);
            int px = cx + (int)Math.round(right * scale);
            int py = cy - (int)Math.round(forward * scale);
            int ox = px - cx, oy = py - cy;
            double d = Math.sqrt(ox * ox + oy * oy);
            if (d > radius - 3) { px = cx + (int)(ox * (radius - 3) / d); py = cy + (int)(oy * (radius - 3) / d); }
            g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFF5968);
        }
        g.drawString(mc.font, Component.literal("N"), cx - 3, cy - 40, 0xFFFFFFFF);
    }

    private static int lastAppliedProfile = -1;

    private static void applyPerformanceProfile(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        if (lastAppliedProfile != ShadowConfig.performanceProfile) {
            applyPerformanceProfileNow(mc);
        }
    }

    public static void applyPerformanceProfileNow(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        switch (ShadowConfig.performanceProfile) {
            case 1 -> { // FPS Boost
                mc.options.entityShadows().set(false);
                mc.options.ambientOcclusion().set(false);
                mc.options.renderDistance().set(Math.min(mc.options.renderDistance().get(), 6));
                mc.options.simulationDistance().set(Math.min(mc.options.simulationDistance().get(), 4));
            }
            case 2 -> { // Quality
                mc.options.entityShadows().set(true);
                mc.options.ambientOcclusion().set(true);
                mc.options.renderDistance().set(Math.max(mc.options.renderDistance().get(), 12));
                mc.options.simulationDistance().set(Math.max(mc.options.simulationDistance().get(), 8));
            }
            default -> { // Balanced
                mc.options.entityShadows().set(true);
                mc.options.ambientOcclusion().set(true);
                mc.options.renderDistance().set(10);
                mc.options.simulationDistance().set(6);
            }
        }
        lastAppliedProfile = ShadowConfig.performanceProfile;
    }

    public static void restorePerformanceDefaults(Minecraft mc) {
        if (mc == null || mc.options == null) return;
        mc.options.entityShadows().set(true);
        mc.options.ambientOcclusion().set(true);
        mc.options.renderDistance().set(12);
        mc.options.simulationDistance().set(8);
        lastAppliedProfile = -1;
    }

    private static boolean isHoldingProjectileWeapon(Minecraft mc) {
        var stack = mc.player.getMainHandItem();
        return stack.is(Items.TRIDENT) || stack.is(Items.CROSSBOW);
    }

    private static String onOff(boolean value) { return value ? "§aON" : "§cOFF"; }

    private static KeyMapping register(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.shadowclient." + name, key, KeyMapping.Category.MISC
        ));
    }
}
