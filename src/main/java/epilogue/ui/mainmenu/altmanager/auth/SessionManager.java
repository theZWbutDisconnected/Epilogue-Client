package epilogue.ui.mainmenu.altmanager.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;

public class SessionManager {
    private static Session session;

    public static void setSession(Session newSession) {
        session = newSession;
        try {
            Field sessionField = Minecraft.class.getDeclaredField("session");
            sessionField.setAccessible(true);
            sessionField.set(Minecraft.getMinecraft(), newSession);
        } catch (Exception e) {
            try {
                Field sessionField = Minecraft.class.getDeclaredField("field_71449_j");
                sessionField.setAccessible(true);
                sessionField.set(Minecraft.getMinecraft(), newSession);
            } catch (Exception ignored) {
            }
        }
    }

    public static Session getSession() {
        if (session == null) {
            session = Minecraft.getMinecraft().getSession();
        }
        return session;
    }
}