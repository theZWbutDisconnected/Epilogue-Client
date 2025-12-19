package epilogue.module;

import net.minecraft.util.ResourceLocation;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.KeyEvent;
import epilogue.events.TickEvent;
import epilogue.util.SoundUtil;

import java.util.LinkedHashMap;

public class ModuleManager {
    private boolean sound = false;
    private boolean soundEnabled = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public java.util.List<Module> getModulesInCategory(ModuleCategory category) {
        return this.modules.values().stream().filter(module -> module.getCategory() == category).collect(java.util.stream.Collectors.toList());
    }

    public void playSound(boolean enabled) {
        this.sound = true;
        this.soundEnabled = enabled;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() != event.getKey()) {
                continue;
            }
            module.toggle();
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                playToggleSound(this.soundEnabled);
            }
        }
    }
    
    private void playToggleSound(boolean enabled) {
        epilogue.module.modules.render.Interface interfaceModule =
            (epilogue.module.modules.render.Interface) getModule("Interface");
        
        if (interfaceModule == null) {
            SoundUtil.playSound("random.click");
            return;
        }
        
        String mode = interfaceModule.toggleSound.getModeString();
        String soundFile = enabled ? "enable" : "disable";
        
        switch (mode) {
            case "Augustus":
                SoundUtil.playSound(new ResourceLocation("epilogue/sounds/augustus/" + soundFile + ".wav"), 1.0f);
                break;
            case "Jello":
                SoundUtil.playSound(new ResourceLocation("epilogue/sounds/jello/" + soundFile + ".wav"), 1.0f);
                break;
            case "Other":
                SoundUtil.playSound(new ResourceLocation("epilogue/sounds/other/" + soundFile + ".wav"), 1.0f);
                break;
            case "Vanilla":
            default:
                SoundUtil.playSound("random.click");
                break;
        }
    }
}