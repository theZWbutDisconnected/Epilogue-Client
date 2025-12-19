package epilogue.module.modules.render;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import epilogue.events.Render2DEvent;
import epilogue.event.EventTarget;
import epilogue.module.Module;
import epilogue.util.render.animations.advanced.Direction;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.util.render.ColorUtil;
import epilogue.util.render.RenderUtil;
import epilogue.font.FontRenderer;
import epilogue.util.render.animations.Translate;
import epilogue.util.render.animations.advanced.Animation;
import epilogue.util.render.PostProcessing;

import java.awt.*;
import java.util.Comparator;

public class ArrayList extends Module {

    private final Minecraft mc = Minecraft.getMinecraft();

    public final ModeValue animation = new ModeValue("Animation", 2, new String[]{"Scale In", "Move In", "Slide In"});
    public final ModeValue rectangleValue = new ModeValue("Rectangle", 1, new String[]{"None", "Top", "Side"});
    public final BooleanValue backgroundValue = new BooleanValue("Back Ground", true);
    public final IntValue bgAlpha = new IntValue("Back Ground Alpha", 40, 1, 255);
    public final IntValue positionOffset = new IntValue("Position", 0, -1, 100);
    public final FloatValue textHeight = new FloatValue("Text Height", 5f, 0f, 10f);
    public final FloatValue textOffset = new FloatValue("Text Offset", 2.5f, -6f, 6f);

    public ArrayList() {
        super("Arraylist", true);
    }

    private String getFormattedTag(String tag) {
        if (tag == null || tag.isEmpty()) return "";
        return " " + tag;
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;
        ScaledResolution sr = new ScaledResolution(mc);

        moduleList(sr);
    }

    public void moduleList(ScaledResolution sr) {
        int count = 1;
        float fontHeight = textHeight.getValue();
        float yValue = 1 + positionOffset.getValue();

        int screenWidth = sr.getScaledWidth();
        epilogue.module.modules.render.Interface interfaceModule = (epilogue.module.modules.render.Interface) Epilogue.moduleManager.getModule("Interface");

        Comparator<Module> sort = (m1, m2) -> {
            double ab = FontRenderer.getStringWidth(m1.getName() + getFormattedTag(m1.getTag()));
            double bb = FontRenderer.getStringWidth(m2.getName() + getFormattedTag(m2.getTag()));
            return Double.compare(bb, ab);
        };

        java.util.ArrayList<Module> enabledMods = new java.util.ArrayList<>(Epilogue.moduleManager.modules.values());

        java.util.ArrayList<float[]> moduleRects = new java.util.ArrayList<>();

        if (animation.getModeString().equals("Slide In")) {
            enabledMods.sort(sort);

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;
                Translate translate = module.getTranslate();
                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));

                if (module.isEnabled() && !module.isHidden()) {
                    translate.translate((screenWidth - moduleWidth - 1.0f - positionOffset.getValue()), yValue);
                    yValue += FontRenderer.getFontHeight() + fontHeight;
                } else {
                    translate.animate((screenWidth - 1) + positionOffset.getValue(), -25.0);
                }

                if (translate.getX() >= screenWidth) {
                    continue;
                }

                float bgWidth = moduleWidth + 4;
                float bottom = FontRenderer.getFontHeight() + fontHeight;
                float leftSide = (float) (translate.getX() - 2.0f);

                if (backgroundValue.getValue()) {
                    moduleRects.add(new float[]{leftSide, (float) translate.getY(), bgWidth, bottom});
                }
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
                PostProcessing.drawBlur(minX, minY, maxX, maxY, () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                        for (float[] rect : moduleRects) {
                            net.minecraft.client.gui.Gui.drawRect((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]), -1);
                        }
                    });
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });
            }

            if (backgroundValue.getValue()) {
                for (float[] rect : moduleRects) {
                    RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3],
                        new Color(21, 21, 21, (int)bgAlpha.getValue().floatValue()).getRGB());
                }
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                Framebuffer bloomBuffer = PostProcessing.beginBloom();
                if (bloomBuffer != null) {
                    int index = 0;
                    for (float[] rect : moduleRects) {
                        int color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                ? interfaceModule.color(index)
                                : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                        RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3], ColorUtil.swapAlpha(color, 255));
                        index++;
                    }
                    PostProcessing.endBloom(bloomBuffer);
                }
            }

            yValue = 1 + positionOffset.getValue();
            count = 1;

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
                float bgLeft = (float) (translate.getX() - 2.0f);
                float bgTop = (float) translate.getY();
                
                float textLeft = bgLeft + 2.0f;
                float textTop = bgTop + (bgHeight - FontRenderer.getFontHeight()) / 2.0f + textOffset.getValue();

                switch (rectangleValue.getModeString()) {
                    case "Top":
                        if (count == 1) {
                            Gui.drawRect((int)bgLeft, (int)bgTop,
                                (int)(bgLeft + bgWidth), (int)(bgTop + 1),
                                interfaceModule.color(count));
                        }
                        break;
                    case "Side":
                        Gui.drawRect((int)(bgLeft + bgWidth), (int)bgTop,
                            (int)(bgLeft + bgWidth + 1), (int)(bgTop + bgHeight),
                            interfaceModule.color(count));
                        break;
                }

                String moduleName = module.getName();
                String moduleTag = getFormattedTag(module.getTag());

                FontRenderer.drawStringWithShadow(moduleName,
                        textLeft,
                        textTop,
                        interfaceModule.color(count));

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
            yValue = 1 + positionOffset.getValue();

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;

                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));
                float xValue = (screenWidth - moduleWidth - 1.0f - positionOffset.getValue());
                float bgWidth = moduleWidth + 4;
                float bgHeight = FontRenderer.getFontHeight() + fontHeight;
                float leftSide = xValue - 2.0f;

                if (backgroundValue.getValue()) {
                    moduleRects.add(new float[]{leftSide, yValue, bgWidth, bgHeight});
                }

                yValue += (float) (moduleAnimation.getOutput() * (FontRenderer.getFontHeight() + fontHeight));
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
                PostProcessing.drawBlur(minX, minY, maxX, maxY, () -> () -> {
                    GlStateManager.enableBlend();
                    GlStateManager.disableTexture2D();
                    GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                    epilogue.util.render.RenderUtil.setup2DRendering(() -> {
                        for (float[] rect : moduleRects) {
                            net.minecraft.client.gui.Gui.drawRect((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]), -1);
                        }
                    });
                    GlStateManager.enableTexture2D();
                    GlStateManager.disableBlend();
                });
            }

            if (backgroundValue.getValue()) {
                for (float[] rect : moduleRects) {
                    RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3],
                        new Color(21, 21, 21, (int)bgAlpha.getValue().floatValue()).getRGB());
                }
            }

            if (backgroundValue.getValue() && !moduleRects.isEmpty()) {
                Framebuffer bloomBuffer = PostProcessing.beginBloom();
                if (bloomBuffer != null) {
                    int index = 0;
                    for (float[] rect : moduleRects) {
                        int color = epilogue.module.modules.render.PostProcessing.isArrayListBloomFromInterface() && interfaceModule != null
                                ? interfaceModule.color(index)
                                : epilogue.module.modules.render.PostProcessing.getBloomColor(index);
                        RenderUtil.drawRect(rect[0], rect[1], rect[2], rect[3], ColorUtil.swapAlpha(color, 255));
                        index++;
                    }
                    PostProcessing.endBloom(bloomBuffer);
                }
            }

            yValue = 1 + positionOffset.getValue();

            for (Module module : enabledMods) {
                if (module.isHidden()) continue;

                Animation moduleAnimation = module.getAnimation();
                moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
                if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;

                float moduleWidth = FontRenderer.getStringWidth(module.getName() + getFormattedTag(module.getTag()));
                float bgWidth = moduleWidth + 4;
                float bgHeight = FontRenderer.getFontHeight() + fontHeight;
                
                float baseX = screenWidth - moduleWidth - 1.0f - positionOffset.getValue();
                float bgLeft = baseX - 2.0f;
                float bgTop = yValue;
                
                float textLeft = baseX;
                float textTop = bgTop + (bgHeight - FontRenderer.getFontHeight()) / 2.0f + textOffset.getValue();

                float alphaAnimation = 1.0f;

                switch (animation.getModeString()) {
                    case "Move In":
                        float moveOffset = (float) Math.abs((moduleAnimation.getOutput() - 1.0) * (2.0 + moduleWidth));
                        textLeft = baseX + moveOffset;
                        bgLeft = baseX + moveOffset - 2.0f;
                        break;
                    case "Scale In":
                        float scale = (float) moduleAnimation.getOutput();
                        float centerX = baseX + moduleWidth / 2.0f;
                        float centerY = bgTop + bgHeight / 2.0f;
                        RenderUtil.scaleStart(centerX, centerY, scale);
                        alphaAnimation = scale;
                        break;
                }

                int textcolor = ColorUtil.swapAlpha(interfaceModule.color(count), alphaAnimation * 255);

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

                yValue += (float) (moduleAnimation.getOutput() * (FontRenderer.getFontHeight() + fontHeight));
                count -= 2;
            }
        }
    }
}