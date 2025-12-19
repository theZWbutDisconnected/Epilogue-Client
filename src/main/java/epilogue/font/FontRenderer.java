package epilogue.font;

import epilogue.Epilogue;
import epilogue.module.modules.render.FontRender;
import net.minecraft.client.Minecraft;

import java.awt.*;

public class FontRenderer {
    
    public static void drawString(String text, float x, float y, int color) {
        FontTransformer transformer = FontTransformer.getInstance();
        
        if (transformer.isMinecraftFont() || !isGlobalFontEnabled()) {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int)x, (int)y, color, false);
            return;
        }
        
        Font font = transformer.getFont(transformer.getSelectedFontName(), transformer.getSelectedFontSize());
        if (font == null) {
            Minecraft.getMinecraft().fontRendererObj.drawString(text, (int)x, (int)y, color, false);
            return;
        }
        
        CustomFontRenderer.drawString(text, x, y, color, font);
    }
    
    public static void drawStringWithShadow(String text, float x, float y, int color) {
        FontTransformer transformer = FontTransformer.getInstance();
        
        if (transformer.isMinecraftFont() || !isGlobalFontEnabled()) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, color);
            return;
        }
        
        Font font = transformer.getFont(transformer.getSelectedFontName(), transformer.getSelectedFontSize());
        if (font == null) {
            Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(text, x, y, color);
            return;
        }
        
        CustomFontRenderer.drawStringWithShadow(text, x, y, color, font);
    }
    
    public static int getStringWidth(String text) {
        FontTransformer transformer = FontTransformer.getInstance();
        
        if (transformer.isMinecraftFont() || !isGlobalFontEnabled()) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        
        Font font = transformer.getFont(transformer.getSelectedFontName(), transformer.getSelectedFontSize());
        if (font == null) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        
        return CustomFontRenderer.getStringWidth(text, font);
    }
    
    public static int getFontHeight() {
        FontTransformer transformer = FontTransformer.getInstance();
        
        if (transformer.isMinecraftFont() || !isGlobalFontEnabled()) {
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        }
        
        Font font = transformer.getFont(transformer.getSelectedFontName(), transformer.getSelectedFontSize());
        if (font == null) {
            return Minecraft.getMinecraft().fontRendererObj.FONT_HEIGHT;
        }
        
        return CustomFontRenderer.getFontHeight(font);
    }
    
    public static String trimStringToWidth(String text, int width) {
        return Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(text, width);
    }
    
    public static String trimStringToWidth(String text, int width, boolean reverse) {
        return Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(text, width, reverse);
    }
    
    private static boolean isGlobalFontEnabled() {
        try {
            FontRender fontRender =
                (FontRender) Epilogue.moduleManager.getModule("FontRender");
            return fontRender != null && fontRender.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }
}
