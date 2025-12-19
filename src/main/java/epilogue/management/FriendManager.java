package epilogue.management;

import epilogue.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class FriendManager extends PlayerFileManager {
    public FriendManager() {
        super(new File("./Epilogue/", "friends.txt"), new Color(ChatColors.DARK_GREEN.toAwtColor()));
    }
}
