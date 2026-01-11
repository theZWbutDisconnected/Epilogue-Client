package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import epilogue.enums.DelayModules;
import epilogue.util.MoveUtil;
import epilogue.util.RayCastUtil;
import epilogue.util.RotationUtil;
import epilogue.value.values.*;
import epiloguemixinbridge.IAccessorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.world.World;
import epilogue.util.ChatUtil;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.management.RotationState;
import epilogue.module.Module;
import net.minecraft.util.AxisAlignedBB;
import java.util.Random;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.text.DecimalFormat;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Random random = new Random();
    private static final DecimalFormat df = new DecimalFormat("0.000000000000");

    private int chanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean jumpFlag = false;
    private int rotateTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private int delayTicksLeft = 0;
    private int airDelayTicksLeft = 0;
    private boolean delayedVelocityActive = false;

    private boolean reduceActive = false;
    private int reduceVelocityTicks = 0;
    private int reduceOffGroundTicks = 0;
    private int reduceTicksSinceTeleport = 0;
    private boolean reduceReceiving = false;

    private boolean jump;
    public static boolean active = false;
    public static boolean receiving = false;
    private final Deque<Packet<?>> delayedPackets = new ConcurrentLinkedDeque<>();
    public int ticksSinceTeleport = 0;
    public int ticksSinceVelocity = 0;
    public int offGroundTicks = 0;

    private static boolean slot;
    private static boolean attack;
    private static boolean swing;
    private static boolean block;
    private static boolean inventory;
    private static boolean dig;

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Vanilla", "JumpReset", "Reduce", "Watchdog"});

    public final PercentValue horizontal = new PercentValue("Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue vertical = new PercentValue("Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionHorizontal = new PercentValue("Explosion Horizontal", 100, () -> this.mode.getValue() == 0);
    public final PercentValue explosionVertical = new PercentValue("Explosion Vertical", 100, () -> this.mode.getValue() == 0);
    public final PercentValue chance = new PercentValue("Chance", 100);
    public final BooleanValue fakeCheck = new BooleanValue("Check Fake", true);

    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);
    public final IntValue airDelayTicks = new IntValue("Air Delay Ticks", 3, 1, 20, () -> this.mode.getValue() == 1 && this.airDelay.getValue());

    public final BooleanValue mixDelay = new BooleanValue("Delay", true, () -> this.mode.getValue() == 2);
    public final IntValue mixDelayTicks = new IntValue("Delay Ticks", 1, 1, 20, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixDelayOnlyInGround = new BooleanValue("Delay Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixDelay.getValue());
    public final BooleanValue mixReduce = new BooleanValue("Reduce", false, () -> this.mode.getValue() == 2);
    public final BooleanValue mixJumpReset = new BooleanValue("Jump Reset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue mixRotate = new BooleanValue("Rotate", false, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue());
    public final BooleanValue mixRotateOnlyInGround = new BooleanValue("Rotate Only In Ground", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final BooleanValue mixAutoMove = new BooleanValue("Auto Move", true, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());
    public final IntValue mixRotateTicks = new IntValue("Rotate Ticks", 3, 1, 20, () -> this.mode.getValue() == 2 && this.mixJumpReset.getValue() && this.mixRotate.getValue());

    public final PercentValue watchdogChance = new PercentValue("Chance", 100, () -> this.mode.getValue() == 3);
    public final BooleanValue watchdogLegitTiming = new BooleanValue("Legit Timing", false, () -> this.mode.getValue() == 3);
    public final BooleanValue onSwing = new BooleanValue("On Swing", true, () -> this.mode.getValue() == 3);
    public final BooleanValue watchdogJumpReset = new BooleanValue("Jumpreset", true, () -> this.mode.getValue() == 3);

    public Velocity() {
        super("Velocity", false);
    }

    private void releaseDelayedPackets() {
        if (delayedPackets.isEmpty() || receiving) return;

        receiving = true;
        active = false;

        if (mc.getNetHandler() == null || delayedPackets.isEmpty()) return;

        for (Packet packet : delayedPackets) {
            try {
                packet.processPacket(mc.getNetHandler());
            } catch (Exception e) {
                try {
                    if (packet instanceof S12PacketEntityVelocity) {
                        packet.processPacket(mc.getNetHandler());
                    } else if (packet instanceof S08PacketPlayerPosLook) {
                        packet.processPacket(mc.getNetHandler());
                    } else if (packet instanceof S19PacketEntityStatus) {
                        packet.processPacket(mc.getNetHandler());
                    } else if (packet instanceof S32PacketConfirmTransaction) {
                        packet.processPacket(mc.getNetHandler());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        delayedPackets.clear();
        receiving = false;
    }

    @EventTarget
    public void onPreMotion(PreMotionEvent event) {
        if (!this.isEnabled() || !this.isWatchdog()) return;
        this.jump = false;
    }

    @EventTarget
    public void onMove(MoveEvent event) {
        if (!this.isEnabled() || !this.isWatchdog()) return;
        if (this.jump) {
            event.setJump(true);
        }
    }

    @EventTarget
    public void onPacketReceiveEvent(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled()) return;

        if (this.onSwing.getValue() && !mc.thePlayer.isSwingInProgress) {
            return;
        }

        Packet<?> packet = event.getPacket();
        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId() && vel.getMotionY() > 0) {
                if (this.watchdogJumpReset.getValue()) {
                    this.jump = true;
                }
            }
        }
    }

    @EventTarget
    public void onPacketSend(PacketEvent event) {
        if (event.getType() != EventType.SEND || !this.isEnabled()) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof net.minecraft.network.play.client.C09PacketHeldItemChange) {
            slot = true;
        } else if (packet instanceof C0APacketAnimation) {
            swing = true;
        } else if (packet instanceof net.minecraft.network.play.client.C02PacketUseEntity &&
                ((net.minecraft.network.play.client.C02PacketUseEntity)packet).getAction() == net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK) {
            attack = true;
        } else if (packet instanceof net.minecraft.network.play.client.C08PacketPlayerBlockPlacement) {
            block = true;
        } else if (packet instanceof net.minecraft.network.play.client.C07PacketPlayerDigging) {
            block = true;
            dig = true;
        } else if (packet instanceof net.minecraft.network.play.client.C0DPacketCloseWindow ||
                packet instanceof net.minecraft.network.play.client.C16PacketClientStatus &&
                        ((net.minecraft.network.play.client.C16PacketClientStatus)packet).getStatus() == net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT ||
                packet instanceof net.minecraft.network.play.client.C0EPacketClickWindow) {
            inventory = true;
        } else if (packet instanceof net.minecraft.network.play.client.C03PacketPlayer) {
            resetBadPackets();
        }
    }

    private boolean badPackets(boolean checkSlot, boolean checkAttack, boolean checkSwing,
                               boolean checkBlock, boolean checkInventory, boolean checkDig) {
        return (slot && checkSlot) ||
                (attack && checkAttack) ||
                (swing && checkSwing) ||
                (block && checkBlock) ||
                (inventory && checkInventory) ||
                (dig && checkDig);
    }

    private void resetBadPackets() {
        slot = false;
        swing = false;
        attack = false;
        block = false;
        inventory = false;
        dig = false;
    }

    private boolean isAuraBlocking() {
            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            if (aura.isEnabled()) {
                int autoBlockValue = aura.autoBlock.getValue();
                return (autoBlockValue == 3 || autoBlockValue == 4 || autoBlockValue == 5) && aura.isPlayerBlocking();
            }
            return false;
    }

    private EntityPlayer getNearestPlayerTarget() {
        if (mc.theWorld == null || mc.thePlayer == null) return null;
        EntityPlayer best = null;
        double bestDist = Double.MAX_VALUE;
        for (EntityPlayer o : mc.theWorld.playerEntities) {
            if (!(o instanceof EntityPlayer)) continue;
            if (o == mc.thePlayer || o.isDead) continue;
            double d = mc.thePlayer.getDistanceToEntity(o);
            if (d < bestDist) {
                bestDist = d;
                best = o;
            }
        }
        return best;
    }

    private Entity getNearTarget() {
            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            if (aura != null && aura.isEnabled()) {
                Entity target = aura.getTarget();
                if (target != null) {
                    return target;
                }
            }
        return getNearestPlayerTarget();
    }

    private boolean isInWeb(Entity entity) {
        if (entity == null) return false;
        try {
            return ((IAccessorEntity) entity).getIsInWeb();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTargetInRaycastRange(Entity entity) {
        if (entity == null || mc.thePlayer == null) return false;
        AxisAlignedBB bb = entity.getEntityBoundingBox();
        if (bb == null) return false;
        return RotationUtil.rayTrace(bb, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, 3.0) != null;
    }

    private boolean watchdogIsTargetInRaycastRange(Entity target) {
        if (target == null || mc.thePlayer == null) {
            return false;
        }
        return RayCastUtil.inView(target);
    }

    private boolean isInBadPosition() {
        if (mc.thePlayer == null) return false;
        return isInWeb(mc.thePlayer) || mc.thePlayer.isOnLadder() || mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    private boolean isVanilla() { return this.mode.getValue() == 0; }
    private boolean isJumpReset() { return this.mode.getValue() == 1; }
    private boolean isReduce() { return this.mode.getValue() == 2; }
    private boolean isWatchdog() { return this.mode.getValue() == 3; }

    private void startRotate(double knockbackX, double knockbackZ) {
        endRotate();
        this.knockbackX = knockbackX;
        this.knockbackZ = knockbackZ;
        if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
            this.rotateTickCounter = 1;
            this.targetRotation = null;
        }
    }

    private void endRotate() {
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
    }

    private void queueDelayedVelocity(PacketEvent event, S12PacketEntityVelocity packet, int ticks) {
        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
        Epilogue.delayManager.delayedPacket.offer(packet);
        event.setCancelled(true);
        this.delayTicksLeft = Math.max(1, ticks);
        this.delayedVelocityActive = true;
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
                    boolean doJumpReset = (this.mode.getValue() == 1) || (this.isReduce() && this.mixJumpReset.getValue());
                    boolean canDoJumpReset = doJumpReset && event.getY() > 0.0;

                    if (this.isReduce() && this.mixJumpReset.getValue() && this.mixRotate.getValue() && canDoJumpReset) {
                        boolean shouldRotate;
                        if (this.mixRotateOnlyInGround.getValue() && mc.thePlayer.onGround) {
                            shouldRotate = true;
                        } else shouldRotate = !this.mixRotateOnlyInGround.getValue();
                        if (shouldRotate) {
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
        if (!this.isEnabled() || mc.thePlayer == null) return;
        if (event.getType() != EventType.RECEIVE || event.isCancelled()) return;

        Packet<?> packet = event.getPacket();

        if (this.isWatchdog() && packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId()) {

                Entity target = this.getNearTarget();
                double distance = target != null ? mc.thePlayer.getDistanceToEntity(target) : 100.0;

                if (receiving ||
                        ticksSinceTeleport < 3 ||
                        isInWeb(mc.thePlayer) ||
                        mc.thePlayer.isSwingInProgress ||
                        isAuraBlocking() ||
                        (target != null && distance <= 3.2)) {
                    return;
                }

                if (!mc.thePlayer.onGround) {
                    delayedPackets.offer(vel);
                    active = true;
                    event.setCancelled(true);

                    ticksSinceVelocity = 0;
                }
            }
        }

        if (this.isWatchdog() && packet instanceof S08PacketPlayerPosLook) {
            S08PacketPlayerPosLook posPacket = (S08PacketPlayerPosLook) packet;
            if (active) {
                delayedPackets.offer(posPacket);
                event.setCancelled(true);
            }
        }

        if (this.isWatchdog() && active && packet instanceof S32PacketConfirmTransaction) {
            delayedPackets.offer(packet);
            event.setCancelled(true);
            return;
        }

        if (this.isWatchdog() && packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus status = (S19PacketEntityStatus) packet;
            if (status.getEntity(mc.theWorld) == mc.thePlayer && status.getOpCode() == 2) {
                ticksSinceVelocity = 0;
            }
        }

        if (this.isReduce() && this.mixReduce.getValue() && packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() == mc.thePlayer.getEntityId()) {
                reduceReceiving = true;

                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                if (aura != null && aura.isEnabled()) {
                    aura.getTarget();
                }

                boolean inBadPos = isInBadPosition();
                boolean isBlocking = isAuraBlocking();

                boolean reduceCondition = reduceTicksSinceTeleport >= 3 && !inBadPos && !isBlocking;

                if (reduceCondition) {
                    reduceActive = true;
                    reduceVelocityTicks = 0;
                }
            }
        }

        if (this.isReduce() && this.mixReduce.getValue() && reduceActive && packet instanceof S32PacketConfirmTransaction) {
            event.setCancelled(true);
            return;
        }

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;
            if (vel.getEntityID() != mc.thePlayer.getEntityId()) return;

            if (this.isReduce()) {
                if (this.mixDelay.getValue()) {
                    if (!this.mixDelayOnlyInGround.getValue() || mc.thePlayer.onGround) {
                        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        Epilogue.delayManager.delayedPacket.offer(vel);
                        event.setCancelled(true);
                        this.delayTicksLeft = this.mixDelayTicks.getValue();
                        this.delayedVelocityActive = true;
                        return;
                    }
                }
                return;
            }

            if (this.isJumpReset() && this.airDelay.getValue() && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer(vel);
                event.setCancelled(true);
                this.airDelayTicksLeft = this.airDelayTicks.getValue();
                this.delayedVelocityActive = true;
                return;
            }
        }

        if (packet instanceof S19PacketEntityStatus) {
            S19PacketEntityStatus p = (S19PacketEntityStatus) packet;
            World world = mc.theWorld;
            if (world != null) {
                Entity entity = p.getEntity(world);
                if (entity != null && entity.equals(mc.thePlayer) && p.getOpCode() == 2) {
                    this.allowNext = false;
                }
            }
        }

        if (packet instanceof S27PacketExplosion) {
            S27PacketExplosion p = (S27PacketExplosion) packet;
            if (p.func_149149_c() != 0.0F || p.func_149144_d() != 0.0F || p.func_149147_e() != 0.0F) {
                this.pendingExplosion = true;
                if (this.explosionHorizontal.getValue() == 0 || this.explosionVertical.getValue() == 0) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) throws InterruptedException {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        ticksSinceTeleport++;
        ticksSinceVelocity++;

        if (!mc.thePlayer.onGround) {
            offGroundTicks++;
        } else {
            offGroundTicks = 0;
        }

        if (this.isWatchdog()) {
            Entity target = this.getNearTarget();
            double distance = target != null ? mc.thePlayer.getDistanceToEntity(target) : 100.0;

            if (this.watchdogIsTargetInRaycastRange(target) &&
                    mc.thePlayer.isSwingInProgress &&
                    ticksSinceVelocity < 3) {

                Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);

                if (aura != null && aura.isEnabled() &&
                        !badPackets(true, true, true, false, true, false) &&
                        ticksSinceTeleport > 3
//                        && !aura.isPlayerBlocking()
                ) {

                    // 移除Attack并改为0.6 reduce
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                    mc.thePlayer.setSprinting(false);

//                    if (mc.getNetHandler() != null) {
//                        mc.getNetHandler().addToSendQueue(
//                                new net.minecraft.network.play.client.C0APacketAnimation()
//                        );
//                    }
//
//                    if (mc.playerController != null) {
//                        mc.playerController.attackEntity(mc.thePlayer, target);
//                    }
                    String motionXStr = df.format(mc.thePlayer.motionX);
                    String motionZStr = df.format(mc.thePlayer.motionZ);
                    ChatUtil.sendRaw("§bBillionaire §fMotion X: " + motionXStr + " | Motion Z: " + motionZStr);
                }
            }

            if (mc.thePlayer.onGround && active) {
                releaseDelayedPackets();
            }
            if (ticksSinceTeleport < 3 && active) {
                releaseDelayedPackets();
            }
            if (mc.thePlayer.isSwingInProgress && active) {
                releaseDelayedPackets();
            }
            if (target != null && distance <= 3.2 && active) {
                if (this.watchdogIsTargetInRaycastRange(target)) {
                    releaseDelayedPackets();
                }
            }
            if (offGroundTicks > 20 && active) {
                releaseDelayedPackets();
            }

            if (this.watchdogLegitTiming.getValue() && active) {
                Thread.sleep(1 + random.nextInt(3));
            }
        }

        if (this.isReduce() && this.mixReduce.getValue()) {
            reduceReceiving = false;
            reduceTicksSinceTeleport++;
        }

        if (this.isReduce() && this.mixReduce.getValue() && event.getType() == EventType.PRE) {
            if (!mc.thePlayer.onGround) {
                reduceOffGroundTicks++;
            } else {
                reduceOffGroundTicks = 0;
            }

            if (reduceActive) {
                reduceVelocityTicks++;
            }

            Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
            Entity target;
            if (aura != null && aura.isEnabled() && aura.getTarget() != null) {
                target = aura.getTarget();
            } else {
                target = getNearestPlayerTarget();
            }

            if (target != null &&
                    mc.thePlayer.isSwingInProgress &&
                    reduceVelocityTicks < 3 &&
                    !mc.thePlayer.onGround) {

                boolean inBadPos = isInBadPosition();
                boolean inRaycast = isTargetInRaycastRange(target);

                boolean canReduce = !inBadPos && aura != null && aura.isEnabled() && inRaycast && reduceTicksSinceTeleport >= 3;

                if (canReduce) {
                    int ab = aura.autoBlock.getValue();
                    if (ab == 3 || ab == 4) {
                        boolean isBlocking = aura.isPlayerBlocking();
                        canReduce = !isBlocking;
                    }
                }

                if (canReduce) {
                    mc.thePlayer.swingItem();
                    mc.playerController.attackEntity(mc.thePlayer, target);
                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                    mc.thePlayer.setSprinting(false);
                }
            }

            boolean shouldReset = mc.thePlayer.onGround ||
                    mc.thePlayer.isSwingInProgress ||
                    (target != null && mc.thePlayer.getDistanceToEntity(target) <= 3.2F) ||
                    reduceOffGroundTicks > 20 ||
                    reduceTicksSinceTeleport < 3;

            if (shouldReset && reduceActive) {
                reduceActive = false;
            }
        }

        if (this.isReduce() && event.getType() == EventType.PRE) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.rotateTickCounter == 1) {
                    double deltaX = -this.knockbackX;
                    double deltaZ = -this.knockbackZ;
                    this.targetRotation = RotationUtil.getRotationsTo(deltaX, 0.0, deltaZ, event.getYaw(), event.getPitch());
                }
                if (this.targetRotation != null) {
                    event.setRotation(this.targetRotation[0], this.targetRotation[1], 2);
                    event.setPervRotation(this.targetRotation[0], 2);
                }
            }
        }

        if (event.getType() == EventType.POST) {
            if (this.isReduce()) {
                int maxTick = this.mixRotateTicks.getValue();
                if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
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
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        if (this.isReduce()) {
            int maxTick = this.mixRotateTicks.getValue();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                if (this.mixAutoMove.getValue()) {
                    mc.thePlayer.movementInput.moveForward = 1.0F;
                }
                if (this.targetRotation != null && RotationState.isActived() && RotationState.getPriority() == 2.0F && MoveUtil.isForwardPressed()) {
                    Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
                    if (aura != null && aura.isEnabled() && aura.moveFix.getValue() == 2 && aura.rotations.getValue() != 3) {
                        MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
                    }
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.isWatchdog() && this.jump && this.watchdogJumpReset.getValue()) {
            if (mc.thePlayer != null && mc.thePlayer.onGround) {
                mc.thePlayer.movementInput.jump = true;
            }
            this.jump = false;
        } else if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null && mc.thePlayer.onGround) {
                mc.thePlayer.movementInput.jump = true;
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
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.endRotate();

        reduceActive = false;
        reduceVelocityTicks = 0;
        reduceOffGroundTicks = 0;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;

        this.jump = false;
        active = false;
        receiving = false;
        delayedPackets.clear();
        ticksSinceTeleport = 0;
        ticksSinceVelocity = 0;
        offGroundTicks = 0;

        resetBadPackets();
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
        this.delayTicksLeft = 0;
        this.airDelayTicksLeft = 0;
        this.delayedVelocityActive = false;
        this.endRotate();

        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();

        reduceActive = false;
        reduceTicksSinceTeleport = 0;
        reduceReceiving = false;

        if (!delayedPackets.isEmpty()) {
            releaseDelayedPackets();
        }
        active = false;
        receiving = false;
        this.jump = false;

        resetBadPackets();
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (this.isWatchdog()) {
            String chanceStr = this.watchdogChance.getValue() + "%";
            String jumpResetStr = this.watchdogJumpReset.getValue() ? "Jumpreset" : "NoJR";
            if (this.watchdogLegitTiming.getValue()) {
                return new String[]{"Reduce", chanceStr, jumpResetStr, "Legit"};
            }
            return new String[]{"Reduce", chanceStr, jumpResetStr};
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}