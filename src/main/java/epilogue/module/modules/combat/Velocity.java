package epilogue.module.modules.combat;

import com.google.common.base.CaseFormat;
import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.Potion;
import net.minecraft.world.World;
import epilogue.enums.BlinkModules;
import epilogue.enums.DelayModules;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.mixin.IAccessorEntity;
import epilogue.module.Module;
import epilogue.module.modules.movement.LongJump;
import epilogue.util.ChatUtil;
import epilogue.util.MoveUtil;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.value.values.PercentValue;

public class Velocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int chanceCounter = 0;
    private int delayChanceCounter = 0;
    private boolean pendingExplosion = false;
    private boolean allowNext = true;
    private boolean reverseFlag = false;
    private boolean delayActive = false;
    private long lastAttackTime = 0L;
    private long blinkStartTime = System.currentTimeMillis();
    private final long blinkDuration = 95L;
    private long reverseStartTime = 0L;
    private boolean jumpFlag = false;
    private int blinkTicks = 0;

    private boolean attackReduceTriggered = false;
    private boolean attackReduceSuccess = false;

    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"Vanilla", "Jump", "Prediction"});
    public final BooleanValue attackReduce = new BooleanValue("Attack Reduce", true, () -> this.mode.getValue() == 2);
    public final PercentValue chance = new PercentValue("Chance", 100);
    public final PercentValue horizontal = new PercentValue("Horizontal", 100);
    public final PercentValue vertical = new PercentValue("Vertical", 100);
    public final PercentValue explosionHorizontal = new PercentValue("ExplosionsHorizontal", 100);
    public final PercentValue explosionVertical = new PercentValue("ExplosionsVertical", 100);
    public final BooleanValue fakeCheck = new BooleanValue("FakeCheck", true);
    public final BooleanValue debugLog = new BooleanValue("DebugLog", true);
    public final IntValue delayTicks = new IntValue("DelayTicks", 1, 1, 20, () -> this.mode.getValue() == 2);
    public final PercentValue delayChance = new PercentValue("Chance", 100, () -> this.mode.getValue() == 2);
    public final BooleanValue jumpReset = new BooleanValue("JumpReset", true, () -> this.mode.getValue() == 2);
    public final BooleanValue sprintReset = new BooleanValue("SprintReset", true, () -> this.mode.getValue() == 2);
    public final IntValue hurtTime = new IntValue("Normal Reduce HurtTime", 10, 1, 10, () -> this.mode.getValue() == 2);
    public final FloatValue factor = new FloatValue("Norma Reduce Factor", 0.6F, 0.1F, 1.0F, () -> this.mode.getValue() == 2);
    public final BooleanValue blink = new BooleanValue("Blink", true, () -> this.mode.getValue() == 2);
    public final BooleanValue showBlinkTicks = new BooleanValue("Show Blink Ticks", false, () -> this.mode.getValue() == 2 && this.blink.getValue());
    public final BooleanValue airDelay = new BooleanValue("Air Delay", false, () -> this.mode.getValue() == 1);

    public Velocity() {
        super("Velocity", false);
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer != null && (mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || ((IAccessorEntity) mc.thePlayer).getIsInWeb());
    }

    private boolean canDelay() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        return mc.thePlayer.onGround && (aura == null || !aura.isEnabled() || !aura.shouldAutoBlock());
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!this.isEnabled() || event.isCancelled() || mc.thePlayer == null) {
            this.pendingExplosion = false;
            this.allowNext = true;
            this.attackReduceTriggered = false;
            return;
        }

        if (this.mode.getValue() == 2) {
            if (!this.allowNext || !this.fakeCheck.getValue()) {
                this.allowNext = true;
                if (this.pendingExplosion) {
                    this.pendingExplosion = false;
                    this.handleExplosion(event);
                    return;
                }

                if (this.jumpReset.getValue() && event.getY() > 0.0) {
                    this.jumpFlag = true;
                    this.attackReduceSuccess = this.attackReduce.getValue() && this.attackReduceTriggered;
                    if (this.debugLog.getValue()) {
                        if (this.attackReduceSuccess) {
                            ChatUtil.sendFormatted(String.format("%sjr!&r", Epilogue.clientName));
                        } else {
                            ChatUtil.sendFormatted(String.format("%sjr!&r", Epilogue.clientName));
                        }
                    }
                }

                if (this.attackReduce.getValue() && this.attackReduceTriggered) {
                    this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
                    if (this.debugLog.getValue()) {
                        ChatUtil.sendFormatted(Epilogue.clientName + "AttackReduce " + this.horizontal.getValue() + "%");
                    }
                    this.attackReduceTriggered = false;
                    return;
                }

                this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
            }
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
                    this.jumpFlag = this.mode.getValue() == 1 && event.getY() > 0.0;
                    if (this.mode.getValue() == 1 && event.getY() > 0.0) {
                        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
                    } else {
                        this.applyVanilla(event);
                    }
                    this.chanceCounter = 0;
                }
            }
        }
    }

    private void applyMotion(KnockbackEvent event, int horizontalPct, int verticalPct) {
        if (horizontalPct > 0) {
            event.setX(event.getX() * horizontalPct / 100.0);
            event.setZ(event.getZ() * horizontalPct / 100.0);
        } else {
            event.setX(mc.thePlayer.motionX);
            event.setZ(mc.thePlayer.motionZ);
        }
        if (verticalPct > 0) {
            event.setY(event.getY() * verticalPct / 100.0);
        } else {
            event.setY(mc.thePlayer.motionY);
        }
    }

    private void applyVanilla(KnockbackEvent event) {
        this.applyMotion(event, this.horizontal.getValue(), this.vertical.getValue());
    }

    private void handleExplosion(KnockbackEvent event) {
        this.applyMotion(event, this.explosionHorizontal.getValue(), this.explosionVertical.getValue());
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
            if (this.mode.getValue() == 2) {
                LongJump longJump = (LongJump) Epilogue.moduleManager.modules.get(LongJump.class);
                boolean canStartJump = longJump != null && longJump.isEnabled() && longJump.canStartJump();
                if (!(this.reverseFlag || this.isInLiquidOrWeb() || this.pendingExplosion || (this.allowNext && this.fakeCheck.getValue()) || canStartJump)) {
                    this.delayChanceCounter = this.delayChanceCounter % 100 + this.delayChance.getValue();
                    if (this.delayChanceCounter >= 100) {
                        Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                        Epilogue.delayManager.delayedPacket.offer(packet);
                        event.setCancelled(true);
                        this.reverseFlag = true;
                        this.reverseStartTime = System.currentTimeMillis();
                        if (this.blink.getValue()) {
                            this.blinkStartTime = System.currentTimeMillis();
                            Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                        }
                        this.delayChanceCounter = 0;
                        return;
                    }
                }
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%svelocity!&r", Epilogue.clientName));
                }
                return;
            }

            if (this.mode.getValue() == 1 && this.airDelay.getValue() && mc.thePlayer != null && !mc.thePlayer.onGround) {
                Epilogue.delayManager.setDelayState(true, DelayModules.VELOCITY);
                Epilogue.delayManager.delayedPacket.offer(packet);
                event.setCancelled(true);
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%sdelay!&r", Epilogue.clientName));
                }
                return;
            }

            if (this.debugLog.getValue()) {
                ChatUtil.sendFormatted(String.format("%svelocity!&r", Epilogue.clientName));
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
                    if (this.mode.getValue() == 2 && this.attackReduce.getValue()) {
                        this.attackReduceTriggered = true;
                    }
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
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%sexplosion!&r", Epilogue.clientName));
                }
            }
        }
    }

    @EventTarget
    public void onSendPacket(PacketEvent event) {
        if (this.isEnabled() && this.mode.getValue() == 2 && event.getType() == EventType.SEND && !event.isCancelled() && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                this.lastAttackTime = System.currentTimeMillis();
            }
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }
        if (this.mode.getValue() == 2 && this.attackReduce.getValue() && this.attackReduceTriggered && mc.thePlayer.hurtTime > 0 && mc.thePlayer.hurtTime <= 10 && this.debugLog.getValue()) {
            ChatUtil.sendFormatted(Epilogue.clientName + "reduce!");
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (event.getType() == EventType.POST && this.isEnabled() && mc.thePlayer != null && this.mode.getValue() == 2) {
            if (this.sprintReset.getValue()) {
                if (mc.thePlayer.hurtTime == 8) {
                    mc.thePlayer.setSprinting(false);
                } else if (mc.thePlayer.hurtTime == 9) {
                    mc.thePlayer.setSprinting(true);
                }
            }
            if (this.factor.getValue() < 1.0F && mc.thePlayer.hurtTime == this.hurtTime.getValue() && System.currentTimeMillis() - this.lastAttackTime <= 8000L) {
                mc.thePlayer.motionX *= this.factor.getValue();
                mc.thePlayer.motionZ *= this.factor.getValue();
            }

            if (this.reverseFlag) {
                boolean shouldRelease = false;
                int delayValue = this.delayTicks.getValue();
                if (delayValue >= 1 && delayValue <= 3) {
                    long requiredDelayMs = delayValue == 1 ? 55L : (delayValue == 2 ? 60L : 100L);
                    if (System.currentTimeMillis() - this.reverseStartTime >= requiredDelayMs) {
                        shouldRelease = true;
                    }
                } else {
                    shouldRelease = this.canDelay() || this.isInLiquidOrWeb() || Epilogue.delayManager.getDelay() >= delayValue;
                }
                if (shouldRelease) {
                    Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
                    this.reverseFlag = false;
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                }
            }

            if (this.delayActive) {
                MoveUtil.setSpeed(MoveUtil.getSpeed(), MoveUtil.getMoveYaw());
                this.delayActive = false;
            }

            if (this.blink.getValue()) {
                if (System.currentTimeMillis() - this.blinkStartTime < this.blinkDuration) {
                    Epilogue.blinkManager.setBlinkState(true, BlinkModules.BLINK);
                    this.blinkTicks++;
                } else {
                    Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
                    this.blinkTicks = 0;
                }
            } else {
                this.blinkTicks = 0;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (this.jumpFlag) {
            this.jumpFlag = false;
            if (mc.thePlayer != null && mc.thePlayer.onGround && mc.thePlayer.isSprinting() && !mc.thePlayer.isPotionActive(Potion.jump) && !this.isInLiquidOrWeb()) {
                mc.thePlayer.movementInput.jump = true;
                if (this.debugLog.getValue()) {
                    ChatUtil.sendFormatted(String.format("%sjr successfully&r", Epilogue.clientName));
                }
            }
            this.attackReduceSuccess = false;
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.onDisabled();
    }

    @Override
    public void onEnabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.delayActive = false;
        this.lastAttackTime = 0L;
        this.blinkStartTime = System.currentTimeMillis();
        this.reverseStartTime = 0L;
        this.jumpFlag = false;
        this.attackReduceTriggered = false;
        this.attackReduceSuccess = false;
        this.blinkTicks = 0;
    }

    @Override
    public void onDisabled() {
        this.pendingExplosion = false;
        this.allowNext = true;
        this.chanceCounter = 0;
        this.delayChanceCounter = 0;
        this.reverseFlag = false;
        this.delayActive = false;
        this.lastAttackTime = 0L;
        this.reverseStartTime = 0L;
        this.jumpFlag = false;
        this.attackReduceTriggered = false;
        this.attackReduceSuccess = false;
        this.blinkTicks = 0;
        if (Epilogue.delayManager.getDelayModule() == DelayModules.VELOCITY) {
            Epilogue.delayManager.setDelayState(false, DelayModules.VELOCITY);
        }
        Epilogue.delayManager.delayedPacket.clear();
        Epilogue.blinkManager.setBlinkState(false, BlinkModules.BLINK);
    }

    @Override
    public String[] getSuffix() {
        String modeName = this.mode.getModeString();
        if (this.mode.getValue() == 2 && this.attackReduce.getValue()) {
            if (this.showBlinkTicks.getValue()) {
                return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName), "AttackReduce", String.valueOf(this.blinkTicks)};
            }
            return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName), "AttackReduce"};
        }
        if (this.showBlinkTicks.getValue()) {
            return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName), String.valueOf(this.blinkTicks)};
        }
        return new String[]{CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, modeName)};
    }
}