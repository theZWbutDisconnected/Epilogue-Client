package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.PacketEvent;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.ncm.rendering.rendersystem.RenderSystem;
import epilogue.ui.chat.GuiChat;
import epilogue.util.RenderUtil;
import epilogue.util.TeamUtil;
import epilogue.util.TimerUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RoundedUtil;
import epilogue.ui.clickgui.menu.Fonts;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.Gui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private int lastTargetEntityId = -1;

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Exhibition", "Epilogue"});
    public final IntValue bgAlpha = new IntValue("BackGround Alpha", 45, 0, 255, () -> this.mode.getValue() == 1);
    public final BooleanValue shadow = new BooleanValue("Text Shadow", true, () -> this.mode.getValue() == 0);
    public final BooleanValue kaOnly = new BooleanValue("Only KillAura", true);
    private static final float SCALE = 1.0F;
    private float anchorX = 0.0f;
    private float anchorY = 0.0f;
    private float lastWidth = 0.0f;
    private float lastHeight = 0.0f;
    private float epilogueDisplayHealth = -1.0f;
    private final float epilogueTextScale = 0.75f;;

    public void renderAt(float x, float y) {
        this.anchorX = x;
        this.anchorY = y;
        render(new Render2DEvent(0.0f));
    }

    public float getLastWidth() {
        return lastWidth <= 0 ? 180f : lastWidth;
    }

    public float getLastHeight() {
        return lastHeight <= 0 ? 80f : lastHeight;
    }

    private void setLastSize(float w, float h) {
        this.lastWidth = w;
        this.lastHeight = h;
    }

    private EntityLivingBase resolveTarget() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura.isEnabled() && aura.isAttackAllowed() && TeamUtil.isEntityLoaded(aura.getTarget())) {
            return aura.getTarget();
        } else if (!(java.lang.Boolean) this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat || mc.currentScreen instanceof GuiChat) ? mc.thePlayer : null;
        }
    }

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    @EventTarget
    public void render(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        EntityLivingBase currentTarget = this.resolveTarget();
        if (currentTarget == null && (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat)) {
            currentTarget = mc.thePlayer;
        }

        if (currentTarget == null) {
            this.target = null;
            return;
        }

        if (this.target != currentTarget) {
            this.target = currentTarget;
            this.animTimer.setTime();
            this.lastTargetEntityId = currentTarget.getEntityId();
        } else {
            updateHurtTrigger(currentTarget);
        }

        switch (this.mode.getValue()) {
            case 0:
                renderExhibitionMode();
                break;
            case 1:
                renderEpilogueMode();
                break;
        }
    }

    private void updateHurtTrigger(EntityLivingBase currentTarget) {
        if (currentTarget == null) return;
        if (currentTarget.getEntityId() != lastTargetEntityId) {
            lastTargetEntityId = currentTarget.getEntityId();
        }
    }

    private float[] computePos() {
        return new float[]{anchorX / SCALE, anchorY / SCALE};
    }

    private void renderExhibitionMode() {
        if (!(this.target instanceof EntityPlayer)) return;
        ScaledResolution resolution = new ScaledResolution(mc);

        double boxWidth = 40 + mc.fontRendererObj.getStringWidth(target.getName());
        double renderWidth = Math.max(boxWidth, 120);

        setLastSize((float) renderWidth, 40.0F);

        float[] pos = computePos();
        float posX = pos[0];
        float posY = pos[1];

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + (float) renderWidth / 2.0F, posY + 20.0F, 0);
        float finalScale = SCALE;
        GlStateManager.scale(finalScale, finalScale, 1.0F);
        GlStateManager.translate(-(float) renderWidth / 2.0F, -20.0F, 0);

        drawExhibitionBorderedRect(-2.5F, -2.5F, (float) renderWidth + 2.5F, 40 + 2.5F, 0.5F, getExhibitionColor(60), getExhibitionColor(10));
        drawExhibitionBorderedRect(-1.5F, -1.5F, (float) renderWidth + 1.5F, 40 + 1.5F, 1.5F, getExhibitionColor(60), getExhibitionColor(40));
        drawExhibitionBorderedRect(0, 0, (float) renderWidth, 40, 0.5F, getExhibitionColor(22), getExhibitionColor(60));
        drawExhibitionBorderedRect(2, 2, 38, 38, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(10));
        drawExhibitionBorderedRect(2.5F, 2.5F, 38 - 0.5F, 38 - 0.5F, 0.5F, getExhibitionColor(17), getExhibitionColor(48));

        GL11.glScissor((int) ((posX + 3) * resolution.getScaleFactor()), (int) ((resolution.getScaledHeight() - (posY + 37)) * resolution.getScaleFactor()), (int) ((37 - 3) * resolution.getScaleFactor()), (int) ((37 - 3) * resolution.getScaleFactor()));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        drawEntityOnScreen(target);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.translate(2, 0, 0);

        GlStateManager.pushMatrix();
        GlStateManager.scale(0.8F, 0.8F, 0.8F);
        mc.fontRendererObj.drawString(target.getName(), 46, 4, -1, this.shadow.getValue());
        GlStateManager.popMatrix();

        float health = target.getHealth();
        float absorption = target.getAbsorptionAmount();
        float progress = health / (target.getMaxHealth() + absorption);
        float realHealthProgress = (health / target.getMaxHealth());

        Color customColor = health >= 0 ? blendColors(new float[]{0f, 0.5f, 1f}, new Color[]{Color.RED, Color.YELLOW, Color.GREEN}, realHealthProgress).brighter() : Color.RED;
        double width = Math.max(mc.fontRendererObj.getStringWidth(target.getName()), 60);

        width = getIncremental(width, 10);
        if (width < 60) {
            width = 60;
        }
        double healthLocation = width * progress;

        drawExhibitionBorderedRect(37, 12, 39 + (float) width, 16, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(0));
        drawExhibitionRect(38 + (float) healthLocation + 0.5F, 12.5F, 38 + (float) width + 0.5F, 15.5F, getExhibitionColorOpacity(customColor.getRGB(), 35));
        drawExhibitionRect(37.5F, 12.5F, 38 + (float) healthLocation + 0.5F, 15.5F, customColor.getRGB());

        if (absorption > 0) {
            double absorptionDifferent = width * (absorption / (target.getMaxHealth() + absorption));
            drawExhibitionRect(38 + (float) healthLocation + 0.5F, 12.5F, 38 + (float) healthLocation + 0.5F + (float) absorptionDifferent, 15.5F, 0x80FFAA00);
        }

        for (int i = 1; i < 10; i++) {
            double dThing = (width / 10) * i;
            drawExhibitionRect(38 + (float) dThing, 12, 38 + (float) dThing + 0.5F, 16, getExhibitionColor(0));
        }

        String str = "HP: " + (int) health + " | Dist: " + (int) mc.thePlayer.getDistanceToEntity(target);
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.7F, 0.7F, 0.7F);
        mc.fontRendererObj.drawString(str, 53, 26, -1, this.shadow.getValue());
        GlStateManager.popMatrix();

        if (target instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) target;
            GL11.glPushMatrix();
            final List<ItemStack> items = new ArrayList<ItemStack>();
            int split = 20;

            for (int index = 3; index >= 0; --index) {
                final ItemStack armor = targetPlayer.inventory.armorInventory[index];
                if (armor != null) {
                    items.add(armor);
                }
            }
            int yOffset = 23;
            if (targetPlayer.getCurrentEquippedItem() != null) {
                items.add(targetPlayer.getCurrentEquippedItem());
            }

            RenderHelper.enableGUIStandardItemLighting();
            for (final ItemStack itemStack : items) {
                if (mc.theWorld != null) {
                    split += 16;
                }
                GlStateManager.pushMatrix();
                GlStateManager.disableAlpha();
                GlStateManager.clear(256);
                mc.getRenderItem().zLevel = -150.0f;
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, split, yOffset);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemStack, split, yOffset);
                mc.getRenderItem().zLevel = 0.0f;

                int renderY = yOffset;
                if (itemStack.getItem() instanceof ItemSword) {
                    int sLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
                    int fLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack);
                    if (sLevel > 0) {
                        drawEnchantTag("S" + getSharpnessColor(sLevel) + sLevel, split, renderY);
                        renderY += 4.5F;
                    }
                    if (fLevel > 0) {
                        drawEnchantTag("F" + getFireAspectColor(fLevel) + fLevel, split, renderY);
                    }
                } else if ((itemStack.getItem() instanceof ItemArmor)) {
                    int pLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack);
                    if (pLevel > 0) {
                        drawEnchantTag("P" + getProtectionColor(pLevel) + pLevel, split, renderY);
                    }
                }

                GlStateManager.disableBlend();
                GlStateManager.disableLighting();
                GlStateManager.enableAlpha();
                GlStateManager.popMatrix();
            }
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
        }

        GlStateManager.popMatrix();
    }

    private void renderEpilogueMode() {
        if (!(this.target instanceof EntityPlayer)) return;

        EntityPlayer p = (EntityPlayer) this.target;

        float hurtAnim = 0.0f;
        if (p.hurtTime > 0) {
            hurtAnim = Math.min(1.0f, p.hurtTime / 10.0f);
        }
        float pop = 1.0f + (0.06f * hurtAnim);

        float[] pos = computePos();
        float x = pos[0];
        float y = pos[1];

        float pad = 6.0f;
        float headSize = 26.0f;
        float radius = 5.0f;

        float hpBarW = 4.0f;
        float hpBarPad = 4.0f;

        float leftW = headSize;
        float rightPad = 8.0f;
        float baseW = 160.0f;
        float baseH = 40.0f;

        float nameW = Fonts.width(Fonts.small(), p.getName());
        float cardW = Math.max(baseW, 110.0f + nameW);
        float cardH = baseH;

        setLastSize(cardW, cardH);

        float cardX = x;
        float cardY = y;

        float innerX = cardX + pad;
        float innerY = cardY + pad;

        float barX = innerX;
        float headX = innerX + hpBarW + hpBarPad;
        float headY = innerY + (cardH - pad * 2.0f - headSize) / 2.0f;

        float contentX = headX + leftW + 10.0f;
        float contentW = cardW - (contentX - cardX) - rightPad;

        int bg = new Color(18, 18, 18, bgAlpha.getValue()).getRGB();
        int bgInner = new Color(22, 22, 22, bgAlpha.getValue() + 20).getRGB();

        float blurX1 = cardX;
        PostProcessing.drawBlur(blurX1, cardY, cardX + cardW, cardY + cardH, () -> () -> {
            RoundedUtil.drawRound(cardX, cardY, cardW, cardH, radius, new Color(-1, true));
        });

        net.minecraft.client.shader.Framebuffer bloom = PostProcessing.beginBloom();
        if (bloom != null) {
            int bloomColor = epilogue.module.modules.render.PostProcessing.getBloomColor(0);
            RoundedUtil.drawRound(cardX, cardY, cardW, cardH, radius, new Color(bloomColor, true));
            PostProcessing.endBloom(bloom);
        }

        RoundedUtil.drawRound(cardX, cardY, cardW, cardH, radius, new Color(bg, true));
        RoundedUtil.drawRound(cardX + 1.0f, cardY + 1.0f, cardW - 2.0f, cardH - 2.0f, radius - 1.0f, new Color(bgInner, true));

        float textY = headY + 3.0f;
        int label = new Color(160, 160, 160, 255).getRGB();
        int value = new Color(235, 235, 235, 255).getRGB();

        float col1X = contentX;
        float col2X = contentX + contentW * 0.55f;
        float colW = (col2X - col1X) - 4.0f;

        drawKeyValueClamped("Name", p.getName(), col1X, textY, colW, label, value);
        drawKeyValueClamped("Block State", p.isBlocking() ? "true" : "false", col2X, textY, colW, label, value);

        float rawHp = p.getHealth();
        float max = p.getMaxHealth() + p.getAbsorptionAmount();
        if (epilogueDisplayHealth < 0.0f) epilogueDisplayHealth = rawHp;
        epilogueDisplayHealth += (rawHp - epilogueDisplayHealth) * 0.12f;
        float frac = max <= 0.0f ? 0.0f : Math.max(0.0f, Math.min(1.0f, epilogueDisplayHealth / max));

        float barY = headY;
        float barH = headSize;
        RoundedUtil.drawRound(barX, barY, hpBarW, barH, 2.0f, new Color(0, 0, 0, 110));
        float fillH = barH * frac;
        float fillY = barY + (barH - fillH);
        RoundedUtil.drawRound(barX, fillY, hpBarW, fillH, 2.0f, new Color(220, 60, 60, 255));

        float line2Y = textY + 10.0f;
        int dist = mc.thePlayer != null ? (int) mc.thePlayer.getDistanceToEntity(p) : 0;
        drawKeyValueClamped("HP", String.valueOf((int) rawHp), col1X, line2Y, colW, label, value);
        drawKeyValueClamped("Distance", dist + "m", col2X, line2Y, colW, label, value);

        renderItemIconsOnly(p, contentX, headY + headSize - 4.0f, contentW, 0.68f);

        GlStateManager.pushMatrix();
        GlStateManager.translate(headX + headSize / 2.0f, headY + headSize / 2.0f, 0);
        GlStateManager.scale(pop, pop, 1.0f);
        GlStateManager.translate(-(headX + headSize / 2.0f), -(headY + headSize / 2.0f), 0);
        drawRoundedHead(p, headX, headY, headSize, 5.5f);
        GlStateManager.popMatrix();
    }

    private void drawSmallText(String s, float x, float y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0f);
        GlStateManager.scale(epilogueTextScale, epilogueTextScale, 1.0f);
        if (this.shadow.getValue()) {
            Fonts.drawWithShadow(Fonts.small(), s, 0.0f, 0.0f, color);
        } else {
            Fonts.draw(Fonts.small(), s, 0.0f, 0.0f, color);
        }
        GlStateManager.popMatrix();
    }

    private void drawKeyValueClamped(String key, String val, float x, float y, float maxWidth, int keyColor, int valColor) {
        String k = key + " ";
        float kw = getSmallTextWidth(k);
        float vw = Math.max(0.0f, maxWidth - kw);
        drawSmallText(k, x, y, keyColor);
        drawSmallText(ellipsizeSmall(val, vw), x + kw, y, valColor);
    }

    private float getSmallTextWidth(String s) {
        return Fonts.width(Fonts.small(), s) * epilogueTextScale;
    }

    private String ellipsizeSmall(String s, float maxW) {
        if (s == null) return "";
        if (maxW <= 0.0f) return "";
        if (getSmallTextWidth(s) <= maxW) return s;
        final String dots = "...";
        float dw = getSmallTextWidth(dots);
        if (dw >= maxW) return "";
        int end = s.length();
        while (end > 0 && getSmallTextWidth(s.substring(0, end)) + dw > maxW) {
            end--;
        }
        return end <= 0 ? "" : s.substring(0, end) + dots;
    }

    private void drawRoundedHead(EntityPlayer p, float x, float y, float size, float radius) {
        ResourceLocation skin = mc.getNetHandler().getPlayerInfo(p.getName()) != null
                ? mc.getNetHandler().getPlayerInfo(p.getName()).getLocationSkin()
                : null;
        if (skin == null) return;
        mc.getTextureManager().bindTexture(skin);
        RenderSystem.nearestFilter();

        float clipX = x;
        float clipY = y;
        float clipS = size;
        RoundedUtil.beginRoundClip(clipX, clipY, clipS, clipS, radius);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0f);
        Gui.drawScaledCustomSizeModalRect(0, 0, 8.0F, 8.0F, 8, 8, (int) size, (int) size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(0, 0, 40.0F, 8.0F, 8, 8, (int) size, (int) size, 64.0F, 64.0F);
        GlStateManager.popMatrix();
        RoundedUtil.endRoundClip();
    }

    private void renderItemIconsOnly(EntityPlayer p, float startX, float y, float width, float scale) {
        final List<ItemStack> items = new ArrayList<>();

        for (int index = 3; index >= 0; --index) {
            final ItemStack armor = p.inventory.armorInventory[index];
            if (armor != null) {
                items.add(armor);
            }
        }
        if (p.getCurrentEquippedItem() != null) {
            items.add(p.getCurrentEquippedItem());
        }

        int count = items.size();
        if (count <= 0) return;

        float icon = 16.0f * scale;

        float gap;
        if (count <= 1) {
            gap = 0.0f;
        } else {
            gap = (width - (count * icon)) / (count - 1);
            gap = Math.max(2.0f, gap);
        }

        float total = (count * icon) + (count - 1) * gap;
        float x0 = startX + (width - total) / 2.0f;

        RenderHelper.enableGUIStandardItemLighting();

        float x = x0;
        for (final ItemStack stack : items) {
            if (stack == null) continue;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 0.0f);
            GlStateManager.scale(scale, scale, 1.0f);
            GlStateManager.translate(-x, -y, 0.0f);

            GlStateManager.disableAlpha();
            GlStateManager.clear(256);
            mc.getRenderItem().zLevel = -150.0f;
            mc.getRenderItem().renderItemAndEffectIntoGUI(stack, (int) x, (int) y);
            mc.getRenderItem().zLevel = 0.0f;
            GlStateManager.enableAlpha();

            GlStateManager.popMatrix();

            x += icon + gap;
        }

        RenderHelper.disableStandardItemLighting();
        RenderHelper.disableStandardItemLighting();
    }

    private double getIncremental(double value, double increment) {
        return Math.ceil(value / increment) * increment;
    }

    private void drawEntityOnScreen(EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(20.0F, 36.0F, 50.0F);

        float largestSize = Math.max(ent.height, ent.width);
        float relativeScale = Math.max(largestSize / 1.8F, 1);

        GlStateManager.scale((float) -16 / relativeScale, (float) 16 / relativeScale, (float) 16 / relativeScale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan((float) 17 / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions.length == colors.length) {
            int[] indicies = getFractionIndicies(fractions, progress);
            float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
            Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};
            float max = range[1] - range[0];
            float value = progress - range[0];
            float weight = value / max;
            return blend(colorRange[0], colorRange[1], 1.0F - weight);
        } else {
            return colors[0];
        }
    }

    private int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];
        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            ++startPoint;
        }
        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }
        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    private Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0F - r;
        float[] rgb1 = color1.getColorComponents(new float[3]);
        float[] rgb2 = color2.getColorComponents(new float[3]);
        return new Color(rgb1[0] * r + rgb2[0] * ir, rgb1[1] * r + rgb2[1] * ir, rgb1[2] * r + rgb2[2] * ir);
    }

    private void drawEnchantTag(String text, int x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        mc.fontRendererObj.drawString(text, x * 2, y * 2, -1, true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private String getProtectionColor(int level) {
        switch (level) {
            case 1:
                return "§a";
            case 2:
                return "§9";
            case 3:
                return "§e";
            case 4:
                return "§c";
            default:
                return "§f";
        }
    }

    private String getSharpnessColor(int level) {
        switch (level) {
            case 1:
                return "§a";
            case 2:
                return "§9";
            case 3:
                return "§e";
            case 4:
                return "§6";
            case 5:
                return "§c";
            default:
                return "§f";
        }
    }

    private String getFireAspectColor(int level) {
        switch (level) {
            case 1:
                return "§6";
            case 2:
                return "§c";
            default:
                return "§f";
        }
    }

    private int getExhibitionColor(int brightness) {
        return getExhibitionColor(brightness, brightness, brightness, 255);
    }

    private int getExhibitionColor(int brightness, int alpha) {
        return getExhibitionColor(brightness, brightness, brightness, alpha);
    }

    private int getExhibitionColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= Math.max(0, Math.min(255, alpha)) << 24;
        color |= Math.max(0, Math.min(255, red)) << 16;
        color |= Math.max(0, Math.min(255, green)) << 8;
        color |= Math.max(0, Math.min(255, blue));
        return color;
    }

    private int getExhibitionColorOpacity(int color, int alpha) {
        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        return getExhibitionColor(red, green, blue, Math.max(0, Math.min(255, alpha)));
    }

    private void drawExhibitionRect(float x1, float y1, float x2, float y2, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, color);
        RenderUtil.disableRenderState();
    }

    private void drawExhibitionBorderedRect(float x1, float y1, float x2, float y2, float borderWidth, int fillColor, int borderColor) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, borderColor);
        RenderUtil.drawRect(x1 + borderWidth, y1 + borderWidth, x2 - borderWidth, y2 - borderWidth, fillColor);
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}