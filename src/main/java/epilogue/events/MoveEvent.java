package epilogue.events;

public class MoveEvent {
    private float forward;
    private float strafe;
    private boolean jump;
    private boolean sneak;
    private double sneakSlowDownMultiplier;

    public MoveEvent(float forward, float strafe,
                     boolean jump, boolean sneak,
                     double sneakSlowDownMultiplier) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
    }

    public float getForward() { return forward; }
    public float getStrafe() { return strafe; }
    public boolean isJump() { return jump; }
    public boolean isSneak() { return sneak; }
    public double getSneakSlowDownMultiplier() { return sneakSlowDownMultiplier; }

    public void setForward(float forward) { this.forward = forward; }
    public void setStrafe(float strafe) { this.strafe = strafe; }
    public void setJump(boolean jump) { this.jump = jump; }
    public void setSneak(boolean sneak) { this.sneak = sneak; }
    public void setSneakSlowDownMultiplier(double multiplier) {
        this.sneakSlowDownMultiplier = multiplier;
    }
}