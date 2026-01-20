package epilogue.module.modules.movement;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.events.LivingUpdateEvent;
import epilogue.events.PlayerUpdateEvent;
import epilogue.events.RightClickMouseEvent;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import epilogue.util.BlockUtil;
import epilogue.util.ItemUtil;
import epilogue.util.PlayerUtil;
import epilogue.util.TeamUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;

public class NoSlow extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private long lastBlockingTime = 0L;

    private boolean wasUsingItem = false;
    private int predictionBlinkTimer = 0;
    private boolean isPredictionBlinking = false;

    public final ModeValue swordMode = new ModeValue("Sword Mode", 2, new String[]{"None", "Vanilla", "Prediction"});
    public final BooleanValue onlyKillAuraAutoBlock = new BooleanValue("Only KillAura AutoBlock", false, () -> this.swordMode.getValue() != 0);
    public final IntValue swordBlinkDelay = new IntValue("Delay", 1, 1, 10, () -> this.swordMode.getValue() == 2);
    public final IntValue swordBlinkDuration = new IntValue("Duration", 1, 1, 5, () -> this.swordMode.getValue() == 2);
    public final BooleanValue swordSprint = new BooleanValue("Sword Sprint", true, () -> this.swordMode.getValue() != 0);
    public final ModeValue foodMode = new ModeValue("Food Mode", 0, new String[]{"None", "Vanilla"});
    public final BooleanValue foodSprint = new BooleanValue("Food Sprint", true, () -> this.foodMode.getValue() != 0);
    public final ModeValue bowMode = new ModeValue("Bow Mode", 0, new String[]{"None", "Vanilla"});
    public final BooleanValue bowSprint = new BooleanValue("Bow Sprint", true, () -> this.bowMode.getValue() != 0);

    public NoSlow() {
        super("NoSlow", false);
    }

    public boolean isSwordActive() {
        return this.swordMode.getValue() != 0 && ItemUtil.isHoldingSword() && (!this.onlyKillAuraAutoBlock.getValue() || this.isKillAuraAutoBlocking());
    }

    public boolean isPredictionMode() {
        return this.swordMode.getValue() == 2 && this.isSwordActive();
    }

    private boolean isKillAuraAutoBlocking() {
        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura.autoBlock.getValue() == 0 || aura.autoBlock.getValue() == 2 || !aura.isPlayerBlocking() || !aura.isEnabled()) {
            return false;
        }
        return aura.shouldAutoBlock();
    }

    public boolean isFoodActive() {
        return this.foodMode.getValue() != 0 && ItemUtil.isEating();
    }

    public boolean isBowActive() {
        return this.bowMode.getValue() != 0 && ItemUtil.isUsingBow();
    }

    public boolean isAnyActive() {
        return NoSlow.mc.thePlayer.isUsingItem() && (this.isSwordActive() || this.isFoodActive() || this.isBowActive());
    }

    public boolean canSprint() {
        return this.isSwordActive() && this.swordSprint.getValue() != false
                || this.isFoodActive() && this.foodSprint.getValue() != false
                || this.isBowActive() && this.bowSprint.getValue() != false;
    }

    private void sendC09() {
        if (mc.thePlayer != null && mc.getNetHandler() != null) {
            int current = mc.thePlayer.inventory.currentItem;
            int next = (current + 1) % 9;
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(next));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(current));
        }
    }

    private void handlePredictionMode() {
        if (mc.thePlayer == null) return;
        boolean isUsingNow = PlayerUtil.isUsingItem();
        if (isUsingNow && !this.wasUsingItem) {
            this.sendC09();
            this.predictionBlinkTimer = 0;
            this.isPredictionBlinking = true;
        }
        this.wasUsingItem = isUsingNow;
        if (this.isPredictionBlinking) {
            ++this.predictionBlinkTimer;
            int delay = this.swordBlinkDelay.getValue();
            int duration = this.swordBlinkDuration.getValue();
            int total = delay + duration;
            int phase = this.predictionBlinkTimer % total;
            if (phase >= delay) {
                mc.thePlayer.stopUsingItem();

                this.isPredictionBlinking = false;
            }
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        boolean isCurrentlyBlocking = this.isSwordActive() && PlayerUtil.isUsingItem();

        if (this.isPredictionMode()) {
            this.handlePredictionMode();
        }

        if (isCurrentlyBlocking) {
            this.lastBlockingTime = System.currentTimeMillis();
        }

        boolean inSprintProtection = System.currentTimeMillis() - this.lastBlockingTime < 300L;
        boolean playerWantsToSprint = NoSlow.mc.gameSettings.keyBindSprint.isKeyDown();

        if (this.isAnyActive() || inSprintProtection) {
            NoSlow.mc.thePlayer.setSprinting((this.canSprint() || inSprintProtection) && playerWantsToSprint && NoSlow.mc.thePlayer.movementInput.moveForward > 0.1f);
        }
    }

    @EventTarget(value=3)
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.isEnabled()) {
            if (NoSlow.mc.objectMouseOver != null) {
                switch (NoSlow.mc.objectMouseOver.typeOfHit) {
                    case BLOCK: {
                        BlockPos blockPos = NoSlow.mc.objectMouseOver.getBlockPos();
                        if (!BlockUtil.isInteractable(blockPos) || PlayerUtil.isSneaking()) break;
                        return;
                    }
                    case ENTITY: {
                        Entity entityHit = NoSlow.mc.objectMouseOver.entityHit;
                        if (entityHit instanceof EntityVillager) {
                            return;
                        }
                        if (!(entityHit instanceof EntityLivingBase) || !TeamUtil.isShop((EntityLivingBase)entityHit)) break;
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        this.lastBlockingTime = 0L;

        this.wasUsingItem = false;
        this.predictionBlinkTimer = 0;
        this.isPredictionBlinking = false;
    }

    @Override
    public void onDisabled() {
        this.lastBlockingTime = 0L;

        this.wasUsingItem = false;
        this.predictionBlinkTimer = 0;
        this.isPredictionBlinking = false;

        if (mc.thePlayer != null) {
            mc.thePlayer.stopUsingItem();
        }
    }
}