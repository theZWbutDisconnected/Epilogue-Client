package epilogue.management;

import epilogue.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class TargetManager extends PlayerFileManager {
    public TargetManager() {
        super(new File("./Epilogue/", "enemies.txt"), new Color(ChatColors.DARK_RED.toAwtColor()));
    }
}
