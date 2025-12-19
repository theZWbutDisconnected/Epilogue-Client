package epilogue.module.modules.misc;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.events.PacketEvent;
import epilogue.event.types.EventType;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import epilogue.util.PacketUtil;
import epilogue.value.values.ModeValue;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.ArrayList;
import java.util.List;

public class Disabler extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeValue mode = new ModeValue("Mode", 0, new String[]{"PredictionInventory"});

    private final List<Packet<?>> inventoryPackets = new ArrayList<>();

    public Disabler() {
        super("Disabler", false);
    }

    @Override
    public void onEnabled() {
        String currentMode = mode.getModeString();
        if (currentMode.equals("PredictionInventory")) {
            ChatUtil.sendFormatted(String.format("%s%s: You can use Vanilla-InvWalk & Silent-InvManager now",
                    Epilogue.clientName, this.getName()));
        }

        resetStates();
    }

    @Override
    public void onDisabled() {
        if (!inventoryPackets.isEmpty()) {
            for (Packet<?> p : inventoryPackets) {
                PacketUtil.sendPacketNoEvent(p);
            }
            inventoryPackets.clear();
        }
        resetStates();
    }

    private void resetStates() {
        inventoryPackets.clear();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;

        if (event.getType() == EventType.SEND) {
            handlePredictionInventory(event);
        }
    }

    private void handlePredictionInventory(PacketEvent event) {
        if (!mode.getModeString().equals("PredictionInventory")) return;

        Packet<?> packet = event.getPacket();
        if (packet instanceof C16PacketClientStatus || packet instanceof C0EPacketClickWindow) {
            event.setCancelled(true);
            inventoryPackets.add(packet);
        } else if (packet instanceof C0DPacketCloseWindow) {
            for (Packet<?> p : inventoryPackets) {
                PacketUtil.sendPacketNoEvent(p);
            }
            inventoryPackets.clear();
        }
    }
}