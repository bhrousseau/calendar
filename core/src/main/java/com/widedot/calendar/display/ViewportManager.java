package com.widedot.calendar.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/**
 * Gestionnaire centralisé pour les viewports
 */
public class ViewportManager {
    
    /**
     * Crée un viewport adapté au contexte actuel (fullscreen/windowed)
     * @param camera La caméra à utiliser
     * @return Le viewport configuré
     */
    public static Viewport createViewport(OrthographicCamera camera) {
        return createViewport(camera, Gdx.graphics.isFullscreen());
    }
    
    /**
     * Crée un viewport adapté au mode spécifié
     * @param camera La caméra à utiliser
     * @param isFullscreen Si on est en mode plein écran
     * @return Le viewport configuré
     */
    public static Viewport createViewport(OrthographicCamera camera, boolean isFullscreen) {
        float screenRatio = (float)Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
        
        System.out.println("ViewportManager: Creating viewport - Mode: " + (isFullscreen ? "Fullscreen" : "Windowed") + 
                         ", Screen: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight() + 
                         ", Ratio: " + String.format("%.3f", screenRatio));
        
        Viewport viewport;
        if (isFullscreen) {
            viewport = createFullscreenViewport(camera, screenRatio);
        } else {
            viewport = createWindowedViewport(camera, screenRatio);
        }
        
        System.out.println("ViewportManager: Created " + viewport.getClass().getSimpleName() + 
                         " - World: " + DisplayConfig.WORLD_WIDTH + "x" + viewport.getWorldHeight());
        
        return viewport;
    }
    
    /**
     * Crée un viewport pour le mode plein écran
     * @param camera La caméra à utiliser
     * @param screenRatio Le ratio d'écran actuel
     * @return Le viewport configuré
     */
    private static Viewport createFullscreenViewport(OrthographicCamera camera, float screenRatio) {
        if (screenRatio <= DisplayConfig.RATIO_16_10) {
            // Écrans 4:3 jusqu'à 16:10 : forcer 16:10 avec bandes noires
            System.out.println("ViewportManager: Fullscreen <= 16:10 - FitViewport avec bandes noires");
            return new FitViewport(DisplayConfig.WORLD_WIDTH, DisplayConfig.REFERENCE_HEIGHT, camera);
        } else if (screenRatio <= DisplayConfig.RATIO_21_9) {
            // 16:9 à 21:9 : utiliser ExtendViewport pour cropper en haut/bas (pas de bandes noires)
            System.out.println("ViewportManager: Fullscreen 16:10 < ratio <= 21:9 - ExtendViewport crop haut/bas");
            return new ExtendViewport(
                DisplayConfig.WORLD_WIDTH,          // minWidth (fixe)
                DisplayConfig.HEIGHT_21_9,          // minHeight (878 pour 21:9)
                DisplayConfig.WORLD_WIDTH,          // maxWidth (fixe)
                DisplayConfig.REFERENCE_HEIGHT,     // maxHeight (1280 pour 16:10)
                camera
            );
        } else {
            // Au-delà de 21:9 : limiter à 21:9 avec bandes noires verticales
            System.out.println("ViewportManager: Fullscreen > 21:9 - FitViewport avec bandes noires verticales");
            return new FitViewport(DisplayConfig.WORLD_WIDTH, DisplayConfig.HEIGHT_21_9, camera);
        }
    }
    
    /**
     * Crée un viewport pour le mode fenêtré
     * @param camera La caméra à utiliser
     * @param screenRatio Le ratio d'écran actuel
     * @return Le viewport configuré
     */
    private static Viewport createWindowedViewport(OrthographicCamera camera, float screenRatio) {
        // En mode fenêtré, la fenêtre est forcée en 16:10, donc utiliser FitViewport 16:10 sans bandes noires
        System.out.println("ViewportManager: Windowed - FitViewport 16:10 parfait");
        return new FitViewport(DisplayConfig.WORLD_WIDTH, DisplayConfig.REFERENCE_HEIGHT, camera);
    }
    
    /**
     * Met à jour un viewport existant lors du redimensionnement
     * @param viewport Le viewport à mettre à jour
     * @param width Nouvelle largeur
     * @param height Nouvelle hauteur
     */
    public static void updateViewport(Viewport viewport, int width, int height) {
        viewport.update(width, height, true);
    }
    
    /**
     * Met à jour un viewport avec reconfiguration automatique selon le mode
     * @param currentViewport Le viewport actuel (sera remplacé si nécessaire)
     * @param width Nouvelle largeur
     * @param height Nouvelle hauteur
     * @return Le viewport à utiliser (peut être le même ou un nouveau)
     */
    public static Viewport updateViewportWithReconfiguration(Viewport currentViewport, int width, int height) {
        boolean isCurrentlyFullscreen = Gdx.graphics.isFullscreen();
        boolean needsReconfiguration = shouldReconfigureViewport(currentViewport, isCurrentlyFullscreen);
        
        if (needsReconfiguration) {
            System.out.println("ViewportManager: Reconfiguration détectée - mode " + 
                             (isCurrentlyFullscreen ? "fullscreen" : "fenêtré"));
            
            // Créer un nouveau viewport adapté au mode actuel
            OrthographicCamera camera = (OrthographicCamera) currentViewport.getCamera();
            Viewport newViewport = createViewport(camera, isCurrentlyFullscreen);
            
            // Conserver la position de la caméra
            newViewport.getCamera().position.set(camera.position);
            newViewport.update(width, height, true);
            
            return newViewport;
        } else {
            currentViewport.update(width, height, true);
            return currentViewport;
        }
    }
    
    /**
     * Reconfigure un viewport en cas de changement de mode (fullscreen/windowed)
     * @param oldViewport L'ancien viewport
     * @param isFullscreen Le nouveau mode
     * @return Le nouveau viewport configuré
     */
    public static Viewport reconfigureViewport(Viewport oldViewport, boolean isFullscreen) {
        OrthographicCamera camera = (OrthographicCamera) oldViewport.getCamera();
        Viewport newViewport = createViewport(camera, isFullscreen);
        
        // Conserver la position de la caméra si possible
        newViewport.getCamera().position.set(camera.position);
        newViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        
        return newViewport;
    }
    
    /**
     * Détermine si un viewport doit être reconfiguré selon le mode actuel
     * @param viewport Le viewport actuel
     * @param isFullscreen Le mode actuel
     * @return true si une reconfiguration est nécessaire
     */
    private static boolean shouldReconfigureViewport(Viewport viewport, boolean isFullscreen) {
        // Détection basée sur le type de viewport et le mode
        if (isFullscreen) {
            // En fullscreen, on devrait avoir ExtendViewport ou FitViewport selon le ratio
            float screenRatio = (float) Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
            boolean shouldBeExtend = (screenRatio > DisplayConfig.RATIO_16_10 && screenRatio <= DisplayConfig.RATIO_21_9);
            
            if (shouldBeExtend) {
                return !(viewport instanceof com.badlogic.gdx.utils.viewport.ExtendViewport);
            } else {
                return !(viewport instanceof com.badlogic.gdx.utils.viewport.FitViewport);
            }
        } else {
            // En fenêtré, on devrait toujours avoir FitViewport 16:10
            return !(viewport instanceof com.badlogic.gdx.utils.viewport.FitViewport);
        }
    }
    
}
