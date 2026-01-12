package me.kall.narutoloading.mixin.noworld.impl.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.gui.ScrollPanel;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("deprecation")
@Mixin(value = ScrollPanel.class, priority = 500)
public abstract class MixinScrollPanel {
    @Shadow(remap = false) protected abstract void drawBackground();
    @Shadow protected abstract void drawPanel(PoseStack mStack, int entryRight, int relativeY, Tesselator tess, int mouseX, int mouseY);
    @Shadow(remap = false) protected abstract int getContentHeight();
    @Shadow(remap = false) protected abstract int getBarHeight();
    @Shadow protected abstract void drawGradientRect(PoseStack mStack, int left, int top, int right, int bottom, int color1, int color2);

    @Shadow(remap = false) @Final private Minecraft client;
    @Shadow(remap = false) @Final protected int left;
    @Shadow(remap = false) @Final protected int width;
    @Shadow(remap = false) @Final protected int bottom;
    @Shadow(remap = false) @Final protected int height;
    @Shadow(remap = false) @Final protected int top;
    @Shadow(remap = false) @Final protected int right;
    @Shadow(remap = false) protected float scrollDistance;
    @Shadow(remap = false) @Final protected int border;
    @Shadow(remap = false) @Final private int barLeft;
    @Shadow(remap = false) @Final private int barWidth;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ci.cancel();
        this.drawBackground();

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder worldr = tess.getBuilder();

        double scale = client.getWindow().getGuiScale();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int)(left  * scale), (int)(client.getWindow().getHeight() - (bottom * scale)),
                (int)(width * scale), (int)(height * scale));

        if (this.client.level != null)
        {
            this.drawGradientRect(matrix, this.left, this.top, this.right, this.bottom, 0xC0101010, 0xD0101010);
        }

        int baseY = this.top + border - (int)this.scrollDistance;
        this.drawPanel(matrix, right, baseY, tess, mouseX, mouseY);

        RenderSystem.disableDepthTest();

        int extraHeight = (this.getContentHeight() + border) - height;
        if (extraHeight > 0)
        {
            int barHeight = getBarHeight();

            int barTop = (int)this.scrollDistance * (height - barHeight) / extraHeight + this.top;
            if (barTop < this.top)
            {
                barTop = this.top;
            }

            RenderSystem.disableTexture();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(barLeft,            this.bottom, 0.0D).uv(0.0F, 1.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth, this.bottom, 0.0D).uv(1.0F, 1.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth, this.top,    0.0D).uv(1.0F, 0.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            worldr.vertex(barLeft,            this.top,    0.0D).uv(0.0F, 0.0F).color(0x00, 0x00, 0x00, 0xFF).endVertex();
            tess.end();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(barLeft,            barTop + barHeight, 0.0D).uv(0.0F, 1.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth, barTop + barHeight, 0.0D).uv(1.0F, 1.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth, barTop,             0.0D).uv(1.0F, 0.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            worldr.vertex(barLeft,            barTop,             0.0D).uv(0.0F, 0.0F).color(0x80, 0x80, 0x80, 0xFF).endVertex();
            tess.end();
            worldr.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            worldr.vertex(barLeft,                barTop + barHeight - 1, 0.0D).uv(0.0F, 1.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth - 1, barTop + barHeight - 1, 0.0D).uv(1.0F, 1.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(barLeft + barWidth - 1, barTop,                 0.0D).uv(1.0F, 0.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            worldr.vertex(barLeft,                barTop,                 0.0D).uv(0.0F, 0.0F).color(0xC0, 0xC0, 0xC0, 0xFF).endVertex();
            tess.end();
        }

        RenderSystem.enableTexture();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
