package epilogue.ui.mainmenu.altmanager.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import epilogue.config.AccountConfig;
import epilogue.ui.mainmenu.altmanager.GuiAltManager;
import epilogue.ui.mainmenu.altmanager.auth.Account;
import epilogue.ui.mainmenu.altmanager.utils.Notification;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GuiSessionLogin extends GuiScreen {
    private String status;
    private GuiButton cancelButton;
    private final GuiScreen previousScreen;
    private GuiTextField sessionField;

    public GuiSessionLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        sessionField = new GuiTextField(1, fontRendererObj, width / 2 - 100, height / 2, 200, 20);
        sessionField.setMaxStringLength(32767);
        sessionField.setFocused(true);
        buttonList.clear();
        buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 + 35, "Login"));
        buttonList.add(cancelButton = new GuiButton(0, width / 2 - 100, height / 2 + 65, "Cancel"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        sessionField.drawTextBox();
        if (status != null) {
            fontRendererObj.drawStringWithShadow(status, (float) width / 2 - 100, (float) height / 2 - 20, -1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        sessionField.textboxKeyTyped(typedChar, keyCode);
        if (keyCode == Keyboard.KEY_RETURN && sessionField.isFocused()) {
            actionPerformed(this.buttonList.get(0));
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(cancelButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button != null && button.id == 0) {
            mc.displayGuiScreen(new GuiAltManager(previousScreen, null));
        }
        if (button != null && button.id == 1) {
            String type;
            try {
                String token = sessionField.getText();
                type = "Access Token";
                String[] playerInfo = getProfileInfo(token);
                epilogue.ui.mainmenu.altmanager.auth.SessionManager.setSession(
                        new Session(playerInfo[0], playerInfo[1], token, "mojang")
                );
                status = "§2Logged in as " + playerInfo[0];

                Account sessionAccount = new Account(
                        "",
                        token,
                        playerInfo[0],
                        System.currentTimeMillis(),
                        playerInfo[1]
                );
                AccountConfig.add(sessionAccount);
                mc.displayGuiScreen(new GuiAltManager(previousScreen, new Notification(status, 5000L)));
            } catch (Exception e) {
                status = "§4Invalid token";
            }
        }
    }

    public static String[] getProfileInfo(String token) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            request.setHeader("Authorization", "Bearer " + token);
            try (CloseableHttpResponse response = client.execute(request)) {
                String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
                String IGN = jsonObject.get("name").getAsString();
                String UUID = jsonObject.get("id").getAsString();
                return new String[]{IGN, UUID};
            }
        }
    }
}