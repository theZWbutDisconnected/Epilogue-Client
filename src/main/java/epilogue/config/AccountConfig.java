package epilogue.config;

import com.google.gson.*;
import epilogue.ui.mainmenu.altmanager.auth.Account;
import epilogue.Epilogue;
import epilogue.util.ChatUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AccountConfig {
    private static final List<Account> accounts = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger("EpilogueAccountConfig");
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final File ACCOUNTS_FILE = new File("./accounts.json");

    public static void init() {
        try {
            File parentDir = ACCOUNTS_FILE.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to initialize account config directory: " + e.getMessage());
        }
    }

    public static void saveToFile() {
        try {
            JsonObject configObject = new JsonObject();
            JsonArray jsonArray = createAccountsJsonArray();
            configObject.add("accounts", jsonArray);

            String finalJson = gson.toJson(configObject);
            try (PrintWriter writer = new PrintWriter(new FileWriter(ACCOUNTS_FILE))) {
                writer.print(finalJson);
            }

            LOGGER.info("Saved " + accounts.size() + " accounts to separate file");

        } catch (Exception e) {
            LOGGER.error("Failed to save account config to file: " + e.getMessage(), e);
            ChatUtil.sendFormatted(String.format("%sFailed to save accounts: %s",
                    Epilogue.clientName, e.getMessage()));
        }
    }

    public static void loadFromFile() {
        accounts.clear();

        if (!ACCOUNTS_FILE.exists()) {
            LOGGER.info("Accounts file does not exist, will be created on save");
            return;
        }

        try {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(ACCOUNTS_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
            }

            if (content.length() == 0) {
                LOGGER.info("Accounts file is empty");
                return;
            }

            JsonObject object = new JsonParser().parse(content.toString()).getAsJsonObject();

            loadAccountsFromJson(object);
            LOGGER.info("Successfully loaded " + accounts.size() + " accounts from file");

        } catch (Exception e) {
            LOGGER.error("Failed to load account config from file: " + e.getMessage(), e);
        }
    }

    public static JsonObject save() {
        JsonObject emptyObject = new JsonObject();
        emptyObject.addProperty("separateFile", true);
        emptyObject.addProperty("accountsFile", "accounts.json");
        return emptyObject;
    }

    public static void load(JsonObject object) {
        loadFromFile();
    }

    private static void loadAccountsFromJson(JsonObject object) {
        if (object == null) return;

        JsonArray jsonArray = object.getAsJsonArray("accounts");
        if (jsonArray != null) {
            for (JsonElement jsonElement : jsonArray) {
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    try {
                        Account account = parseAccountFromJson(jsonObject);
                        if (account != null) {
                            accounts.add(account);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse account entry: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static Account parseAccountFromJson(JsonObject jsonObject) {
        String refreshToken = getStringProperty(jsonObject, "refreshToken", "");
        String accessToken = getStringProperty(jsonObject, "accessToken", "");
        String username = getStringProperty(jsonObject, "username", "");
        long timestamp = getLongProperty(jsonObject, "timestamp", System.currentTimeMillis());
        String uuid = getStringProperty(jsonObject, "uuid", "");

        if (username.isEmpty() && refreshToken.isEmpty() && accessToken.isEmpty()) {
            LOGGER.warn("Skipping invalid account entry (missing required fields)");
            return null;
        }

        return new Account(refreshToken, accessToken, username, timestamp, uuid);
    }

    private static JsonArray createAccountsJsonArray() {
        JsonArray jsonArray = new JsonArray();
        for (Account account : accounts) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("refreshToken", account.getRefreshToken());
            jsonObject.addProperty("accessToken", account.getAccessToken());
            jsonObject.addProperty("username", account.getUsername());
            jsonObject.addProperty("timestamp", account.getTimestamp());
            jsonObject.addProperty("uuid", account.getUUID());
            jsonArray.add(jsonObject);
        }
        return jsonArray;
    }

    private static String getStringProperty(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key)) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive()) {
                return element.getAsString();
            }
        }
        return defaultValue;
    }

    private static long getLongProperty(JsonObject obj, String key, long defaultValue) {
        if (obj.has(key)) {
            JsonElement element = obj.get(key);
            if (element.isJsonPrimitive()) {
                try {
                    return element.getAsLong();
                } catch (Exception e) {
                    try {
                        return Long.parseLong(element.getAsString());
                    } catch (Exception e2) {
                    }
                }
            }
        }
        return defaultValue;
    }

    public static Account get(int index) {
        if (index >= 0 && index < accounts.size()) {
            return accounts.get(index);
        }
        return null;
    }

    public static void add(Account account) {
        if (account != null) {
            accounts.add(account);
            saveToFile();
        }
    }

    public static void remove(int index) {
        if (index >= 0 && index < accounts.size()) {
            accounts.remove(index);
            saveToFile();
        }
    }

    public static void swap(int i, int j) {
        if (i >= 0 && i < accounts.size() && j >= 0 && j < accounts.size()) {
            Collections.swap(accounts, i, j);
            saveToFile();
        }
    }

    public static void clear() {
        accounts.clear();
        saveToFile();
    }

    public static int size() {
        return accounts.size();
    }

    public static List<Account> all() {
        return new ArrayList<>(accounts);
    }

}