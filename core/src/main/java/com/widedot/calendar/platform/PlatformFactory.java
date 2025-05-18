package com.widedot.calendar.platform;

public class PlatformFactory {
    private static PlatformSpecific platform;

    public static PlatformSpecific getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("PlatformSpecific implementation not set. Please inject from backend.");
        }
        return platform;
    }

    public static void setPlatform(PlatformSpecific platformImpl) {
        if (platform != null) {
            platform.dispose();
        }
        platform = platformImpl;
        if (platform != null) {
            platform.initialize();
        }
    }

    public static void dispose() {
        if (platform != null) {
            platform.dispose();
            platform = null;
        }
    }
} 