package epilogue.module.modules.combat;

import epilogue.Epilogue;
import epilogue.event.EventManager;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.mixin.IAccessorEntity;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import epilogue.util.RotationUtil;
import epilogue.value.values.*;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;

public class Velocity2 extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private boolean pendingExplosion = false;
    private int rotateTickCounter = 0;
    private float[] targetRotation = null;
    private double knockbackX = 0.0;
    private double knockbackZ = 0.0;
    private boolean jumpFlag = false;
    private int attackCounter = 0;
    private int attackReduceTicksLeft = 0;
    private boolean attackReduce3Active = false;
    private int attackReduce3TicksLeft = 0;
    public static boolean hasReceivedVelocity = false;
    private boolean validVelocityReceived = false;
    private int hitsCount = 0;
    private boolean autoReduceActive = false;
    private float reduceYaw = Float.NaN;
    private float reducePitch = Float.NaN;
    private int reduceRotateTicksLeft = 0;
    private int lastHurtTime = 0;

    public final ModeValue mode = new ModeValue("mode", 0, new String[]{"Hypixel"});

    public final BooleanValue rotate = new BooleanValue("Rotate", false);
    public final IntValue rotateTick = new IntValue("RotateTick", 2, 1, 12, () -> this.rotate.getValue());
    public final BooleanValue autoMove = new BooleanValue("AutoMove", false, () -> this.rotate.getValue());

    public final BooleanValue airRotate = new BooleanValue("AirRotate", false, () -> this.rotate.getValue());
    public final IntValue airRotateTicks = new IntValue(
            "AirRotateTicks",
            2,
            1,
            10,
            () -> this.rotate.getValue() && this.airRotate.getValue()
    );
    public final BooleanValue jumpReset = new BooleanValue("JumpReset", false, () -> this.mode.getValue() == 0);

    public final BooleanValue attackReduce3 = new BooleanValue("AttackReduce3", false);
    public final BooleanValue sprintReset3 = new BooleanValue("SprintReset3", true, () -> this.attackReduce3.getValue());
    public final FloatValue reduce3Factor = new FloatValue("Reduce3-Factor", 0.6F, 0.1F, 1.0F, () -> this.attackReduce3.getValue());

    public final BooleanValue autoReduce = new BooleanValue("AutoReduce", false);
    public final BooleanValue reduceDebug = new BooleanValue("Reduce Debug", false, () -> this.autoReduce.getValue());

    public final BooleanValue reduceMode = new BooleanValue("Reduce", false);
    public final ModeValue reduceType = new ModeValue("Reduce-Type", 0, new String[]{"Attackreduce", "Reduce2"}, () -> this.reduceMode.getValue());
    public final FloatValue reduceFactor = new FloatValue("Reduce-Factor", 0.6F, 0.1F, 1.0F, () -> this.reduceMode.getValue());
    public final IntValue attackTick = new IntValue(
            "AttackTick",
            4,
            1,
            10,
            () -> this.reduceMode.getValue() && this.reduceType.getValue() == 0
    );
    public final BooleanValue sprintReset = new BooleanValue("SprintReset", true, () -> this.reduceMode.getValue());
    public final PercentValue horizontal = new PercentValue("horizontal", 100);
    public final PercentValue vertical = new PercentValue("vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("explosions-horizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("explosions-vertical", 100);
    public final BooleanValue showLog = new BooleanValue("LOG", false);

    public Velocity2() {
        super("Velocity2", false);
    }

    private boolean isInLiquidOrWeb() {
        if (mc.thePlayer == null) {
            return false;
        }
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb();
    }

    private Entity getAuraTarget() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura != null && aura.isEnabled()) {
            return aura.getTarget();
        }
        return null;
    }

    private void computeReduceRotations(Entity target) {
        if (mc.thePlayer == null || target == null) {
            this.reduceYaw = Float.NaN;
            this.reducePitch = Float.NaN;
            return;
        }

        double yDist = target.posY - mc.thePlayer.posY;
        double targetY;
        if (yDist >= 1.7) {
            targetY = target.posY;
        } else if (yDist <= -1.7) {
            targetY = target.posY + target.getEyeHeight();
        } else {
            targetY = target.posY + target.getEyeHeight() / 2.0f;
        }

        float[] rotations = RotationUtil.getRotations(
                target.posX - mc.thePlayer.posX,
                targetY - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()),
                target.posZ - mc.thePlayer.posZ,
                mc.thePlayer.rotationYaw,
                mc.thePlayer.rotationPitch,
                180.0f,
                0.0f
        );
        this.reduceYaw = rotations[0];
        this.reducePitch = MathHelper.clamp_float(rotations[1], -90.0f, 90.0f);
    }

    private boolean isMoving() {
        if (mc.thePlayer == null) {
            return false;
        }
        return mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f;
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (this.reduceMode.getValue() && this.reduceType.getValue() == 0) {
            this.attackCounter++;
        }

        if (this.autoReduceActive) {
            this.hitsCount++;
        }
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            return;
        }

        if (mc.thePlayer.hurtTime > 0) {
            if (this.pendingExplosion) {
                this.pendingExplosion = false;
                this.handleExplosion(event);
                return;
            }

            if (this.attackReduce3.getValue() && event.getY() > 0.0) {
                this.attackReduce3Active = true;
                this.attackReduce3TicksLeft = 1;
            }

            if (this.autoReduce.getValue() && event.getY() > 0.0) {
                double velocityX = event.getX();
                double velocityZ = event.getZ();
                double velocityY = event.getY();
                hasReceivedVelocity = true;
                this.validVelocityReceived = true;

                this.autoReduceActive = true;
                this.hitsCount = 0;

                Entity target = this.getAuraTarget();
                if (target != null) {
                    this.computeReduceRotations(target);
                    this.reduceRotateTicksLeft = 3;
                    if (this.reduceDebug.getValue()) {
                        ChatUtil.sendFormatted("&6&l[AutoReduce&6&l] &7| &fActivated &7| &fTarget: &6" + target.getName());
                    }
                }
            }

            if (this.rotate.getValue() && event.getY() > 0.0) {
                boolean isAirborne = !mc.thePlayer.onGround;
                boolean shouldRotate = !isAirborne || (this.airRotate.getValue() && isAirborne);

                if (shouldRotate) {
                    this.knockbackX = event.getX();
                    this.knockbackZ = event.getZ();
                    if (Math.abs(this.knockbackX) > 0.01 || Math.abs(this.knockbackZ) > 0.01) {
                        this.rotateTickCounter = 1;
                        if (this.showLog.getValue()) {
                            ChatUtil.sendFormatted(String.format("%s[Rotation] Rotations&r", Epilogue.clientName));
                        }
                    }
                }
            }

            if (this.jumpReset.getValue() && event.getY() > 0.0) {
                this.jumpFlag = true;
                if (this.showLog.getValue()) {
                    String triggerLog = String.format(
                            "&a&l[%s&a&l] &7| &a&lJump Reset &7| &fVelocityX: &a%.3f &7| &fVelocityZ: &a%.3f &7| &fVelocityY: &a%.3f",
                            Epilogue.clientName,
                            event.getX(),
                            event.getZ(),
                            event.getY()
                    );
                    ChatUtil.sendFormatted(triggerLog);
                }
            }

            this.applyVanilla(event);

            if (this.showLog.getValue()) {
                ChatUtil.sendFormatted(
                        String.format(
                                "%sReleases X:%.3f Z:%.3f Y:%.3f&r",
                                Epilogue.clientName,
                                event.getX(),
                                event.getZ(),
                                event.getY()
                        )
                );
            }
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        if (this.horizontal.getValue() > 0) {
            event.setX(event.getX() * this.horizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.horizontal.getValue() / 100.0);
        }
        if (this.vertical.getValue() > 0) {
            event.setY(event.getY() * this.vertical.getValue() / 100.0);
        }
    }

    private void handleExplosion(KnockbackEvent event) {
        if (this.explosionHorizontal.getValue() > 0) {
            event.setX(event.getX() * this.explosionHorizontal.getValue() / 100.0);
            event.setZ(event.getZ() * this.explosionHorizontal.getValue() / 100.0);
        }
        if (this.explosionVertical.getValue() > 0) {
            event.setY(event.getY() * this.explosionVertical.getValue() / 100.0);
        }
    }

    private int getActiveRotateTicks() {
        int maxTick = this.rotateTick.getValue();
        if (this.airRotate.getValue() && mc.thePlayer != null && !mc.thePlayer.onGround) {
            maxTick = this.airRotateTicks.getValue();
        }
        return maxTick;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() == EventType.PRE) {
            int maxTick = this.getActiveRotateTicks();

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

            if (this.reduceRotateTicksLeft > 0 && !Float.isNaN(this.reduceYaw) && !Float.isNaN(this.reducePitch)) {
                event.setRotation(this.reduceYaw, this.reducePitch, 180);
                event.setPervRotation(this.reduceYaw, 180);
                this.reduceRotateTicksLeft--;
            }

            if (this.autoReduce.getValue() && hasReceivedVelocity && this.validVelocityReceived) {
                if (mc.thePlayer.hurtTime == 9) {
                    this.hitsCount++;
                }

                Entity target = this.getAuraTarget();
                if (this.isMoving() && mc.thePlayer.isSprinting() && target != null && target != mc.thePlayer && target instanceof EntityPlayer) {
                    double distance = mc.thePlayer.getDistanceToEntity(target);
                    if (distance > 4.0) {
                        if (this.reduceDebug.getValue()) {
                            ChatUtil.sendFormatted("&6&l[AutoReduce&6&l] &7| &cTarget too far: " + String.format("%.1f", distance));
                        }
                        return;
                    }

                    if (target.isDead) {
                        if (this.reduceDebug.getValue()) {
                            ChatUtil.sendFormatted("&6&l[AutoReduce&6&l] &7| &cTarget is dead");
                        }
                        return;
                    }

                    EventManager.call(new AttackEvent(target));
                    mc.getNetHandler().addToSendQueue(new C0APacketAnimation());
                    mc.getNetHandler().addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK));

                    if (this.reduceDebug.getValue()) {
                        ChatUtil.sendFormatted("&6&l[AutoReduce&6&l] &7| &6&lCounter Attack");
                    }

                    mc.thePlayer.motionX *= 0.6;
                    mc.thePlayer.motionZ *= 0.6;
                    mc.thePlayer.setSprinting(false);

                    if (this.reduceDebug.getValue()) {
                        ChatUtil.sendFormatted(
                                String.format(
                                        "&6&l[AutoReduce&6&l] &7| &fReduce0.6 &7| &fMotionX: &6%.3f &7| &fMotionZ: &6%.3f &7| &fDistance: &6%.1f",
                                        mc.thePlayer.motionX,
                                        mc.thePlayer.motionZ,
                                        distance
                                )
                        );
                    }

                    hasReceivedVelocity = false;
                    this.validVelocityReceived = false;
                    this.autoReduceActive = false;
                }
            }
        }

        if (event.getType() == EventType.POST) {
            int maxTick = this.getActiveRotateTicks();
            if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick) {
                this.rotateTickCounter++;
                if (this.rotateTickCounter > maxTick) {
                    this.rotateTickCounter = 0;
                    this.targetRotation = null;
                    this.knockbackX = 0.0;
                    this.knockbackZ = 0.0;
                }
            }

            if (this.reduceMode.getValue() && this.reduceType.getValue() == 1 && this.attackReduceTicksLeft > 0) {
                this.attackReduceTicksLeft--;
                if (this.attackReduceTicksLeft <= 0) {
                }
            }

            if (this.attackReduce3TicksLeft > 0) {
                this.attackReduce3TicksLeft--;
                if (this.attackReduce3TicksLeft <= 0 && this.attackReduce3Active) {
                    float factor = this.reduce3Factor.getValue();
                    mc.thePlayer.motionX *= factor;
                    mc.thePlayer.motionZ *= factor;
                    if (this.sprintReset3.getValue()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    ChatUtil.sendFormatted(Epilogue.clientName + "&aAttackReduce3 reduced velocity by " + (int) ((1.0f - factor) * 100.0f) + "%");
                    this.attackReduce3Active = false;
                }
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer.onGround
                    && mc.thePlayer.isSprinting()
                    && !mc.thePlayer.isPotionActive(Potion.moveSlowdown)
                    && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if (this.showLog.getValue()) {
                    String logMessage = String.format(
                            "&b&l[%s&b&l] &7| &b&lJump Reset &7| &fMotionX: &b%.3f &7| &fMotionZ: &b%.3f &7| &fMotionY: &b%.3f",
                            Epilogue.clientName,
                            mc.thePlayer.motionX,
                            mc.thePlayer.motionZ,
                            mc.thePlayer.motionY
                    );
                    ChatUtil.sendFormatted(logMessage);
                }
            }
        }

        if (this.autoReduce.getValue() && hasReceivedVelocity && this.validVelocityReceived) {
            if (mc.thePlayer.hurtTime > 0
                    && this.lastHurtTime == 0
                    && mc.thePlayer.onGround
                    && mc.thePlayer.isSprinting()
                    && !mc.thePlayer.isPotionActive(Potion.moveSlowdown)
                    && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if (this.reduceDebug.getValue()) {
                    String log = String.format(
                            "&6&l[AutoReduce&6&l] &7| &6&lJump Reset (1st tick) &7| &fMotionX: &6%.3f &7| &fMotionZ: &6%.3f &7| &fMotionY: &6%.3f",
                            mc.thePlayer.motionX,
                            mc.thePlayer.motionZ,
                            mc.thePlayer.motionY
                    );
                    ChatUtil.sendFormatted(log);
                }
            }
            this.lastHurtTime = mc.thePlayer.hurtTime;
        } else {
            this.lastHurtTime = 0;
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        int maxTick = this.getActiveRotateTicks();
        if (this.rotateTickCounter > 0 && this.rotateTickCounter <= maxTick && this.autoMove.getValue()) {
            mc.thePlayer.movementInput.moveForward = 1.0f;
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
            S12PacketEntityVelocity p = (S12PacketEntityVelocity) event.getPacket();
            if (p.getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }

            double motionX = p.getMotionX() / 8000.0;
            double motionY = p.getMotionY() / 8000.0;
            double motionZ = p.getMotionZ() / 8000.0;
            double horizontalPower = Math.sqrt(motionX * motionX + motionZ * motionZ);

            if (this.autoReduce.getValue()) {
                if (horizontalPower > 0.1 && motionY >= 0.0) {
                    this.validVelocityReceived = true;
                    hasReceivedVelocity = true;
                    this.autoReduceActive = true;
                    this.hitsCount = 0;
                    Entity target = this.getAuraTarget();
                    if (target != null) {
                        this.computeReduceRotations(target);
                        this.reduceRotateTicksLeft = 3;
                    }
                } else {
                    this.validVelocityReceived = false;
                    hasReceivedVelocity = false;
                    if (this.reduceDebug.getValue()) {
                        ChatUtil.sendFormatted("&6&l[AutoReduce&6&l] &7| &cInvalid velocity (\u4f20\u9001/\u6362\u4e16\u754c)");
                    }
                }
            }

            if (this.reduceMode.getValue()) {
                if (this.reduceType.getValue() == 0) {
                    if (this.attackCounter >= this.attackTick.getValue()) {
                        float factor = this.reduceFactor.getValue();
                        mc.thePlayer.motionX *= factor;
                        mc.thePlayer.motionZ *= factor;
                        if (this.sprintReset.getValue()) {
                            mc.thePlayer.setSprinting(false);
                        }
                        ChatUtil.sendFormatted(Epilogue.clientName + "&aSuccessfully reduced velocity by " + (int) ((1.0f - factor) * 100.0f) + "%");
                        this.attackCounter = 0;
                    }
                } else if (this.reduceType.getValue() == 1) {
                    this.attackReduceTicksLeft = 2;
                    float factor = this.reduceFactor.getValue();
                    mc.thePlayer.motionX *= factor;
                    mc.thePlayer.motionZ *= factor;
                    if (this.sprintReset.getValue()) {
                        mc.thePlayer.setSprinting(false);
                    }
                    ChatUtil.sendFormatted(Epilogue.clientName + "&aReduce2 activated! Velocity reduced by " + (int) ((1.0f - factor) * 100.0f) + "%");
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
    public void onLoadWorld(LoadWorldEvent event) {
    }

    @Override
    public void onEnabled() {
        this.resetFlags();
    }

    @Override
    public void onDisabled() {
        this.resetFlags();
    }

    private void resetFlags() {
        this.pendingExplosion = false;
        this.jumpFlag = false;
        this.rotateTickCounter = 0;
        this.targetRotation = null;
        this.knockbackX = 0.0;
        this.knockbackZ = 0.0;
        this.attackCounter = 0;
        this.attackReduceTicksLeft = 0;
        this.attackReduce3Active = false;
        this.attackReduce3TicksLeft = 0;
        hasReceivedVelocity = false;
        this.validVelocityReceived = false;
        this.hitsCount = 0;
        this.autoReduceActive = false;
        this.lastHurtTime = 0;
        this.reduceYaw = Float.NaN;
        this.reducePitch = Float.NaN;
        this.reduceRotateTicksLeft = 0;
    }

    @Override
    public String[] getSuffix() {
        if (this.reduceMode.getValue()) {
            return new String[]{"Reduce"};
        }
        return new String[0];
    }
}
