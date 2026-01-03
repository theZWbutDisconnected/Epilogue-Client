package epilogue.ui.clickgui.dropdown.components.settings;

import epilogue.ui.clickgui.dropdown.components.SettingComponent;
import epilogue.ui.clickgui.dropdown.utils.DropdownFontRenderer;
import epilogue.util.render.ColorUtil;
import epilogue.value.values.TextValue;

import java.awt.*;

public class TextComponent extends SettingComponent<TextValue> {

    private boolean isTyping = false;
    private String currentText = "";
    private int cursorPosition = 0;
    private int cursorBlink = 0;

    public TextComponent(TextValue textValue) {
        super(textValue);
        this.currentText = textValue.getValue();
    }

    @Override
    public void initGui() {
        currentText = getSetting().getValue();
        cursorPosition = currentText.length();
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!isTyping) return;


        if (typedChar >= 32 && typedChar <= 126) {

            currentText = currentText.substring(0, cursorPosition) + typedChar + currentText.substring(cursorPosition);
            cursorPosition++;
            applyValueChange(currentText);
        } else if (keyCode == 14) {
            if (cursorPosition > 0) {
                currentText = currentText.substring(0, cursorPosition - 1) + currentText.substring(cursorPosition);
                cursorPosition--;
                applyValueChange(currentText);
            }
        } else if (keyCode == 28) {
            isTyping = false;
        } else if (keyCode == 203) {
            if (cursorPosition > 0) {
                cursorPosition--;
            }
        } else if (keyCode == 205) {
            if (cursorPosition < currentText.length()) {
                cursorPosition++;
            }
        } else if (keyCode == 199) {
            cursorPosition = 0;
        } else if (keyCode == 207) {
            cursorPosition = currentText.length();
        }

        cursorBlink = 0;
    }

    @Override
    public void updateCountSize() {
        countSize = 1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        String name = getSetting().getName();


        Color nameColor = ColorUtil.applyOpacity(textColor, alpha);
        DropdownFontRenderer.drawString(name, x + 5, y + DropdownFontRenderer.getMiddleOfBox(height, 18), nameColor.getRGB(), 18);


        float textFieldWidth = width * 0.4f;
        float textFieldHeight = 16;
        float textFieldX = x + width - textFieldWidth - 5;
        float textFieldY = y + (height - textFieldHeight) / 2;

        Color fieldBgColor = ColorUtil.applyOpacity(new Color(30, 30, 30), alpha);
        net.minecraft.client.gui.Gui.drawRect((int)textFieldX, (int)textFieldY,
                (int)(textFieldX + textFieldWidth), (int)(textFieldY + textFieldHeight),
                fieldBgColor.getRGB());


        Color borderColor = isTyping ?
                ColorUtil.applyOpacity(new Color(100, 150, 255), alpha) :
                ColorUtil.applyOpacity(new Color(60, 60, 60), alpha);
        net.minecraft.client.gui.Gui.drawRect((int)textFieldX, (int)textFieldY,
                (int)(textFieldX + textFieldWidth), (int)textFieldY + 1, borderColor.getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)textFieldX, (int)(textFieldY + textFieldHeight - 1),
                (int)(textFieldX + textFieldWidth), (int)(textFieldY + textFieldHeight), borderColor.getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)textFieldX, (int)textFieldY,
                (int)textFieldX + 1, (int)(textFieldY + textFieldHeight), borderColor.getRGB());
        net.minecraft.client.gui.Gui.drawRect((int)(textFieldX + textFieldWidth - 1), (int)textFieldY,
                (int)(textFieldX + textFieldWidth), (int)(textFieldY + textFieldHeight), borderColor.getRGB());


        String displayText = currentText;
        if (displayText.isEmpty() && !isTyping) {
            displayText = "Click to type...";
        }

        Color textColorValue = displayText.equals("Click to type...") ?
                ColorUtil.applyOpacity(new Color(150, 150, 150), alpha) :
                ColorUtil.applyOpacity(Color.WHITE, alpha);


        int textY = (int)(textFieldY + (textFieldHeight - 8) / 2);
        DropdownFontRenderer.drawString(displayText, textFieldX + 4, textY, textColorValue.getRGB(), 16);


        if (isTyping && (cursorBlink / 10) % 2 == 0) {

            String beforeCursor = currentText.substring(0, cursorPosition);
            int cursorX = (int)(textFieldX + 4 + DropdownFontRenderer.getStringWidth(beforeCursor, 16));
            net.minecraft.client.gui.Gui.drawRect(cursorX, textY, cursorX + 1, textY + 9,
                    ColorUtil.applyOpacity(Color.WHITE, alpha).getRGB());
        }


        cursorBlink++;
        if (cursorBlink > 20) cursorBlink = 0;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isClickable(y + height)) {

            float textFieldWidth = width * 0.4f;
            float textFieldHeight = 16;
            float textFieldX = x + width - textFieldWidth - 5;
            float textFieldY = y + (height - textFieldHeight) / 2;

            if (isHovering(textFieldX, textFieldY, textFieldWidth, textFieldHeight, mouseX, mouseY)) {
                isTyping = !isTyping;
                if (!isTyping) {

                    applyValueChange(currentText);
                }
            } else {
                isTyping = false;
                applyValueChange(currentText);
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {

    }

    private boolean isHovering(float x, float y, float width, float height, int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}