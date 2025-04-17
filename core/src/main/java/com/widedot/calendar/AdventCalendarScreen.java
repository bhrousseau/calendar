package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.widedot.calendar.config.GameConfig;
import com.widedot.calendar.painting.PaintingManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Écran principal du calendrier de l'Avent
 * Affiche la grille de 24 cases et gère les interactions
 */
public class AdventCalendarScreen implements Screen {
    private final Game game;
    private final AdventCalendarGame adventGame;
    private final OrthographicCamera camera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final Texture lockedTexture;
    private final Texture defaultUnlockedTexture;
    private final Map<Integer, Texture> unlockedTextures;
    private final Map<Integer, Rectangle> boxes;
    
    // Constantes pour les noms de fichiers d'images
    private static final String LOCKED_TEXTURE_PATH = "images/locked.png";
    private static final String UNLOCKED_TEXTURE_PATH = "images/unlocked.png";
    private static final String UNLOCKED_FORMAT = "images/%02d-unlocked.png";
    
    // Dimensions de base du monde
    private static final float WORLD_WIDTH = 800;
    private static final float WORLD_HEIGHT = 600;
    private static final float BOX_SIZE = 100;
    private static final float BOX_SPACING = 20;
    private static final int GRID_COLS = 6;
    private static final int GRID_ROWS = 4;
    
    // Dimensions actuelles de la fenêtre
    private float currentWidth;
    private float currentHeight;
    private float boxSize;
    private float boxSpacing;
        
        /**
         * Constructeur
         * @param game L'instance du jeu
         */
        public AdventCalendarScreen(Game game) {
            this.game = game;
            this.adventGame = (AdventCalendarGame) game; // Cast en AdventCalendarGame
            
            this.camera = new OrthographicCamera();
            this.camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
            
            this.batch = new SpriteBatch();
            this.font = new BitmapFont();
            
            // Charger les textures avec Gdx.files.internal
            try {
                this.lockedTexture = new Texture(Gdx.files.internal(LOCKED_TEXTURE_PATH));
                this.defaultUnlockedTexture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des textures de base: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
            
            this.unlockedTextures = new HashMap<>();
            this.boxes = new HashMap<Integer, Rectangle>();
        
            // Initialiser les dimensions
            this.currentWidth = WORLD_WIDTH;
            this.currentHeight = WORLD_HEIGHT;
            this.boxSize = BOX_SIZE;
            this.boxSpacing = BOX_SPACING;
            
            // Initialiser les boîtes avec les dimensions par défaut
            initializeBoxes();
            
            // Appeler resize pour ajuster les dimensions à la taille de la fenêtre
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
    
    /**
     * Constructeur avec AdventCalendarGame
     * @param adventGame L'instance du jeu de calendrier de l'Avent
     */
    public AdventCalendarScreen(AdventCalendarGame adventGame) {
        this.game = adventGame;
        this.adventGame = adventGame;
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, WORLD_WIDTH, WORLD_HEIGHT);
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        
        // Charger les textures avec Gdx.files.internal
        try {
            this.lockedTexture = new Texture(Gdx.files.internal(LOCKED_TEXTURE_PATH));
            this.defaultUnlockedTexture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des textures de base: " + e.getMessage());
            throw e;
        }
        
        this.unlockedTextures = new HashMap<>();
        this.boxes = new HashMap<Integer, Rectangle>();
        
        // Initialiser les dimensions
        this.currentWidth = WORLD_WIDTH;
        this.currentHeight = WORLD_HEIGHT;
        this.boxSize = BOX_SIZE;
        this.boxSpacing = BOX_SPACING;
        
        // Initialiser les boîtes avec les dimensions par défaut
        initializeBoxes();
        
        // Appeler resize pour ajuster les dimensions à la taille de la fenêtre
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }
    
    /**
     * Initialise la position et la taille des boîtes
     */
    private void initializeBoxes() {
        // Calculer les dimensions de la grille
        float gridWidth = GRID_COLS * (boxSize + boxSpacing) - boxSpacing;
        float gridHeight = GRID_ROWS * (boxSize + boxSpacing) - boxSpacing;
        
        // Centrer la grille
        float startX = (currentWidth - gridWidth) / 2;
        float startY = (currentHeight - gridHeight) / 2;
        
        // Créer une liste des jours (1 à 24)
        List<Integer> days = new ArrayList<>();
        for (int i = 1; i <= 24; i++) {
            days.add(i);
        }
        
        // Mélanger la liste des jours
        Collections.shuffle(days, new Random(GameConfig.getInstance().getGameSeed()));
        
        // Créer les boîtes avec les jours mélangés
        for (int i = 0; i < 24; i++) {
            int dayId = days.get(i);
            int row = i / GRID_COLS;
            int col = i % GRID_COLS;
            float x = startX + col * (boxSize + boxSpacing);
            float y = startY + (GRID_ROWS - 1 - row) * (boxSize + boxSpacing);
            boxes.put(dayId, new Rectangle(x, y, boxSize, boxSize));
        }
    }
    
    /**
     * Charge la texture pour un jour déverrouillé
     * @param dayId L'identifiant du jour
     * @return La texture chargée ou la texture par défaut si le chargement échoue
     */
    private Texture loadUnlockedTexture(int dayId) {
        if (unlockedTextures.containsKey(dayId)) {
            return unlockedTextures.get(dayId);
        }
        
        // Essayer d'abord le format "xx-unlocked.png"
        String texturePath = String.format(UNLOCKED_FORMAT, dayId);
        try {
            Texture texture = new Texture(Gdx.files.internal(texturePath));
            unlockedTextures.put(dayId, texture);
            return texture;
        } catch (Exception e) {
            // Si l'image spécifique n'est pas trouvée, essayer "unlocked.png"
            try {
                Texture texture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));
                unlockedTextures.put(dayId, texture);
                return texture;
            } catch (Exception e2) {
                // Si "unlocked.png" n'est pas trouvé, utiliser la texture par défaut
                System.out.println("Texture " + UNLOCKED_TEXTURE_PATH + " non trouvée, utilisation de la texture par défaut");
                return defaultUnlockedTexture;
            }
        }
    }
    
    @Override
    public void render(float delta) {
        // Effacer l'écran avec une couleur de fond
        Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Mettre à jour la caméra
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        // Dessiner les boîtes
        batch.begin();
        for (int i = 1; i <= 24; i++) {
            Rectangle box = boxes.get(i);
            Texture texture;
            
            if (adventGame != null && adventGame.isPaintingUnlocked(i)) {
                texture = loadUnlockedTexture(i);
            } else {
                texture = lockedTexture;
            }
            
            batch.draw(texture, box.x, box.y, box.width, box.height);
        }
        batch.end();
        
        // Gérer les entrées utilisateur
        handleInput();
    }
    
    /**
     * Gère les entrées utilisateur
     */
    private void handleInput() {
        if (Gdx.input.justTouched()) {
            System.out.println("Toucher détecté dans AdventCalendarScreen");
            Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(touchPos);
            System.out.println("Position du toucher: (" + touchPos.x + ", " + touchPos.y + ")");
            
            for (int i = 1; i <= 24; i++) {
                Rectangle box = boxes.get(i);
                if (box.contains(touchPos.x, touchPos.y)) {
                    System.out.println("Case cliquée : jour " + i);
                    if (!adventGame.isPaintingUnlocked(i)) {
                        System.out.println("Déverrouillage du jour " + i);
                        adventGame.unlockPainting(i);
                    } else if (!adventGame.isPaintingVisited(i)) {
                        System.out.println("Marquage du jour " + i + " comme visité et lancement du mini-jeu");
                        adventGame.markPaintingAsVisited(i);
                        adventGame.launchGame(i);
                    } else {
                        System.out.println("Lancement du mini-jeu pour le jour " + i);
                        adventGame.launchGame(i);
                    }
                    break;
                }
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        System.out.println("Méthode resize de AdventCalendarScreen appelée avec dimensions: " + width + "x" + height);
        // Mettre à jour les dimensions actuelles
        this.currentWidth = width;
        this.currentHeight = height;
        
        // Mettre à jour la caméra
        camera.setToOrtho(false, width, height);
        
        // Calculer les nouvelles dimensions des boîtes en conservant les proportions
        float aspectRatio = (float) GRID_COLS / GRID_ROWS;
        float gridWidth, gridHeight;
        
        if (width / height > aspectRatio) {
            // La hauteur est la contrainte
            gridHeight = height * 0.9f; // Utiliser 90% de la hauteur
            gridWidth = gridHeight * aspectRatio;
        } else {
            // La largeur est la contrainte
            gridWidth = width * 0.9f; // Utiliser 90% de la largeur
            gridHeight = gridWidth / aspectRatio;
        }
        
        // Calculer la taille des boîtes et l'espacement
        this.boxSize = (gridWidth - (GRID_COLS - 1) * BOX_SPACING) / GRID_COLS;
        this.boxSpacing = BOX_SPACING * (boxSize / BOX_SIZE); // Ajuster l'espacement proportionnellement
        
        // Réinitialiser les boîtes avec les nouvelles dimensions
        initializeBoxes();
    }
    
    @Override
    public void show() {
        // Non utilisé
    }
    
    @Override
    public void hide() {
        // Non utilisé
    }
    
    @Override
    public void pause() {
        // Non utilisé
    }
    
    @Override
    public void resume() {
        // Non utilisé
    }
    
    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        lockedTexture.dispose();
        defaultUnlockedTexture.dispose();
        for (Texture texture : unlockedTextures.values()) {
            if (texture != defaultUnlockedTexture) {
                texture.dispose();
            }
        }
    }
} 