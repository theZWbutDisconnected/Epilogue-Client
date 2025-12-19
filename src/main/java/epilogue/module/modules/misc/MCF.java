package epilogue.module.modules.misc;

import epilogue.Epilogue;
import epilogue.event.EventTarget;
import epilogue.events.KeyEvent;
import epilogue.module.Module;
import epilogue.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class MCF extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCF() {
        super("MCF", false, true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (this.isEnabled() && event.getKey() == -98) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectType.ENTITY && mc.objectMouseOver.entityHit instanceof EntityPlayer) {
                String hitName = mc.objectMouseOver.entityHit.getName();
                if (!Epilogue.friendManager.isFriend(hitName)) {
                    Epilogue.friendManager.add(hitName);
                    ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your friend list&r", Epilogue.clientName, hitName));
                } else {
                    Epilogue.friendManager.remove(hitName);
                    ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your friend list&r", Epilogue.clientName, hitName));
                }
            }
        }
    }
}
