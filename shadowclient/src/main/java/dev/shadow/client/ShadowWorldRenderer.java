package dev.shadow.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.EntityHitResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** World-space player outlines for the local client. */
public final class ShadowWorldRenderer {
    private static final Set<UUID> CHAMS_APPLIED = new HashSet<>();
    private ShadowWorldRenderer() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ShadowWorldRenderer::renderPlayerBoxes);
    }

    private static void renderPlayerBoxes(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean drawPlayers = ShadowConfig.esp && ShadowConfig.boxes2d;
        boolean drawTracers = ShadowConfig.tracers;
        boolean drawHealth = ShadowConfig.health;
        boolean drawNames = ShadowConfig.nameTags;
        boolean drawChams = ShadowConfig.chams;
        boolean drawItems = ShadowConfig.itemEsp;
        Player aimedPlayer = null;
        if (ShadowConfig.targetEsp && mc.hitResult instanceof EntityHitResult hit
            && hit.getEntity() instanceof Player p && p != mc.player && p.isAlive() && !p.isSpectator()) {
            aimedPlayer = p;
        }
        if (!drawPlayers && !drawTracers && !drawHealth && !drawNames && !drawChams && !drawItems && aimedPlayer == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.position();
        PoseStack matrices = context.matrices();
        VertexConsumer lines = context.consumers().getBuffer(RenderTypes.LINES);
        matrices.pushPose();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (Player target : mc.level.players()) {
            if (target == mc.player || !target.isAlive() || target.isSpectator()) continue;
            if (target.distanceToSqr(mc.player) > 64.0 * 64.0) continue;
            float[] color = target == aimedPlayer ? new float[]{1.0f, 0.82f, 0.12f} : colorFor(target);
            AABB bounds = target.getBoundingBox().inflate(0.025);
            if (drawPlayers && target != aimedPlayer) drawBox(matrices, lines, bounds, color, 235);
            // Chams-style client glow outline; this uses Minecraft's entity outline pass, not a through-wall filled model shader.
            if (drawChams) { target.setGlowingTag(true); CHAMS_APPLIED.add(target.getUUID()); }
            if (drawTracers) {
                Vec3 from = new Vec3(0, 0, 0);
                Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(cameraPos);
                drawLine(matrices, lines, from, to, color, 230);
            }
            if (drawHealth) drawHealthBar(matrices, lines, target, color);
            if (drawNames) drawNameTag(context, matrices, target, cameraPos);
        }
        if (drawItems) {
            for (ItemEntity item : mc.level.getEntitiesOfClass(ItemEntity.class, mc.player.getBoundingBox().inflate(48.0))) {
                if (item.isAlive()) drawBox(matrices, lines, item.getBoundingBox().inflate(0.08), new float[]{1.0f, 0.72f, 0.18f}, 230);
            }
        }
        if (!drawChams && !CHAMS_APPLIED.isEmpty()) {
            for (Player player : mc.level.players()) {
                if (CHAMS_APPLIED.remove(player.getUUID())) player.setGlowingTag(false);
            }
        }
        // Target ESP is independent of regular Player ESP and uses a bright gold outline.
        if (aimedPlayer != null) {
            drawBox(matrices, lines, aimedPlayer.getBoundingBox().inflate(0.075), new float[]{1.0f, 0.82f, 0.12f}, 255);
        }
        matrices.popPose();
    }

    private static void drawLine(PoseStack matrices, VertexConsumer consumer, Vec3 a, Vec3 b, float[] color, int alpha) {
        PoseStack.Pose pose = matrices.last();
        int r = Math.round(color[0] * 255), g = Math.round(color[1] * 255), bl = Math.round(color[2] * 255);
        consumer.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(r, g, bl, alpha).setNormal(0, 1, 0);
        consumer.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(r, g, bl, alpha).setNormal(0, 1, 0);
    }

    private static void drawHealthBar(PoseStack matrices, VertexConsumer consumer, Player player, float[] ignored) {
        AABB b = player.getBoundingBox();
        double x = b.minX - 0.06, z = (b.minZ + b.maxZ) * 0.5;
        double bottom = b.minY, top = b.minY + player.getBbHeight();
        float ratio = Math.max(0.0f, Math.min(1.0f, player.getHealth() / Math.max(1.0f, player.getMaxHealth())));
        drawLine(matrices, consumer, new Vec3(x, bottom, z), new Vec3(x, top, z), new float[]{0.12f, 0.08f, 0.08f}, 220);
        float[] hp = ratio > 0.6f ? new float[]{0.2f, 1.0f, 0.25f} : ratio > 0.3f ? new float[]{1.0f, 0.8f, 0.15f} : new float[]{1.0f, 0.2f, 0.2f};
        drawLine(matrices, consumer, new Vec3(x, bottom, z), new Vec3(x, bottom + (top - bottom) * ratio, z), hp, 255);
    }

    private static void drawNameTag(WorldRenderContext context, PoseStack matrices, Player player, Vec3 cameraPos) {
        Minecraft mc = Minecraft.getInstance();
        matrices.pushPose();
        Vec3 pos = player.position().add(0, player.getBbHeight() + 0.35, 0).subtract(cameraPos);
        matrices.translate(pos.x, pos.y, pos.z);
        matrices.mulPose(mc.gameRenderer.getMainCamera().rotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        String label = player.getName().getString() + "  " + String.format("%.1fm", Math.sqrt(player.distanceToSqr(mc.player)));
        int width = mc.font.width(label);
        mc.font.drawInBatch(Component.literal(label), -width / 2.0f, 0, 0xFFFFFFFF, false,
            matrices.last().pose(), context.consumers(), Font.DisplayMode.SEE_THROUGH, 0x60000000, LightTexture.FULL_BRIGHT);
        matrices.popPose();
    }

    private static void drawBox(PoseStack matrices, VertexConsumer consumer, AABB box, float[] color, int alpha) {
        double[][] p = {
            {box.minX, box.minY, box.minZ}, {box.maxX, box.minY, box.minZ},
            {box.maxX, box.minY, box.maxZ}, {box.minX, box.minY, box.maxZ},
            {box.minX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.minZ},
            {box.maxX, box.maxY, box.maxZ}, {box.minX, box.maxY, box.maxZ}
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},
                         {0,4},{1,5},{2,6},{3,7}};
        PoseStack.Pose pose = matrices.last();
        int red = Math.round(color[0] * 255), green = Math.round(color[1] * 255);
        int blue = Math.round(color[2] * 255);
        for (int[] edge : edges) {
            double[] a = p[edge[0]], b = p[edge[1]];
            consumer.addVertex(pose, (float)a[0], (float)a[1], (float)a[2])
                .setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f);
            consumer.addVertex(pose, (float)b[0], (float)b[1], (float)b[2])
                .setColor(red, green, blue, alpha).setNormal(0.0f, 1.0f, 0.0f);
        }
    }

    private static float[] colorFor(Player player) {
        return switch (ShadowConfig.espColor) {
            case 1 -> new float[]{0.15f, 0.85f, 1.0f};
            case 2 -> new float[]{1.0f, 0.20f, 0.25f};
            case 3 -> new float[]{0.25f, 1.0f, 0.40f};
            default -> new float[]{0.75f, 0.30f, 1.0f};
        };
    }
}
