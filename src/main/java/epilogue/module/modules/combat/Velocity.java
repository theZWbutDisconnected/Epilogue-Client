package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.mixin.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.management.RotationState;
import epilogue.module.Module;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.PercentValue;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private int rotateTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private boolean prevAuraRotationBlocked = false;
    private boolean prevAuraAttackBlocked = false;
    private boolean airRotateActive = false;
    private int advancedTimeWindowTicks = 0;
    private int delayTicksLeft = 0;
    private int airDelayTicksLeft = 0;
    private boolean delayedVelocityActive = false;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Vanilla", "JumpReset", "Mix"});
    public final PercentValue horizontal = new PercentValue("Horizontal", 100);
    public final PercentValue vertical = new PercentValue("Vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("Explosions Horizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("Explosions Vertical", 100);
    public final PercentValue chance = new PercentValue("Change", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);
    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);
    public final BooleanValue mixJumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue mixDelay = new BooleanValue("Delay", false, () -> this.mode.getValue() == 2);
    public final IntValue mixDelayTicks = new IntValue("Delay Ticks", 3, 1 , 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixRotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue());
    public final BooleanValue mixRotateOnlyInAir = new BooleanValue("Rotate Only In Air", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final BooleanValue mixAutoMove = new BooleanValue("Auto Move", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final IntValue mixRotateTicks = new IntValue("Rotate Ticks", 3, 1, 20, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final BooleanValue mixAirDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 2);
    public final IntValue mixAirDelayTicks = new IntValue("Air Delay Ticks", 1, 1, 20, () -> this.mode.getValue() == 2 && this.mixAirDelay.getValue());
    public final BooleanValue mixAdvancedTimeDelay = new BooleanValue("Advanced Time Delay", false, () -> this.mode.getValue() == 2);

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer != null && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb());
    }

    private boolean isMix() {
        return this.mode.getValue() == 2;
    }

    private void startRotate(double knockbackX, double knockbackZ) {
        this.endRotate();
        this.knockbackX = knockbackX;
        this.knockbackZ = knockbackZ;
        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
            this.rotateTickCounter = 1;
            this.targetRotation = null;
            this.airRotateActive = true;
            this.prevAuraRotationBlocked = Aura.rotationBlocked;
            this.prevAuraAttackBlocked = Aura.attackBlocked;
            Aura.rotationBlocked = true;
        }
    }

    private boolean isAuraTargetHitByRay() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura == null || !aura.isEnabled()) {
            return true;
        }
        if (aura.target == null || aura.target.getBox() == null) {
            return true;
        }
        return epilogue.util.RotationUtil.rayTrace(aura.target.getBox(), mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, aura.attackRange.getValue()) != null;
    }

    private void updateRotateGates() {
        if (!this.airRotateActive || this.rotateTickCounter <= 0 || this.rotateTickCounter > this.mixRotateTicks.getValue()) {
            return;
        }
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura != null && aura.isEnabled()) {
            Aura.rotationBlocked = true;
            Aura.attackBlocked = !this.isAuraTargetHitByRay();
        } else {
            Aura.rotationBlocked = this.prevAuraRotationBlocked;
            Aura.attackBlocked = this.prevAuraAttackBlocked;
        }
    }

    private void endRotate() {
        if (!this.airRotateActive && this.rotateTickCounter <= 0) {
            return;
        }
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
        Aura.rotationBlocked = this.prevAuraRotationBlocked;
        Aura.attackBlocked = this.prevAuraAttackBlocked;
    }

    private void startDelayedVelocity(int ticks) {
        this.delayedVelocityActive = true;
        this.delayTicksLeft = Math.max(1, ticks);
    }

    private void startAirDelayedVelocity(int ticks) {
        this.delayedVelocityActive = true;
        this.airDelayTicksLeft = Math.max(1, ticks);
    }

    private int getAdvancedTimeDelayTicks() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return 0;
        }
        double footY = mc.thePlayer.getEntityBoundingBox().minY;
        int x = MathHelper.floor_double(mc.thePlayer.posX);
        int z = MathHelper.floor_double(mc.thePlayer.posZ);
        int y = MathHelper.floor_double(footY);

        int groundY = y;
        while (groundY > 0 && mc.theWorld.isAirBlock(new net.minecraft.util.BlockPos(x, groundY - 1, z))) {
            groundY--;
        }

        double lv = Math.max(0.0, footY - (double) groundY);
        if (lv < 0.5) {
            return 1;
        }
        if (lv < 1.0) {
            return 3;
        }
        if (lv < 3.0) {
            return 8;
        }
        return 0;
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.endRotate();
            return;
        }

        if (!this.allowNext || !this.fakeCheck.getValue()) {
            this.allowNext = true;
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                this.handleExplosion(event);
            } else {
                this.chanceCounter = this.chanceCounter % 100 + this.chance.getValue();
                if (this.chanceCounter >= 100) {
                    boolean doJumpReset = (this.mode.getValue() == 1) || (this.isMix() && this.mixJumpReset.getValue());
                    boolean canDoJumpReset = doJumpReset && event.getY() > 0.0;

                    if (this.isMix() && this.mixJumpReset.getValue() && this.mixRotate.getValue() && canDoJumpReset) {
                        if (!this.mixRotateOnlyInAir.getValue() || !mc.thePlayer.onGround) {
                            this.startRotate(event.getX(), event.getZ());
                        }
                    }

                    this.jumpFlag = canDoJumpReset;
                    this.applyVanilla(event);
                    this.chanceCounter = 0;
                }
            }
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        if (this.horizontal.getValue() > 0) {
            event.setX(event.getX() * this.horizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.horizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.vertical.getValue() > 0) {
            event.setY(event.getY() * this.vertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    private void handleExplosion(KnockbackEvent event) {
        if (this.explosionHorizontal.getValue() > 0) {
            event.setX(event.getX() * this.explosionHorizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.explosionHorizontal.getValue() / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (this.explosionVertical.getValue() > 0) {
            event.setY(event.getY() * this.explosionVertical.getValue() / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();
            if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }

            if (this.isMix()) {
                if (this.mixAdvancedTimeDelay.getValue() && !mc.thePlayer.onGround) {
                    int ticks = this.getAdvancedTimeDelayTicks();
                    if (ticks > 0) {
                        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                        event.setCancelled(true);
                        this.startDelayedVelocity(ticks);
                        return;
                    }
                }

                if (this.mixAirDelay.getValue() && !mc.thePlayer.onGround) {
                    Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                    Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                    event.setCancelled(true);
                    this.startAirDelayedVelocity(this.mixAirDelayTicks.getValue());
                    return;
                }

                if (this.mixDelay.getValue()) {
                    Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                    Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                    event.setCancelled(true);
                    this.startDelayedVelocity(this.mixDelayTicks.getValue());
                    return;
                }
                return;
            }

            if (this.mode.getValue() == 1 && this.airDelay.getValue() && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer((Packet<INetHandlerPlayClient>) (Packet<?>) packet);
                event.setCancelled(true);
                return;
            }
            return;
        }

        if (event.getPacket() instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus packet = (S19PacketEntityStatus) event.getPacket();
            World world = mc.theWorld;
            if (world != null) {
                Entity entity = packet.getEntity(world);
                if (entity != null && entity.equals(mc.thePlayer) && packet.getOpCode() == 2) {
                    this.allowNext = false;
                }
            }
            return;
        }

        if (event.getPacket() instanceof S27PacketExplosion) {
            S27PacketExplosion packet = (S27PacketExplosion) event.getPacket();
            if (packet.func_149149_c() != 0.0F || packet.func_149144_d() != 0.0F || packet.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null && mc.thePlayer.onGround) {
                mc.thePlayer.movementInput.jump = true;
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (!this.isMix()) {
            return;
        }

        if (event.getType() == EventType.PRE) {
            if (this.advancedTimeWindowTicks > 0) {
                this.advancedTimeWindowTicks--;
            }
            if (mc.thePlayer.onGround && mc.gameSettings != null && mc.gameSettings.keyBindJump != null && mc.gameSettings.keyBindJump.isKeyDown()) {
                this.advancedTimeWindowTicks = 12;
            }
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.rotateTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = epilogue.util.RotationUtil.getRotationsTo(deltaX, 0.0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
            return;
        }

        if (event.getType() == EventType.POST) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                this.updateRotateGates();
                this.rotateTickCounter++;
                if (this.rotateTickCounter > maxTick) {
                    this.endRotate();
                }
            }

            if (this.delayedVelocityActive) {
                if (this.airDelayTicksLeft > 0) {
                    this.airDelayTicksLeft--;
                    if (this.airDelayTicksLeft <= 0) {
                        Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                        this.delayedVelocityActive = false;
                    }
                } else if (this.delayTicksLeft > 0) {
                    this.delayTicksLeft--;
                    if (this.delayTicksLeft <= 0) {
                        Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                        this.delayedVelocityActive = false;
                    }
                } else {
                    this.delayedVelocityActive = false;
                }
            }
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null || !this.isMix()) {
            return;
        }
        int maxTick = this.mixRotateTicks.getValue();
        if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
            if (this.mixAutoMove.getValue()) {
                mc.thePlayer.movementInput.moveForward = 1.0F;
            }
            if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && epilogue.util.MoveUtil.isForwardPressed()) {
                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                if (aura != null && aura.isEnabled() && aura.moveFix.getValue() == 2 && aura.rotations.getValue() != 3) {
                    epilogue.util.MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
        this.advancedTimeWindowTicks = 0;
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.endRotate();
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.airRotateActive = false;
        this.advancedTimeWindowTicks = 0;
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.endRotate();

        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}