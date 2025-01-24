package net.optifine.util;

public class NumUtils {
    public static float limit(float val, float min, float max) {
        if (val < min) {
            return min;
        } else {
            return Math.min(val, max);
        }
    }
    public static int limit(int val, int min, int max) {
        if (val < min) {
            return min;
        } else {
            return Math.min(val, max);
        }
    }

    public static int mod(int x, int y) {
        int result = x % y;
        if (result < 0) {
            result += y;
        }

        return result;
    }
}
