package epilogue.ui.mainmenu.altmanager;

import epilogue.config.AccountConfig;
import epilogue.ui.mainmenu.GuiMainMenu;
import epilogue.ui.mainmenu.altmanager.auth.Account;
import epilogue.ui.mainmenu.altmanager.auth.MicrosoftAuth;
import epilogue.ui.mainmenu.altmanager.auth.SessionManager;
import epilogue.ui.mainmenu.altmanager.gui.GuiAltCracked;
import epilogue.ui.mainmenu.altmanager.gui.GuiMicrosoftAuth;
import epilogue.ui.mainmenu.altmanager.gui.GuiSessionLogin;
import epilogue.ui.mainmenu.altmanager.utils.Notification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import org.lwjgl.input.Keyboard;

import javax.swing.*;
import java.awt.Component;
import java.awt.HeadlessException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class GuiAltManager extends GuiScreen {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private int selectedAccount = -1;
    private GuiButton loginButton;
    private GuiButton deleteButton;
    private GuiButton cancelButton;
    private final GuiScreen previousScreen;
    private ExecutorService executor;
    private Notification notification;
    private CompletableFuture<Void> task;
    private GuiAccountList guiAccountList;

    public GuiAltManager(GuiScreen previousScreen, Notification notification) {
        this.previousScreen = previousScreen;
        this.notification = notification;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();
        buttonList.add(loginButton = new GuiButton(0, this.width / 2 - 160, this.height - 48, 78, 20, "Login"));
        buttonList.add(new GuiButton(5, this.width / 2 + 3, this.height - 48, 78, 20, "Token"));
        buttonList.add(new GuiButton(1, this.width / 2 - 160, this.height - 24, 78, 20, "Microsoft"));
        buttonList.add(new GuiButton(4, this.width / 2 + 3, this.height - 24, 78, 20, "Offline"));
        buttonList.add(new GuiButton(7, this.width / 2 - 78, this.height - 24, 78, 20, "Change Skin"));
        buttonList.add(deleteButton = new GuiButton(2, this.width / 2 + 84, this.height - 48, 78, 20, "Delete"));
        buttonList.add(cancelButton = new GuiButton(3, this.width / 2 + 84, this.height - 24, 78, 20, "Back"));
        guiAccountList = new GuiAccountList(mc);
        guiAccountList.registerScrollButtons(11, 12);
        updateScreen();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float renderPartialTicks) {
        this.drawDefaultBackground();
        if (guiAccountList != null) {
            guiAccountList.drawScreen(mouseX, mouseY, renderPartialTicks);
        }
        drawCenteredString(fontRendererObj, String.format("§rAccount Manager §8(§7%s§8)§r", AccountConfig.size()), width / 2, 10, -1);
        if (notification != null && !notification.isExpired()) {
            drawCenteredString(this.fontRendererObj, notification.getMessage(), mc.currentScreen.width / 2, 22, -1);
        } else {
            drawCenteredString(this.fontRendererObj, "Username: §7" + mc.getSession().getUsername(), mc.currentScreen.width / 2, 22, -1);
        }
        super.drawScreen(mouseX, mouseY, renderPartialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public void updateScreen() {
        if (loginButton != null && deleteButton != null) {
            loginButton.enabled = deleteButton.enabled = selectedAccount >= 0 && (task == null || task.isDone());
        }
        if (notification != null && notification.isExpired()) {
            notification = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (guiAccountList != null) {
            guiAccountList.handleMouseInput();
        }
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        switch (keyCode) {
            case Keyboard.KEY_UP:
                if (selectedAccount > 0) {
                    --selectedAccount;
                    guiAccountList.scrollBy(-guiAccountList.getSlotHeight());
                    if (isCtrlKeyDown()) {
                        AccountConfig.swap(selectedAccount, selectedAccount + 1);
                    }
                }
                break;
            case Keyboard.KEY_DOWN:
                if (selectedAccount < AccountConfig.size() - 1) {
                    ++selectedAccount;
                    guiAccountList.scrollBy(guiAccountList.getSlotHeight());
                    if (isCtrlKeyDown()) {
                        AccountConfig.swap(selectedAccount, selectedAccount - 1);
                    }
                }
                break;
            case Keyboard.KEY_RETURN:
                if (loginButton.enabled) actionPerformed(loginButton);
                break;
            case Keyboard.KEY_DELETE:
                if (deleteButton.enabled) actionPerformed(deleteButton);
                break;
            case Keyboard.KEY_ESCAPE:
                actionPerformed(cancelButton);
                break;
        }

        if (isKeyComboCtrlC(keyCode) && selectedAccount >= 0 && selectedAccount < AccountConfig.size()) {
            Account acc = AccountConfig.get(selectedAccount);
            if (acc != null && !StringUtils.isBlank(acc.getUsername()) && !"???".equals(acc.getUsername())) {
                setClipboardString(acc.getUsername());
                this.notification = new Notification("§aCopied username to clipboard!", 2000L);
            } else if (acc != null && !StringUtils.isBlank(acc.getAccessToken())) {
                setClipboardString(acc.getAccessToken());
                this.notification = new Notification("§aCopied access token to clipboard!", 2000L);
            }
        }
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button == null || !button.enabled) return;

        switch (button.id) {
            case 0:
                if (task != null && !task.isDone()) return;
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newSingleThreadExecutor();
                }
                final Account account = AccountConfig.get(selectedAccount);
                final String originalUsername = StringUtils.isBlank(account.getUsername()) ? "???" : account.getUsername();
                notification = new Notification(String.format("§7Logging in... (%s)§r", originalUsername), -1L);
                updateScreen();

                if (StringUtils.isBlank(account.getRefreshToken()) && !StringUtils.isBlank(account.getAccessToken())) {
                    task = CompletableFuture.runAsync(() -> {
                        try {
                            String token = account.getAccessToken();
                            String[] playerInfo = GuiSessionLogin.getProfileInfo(token);
                            SessionManager.setSession(new Session(playerInfo[0], playerInfo[1], token, "mojang"));
                            account.setUsername(playerInfo[0]);
                            account.setTimestamp(System.currentTimeMillis());
                            this.notification = new Notification(String.format("§aLogged in as %s!§r", playerInfo[0]), 5000L);
                        } catch (IOException ioe) {
                            this.notification = new Notification(String.format("§cLogin failed for %s: %s§r", originalUsername,
                                    ioe.getMessage() != null && ioe.getMessage().contains("401") ? "Invalid/Expired Token" : "API Error"), 5000L);
                        } catch (Exception e) {
                            this.notification = new Notification(String.format("§cLogin failed for %s: Error processing profile§r", originalUsername), 5000L);
                        }
                    }, executor).whenComplete((res, ex) -> updateScreen());
                } else if (!StringUtils.isBlank(account.getRefreshToken())) {
                    final AtomicReference<String> currentRefreshToken = new AtomicReference<>(account.getRefreshToken());
                    final AtomicReference<String> currentAccessToken = new AtomicReference<>(account.getAccessToken());

                    CompletableFuture<Session> loginAttemptFuture = MicrosoftAuth.login(currentAccessToken.get(), executor)
                            .handle((session, error) -> {
                                if (session != null) {
                                    this.notification = new Notification(String.format("§aSuccessful login! (%s)§r", session.getUsername()), 5000L);
                                    return CompletableFuture.completedFuture(session);
                                }
                                if (StringUtils.isBlank(currentRefreshToken.get())) {
                                    throw new RuntimeException("Current access token invalid and no refresh token available.");
                                }
                                this.notification = new Notification(String.format("§7Refreshing Microsoft access tokens... (%s)§r", originalUsername), -1L);
                                return MicrosoftAuth.refreshMSAccessTokens(currentRefreshToken.get(), executor)
                                        .thenComposeAsync(msAccessTokens -> {
                                            this.notification = new Notification(String.format("§7Acquiring Xbox access token... (%s)§r", originalUsername), -1L);
                                            currentRefreshToken.set(msAccessTokens.get("refresh_token"));
                                            return MicrosoftAuth.acquireXboxAccessToken(msAccessTokens.get("access_token"), executor);
                                        }, executor)
                                        .thenComposeAsync(xboxAccessToken -> {
                                            this.notification = new Notification(String.format("§7Acquiring Xbox XSTS token... (%s)§r", originalUsername), -1L);
                                            return MicrosoftAuth.acquireXboxXstsToken(xboxAccessToken, executor);
                                        }, executor)
                                        .thenComposeAsync(xboxXstsData -> {
                                            this.notification = new Notification(String.format("§7Acquiring Minecraft access token... (%s)§r", originalUsername), -1L);
                                            return MicrosoftAuth.acquireMCAccessToken(xboxXstsData.get("Token"), xboxXstsData.get("uhs"), executor);
                                        }, executor)
                                        .thenComposeAsync(mcToken -> {
                                            this.notification = new Notification(String.format("§7Fetching your Minecraft profile... (%s)§r", originalUsername), -1L);
                                            currentAccessToken.set(mcToken);
                                            return MicrosoftAuth.login(mcToken, executor);
                                        }, executor);
                            })
                            .thenCompose(Function.identity());

                    task = loginAttemptFuture.thenAccept(finalSession -> {
                        account.setRefreshToken(currentRefreshToken.get());
                        account.setAccessToken(finalSession.getToken());
                        account.setUsername(finalSession.getUsername());
                        account.setTimestamp(System.currentTimeMillis());
                        SessionManager.setSession(finalSession);
                        if (this.notification == null || !this.notification.getMessage().startsWith("§aSuccessful login")) {
                            this.notification = new Notification(String.format("§aSuccessful login! (%s)§r", finalSession.getUsername()), 5000L);
                        }
                    }).exceptionally(ex -> {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        this.notification = new Notification(String.format("§cLogin failed for %s: %s§r", originalUsername, cause.getMessage()), 5000L);
                        return null;
                    }).whenComplete((res, ex) -> updateScreen());
                } else {
                    notification = new Notification(String.format("§cCannot login: Account %s has no token information.§r", originalUsername), 5000L);
                    updateScreen();
                }
                break;
            case 1:
                if (task != null && !task.isDone()) return;
                if (executor == null || executor.isShutdown()) {
                    executor = Executors.newSingleThreadExecutor();
                }

                notification = new Notification("§7Opening Microsoft login in browser...§r", -1L);
                updateScreen();

                task = GuiMicrosoftAuth.acquireMSAuthCode(executor)
                        .thenComposeAsync(code -> GuiMicrosoftAuth.acquireMSAccessTokens(code, executor), executor)
                        .thenComposeAsync(msTokens -> {
                            String msAccess = msTokens.get("access_token");
                            String msRefresh = msTokens.get("refresh_token");
                            return GuiMicrosoftAuth.acquireXboxAccessToken(msAccess, executor)
                                    .thenComposeAsync(xboxAccess -> GuiMicrosoftAuth.acquireXboxXstsToken(xboxAccess, executor), executor)
                                    .thenComposeAsync(xstsData -> GuiMicrosoftAuth.acquireMCAccessToken(
                                                    xstsData.get("Token"),
                                                    xstsData.get("uhs"),
                                                    executor
                                            ), executor)
                                    .thenComposeAsync(mcToken -> GuiMicrosoftAuth.login(mcToken, executor)
                                            .thenApply(session -> {
                                                java.util.Map<String, Object> result = new java.util.HashMap<>();
                                                result.put("session", session);
                                                result.put("refresh_token", msRefresh);
                                                result.put("access_token", mcToken);
                                                return result;
                                            }), executor);
                        }, executor)
                        .thenAccept(result -> {
                            Session session = (Session) result.get("session");
                            String refresh = (String) result.get("refresh_token");
                            String access = (String) result.get("access_token");

                            SessionManager.setSession(session);
                            Account newAcc = new Account(
                                    refresh,
                                    access,
                                    session.getUsername(),
                                    System.currentTimeMillis(),
                                    session.getPlayerID()
                            );
                            AccountConfig.add(newAcc);
                            this.notification = new Notification(String.format("§aSuccessful Microsoft login! (%s)§r", session.getUsername()), 5000L);
                        })
                        .exceptionally(ex -> {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            this.notification = new Notification("§cMicrosoft login failed: " + String.valueOf(cause.getMessage()) + "§r", 5000L);
                            return null;
                        })
                        .whenComplete((r, ex) -> updateScreen());
                break;
            case 2:
                if (selectedAccount >= 0 && selectedAccount < AccountConfig.size()) {
                    AccountConfig.remove(selectedAccount);
                    if (selectedAccount >= AccountConfig.size() && AccountConfig.size() > 0) {
                        selectedAccount = AccountConfig.size() - 1;
                    } else if (AccountConfig.size() == 0) {
                        selectedAccount = -1;
                    }
                }
                updateScreen();
                break;
            case 3:
                this.mc.displayGuiScreen(this.previousScreen instanceof GuiMainMenu ? this.previousScreen : new GuiMainMenu());
                break;
            case 4:
                mc.displayGuiScreen(new GuiAltCracked(this));
                break;
            case 5:
                mc.displayGuiScreen(new GuiSessionLogin(this));
                break;
            case 7:
                try {
                    JFileChooser jFileChooser = new JFileChooser() {
                        @Override
                        protected JDialog createDialog(Component parent) throws HeadlessException {
                            JDialog dialog = super.createDialog(parent);
                            dialog.setModal(true);
                            dialog.setAlwaysOnTop(true);
                            return dialog;
                        }
                    };
                    int returnVal = jFileChooser.showOpenDialog(null);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File skinFile = jFileChooser.getSelectedFile();
                        String url = "https://api.minecraftservices.com/minecraft/profile/skins";

                        if (!skinFile.getName().endsWith(".png")) {
                            SwingUtilities.invokeLater(() -> this.notification = new Notification("Its seems that the file isn't a skin..", 2000L));
                            break;
                        }

                        int result = JOptionPane.showConfirmDialog(null, "Is this a slim skin?", "alert", JOptionPane.YES_NO_CANCEL_OPTION);
                        if (result == JOptionPane.CANCEL_OPTION) break;
                        String skinType;
                        if (result == JOptionPane.YES_OPTION) {
                            skinType = "slim";
                        } else {
                            skinType = "classic";
                        }
                        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                            HttpPost post = new HttpPost(url);
                            post.setHeader("Accept", "*/*");
                            post.setHeader("Authorization", "Bearer " + mc.getSession().getToken());
                            post.setHeader("User-Agent", "MojangSharp/0.1");
                            String boundary = "----EpilogueSkinUpload" + System.currentTimeMillis();
                            post.setHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            OutputStreamWriter writer = new OutputStreamWriter(baos, java.nio.charset.StandardCharsets.UTF_8);
                            writer.write("--" + boundary + "\r\n");
                            writer.write("Content-Disposition: form-data; name=\"variant\"\r\n");
                            writer.write("Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                            writer.write(skinType);
                            writer.write("\r\n");
                            writer.flush();
                            writer.write("--" + boundary + "\r\n");
                            writer.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + skinFile.getName() + "\"\r\n");
                            writer.write("Content-Type: image/png\r\n\r\n");
                            writer.flush();

                            try (FileInputStream fis = new FileInputStream(skinFile)) {
                                byte[] buffer = new byte[4096];
                                int len;
                                while ((len = fis.read(buffer)) != -1) {
                                    baos.write(buffer, 0, len);
                                }
                            }

                            writer.write("\r\n");
                            writer.write("--" + boundary + "--\r\n");
                            writer.flush();

                            ByteArrayEntity entity = new ByteArrayEntity(baos.toByteArray());
                            post.setEntity(entity);

                            try (CloseableHttpResponse response = httpClient.execute(post)) {
                                int statusCode = response.getStatusLine().getStatusCode();
                                if (statusCode == 200 || statusCode == 204) {
                                    SwingUtilities.invokeLater(() -> this.notification = new Notification("Skin changed!", 2000L));
                                } else {
                                    SwingUtilities.invokeLater(() -> this.notification = new Notification("Failed to change skin.", 2000L));
                                }
                            }
                        }

                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> this.notification = new Notification("Failed to change skin.", 2000L));
                }
                break;
            default:
                if (guiAccountList != null) {
                    guiAccountList.actionPerformed(button);
                }
                break;
        }
    }

    class GuiAccountList extends GuiSlot {
        public GuiAccountList(Minecraft mc) {
            super(mc, GuiAltManager.this.width, GuiAltManager.this.height, 32, GuiAltManager.this.height - 64, 27);
        }

        @Override
        protected int getSize() {
            return AccountConfig.size();
        }

        @Override
        protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
            if (slotIndex < 0 || slotIndex >= getSize()) return;
            GuiAltManager.this.selectedAccount = slotIndex;
            GuiAltManager.this.updateScreen();
            if (isDoubleClick && loginButton.enabled) {
                GuiAltManager.this.actionPerformed(loginButton);
            }
        }

        @Override
        protected boolean isSelected(int slotIndex) {
            return slotIndex == GuiAltManager.this.selectedAccount;
        }

        @Override
        protected int getContentHeight() {
            return this.getSize() * this.slotHeight;
        }

        @Override
        protected void drawBackground() {
            GuiAltManager.this.drawDefaultBackground();
        }

        @Override
        protected void drawSlot(int entryID, int x, int y, int k, int mouseXIn, int mouseYIn) {
            if (entryID < 0 || entryID >= getSize()) return;
            Account account = AccountConfig.get(entryID);
            String username = StringUtils.isBlank(account.getUsername()) ? "???" : account.getUsername();
            String accountType = "";
            String accountTypeColor = "§7";
            if (!StringUtils.isBlank(account.getRefreshToken())) {
                accountType = " (Microsoft)";
                accountTypeColor = "§9";
            } else if (!StringUtils.isBlank(account.getAccessToken())) {
                accountType = " (Token)";
                accountTypeColor = "§6";
            } else if ("???".equals(username)) {
                accountTypeColor = "§7";
            }
            String displayName = String.format("%s%s%s§r",
                    SessionManager.getSession().getUsername().equals(username) ? "§a§l" : accountTypeColor,
                    username,
                    accountType
            );
            GuiAltManager.this.drawString(GuiAltManager.this.fontRendererObj, displayName, x + 30, y + 3, -1);
            String time = String.format("§8§o%s§r", sdf.format(new Date(account.getTimestamp())));
            renderHead(x + 3, y + 1f, 21, account.getUUID());
            GuiAltManager.this.drawString(GuiAltManager.this.fontRendererObj, time, x + 30, y + 14, -1);
        }
    }

    private void renderHead(final double x, final double y, final int size, String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            uuid = "8667ba71-b85a-4004-af54-457a9734eed7";
        }
        ResourceLocation skin = new ResourceLocation("textures/entity/steve.png");
        mc.getTextureManager().bindTexture(skin);
        float fx = (float) x;
        float fy = (float) y;
        float fSize = (float) size;
        Gui.drawModalRectWithCustomSizedTexture((int) fx, (int) fy, 0.0f, 0.0f, size, size, fSize, fSize);
    }
}