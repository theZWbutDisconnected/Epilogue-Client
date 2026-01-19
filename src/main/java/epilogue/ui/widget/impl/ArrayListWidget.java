package epilogue.ui.widget.impl;

import epilogue.Epilogue;
import epilogue.ui.widget.Widget;
import epilogue.ui.widget.WidgetAlign;
import epilogue.module.Module;
import epilogue.module.modules.render.ArrayList;
import net.minecraft.client.gui.ScaledResolution;

public class ArrayListWidget extends Widget {
    public ArrayListWidget() {
        super("ArrayList", WidgetAlign.RIGHT | WidgetAlign.TOP);
        this.x = 0.98f;
        this.y = 0.02f;
        this.width = 110f;
        this.height = 140f;
    }

    @Override
    public boolean shouldRender() {
        Module m = Epilogue.moduleManager.getModule("Arraylist");
        return m != null && m.isEnabled();
    }

    @Override
    public void render(float partialTicks) {
        Module m = Epilogue.moduleManager.getModule("Arraylist");
        if (!(m instanceof ArrayList)) return;
        ArrayList arrayList = (ArrayList) m;

        ScaledResolution sr = new ScaledResolution(mc);
        float screenMid = sr.getScaledWidth() / 2.0f;
        float centerX = renderX + width / 2.0f;
        boolean renderRight = centerX >= screenMid;

        float anchorTopY = renderY;
        float anchorX = renderRight ? (renderX + width) : renderX;

        if (arrayList.mode.getModeString().equals("HotKey")) {
            anchorX = renderX;
        }

        arrayList.renderAt(anchorX, anchorTopY);

        this.width = arrayList.getLastWidth();
        this.height = arrayList.getLastHeight();

        if (renderRight && !arrayList.mode.getModeString().equals("HotKey")) {
            this.renderX = anchorX - this.width;
        } else {
            this.renderX = anchorX;
        }
    }
}
