package net.paulem.launchermc.utils;

public class MathUtils {
    /**
     * Linearly interpolate a color channel (0.0–1.0 Color API) to 0–255 int.
     */
    public static int lerp(double from, double to, double t) {
        return (int) ((from + t * (to - from)) * 255);
    }
}
