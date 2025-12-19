package epilogue.module.modules.player;

import epilogue.module.Module;
import epilogue.value.values.BooleanValue;

public class NoDebuff extends Module {
    public final BooleanValue blindness = new BooleanValue("Blindness", true);
    public final BooleanValue nausea = new BooleanValue("Nausea", true);

    public NoDebuff() {
        super("NoDebuff", false);
    }
}
