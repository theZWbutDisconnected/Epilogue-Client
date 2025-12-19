package epilogue.module.modules.render;

import epilogue.module.Module;
import epilogue.value.values.PercentValue;

public class NoHurtCam extends Module {
    public final PercentValue multiplier = new PercentValue("Multiplier", 0);

    public NoHurtCam() {
        super("NoHurtCam", false, true);
    }
}
