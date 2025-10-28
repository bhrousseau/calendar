package com.widedot.calendar;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Bridge pour gérer le clavier natif iOS/Android via un input HTML invisible
 * Nécessaire car Safari iOS n'ouvre le clavier que sur un vrai input focusé
 */
public final class IosKeyboardBridge {
    private static InputElement input;
    private static boolean mounted;

    public static void show(boolean visible, String placeholder) {
        if (visible) {
            ensureMounted(placeholder);
        } else {
            hide();
        }
    }

    private static void ensureMounted(String placeholder) {
        if (mounted) {
            input.setAttribute("placeholder", placeholder != null ? placeholder : "");
            input.focus();
            return;
        }
        
        input = Document.get().createTextInputElement();
        input.setId("gdx-soft-input");
        input.setAttribute("placeholder", placeholder != null ? placeholder : "");
        
        Style s = input.getStyle();
        s.setPosition(Style.Position.FIXED);
        s.setBottom(0, Style.Unit.PX);
        s.setLeft(0, Style.Unit.PX);
        s.setWidth(1, Style.Unit.PX);
        s.setHeight(32, Style.Unit.PX);
        s.setOpacity(0.01);
        s.setZIndex(2147483647);
        s.setProperty("pointerEvents", "auto");
        s.setProperty("fontSize", "16px");
        s.setProperty("color", "transparent");
        s.setProperty("background", "transparent");
        s.setProperty("border", "none");
        s.setProperty("outline", "none");

        input.setAttribute("autocapitalize", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("spellcheck", "false");
        input.setAttribute("autocomplete", "off");
        input.setAttribute("inputmode", "text");
        input.setAttribute("enterkeyhint", "done");

        RootPanel.get().getElement().appendChild(input);
        input.focus();
        forceFocusDelayed(input);
        
        mounted = true;
    }

    public static void hide() {
        if (!mounted) return;
        input.blur();
        input.removeFromParent();
        mounted = false;
        input = null;
    }

    public static boolean isMounted() { 
        return mounted; 
    }

    public static void focus() { 
        if (input != null) input.focus(); 
    }
    
    private static native void forceFocusDelayed(InputElement input) /*-{
        setTimeout(function() { input.focus(); }, 50);
        setTimeout(function() { input.focus(); }, 100);
    }-*/;
}

