package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.Gdx;
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

    // Dimensions de base du monde
    private static final float BASE_WIDTH = 800;
    private static final float BASE_HEIGHT = 600;

    /**
     * Constructeur
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     */
    public GameScreen(int dayId, Game game) {
        this.dayId = dayId;
        this.game = game;
        this.camera = new OrthographicCamera();
        this.viewport = new ScreenViewport(camera);
        this.batch = new SpriteBatch();
        this.theme = null; // Le thème sera chargé dans la méthode show()
        
        // Initialiser les dimensions
        this.currentWidth = BASE_WIDTH;
        this.currentHeight = BASE_HEIGHT;
    }

    /**
     * Charge la peinture associée au jour
     * @param day L'identifiant du jour
     * @return La peinture associée au jour
     */
    protected abstract Theme loadTheme(int day);

    /**
     * Initialise le jeu
     */
    protected abstract void initializeGame();

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
    }

    @Override
    public void render(float delta) {
        // Effacer l'écran
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
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
        
        // Mettre à jour la caméra
        camera.setToOrtho(false, width, height);
        
        // Mettre à jour le viewport
        viewport.update(width, height, true);
        
        System.out.println("Redimensionnement terminé");
    }

    @Override
    public void hide() {
        System.out.println("Méthode hide de GameScreen appelée pour le jour " + dayId);
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