package epilogue.module.modules.misc;

import epilogue.event.EventTarget;
import epilogue.events.Render2DEvent;
import epilogue.module.Module;
import epilogue.util.TimerUtil;
import epilogue.value.values.FloatValue;
import epilogue.value.values.IntValue;
import epilogue.value.values.TextValue;
import net.minecraft.client.Minecraft;

public class Spammer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final TimerUtil timer = new TimerUtil();
    private int charOffset = 19968;
    public final TextValue text = new TextValue("Text", "meow");
    public final FloatValue delay = new FloatValue("Delay", 3.5F, 0.0F, 3600.0F);
    public final IntValue random = new IntValue("Random", 0, 0, 10);

    public Spammer() {
        super("Spammer", false);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled()) {
            if (this.timer.hasTimeElapsed((long) (this.delay.getValue() * 1000.0F))) {
                this.timer.reset();
                String text = this.text.getValue();
                if (this.random.getValue() > 0) {
                    text = String.format("%s ", text);
                    for (int i = 0; i < this.random.getValue(); i++) {
                        text = String.format("%s%s", text, (char) this.charOffset);
                        this.charOffset++;
                        if (this.charOffset > 40959) {
                            this.charOffset = 19968;
                        }
                    }
                }
                mc.thePlayer.sendChatMessage(text);
            }
        }
    }
}
