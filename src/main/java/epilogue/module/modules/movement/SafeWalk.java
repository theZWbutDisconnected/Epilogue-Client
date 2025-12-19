package epilogue.module.modules.movement;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.SafeWalkEvent;
import epilogue.events.UpdateEvent;
import epilogue.module.Module;
import epilogue.module.modules.player.Scaffold;
import epilogue.util.ItemUtil;
import epilogue.util.MoveUtil;
import epilogue.util.PlayerUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import net.minecraft.client.Minecraft;

public class SafeWalk extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatValue motion = new FloatValue("Motion", 1.0F, 0.5F, 1.0F);
    public final FloatValue speedMotion = new FloatValue("Speed Motion", 1.0F, 0.5F, 1.5F);
    public final BooleanValue air = new BooleanValue("In Air", false);
    public final BooleanValue directionCheck = new BooleanValue("Check Direction", true);
    public final BooleanValue pitCheck = new BooleanValue("Check Pitch", true);
    public final BooleanValue requirePress = new BooleanValue("Only Shift Hold", false);
    public final BooleanValue blocksOnly = new BooleanValue("Only Blocks", true);

    private boolean canSafeWalk() {
        Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
        if (scaffold.isEnabled()) {
            return false;
        } else if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if (this.blocksOnly.getValue() && !ItemUtil.isHoldingBlock()) {
            return false;
        } else {
            return (!this.requirePress.getValue() || mc.gameSettings.keyBindUseItem.isKeyDown()) && (mc.thePlayer.onGround && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -1.0)
                    || this.air.getValue() && PlayerUtil.canMove(mc.thePlayer.motionX, mc.thePlayer.motionZ, -2.0));
        }
    }

    public SafeWalk() {
        super("SafeWalk", false);
    }

    @EventTarget
    public void onMove(SafeWalkEvent event) {
        if (this.isEnabled()) {
            if (this.canSafeWalk()) {
                event.setSafeWalk(true);
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer.onGround && MoveUtil.isForwardPressed() && this.canSafeWalk()) {
                if (MoveUtil.getSpeedLevel() <= 0) {
                    if (this.motion.getValue() != 1.0F) {
                        MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.motion.getValue());
                    }
                } else if (this.speedMotion.getValue() != 1.0F) {
                    MoveUtil.setSpeed(MoveUtil.getSpeed() * (double) this.speedMotion.getValue());
                }
            }
        }
    }
}
