package gregtech.client.utils;

import codechicken.lib.texture.TextureUtils;
import codechicken.lib.vec.Matrix4;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.util.GTLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Function;

@SideOnly(Side.CLIENT)
public class RenderUtil {

    private static final Deque<int[]> scissorFrameStack = new ArrayDeque<>();

    public static void useScissor(int x, int y, int width, int height, Runnable codeBlock) {
        pushScissorFrame(x, y, width, height);
        try {
            codeBlock.run();
        } finally {
            popScissorFrame();
        }
    }

    private static int[] peekFirstScissorOrFullScreen() {
        int[] currentTopFrame = scissorFrameStack.isEmpty() ? null : scissorFrameStack.peek();
        if (currentTopFrame == null) {
            Minecraft minecraft = Minecraft.getMinecraft();
            return new int[]{0, 0, minecraft.displayWidth, minecraft.displayHeight};
        }
        return currentTopFrame;
    }

    public static void pushScissorFrame(int x, int y, int width, int height) {
        int[] parentScissor = peekFirstScissorOrFullScreen();
        int parentX = parentScissor[0];
        int parentY = parentScissor[1];
        int parentWidth = parentScissor[2];
        int parentHeight = parentScissor[3];

        boolean pushedFrame = false;
        if (x <= parentX + parentWidth && y <= parentY + parentHeight) {
            int newX = Math.max(x, parentX);
            int newY = Math.max(y, parentY);
            int newWidth = width - (newX - x);
            int newHeight = height - (newY - y);
            if (newWidth > 0 && newHeight > 0) {
                int maxWidth = parentWidth - (x - parentX);
                int maxHeight = parentHeight - (y - parentY);
                newWidth = Math.min(maxWidth, newWidth);
                newHeight = Math.min(maxHeight, newHeight);
                applyScissor(newX, newY, newWidth, newHeight);
                //finally, push applied scissor on top of scissor stack
                if (scissorFrameStack.isEmpty()) {
                    GL11.glEnable(GL11.GL_SCISSOR_TEST);
                }
                scissorFrameStack.push(new int[]{newX, newY, newWidth, newHeight});
                pushedFrame = true;
            }
        }
        if (!pushedFrame) {
            if (scissorFrameStack.isEmpty()) {
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
            scissorFrameStack.push(new int[]{parentX, parentY, parentWidth, parentHeight});
        }
    }

    public static void popScissorFrame() {
        scissorFrameStack.pop();
        int[] parentScissor = peekFirstScissorOrFullScreen();
        int parentX = parentScissor[0];
        int parentY = parentScissor[1];
        int parentWidth = parentScissor[2];
        int parentHeight = parentScissor[3];
        applyScissor(parentX, parentY, parentWidth, parentHeight);
        if (scissorFrameStack.isEmpty()) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    //applies scissor with gui-space coordinates and sizes
    private static void applyScissor(int x, int y, int w, int h) {
        //translate upper-left to bottom-left
        ScaledResolution r = ((GuiIngameForge) Minecraft.getMinecraft().ingameGUI).getResolution();
        int s = r == null ? 1 : r.getScaleFactor();
        int translatedY = r == null ? 0 : (r.getScaledHeight() - y - h);
        GL11.glScissor(x * s, translatedY * s, w * s, h * s);
    }

    /***
     * used to render pixels in stencil mask. (e.g. Restrict rendering results to be displayed only in Monitor Screens)
     * if you want to do the similar things in Gui(2D) not World(3D), plz consider using the {@link #useScissor(int, int, int, int, Runnable)}
     * that you don't need to draw mask to build a rect mask easily.
     * @param mask draw mask
     * @param renderInMask rendering in the mask
     * @param shouldRenderMask should mask be rendered too
     */
    public static void useStencil(Runnable mask, Runnable renderInMask, boolean shouldRenderMask) {
        GL11.glStencilMask(0xFF);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);

        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);

        if (!shouldRenderMask) {
            GL11.glColorMask(false, false, false, false);
            GL11.glDepthMask(false);
        }

        mask.run();

        if (!shouldRenderMask) {
            GL11.glColorMask(true, true, true, true);
            GL11.glDepthMask(true);
        }

        GL11.glStencilMask(0x00);
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

        renderInMask.run();

        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void useLightMap(float x, float y, Runnable codeBlock) {
        /* hack the lightmap */
        GL11.glPushAttrib(GL11.GL_LIGHTING_BIT);
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
        float lastBrightnessX = OpenGlHelper.lastBrightnessX;
        float lastBrightnessY = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
        if (codeBlock != null) {
            codeBlock.run();
        }
        /* restore the lightmap  */
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        GL11.glPopAttrib();
    }

    public static void moveToFace(double x, double y, double z, EnumFacing face) {
        GlStateManager.translate(x + 0.5 + face.getXOffset() * 0.5, y + 0.5 + face.getYOffset() * 0.5, z + 0.5 + face.getZOffset() * 0.5);
    }

    public static void rotateToFace(EnumFacing face, @Nullable EnumFacing spin) {
        int angle = spin == EnumFacing.EAST ? 90 : spin == EnumFacing.SOUTH ? 180 : spin == EnumFacing.WEST ? -90 : 0;
        switch (face) {
            case UP:
                GlStateManager.scale(1.0f, -1.0f, 1.0f);
                GlStateManager.rotate(90.0f, 1.0f, 0.0f, 0.0f);
                GlStateManager.rotate(angle, 0, 0, 1);
                break;
            case DOWN:
                GlStateManager.scale(1.0f, -1.0f, 1.0f);
                GlStateManager.rotate(-90.0f, 1.0f, 0.0f, 0.0f);
                GlStateManager.rotate(spin == EnumFacing.EAST ? 90 : spin == EnumFacing.NORTH ? 180 : spin == EnumFacing.WEST ? -90 : 0, 0, 0, 1);
                break;
            case EAST:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(-90.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(angle, 0, 0, 1);
                break;
            case WEST:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(90.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(angle, 0, 0, 1);
                break;
            case NORTH:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(angle, 0, 0, 1);
                break;
            case SOUTH:
                GlStateManager.scale(-1.0f, -1.0f, -1.0f);
                GlStateManager.rotate(180.0f, 0.0f, 1.0f, 0.0f);
                GlStateManager.rotate(angle, 0, 0, 1);
                break;
            default:
                break;
        }
    }

    private static final Map<TextureAtlasSprite, Integer> textureMap = new HashMap<>();

    public static void bindTextureAtlasSprite(TextureAtlasSprite textureAtlasSprite) {
        if (textureAtlasSprite == null) {
            return;
        }
        if (textureMap.containsKey(textureAtlasSprite)) {
            GlStateManager.bindTexture(textureMap.get(textureAtlasSprite));
            return;
        }

        final int iconWidth = textureAtlasSprite.getIconWidth();
        final int iconHeight = textureAtlasSprite.getIconHeight();
        final int frameCount = textureAtlasSprite.getFrameCount();
        if (iconWidth <= 0 || iconHeight <= 0 || frameCount <= 0) {
            return;
        }

        BufferedImage bufferedImage = new BufferedImage(iconWidth, iconHeight * frameCount, BufferedImage.TYPE_4BYTE_ABGR);
        for (int i = 0; i < frameCount; i++) {
            int[][] frameTextureData = textureAtlasSprite.getFrameTextureData(i);
            int[] largestMipMapTextureData = frameTextureData[0];
            bufferedImage.setRGB(0, i * iconHeight, iconWidth, iconHeight, largestMipMapTextureData, 0, iconWidth);
        }
        int glTextureId = TextureUtil.glGenTextures();
        if (glTextureId != -1) {
            TextureUtil.uploadTextureImageAllocate(glTextureId, bufferedImage, false, false);
            textureMap.put(textureAtlasSprite, glTextureId);
            GlStateManager.bindTexture(textureMap.get(textureAtlasSprite));
        }
    }

    /***
     * avoid z-fighting. not familiar with the CCL, its a trick.
     * //TODO could DisableDepthMask in the CCL?
     * @param translation origin
     * @param side facing
     * @param layer level
     * @return adjust
     */
    public static Matrix4 adjustTrans(Matrix4 translation, EnumFacing side, int layer) {
        Matrix4 trans = translation.copy();
        switch (side) {
            case DOWN:
                trans.translate(0, -0.0005D * layer, 0);
                break;
            case UP:
                trans.translate(0, 0.0005D * layer, 0);
                break;
            case NORTH:
                trans.translate(0, 0, -0.0005D * layer);
                break;
            case SOUTH:
                trans.translate(0, 0, 0.0005D * layer);
                break;
            case EAST:
                trans.translate(0.0005D * layer, 0, 0);
                break;
            case WEST:
                trans.translate(-0.0005D * layer, 0, 0);
                break;
        }
        return trans;
    }

    public static Function<Float, Integer> colorInterpolator(int color1, int color2) {
        int a = color1 >> 24 & 255;
        int r = color1 >> 16 & 255;
        int g = color1 >> 8 & 255;
        int b = color1 & 255;

        int a2 = color2 >> 24 & 255;
        int r2 = color2 >> 16 & 255;
        int g2 = color2 >> 8 & 255;
        int b2 = color2 & 255;
        return (f) -> {
            int A = (int) (a * (1 - f) + a2 * (f));
            int R = (int) (r * (1 - f) + r2 * (f));
            int G = (int) (g * (1 - f) + g2 * (f));
            int B = (int) (b * (1 - f) + b2 * (f));
            return A << 24 | R << 16 | G << 8 | B;
        };
    }

    public static void renderRect(float x, float y, float width, float height, float z, int color) {
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(f, f1, f2, f3);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        buffer.pos(x + width, y, z).endVertex();
        buffer.pos(x, y, z).endVertex();
        buffer.pos(x, y + height, z).endVertex();
        buffer.pos(x + width, y + height, z).endVertex();
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void renderGradientRect(float x, float y, float width, float height, float z, int startColor, int endColor, boolean horizontal) {
        float startAlpha = (float) (startColor >> 24 & 255) / 255.0F;
        float startRed = (float) (startColor >> 16 & 255) / 255.0F;
        float startGreen = (float) (startColor >> 8 & 255) / 255.0F;
        float startBlue = (float) (startColor & 255) / 255.0F;
        float endAlpha = (float) (endColor >> 24 & 255) / 255.0F;
        float endRed = (float) (endColor >> 16 & 255) / 255.0F;
        float endGreen = (float) (endColor >> 8 & 255) / 255.0F;
        float endBlue = (float) (endColor & 255) / 255.0F;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        if (horizontal) {
            buffer.pos(x + width, y, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
            buffer.pos(x, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y + height, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
        } else {
            buffer.pos(x + width, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y, z).color(startRed, startGreen, startBlue, startAlpha).endVertex();
            buffer.pos(x, y + height, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        }
        buffer.pos(x + width, y + height, z).color(endRed, endGreen, endBlue, endAlpha).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    public static void renderText(float x, float y, float z, float scale, int color, final String renderedText, boolean centered) {
        GlStateManager.pushMatrix();
        final FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        final int width = fr.getStringWidth(renderedText);
        GlStateManager.translate(x, y - scale * 4, z);
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(-0.5f * (centered ? 1 : 0) * width, 0.0f, 0.5f);

        fr.drawString(renderedText, 0, 0, color);
        GlStateManager.popMatrix();
    }

    public static void renderItemOverLay(float x, float y, float z, float scale, ItemStack itemStack) {
        net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 0.0001f);
        GlStateManager.translate(x * 16, y * 16, z * 16);
        RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
        renderItem.renderItemAndEffectIntoGUI(itemStack, 0, 0);
        GlStateManager.popMatrix();
        net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
    }

    public static void renderFluidOverLay(float x, float y, float width, float height, float z, FluidStack fluidStack, float alpha) {
        if (fluidStack != null) {
            int color = fluidStack.getFluid().getColor(fluidStack);
            float r = (color >> 16 & 255) / 255.0f;
            float g = (color >> 8 & 255) / 255.0f;
            float b = (color & 255) / 255.0f;
            TextureAtlasSprite sprite = TextureUtils.getTexture(fluidStack.getFluid().getStill(fluidStack));
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableAlpha();
            //GlStateManager.disableLighting();
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            double uMin = sprite.getInterpolatedU(16D - width * 16D), uMax = sprite.getInterpolatedU(width * 16D);
            double vMin = sprite.getMinV(), vMax = sprite.getInterpolatedV(height * 16D);
            buf.pos(x, y, z).tex(uMin, vMin).color(r, g, b, alpha).endVertex();
            buf.pos(x, y + height, z).tex(uMin, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y + height, z).tex(uMax, vMax).color(r, g, b, alpha).endVertex();
            buf.pos(x + width, y, z).tex(uMax, vMin).color(r, g, b, alpha).endVertex();
            tess.draw();

            //GlStateManager.enableLighting();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();
            GlStateManager.color(1F, 1F, 1F, 1F);
        }
    }

    public static void renderTextureArea(TextureArea textureArea, float x, float y, float width, float height, float z) {
        double imageU = textureArea.offsetX;
        double imageV = textureArea.offsetY;
        double imageWidth = textureArea.imageWidth;
        double imageHeight = textureArea.imageHeight;
        Minecraft.getMinecraft().renderEngine.bindTexture(textureArea.imageLocation);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x, y + height, z).tex(imageU, imageV + imageHeight).endVertex();
        bufferbuilder.pos(x + width, y + height, z).tex(imageU + imageWidth, imageV + imageHeight).endVertex();
        bufferbuilder.pos(x + width, y, z).tex(imageU + imageWidth, imageV).endVertex();
        bufferbuilder.pos(x, y, z).tex(imageU, imageV).endVertex();
        tessellator.draw();
    }

    public static void renderLineChart(List<Long> data, long max, float x, float y, float width, float height, float lineWidth, int color) {
        float durX = data.size() > 1 ? width / (data.size() - 1) : 0;
        float hY = max > 0 ? height / max : 0;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        GlStateManager.color(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >> 24) & 0xFF) / 255f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        float last_x = x + 0 * durX;
        float last_y = y - data.get(0) * hY;
        for (int i = 0; i < data.size(); i++) {
            float _x = x + i * durX;
            float _y = y - data.get(i) * hY;
            // draw lines
            if (i != 0) {
                bufferbuilder.pos(last_x, last_y - lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(last_x, last_y + lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(_x, _y + lineWidth, 0.01D).endVertex();
                bufferbuilder.pos(_x, _y - lineWidth, 0.01D).endVertex();
                last_x = _x;
                last_y = _y;
            }
            // draw points
            bufferbuilder.pos(_x - 3 * lineWidth, _y, 0.01D).endVertex();
            bufferbuilder.pos(_x, _y + 3 * lineWidth, 0.01D).endVertex();
            bufferbuilder.pos(_x + 3 * lineWidth, _y, 0.01D).endVertex();
            bufferbuilder.pos(_x, _y - 3 * lineWidth, 0.01D).endVertex();
        }
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
    }

    public static void renderLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        float hypo = (float) Math.sqrt((y1 - y2) * (y1 - y2) + (x1 - x2) * (x1 - x2));
        float w = (x2 - x1) / hypo * lineWidth;
        float h = (y1 - y2) / hypo * lineWidth;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        GlStateManager.color(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >> 24) & 0xFF) / 255f);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
        if (w * h > 0) {
            bufferbuilder.pos(x1 - w, y1 - h, 0.01D).endVertex();
            bufferbuilder.pos(x1 + w, y1 + h, 0.01D).endVertex();
            bufferbuilder.pos(x2 + w, y2 + h, 0.01D).endVertex();
            bufferbuilder.pos(x2 - w, y2 - h, 0.01D).endVertex();
        } else {
            h = (y2 - y1) / hypo * lineWidth;
            bufferbuilder.pos(x1 + w, y1 - h, 0.01D).endVertex();
            bufferbuilder.pos(x1 - w, y1 + h, 0.01D).endVertex();
            bufferbuilder.pos(x2 - w, y2 + h, 0.01D).endVertex();
            bufferbuilder.pos(x2 + w, y2 - h, 0.01D).endVertex();
        }
        tessellator.draw();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
    }

    public static void drawFluidTexture(double xCoord, double yCoord, TextureAtlasSprite textureSprite, int maskTop, int maskRight, double zLevel) {
        double uMin = textureSprite.getMinU();
        double uMax = textureSprite.getMaxU();
        double vMin = textureSprite.getMinV();
        double vMax = textureSprite.getMaxV();
        uMax = uMax - maskRight / 16.0 * (uMax - uMin);
        vMax = vMax - maskTop / 16.0 * (vMax - vMin);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(xCoord, yCoord + 16, zLevel).tex(uMin, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + 16, zLevel).tex(uMax, vMax).endVertex();
        buffer.pos(xCoord + 16 - maskRight, yCoord + maskTop, zLevel).tex(uMax, vMin).endVertex();
        buffer.pos(xCoord, yCoord + maskTop, zLevel).tex(uMin, vMin).endVertex();
        tessellator.draw();
    }

    public static void drawFluidForGui(FluidStack contents, int tankCapacity, int startX, int startY, int widthT, int heightT) {
        widthT--;
        heightT--;
        Fluid fluid = contents.getFluid();
        ResourceLocation fluidStill = fluid.getStill(contents);
        TextureAtlasSprite fluidStillSprite = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(fluidStill.toString());
        int fluidColor = fluid.getColor(contents);
        int scaledAmount;
        if (contents.amount == tankCapacity) {
            scaledAmount = heightT;
        } else {
            scaledAmount = contents.amount * heightT / tankCapacity;
        }
        if (contents.amount > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        if (scaledAmount > heightT) {
            scaledAmount = heightT;
        }
        GlStateManager.enableBlend();
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        // fluid is RGBA for GT guis, despite MC's fluids being ARGB
        setGlColorFromInt(fluidColor, 0xFF);

        final int xTileCount = widthT / 16;
        final int xRemainder = widthT - xTileCount * 16;
        final int yTileCount = scaledAmount / 16;
        final int yRemainder = scaledAmount - yTileCount * 16;

        final int yStart = startY + heightT;

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int width = xTile == xTileCount ? xRemainder : 16;
                int height = yTile == yTileCount ? yRemainder : 16;
                int x = startX + xTile * 16;
                int y = yStart - (yTile + 1) * 16;
                if (width > 0 && height > 0) {
                    int maskTop = 16 - height;
                    int maskRight = 16 - width;

                    drawFluidTexture(x, y, fluidStillSprite, maskTop, maskRight, 0.0);
                }
            }
        }
        GlStateManager.disableBlend();
    }

    public static int packColor(int red, int green, int blue, int alpha) {
        return (red & 0xFF) << 24 | (green & 0xFF) << 16 | (blue & 0xFF) << 8 | (alpha & 0xFF);
    }

    public static void setGlColorFromInt(int colorValue, int opacity) {
        int i = (colorValue & 0xFF0000) >> 16;
        int j = (colorValue & 0xFF00) >> 8;
        int k = (colorValue & 0xFF);
        GlStateManager.color(i / 255.0f, j / 255.0f, k / 255.0f, opacity / 255.0f);
    }

    public static void setGlClearColorFromInt(int colorValue, int opacity) {
        int i = (colorValue & 0xFF0000) >> 16;
        int j = (colorValue & 0xFF00) >> 8;
        int k = (colorValue & 0xFF);
        GlStateManager.clearColor(i / 255.0f, j / 255.0f, k / 255.0f, opacity / 255.0f);
    }

    public static int getFluidColor(FluidStack fluidStack) {
        if (fluidStack.getFluid() == FluidRegistry.WATER)
            return 0x3094CF;
        else if (fluidStack.getFluid() == FluidRegistry.LAVA)
            return 0xFFD700;
        return fluidStack.getFluid().getColor(fluidStack);
    }

    public static boolean updateFBOSize(Framebuffer fbo, int width, int height) {
        if (fbo.framebufferWidth != width || fbo.framebufferHeight != height) {
            fbo.createBindFramebuffer(width, height);
            return true;
        }
        return false;
    }

    public static void hookDepthBuffer(Framebuffer fbo, int depthBuffer) {
        //Hook DepthBuffer
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, fbo.framebufferObject);
        if (fbo.isStencilEnabled()) {
            OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, org.lwjgl.opengl.EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, OpenGlHelper.GL_RENDERBUFFER, depthBuffer);
            OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, org.lwjgl.opengl.EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, OpenGlHelper.GL_RENDERBUFFER, depthBuffer);
        } else {
            OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, OpenGlHelper.GL_RENDERBUFFER, depthBuffer);
        }
    }

    public static void hookDepthTexture(Framebuffer fbo, int depthTexture) {
        //Hook DepthTexture
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, fbo.framebufferObject);
        if (fbo.isStencilEnabled()) {
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
        } else {
            OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0);
        }
    }

    /**
     * Makes given BakedQuad emissive; specifically, it makes a new UnpackedBakedQuad with
     * constant lightmap coordination and {@code applyDiffuseLighting} set to {@code false}.
     * The other properties, such as textures, tint color and other vertex data will be copied from
     * the template.<p>
     * Note that this method just returns {@code quad} if Optifine is installed.
     *
     * @param quad Template BakedQuad.
     * @return New UnpackedBakedQuad with emissive property
     */
    public static BakedQuad makeEmissive(BakedQuad quad) {
        if (FMLClientHandler.instance().hasOptifine()) return quad;
        VertexFormat format = quad.getFormat();
        if (!format.getElements().contains(DefaultVertexFormats.TEX_2S)) {
            format = new VertexFormat(quad.getFormat());
            format.addElement(DefaultVertexFormats.TEX_2S);
        }
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format) {
            @Override
            public void put(int element, @Nonnull float... data) {
                if (this.getVertexFormat().getElement(element) == DefaultVertexFormats.TEX_2S)
                    super.put(element, 480.0f / 0xFFFF, 480.0f / 0xFFFF);
                else super.put(element, data);
            }
        };
        quad.pipe(builder);
        builder.setApplyDiffuseLighting(false);
        return builder.build();
    }

    @Nullable
    public static BakedQuad clamp(@Nonnull BakedQuad quad,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2) {
        // full block, no need to clamp
        if (x1 == 0 && y1 == 0 && z1 == 0 && x2 == 1 && y2 == 1 && z2 == 1) return quad;

        int posElementIndex = quad.getFormat().getElements().indexOf(DefaultVertexFormats.POSITION_3F);
        if (posElementIndex == -1) return quad; // shouldn't be possible but who knows
        int uvElementIndex = quad.getFormat().getElements().indexOf(DefaultVertexFormats.TEX_2F); // optional

        VertexCache[] vertices = new VertexCache[4];

        for (int vi = 0; vi < 4; vi++) {
            vertices[vi] = new VertexCache();
            vertices[vi].x = getFloatValue(quad, vi, posElementIndex, 0);
            vertices[vi].y = getFloatValue(quad, vi, posElementIndex, 1);
            vertices[vi].z = getFloatValue(quad, vi, posElementIndex, 2);
            if (uvElementIndex >= 0) {
                vertices[vi].u = getFloatValue(quad, vi, uvElementIndex, 0);
                vertices[vi].v = getFloatValue(quad, vi, uvElementIndex, 1);
            }
            vertices[vi].clampInBounds(x1, y1, z1, x2, y2, z2);
        }

        // if the position of the vertex is clamped, we need to re-adjust UV according to transformation
        if (!vertices[0].clamped && !vertices[1].clamped && !vertices[2].clamped && !vertices[3].clamped) {
            // nothing changed, return original
            return quad;
        }

        Vector3f v1 = new Vector3f(), v2 = new Vector3f();

        // quick way to test if the resulting quad has any surface area
        // this approach is not perfect, if vertex 1, 0 and 2 are aligned on a straight line,
        // but I think such situation is unlikely to emerge.
        v1.set(
                vertices[1].x - vertices[0].x,
                vertices[1].y - vertices[0].y,
                vertices[1].z - vertices[0].z
        );
        v2.set(
                vertices[2].x - vertices[0].x,
                vertices[2].y - vertices[0].y,
                vertices[2].z - vertices[0].z
        );

        if (Vector3f.cross(v1, v2, v2).lengthSquared() == 0) {
            return null;
        }

        // no UV data to handle
        if (uvElementIndex == -1) {
            MutableBakedQuad mq = new MutableBakedQuad(quad);
            for (int vi = 0; vi < 4; vi++) {
                mq.putData(vi, posElementIndex, vertices[vi].x, vertices[vi].y, vertices[vi].z);
            }
            return mq;
        }

        // calculate barycentric coord for new UV coord
        // or... https://jcgt.org/published/0011/03/04/paper.pdf whatever the hell this is...

        double[] halfTangents = new double[4];
        double[] weights = new double[4];

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            if (!vertices[vertexIndex].clamped) continue;

            VertexCache vertex = vertices[vertexIndex];

            for (int vi = 0; vi < 4; vi++) {
                v1.set(
                        getFloatValue(quad, vi, posElementIndex, 0) - vertex.x,
                        getFloatValue(quad, vi, posElementIndex, 1) - vertex.y,
                        getFloatValue(quad, vi, posElementIndex, 2) - vertex.z
                );
                int nextVertex = (vi + 1) % 4;
                v2.set(
                        getFloatValue(quad, nextVertex, posElementIndex, 0) - vertex.x,
                        getFloatValue(quad, nextVertex, posElementIndex, 1) - vertex.y,
                        getFloatValue(quad, nextVertex, posElementIndex, 2) - vertex.z
                );
                halfTangents[vi] = Math.tan(Vector3f.angle(v1, v2) / 2);
            }

            double weightSum = 0;

            for (int vi = 0; vi < 4; vi++) {
                int nextVertex = (vi + 1) % 4;
                v1.set(
                        getFloatValue(quad, nextVertex, posElementIndex, 0) - vertex.x,
                        getFloatValue(quad, nextVertex, posElementIndex, 1) - vertex.y,
                        getFloatValue(quad, nextVertex, posElementIndex, 2) - vertex.z
                );

                double wgt = (halfTangents[vi] + halfTangents[nextVertex]) / v1.length();

                weights[nextVertex] = wgt;
                weightSum += wgt;
            }

            vertex.u = 0;
            vertex.v = 0;

            for (int vi = 0; vi < 4; vi++) {
                vertex.u += getFloatValue(quad, vi, uvElementIndex, 0) * (weights[vi] / weightSum);
                vertex.v += getFloatValue(quad, vi, uvElementIndex, 1) * (weights[vi] / weightSum);
            }
        }

        for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
            if (!vertices[vertexIndex].clamped) continue;

            VertexCache vertex = vertices[vertexIndex];

            if (Double.isNaN(vertex.u) || Double.isNaN(vertex.v)) {
                GTLog.logger.info("@Tictim <<< laugh at this user!!!!!!");
                break;
            }
        }

        MutableBakedQuad mq = new MutableBakedQuad(quad);
        for (int vi = 0; vi < 4; vi++) {
            mq.putData(vi, posElementIndex, vertices[vi].x, vertices[vi].y, vertices[vi].z);
            mq.putData(vi, uvElementIndex, vertices[vi].u, vertices[vi].v);
        }
        return mq;
    }

    private static float getFloatValue(@Nonnull BakedQuad quad, int vertex, int element, int index) {
        int[] vertexData = quad.getVertexData();

        VertexFormatElement e = quad.getFormat().getElement(element);
        int vertexStart = vertex * quad.getFormat().getSize() + quad.getFormat().getOffset(element);
        int mask = (256 << (8 * (e.getType().getSize() - 1))) - 1;

        int pos = vertexStart + e.getType().getSize() * index;
        int i = pos >> 2;
        int offset = pos & 3;
        int bits = vertexData[i] >>> (offset * 8);
        if ((pos + e.getType().getSize() - 1) / 4 != i) {
            bits |= vertexData[i + 1] << ((4 - offset) * 8);
        }
        bits &= mask;
        return switch (e.getType()) {
            case FLOAT -> Float.intBitsToFloat(bits);
            case UBYTE, USHORT -> (float) bits / mask;
            case UINT -> (float) ((double) (bits & 0xFFFFFFFFL) / 0xFFFFFFFFL);
            case BYTE -> ((float) (byte) bits) / (mask >> 1);
            case SHORT -> ((float) (short) bits) / (mask >> 1);
            case INT -> (float) ((double) (bits & 0xFFFFFFFFL) / (0xFFFFFFFFL >> 1));
        };
    }

    private static int getIntValue(@Nonnull BakedQuad quad, int vertex, int element, int index) {
        int[] vertexData = quad.getVertexData();

        VertexFormatElement e = quad.getFormat().getElement(element);
        int vertexStart = vertex * quad.getFormat().getSize() + quad.getFormat().getOffset(element);
        int mask = (256 << (8 * (e.getType().getSize() - 1))) - 1;

        int pos = vertexStart + e.getType().getSize() * index;
        int i = pos >> 2;
        int offset = pos & 3;
        int bits = vertexData[i] >>> (offset * 8);
        if ((pos + e.getType().getSize() - 1) / 4 != i) {
            bits |= vertexData[i + 1] << ((4 - offset) * 8);
        }
        bits &= mask;
        return bits;
    }

    @Nonnull
    public static String prettyPrintBakedQuad(@Nonnull BakedQuad quad) {
        StringBuilder b = new StringBuilder();
        if (quad.hasTintIndex()) {
            b.append("Tint Index: ").append(quad.getTintIndex()).append("\n");
        }
        b.append("Face: ").append(quad.getFace());
        b.append("\nApply Diffuse Lighting: ").append(quad.shouldApplyDiffuseLighting());
        b.append("\nVertices:");

        for (int vi = 0; vi < 4; vi++) {
            b.append("\n  [").append(vi).append("]");

            for (int ei = 0; ei < quad.getFormat().getElementCount(); ei++) {
                VertexFormatElement element = quad.getFormat().getElement(ei);

                b.append("\n    ").append(ei).append(" - ")
                        .append(element.getUsage().getDisplayName()).append(" :: ");

                for (int i = 0; i < element.getElementCount(); i++) {
                    if (i > 0) b.append(", ");

                    if (element.getType() == VertexFormatElement.EnumType.FLOAT) {
                        b.append(getFloatValue(quad, vi, ei, i));
                    } else {
                        int value = getIntValue(quad, vi, ei, i);
                        b.append(value)
                                .append(" (0x")
                                .append(Integer.toHexString(value).toUpperCase(Locale.ROOT))
                                .append(")");
                    }
                }
            }
        }
        return b.toString();
    }

    private static final class VertexCache {
        float x, y, z, u, v;
        boolean clamped;

        private void clampInBounds(float x1, float y1, float z1, float x2, float y2, float z2) {
            float x = MathHelper.clamp(this.x, x1, x2);
            float y = MathHelper.clamp(this.y, y1, y2);
            float z = MathHelper.clamp(this.z, z1, z2);
            if (this.x != x) {
                this.x = x;
                this.clamped = true;
            }
            if (this.y != y) {
                this.y = y;
                this.clamped = true;
            }
            if (this.z != z) {
                this.z = z;
                this.clamped = true;
            }
        }
    }
}
