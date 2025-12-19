package epilogue.ui.mainmenu;

import epilogue.util.render.RenderUtil;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;

public class GuiMainMenu extends GuiScreen {
    private static final ResourceLocation BG = new ResourceLocation("minecraft", "epilogue/mainmenu/mainmenubg.png");
    private static final ResourceLocation EPILO_LOGO = new ResourceLocation("minecraft", "epilogue/mainmenu/EpilogueLogo.png");
    private static final ResourceLocation ICON_SINGLEPLAYER = new ResourceLocation("minecraft", "epilogue/mainmenu/singleplayer.png");
    private static final ResourceLocation ICON_MULTIPLAYER = new ResourceLocation("minecraft", "epilogue/mainmenu/multiplayer.png");
    private static final ResourceLocation ICON_ALTMANAGER = new ResourceLocation("minecraft", "epilogue/mainmenu/altmanager.png");
    private static final ResourceLocation ICON_OPTIONS = new ResourceLocation("minecraft", "epilogue/mainmenu/option.png");
    private static final ResourceLocation ICON_QUIT = new ResourceLocation("minecraft", "epilogue/mainmenu/quit.png");

    private float animatedMouseX;
    private float animatedMouseY;
    private float hoverSp;
    private float hoverMp;
    private float hoverAlt;
    private float hoverOpt;
    private float hoverQuit;

    private Particle rain;

    @Override
    public void initGui() {
        super.initGui();

        this.animatedMouseX = this.width / 2.0f;
        this.animatedMouseY = this.height / 2.0f;
        this.hoverSp = 0.0f;
        this.hoverMp = 0.0f;
        this.hoverAlt = 0.0f;
        this.hoverOpt = 0.0f;
        this.hoverQuit = 0.0f;

        this.rain = new Particle();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.animatedMouseX == 0 && this.animatedMouseY == 0) {
            this.animatedMouseX = this.width / 2.0f;
            this.animatedMouseY = this.height / 2.0f;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableCull();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glViewport(0, 0, this.mc.displayWidth, this.mc.displayHeight);
        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        this.drawDefaultBackground();
        drawSigmaJelloBackground(mouseX, mouseY, partialTicks);
        ScaledResolution sr = new ScaledResolution(this.mc);
        if (this.rain != null) {
            this.rain.update(sr);
            this.rain.render(sr);
        }
        drawEpiloLogo();
        drawSigmaJelloButtons(mouseX, mouseY);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSigmaJelloBackground(int mouseX, int mouseY, float partialTicks) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        float targetX = mouseX;
        float targetY = mouseY;
        this.animatedMouseX += (targetX - this.animatedMouseX) * 0.085f;
        this.animatedMouseY += (targetY - this.animatedMouseY) * 0.085f;
        float screenW = sr.getScaledWidth();
        float screenH = sr.getScaledHeight();

        //这里填png宽高
        float texW = 4096.0f;
        float texH = 1755.0f;
        float scale = Math.max(screenW / texW, screenH / texH);
        float minOverflowX = screenW * 0.18f;
        float minOverflowY = screenH * 0.08f;
        float needScaleX = (screenW + minOverflowX) / texW;
        float needScaleY = (screenH + minOverflowY) / texH;
        scale = Math.max(scale, Math.max(needScaleX, needScaleY));
        float drawW = texW * scale;
        float drawH = texH * scale;
        float overflowX = Math.max(0.0f, drawW - screenW);
        float overflowY = Math.max(0.0f, drawH - screenH);
        float nx = (this.animatedMouseX / Math.max(1.0f, screenW) - 0.5f) * 2.0f;
        float ny = (this.animatedMouseY / Math.max(1.0f, screenH) - 0.5f) * 2.0f;
        float parallaxStrengthX = 0.55f;
        float parallaxStrengthY = 0.35f;
        float panX = (overflowX * 0.5f) + (nx * overflowX * 0.5f * parallaxStrengthX);
        float panY = (overflowY * 0.5f) + (ny * overflowY * 0.5f * parallaxStrengthY);
        float x = -panX;
        float y = -panY;
        this.mc.getTextureManager().bindTexture(BG);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        applyHighQualityTextureSampling();
        RenderUtil.drawTexturedRect(x, y, drawW, drawH);
    }

    private void drawEpiloLogo() {
        ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;
        int w = 256;
        int h = 128;
        int x = centerX - w / 2;
        int y = centerY - 125;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GlStateManager.color(0.9f, 0.9f, 0.9f, 1.0f);
        this.mc.getTextureManager().bindTexture(EPILO_LOGO);
        applyHighQualityTextureSampling();
        RenderUtil.drawTexturedRect(x, y, w, h);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
    }

    private void drawSigmaJelloButtons(int mouseX, int mouseY) {
        ScaledResolution sr = new ScaledResolution(this.mc);
        int centerX = sr.getScaledWidth() / 2;
        int centerY = sr.getScaledHeight() / 2;
        int size = 64;
        int gap = 22;
        int count = 5;
        int totalWidth = count * size + (count - 1) * gap;
        int startX = centerX - totalWidth / 2;
        int y = centerY + 10;
        float speed = 0.22f;
        this.hoverSp = animateTowards(this.hoverSp, isHovering(mouseX, mouseY, startX + (size + gap) * 0, y, size, size) ? 1.0f : 0.0f, speed);
        this.hoverMp = animateTowards(this.hoverMp, isHovering(mouseX, mouseY, startX + (size + gap) * 1, y, size, size) ? 1.0f : 0.0f, speed);
        this.hoverAlt = animateTowards(this.hoverAlt, isHovering(mouseX, mouseY, startX + (size + gap) * 2, y, size, size) ? 1.0f : 0.0f, speed);
        this.hoverOpt = animateTowards(this.hoverOpt, isHovering(mouseX, mouseY, startX + (size + gap) * 3, y, size, size) ? 1.0f : 0.0f, speed);
        this.hoverQuit = animateTowards(this.hoverQuit, isHovering(mouseX, mouseY, startX + (size + gap) * 4, y, size, size) ? 1.0f : 0.0f, speed);
        drawIcon(startX + (size + gap) * 0, y, size, ICON_SINGLEPLAYER, this.hoverSp);
        drawIcon(startX + (size + gap) * 1, y, size, ICON_MULTIPLAYER, this.hoverMp);
        drawIcon(startX + (size + gap) * 2, y, size, ICON_ALTMANAGER, this.hoverAlt);
        drawIcon(startX + (size + gap) * 3, y, size, ICON_OPTIONS, this.hoverOpt);
        drawIcon(startX + (size + gap) * 4, y, size, ICON_QUIT, this.hoverQuit);
    }

    private void drawIcon(int x, int y, int size, ResourceLocation icon, float hoverT) {
        float alpha = 0.80f + 0.20f * hoverT;
        float scale = 1.0f + 0.08f * hoverT;
        float brightness = 0.92f + 0.08f * hoverT;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + size / 2.0f, y + size / 2.0f, 0);
        GlStateManager.scale(scale, scale, 1.0f);
        GlStateManager.translate(-(x + size / 2.0f), -(y + size / 2.0f), 0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.003921569f);
        GlStateManager.enableBlend();
        GlStateManager.color(brightness, brightness, brightness, alpha);
        this.mc.getTextureManager().bindTexture(icon);
        applyHighQualityTextureSampling();
        RenderUtil.drawTexturedRect(x, y, size, size);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static float animateTowards(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static void applyHighQualityTextureSampling() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            ScaledResolution sr = new ScaledResolution(this.mc);
            int centerX = sr.getScaledWidth() / 2;
            int centerY = sr.getScaledHeight() / 2;
            int size = 64;
            int gap = 22;
            int count = 5;
            int totalWidth = count * size + (count - 1) * gap;
            int startX = centerX - totalWidth / 2;
            int y = centerY + 10;
            if (isHovering(mouseX, mouseY, startX + (size + gap) * 0, y, size, size)) {
                this.mc.displayGuiScreen(new GuiSelectWorld(this));
                return;
            }
            if (isHovering(mouseX, mouseY, startX + (size + gap) * 1, y, size, size)) {
                this.mc.displayGuiScreen(new GuiMultiplayer(this));
                return;
            }
            if (isHovering(mouseX, mouseY, startX + (size + gap) * 2, y, size, size)) {
                this.mc.displayGuiScreen(new epilogue.ui.mainmenu.altmanager.GuiAltManager(this));
                return;
            }
            if (isHovering(mouseX, mouseY, startX + (size + gap) * 3, y, size, size)) {
                this.mc.displayGuiScreen(new GuiOptions(this, this.mc.gameSettings));
                return;
            }
            if (isHovering(mouseX, mouseY, startX + (size + gap) * 4, y, size, size)) {
                this.mc.shutdown();
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void onGuiClosed() {
    }
    private boolean isHovering(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }
}