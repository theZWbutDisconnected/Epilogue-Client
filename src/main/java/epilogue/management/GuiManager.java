package epilogue.management;

import net.minecraft.client.Minecraft;
import epilogue.ui.clickgui.dropdown.DropdownClickGui;

public class GuiManager {
    private final Minecraft mc = Minecraft.getMinecraft();
    private final DropdownClickGui dropdownClickGui;
    
    public GuiManager() {
        this.dropdownClickGui = new DropdownClickGui();
    }
    
    public void closeAllGuis() {
        closeDropdownGui();
    }

    public void openDropdownGui() {
        closeAllGuis();
        mc.displayGuiScreen(dropdownClickGui);
    }
    
    public void closeDropdownGui() {
        if (mc.currentScreen == dropdownClickGui) {
            mc.displayGuiScreen(null);
        }
    }
    
    public boolean isDropdownGuiOpen() {
        return mc.currentScreen == dropdownClickGui;
    }
}
