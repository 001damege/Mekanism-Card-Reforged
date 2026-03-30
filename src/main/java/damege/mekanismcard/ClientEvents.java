package damege.mekanismcard;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import damege.mekanismcard.item.MassUpgradeConfigurator;
import mekanism.api.Upgrade;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

public final class ClientEvents {
    private static final int MAX_RENDER = 500;

    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.getItem() instanceof MassUpgradeConfigurator configurator) {
            if (configurator.isSelectionModeActive(mainHand)) {
                float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.3f + 0.7f);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                float r = 0.0f, g = 1.0f, b = 1.0f;
                Matrix4f matrix = event.getPoseStack().last().pose();
                BufferBuilder buf = new BufferBuilder(0);
                buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                float size = 0.5f;
                float thickness = 0.02f;
                float z = 0.01f;

                buf.vertex(matrix, -thickness, -thickness, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, size + thickness, -thickness, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, size + thickness, size + thickness, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, -thickness, size + thickness, z).color(r, g, b, 0.6f * pulse);

                buf.vertex(matrix, size, 0, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, size, size, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, 0, size, z).color(r, g, b, 0.6f * pulse);
                buf.vertex(matrix, 0, 0, z).color(r, g, b, 0.6f * pulse);

                BufferUploader.drawWithShader(buf.end());
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
            }
        }
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof MassUpgradeConfigurator configurator)) {
            return;
        }

        if (!configurator.isSelectionModeActive(mainHand)) {
            return;
        }
        BlockPos[] points = configurator.getSelectionPoints(mainHand);
        if (points[0] == null || points[1] == null) {
            return;
        }

        MassUpgradeConfigurator.Mode currentMode = configurator.getCurrentMode();
        Upgrade upgradeType = configurator.getSelectedUpgradeFromInventory(player);
        boolean hasUpgrade = upgradeType != null;
        float r, g, b;
        if (!hasUpgrade) {
            r = g = b = 0.5f;
        } else {
            if (currentMode == MassUpgradeConfigurator.Mode.INSTALL) {
                r = 0.0f;
                g = 1.0f;
            } else {
                r = 1.0f;
                g = 0.0f;
            }
            b = 0.0f;
        }

        int minX = Math.min(points[0].getX(), points[1].getX());
        int maxX = Math.max(points[0].getX(), points[1].getX());
        int minY = Math.min(points[0].getY(), points[1].getY());
        int maxY = Math.max(points[0].getY(), points[1].getY());
        int minZ = Math.min(points[0].getZ(), points[1].getZ());
        int maxZ = Math.max(points[0].getZ(), points[1].getZ());
        Camera camera = event.getCamera();
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (count++ >= MAX_RENDER) {
                        return;
                    }
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = level.getBlockEntity(pos);
                    // 只渲染 Mekanism 机器
                    if (be instanceof TileEntityMekanism) {
                        AABB worldBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                        AABB relativeBox = worldBox.move(-camX, -camY, -camZ);
                        renderSingleBox(event.getPoseStack(), relativeBox, r, g, b);
                    }
                }
            }
        }
    }

    private static void renderSingleBox(PoseStack stack, AABB box, float r, float g, float b) {
        AABB inflated = box.inflate(0.002);
        Matrix4f matrix = stack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buf = new BufferBuilder(0);
        buf.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
        LevelRenderer.renderLineBox(stack, buf, inflated, r, g, b, 1.0f);
        BufferUploader.drawWithShader(buf.end());

        fillBox(buf, matrix, inflated, r, g, b, 0.1f);
        BufferUploader.drawWithShader(buf.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void fillBox(BufferBuilder buf, Matrix4f m, AABB b, float r, float g, float bl, float a) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ;
        double x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        quad(buf, m, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, bl, a);
        quad(buf, m, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, bl, a);
        quad(buf, m, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, r, g, bl, a);
        quad(buf, m, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, bl, a);
        quad(buf, m, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, bl, a);
        quad(buf, m, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, bl, a);
    }

    private static void quad(BufferBuilder b, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2, double x3, double y3, double z3, double x4, double y4, double z4, float r, float g, float bl, float a) {
        v(b, m, x1, y1, z1, r, g, bl, a);
        v(b, m, x2, y2, z2, r, g, bl, a);
        v(b, m, x3, y3, z3, r, g, bl, a);
        v(b, m, x4, y4, z4, r, g, bl, a);
    }

    private static void v(BufferBuilder b, Matrix4f m, double x, double y, double z, float r, float g, float bl, float a) {
        b.vertex(m, (float) x, (float) y, (float) z).color(r, g, bl, a);
    }

    private ClientEvents() {
    }
}
