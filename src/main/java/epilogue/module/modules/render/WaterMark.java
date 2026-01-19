package epilogue.module.modules.render;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import epilogue.module.Module;
import epilogue.util.RenderUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RoundedUtil;
import epilogue.font.FontRenderer;
import epilogue.font.FontTransformer;
import epilogue.font.CustomFontRenderer;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.ModeValue;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Color;

public class WaterMark extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Exhibition", "Epilogue"});
    public final ModeValue mode1 = new ModeValue("Epilogue Mode", 1, new String[]{"Normal", "Image"}, () -> this.mode.getValue() == 1);
    public final ModeValue mode2 = new ModeValue("Image Mode", 1, new String[]{"Logo", "Text"}, () -> this.mode1.getValue() == 1);
    public final BooleanValue syncInterfaceColor = new BooleanValue("Sync Interface Color", true, () -> mode.getValue() == 1);
    public final FloatValue bgalpha = new FloatValue("Alpha", 0.4f, 0.0f, 1.0f, () -> mode.getValue() == 1 && mode1.getValue() == 0);
    public final FloatValue scale = new FloatValue("Scale", 1.0f, 0.5f, 5.0f, () -> mode.getValue() == 1 && mode1.getValue() == 1);

    private static final ResourceLocation EPILOGUE_LOGO = new ResourceLocation("minecraft", "epilogue/logo/EpilogueLogo.png");
    private static final ResourceLocation EPILOGUE_LOGO_2 = new ResourceLocation("minecraft", "epilogue/mainmenu/EpilogueLogo.png");

    private static void applyHighQualityTextureSampling() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    private float lastWidth = 0.0f;
    private float lastHeight = 0.0f;

    public WaterMark() {
        super("WaterMark", false);
    }

    public void render(float x, float y) {
        render(x, y, 0.0f);
    }

    public void render(float x, float y, float partialTicks) {
        Interface interfaceModule = (Interface) Epilogue.moduleManager.getModule("Interface");
        int fps = Minecraft.getDebugFPS();

        switch (mode.getValue()) {
            case 0:{
                int nColor = interfaceModule != null ? interfaceModule.color(0) : 0xFFFFFF;
                int whiteColor = 0xFFFFFF;
                int grayColor = 0xAAAAAA;

                mc.fontRendererObj.drawStringWithShadow("E", x, y, nColor);
                float nWidth = mc.fontRendererObj.getStringWidth("E");

                mc.fontRendererObj.drawStringWithShadow("pilogue ", x + nWidth, y, whiteColor);
                float nightSkyWidth = mc.fontRendererObj.getStringWidth("Epilogue ");

                mc.fontRendererObj.drawStringWithShadow("[", x + nightSkyWidth, y, grayColor);
                float bracketWidth = mc.fontRendererObj.getStringWidth("[");

                String fpsText = fps + " FPS";
                mc.fontRendererObj.drawStringWithShadow(fpsText, x + nightSkyWidth + bracketWidth, y, whiteColor);
                float fpsWidth = mc.fontRendererObj.getStringWidth(fpsText);

                mc.fontRendererObj.drawStringWithShadow("]", x + nightSkyWidth + bracketWidth + fpsWidth, y, grayColor);

                lastWidth = nightSkyWidth + bracketWidth + fpsWidth + mc.fontRendererObj.getStringWidth("]");
                lastHeight = mc.fontRendererObj.FONT_HEIGHT;
                break;
            }

            case 1:{
                switch (mode1.getValue()) {
                    case 0:{
                        float s = 1.3f;
                        float h = 14.0f * s;
                        float padX = 6.0f * s;
                        float padY = 3.0f * s;
                        float icon = 9.0f * s;
                        float sepW = 1.0f;
                        float spacing = 5.0f * s;

                        float gapRadius = 1.5f * s;

                        String client = Epilogue.clientName;
                        String fpsText = Minecraft.getDebugFPS() + " fps";
                        String user = mc.thePlayer != null ? mc.thePlayer.getName() : "User";

                        FontTransformer ft = FontTransformer.getInstance();
                        java.awt.Font clientFont = ft.getFont("NunitoBold", 33.1f);
                        java.awt.Font infoFont = ft.getFont("NunitoLight", 34.5f);

                        float clientW = clientFont != null ? CustomFontRenderer.getStringWidth(client, clientFont) : FontRenderer.getStringWidth(client);
                        float fpsW = infoFont != null ? CustomFontRenderer.getStringWidth(fpsText, infoFont) : FontRenderer.getStringWidth(fpsText);
                        float userW = infoFont != null ? CustomFontRenderer.getStringWidth(user, infoFont) : FontRenderer.getStringWidth(user);

                        float clientH = clientFont != null ? CustomFontRenderer.getFontHeight(clientFont) : FontRenderer.getFontHeight();
                        float infoH = infoFont != null ? CustomFontRenderer.getFontHeight(infoFont) : FontRenderer.getFontHeight();

                        float clientTextY = y + (h - clientH) / 2.0f + 1.6f;
                        float infoTextY = y + (h - infoH) / 2.0f + 2.4f;

                        float w = padX + s + spacing + icon + spacing + clientW + spacing + sepW + spacing + fpsW + spacing + sepW + spacing + userW + padX;

                        float smallR = 2.5f * s;
                        float largeR = 6.0f * s;
                        int bgA = (int) (Math.max(0.0f, Math.min(1.0f, bgalpha.getValue())) * 255.0f);
                        Color bgCol = new Color(20, 20, 20, bgA);

                        Runnable bgMask = () -> RoundedUtil.drawRound(x, y, w, h, new Color(255, 255, 255, 255), 24, smallR, largeR, largeR, smallR);

                        PostProcessing.drawBlur(x, y, x + w, y + h, () -> bgMask);

                        java.util.function.Consumer<Boolean> drawNormal = (bloomPass) -> {
                            GlStateManager.enableBlend();
                            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                            GlStateManager.enableAlpha();

                            float bloomShiftX = bloomPass ? 1.0f : 0.0f;

                            if (!bloomPass) {
                                RoundedUtil.drawRound(x, y, w, h, bgCol, 24, smallR, largeR, largeR, smallR);
                            } else {
                                int bloomTint = epilogue.module.modules.render.PostProcessing.getBloomColor(0);
                                RoundedUtil.drawRound(x, y, w, h, new Color(bloomTint, true), 24, smallR, largeR, largeR, smallR);
                            }

                            float cx = x + bloomShiftX;
                            int main = interfaceModule != null ? interfaceModule.getMainColor().getRGB() : 0xFFFFFFFF;
                            int dynamicGap = interfaceModule != null ? interfaceModule.color(0) : main;
                            int bloomTint = epilogue.module.modules.render.PostProcessing.getBloomColor(0);
                            int aMain = bloomPass ? 255 : 220;
                            int gapCol = ColorUtil.swapAlpha(bloomPass ? bloomTint : dynamicGap, aMain);

                            GlStateManager.disableTexture2D();
                            RoundedUtil.drawRound(cx, y + padY, s, h - padY * 2.0f, gapRadius, new Color(gapCol, true));
                            GlStateManager.enableTexture2D();

                            cx += s + spacing;

                            int iconCol = ColorUtil.swapAlpha(bloomPass ? bloomTint : 0xFFFFFFFF, bloomPass ? 255 : 230);

                            int tex = mc.getTextureManager().getTexture(EPILOGUE_LOGO).getGlTextureId();
                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                            int prevMin = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
                            int prevMag = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

                            RenderUtil.drawImage(EPILOGUE_LOGO, cx, y + (h - icon) / 2.0f, cx + icon, y + (h - icon) / 2.0f + icon,
                                    iconCol, iconCol, iconCol, iconCol);

                            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, prevMin);
                            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, prevMag);

                            cx += icon + spacing;

                            int clientCol = ColorUtil.swapAlpha(bloomPass ? bloomTint : main, bloomPass ? 255 : 230);
                            if (clientFont != null) {
                                if (!bloomPass && syncInterfaceColor.getValue() && interfaceModule != null) {
                                    float tx = cx;
                                    for (int i = 0; i < client.length(); i++) {
                                        String ch = String.valueOf(client.charAt(i));
                                        int cc = ColorUtil.swapAlpha(interfaceModule.color(i), 255);
                                        CustomFontRenderer.drawStringWithShadow(ch, tx, clientTextY, cc, clientFont);
                                        tx += CustomFontRenderer.getStringWidth(ch, clientFont);
                                    }
                                } else {
                                    CustomFontRenderer.drawStringWithShadow(client, cx, clientTextY, clientCol, clientFont);
                                }
                            } else {
                                FontRenderer.drawStringWithShadow(client, cx, clientTextY, clientCol);
                            }
                            cx += clientW + spacing;

                            int sepCol = ColorUtil.swapAlpha(bloomPass ? bloomTint : 0xFFFFFFFF, bloomPass ? 220 : 140);

                            GlStateManager.disableTexture2D();
                            net.minecraft.client.gui.Gui.drawRect((int) cx, (int) (y + padY), (int) cx + 1, (int) (y + h - padY), sepCol);
                            GlStateManager.enableTexture2D();
                            cx += sepW + spacing;

                            int textCol = ColorUtil.swapAlpha(bloomPass ? bloomTint : 0xFFFFFFFF, bloomPass ? 255 : 230);
                            if (infoFont != null) {
                                CustomFontRenderer.drawStringWithShadow(fpsText, cx, infoTextY, textCol, infoFont);
                            } else {
                                FontRenderer.drawStringWithShadow(fpsText, cx, infoTextY, textCol);
                            }

                            cx += fpsW + spacing;

                            GlStateManager.disableTexture2D();
                            net.minecraft.client.gui.Gui.drawRect((int) cx, (int) (y + padY), (int) cx + 1, (int) (y + h - padY), sepCol);
                            GlStateManager.enableTexture2D();
                            cx += sepW + spacing;

                            if (infoFont != null) {
                                CustomFontRenderer.drawStringWithShadow(user, cx, infoTextY, textCol, infoFont);
                            } else {
                                FontRenderer.drawStringWithShadow(user, cx, infoTextY, textCol);
                            }

                            if (!bloomPass) {
                                GlStateManager.disableBlend();
                            }
                        };

                        Framebuffer bloomBuffer = PostProcessing.beginBloom();
                        if (bloomBuffer != null) {
                            drawNormal.accept(true);
                            PostProcessing.endBloom(bloomBuffer);
                        }
                        drawNormal.accept(false);

                        lastWidth = w;
                        lastHeight = h;
                        break;
                    }

                    case 1:{
                        switch (mode2.getValue()){
                            case 0:{
                                float baseSize = 48.0f;
                                float s = scale.getValue();

                                float a = 1.0f;
                                float size = baseSize * s;

                                float x1 = x + size;
                                float y1 = y + size;

                                java.util.function.Consumer<Boolean> drawLogoColored = (bloomPass) -> {
                                    if (syncInterfaceColor.getValue() && interfaceModule != null) {
                                        float sweepSpeed = 0.03125f;
                                        int aaBase = (int) (a * 255.0f);
                                        int aa = bloomPass ? 255 : aaBase;
                                        float time = (mc.thePlayer != null ? (mc.thePlayer.ticksExisted + partialTicks) : (System.currentTimeMillis() * 0.001f));

                                        int main = interfaceModule.getMainColor().getRGB();
                                        int second = interfaceModule.getSecondColor().getRGB();
                                        float cycles = 1.5f;
                                        float edge = 0.18f;
                                        float flow = time * (sweepSpeed * 0.125f);

                                        mc.getTextureManager().bindTexture(EPILOGUE_LOGO);
                                        GL11.glShadeModel(GL11.GL_SMOOTH);
                                        GlStateManager.enableTexture2D();
                                        GlStateManager.enableBlend();
                                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                                        GlStateManager.enableAlpha();

                                        GlStateManager.resetColor();
                                        mc.getTextureManager().bindTexture(EPILOGUE_LOGO);
                                        GlStateManager.enableTexture2D();

                                        int strips = 48;
                                        float stripH = size / strips;

                                        ScaledResolution sr = new ScaledResolution(mc);
                                        int factor = Math.max(1, sr.getScaleFactor());

                                        GL11.glEnable(GL11.GL_SCISSOR_TEST);
                                        for (int i = 0; i < strips; i++) {
                                            float yTop = y + i * stripH;
                                            float yBot = yTop + stripH + 0.35f;

                                            float vy = (yTop - y) / size;
                                            float xCenter = ((float) Math.sin((time * sweepSpeed) * 0.35f + vy * 2.0f) * 0.5f + 0.5f);
                                            float diag = (xCenter + vy) * 0.5f;
                                            float p = diag * cycles - flow;
                                            float f = p - (float) Math.floor(p);

                                            float tri = 1.0f - Math.abs(f * 2.0f - 1.0f);
                                            float t = Math.max(0.0f, Math.min(1.0f, (tri - (0.5f - edge)) / (2.0f * edge)));

                                            int col = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, t), aa);

                                            int sx = (int) (x * factor);
                                            int sy = (int) (yTop * factor);
                                            int sw = (int) (size * factor);
                                            int sh = (int) ((yBot - yTop) * factor);
                                            if (sw <= 0 || sh <= 0) continue;

                                            GL11.glScissor(sx, mc.displayHeight - (sy + sh), sw, sh);

                                            float shift = (float) Math.sin((diag) * (float) Math.PI * 2.0f + time * 0.6f * sweepSpeed * 2.0f) * 0.03f;
                                            int c1 = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, Math.max(0.0f, Math.min(1.0f, xCenter + shift))), aa);
                                            int c2 = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, Math.max(0.0f, Math.min(1.0f, xCenter - shift))), aa);

                                            RenderUtil.drawImage(EPILOGUE_LOGO, x, y, x1, y1, c1, col, c2, col);
                                        }
                                        GL11.glDisable(GL11.GL_SCISSOR_TEST);
                                    } else {
                                        int white = ColorUtil.swapAlpha(0xFFFFFFFF, (int) (a * 255.0f));
                                        RenderUtil.drawImage(EPILOGUE_LOGO, x, y, x1, y1, white, white, white, white);
                                    }
                                };
                                Framebuffer bloomBuffer = PostProcessing.beginBloom();
                                if (bloomBuffer != null) {
                                    drawLogoColored.accept(true);
                                    PostProcessing.endBloom(bloomBuffer);
                                }
                                GlStateManager.enableBlend();
                                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                                GlStateManager.enableAlpha();
                                drawLogoColored.accept(false);
                                GlStateManager.disableBlend();
                                lastWidth = size;
                                lastHeight = size;
                                break;
                            }

                            case 1:{
                                float baseSize = 48.0f;
                                float s = scale.getValue();

                                float a = 1.0f;
                                float size = baseSize * s;
                                float logoAspect = 2730.0f / 1535.0f;
                                float drawW = size;
                                float drawH = size / logoAspect;

                                float x1 = x + drawW;
                                float y1 = y + drawH;

                                java.util.function.Consumer<Boolean> drawLogoColored = (bloomPass) -> {
                                    if (syncInterfaceColor.getValue() && interfaceModule != null) {

                                        float sweepSpeed = 0.03125f;
                                        int aaBase = (int) (a * 255.0f);
                                        int aa = bloomPass ? 255 : aaBase;
                                        float time = (mc.thePlayer != null ? (mc.thePlayer.ticksExisted + partialTicks) : (System.currentTimeMillis() * 0.001f));

                                        int main = interfaceModule.getMainColor().getRGB();
                                        int second = interfaceModule.getSecondColor().getRGB();
                                        float cycles = 1.5f;
                                        float edge = 0.18f;
                                        float flow = time * (sweepSpeed * 0.125f);

                                        mc.getTextureManager().bindTexture(EPILOGUE_LOGO_2);
                                        applyHighQualityTextureSampling();

                                        GL11.glShadeModel(GL11.GL_SMOOTH);
                                        GlStateManager.enableTexture2D();
                                        GlStateManager.enableBlend();
                                        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                                        GlStateManager.enableAlpha();

                                        GlStateManager.resetColor();
                                        mc.getTextureManager().bindTexture(EPILOGUE_LOGO_2);
                                        GlStateManager.enableTexture2D();

                                        int strips = 48;
                                        float stripH = size / strips;

                                        ScaledResolution sr = new ScaledResolution(mc);
                                        int factor = Math.max(1, sr.getScaleFactor());

                                        GL11.glEnable(GL11.GL_SCISSOR_TEST);
                                        for (int i = 0; i < strips; i++) {
                                            float yTop = y + i * stripH;
                                            float yBot = yTop + stripH + 0.35f;

                                            float vy = (yTop - y) / size;
                                            float xCenter = ((float) Math.sin((time * sweepSpeed) * 0.35f + vy * 2.0f) * 0.5f + 0.5f);
                                            float diag = (xCenter + vy) * 0.5f;
                                            float p = diag * cycles - flow;
                                            float f = p - (float) Math.floor(p);

                                            float tri = 1.0f - Math.abs(f * 2.0f - 1.0f);
                                            float t = Math.max(0.0f, Math.min(1.0f, (tri - (0.5f - edge)) / (2.0f * edge)));

                                            int col = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, t), aa);

                                            int sx = (int) (x * factor);
                                            int sy = (int) (yTop * factor);
                                            int sw = (int) (size * factor);
                                            int sh = (int) ((yBot - yTop) * factor);
                                            if (sw <= 0 || sh <= 0) continue;

                                            GL11.glScissor(sx, mc.displayHeight - (sy + sh), sw, sh);

                                            float shift = (float) Math.sin((diag) * (float) Math.PI * 2.0f + time * 0.6f * sweepSpeed * 2.0f) * 0.03f;
                                            int c1 = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, Math.max(0.0f, Math.min(1.0f, xCenter + shift))), aa);
                                            int c2 = ColorUtil.swapAlpha(ColorUtil.fadeBetween(main, second, Math.max(0.0f, Math.min(1.0f, xCenter - shift))), aa);

                                            if (!bloomPass) {
                                                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                                            } else {
                                                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                            }
                                            RenderUtil.drawImage(EPILOGUE_LOGO_2, x, y, x1, y1, c1, col, c2, col);
                                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                        }
                                        GL11.glDisable(GL11.GL_SCISSOR_TEST);
                                    } else {
                                        int aa = (int) (a * 255.0f);
                                        int white = ColorUtil.swapAlpha(0xFFFFFFFF, aa);
                                        GlStateManager.enableBlend();
                                        if (!bloomPass) {
                                            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
                                        } else {
                                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                        }
                                        mc.getTextureManager().bindTexture(EPILOGUE_LOGO_2);
                                        applyHighQualityTextureSampling();
                                        RenderUtil.drawImage(EPILOGUE_LOGO_2, x, y, x1, y1, white, white, white, white);
                                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                                        if (!bloomPass) {
                                            int ghost = ColorUtil.swapAlpha(0xFFFFFFFF, (int) (aa * 0.55f));
                                            RenderUtil.drawImage(EPILOGUE_LOGO_2, x, y, x1, y1, ghost, ghost, ghost, ghost);
                                        }
                                    }
                                };
                                GlStateManager.enableBlend();
                                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                                GlStateManager.enableAlpha();
                                drawLogoColored.accept(false);
                                GlStateManager.disableBlend();
                                lastWidth = drawW;
                                lastHeight = drawH;
                                break;
                            }
                        }
                        break;
                    }
                }
                break;
            }
        }
    }

    public float getLastWidth() {
        return lastWidth <= 0 ? 140f : lastWidth;
    }

    public float getLastHeight() {
        return lastHeight <= 0 ? 12f : lastHeight;
    }
}