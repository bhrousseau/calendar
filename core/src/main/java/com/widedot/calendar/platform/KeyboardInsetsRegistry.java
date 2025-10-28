package com.widedot.calendar.platform;

import com.widedot.calendar.ui.BottomInputBar;

/**
 * Registre global pour transmettre les insets du clavier au BottomInputBar actif
 */
public final class KeyboardInsetsRegistry {
    private static BottomInputBar activeInputBar;
    
    /**
     * Enregistre le BottomInputBar actif qui recevra les notifications d'insets
     */
    public static void register(BottomInputBar inputBar) {
        activeInputBar = inputBar;
    }
    
    /**
     * Désenregistre le BottomInputBar actif
     */
    public static void unregister(BottomInputBar inputBar) {
        if (activeInputBar == inputBar) {
            activeInputBar = null;
        }
    }
    
    /**
     * Notifie le BottomInputBar actif d'un changement d'insets clavier
     */
    public static void notifyInsetsChanged(int bottomInsetPx) {
        if (activeInputBar != null) {
            activeInputBar.setKeyboardInsetPx(bottomInsetPx);
        }
    }
    
    /**
     * Obtient le BottomInputBar actif
     */
    public static BottomInputBar getActive() {
        return activeInputBar;
    }
}

