package epilogue.module.modules.render;

import epilogue.Epilogue;
import epilogue.module.Module;
import epilogue.module.ModuleCategory;
import epilogue.value.values.ModeValue;
import org.lwjgl.input.Keyboard;

public class ClickGUI extends Module {
    
    private final ModeValue mode = new ModeValue("Mode", 4, new String[]{"IDEA", "Augustus", "Best", "Dropdown", "WebGUI"});

    public ClickGUI() {
        super("ClickGUI", false);
        this.setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        if (Epilogue.guiManager != null) {
            String selectedMode = mode.getModeString();
            
            if (selectedMode.equals("IDEA")) {
                if (Epilogue.guiManager.isClickGuiOpen()) {
                    Epilogue.guiManager.closeClickGui();
                } else {
                    Epilogue.guiManager.openClickGui();
                }
            } else if (selectedMode.equals("Augustus")) {
                if (Epilogue.guiManager.isAugustusGuiOpen()) {
                    Epilogue.guiManager.closeAugustusGui();
                } else {
                    Epilogue.guiManager.openAugustusGui();
                }
            } else if (selectedMode.equals("Best")) {
                if (Epilogue.guiManager.isBestGuiOpen()) {
                    Epilogue.guiManager.closeBestGui();
                } else {
                    Epilogue.guiManager.openBestGui();
                }
            } else if (selectedMode.equals("Dropdown")) {
                if (Epilogue.guiManager.isDropdownGuiOpen()) {
                    Epilogue.guiManager.closeDropdownGui();
                } else {
                    Epilogue.guiManager.openDropdownGui();
                }
            } else if (selectedMode.equals("WebGUI")) {
                Epilogue.guiManager.closeAllGuis();
                Epilogue.guiManager.openWebGui();
            }
        }
        this.setEnabled(false);
    }
    
    public ModeValue getMode() {
        return mode;
    }
    

    @Override
    public void onDisabled() {
    }

    @Override
    public ModuleCategory getCategory() {
        return ModuleCategory.RENDER;
    }
}
