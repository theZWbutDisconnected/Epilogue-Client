package epilogue.module.modules.misc;

import epilogue.module.Module;

public class GhostHunter extends Module {
    public GhostHunter() {
        super("Ghost Hunter", false, true);
    }

    public String stripObfuscated(String string) {
        return string.replaceAll("§k", "");
    }
}
