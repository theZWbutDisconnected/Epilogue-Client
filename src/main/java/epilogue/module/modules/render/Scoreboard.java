package epilogue.module.modules.render;

import epilogue.ui.chat.GuiChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.EnumChatFormatting;
import epilogue.module.Module;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.util.render.RenderUtil;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RoundedUtil;
import epilogue.ui.clickgui.menu.Fonts;

import java.util.Collection;
import java.util.List;
import java.awt.Color;
import java.awt.Font;

public class Scoreboard extends Module {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    public final BooleanValue hide = new BooleanValue("Hide", false);
    public final BooleanValue removeRedScore = new BooleanValue("Remove Red Score", false);
    public final IntValue round = new IntValue("Round", 0, 0, 12);
    public final IntValue scale = new IntValue("Scale", 100, 50, 150);
    
    public static Scoreboard instance;
    private float lastWidth;
    private float lastHeight;
    
    public Scoreboard() {
        super("Scoreboard", false);
        instance = this;
    }
    
    public boolean shouldHideOriginalScoreboard() {
        return this.isEnabled();
    }

    public void renderAt(float anchorRightX, float anchorTopY) {
        if (mc.theWorld == null || mc.gameSettings.showDebugInfo) return;
        if (hide.getValue()) return;

        Font textFont = Fonts.msyhSmall();
        Font titleFont = Fonts.msyhHeading();
        int fontHeight = Fonts.height(textFont);

        float s = Math.max(0.1f, scale.getValue() / 100.0f);

        boolean preview = (mc.currentScreen instanceof net.minecraft.client.gui.GuiChat) || (mc.currentScreen instanceof GuiChat);

        ScoreObjective scoreobjective = mc.theWorld.getScoreboard().getObjectiveInDisplaySlot(1);
        if (scoreobjective != null) {
            net.minecraft.scoreboard.Scoreboard scoreboard = scoreobjective.getScoreboard();
            Collection<Score> collection = scoreboard.getSortedScores(scoreobjective);
            List<Score> list = com.google.common.collect.Lists.newArrayList(com.google.common.collect.Iterables.filter(collection, new com.google.common.base.Predicate<Score>() {
                public boolean apply(Score score) {
                    return score.getPlayerName() != null && !score.getPlayerName().startsWith("#");
                }
            }));
            
            if (list.size() > 15) {
                collection = com.google.common.collect.Lists.newArrayList(com.google.common.collect.Iterables.skip(list, collection.size() - 15));
            } else {
                collection = list;
            }
            
            int maxWidth = Fonts.widthMCFormatted(titleFont, scoreobjective.getDisplayName());
            
            for (Score score : collection) {
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String s1 = removeRedScore.getValue()
                        ? ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName())
                        : (ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName()) + ": " + EnumChatFormatting.RED + score.getScorePoints());
                maxWidth = Math.max(maxWidth, Fonts.widthMCFormatted(textFont, s1));
            }
            
            int listSize = collection.size();
            int height = listSize * fontHeight;
            int paddingX = 4;
            int maxWidthPadded = maxWidth + paddingX * 2;

            float scaledW = (maxWidthPadded + 4) * s;
            float scaledH = (height + fontHeight + 2) * s;

            float x = anchorRightX - scaledW;
            float yTop = anchorTopY - (fontHeight + 1) * (s - 1.0f);
            float yOffset = -12.0f;
            yTop += yOffset;
            float drawX = x;
            float drawY = yTop;
            float drawW = scaledW;
            float drawH = scaledH;
            
            int backgroundColor = 0x90505050;

            Framebuffer bloomBuffer;
            int r = Math.max(0, round.getValue());
            float scaledR = r * s;
            bloomBuffer = PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                int glowColor = epilogue.module.modules.render.PostProcessing.getBloomColor();
                if (r > 0) {
                    RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(glowColor, true));
                } else {
                    RenderUtil.drawRect(drawX, drawY, drawW, drawH, glowColor);
                }
                mc.getFramebuffer().bindFramebuffer(false);
            }
            
            GlStateManager.enableBlend();
            if (r > 0) {
                PostProcessing.drawBlur(drawX, drawY, drawX + drawW, drawY + drawH, () -> () ->
                        RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(-1, true))
                );
            } else {
                PostProcessing.drawBlurRect(drawX, drawY, drawX + drawW, drawY + drawH);
            }
            if (r > 0) {
                RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(backgroundColor, true));
            } else {
                Gui.drawRect((int) drawX, (int) drawY, (int) (drawX + drawW), (int) (drawY + drawH), backgroundColor);
            }
            
            PostProcessing.endBloom(bloomBuffer);
            
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            String titleText = scoreobjective.getDisplayName();
            GlStateManager.pushMatrix();
            GlStateManager.scale(s, s, 1.0f);

            float basePanelX = drawX / s;
            float basePanelY = drawY / s;
            float basePanelW = drawW / s;
            float bodyOffsetY = 12.0f;
            float titleX = basePanelX + basePanelW / 2f - Fonts.widthMCFormatted(titleFont, titleText) / 2f;
            float titleY = basePanelY;
            Fonts.drawMCFormattedWithShadow(titleFont, titleText, titleX, titleY, 0xFFFFFFFF);
            
            int index = 0;
            for (Score score : collection) {
                ++index;
                ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(score.getPlayerName());
                String playerName = ScorePlayerTeam.formatPlayerName(scoreplayerteam, score.getPlayerName());
                String scoreValue = "" + EnumChatFormatting.RED + score.getScorePoints();
                float baseYPos = basePanelY + bodyOffsetY + (listSize - index) * fontHeight;
                float textX = basePanelX + paddingX;
                float yPos = baseYPos;
                Fonts.drawMCFormattedWithShadow(textFont, playerName, textX, yPos, 0xFFFFFFFF);
                if (!removeRedScore.getValue()) {
                    float sx = basePanelX + maxWidthPadded - paddingX - Fonts.widthMCFormatted(textFont, scoreValue);
                    Fonts.drawMCFormattedWithShadow(textFont, scoreValue, sx, yPos, 0xFFFFFFFF);
                }
            }
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();

            lastWidth = scaledW;
            lastHeight = scaledH;
        } else if (preview) {
            String title = "Test Server";
            String l1 = "PlayerOne: " + EnumChatFormatting.RED + "20";
            String l2 = "PlayerTwo: " + EnumChatFormatting.RED + "15";
            String l3 = "PlayerThree: " + EnumChatFormatting.RED + "7";

            int maxWidth = Fonts.widthMCFormatted(titleFont, title);
            maxWidth = Math.max(maxWidth, Fonts.widthMCFormatted(textFont, l1));
            maxWidth = Math.max(maxWidth, Fonts.widthMCFormatted(textFont, l2));
            maxWidth = Math.max(maxWidth, Fonts.widthMCFormatted(textFont, l3));

            int listSize = 3;
            int height = listSize * fontHeight;
            int paddingX = 4;
            int maxWidthPadded = maxWidth + paddingX * 2;

            float scaledW = (maxWidthPadded + 4) * s;
            float scaledH = (height + fontHeight + 2) * s;
            float x = anchorRightX - scaledW;
            float yTop = anchorTopY - (fontHeight + 1) * (s - 1.0f);
            float yOffset = -4.0f;
            yTop += yOffset;

            int backgroundColor = 0x90505050;

            float drawX = x;
            float drawY = yTop;
            float drawW = scaledW;
            float drawH = scaledH;
            int r = Math.max(0, round.getValue());
            float scaledR = r * s;

            Framebuffer bloomBuffer = PostProcessing.beginBloom();
            if (bloomBuffer != null) {
                int glowColor = epilogue.module.modules.render.PostProcessing.getBloomColor();
                if (r > 0) {
                    RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(glowColor, true));
                } else {
                    RenderUtil.drawRect(drawX, drawY, drawW, drawH, glowColor);
                }
                mc.getFramebuffer().bindFramebuffer(false);
            }

            GlStateManager.enableBlend();
            if (r > 0) {
                PostProcessing.drawBlur(drawX, drawY, drawX + drawW, drawY + drawH, () -> () ->
                        RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(-1, true))
                );
            } else {
                PostProcessing.drawBlurRect(drawX, drawY, drawX + drawW, drawY + drawH);
            }
            if (r > 0) {
                RoundedUtil.drawRound(drawX, drawY, drawW, drawH, scaledR, new Color(backgroundColor, true));
            } else {
                Gui.drawRect((int) drawX, (int) drawY, (int) (drawX + drawW), (int) (drawY + drawH), backgroundColor);
            }

            PostProcessing.endBloom(bloomBuffer);

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.pushMatrix();
            GlStateManager.scale(s, s, 1.0f);
            float basePanelX = drawX / s;
            float basePanelY = drawY / s;
            float basePanelW = drawW / s;
            float bodyOffsetY = 3.0f;

            float tx = basePanelX + basePanelW / 2f - Fonts.widthMCFormatted(titleFont, title) / 2f;
            float ty = basePanelY;
            Fonts.drawMCFormattedWithShadow(titleFont, title, tx, ty, 0xFFFFFFFF);

            float bx = basePanelX + paddingX;
            Fonts.drawMCFormattedWithShadow(textFont, l3, bx, basePanelY + bodyOffsetY + 0 * fontHeight, 0xFFFFFFFF);
            Fonts.drawMCFormattedWithShadow(textFont, l2, bx, basePanelY + bodyOffsetY + 1 * fontHeight, 0xFFFFFFFF);
            Fonts.drawMCFormattedWithShadow(textFont, l1, bx, basePanelY + bodyOffsetY + 2 * fontHeight, 0xFFFFFFFF);
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();

            lastWidth = scaledW;
            lastHeight = scaledH;
        }
    }

    public float getLastWidth() {
        return lastWidth <= 0 ? 140f : lastWidth;
    }

    public float getLastHeight() {
        return lastHeight <= 0 ? 160f : lastHeight;
    }
}
