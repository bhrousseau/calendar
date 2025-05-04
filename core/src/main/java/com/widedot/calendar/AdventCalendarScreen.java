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
import com.badlogic.gdx.audio.Sound;
import com.widedot.calendar.config.Config;
import com.widedot.calendar.config.ThemeManager;
import com.widedot.calendar.data.Theme;

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
    private final Map<Integer, Texture> themeIconTextures; // Cache pour les textures d'icônes
    private final ThemeManager themeManager;

    // Sons
    private Sound lockedSound;
    private Sound openSound;

    // Constantes pour les noms de fichiers d'images
    private static final String LOCKED_TEXTURE_PATH = "images/locked.png";
    private static final String UNLOCKED_TEXTURE_PATH = "images/unlocked.png";
    private static final String THEMES_JSON_PATH = "themes.json";

    // Constantes pour les noms de fichiers audio
    private static final String LOCKED_SOUND_PATH = "audio/locked.wav";
    private static final String OPEN_SOUND_PATH = "audio/open2.wav";

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
            this.themeManager = ThemeManager.getInstance();
            this.themeIconTextures = new HashMap<>();

            // Charger les textures avec Gdx.files.internal
            try {
                this.lockedTexture = new Texture(Gdx.files.internal(LOCKED_TEXTURE_PATH));
                this.defaultUnlockedTexture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));

                // Charger les sons
                this.lockedSound = Gdx.audio.newSound(Gdx.files.internal(LOCKED_SOUND_PATH));
                this.openSound = Gdx.audio.newSound(Gdx.files.internal(OPEN_SOUND_PATH));
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement des ressources: " + e.getMessage());
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
        this.themeManager = ThemeManager.getInstance();
        this.themeIconTextures = new HashMap<>();

        // Charger les textures avec Gdx.files.internal
        try {
            this.lockedTexture = new Texture(Gdx.files.internal(LOCKED_TEXTURE_PATH));
            this.defaultUnlockedTexture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));

            // Charger les sons
            this.lockedSound = Gdx.audio.newSound(Gdx.files.internal(LOCKED_SOUND_PATH));
            this.openSound = Gdx.audio.newSound(Gdx.files.internal(OPEN_SOUND_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des ressources: " + e.getMessage());
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
        Collections.shuffle(days, new Random(Config.getInstance().getGameSeed()));

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
        Texture texture = new Texture(Gdx.files.internal(UNLOCKED_TEXTURE_PATH));
        unlockedTextures.put(dayId, texture);
        return texture;
    }

    /**
     * Obtient la texture de l'icône pour un jour donné
     * @param dayId L'identifiant du jour (1-24)
     * @return La texture de l'icône correspondante ou null
     */
    private Texture getThemeIconForDay(int dayId) {
        // Vérifier si la texture est déjà dans le cache
        if (themeIconTextures.containsKey(dayId)) {
            return themeIconTextures.get(dayId);
        }

        try {
            // Chercher d'abord le thème associé au jour via ThemeManager
            Theme theme = themeManager.getThemeByDay(dayId);

            // Si aucun thème n'est associé au jour, utiliser un thème cyclique
            if (theme == null && themeManager.getThemeCount() > 0) {
                // Utiliser l'index modulo pour distribuer cycliquement les thèmes
                int themeIndex = (dayId - 1) % themeManager.getThemeCount();
                theme = themeManager.getAllThemes().get(themeIndex);
            }

            if (theme != null) {
                // Construire le chemin de l'icône en remplaçant "full" par "icon" et .jpg par .png
                String iconPath = theme.getFullImagePath().replace("full", "icon").replace(".jpg", ".png");

                // Charger et mettre en cache la texture
                Texture iconTexture = new Texture(Gdx.files.internal(iconPath));
                themeIconTextures.put(dayId, iconTexture);
                return iconTexture;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'icône pour le jour " + dayId + ": " + e.getMessage());
        }

        return null;
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

            // Dessiner l'icône du thème derrière la case
            Texture themeIcon = getThemeIconForDay(i);
            if (themeIcon != null) {
                // Réduire la taille de l'icône de 10% et la centrer
                float iconWidth = box.width * 0.75f;
                float iconHeight = box.height * 0.75f;
                float iconX = box.x + (box.width - iconWidth) / 2; // Centrer horizontalement
                float iconY = box.y + (box.height - iconHeight) / 2; // Centrer verticalement

                batch.draw(themeIcon, iconX, iconY, iconWidth, iconHeight);
            }

            // Dessiner la case (verrouillée ou déverrouillée)
            Texture texture;
            if (adventGame != null && adventGame.isUnlocked(i)) {
                texture = unlockedTextures.containsKey(i) ? unlockedTextures.get(i) : loadUnlockedTexture(i);
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

                    // Vérifier si la case est déverrouillée
                    if (!adventGame.isUnlocked(i)) {
                        System.out.println("Déverrouillage du jour " + i);
                        adventGame.unlock(i);
                    } else if (!adventGame.isVisited(i)) {
                        System.out.println("Marquage du jour " + i + " comme visité et lancement du mini-jeu");
                        adventGame.setVisited(i, true);
                    }

                    if (!adventGame.isUnlocked(i)) {
                        // Jouer le son "locked" pour une case verrouillée
                        lockedSound.play();
                    } else if (!adventGame.isVisited(i)) {
                        // Jouer le son "open" pour une case déverrouillée mais pas encore visitée
                        openSound.play();
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

        if ((float) width / height > aspectRatio) {
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

        // Disposer des sons
        lockedSound.dispose();
        openSound.dispose();

        // Disposer des textures déverrouillées
        for (Texture texture : unlockedTextures.values()) {
            if (texture != defaultUnlockedTexture) {
                texture.dispose();
            }
        }

        // Disposer des textures des icônes de thèmes
        for (Texture texture : themeIconTextures.values()) {
            texture.dispose();
        }
    }
}
