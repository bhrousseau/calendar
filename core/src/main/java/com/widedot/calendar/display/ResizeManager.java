package com.widedot.calendar.display;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Gestionnaire centralisé pour les opérations de redimensionnement
 */
public class ResizeManager {
    
    /**
     * Interface pour les objets qui peuvent être redimensionnés
     */
    public interface Resizable {
        void onResize(int width, int height, Viewport viewport);
    }
    
    /**
     * Gère le redimensionnement standard d'un écran de jeu
     * @param viewport Le viewport à mettre à jour
     * @param width Nouvelle largeur
     * @param height Nouvelle hauteur
     * @param resizables Objets à redimensionner
     */
    public static void handleGameScreenResize(Viewport viewport, int width, int height, Resizable... resizables) {
        System.out.println("Redimensionnement: " + width + "x" + height);
        
        // Mettre à jour le viewport
        ViewportManager.updateViewport(viewport, width, height);
        
        // Redimensionner tous les objets
        for (Resizable resizable : resizables) {
            resizable.onResize(width, height, viewport);
        }
        
        // Informations de debug
        System.out.println("Viewport: " + DisplayConfig.WORLD_WIDTH + "x" + viewport.getWorldHeight());
        System.out.println("Redimensionnement terminé");
    }
    
    /**
     * Calcule une échelle de police adaptative basée sur la taille d'écran
     * @param width Largeur d'écran
     * @param height Hauteur d'écran
     * @param baseScale Échelle de base
     * @param minScale Échelle minimale
     * @param maxScale Échelle maximale
     * @return Échelle calculée
     */
    public static float calculateAdaptiveFontScale(int width, int height, float baseScale, float minScale, float maxScale) {
        float screenDiagonal = (float) Math.sqrt(width * width + height * height);
        float scale = Math.max(minScale, Math.min(maxScale, baseScale * screenDiagonal / 1000f));
        return scale;
    }
    
    /**
     * Applique une échelle adaptative à une police
     * @param font La police à redimensionner
     * @param width Largeur d'écran
     * @param height Hauteur d'écran
     * @param baseScale Échelle de base
     * @param minScale Échelle minimale
     * @param maxScale Échelle maximale
     */
    public static void applyAdaptiveFontScale(BitmapFont font, int width, int height, 
                                              float baseScale, float minScale, float maxScale) {
        float scale = calculateAdaptiveFontScale(width, height, baseScale, minScale, maxScale);
        font.getData().setScale(scale);
    }
    
    /**
     * Classe utilitaire pour encapsuler les paramètres de redimensionnement de police
     */
    public static class FontScaleParams {
        public final float baseScale;
        public final float minScale;
        public final float maxScale;
        
        public FontScaleParams(float baseScale, float minScale, float maxScale) {
            this.baseScale = baseScale;
            this.minScale = minScale;
            this.maxScale = maxScale;
        }
        
        public static FontScaleParams standard() {
            return new FontScaleParams(1.0f, 0.8f, 1.5f);
        }
        
        public static FontScaleParams info() {
            return new FontScaleParams(1.0f, 0.8f, 1.2f);
        }
    }
}
