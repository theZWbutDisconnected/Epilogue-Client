package epilogue.module.modules.render;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import epilogue.module.Module;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.RoundedUtil;
import epilogue.font.FontRenderer;
import epilogue.util.render.animations.Translate;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.PostProcessing;

import java.awt.*;
import java.util.Comparator;

public class ArrayList extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    private float lastWidth = 0.0f;
    private float lastHeight = 0.0f;

    private float hotKeyHeaderAnim = 0.0f;
    private long hotKeyHeaderAnimLastMs = 0L;
    private float hotKeyHeaderWAnim = 0.0f;
    private float hotKeyHeaderWTarget = 0.0f;

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Normal", "HotKey"});
    public final ModeValue animation = new ModeValue("Animation", 2, new String[]{"Scale In", "Move In", "Slide In"});
    public final ModeValue rectangleValue = new ModeValue("Rectangle", 1, new String[]{"None", "Top", "Side"}, () -> this.mode.getValue() == 0);
    public final BooleanValue backgroundValue = new BooleanValue("Back Ground", true);
    public final IntValue bgAlpha = new IntValue("Back Ground Alpha", 40, 1, 255);
    public final IntValue round = new IntValue("Round", 0, 0, 12, () -> this.mode.getValue() == 0);
    public final FloatValue textHeight = new FloatValue("Text Height", 5f, 0f, 10f, () -> this.mode.getValue() == 0);
    public final FloatValue textOffset = new FloatValue("Text Offset", 2.5f, -6f, 6f, () -> this.mode.getValue() == 0);

    public final ModeValue hotKeyIconStyle = new ModeValue("HotKey Icon", 0, new String[]{"Category", "Toggle"}, () -> this.mode.getValue() == 1);
    public final ModeValue hotKeyBgColor = new ModeValue("HotKey BG", 0, new String[]{"Dark", "Synced"}, () -> this.mode.getValue() == 1);
    public final BooleanValue hotKeyHeader = new BooleanValue("HotKey Header", false, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyCount = new FloatValue("HotKey Count", 1.7f, 1.4f, 2.5f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyRadius = new FloatValue("HotKey Radius", 5.3f, 0.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyTextOffset = new FloatValue("HotKey Text Offset", 0.12413793f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyIconOffset = new FloatValue("HotKey Icon Offset", 1.6f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyCombatIconOffsetX = new FloatValue("Combat Icon Offset X", -0.4f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyMovementIconOffsetX = new FloatValue("Movement Icon Offset X", -1.1f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyPlayerIconOffsetX = new FloatValue("Player Icon Offset X", 0.0f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyRenderIconOffsetX = new FloatValue("Render Icon Offset X", -0.7f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final FloatValue hotKeyMiscIconOffsetX = new FloatValue("Misc Icon Offset X", 0.0f, -6.0f, 6.0f, () -> this.mode.getValue() == 1);
    public final BooleanValue hotKeyTextShadow = new BooleanValue("HotKey Text Shadow", true, () -> this.mode.getValue() == 1);
    public final BooleanValue hotKeyIconShadow = new BooleanValue("HotKey Icon Shadow", false, () -> this.mode.getValue() == 1);
    public final BooleanValue bugGlow = new BooleanValue("Bug Glow", false);

    public ArrayList() {
        super("Arraylist", true);
    }

    private void drawBottomRoundedRow(float x, float y, float w, float h, float radius, Color color) {
        if (w <= 0.0f || h <= 0.0f) return;
        float r = Math.max(0.0f, Math.min(radius, Math.min(w, h) / 2.0f));
        if (r <= 0.0f) {
            RenderUtil.drawRect(x, y, w, h, color.getRGB());
            return;
        }

        RenderUtil.drawRect(x, y, w, Math.max(0.0f, h - r), color.getRGB());
        RenderUtil.drawRect(x + r, y + h - r, Math.max(0.0f, w - 2.0f * r), r, color.getRGB());
        RoundedUtil.drawRound(x, y + h - 2.0f * r, 2.0f * r, 2.0f * r, r, color);
        RoundedUtil.drawRound(x + w - 2.0f * r, y + h - 2.0f * r, 2.0f * r, 2.0f * r, r, color);
    }

    private String getFormattedTag(String tag) {
        if (tag == null || tag.isEmpty()) return "";
        return tag;
    }

    public void renderAt(float x, float y) {
        ScaledResolution sr = new ScaledResolution(mc);
        if (!bugGlow.getValue()) {
            if (mode.getValue() == 1) {
                hotKeyRender(sr, x, y);
            } else {
                moduleList(sr, x, y);
            }
            return;
        }

        boolean prevForced = epilogue.util.render.PostProcessing.isInternalForcedPostProcessing();
        boolean prevSuppress = epilogue.util.render.PostProcessing.isInternalBloomSuppressed();
        try {
            epilogue.util.render.PostProcessing.setInternalForcedPostProcessing(true);
            epilogue.util.render.PostProcessing.setInternalBloomSuppressed(false);
            if (mode.getValue() == 1) {
                hotKeyRender(sr, x, y);
            } else {
                moduleList(sr, x, y);
            }
        } finally {
            epilogue.util.render.PostProcessing.setInternalForcedPostProcessing(prevForced);
            epilogue.util.render.PostProcessing.setInternalBloomSuppressed(prevSuppress);
        }

        if (mode.getValue() == 1) {
            hotKeyRender(sr, x, y);
        } else {
            moduleList(sr, x, y);
        }
    }

    public float getLastWidth() {
        return lastWidth <= 0 ? 110f : lastWidth;
    }

    public float getLastHeight() {
        return lastHeight <= 0 ? 140f : lastHeight;
    }

    private int hotKeyBgColor(epilogue.module.modules.render.Interface interfaceModule, int idx) {
        if (hotKeyBgColor.getModeString().equals("Synced") && interfaceModule != null) {
            return ColorUtil.swapAlpha(interfaceModule.color(idx), bgAlpha.getValue());
        }
        return new Color(21, 21, 21, bgAlpha.getValue()).getRGB();
    }

    private String categoryIcon(Module module) {
        if (module == null) return "A";
        epilogue.module.ModuleCategory cat = module.getCategory();
        if (cat == null) return "A";
        switch (cat) {
            case COMBAT:
                return "A";
            case MOVEMENT:
                return "B";
            case PLAYER:
                return "D";
            case RENDER:
                return "C";
            case MISC:
            default:
                return "E";
        }
    }

    private float categoryIconOffsetX(Module module) {
        if (module == null || module.getCategory() == null) return 0.0f;
        switch (module.getCategory()) {
            case COMBAT:
                return hotKeyCombatIconOffsetX.getValue();
            case MOVEMENT:
                return hotKeyMovementIconOffsetX.getValue();
            case PLAYER:
                return hotKeyPlayerIconOffsetX.getValue();
            case RENDER:
                return hotKeyRenderIconOffsetX.getValue();
            case MISC:
            default:
                return hotKeyMiscIconOffsetX.getValue();
        }
    }

    private void hotKeyRender(ScaledResolution sr, float baseX, float baseY) {
        epilogue.module.modules.render.Interface interfaceModule = (epilogue.module.modules.render.Interface) Epilogue.moduleManager.getModule("Interface");

        java.util.ArrayList<float[]> hotKeyBgRects = new java.util.ArrayList<>();

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        int count = 0;
        int counts = 0;
        float hight = 13f;

        boolean flip = baseX <= sr.getScaledWidth() / 2f;

        int renderx;
        int rendery;

        float headerYOffset = 2.0f;
        float headerX = 0.0f;
        float headerY = 0.0f;
        float headerW = 88.0f;
        boolean headerEnabled = hotKeyHeader.getValue();

        float headerMinX = Float.MAX_VALUE;
        float headerMinY = Float.MAX_VALUE;
        float headerMaxX = -Float.MAX_VALUE;
        float headerMaxY = -Float.MAX_VALUE;

        Comparator<Module> sort = (m1, m2) -> {
            double ab = FontRenderer.getStringWidth(m1.getName() + m1.getTag());
            double bb = FontRenderer.getStringWidth(m2.getName() + m2.getTag());
            return Double.compare(bb, ab);
        };

        java.util.ArrayList<Module> enabledMods = new java.util.ArrayList<>(Epilogue.moduleManager.modules.values());
        enabledMods.sort(sort);

        long nowMs = System.currentTimeMillis();
        if (hotKeyHeaderAnimLastMs == 0L) hotKeyHeaderAnimLastMs = nowMs;
        float dt = (nowMs - hotKeyHeaderAnimLastMs) / 1000.0f;
        hotKeyHeaderAnimLastMs = nowMs;
        float speed = 1.0f / 0.15f;
        if (headerEnabled) {
            hotKeyHeaderAnim = Math.min(1.0f, hotKeyHeaderAnim + dt * speed);
        } else {
            hotKeyHeaderAnim = Math.max(0.0f, hotKeyHeaderAnim - dt * speed);
        }

        int renderxHeader = (int) baseX + (flip ? 60 : 17);
        int baseRenderyNoHeader = (int) baseY + 6;
        int baseRenderyHeader = (int) (baseY + 24 + headerYOffset);

        renderx = renderxHeader;
        int baseRendery = (int) (baseRenderyNoHeader + (baseRenderyHeader - baseRenderyNoHeader) * hotKeyHeaderAnim);
        rendery = baseRendery;

        float topBgW = 0.0f;
        float topBgFullRightX = 0.0f;
        if (hotKeyHeaderAnim > 0.001f) {
            for (Module m : enabledMods) {

                Animation a = m.getAnimation();
                a.setDirection(m.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!m.isEnabled() && a.finished(Direction.BACKWARDS)) continue;
                if (m.isHidden()) continue;

                String t = m.getName() + m.getTag();
                float anim = (float) a.getOutput();
                float fullW = (float) (FontRenderer.getStringWidth(t) + 6);
                float w = fullW * anim;
                topBgW = Math.max(0.0f, w);

                int x;
                if (flip) {
                    x = renderx - 38;
                } else {
                    x = renderx - FontRenderer.getStringWidth(t) + 52;
                }

                float maskXFull = x - 2;
                float maskIconXFull = x + fullW + 2;
                topBgFullRightX = maskIconXFull + 13.0f;
                break;
            }

            float headerContentMinW = (float) (epilogue.ui.clickgui.menu.Fonts.width(epilogue.ui.clickgui.menu.Fonts.small(), "HotKeys") + 18);
            hotKeyHeaderWTarget = Math.max(28.0f, Math.max(headerContentMinW, topBgW * (2.0f / 3.0f)));
            if (hotKeyHeaderWAnim <= 0.0f) hotKeyHeaderWAnim = hotKeyHeaderWTarget;
            float wSpeed = 1.0f / 0.15f;
            float wT = Math.min(1.0f, dt * wSpeed);
            hotKeyHeaderWAnim += (hotKeyHeaderWTarget - hotKeyHeaderWAnim) * wT;
            headerW = hotKeyHeaderWAnim;

            if (flip) {
                int xOffset = -43;
                headerX = renderx - 15 + xOffset;
            } else {
                float baseHeaderX = renderx - 15;
                float rightEdge = topBgFullRightX > 0.0f ? topBgFullRightX : (baseHeaderX + headerW);
                headerX = rightEdge - headerW;
            }
            float headerYHeader = baseRenderyHeader - 23;
            headerY = headerYHeader;

            headerMinX = Math.min(headerMinX, headerX);
            headerMinY = Math.min(headerMinY, headerY);
            float headerH = 13.0f * hotKeyHeaderAnim;
            headerMaxX = Math.max(headerMaxX, headerX + headerW);
            headerMaxY = Math.max(headerMaxY, headerY + headerH);

            hotKeyBgRects.add(new float[]{headerX, headerY, headerW, headerH});
            minX = Math.min(minX, headerX);
            minY = Math.min(minY, headerY);
            maxX = Math.max(maxX, headerX + headerW);
            maxY = Math.max(maxY, headerY + headerH);
        }

        for (Module module : enabledMods) {
            counts++;
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            if (module.isHidden()) continue;

            String nameText = module.getName();
            String tagText = module.getTag();
            if (tagText == null) tagText = "";
            String displayText = nameText + tagText;

            int x;
            if (flip) {
                x = renderx - 38;
            } else {
                int textWidth = FontRenderer.getStringWidth(displayText);
                x = renderx - textWidth + 52;
            }

            float anim = (float) moduleAnimation.getOutput();
            int width = FontRenderer.getStringWidth(displayText) + 6;

            float ry = rendery + count - 4;

            float maskW = width * anim;
            float maskIconW = 13f * anim;
            float maskX = flip ? (x - 2) : (x - 2 + (width - maskW));
            float maskIconX = flip ? (renderx - 58 + (13f - maskIconW)) : (x + width + 2);

            if (anim > 0.001f) {
                hotKeyBgRects.add(new float[]{maskX, ry, maskW, hight});
                minX = Math.min(minX, maskX);
                minY = Math.min(minY, ry);
                maxX = Math.max(maxX, maskX + maskW);
                maxY = Math.max(maxY, ry + hight);

                hotKeyBgRects.add(new float[]{maskIconX, ry, maskIconW, hight});
                minX = Math.min(minX, maskIconX);
                minY = Math.min(minY, ry);
                maxX = Math.max(maxX, maskIconX + maskIconW);
                maxY = Math.max(maxY, ry + hight);
            }

            count += (int) (moduleAnimation.getOutput() * (hight * hotKeyCount.getValue()));
        }

        if (!hotKeyBgRects.isEmpty() && minX < maxX && minY < maxY) {
            PostProcessing.drawBlur(minX, minY, maxX, maxY, () -> () -> {
                GlStateManager.enableBlend();
                GlStateManager.disableTexture2D();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                    for (float[] rect : hotKeyBgRects) {
                        RoundedUtil.drawRound(rect[0], rect[1], rect[2], rect[3], hotKeyRadius.getValue(), new Color(-1, true));
                    }
                });
                GlStateManager.enableTexture2D();
                GlStateManager.disableBlend();
            });

            Framebuffer bloomBuffer = PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                int index = 0;
                for (float[] rect : hotKeyBgRects) {
                    int color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                            ? interfaceModule.color(index)
                            : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                    RoundedUtil.drawRound(rect[0], rect[1], rect[2], rect[3], hotKeyRadius.getValue(), new Color(ColorUtil.swapAlpha(color, 255), true));
                    index++;
                }
                PostProcessing.endBloom(bloomBuffer);
            }
        }

        counts = 0;
        count = 0;

        for (Module module : enabledMods) {
            counts++;

            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            if (module.isHidden()) continue;

            String nameText = module.getName();
            String tagText = module.getTag();
            if (tagText == null) tagText = "";
            String displayText = nameText + tagText;

            int x;
            if (flip) {
                x = renderx - 38;
            } else {
                int textWidth = FontRenderer.getStringWidth(displayText);
                x = renderx - textWidth + 52;
            }

            float anim = (float) moduleAnimation.getOutput();
            int width = FontRenderer.getStringWidth(displayText) + 6;

            float drawW = width * anim;
            float drawX = flip ? (x - 2) : (x - 2 + (width - drawW));

            int bg = hotKeyBgColor(interfaceModule, counts);
            Color bgC = new Color(bg, true);

            if (anim > 0.001f) {
                RoundedUtil.drawRound(drawX, rendery + count - 4, drawW, hight, hotKeyRadius.getValue(), bgC);
            }

            float iconW = 13f * anim;
            int baseIconX = flip ? renderx - 58 : x + width + 2;
            float iconX = flip ? (baseIconX + (13f - iconW)) : baseIconX;

            if (anim > 0.001f) {
                RoundedUtil.drawRound(iconX, rendery + count - 4, iconW, hight, hotKeyRadius.getValue(), bgC);
            }

            String icon = categoryIcon(module);
            float xOffset = !icon.isEmpty() ? (icon.charAt(0) == 'B' ? 2.95f : (icon.charAt(0) == 'G' ? 1.0f : 2.0f)) : 2.0f;
            float iconOffsetX = categoryIconOffsetX(module);

            float textY = rendery + count - 1 + hotKeyTextOffset.getValue();
            float textX = drawX + 3;
            int alpha = (int) Math.max(0.0f, Math.min(255.0f, (float) (255.0f * anim)));
            int nameBase = interfaceModule != null ? interfaceModule.color(counts) : -1;
            int nameColor = ColorUtil.swapAlpha(nameBase, alpha);
            int tagColor = ColorUtil.swapAlpha(0xFF888888, alpha);

            if (hotKeyTextShadow.getValue()) {
                FontRenderer.drawStringWithShadow(nameText, textX, textY, nameColor);
                if (!tagText.isEmpty()) {
                    float nameW = FontRenderer.getStringWidth(nameText);
                    FontRenderer.drawStringWithShadow(tagText, textX + nameW, textY, tagColor);
                }
            } else {
                FontRenderer.drawString(nameText, textX, textY, nameColor);
                if (!tagText.isEmpty()) {
                    float nameW = FontRenderer.getStringWidth(nameText);
                    FontRenderer.drawString(tagText, textX + nameW, textY, tagColor);
                }
            }

            if (anim > 0.001f) {
                if (hotKeyIconStyle.getModeString().equals("Category")) {
                    if (hotKeyIconShadow.getValue()) {
                        epilogue.ui.clickgui.menu.Fonts.drawWithShadow(epilogue.ui.clickgui.menu.Fonts.icon(), icon, iconX + xOffset + iconOffsetX, textY + hotKeyIconOffset.getValue(), nameColor);
                    } else {
                        epilogue.ui.clickgui.menu.Fonts.draw(epilogue.ui.clickgui.menu.Fonts.icon(), icon, iconX + xOffset + iconOffsetX, textY + hotKeyIconOffset.getValue(), nameColor);
                    }
                } else {
                    if (hotKeyIconShadow.getValue()) {
                        epilogue.ui.clickgui.menu.Fonts.drawWithShadow(epilogue.ui.clickgui.menu.Fonts.icon(), "X", iconX + 1.8f, textY + hotKeyIconOffset.getValue(), nameColor);
                    } else {
                        epilogue.ui.clickgui.menu.Fonts.draw(epilogue.ui.clickgui.menu.Fonts.icon(), "X", iconX + 1.8f, textY + hotKeyIconOffset.getValue(), nameColor);
                    }
                }
            }

            count += (int) (moduleAnimation.getOutput() * (hight * hotKeyCount.getValue()));
        }

        if (hotKeyHeaderAnim > 0.001f) {
            int bg = hotKeyBgColor(interfaceModule, 0);
            float headerH = 13.0f * hotKeyHeaderAnim;
            Color bgC = new Color(bg, true);
            RoundedUtil.drawRound(headerX, headerY, headerW, headerH, hotKeyRadius.getValue(), bgC);
            int headerAlpha = (int) Math.max(0.0f, Math.min(255.0f, hotKeyHeaderAnim * 255.0f));
            if (hotKeyTextShadow.getValue()) {
                epilogue.ui.clickgui.menu.Fonts.drawWithShadow(epilogue.ui.clickgui.menu.Fonts.small(), "HotKeys", headerX + 14, (float) (headerY + 4.5), ColorUtil.swapAlpha(-1, headerAlpha));
            } else {
                epilogue.ui.clickgui.menu.Fonts.draw(epilogue.ui.clickgui.menu.Fonts.small(), "HotKeys", headerX + 14, (float) (headerY + 4.5), ColorUtil.swapAlpha(-1, headerAlpha));
            }

            int headerIconColor = interfaceModule != null ? interfaceModule.color(0) : -1;
            headerIconColor = ColorUtil.swapAlpha(headerIconColor, headerAlpha);
            if (hotKeyIconShadow.getValue()) {
                epilogue.ui.clickgui.menu.Fonts.drawWithShadow(epilogue.ui.clickgui.menu.Fonts.icon(), "W", headerX + 2, (float) (headerY + 4.5), headerIconColor);
            } else {
                epilogue.ui.clickgui.menu.Fonts.draw(epilogue.ui.clickgui.menu.Fonts.icon(), "W", headerX + 2, (float) (headerY + 4.5), headerIconColor);
            }
        }

        float finalMinX = minX;
        float finalMinY = minY;
        float finalMaxX = maxX;
        float finalMaxY = maxY;

        if (hotKeyHeaderAnim > 0.001f && headerMinX < headerMaxX && headerMinY < headerMaxY) {
            finalMinX = Math.min(finalMinX, headerMinX);
            finalMinY = Math.min(finalMinY, headerMinY);
            finalMaxX = Math.max(finalMaxX, headerMaxX);
            finalMaxY = Math.max(finalMaxY, headerMaxY);
        }

        if (!hotKeyBgRects.isEmpty() && finalMinX < finalMaxX && finalMinY < finalMaxY) {
            lastWidth = finalMaxX - finalMinX;
            lastHeight = finalMaxY - finalMinY;
        } else if (hotKeyHeaderAnim > 0.001f && headerMinX < headerMaxX && headerMinY < headerMaxY) {
            lastWidth = headerMaxX - headerMinX;
            lastHeight = headerMaxY - headerMinY;
        } else {
            lastWidth = 0.0f;
            lastHeight = 0.0f;
        }
    }

    public void moduleList(ScaledResolution sr, float x, float y) {
        int count = 1;
        float fontHeight = textHeight.getValue();
        float yValue = y;

        boolean alignRight = x >= (sr.getScaledWidth() / 2.0f);
        int screenWidth = sr.getScaledWidth();
        epilogue.module.modules.render.Interface interfaceModule = (epilogue.module.modules.render.Interface) Epilogue.moduleManager.getModule("Interface");

        Comparator<Module> sort = (m1, m2) -> {
            double ab = FontRenderer.getStringWidth(m1.getName() + getFormattedTag(m1.getTag()));
            double bb = FontRenderer.getStringWidth(m2.getName() + getFormattedTag(m2.getTag()));
            return Double.compare(bb, ab);
        };

        java.util.ArrayList<Module> enabledMods = new java.util.ArrayList<>(Epilogue.moduleManager.modules.values());

        java.util.ArrayList<float[]> moduleRects = new java.util.ArrayList<>();

        float textMinX = Float.MAX_VALUE;
        float textMinY = Float.MAX_VALUE;
        float textMaxX = -Float.MAX_VALUE;
        float textMaxY = -Float.MAX_VALUE;

        if (animation.getModeString().equals("Slide In")) {
            enabledMods.sort(sort);

            for (Module module : enabledMods) {

                if (module.isHidden()) continue;
                Translate translate = module.getTranslate();
                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));

                if (module.isEnabled() && !module.isHidden()) {
                    translate.translate((alignRight ? (x - moduleWidth - 1.0f) : (x + 1.0f)), yValue);
                    yValue += FontRenderer.getFontHeight() + fontHeight;
                } else {
                    translate.animate((alignRight ? (x - 1) : (x + 1)), -25.0);
                }

                if (translate.getX() >= screenWidth) {
                    continue;
                }

                float bgWidth = moduleWidth + 4;
                float bottom = FontRenderer.getFontHeight() + fontHeight;
                float leftSide = (float) (translate.getX() - (alignRight ? 2.0f : 0.0f));

                if (backgroundValue.getValue()) {
                    moduleRects.add(new float[]{leftSide, (float) translate.getY(), bgWidth, bottom});
                }

                float tLeft = leftSide + 2.0f;
                float tTop = (float) translate.getY();
                textMinX = Math.min(textMinX, tLeft);
                textMinY = Math.min(textMinY, tTop);
                textMaxX = Math.max(textMaxX, tLeft + moduleWidth);
                textMaxY = Math.max(textMaxY, tTop + bottom);
            }

            if (!moduleRects.isEmpty()) {
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE;
                float maxY = -Float.MAX_VALUE;
                for (float[] rect : moduleRects) {
                    minX = Math.min(minX, rect[0]);
                    minY = Math.min(minY, rect[1]);
                    maxX = Math.max(maxX, rect[0] + rect[2]);
                    maxY = Math.max(maxY, rect[1] + rect[3]);
                }
                lastWidth = maxX - minX;
                lastHeight = maxY - minY;
            } else if (textMaxX > textMinX && textMaxY > textMinY) {
                lastWidth = textMaxX - textMinX;
                lastHeight = textMaxY - textMinY;
            } else {
                lastWidth = 0.0f;
                lastHeight = 0.0f;
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE;
                float maxY = -Float.MAX_VALUE;
                for (float[] rect : moduleRects) {
                    minX = Math.min(minX, rect[0]);
                    minY = Math.min(minY, rect[1]);
                    maxX = Math.max(maxX, rect[0] + rect[2]);
                    maxY = Math.max(maxY, rect[1] + rect[3]);
                }
                int r = Math.max(0, round.getValue());
                PostProcessing.drawBlur(minX, minY, maxX, maxY, () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                        if (r > 0) {
                            for (float[] rect : moduleRects) {
                                float by = rect[1] - r;
                                drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(-1, true));
                            }
                        } else {
                            for (float[] rect : moduleRects) {
                                net.minecraft.client.gui.Gui.drawRect((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]), -1);
                            }
                        }
                    });
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });
            }

            if (backgroundValue.getValue()) {
                int r = Math.max(0, round.getValue());
                if (r > 0) {
                    for (float[] rect : moduleRects) {
                        float by = rect[1] - r;
                        drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(21, 21, 21, (int)bgAlpha.getValue().floatValue()));
                    }
                } else {
                    for (float[] rect : moduleRects) {
                        RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3],
                                new Color(21, 21, 21, (int) bgAlpha.getValue().floatValue()).getRGB());
                    }
                }
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                Framebuffer bloomBuffer = PostProcessing.beginBloom();
                if (bloomBuffer != null) {
                    int index = 0;
                    int r = Math.max(0, round.getValue());
                    if (r > 0) {
                        for (float[] rect : moduleRects) {
                            int color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                    ? interfaceModule.color(index)
                                    : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                            float by = rect[1] - r;
                            drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(ColorUtil.swapAlpha(color, 255), true));
                            index++;
                        }
                    } else {
                        int color;
                        for (float[] rect : moduleRects) {
                            color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                    ? interfaceModule.color(index)
                                    : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                            RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3], ColorUtil.swapAlpha(color, 255));
                            index++;
                        }
                    }
                    PostProcessing.endBloom(bloomBuffer);
                }
            }

            yValue = y;

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;
                Translate translate = module.getTranslate();
                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));

                if (module.isEnabled() && !module.isHidden()) {
                    yValue += FontRenderer.getFontHeight() + fontHeight;
                } else {
                    continue;
                }

                if (translate.getX() >= screenWidth) {
                    continue;
                }

                float bgWidth = moduleWidth + 4;
                float bgHeight = FontRenderer.getFontHeight() + fontHeight;
                float bgLeft = (float) (translate.getX() - (alignRight ? 2.0f : 0.0f));
                float bgTop = (float) translate.getY();

                float textLeft = bgLeft + 2.0f;
                float textTop = bgTop + (bgHeight - FontRenderer.getFontHeight()) / 2.0f + textOffset.getValue();

                switch (rectangleValue.getModeString()) {
                    case "Top":
                        if (count == 1) {
                            if (interfaceModule != null) {
                                Gui.drawRect((int)bgLeft, (int)bgTop,
                                    (int)(bgLeft + bgWidth), (int)(bgTop + 1),
                                    interfaceModule.color(count));
                            }
                        }
                        break;
                    case "Side":
                        if (interfaceModule != null) {
                            Gui.drawRect((int)(bgLeft + bgWidth), (int)bgTop,
                                (int)(bgLeft + bgWidth + 1), (int)(bgTop + bgHeight),
                                interfaceModule.color(count));
                        }
                        break;
                }

                String moduleName = module.getName();
                String moduleTag = getFormattedTag(module.getTag());

                if (interfaceModule != null) {
                    FontRenderer.drawStringWithShadow(moduleName,
                            textLeft,
                            textTop,
                            interfaceModule.color(count));
                }

                if (!moduleTag.isEmpty()) {
                    float nameWidth = FontRenderer.getStringWidth(moduleName);
                    FontRenderer.drawStringWithShadow(moduleTag,
                            textLeft + nameWidth,
                            textTop,
                            0xFF888888);
                }

                count -= 1;
            }

        }

        if (!animation.getModeString().equals("Slide In")) {
            enabledMods.sort(sort);

            moduleRects.clear();
            yValue = y;

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;

                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));
                float xValue = alignRight ? (x - moduleWidth - 1.0f) : (x + 1.0f);
                float bgWidth = moduleWidth + 4;
                float bgHeight = FontRenderer.getFontHeight() + fontHeight;
                float leftSide = xValue - (alignRight ? 2.0f : 0.0f);

                float baseY = yValue;

                float anim = (float) moduleAnimation.getOutput();
                if (anim < 0.0f) anim = 0.0f;
                if (anim > 1.0f) anim = 1.0f;

                if (backgroundValue.getValue()) {
                    if (animation.getModeString().equals("Scale In")) {
                        float w = bgWidth * anim;
                        float h = bgHeight * anim;
                        float cx = leftSide + bgWidth / 2.0f;
                        float cy = baseY + bgHeight / 2.0f;
                        float rx = cx - w / 2.0f;
                        float ry = cy - h / 2.0f;
                        moduleRects.add(new float[]{rx, ry, w, h});
                    } else {
                        float w = bgWidth;
                        float rx;
                        if (animation.getModeString().equals("Move In")) {
                            float moveOffset = Math.abs((anim - 1.0f) * (2.0f + moduleWidth));
                            rx = alignRight ? (leftSide + moveOffset) : (leftSide - moveOffset);
                        } else {
                            rx = leftSide;
                        }
                        moduleRects.add(new float[]{rx, baseY, w, bgHeight});
                    }
                }

                float tLeft = leftSide + 2.0f;
                float tTop = baseY;
                textMinX = Math.min(textMinX, tLeft);
                textMinY = Math.min(textMinY, tTop);
                textMaxX = Math.max(textMaxX, tLeft + moduleWidth);
                textMaxY = Math.max(textMaxY, tTop + bgHeight);

                yValue = baseY + (float) (moduleAnimation.getOutput() * (FontRenderer.getFontHeight() + fontHeight));
            }

            if (!moduleRects.isEmpty()) {
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE;
                float maxY = -Float.MAX_VALUE;
                for (float[] rect : moduleRects) {
                    minX = Math.min(minX, rect[0]);
                    minY = Math.min(minY, rect[1]);
                    maxX = Math.max(maxX, rect[0] + rect[2]);
                    maxY = Math.max(maxY, rect[1] + rect[3]);
                }
                lastWidth = maxX - minX;
                lastHeight = maxY - minY;
            } else if (textMaxX > textMinX && textMaxY > textMinY) {
                lastWidth = textMaxX - textMinX;
                lastHeight = textMaxY - textMinY;
            } else {
                lastWidth = 0.0f;
                lastHeight = 0.0f;
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE;
                float maxY = -Float.MAX_VALUE;
                for (float[] rect : moduleRects) {
                    minX = Math.min(minX, rect[0]);
                    minY = Math.min(minY, rect[1]);
                    maxX = Math.max(maxX, rect[0] + rect[2]);
                    maxY = Math.max(maxY, rect[1] + rect[3]);
                }
                int r = Math.max(0, round.getValue());
                PostProcessing.drawBlur(minX, minY, maxX, maxY, () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                        if (r > 0) {
                            for (float[] rect : moduleRects) {
                                float by = rect[1] - r;
                                drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(-1, true));
                            }
                        } else {
                            for (float[] rect : moduleRects) {
                                net.minecraft.client.gui.Gui.drawRect((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]), -1);
                            }
                        }
                    });
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });
            }

            if (backgroundValue.getValue()) {
                int r = Math.max(0, round.getValue());
                if (r > 0) {
                    for (float[] rect : moduleRects) {
                        float by = rect[1] - r;
                        drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(21, 21, 21, (int)bgAlpha.getValue().floatValue()));
                    }
                } else {
                    for (float[] rect : moduleRects) {
                        RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3],
                                new Color(21, 21, 21, (int) bgAlpha.getValue().floatValue()).getRGB());
                    }
                }
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                Framebuffer bloomBuffer = PostProcessing.beginBloom();
                if (bloomBuffer != null) {
                    int index = 0;
                    int r = Math.max(0, round.getValue());
                    if (r > 0) {
                        for (float[] rect : moduleRects) {
                            int color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                    ? interfaceModule.color(index)
                                    : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                            float by = rect[1] - r;
                            drawBottomRoundedRow(rect[0], by, rect[2], rect[3] + r, r, new Color(ColorUtil.swapAlpha(color, 255), true));
                            index++;
                        }
                    } else {
                        int color;
                        for (float[] rect : moduleRects) {
                            color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                    ? interfaceModule.color(index)
                                    : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                            RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3], ColorUtil.swapAlpha(color, 255));
                            index++;
                        }
                    }
                    PostProcessing.endBloom(bloomBuffer);
                }
            }

            yValue = y;

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;

                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));
                float bgWidth = moduleWidth + 4;
                float bgHeight = FontRenderer.getFontHeight() + fontHeight;

                float baseX = alignRight ? (x - moduleWidth - 1.0f) : (x + 1.0f);
                float bgLeft = baseX - (alignRight ? 2.0f : 0.0f);
                float baseY = yValue;
                float bgTop = baseY;

                float textLeft = baseX;
                float textTop = bgTop + (bgHeight - FontRenderer.getFontHeight()) / 2.0f + textOffset.getValue();

                float alphaAnimation = 1.0f;

                switch (animation.getModeString()) {
                    case "Move In":
                        float moveOffset = (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));
                        textLeft = alignRight ? (baseX + moveOffset) : (baseX - moveOffset);
                        bgLeft = alignRight ? (baseX + moveOffset - 2.0f) : (baseX - moveOffset);
                        break;
                    case "Scale In":
                        float scale = (float) moduleAnimation.getOutput();
                        float centerX = baseX + moduleWidth / 2.0f;
                        float centerY = bgTop + bgHeight / 2.0f;

                        RenderUtil.scaleStart(centerX, centerY, scale);
                        alphaAnimation = scale;
                        break;
                }

                int textcolor = 0;
                if (interfaceModule != null) {
                    textcolor = ColorUtil.swapAlpha(interfaceModule.color(count), alphaAnimation * 255);
                }

                switch (rectangleValue.getModeString()) {
                    case "Top":
                        if (count == 1) {
                            Gui.drawRect((int)bgLeft, (int)bgTop,
                                (int)(bgLeft + bgWidth), (int)(bgTop + 1), textcolor);
                        }
                        break;
                    case "Side":
                        Gui.drawRect((int)(bgLeft + bgWidth), (int)bgTop,
                            (int)(bgLeft + bgWidth + 1), (int)(bgTop + bgHeight), textcolor);
                        break;
                }

                String moduleName = module.getName();
                String moduleTag = getFormattedTag(module.getTag());

                FontRenderer.drawStringWithShadow(moduleName,
                        textLeft,
                        textTop,
                        textcolor);

                if (!moduleTag.isEmpty()) {
                    float nameWidth = FontRenderer.getStringWidth(moduleName);
                    int tagColor = ColorUtil.swapAlpha(0xFF888888, alphaAnimation * 255);
                    FontRenderer.drawStringWithShadow(moduleTag,
                            textLeft + nameWidth,
                            textTop,
                            tagColor);
                }

                if (animation.getModeString().equals("Scale In")) {
                    RenderUtil.scaleEnd();
                }

                yValue = baseY + (float) (moduleAnimation.getOutput() * (FontRenderer.getFontHeight() + fontHeight));
                count -= 2;
            }
        }
    }
}