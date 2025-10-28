package com.widedot.calendar.gwt;

import com.badlogic.gdx.Gdx;
import com.widedot.calendar.IosKeyboardBridge;
import com.widedot.calendar.platform.PlatformInfo;
import com.widedot.calendar.platform.KeyboardInsetsRegistry;

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
        if (isMobileBrowser()) {
            IosKeyboardBridge.show(visible, "Tapez votre réponse...");
        }
    }
    
    @Override
    public void onKeyboardInsetsChanged(int bottomInsetPx) {
        KeyboardInsetsRegistry.notifyInsetsChanged(bottomInsetPx);
    }
    
    private native String getUserAgent() /*-{
        return $wnd.navigator.userAgent || "";
    }-*/;
}
