package epilogue.ui.mainmenu.altmanager.utils;

public class Notification {
    private final String message;
    private final long expireAt;

    public Notification(String message, long durationMs) {
        this.message = message;
        this.expireAt = durationMs < 0 ? -1L : (System.currentTimeMillis() + durationMs);
    }

    public String getMessage() {
        return message;
    }

    public boolean isExpired() {
        if (expireAt < 0) return false;
        return System.currentTimeMillis() > expireAt;
    }
}