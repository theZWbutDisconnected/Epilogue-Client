package epilogue.mixin;

import epilogue.event.EventManager;
import epilogue.events.ChatGUIEvent;
import epilogue.ui.chat.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiScreen.class)
public class MixinGuiChat {
    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        GuiScreen self = (GuiScreen) (Object) this;
        if (!(self instanceof net.minecraft.client.gui.GuiChat) && !(self instanceof GuiChat)) {
            return;
        }
        EventManager.call(new ChatGUIEvent(mouseX, mouseY));
    }
}
