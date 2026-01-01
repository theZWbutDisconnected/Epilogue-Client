package epilogue.module.modules.player;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.*;
import epilogue.management.RotationState;
import epilogue.module.Module;
import epilogue.module.modules.combat.Aura;
import epilogue.util.*;
import epilogue.value.values.BooleanValue;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.ModeValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockFurnace;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChestAura extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final IntValue delay = new IntValue("Delay", 100, 0, 1000);
    private final FloatValue rotationSpeed = new FloatValue("RotationSpeed", 180.0f, 1.0f, 180.0f);
    private final FloatValue range = new FloatValue("Range", 3.0f, 0.0f, 15.0f);
    private final BooleanValue swing = new BooleanValue("Swing", true);
    private final ModeValue moveFix = new ModeValue("MoveFix", 1, new String[]{"None", "Silent"});

    private final TimerUtil delayAfterOpenTimer = new TimerUtil();

    private final Set<Long> opened = new HashSet<>();

    public ChestAura() {
        super("ChestAura", false);
    }

    @Override
    public void onEnabled() {
        this.opened.clear();
        this.delayAfterOpenTimer.reset();
    }

    @Override
    public void onDisabled() {
        this.opened.clear();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(this.range.getValue())};
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (currentGuiScreeen()) return;

        if (!delayAfterOpenTimer.hasTimeElapsed((long) this.delay.getValue() * 10L)) {
            return;
        }

        if (mc.thePlayer.ticksExisted % 20 == 0) {
            return;
        }

        if (mc.currentScreen instanceof GuiContainer) {
            return;
        }

        float radius = this.range.getValue();
        float reach = mc.playerController.getBlockReachDistance();

        BlockPos targetPos = null;
        List<BlockPos> candidates = new ArrayList<>();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityBrewingStand || tileEntity instanceof TileEntityFurnace)) {
                continue;
            }

            BlockPos pos = tileEntity.getPos();
            if (pos == null) {
                continue;
            }
            if (opened.contains(pos.toLong())) {
                continue;
            }

            if (mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > radius) {
                continue;
            }
            if (mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= reach) {
                continue;
            }

            Block block = mc.theWorld.getBlockState(pos).getBlock();
            if (!(block instanceof BlockChest || block instanceof BlockFurnace || block instanceof BlockBrewingStand)) {
                continue;
            }

            candidates.add(pos);
        }

        if (candidates.isEmpty()) {
            return;
        }

        candidates.sort(Comparator.comparingDouble(p -> mc.thePlayer.getDistance(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)));

        for (BlockPos pos : candidates) {
            float[] bestRotations = null;
            MovingObjectPosition bestRay = null;

            for (double[] offset : getAimOffsets()) {
                float[] rotations = RotationUtil.getRotationsTo(
                        pos.getX() + offset[0],
                        pos.getY() + offset[1],
                        pos.getZ() + offset[2],
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch
                );

                float maxStep = this.rotationSpeed.getValue();
                float yawDelta = MathHelper.wrapAngleTo180_float(rotations[0] - mc.thePlayer.rotationYaw);
                float pitchDelta = MathHelper.wrapAngleTo180_float(rotations[1] - mc.thePlayer.rotationPitch);
                yawDelta = Math.max(-maxStep, Math.min(maxStep, yawDelta));
                pitchDelta = Math.max(-maxStep, Math.min(maxStep, pitchDelta));

                float yaw = mc.thePlayer.rotationYaw + yawDelta;
                float pitch = mc.thePlayer.rotationPitch + pitchDelta;

                MovingObjectPosition ray = RotationUtil.rayTrace(yaw, pitch, reach, 1.0f);
                if (ray == null || ray.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || ray.getBlockPos() == null || !ray.getBlockPos().equals(pos) || ray.sideHit == null) {
                    continue;
                }

                bestRotations = new float[]{yaw, pitch};
                bestRay = ray;
                break;
            }

            if (bestRotations == null || bestRay == null) {
                continue;
            }

            targetPos = pos;

            event.setRotation(bestRotations[0], bestRotations[1], 3);

            float hitX = (float) (bestRay.hitVec.xCoord - (double) targetPos.getX());
            float hitY = (float) (bestRay.hitVec.yCoord - (double) targetPos.getY());
            float hitZ = (float) (bestRay.hitVec.zCoord - (double) targetPos.getZ());

            PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(targetPos, bestRay.sideHit.getIndex(), mc.thePlayer.getCurrentEquippedItem(), hitX, hitY, hitZ));
            if (this.swing.getValue()) {
                mc.thePlayer.swingItem();
            } else {
                PacketUtil.sendPacket(new C0APacketAnimation());
            }

            opened.add(targetPos.toLong());
            this.delayAfterOpenTimer.reset();
            return;
        }
    }

    private static List<double[]> getAimOffsets() {
        List<double[]> offsets = new ArrayList<>();
        offsets.add(new double[]{0.5, 0.5, 0.5});
        offsets.add(new double[]{0.5, 0.25, 0.5});
        offsets.add(new double[]{0.5, 0.75, 0.5});
        offsets.add(new double[]{0.25, 0.5, 0.5});
        offsets.add(new double[]{0.75, 0.5, 0.5});
        offsets.add(new double[]{0.5, 0.5, 0.25});
        offsets.add(new double[]{0.5, 0.5, 0.75});
        return offsets;
    }

    @EventTarget
    public void onPostMotion(PostMotionEvent event) {
        if (!this.isEnabled()) {
            return;
        }
    }

    private boolean currentGuiScreeen() {
        GuiScreen current = mc.currentScreen;
        if (mc.thePlayer == null || mc.theWorld == null) {
            return true;
        }

        if (mc.thePlayer.isOnLadder()) {
            return true;
        }

        if (current instanceof GuiChest) {
            return true;
        }

        Aura aura = (Aura) Epilogue.moduleManager.modules.get(Aura.class);
        if (aura != null && aura.target != null) {
            return true;
        }

        if (mc.thePlayer.isUsingItem()) {
            return true;
        }

        Scaffold scaffold = (Scaffold) Epilogue.moduleManager.modules.get(Scaffold.class);
        return scaffold != null && scaffold.isEnabled();
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        RenderUtil.enableRenderState();
        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (tileEntity instanceof TileEntityChest || tileEntity instanceof TileEntityBrewingStand || tileEntity instanceof TileEntityFurnace) {
                BlockPos pos = tileEntity.getPos();
                Color color = opened.contains(pos.toLong()) ? new Color(255, 0, 0, 60) : new Color(25, 255, 0, 120);
                if (mc.thePlayer.getDistance(pos.getX(), pos.getY(), pos.getZ()) < 20.0) {
                    RenderUtil.drawBlockBox(pos, 1.0, color.getRed(), color.getGreen(), color.getBlue());
                    RenderUtil.drawBlockBoundingBox(pos, 1.0, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha(), 1.0f);
                }
            }
        }
        RenderUtil.disableRenderState();
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled()) {
            if (this.moveFix.getValue() == 1
                    && RotationState.isActived()
                    && RotationState.getPriority() == 3.0F
                    && MoveUtil.isForwardPressed()) {
                MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
            }
        }
    }

    @EventTarget
    public void onWorld(LoadWorldEvent event) {
        this.opened.clear();
    }
}