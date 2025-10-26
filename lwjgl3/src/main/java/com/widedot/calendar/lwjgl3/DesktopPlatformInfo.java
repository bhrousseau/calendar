package com.widedot.calendar.lwjgl3;

import com.widedot.calendar.platform.PlatformInfo;

/**
 * Implémentation Desktop de PlatformInfo
 * Desktop n'est jamais considéré comme mobile browser
 */
public class DesktopPlatformInfo implements PlatformInfo {
    @Override
    public boolean isMobileBrowser() {
        return false; // Desktop n'est jamais mobile
    }
    
    @Override
    public void onVirtualKeyboardRequest(boolean visible) {
        // Pas de clavier virtuel sur desktop
    }
}
