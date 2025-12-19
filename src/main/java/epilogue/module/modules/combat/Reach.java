package epilogue.module.modules.combat;

import epilogue.event.EventTarget;
import epilogue.event.types.EventType;
import epilogue.events.PickEvent;
import epilogue.events.RaytraceEvent;
import epilogue.events.TickEvent;
import epilogue.module.Module;
import epilogue.value.values.FloatValue;
import epilogue.value.values.PercentValue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public class Reach extends Module {
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private final Random theRandom = new Random();
    private boolean expanding = true;
    public final FloatValue range = new FloatValue("Range", 3.1F, 3.0F, 6.0F);
    public final PercentValue chance = new PercentValue("Chance", 100);

    public Reach() {
        super("Reach", false);
    }

    @EventTarget
    public void onPick(PickEvent event) {
        if (this.isEnabled() && this.expanding) {
            event.setRange(this.range.getValue().doubleValue());
        }
    }

    @EventTarget
    public void onRaytrace(RaytraceEvent event) {
        if (this.isEnabled() && this.expanding) {
            event.setRange(Math.max(event.getRange(), this.range.getValue().doubleValue() + 0.5));
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            this.expanding = this.theRandom.nextDouble() <= (double) this.chance.getValue() / 100.0;
        }
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.range.getValue())};
    }
}
