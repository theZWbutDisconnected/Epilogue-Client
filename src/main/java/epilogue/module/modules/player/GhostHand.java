package epilogue.module.modules.player;

import epilogue.module.Module;
import epilogue.util.ItemUtil;
import epilogue.util.TeamUtil;
import epilogue.value.values.BooleanValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

public class GhostHand extends Module {
    public final BooleanValue teamsOnly = new BooleanValue("TeamOnly", true);
    public final BooleanValue ignoreWeapons = new BooleanValue("IgnoreWeapons", false);

    public GhostHand() {
        super("GhostHand", false);
    }

    public boolean shouldSkip(Entity entity) {
        return entity instanceof EntityPlayer
                && !TeamUtil.isBot((EntityPlayer) entity)
                && (!this.teamsOnly.getValue() || TeamUtil.isSameTeam((EntityPlayer) entity))
                && (!this.ignoreWeapons.getValue() || !ItemUtil.hasRawUnbreakingEnchant());
    }
}
