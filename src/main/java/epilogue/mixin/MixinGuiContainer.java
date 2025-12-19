package epilogue.mixin;

import epilogue.Epilogue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainer;
import epilogue.module.modules.player.ChestStealer;
import epilogue.module.modules.render.ChestView;
import epilogue.module.modules.render.dynamicisland.DynamicIsland;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {
    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (((Object) this) instanceof GuiChest) {
            ChestView chestView = (ChestView) Epilogue.moduleManager.getModule("ChestView");
            ChestStealer chestStealer = (ChestStealer) Epilogue.moduleManager.getModule("ChestStealer");
            DynamicIsland dynamicIsland = (DynamicIsland) Epilogue.moduleManager.getModule("DynamicIsland");
            if ((chestView != null && chestView.isEnabled() &&
                chestStealer != null && chestStealer.isEnabled()) || (dynamicIsland.isEnabled() && chestStealer.isEnabled())) {
                Minecraft.getMinecraft().setIngameFocus();
                Minecraft.getMinecraft().currentScreen = (GuiChest) ((Object) this);
                ci.cancel();
            }
        }
    }
}