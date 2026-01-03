package epilogue.ui.chat;

import epilogue.ui.clickgui.menu.Fonts;
import epilogue.util.render.PostProcessing;
import epilogue.util.render.RoundedUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiChat extends GuiScreen {
    private String historyBuffer = "";
    private int sentHistoryCursor = -1;
    private boolean playerNamesFound;
    private int autocompleteIndex;
    private final List<String> foundPlayerNames = new ArrayList<>();
    protected GuiTextField inputField;
    private String defaultInputFieldText = "";
    private float openAnim = 0.0f;
    private boolean closing = false;
    private long animStartNs = 0L;
    private float animFrom = 0.0f;
    private float animTo = 1.0f;

    public GuiChat() {
    }

    public GuiChat(String defaultText) {
        this.defaultInputFieldText = defaultText;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.sentHistoryCursor = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new GuiTextField(0, this.fontRendererObj, 4, this.height - 12, this.width - 4, 12);
        this.inputField.setMaxStringLength(100);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setFocused(true);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setCanLoseFocus(false);
        this.openAnim = 0.0f;
        this.closing = false;
        startAnim(0.0f, 1.0f);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        this.mc.ingameGUI.getChatGUI().resetScroll();
    }

    @Override
    public void updateScreen() {
        if (this.inputField != null) {
            this.inputField.updateCursorCounter();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();
        if (i != 0) {
            if (i > 1) i = 1;
            if (i < -1) i = -1;
            if (!isShiftKeyDown()) i *= 7;
            this.mc.ingameGUI.getChatGUI().scroll(i);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {

        if (keyCode == 15) {
            this.autocompletePlayerNames();
        } else {
            this.playerNamesFound = false;
        }

        if (keyCode == 1) {
            startClose();
            return;
        }

        if (keyCode == 28 || keyCode == 156) {
            String s = this.inputField.getText().trim();
            if (!s.isEmpty()) {
                this.sendChatMessage(s);
            }
            startClose();
            return;
        }

        if (keyCode == 200) {
            this.getSentHistory(-1);
            return;
        }

        if (keyCode == 208) {
            this.getSentHistory(1);
            return;
        }

        if (keyCode == 201) {
            this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
            return;
        }

        if (keyCode == 209) {
            this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
            return;
        }

        this.inputField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            IChatComponent ichatcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
            if (this.handleComponentClick(ichatcomponent)) {
                return;
            }
        }

        this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateAnim();

        float eased = openAnim;
        eased = 1.0f - (float) Math.pow(1.0f - eased, 3.0);

        ScaledResolution sr = new ScaledResolution(this.mc);
        float barW = sr.getScaledWidth() - (2.0f * 2.0f);
        float barH = 12.0f;
        float barX = 2.0f;
        float barY = sr.getScaledHeight() - (barH + 2.0f);
        float animH = (barH + 2.0f) * eased;
        float animY = sr.getScaledHeight() - (animH + 2.0f);

        float slideOffset = (barH + 2.0f + 6.0f) * (1.0f - eased);
        barY += slideOffset;
        animY += slideOffset;

        float finalAnimY = animY;
        PostProcessing.drawBlur(barX, animY, barX + barW, sr.getScaledHeight(), () -> () -> {
            RoundedUtil.drawRound(barX, finalAnimY, barW, animH + 2.0f, 6.0f, new Color(255, 255, 255, 255));
        });

        RoundedUtil.drawRound(barX, animY, barW, animH + 2.0f, 6.0f, new Color(0, 0, 0, 95));

        this.inputField.yPosition = (int) (barY + (barH - 10.0f) / 2.0f);
        this.inputField.width = (int) (barW - 8.0f);
        this.inputField.xPosition = (int) (barX + 4.0f);
        this.inputField.drawTextBox();

        IChatComponent ichatcomponent = this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());
        if (ichatcomponent != null && ichatcomponent.getChatStyle().getChatHoverEvent() != null) {
            this.handleComponentHover(ichatcomponent, mouseX, mouseY);
        }

        if (this.openAnim > 0.85f) {
            String hint = "Enter to send, Tab to complete";
            Fonts.draw(Fonts.tiny(), hint, (int) (barX + 6.0f), (int) (animY - 6.0f), 0x66FFFFFF);
        }

        if (closing && openAnim <= 0.02f) {
            this.mc.displayGuiScreen(null);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void autocompletePlayerNames() {
        if (this.playerNamesFound) {
            this.inputField.deleteFromCursor(this.inputField.func_146197_a(-1, this.inputField.getCursorPosition(), false) - this.inputField.getCursorPosition());
            if (this.autocompleteIndex >= this.foundPlayerNames.size()) {
                this.autocompleteIndex = 0;
            }
        } else {
            int i = this.inputField.func_146197_a(-1, this.inputField.getCursorPosition(), false);
            this.foundPlayerNames.clear();
            this.autocompleteIndex = 0;
            String s = this.inputField.getText().substring(i).toLowerCase();
            String s1 = this.inputField.getText().substring(0, this.inputField.getCursorPosition());
            this.sendAutocompleteRequest(s1, s);
            if (this.foundPlayerNames.isEmpty()) {
                return;
            }
            this.playerNamesFound = true;
            this.inputField.deleteFromCursor(i - this.inputField.getCursorPosition());
        }

        if (this.foundPlayerNames.size() > 1) {
            StringBuilder stringbuilder = new StringBuilder();
            for (String s2 : this.foundPlayerNames) {
                if (stringbuilder.length() > 0) {
                    stringbuilder.append(", ");
                }
                stringbuilder.append(s2);
            }
            this.mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(stringbuilder.toString()), 1);
        }

        this.inputField.writeText(this.foundPlayerNames.get(this.autocompleteIndex++));
    }

    private void sendAutocompleteRequest(String leftOfCursor, String rightOfCursor) {
        if (!leftOfCursor.isEmpty()) {
            BlockPos blockpos = null;
            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                blockpos = this.mc.objectMouseOver.getBlockPos();
            }
            this.mc.thePlayer.sendQueue.addToSendQueue(new C14PacketTabComplete(leftOfCursor, blockpos));
        }
    }

    private void getSentHistory(int msgPos) {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.mc.ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp_int(i, 0, j);

        if (i != this.sentHistoryCursor) {
            if (i == j) {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            } else {
                if (this.sentHistoryCursor == j) {
                    this.historyBuffer = this.inputField.getText();
                }
                this.inputField.setText(this.mc.ingameGUI.getChatGUI().getSentMessages().get(i));
                this.sentHistoryCursor = i;
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static void open(String defaultText) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;
        if (defaultText == null) defaultText = "";
        mc.displayGuiScreen(defaultText.isEmpty() ? new GuiChat() : new GuiChat(defaultText));
    }

    private void startClose() {
        if (closing) return;
        closing = true;
        startAnim(openAnim, 0.0f);
    }

    private void startAnim(float from, float to) {
        this.animFrom = from;
        this.animTo = to;
        this.animStartNs = System.nanoTime();
    }

    private void updateAnim() {
        if (animStartNs == 0L) return;
        float t = (float) ((System.nanoTime() - animStartNs) / (double) 220_000_000L);
        if (t >= 1.0f) {
            openAnim = animTo;
            return;
        }
        t = Math.max(0.0f, Math.min(1.0f, t));
        float eased = 1.0f - (float) Math.pow(1.0f - t, 3.0);
        openAnim = animFrom + (animTo - animFrom) * eased;
    }
}
