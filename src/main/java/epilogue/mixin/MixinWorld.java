package epilogue.mixin;

import epilogue.Epilogue;
import epilogue.module.modules.movement.Jesus;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SideOnly(Side.CLIENT)
@Mixin({World.class})
public abstract class MixinWorld {
    @Redirect(
            method = {"handleMaterialAcceleration"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/Entity;isPushedByWater()Z"
            )
    )
    private boolean handleMaterialAcceleration(Entity entity) {
        if (entity instanceof EntityPlayerSP && Epilogue.moduleManager != null) {
            Jesus jesus = (Jesus) Epilogue.moduleManager.modules.get(Jesus.class);
            if (jesus.isEnabled() && jesus.noPush.getValue()) {
                return false;
            }
        }
        return entity.isPushedByWater();
    }
}
