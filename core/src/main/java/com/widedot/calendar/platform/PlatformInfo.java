package com.widedot.calendar.platform;

/**
 * Interface pour la détection de plateforme
 * Permet de différencier mobile browser vs desktop
 */
public interface PlatformInfo {
    /**
     * @return true seulement pour GWT mobile (Android/iOS)
     */
    boolean isMobileBrowser();
    
    /**
     * Hint pour la gestion du clavier virtuel
     * @param visible true si le clavier virtuel doit être affiché
     */
    void onVirtualKeyboardRequest(boolean visible);
    
    /**
     * Vide l'input natif (pour la synchronisation avec le clavier iOS)
     */
    default void clearNativeInput() {
        // Implémentation par défaut vide pour les plateformes non-mobiles
    }
}
