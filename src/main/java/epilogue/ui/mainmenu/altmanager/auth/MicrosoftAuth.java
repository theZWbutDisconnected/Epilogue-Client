package epilogue.ui.mainmenu.altmanager.auth;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

public class MicrosoftAuth {
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectTimeout(10000)
            .setSocketTimeout(10000)
            .build();

    public static CompletableFuture<Map<String, String>> refreshMSAccessTokens(String refreshToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://login.live.com/oauth20_token.srf"));
                request.setConfig(REQUEST_CONFIG);
                String body = "client_id=00000000402b5328&grant_type=refresh_token&refresh_token=" + refreshToken + "&scope=service::user.auth.xboxlive.com::MBI_SSL";
                request.setHeader("Content-Type", "application/x-www-form-urlencoded");
                request.setEntity(new StringEntity(body));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                Map<String, String> map = new HashMap<>();
                map.put("access_token", json.get("access_token").getAsString());
                map.put("refresh_token", json.get("refresh_token").getAsString());
                return map;
            } catch (Exception e) {
                throw new CompletionException("Unable to refresh Microsoft access tokens!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(String msAccessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://user.auth.xboxlive.com/user/authenticate"));
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
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                return json.get("Token").getAsString();
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(String xboxAccessToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"));
                request.setConfig(REQUEST_CONFIG);
                JsonObject payload = new JsonObject();
                payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                payload.addProperty("TokenType", "JWT");
                JsonObject properties = new JsonObject();
                properties.addProperty("SandboxId", "RETAIL");
                JsonArrayWrapper userTokens = new JsonArrayWrapper();
                userTokens.addString(xboxAccessToken);
                properties.add("UserTokens", userTokens.get());
                payload.add("Properties", properties);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                String token = json.get("Token").getAsString();
                String uhs = json.getAsJsonObject("DisplayClaims").getAsJsonArray("xui")
                        .get(0).getAsJsonObject().get("uhs").getAsString();
                Map<String, String> map = new HashMap<>();
                map.put("Token", token);
                map.put("uhs", uhs);
                return map;
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Xbox XSTS token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<String> acquireMCAccessToken(String xstsToken, String uhs, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpPost request = new HttpPost(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"));
                request.setConfig(REQUEST_CONFIG);
                JsonObject payload = new JsonObject();
                payload.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xstsToken);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
                return json.get("access_token").getAsString();
            } catch (Exception e) {
                throw new CompletionException("Unable to acquire Minecraft access token!", e);
            }
        }, executor);
    }

    public static CompletableFuture<Session> login(String mcToken, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpGet request = new HttpGet(URI.create("https://api.minecraftservices.com/minecraft/profile"));
                request.setConfig(REQUEST_CONFIG);
                request.setHeader("Authorization", "Bearer " + mcToken);
                HttpResponse res = client.execute(request);
                JsonObject json = new JsonParser().parse(EntityUtils.toString(res.getEntity())).getAsJsonObject();
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
                throw new CancellationException("Minecraft profile fetching was cancelled!");
            } catch (Exception e) {
                throw new CompletionException("Unable to fetch Minecraft profile!", e);
            }
        }, executor);
    }

    private static class JsonArrayWrapper {
        private final com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        void addString(String s) { array.add(s); }
        com.google.gson.JsonArray get() { return array; }
    }
}