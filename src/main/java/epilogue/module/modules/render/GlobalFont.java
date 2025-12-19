package epilogue.module.modules.render;

import epilogue.font.FontTransformer;
import epilogue.module.Module;
import epilogue.module.ModuleCategory;
import epilogue.value.values.FloatValue;
import epilogue.value.values.ModeValue;

import java.awt.Font;

public class GlobalFont extends Module {
    private final FontTransformer fontTransformer = FontTransformer.getInstance();
    
    public final ModeValue fontType = new ModeValue("Font", 1, fontTransformer.getAvailableFonts());
    public final FloatValue fontSize = new FloatValue("Size", 30.0f, 18.0f, 120.0f);
    
    public GlobalFont() {
        super("GlobalFont", true);
    }
    
    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.RENDER;
    }
    
    @Override
    public void onEnabled() {
        updateFont();
    }
    
    @Override
    public void onDisabled() {
        fontTransformer.setFont("minecraft", 18.0f);
    }
    
    @Override
    public void verifyValue(String valueName) {
        if (isEnabled()) {
            updateFont();
        }
    }
    
    private void updateFont() {
        String selectedFont = fontType.getModeString();
        float size = fontSize.getValue();
        fontTransformer.setFont(selectedFont, size);
    }
    
    public Font getCurrentFont() {
        return fontTransformer.getFont(fontType.getModeString(), fontSize.getValue());
    }
    
    public boolean isCustomFont() {
        return isEnabled() && !fontTransformer.isMinecraftFont();
    }
    
    public String getCurrentFontName() {
        return isEnabled() ? fontType.getModeString() : "minecraft";
    }
    
    public float getCurrentFontSize() {
        return isEnabled() ? fontSize.getValue() : 18.0f;
    }
}
