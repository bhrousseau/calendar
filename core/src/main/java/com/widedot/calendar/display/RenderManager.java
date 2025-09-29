package com.widedot.calendar.display;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Gestionnaire centralisé pour les opérations de rendu communes
 */
public class RenderManager {
    
    /**
     * Effectue les opérations standard de début de rendu
     * @param viewport Le viewport à appliquer
     * @param camera La caméra à mettre à jour
     * @param batch Le SpriteBatch à configurer
     * @param clearColor Couleur de fond (null pour noir par défaut)
     */
    public static void beginStandardRender(Viewport viewport, OrthographicCamera camera, SpriteBatch batch, float[] clearColor) {
        // Effacer l'écran
        if (clearColor != null && clearColor.length >= 3) {
            float alpha = clearColor.length > 3 ? clearColor[3] : 1.0f;
            Gdx.gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], alpha);
        } else {
            Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Noir par défaut
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Mettre à jour la caméra
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }
    
    /**
     * Effectue les opérations standard de début de rendu avec fond noir
     * @param viewport Le viewport à appliquer
     * @param camera La caméra à mettre à jour
     * @param batch Le SpriteBatch à configurer
     */
    public static void beginStandardRender(Viewport viewport, OrthographicCamera camera, SpriteBatch batch) {
        beginStandardRender(viewport, camera, batch, null);
    }
    
    /**
     * Dessine un fond plein écran
     * @param batch Le SpriteBatch (doit être en cours d'utilisation)
     * @param texture La texture à utiliser (généralement whiteTexture)
     * @param viewport Le viewport pour les dimensions
     * @param r Composante rouge (0-1)
     * @param g Composante verte (0-1)
     * @param b Composante bleue (0-1)
     * @param a Composante alpha (0-1)
     */
    public static void drawFullScreenBackground(SpriteBatch batch, Texture texture, Viewport viewport, 
                                                float r, float g, float b, float a) {
        batch.setColor(r, g, b, a);
        batch.draw(texture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        batch.setColor(1, 1, 1, 1); // Remettre couleur par défaut
    }
    
    /**
     * Dessine un fond plein écran noir
     * @param batch Le SpriteBatch (doit être en cours d'utilisation)
     * @param texture La texture à utiliser (généralement whiteTexture)
     * @param viewport Le viewport pour les dimensions
     */
    public static void drawBlackBackground(SpriteBatch batch, Texture texture, Viewport viewport) {
        drawFullScreenBackground(batch, texture, viewport, 0, 0, 0, 1);
    }
    
    /**
     * Dessine un overlay semi-transparent plein écran
     * @param batch Le SpriteBatch (doit être en cours d'utilisation)
     * @param texture La texture à utiliser (généralement whiteTexture)
     * @param viewport Le viewport pour les dimensions
     * @param r Composante rouge (0-1)
     * @param g Composante verte (0-1)
     * @param b Composante bleue (0-1)
     * @param alpha Transparence (0-1)
     */
    public static void drawOverlay(SpriteBatch batch, Texture texture, Viewport viewport, 
                                   float r, float g, float b, float alpha) {
        drawFullScreenBackground(batch, texture, viewport, r, g, b, alpha);
    }
    
    /**
     * Dessine un overlay noir semi-transparent
     * @param batch Le SpriteBatch (doit être en cours d'utilisation)
     * @param texture La texture à utiliser (généralement whiteTexture)
     * @param viewport Le viewport pour les dimensions
     * @param alpha Transparence (0-1)
     */
    public static void drawBlackOverlay(SpriteBatch batch, Texture texture, Viewport viewport, float alpha) {
        drawOverlay(batch, texture, viewport, 0, 0, 0, alpha);
    }
    
    /**
     * Calcule les dimensions pour centrer une image tout en conservant son ratio
     * @param imageWidth Largeur originale de l'image
     * @param imageHeight Hauteur originale de l'image
     * @param maxWidth Largeur maximale disponible
     * @param maxHeight Hauteur maximale disponible
     * @return Tableau [x, y, width, height] pour le positionnement centré
     */
    public static float[] calculateCenteredImageBounds(float imageWidth, float imageHeight, 
                                                       float maxWidth, float maxHeight) {
        float aspectRatio = imageWidth / imageHeight;
        
        float finalWidth, finalHeight;
        
        // Calculer les dimensions pour s'adapter dans l'espace disponible
        if (maxWidth / aspectRatio <= maxHeight) {
            // L'image sera limitée par la largeur
            finalWidth = maxWidth;
            finalHeight = finalWidth / aspectRatio;
        } else {
            // L'image sera limitée par la hauteur
            finalHeight = maxHeight;
            finalWidth = finalHeight * aspectRatio;
        }
        
        // Centrer
        float x = (maxWidth - finalWidth) / 2f;
        float y = (maxHeight - finalHeight) / 2f;
        
        return new float[]{x, y, finalWidth, finalHeight};
    }
    
    /**
     * Gère le rendu standard d'un écran de jeu
     * @param viewport Le viewport
     * @param camera La caméra
     * @param batch Le SpriteBatch
     * @param gameRenderer Interface pour le rendu spécifique du jeu
     * @param inputHandler Interface pour la gestion des entrées
     * @param delta Temps écoulé
     */
    public static void renderGameScreen(Viewport viewport, OrthographicCamera camera, SpriteBatch batch,
                                        GameRenderer gameRenderer, InputHandler inputHandler, float delta) {
        beginStandardRender(viewport, camera, batch);
        
        // Mettre à jour le jeu
        gameRenderer.updateGame(delta);
        
        // Dessiner le jeu
        batch.begin();
        gameRenderer.renderGame();
        batch.end();
        
        // Gérer les raccourcis globaux (plein écran)
        InputManager.handleGlobalInput();
        
        // Gérer les entrées utilisateur
        inputHandler.handleInput();
    }
    
    /**
     * Interface pour le rendu spécifique du jeu
     */
    public interface GameRenderer {
        void updateGame(float delta);
        void renderGame();
    }
    
    /**
     * Interface pour la gestion des entrées
     */
    public interface InputHandler {
        void handleInput();
    }
}
