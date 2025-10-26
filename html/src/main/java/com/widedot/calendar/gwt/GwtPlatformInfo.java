package com.widedot.calendar.gwt;

import com.widedot.calendar.platform.PlatformInfo;

/**
 * Implémentation GWT de PlatformInfo
 * Détecte les navigateurs mobiles via user-agent
 */
public class GwtPlatformInfo implements PlatformInfo {
    
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
        // Optionnel : forcer focus/blur de l'input HTML interne si besoin
        // Pour l'instant, la gestion est automatique via LibGDX
    }
    
    /**
     * Obtient le user-agent du navigateur
     * @return Le user-agent
     */
    private native String getUserAgent() /*-{
        return $wnd.navigator.userAgent || "";
    }-*/;
}
