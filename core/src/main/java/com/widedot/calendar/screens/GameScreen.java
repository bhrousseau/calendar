package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.AdventCalendarScreen;
import com.widedot.calendar.Main;

/**
 * Classe abstraite de base pour tous les écrans de jeu
 */
public abstract class GameScreen implements Screen {
    protected final Game game;
    protected final int dayId;
    protected OrthographicCamera camera;
    protected Viewport viewport;
    protected final SpriteBatch batch;
    protected final Paintable paintable;
    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;
    
    // Dimensions actuelles de la fenêtre
    protected float currentWidth;
    protected float currentHeight;

    /**
     * Constructeur
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     */
    public GameScreen(int dayId, Game game) {
        this.dayId = dayId;
        this.game = game;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        this.batch = new SpriteBatch();
        this.paintable = loadPaintable(dayId);
        
        // Initialiser les dimensions
        this.currentWidth = WORLD_WIDTH;
        this.currentHeight = WORLD_HEIGHT;
    }

    /**
     * Charge la peinture associée au jour
     * @param day L'identifiant du jour
     * @return La peinture associée au jour
     */
    protected abstract Paintable loadPaintable(int day);

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

    @Override
    public void show() {
        System.out.println("Méthode show de GameScreen appelée pour le jour " + dayId);
        initializeGame();
    }

    @Override
    public void render(float delta) {
        // Effacer l'écran
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Mettre à jour la caméra
        camera.update();
        
        // Configurer le batch avec la matrice de projection de la caméra
        batch.setProjectionMatrix(camera.combined);
        
        // Gérer les entrées et mettre à jour le jeu
        handleInput();
        updateGame(delta);
        
        // Rendre le jeu
        batch.begin();
        renderGame();
        batch.end();
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
     * Retourne au menu principal
     */
    protected void returnToMainMenu() {
        // Créer un nouvel écran de menu principal
        AdventCalendarScreen mainScreen = new AdventCalendarScreen(game);
        
        // Mettre à jour l'écran de Game
        game.setScreen(mainScreen);
        
        // Mettre à jour l'écran de Main
        Game mainGame = (Game) Gdx.app.getApplicationListener();
        if (mainGame instanceof Main) {
            ((Main) mainGame).setScreen(mainScreen);
            System.out.println("Écran de Main mis à jour avec: " + mainScreen.getClass().getSimpleName());
        }
    }
} 