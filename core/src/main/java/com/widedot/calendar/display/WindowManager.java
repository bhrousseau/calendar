package com.widedot.calendar.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;

/**
 * Gestionnaire centralisé pour la gestion des fenêtres et du plein écran
 */
public class WindowManager {
    
    /**
     * Bascule entre mode plein écran et mode fenêtré
     */
    public static void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            switchToWindowed();
        } else {
            switchToFullscreen();
        }
    }
    
    /**
     * Passe en mode plein écran
     */
    public static void switchToFullscreen() {
        DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        Gdx.graphics.setFullscreenMode(displayMode);
        System.out.println("Basculé en plein écran (" + 
                         displayMode.width + "x" + displayMode.height + ")");
    }
    
    /**
     * Passe en mode fenêtré avec une taille optimale
     */
    public static void switchToWindowed() {
        WindowSize optimalSize = calculateOptimalWindowSize();
        Gdx.graphics.setWindowedMode(optimalSize.width, optimalSize.height);
        System.out.println("Basculé en mode fenêtré (" + 
                         optimalSize.width + "x" + optimalSize.height + 
                         ") - ratio 16:10, " + optimalSize.method);
    }
    
    /**
     * Calcule la taille optimale de fenêtre basée sur l'écran
     * @return La taille optimale avec la méthode utilisée
     */
    public static WindowSize calculateOptimalWindowSize() {
        DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        int screenWidth = displayMode.width;
        int screenHeight = displayMode.height;
        
        // Option 1: Basé sur 50% de la hauteur d'écran avec ratio 16:10
        int heightBasedWindowHeight = screenHeight / 2;
        int heightBasedWindowWidth = Math.round(heightBasedWindowHeight * 16f / 10f);
        
        // Option 2: Basé sur la largeur maximale de l'écran avec ratio 16:10
        int widthBasedWindowWidth = screenWidth;
        int widthBasedWindowHeight = Math.round(widthBasedWindowWidth * 10f / 16f);
        
        // Choisir la plus petite des deux options
        if (heightBasedWindowWidth <= screenWidth) {
            // La fenêtre basée sur la hauteur rentre dans l'écran
            return new WindowSize(
                heightBasedWindowWidth, 
                heightBasedWindowHeight, 
                "50% hauteur écran"
            );
        } else {
            // La fenêtre basée sur la hauteur est trop large, utiliser la largeur
            return new WindowSize(
                widthBasedWindowWidth, 
                widthBasedWindowHeight, 
                "largeur écran max"
            );
        }
    }
    
    /**
     * Classe pour encapsuler une taille de fenêtre avec sa méthode de calcul
     */
    public static class WindowSize {
        public final int width;
        public final int height;
        public final String method;
        
        public WindowSize(int width, int height, String method) {
            this.width = width;
            this.height = height;
            this.method = method;
        }
        
        public float getRatio() {
            return (float) width / height;
        }
    }
}
