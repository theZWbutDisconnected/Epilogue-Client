package epilogue.ui.mainmenu.altmanager.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import epilogue.config.AccountConfig;
import epilogue.ui.mainmenu.altmanager.GuiAltManager;
import epilogue.ui.mainmenu.altmanager.auth.Account;
import epilogue.ui.mainmenu.altmanager.auth.SessionManager;
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
    private GuiButton loginButton;
    private GuiButton cancelButton;
    private final GuiScreen previousScreen;
    private GuiTextField sessionField;

    public GuiSessionLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        sessionField = new GuiTextField(1, fontRendererObj, width / 2 - 100, height / 2 - 10, 200, 20);
        sessionField.setMaxStringLength(32767);
        sessionField.setFocused(true);

        buttonList.clear();
        buttonList.add(loginButton = new GuiButton(1, width / 2 - 100, height / 2 + 30, 200, 20, "Login"));
        buttonList.add(cancelButton = new GuiButton(0, width / 2 - 100, height / 2 + 55, 200, 20, "Cancel"));

        status = "Enter your Minecraft Access Token";
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        fontRendererObj.drawString("Token Login", width / 2, 20, 0xFFFFFF);
        fontRendererObj.drawString("Paste your Access Token below", width / 2, height / 2 - 40, 0xAAAAAA);

        if (status != null) {
            int color = status.contains("§4") ? 0xFF5555 : (status.contains("§2") ? 0x55FF55 : 0xFFFFFF);
            String cleanStatus = status.replaceAll("§.", "");
            fontRendererObj.drawString(cleanStatus, width / 2, height / 2 - 25, color);
        }

        sessionField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (sessionField.isFocused() && sessionField.textboxKeyTyped(typedChar, keyCode)) {

        } else if (keyCode == Keyboard.KEY_RETURN) {
            actionPerformed(loginButton);
        } else if (keyCode == Keyboard.KEY_ESCAPE) {
            actionPerformed(cancelButton);
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        sessionField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            mc.displayGuiScreen(previousScreen);
            return;
        }

        if (button.id == 1) {
            loginButton.enabled = false;
            status = "§eContacting Mojang servers...";
            mc.scheduleResourcesRefresh();

            new Thread(() -> {
                try {
                    String rawToken = sessionField.getText().trim();
                    if (rawToken.isEmpty()) {
                        throw new IllegalArgumentException("Token is empty!");
                    }


                    String token = rawToken.replaceAll("[\\s\n\r]+", "");

                    String[] profile = getProfileInfo(token);


                    Session newSession = new Session(profile[0], profile[1], token, "mojang");
                    SessionManager.setSession(newSession);


                    Account account = new Account(
                            "",
                            token,
                            profile[0],
                            System.currentTimeMillis(),
                            profile[1]
                    );
                    AccountConfig.add(account);

                    status = "§2Success! Logged in as " + profile[0];


                    mc.addScheduledTask(() -> {
                        mc.displayGuiScreen(new GuiAltManager(previousScreen,
                                new Notification("§aLogged in as §f" + profile[0], 5000L)));
                    });

                } catch (Exception e) {
                    String msg = "§4Login failed";
                    if (e instanceof IllegalArgumentException) {
                        msg = "§4Invalid input: " + e.getMessage();
                    } else if (e instanceof IOException) {
                        String emsg = e.getMessage() != null ? e.getMessage() : e.toString();
                        if (emsg.contains("401")) {
                            msg = "§4Invalid or expired token";
                        } else if (emsg.contains("404")) {
                            msg = "§4API error (check URL)";
                        } else {
                            msg = "§4Network error: " + emsg;
                        }
                    } else {
                        msg = "§4Unexpected error: " + e.getClass().getSimpleName();
                    }
                    e.printStackTrace();

                    final String finalMsg = msg;
                    mc.addScheduledTask(() -> {
                        status = finalMsg;
                        loginButton.enabled = true;
                    });
                }
            }, "TokenLoginThread").start();
        }
    }


    public static String[] getProfileInfo(String token) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            request.setHeader("Authorization", "Bearer " + token);

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                if (statusCode != 200) {
                    throw new IOException("HTTP " + statusCode + ": " + responseBody);
                }

                JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();

                if (!json.has("name") || !json.has("id")) {
                    throw new IOException("Missing 'name' or 'id' in response: " + responseBody);
                }

                String name = json.get("name").getAsString();
                String uuid = json.get("id").getAsString().replace("-", "");

                if (name.isEmpty() || uuid.length() != 32) {
                    throw new IOException("Invalid name or UUID format");
                }

                return new String[]{name, uuid};
            }
        }
    }
}
