package com.widedot.calendar.gwt;

import com.badlogic.gdx.Gdx;
import com.widedot.calendar.IosKeyboardBridge;
import com.widedot.calendar.platform.PlatformInfo;

/**
 * Implémentation GWT de PlatformInfo
 * Détecte les navigateurs mobiles via user-agent et gère le clavier iOS
 */
public class GwtPlatformInfo implements PlatformInfo {
    private static GwtPlatformInfo currentInstance;
    
    public GwtPlatformInfo() {
        currentInstance = this;
    }
    
    public static GwtPlatformInfo getCurrentInstance() {
        return currentInstance;
    }
    
    @Override
    public boolean isMobileBrowser() {
        String userAgent = getUserAgent();
        return userAgent.contains("Android") || 
               userAgent.contains("iPhone") || 
               userAgent.contains("iPad") || 
               userAgent.contains("Mobile");
    }
    
    @Override
    public void onVirtualKeyboardRequest(boolean visible) {
        // Utiliser le bridge iOS pour gérer le clavier natif
        if (isMobileBrowser()) {
            IosKeyboardBridge.show(visible, "Tapez votre réponse...");
        }
    }
    
    /**
     * Appelé quand le texte change dans l'input natif
     * @param text Le nouveau texte
     */
    public void onNativeInputChanged(String text) {
        // Cette méthode sera appelée par IosKeyboardBridge
        // Le BottomInputBar écoutera ces changements via PlatformRegistry
        Gdx.app.log("GwtPlatformInfo", "Native input changed: " + text);
    }
    
    @Override
    public void clearNativeInput() {
        IosKeyboardBridge.clear();
    }
    
    @Override
    public void onKeyboardInsetsChanged(int bottomInsetPx) {
        // Cette méthode sera appelée par IosInsetsBridge
        // Le BottomInputBar écoutera ces changements via PlatformRegistry
        Gdx.app.log("GwtPlatformInfo", "Keyboard insets changed: " + bottomInsetPx + "px");
    }
    
    /**
     * Obtient le user-agent du navigateur
     * @return Le user-agent
     */
    private native String getUserAgent() /*-{
        return $wnd.navigator.userAgent || "";
    }-*/;
}
