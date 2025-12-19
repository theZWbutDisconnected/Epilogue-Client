package epilogue.ui.mainmenu;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.util.Random;
//Rain Particle
public final class Particle {
    //嘻嘻这个注释是ai写的。好玩捏。
    public static final String MAX_PARTICLES_DESC = "这个是屏幕上最多同时存在的雨滴数量上限";
    public static final int MAX_PARTICLES = 220;

    public static final String SPAWN_RATE_PER_SEC_DESC = "这个是每秒生成雨滴的数量（密度）";
    public static final float SPAWN_RATE_PER_SEC = 160.0f;

    public static final String SPEED_MIN_DESC = "这个是雨滴最小移动速度（像素/秒）";
    public static final float SPEED_MIN = 420.0f;

    public static final String SPEED_MAX_DESC = "这个是雨滴最大移动速度（像素/秒）";
    public static final float SPEED_MAX = 980.0f;

    public static final String LENGTH_MIN_DESC = "这个是雨滴线段最小长度（像素）";
    public static final float LENGTH_MIN = 16.0f;

    public static final String LENGTH_MAX_DESC = "这个是雨滴线段最大长度（像素）";
    public static final float LENGTH_MAX = 44.0f;

    public static final String THICKNESS_MIN_DESC = "这个是雨滴最小线宽（像素）";
    public static final float THICKNESS_MIN = 1.2f;

    public static final String THICKNESS_MAX_DESC = "这个是雨滴最大线宽（像素）";
    public static final float THICKNESS_MAX = 2.6f;

    public static final String ALPHA_MIN_DESC = "这个是雨滴最小透明度（可见度）";
    public static final float ALPHA_MIN = 0.22f;

    public static final String ALPHA_MAX_DESC = "这个是雨滴最大透明度（可见度）";
    public static final float ALPHA_MAX = 0.60f;

    public static final String DRIFT_PX_MIN_DESC = "这个是雨滴漂移摆动的最小幅度（像素）";
    public static final float DRIFT_PX_MIN = 0.3f;

    public static final String DRIFT_PX_MAX_DESC = "这个是雨滴漂移摆动的最大幅度（像素）";
    public static final float DRIFT_PX_MAX = 1.8f;

    public static final String DRIFT_HZ_MIN_DESC = "这个是雨滴漂移摆动的最小频率（赫兹）";
    public static final float DRIFT_HZ_MIN = 0.35f;

    public static final String DRIFT_HZ_MAX_DESC = "这个是雨滴漂移摆动的最大频率（赫兹）";
    public static final float DRIFT_HZ_MAX = 1.15f;

    public static final String JITTER_DESC = "这个是随机参数抖动强度（让雨滴更不规律）";
    public static final float JITTER = 0.10f;

    public static final String EDGE_PAD_DESC = "这个是屏幕边界外的生成/回收缓冲距离（像素）";
    public static final float EDGE_PAD = 40.0f;

    private final Random random = new Random();

    private final Drop[] drops = new Drop[MAX_PARTICLES];
    private int live;
    private float spawnAccumulator;

    private float time;

    private long lastNanos;

    public Particle() {
        for (int i = 0; i < drops.length; i++) {
            drops[i] = new Drop();
        }
        lastNanos = System.nanoTime();
    }

    public void update(ScaledResolution sr) {
        float dt = consumeDtSeconds();
        float maxDt = 1.0f / 20.0f;
        if (dt > maxDt) dt = maxDt;

        time += dt;

        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        float vx = -(float) Math.cos(Math.toRadians(45.0));
        float vy = (float) Math.sin(Math.toRadians(45.0));
        float px = -vy;
        float py = vx;

        for (int i = 0; i < live; i++) {
            Drop d = drops[i];

            float phase = (float) (time * d.driftHz * (Math.PI * 2.0) + d.driftPhase);
            float drift = (float) Math.sin(phase) * d.driftPx;

            d.x += d.speed * vx * dt + px * drift;
            d.y += d.speed * vy * dt + py * drift;

            if (d.x < -EDGE_PAD || d.y > h + EDGE_PAD) {
                int last = live - 1;
                drops[i] = drops[last];
                drops[last] = d;
                live--;
                i--;
            }
        }

        spawnAccumulator += SPAWN_RATE_PER_SEC * dt;
        int toSpawn = (int) spawnAccumulator;
        spawnAccumulator -= toSpawn;

        for (int i = 0; i < toSpawn && live < MAX_PARTICLES; i++) {
            spawn(sr);
        }
    }

    public void render(ScaledResolution sr) {
        if (live <= 0) return;

        float vx = -(float) Math.cos(Math.toRadians(45.0));
        float vy = (float) Math.sin(Math.toRadians(45.0));

        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < live; i++) {
            Drop d = drops[i];

            float x0 = d.x;
            float y0 = d.y;
            float x1 = d.x + vx * d.length;
            float y1 = d.y + vy * d.length;

            GL11.glLineWidth(d.thickness);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, d.alpha);
            GL11.glVertex2f(x0, y0);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, d.alpha * 0.35f);
            GL11.glVertex2f(x1, y1);
        }
        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        GlStateManager.enableTexture2D();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GlStateManager.enableDepth();
    }

    private void spawn(ScaledResolution sr) {
        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        Drop d = drops[live++];

        float t = random.nextFloat();
        float startX;
        float startY;
        if (random.nextBoolean()) {
            startX = w + EDGE_PAD;
            startY = -EDGE_PAD + t * (h + EDGE_PAD);
        } else {
            startX = -EDGE_PAD + t * (w + EDGE_PAD);
            startY = -EDGE_PAD;
        }

        d.x = startX;
        d.y = startY;

        d.speed = lerp(SPEED_MIN, SPEED_MAX, jittered());
        d.length = lerp(LENGTH_MIN, LENGTH_MAX, jittered());
        d.thickness = lerp(THICKNESS_MIN, THICKNESS_MAX, jittered());
        d.alpha = lerp(ALPHA_MIN, ALPHA_MAX, jittered());
        d.driftPx = lerp(DRIFT_PX_MIN, DRIFT_PX_MAX, jittered());
        d.driftHz = lerp(DRIFT_HZ_MIN, DRIFT_HZ_MAX, jittered());
        d.driftPhase = random.nextFloat() * (float) (Math.PI * 2.0);
    }

    private float jittered() {
        float r = random.nextFloat();
        float j = (random.nextFloat() - 0.5f) * 2.0f * JITTER;
        r += j;
        if (r < 0.0f) r = 0.0f;
        if (r > 1.0f) r = 1.0f;
        return r;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float consumeDtSeconds() {
        long now = System.nanoTime();
        long prev = lastNanos;
        lastNanos = now;
        return (now - prev) / 1_000_000_000.0f;
    }

    private static final class Drop {
        float x;
        float y;
        float speed;
        float length;
        float thickness;
        float alpha;
        float driftPx;
        float driftHz;
        float driftPhase;
    }
}
