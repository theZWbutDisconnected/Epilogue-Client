package epilogue.ui.clickgui.dropdown;

import epilogue.module.ModuleCategory;
import epilogue.util.render.ColorUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DropdownClickGui extends GuiScreen {

    private List<CategoryPanel> categoryPanels;
    public boolean binding;

    @Override
    public void initGui() {
        if (categoryPanels == null) {
            categoryPanels = new ArrayList<>();
            for (ModuleCategory category : ModuleCategory.values()) {
                categoryPanels.add(new CategoryPanel(category, null));
            }
        }

        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.initGui();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE && !binding) {
            mc.displayGuiScreen(null);
            return;
        }
        categoryPanels.forEach(categoryPanel -> categoryPanel.keyTyped(typedChar, keyCode));
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        binding = categoryPanels.stream().anyMatch(CategoryPanel::isTyping);

        ScaledResolution sr = new ScaledResolution(mc);
        
        Gui.drawRect(0, 0, sr.getScaledWidth(), sr.getScaledHeight(), 
            ColorUtil.applyOpacity(new Color(0, 0, 0), 0.6f).getRGB());

        for (CategoryPanel catPanels : categoryPanels) {
            catPanels.drawScreen(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        categoryPanels.forEach(cat -> cat.mouseClicked(mouseX, mouseY, mouseButton));
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        categoryPanels.forEach(cat -> cat.mouseReleased(mouseX, mouseY, state));
    }

    @Override
    public void onGuiClosed() {
        if (categoryPanels != null) {
            for (CategoryPanel panel : categoryPanels) {
                panel.onGuiClosed();
            }
        }
    }
}