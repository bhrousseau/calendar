package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.AdventCalendarScreen;
import com.widedot.calendar.AdventCalendarGame;

/**
 * Classe abstraite de base pour tous les écrans de jeu
 */
public abstract class GameScreen implements Screen {
    protected final Game game;
    protected final int dayId;
    protected OrthographicCamera camera;
    protected Viewport viewport;
    protected final SpriteBatch batch;
    protected Theme theme;
    
    // Dimensions actuelles de la fenêtre
    protected float currentWidth;
    protected float currentHeight;

    // Dimensions supprimées - utilisation des ViewRules
    
    // Flag pour éviter la double initialisation
    private boolean isInitialized = false;

    /**
     * Constructeur
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     */
    public GameScreen(int dayId, Game game) {
        this.dayId = dayId;
        this.game = game;
        this.camera = new OrthographicCamera();
        
        // Utiliser FitViewport pour maintenir le ratio avec bandes noires
        // Ratio cible : 16:9 (1.777...) 
        float targetWidth = 2048f;  // Largeur fixe
        float targetHeight = targetWidth * 9f / 16f;    // 1152 (ratio 16:9)
        
        this.viewport = new FitViewport(targetWidth, targetHeight, camera);
        
        this.batch = new SpriteBatch();
        this.theme = null;
        
        // Initialiser les dimensions avec les valeurs par défaut
        this.currentWidth = 1280;
        this.currentHeight = 720;
    }

    /**
     * Charge la peinture associée au jour
     * @param day L'identifiant du jour
     * @return La peinture associée au jour
     */
    protected abstract Theme loadTheme(int day);

    /**
     * Initialise le jeu (appelée une seule fois)
     */
    protected abstract void initializeGame();

    /**
     * Appelée quand l'écran devient actif (peut être appelée plusieurs fois)
     */
    protected void onScreenActivated() {
        // Override dans les classes enfants si nécessaire
        // Exemple: reprendre la musique, réactiver les animations, etc.
    }

    /**
     * Appelée quand l'écran devient inactif
     */
    protected void onScreenDeactivated() {
        // Override dans les classes enfants si nécessaire  
        // Exemple: mettre en pause la musique, arrêter les animations, etc.
    }

    /**
     * Met à jour l'état du jeu
     * @param delta Temps écoulé depuis la dernière mise à jour
     */
    protected abstract void updateGame(float delta);

    /**
     * Affiche le jeu
     */
    protected abstract void renderGame();

    /**
     * Gère les entrées utilisateur
     */
    protected abstract void handleInput();
    
    /**
     * Gère les raccourcis globaux (plein écran, etc.)
     * 
     * Raccourcis disponibles :
     * - F11 : Basculer plein écran / fenêtré
     * - Alt+Enter : Basculer plein écran / fenêtré (raccourci alternatif)
     */
    protected void handleGlobalInput() {
        // F11 pour basculer en plein écran
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11)) {
            toggleFullscreen();
        }
        
        // Alt+Enter pour basculer en plein écran (raccourci standard)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && 
            (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
            toggleFullscreen();
        }
    }
    
    /**
     * Bascule entre mode fenêtré et plein écran
     */
    protected void toggleFullscreen() {
        if (Gdx.graphics.isFullscreen()) {
            // Passer en mode fenêtré : taille fixe ratio 16:9, optimisée pour l'écran
            int screenWidth = Gdx.graphics.getDisplayMode().width;
            int screenHeight = Gdx.graphics.getDisplayMode().height;
            
            // Option 1: Basé sur 50% de la hauteur d'écran
            int heightBasedWindowHeight = screenHeight / 2;
            int heightBasedWindowWidth = Math.round(heightBasedWindowHeight * 16f / 9f);
            
            // Option 2: Basé sur la largeur maximale de l'écran
            int widthBasedWindowWidth = screenWidth;
            int widthBasedWindowHeight = Math.round(widthBasedWindowWidth * 9f / 16f);
            
            // Choisir la plus petite des deux options
            int windowWidth, windowHeight;
            String method;
            
            if (heightBasedWindowWidth <= screenWidth) {
                // La fenêtre basée sur la hauteur rentre dans l'écran
                windowWidth = heightBasedWindowWidth;
                windowHeight = heightBasedWindowHeight;
                method = "50% hauteur écran";
            } else {
                // La fenêtre basée sur la hauteur est trop large, utiliser la largeur
                windowWidth = widthBasedWindowWidth;
                windowHeight = widthBasedWindowHeight;
                method = "largeur écran max";
            }
            
            Gdx.graphics.setWindowedMode(windowWidth, windowHeight);
            System.out.println("Basculé en mode fenêtré (" + windowWidth + "x" + windowHeight + ") - ratio 16:9, " + method);
        } else {
            // Passer en plein écran
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            System.out.println("Basculé en plein écran (" + 
                             Gdx.graphics.getDisplayMode().width + "x" + 
                             Gdx.graphics.getDisplayMode().height + ")");
        }
    }

    /**
     * Détermine si les entrées utilisateur doivent être traitées
     * @return true si les entrées peuvent être traitées, false sinon
     */
    protected boolean canHandleInput() {
        // Vérifier si une transition est en cours
        if (TransitionScreen.isTransitionActive()) {
            return false;
        }
        return true;
    }

    @Override
    public void show() {
        System.out.println("Méthode show de GameScreen appelée pour le jour " + dayId);
        
        // Initialisation lourde (une seule fois)
        if (!isInitialized) {
            System.out.println("Initialisation de l'écran de jeu pour le jour " + dayId);
            
            // Récupérer le thème pour ce jour
            if (game instanceof AdventCalendarGame) {
                AdventCalendarGame adventGame = (AdventCalendarGame) game;
                
                try {
                    theme = loadTheme(dayId);
                    System.out.println("Thème chargé: " + (theme != null ? theme.getTitle() : "null"));
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement du thème: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // Définir un score initial de 0 si non défini
                if (adventGame.getScore(dayId) == 0) {
                    adventGame.setScore(dayId, 0);
                }
            }
            
            // Initialiser le jeu
            initializeGame();
            
            isInitialized = true;
            System.out.println("Initialisation de l'écran de jeu terminée");
        }
        
        // Activation légère de l'écran (à chaque show())
        onScreenActivated();
    }

    @Override
    public void render(float delta) {
        // Effacer l'écran avec un fond noir
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Mettre à jour la caméra
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        // Mettre à jour le jeu
        updateGame(delta);
        
        // Dessiner le jeu
        batch.begin();
        renderGame();
        batch.end();
        
        // Gérer les raccourcis globaux (plein écran)
        handleGlobalInput();
        
        // Gérer les entrées utilisateur seulement si autorisé
        if (canHandleInput()) {
            handleInput();
        }
    }

    @Override
    public void resize(int width, int height) {
        System.out.println("Redimensionnement de GameScreen: " + width + "x" + height);
        
        // Mettre à jour les dimensions actuelles
        this.currentWidth = width;
        this.currentHeight = height;
        
        // Mettre à jour le viewport (FitViewport gère automatiquement les bandes noires)
        viewport.update(width, height, true);
        
        // Informations de debug
        System.out.println("Monde: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
        System.out.println("Redimensionnement terminé");
    }

    @Override
    public void hide() {
        System.out.println("Méthode hide de GameScreen appelée pour le jour " + dayId);
        
        // Désactivation de l'écran
        onScreenDeactivated();
    }

    @Override
    public void pause() {
        System.out.println("Méthode pause de GameScreen appelée pour le jour " + dayId);
    }

    @Override
    public void resume() {
        System.out.println("Méthode resume de GameScreen appelée pour le jour " + dayId);
    }

    @Override
    public void dispose() {
        System.out.println("Méthode dispose de GameScreen appelée pour le jour " + dayId);
        batch.dispose();
    }

    /**
     * Retourne au menu principal (calendrier de l'avent)
     */
    protected void returnToMainMenu() {
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            Screen calendarScreen = new AdventCalendarScreen(adventGame);
            
            // Utiliser l'écran de transition pour revenir au calendrier
            game.setScreen(new com.widedot.calendar.screens.TransitionScreen(game, calendarScreen));
        }
    }
} 