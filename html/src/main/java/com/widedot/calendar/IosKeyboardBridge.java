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
        
        // Le rendre "visible" pour iOS, mais quasi invisible à l'œil
        Style s = input.getStyle();
        s.setPosition(Style.Position.FIXED);
        s.setBottom(0, Style.Unit.PX);
        s.setLeft(0, Style.Unit.PX);
        
        // Important pour iOS/Android :
        s.setWidth(1, Style.Unit.PX);
        s.setHeight(1, Style.Unit.PX);
        s.setOpacity(0.01);            // pas 0
        s.setZIndex(2147483647);       // au-dessus du canvas
        s.setProperty("pointerEvents", "auto"); // peut recevoir le focus
        s.setProperty("fontSize", "16px");      // évite le zoom iOS et aide l'ouverture du clavier

        // Désactiver l'autocap/autocorrect pour un comportement console
        input.setAttribute("autocapitalize", "off");
        input.setAttribute("autocorrect", "off");
        input.setAttribute("spellcheck", "false");
        input.setAttribute("autocomplete", "off");   // Android : évite les suggestions
        input.setAttribute("inputmode", "text");     // iPadOS/Android : influence l'apparition du clavier
        input.setAttribute("enterkeyhint", "done");  // IME "Done" sur Android

        RootPanel.get().getElement().appendChild(input);

        input.focus(); // Crucial : appeler pendant le handler du tap
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

    public static void setValue(String v) {
        if (input != null) input.setValue(v);
    }

    public static void focus() { 
        if (input != null) input.focus(); 
    }

    public static void clear() {
        if (input != null) input.setValue("");
    }
}

