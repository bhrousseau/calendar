package com.widedot.calendar;

import com.widedot.calendar.platform.PlatformRegistry;

/**
 * Bridge pour mesurer les insets du clavier iOS via visualViewport
 */
public final class IosInsetsBridge {
    
    public static native void install() /*-{
        $wnd.gdxKeyboardInsetsChanged = function(inset) {
            @com.widedot.calendar.platform.PlatformRegistry::get()().@com.widedot.calendar.platform.PlatformInfo::onKeyboardInsetsChanged(I)(inset);
        };
    }-*/;
}