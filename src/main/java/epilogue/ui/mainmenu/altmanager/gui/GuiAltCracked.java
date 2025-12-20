package epilogue.ui.mainmenu.altmanager.gui;

import epilogue.ui.mainmenu.altmanager.auth.SessionManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

public class GuiAltCracked extends GuiScreen {
    private final GuiScreen previousScreen;
    private GuiTextField nameField;

    public GuiAltCracked(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        nameField = new GuiTextField(1, fontRendererObj, width / 2 - 100, height / 2, 200, 20);
        nameField.setMaxStringLength(32);
        nameField.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 + 35, "Login"));
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 2 + 65, "Cancel"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        nameField.drawTextBox();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        nameField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN && nameField.isFocused()) {
            actionPerformed(this.buttonList.get(0));
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(this.buttonList.get(1));
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(previousScreen);
        } else if (button.id == 1) {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                SessionManager.setSession(new Session(name, "", "", "mojang"));
            }
            mc.displayGuiScreen(previousScreen);
        }
    }
}