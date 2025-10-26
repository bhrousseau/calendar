package com.widedot.calendar.platform;

/**
 * Registre centralisé pour l'accès à PlatformInfo
 * Singleton thread-safe pour la détection de plateforme
 */
public final class PlatformRegistry {
    private static PlatformInfo INSTANCE;
    
    /**
     * Définit l'implémentation de plateforme
     * @param info L'implémentation PlatformInfo
     */
    public static void set(PlatformInfo info) {
        INSTANCE = info;
    }
    
    /**
     * @return L'implémentation PlatformInfo actuelle
     */
    public static PlatformInfo get() {
        return INSTANCE;
    }
    
    /**
     * @return true si la plateforme actuelle est un navigateur mobile
     */
    public static boolean isMobileBrowser() {
        return INSTANCE != null && INSTANCE.isMobileBrowser();
    }
}
