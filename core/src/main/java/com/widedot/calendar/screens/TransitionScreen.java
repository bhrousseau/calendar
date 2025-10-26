package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.widedot.calendar.display.ViewportManager;

/**
 * Écran de transition entre deux écrans avec effet de fondu
 */
public class TransitionScreen implements Screen {
    private final Game game;
    private final Screen sourceScreen; // Écran source (celui de départ)
    private final Screen targetScreen; // Écran cible (celui d'arrivée)
    private final SpriteBatch batch;
    private final OrthographicCamera camera;
    private Viewport viewport;
    private final Texture blackTexture;
    
    // Variable statique pour indiquer si une transition est en cours
    private static boolean isTransitionActive = false;
    
    private enum TransitionState {
        FADE_OUT,    // Fade out depuis l'écran source
        BLACK_SCREEN, // Écran noir avec initialisation
        FADE_IN      // Fade in vers l'écran cible
    }
    
    private TransitionState state = TransitionState.FADE_OUT;
    private float alpha = 0f; // Opacité du noir
    private float timer = 0f;
    private boolean targetInitialized = false;
    
    private static final float FADE_OUT_DURATION = 0.2f; // Raccourci à 0.8 seconde pour le fade out
    private static final float BLACK_SCREEN_DURATION = 0.1f; // Temps d'attente sur écran noir
    private static final float FADE_IN_DURATION = 0.8f; // Raccourci à 0.8 seconde pour le fade in
    
    /**
     * Vérifie si une transition est actuellement en cours
     * @return true si une transition est en cours, false sinon
     */
    public static boolean isTransitionActive() {
        return isTransitionActive;
    }
    
    /**
     * Constructeur
     * @param game L'instance du jeu
     * @param targetScreen L'écran cible vers lequel effectuer la transition
     */
    public TransitionScreen(Game game, Screen targetScreen) {
        // Marquer le début d'une transition
        isTransitionActive = true;
        
        this.game = game;
        this.sourceScreen = game.getScreen(); // Stocker l'écran actuel
        this.targetScreen = targetScreen;
        
        // Initialiser le batch et la caméra
        this.batch = new SpriteBatch();
        this.camera = new OrthographicCamera();
        this.viewport = new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), camera);
        
        // Créer une texture blanche pour l'effet de fondu
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        this.blackTexture = new Texture(pixmap);
        pixmap.dispose();
        
        // Configurer la caméra
        this.camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        Gdx.app.log("TransitionScreen", "TransitionScreen créé - début de la transition");
    }
    
    @Override
    public void render(float delta) {
        // Limiter delta pour éviter les sauts trop grands
        float limitedDelta = Math.min(delta, 1/30f);
        
        // Mettre à jour le timer et l'alpha
        timer += limitedDelta;
        
        // Mettre à jour la caméra
        viewport.apply();
        camera.update();
        
        // Effacer l'écran
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Gérer la transition selon l'état
        if (state == TransitionState.FADE_OUT) {
            // Phase de fade out, rendre l'écran source avec un fondu au noir
            alpha = Math.min(timer / FADE_OUT_DURATION, 1.0f);
            
            // Rendre l'écran source
            if (sourceScreen != null) {
                sourceScreen.render(limitedDelta);
            }
            
            // Dessiner un rectangle noir semi-transparent par-dessus
            batch.begin();
            batch.setProjectionMatrix(camera.combined);
            batch.setColor(0, 0, 0, alpha);
            batch.draw(blackTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            
            // Si le fade out est terminé, passer à l'écran noir
            if (alpha >= 1.0f) {
                state = TransitionState.BLACK_SCREEN;
                timer = 0f;
                Gdx.app.log("TransitionScreen", "Transition : passage à l'écran noir");
            }
        } else if (state == TransitionState.BLACK_SCREEN) {
            // Phase d'écran noir et d'initialisation
            
            // Initialiser l'écran cible
            if (!targetInitialized && targetScreen != null) {
                // Appeler show() sur l'écran cible pour qu'il s'initialise
                targetScreen.show();
                targetInitialized = true;
                Gdx.app.log("TransitionScreen", "Écran cible initialisé");
                
                // Si l'écran source est toujours attaché, le détacher
                if (sourceScreen != null && sourceScreen != targetScreen) {
                    sourceScreen.hide();
                }
            }
            
            // Dessiner un rectangle noir plein
            batch.begin();
            batch.setProjectionMatrix(camera.combined);
            batch.setColor(0, 0, 0, 1);
            batch.draw(blackTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            
            // Si le temps d'écran noir est écoulé, passer au fade in
            if (timer >= BLACK_SCREEN_DURATION) {
                state = TransitionState.FADE_IN;
                timer = 0f;
                alpha = 1.0f;
                Gdx.app.log("TransitionScreen", "Transition : passage au fade in");
            }
        } else {
            // Phase de fade in, rendre l'écran cible avec un fondu depuis le noir
            alpha = Math.max(1.0f - (timer / FADE_IN_DURATION), 0f);
            
            // Rendre l'écran cible
            if (targetScreen != null) {
                targetScreen.render(limitedDelta);
            }
            
            // Dessiner un rectangle noir semi-transparent par-dessus (qui s'efface progressivement)
            batch.begin();
            batch.setProjectionMatrix(camera.combined);
            batch.setColor(0, 0, 0, alpha);
            batch.draw(blackTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            batch.end();
            
            // Si le fade in est terminé, passer à l'écran cible
            if (alpha <= 0f) {
                Gdx.app.log("TransitionScreen", "Transition terminée, passage à l'écran cible");
                
                // Marquer la fin de la transition
                isTransitionActive = false;
                
                game.setScreen(targetScreen);
                dispose();
                return;
            }
        }
    }
    
    @Override
    public void show() {
        // Appelé lorsque cet écran devient l'écran actif
        Gdx.app.log("TransitionScreen", "Écran de transition affiché");
    }
    
    @Override
    public void resize(int width, int height) {
        this.viewport = ViewportManager.updateViewportWithReconfiguration(viewport, width, height);
        camera.position.set(viewport.getWorldWidth() / 2, viewport.getWorldHeight() / 2, 0);
        camera.update();
        
        // Transmettre le resize uniquement à l'écran cible
        // (empêche de redimensionner l'écran source pendant la transition)
        if (targetScreen != null) {
            targetScreen.resize(width, height);
        }
    }
    
    @Override
    public void hide() {
        // Appelé lorsque cet écran n'est plus l'écran actif
        Gdx.app.log("TransitionScreen", "Écran de transition masqué");
    }
    
    @Override
    public void pause() {
        // Transmettre l'événement pause aux écrans sous-jacents
        if (targetScreen != null) {
            targetScreen.pause();
        }
    }
    
    @Override
    public void resume() {
        // Transmettre l'événement resume aux écrans sous-jacents
        if (targetScreen != null) {
            targetScreen.resume();
        }
    }
    
    @Override
    public void dispose() {
        batch.dispose();
        blackTexture.dispose();
        Gdx.app.log("TransitionScreen", "Ressources de l'écran de transition libérées");
        
        // En cas d'appel à dispose sans passer par la fin normale de la transition
        isTransitionActive = false;
    }
} 