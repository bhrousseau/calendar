package com.widedot.calendar.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Gestionnaire centralisé pour les raccourcis globaux d'affichage
 */
public class InputManager {
    
    /**
     * Gère les raccourcis globaux d'affichage (plein écran, etc.)
     * À appeler dans la boucle de rendu de chaque écran
     * 
     * Raccourcis disponibles :
     * - F11 : Basculer plein écran / fenêtré
     * - Alt+Enter : Basculer plein écran / fenêtré (raccourci alternatif)
     */
    public static void handleGlobalInput() {
        // F11 pour basculer en plein écran
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            WindowManager.toggleFullscreen();
        }
        
        // Alt+Enter pour basculer en plein écran (raccourci standard)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && 
            (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || 
             Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
            WindowManager.toggleFullscreen();
        }
    }
}
