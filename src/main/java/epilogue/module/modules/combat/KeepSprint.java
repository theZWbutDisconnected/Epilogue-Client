package epilogue.module.modules.combat;

import epilogue.module.Module;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.PercentValue;
import net.minecraft.client.Minecraft;

public class KeepSprint extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final PercentValue slowdown = new PercentValue("Slow", 0);
    public final BooleanValue groundOnly = new BooleanValue("Only Ground", false);
    public final BooleanValue reachOnly = new BooleanValue("Only In Reach", false);

    public KeepSprint() {
        super("KeepSprint", false);
    }

    public boolean shouldKeepSprint() {
        if (this.groundOnly.getValue() && !mc.thePlayer.onGround) {
            return false;
        } else {
            return !this.reachOnly.getValue() || mc.objectMouseOver.hitVec.distanceTo(mc.getRenderViewEntity().getPositionEyes(1.0F)) > 3.0;
        }
    }
}
