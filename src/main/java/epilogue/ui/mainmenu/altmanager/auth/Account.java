package epilogue.ui.mainmenu.altmanager.auth;

public class Account {
    private String refreshToken;
    private String accessToken;
    private String username;
    private long timestamp;
    private String uuid;

    public Account(String refreshToken, String accessToken, String username, long timestamp, String uuid) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.username = username;
        this.timestamp = timestamp;
        this.uuid = uuid;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }
}