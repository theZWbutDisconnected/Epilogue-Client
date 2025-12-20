package epilogue.ui.mainmenu.altmanager.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.Session;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

public class GuiMicrosoftAuth extends GuiScreen {
    private static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    private static final int PORT = 25575;
    private static final String REDIRECT_URI = "http://localhost:" + PORT + "/callback";
    private static final String SCOPE = "XboxLive.signin XboxLive.offline_access";

    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setSocketTimeout(10000)
            .build();

    public static CompletableFuture<String> acquireMSAuthCode(Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            String state = UUID.randomUUID().toString().replace("-", "");
            String url = "https://login.live.com/oauth20_authorize.srf"
                    + "?client_id=" + CLIENT_ID
                    + "&response_type=code"
                    + "&scope=" + urlEncode(SCOPE)
                    + "&redirect_uri=" + urlEncode(REDIRECT_URI)
                    + "&state=" + state;

            String logMessage = "[Epilogue MS-Login] Open this URL in your browser if it does not open automatically: " + url;
            System.out.println(logMessage);
            System.err.println(logMessage);

            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI.create(url));
                }
            } catch (Exception ignored) {
            }
            try (ServerSocket serverSocket = new ServerSocket()) {
                serverSocket.bind(new InetSocketAddress("localhost", PORT));

                serverSocket.setSoTimeout(300000);
                try (Socket socket = serverSocket.accept()) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line = reader.readLine();
                    if (line == null) throw new RuntimeException("Empty response");
                    String firstLine = line;
                    int start = firstLine.indexOf("GET /callback?");
                    int end = firstLine.indexOf(" HTTP/1.1");
                    if (start < 0 || end < 0) throw new RuntimeException("Invalid callback");
                    String query = firstLine.substring("GET /callback?".length(), end);
                    Map<String, String> params = parseQuery(query);
                    String returnedState = params.get("state");
                    if (returnedState == null || !returnedState.equals(state)) {
                        throw new RuntimeException("Invalid state");
                    }
                    String code = params.get("code");
                    if (code == null) throw new RuntimeException("No code");
                    String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><body>You can now return to Minecraft.</body></html>";
                    socket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                    return code;
                }
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft auth code", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireMSAccessTokens(String code, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://login.live.com/oauth20_token.srf");
                request.setConfig(REQUEST_CONFIG);
                String body = "client_id=" + CLIENT_ID
                        + "&grant_type=authorization_code"
                        + "&code=" + urlEncode(code)
                        + "&redirect_uri=" + urlEncode(REDIRECT_URI)
                        + "&scope=" + urlEncode(SCOPE);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                Map<String, String> map = new HashMap<>();
                map.put("access_token", json.get("access_token").getAsString());
                map.put("refresh_token", json.get("refresh_token").getAsString());
                return map;
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Microsoft access tokens", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> refreshMSAccessTokens(String refreshToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://login.live.com/oauth20_token.srf");
                request.setConfig(REQUEST_CONFIG);
                String body = "client_id=" + CLIENT_ID
                        + "&grant_type=refresh_token"
                        + "&refresh_token=" + urlEncode(refreshToken)
                        + "&scope=" + urlEncode(SCOPE);
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                Map<String, String> map = new HashMap<>();
                map.put("access_token", json.get("access_token").getAsString());
                map.put("refresh_token", json.get("refresh_token").getAsString());
                return map;
            } catch (Exception e) {
                throw new CompletionException("Unable to refresh Microsoft access tokens", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(String msAccessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://user.auth.xboxlive.com/user/authenticate");
                request.setConfig(REQUEST_CONFIG);
                JsonObject payload = new JsonObject();
                payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
                payload.addProperty("TokenType", "JWT");
                JsonObject properties = new JsonObject();
                properties.addProperty("AuthMethod", "RPS");
                properties.addProperty("SiteName", "user.auth.xboxlive.com");
                properties.addProperty("RpsTicket", "d=" + msAccessToken);
                payload.add("Properties", properties);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                return json.get("Token").getAsString();
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox access token", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(String xboxAccessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                request.setConfig(REQUEST_CONFIG);
                JsonObject payload = new JsonObject();
                payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                payload.addProperty("TokenType", "JWT");
                JsonObject properties = new JsonObject();
                properties.addProperty("SandboxId", "RETAIL");
                com.google.gson.JsonArray userTokens = new com.google.gson.JsonArray();
                userTokens.add(xboxAccessToken);
                properties.add("UserTokens", userTokens);
                payload.add("Properties", properties);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                String token = json.get("Token").getAsString();
                String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                        .get(0).getAsJsonObject().get("uhs").getAsString();
                Map<String, String> map = new HashMap<>();
                map.put("Token", token);
                map.put("uhs", uhs);
                return map;
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox XSTS token", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(String xstsToken, String uhs, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");
                request.setConfig(REQUEST_CONFIG);
                JsonObject payload = new JsonObject();
                payload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                return json.get("access_token").getAsString();
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(String mcToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
                return Optional.ofNullable(json.get("id"))
                        .map(JsonElement::getAsString)
                        .filter(uuid -> !StringUtils.isBlank(uuid))
                        .map(uuid -> new Session(
                                json.get("name").getAsString(),
                                uuid,
                                mcToken,
                                Session.Type.MOJANG.toString()
                        ))
                        .orElseThrow(() -> new Exception("Invalid profile response"));
            } catch (InterruptedException e) {
                throw new CancellationException("Minecraft profile fetching was cancelled");
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile", e);
            }
        }, executor);
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        String[] parts = query.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx <= 0) continue;
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            map.put(key, value);
        }
        return map;
    }
}