package epilogue.module.modules.render;

import epilogue.module.Module;
import epilogue.value.values.ModeValue;
import epilogue.value.values.FloatValue;

public class Animations extends Module {
    public static Animations INSTANCE;

    public final ModeValue swordMode = new ModeValue("Sword", 1, new String[]{"Old", "Push", "Dash", "Slash", "Slide", "Scale", "Swank", "Swang", "Swonk", "Edit", "Rhys", "Stab", "Float", "Remix", "Avatar", "Xiv", "Yamato", "SlideSwing", "SmallPush", "Invent", "Leaked", "Aqua", "Astro", "Fadeaway", "Astolfo", "AstolfoSpin", "Moon", "MoonPush", "Smooth", "Tap1", "Tap2", "Sigma1", "Sigma2"});
    public final FloatValue blockPosX = new FloatValue("BlockPos X", 0f, -1f, 1f);
    public final FloatValue blockPosY = new FloatValue("BlockPos Y", 0f, -1f, 1f);
    public final FloatValue blockPosZ = new FloatValue("BlockPos Z", 0f, -1f, 1f);
    public final FloatValue scale = new FloatValue("Item Size", 0f, -0.5f, 0.5f);

    public Animations() {
        super("Animations", true);
        INSTANCE = this;
    }

    public static Animations getInstance() {
        return INSTANCE;
    }
}