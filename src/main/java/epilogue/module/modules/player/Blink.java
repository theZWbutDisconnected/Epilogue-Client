package epilogue.module.modules.player;

import epilogue.Epilogue;
import epilogue.enums.BlinkModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.event.types.Priority;
import epilogue.events.LoadWorldEvent;
import epilogue.events.TickEvent;
import epilogue.module.Module;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;

public class Blink extends Module {
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Default", "Pulse"});
    public final IntValue ticks = new IntValue("Ticks", 20, 0, 1200);

    public Blink() {
        super("Blink", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            if (!Epilogue.blinkManager.getBlinkingModule().equals(BlinkModules.BLINK)) {
                this.setEnabled(false);
            } else {
                if (this.ticks.getValue() > 0 && Epilogue.blinkManager.countMovement() > (long) this.ticks.getValue()) {
                    switch (this.mode.getValue()) {
                        case 0:
                            this.setEnabled(false);
                            break;
                        case 1:
                            Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                            Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    }
                }
            }
        }
    }

    @EventTarget
    public void onWorldLoad(LoadWorldEvent event) {
        this.setEnabled(false);
    }

    @Override
    public void onEnabled() {
        Epilogue.blinkManager.setBlinkState(false, Epilogue.blinkManager.getBlinkingModule());
        Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
    }

    @Override
    public void onDisabled() {
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }
}