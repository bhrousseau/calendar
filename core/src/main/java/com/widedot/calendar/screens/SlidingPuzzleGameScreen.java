package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.widedot.calendar.display.DisplayConfig;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Sound;
import com.widedot.calendar.config.Config;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.debug.SlidingPuzzleDebugManager;
import com.widedot.calendar.config.DayMappingManager;
import com.widedot.calendar.utils.CarlitoFontManager;

/**
 * Écran de jeu pour le puzzle coulissant
 */
public class SlidingPuzzleGameScreen extends GameScreen {
    // Input processor pour les clics et raccourcis clavier
    private InputAdapter inputProcessor;
    
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Rectangle infoButton;
    private final Rectangle closeButton;
    private final Color infoPanelColor;
    private boolean showInfoPanel;
    private final Texture whiteTexture;
    private Texture infoButtonTexture;
    private Texture closeButtonTexture;
    private Color backgroundColor;
    private boolean isTestMode; // Ajout d'une variable pour le mode test
    
    // Sons
    private Sound solveSound;
    private Sound slidingSound;
    private static final String SOLVE_SOUND_PATH = "audio/win.mp3";
    private static final String SLIDING_SOUND_PATH = "audio/sliding.mp3";
    
    // Paramètres du jeu provenant de la configuration
    private int gridSize;
    private final float GRID_MARGIN = 80; // Marge autour de la grille
    private final float SPACING_RATIO = 0.05f; // Espacement fixe de 5% de la taille des tuiles
    private float animationSpeed; // Vitesse de l'animation (tuiles par seconde)
    private int shuffleMoves; // Nombre de mouvements pour mélanger le puzzle
    
    private int[] puzzleState; // État actuel du puzzle (index = position, valeur = numéro de tuile)
    private Rectangle[] gridZones; // Positions des tuiles (index = position)
    private float tileSize; // Taille dynamique des tuiles
    private float tileSpacing; // Espacement dynamique entre les tuiles
    private int emptyTileIndex; // Position de la case vide
    private boolean isPuzzleSolved; // Indique si le puzzle est résolu
    private Texture puzzleTexture; // Texture du puzzle
    private TextureRegion[] puzzleTiles; // Régions de texture pour chaque tuile
    private Theme theme; // Le thème du jeu
    private Texture backgroundTexture; // Texture du background
    
    // Système de debug
    private SlidingPuzzleDebugManager debugManager;
    private ObjectMap<String, Object> gameParameters;
    private String gameReference;
    
    // Paramètres du filtre de couleur pour le background (gris par défaut)
    private float backgroundHue = 0f;
    private float backgroundSaturation = 0f;
    private float backgroundLightness = 0f;

    // Variables d'animation
    private static class AnimationState {
        // Constantes de timing
        private static final float FADE_IN_DURATION = 1f;
        private static final float MERGE_DURATION = 1f;
        private static final float PHASE_DELAY = 0f;
        private static final float PIXELS_PER_SECOND = 500f;
        private static final float VICTORY_MESSAGE_DELAY = 1f;

        // État de l'animation
        private boolean isActive = false;
        private boolean isComplete = false;
        private float progress = 0f;
        private float totalDuration = 0f;
        private float irisOpenDuration = 0f;
        private float victoryMessageTimer = 0f;

        // Progression des phases
        private float fadeProgress = 0f;
        private float mergeProgress = 0f;
        private float irisOpenProgress = 0f;

        public void start() {
            isActive = true;
            isComplete = false;
            progress = 0f;
            victoryMessageTimer = 0f;
            fadeProgress = 0f;
            mergeProgress = 0f;
            irisOpenProgress = 0f;
        }

        public void update(float delta) {
            if (!isActive) return;
            
            progress += delta;
            
            // Calculer les progressions de chaque phase
            fadeProgress = Math.min(1f, progress / FADE_IN_DURATION);
            mergeProgress = Math.max(0f, Math.min(1f, (progress - FADE_IN_DURATION - PHASE_DELAY) / MERGE_DURATION));
            
            float irisOpenStart = FADE_IN_DURATION + MERGE_DURATION + PHASE_DELAY;
            if (progress >= irisOpenStart) {
                irisOpenProgress = Math.min(1f, (progress - irisOpenStart) / irisOpenDuration);
            }

            if (progress >= totalDuration) {
                isActive = false;
                isComplete = true;
                fadeProgress = 1f;
                mergeProgress = 1f;
                irisOpenProgress = 1f;
            }
        }

        public void updateVictoryMessageTimer(float delta) {
            if (isComplete) {
                victoryMessageTimer += delta;
            }
        }

        public boolean shouldShowVictoryMessage() {
            return isComplete && victoryMessageTimer >= VICTORY_MESSAGE_DELAY;
        }

        public void calculateDurations(float imageWidth, float imageHeight, Theme.CropInfo squareCrop) {
            // Calculer la durée de l'ouverture rectangulaire
            float totalDistance = Math.max(
                imageWidth - squareCrop.getWidth(),
                imageHeight - squareCrop.getHeight()
            );
            irisOpenDuration = totalDistance / PIXELS_PER_SECOND;
            
            // Calculer la durée totale
            totalDuration = FADE_IN_DURATION + MERGE_DURATION + PHASE_DELAY + irisOpenDuration;
        }

        public boolean isActive() { return isActive; }
        public boolean isComplete() { return isComplete; }
        public float getFadeProgress() { return fadeProgress; }
        public float getMergeProgress() { return mergeProgress; }
        public float getIrisOpenProgress() { return irisOpenProgress; }
    }

    // Variables d'animation de victoire
    private final AnimationState animationState = new AnimationState();
    private Texture fullImageTexture;
    
    // Variables de calcul du background mutualisées (comme MastermindGameScreen)
    private float currentBgX, currentBgY, currentBgWidth, currentBgHeight;
    private float currentScaleX, currentScaleY;
    
    // Variables pour le fondu des boutons lors de la victoire
    private boolean isButtonsFading = false;
    private float buttonsFadeTimer = 0f;
    private static final float BUTTONS_FADE_DURATION = 1.0f; // Durée du fondu en secondes

    // Variables d'animation des tuiles
    private static class TileAnimation {
        private boolean isActive = false;
        private float progress = 0f;
        private int tileIndex;
        private Vector3 start = new Vector3();
        private Vector3 end = new Vector3();

        public void start(int tileIndex, Vector3 start, Vector3 end) {
            this.isActive = true;
            this.progress = 0f;
            this.tileIndex = tileIndex;
            this.start.set(start);
            this.end.set(end);
        }

        public void update(float delta, float speed) {
            if (!isActive) return;
            
            progress += delta * speed;
            if (progress >= 1f) {
                isActive = false;
                progress = 1f;
            }
        }

        public boolean isActive() { return isActive; }
        public float getProgress() { return progress; }
        public int getTileIndex() { return tileIndex; }
        public Vector3 getCurrentPosition() {
            Vector3 current = new Vector3();
            current.x = start.x + (end.x - start.x) * progress;
            current.y = start.y + (end.y - start.y) * progress;
            return current;
        }
    }

    private final TileAnimation tileAnimation = new TileAnimation();

    private boolean isAdjacent(int tileIndex1, int tileIndex2) {
        // Convertir les indices linéaires en coordonnées de grille
        int row1 = tileIndex1 / gridSize;
        int col1 = tileIndex1 % gridSize;
        int row2 = tileIndex2 / gridSize;
        int col2 = tileIndex2 % gridSize;

        // Vérifier l'adjacence horizontale ou verticale
        boolean horizontalAdjacent = row1 == row2 && Math.abs(col1 - col2) == 1;
        boolean verticalAdjacent = col1 == col2 && Math.abs(row1 - row2) == 1;

        return horizontalAdjacent || verticalAdjacent;
    }

    
    

    /**
     * Initialise un état du puzzle résoluble en simulant des mouvements valides
     */
    private void initializeSolvablePuzzle() {
        // Commencer avec l'état résolu
        for (int i = 0; i < puzzleState.length; i++) {
            puzzleState[i] = i;
        }
        emptyTileIndex = puzzleState.length - 1;

        // Nombre de mouvements aléatoires à effectuer
        int numMoves = shuffleMoves;

        // Effectuer des mouvements aléatoires valides
        for (int i = 0; i < numMoves; i++) {
            // Trouver les tuiles adjacentes à la case vide
            int row = emptyTileIndex / gridSize;
            int col = emptyTileIndex % gridSize;
            int[] possibleMoves = new int[4];
            int numPossibleMoves = 0;

            // Vérifier les 4 directions possibles
            if (row > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - gridSize; // Haut
            if (row < gridSize - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + gridSize; // Bas
            if (col > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - 1; // Gauche
            if (col < gridSize - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + 1; // Droite

            // Choisir un mouvement aléatoire parmi les possibles
            if (numPossibleMoves > 0) {
                int moveIndex = (int)(Math.random() * numPossibleMoves);
                int tileToMove = possibleMoves[moveIndex];

                // Échanger la tuile avec la case vide
                int temp = puzzleState[tileToMove];
                puzzleState[tileToMove] = puzzleState[emptyTileIndex];
                puzzleState[emptyTileIndex] = temp;
                emptyTileIndex = tileToMove;
            }
        }
    }

    /**
     * Constructeur avec paramètres dynamiques
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     * @param theme Le thème du jeu
     * @param parameters Les paramètres du jeu
     */
    public SlidingPuzzleGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        this.theme = theme;
        this.gameParameters = parameters; // Stocker les paramètres pour le debug
        
        // Obtenir la référence du jeu pour la sauvegarde
        this.gameReference = DayMappingManager.getInstance().getGameReferenceForDay(dayId);
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.gridSize = 4;
        this.animationSpeed = 10f;
        this.shuffleMoves = 200;
        this.backgroundColor = new Color(0, 0, 0, 1);
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        Gdx.app.log("SlidingPuzzleGameScreen", "Mode test: " + isTestMode);
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            // Taille de la grille
            if (parameters.containsKey("size")) {
                this.gridSize = ((Number)parameters.get("size")).intValue();
            }
            
            // Couleur de fond
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String)parameters.get("bgColor");
                String[] parts = bgColor.split(",");
                if (parts.length == 3) {
                    try {
                        float r = Integer.parseInt(parts[0]) / 255f;
                        float g = Integer.parseInt(parts[1]) / 255f;
                        float b = Integer.parseInt(parts[2]) / 255f;
                        this.backgroundColor = new Color(r, g, b, 1);
                    } catch (NumberFormatException e) {
                        Gdx.app.error("SlidingPuzzleGameScreen", "Format de couleur invalide: " + bgColor);
                    }
                }
            }
            
            // Nombre de mouvements pour mélanger
            if (parameters.containsKey("shuffle")) {
                this.shuffleMoves = ((Number)parameters.get("shuffle")).intValue();
            }
            
            // Vitesse d'animation
            if (parameters.containsKey("animationSpeed")) {
                this.animationSpeed = ((Number)parameters.get("animationSpeed")).floatValue();
            }
            
            // Paramètres du filtre de couleur du background
            Gdx.app.log("SlidingPuzzleGameScreen", "Chargement des paramètres HSL...");
            
            if (parameters.containsKey("bgHue")) {
                float hue = ((Number)parameters.get("bgHue")).floatValue();
                this.backgroundHue = Math.max(0, Math.min(360, hue));
                Gdx.app.log("SlidingPuzzleGameScreen", "bgHue trouvé dans paramètres: " + backgroundHue);
            } else {
                Gdx.app.log("SlidingPuzzleGameScreen", "bgHue NON TROUVÉ dans paramètres - utilisation de la valeur par défaut: " + backgroundHue);
            }
            
            if (parameters.containsKey("bgSaturation")) {
                float saturation = ((Number)parameters.get("bgSaturation")).floatValue();
                this.backgroundSaturation = Math.max(0, Math.min(100, saturation));
                Gdx.app.log("SlidingPuzzleGameScreen", "bgSaturation trouvé dans paramètres: " + backgroundSaturation);
            } else {
                Gdx.app.log("SlidingPuzzleGameScreen", "bgSaturation NON TROUVÉ dans paramètres - utilisation de la valeur par défaut: " + backgroundSaturation);
            }
            
            if (parameters.containsKey("bgLightness")) {
                float lightness = ((Number)parameters.get("bgLightness")).floatValue();
                this.backgroundLightness = Math.max(-100, Math.min(100, lightness));
                Gdx.app.log("SlidingPuzzleGameScreen", "bgLightness trouvé dans paramètres: " + backgroundLightness);
            } else {
                Gdx.app.log("SlidingPuzzleGameScreen", "bgLightness NON TROUVÉ dans paramètres - utilisation de la valeur par défaut: " + backgroundLightness);
            }
        }
        
        // Initialisation des couleurs et éléments UI
        // Utiliser le gestionnaire Distance Field pour une qualité optimale
        CarlitoFontManager.initialize();
        this.font = CarlitoFontManager.getFont();
        // Utiliser une échelle entière pour éviter les problèmes d'alignement de pixels
        font.getData().setScale(1.0f);
        this.layout = new GlyphLayout();
        
        // Initialiser les boutons (tailles seront définies dans updateButtonPositions)
        this.infoButton = new Rectangle(0, 0, 100, 100);
        this.closeButton = new Rectangle(0, 0, 100, 100);
        this.infoPanelColor = new Color(0.3f, 0.3f, 0.3f, 0.8f);
        this.showInfoPanel = false;
        
        Gdx.app.log("SlidingPuzzleGameScreen", "Création du puzzle coulissant pour le jour " + dayId);
        Gdx.app.log("SlidingPuzzleGameScreen", "Paramètres: gridSize=" + gridSize + ", shuffle=" + shuffleMoves);

        Gdx.app.log("SlidingPuzzleGameScreen", "Création de la texture blanche...");
        // Créer une texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        Gdx.app.log("SlidingPuzzleGameScreen", "Texture blanche créée");
        
        // Charger la texture du bouton info
        Gdx.app.log("SlidingPuzzleGameScreen", "Chargement bouton info...");
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
            Gdx.app.log("SlidingPuzzleGameScreen", "Bouton info chargé");
        } catch (Throwable e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement du bouton info: " + e.getClass().getName() + ": " + e.getMessage());
            this.infoButtonTexture = null;
        }
        
        // Charger la texture du bouton close
        Gdx.app.log("SlidingPuzzleGameScreen", "Chargement bouton close...");
        try {
            this.closeButtonTexture = new Texture(Gdx.files.internal("images/ui/close.png"));
            Gdx.app.log("SlidingPuzzleGameScreen", "Bouton close chargé");
        } catch (Throwable e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement du bouton close: " + e.getClass().getName() + ": " + e.getMessage());
            this.closeButtonTexture = null;
        }
        
        // Charger la texture du background (le filtre HSL sera appliqué via shader GPU)
        Gdx.app.log("SlidingPuzzleGameScreen", "Chargement du background...");
        try {
            Gdx.app.log("SlidingPuzzleGameScreen", "Chargement de l'image background-0.png...");
            // Charger directement la texture sans modification
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/spz/background-0.png"));
            Gdx.app.log("SlidingPuzzleGameScreen", "Texture background créée - " + 
                             backgroundTexture.getWidth() + "x" + backgroundTexture.getHeight());
            
            Gdx.app.log("SlidingPuzzleGameScreen", "Background chargé - Shader HSL sera appliqué au rendu - Teinte: " + backgroundHue + 
                             ", Saturation: " + backgroundSaturation + 
                             ", Luminosité: " + backgroundLightness);
        } catch (Throwable e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement du background: " + e.getClass().getName() + ": " + e.getMessage());
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
            }
            Gdx.app.error("SlidingPuzzleGameScreen", stackTrace.toString());
            this.backgroundTexture = null;
        }

        // Charger le son de résolution
        Gdx.app.log("SlidingPuzzleGameScreen", "Chargement des sons...");
        try {
            this.solveSound = Gdx.audio.newSound(Gdx.files.internal(SOLVE_SOUND_PATH));
            this.slidingSound = Gdx.audio.newSound(Gdx.files.internal(SLIDING_SOUND_PATH));
            Gdx.app.log("SlidingPuzzleGameScreen", "Sons chargés");
        } catch (Throwable e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement des sons: " + e.getClass().getName() + ": " + e.getMessage());
        }
        
        // Initialiser les tableaux (uniquement dans le constructeur)
        Gdx.app.log("SlidingPuzzleGameScreen", "Initialisation des tableaux...");
        this.puzzleState = new int[gridSize * gridSize];
        this.gridZones = new Rectangle[gridSize * gridSize];
        
        // Initialiser tous les Rectangle du tableau gridZones
        for (int i = 0; i < gridZones.length; i++) {
            gridZones[i] = new Rectangle(0, 0, 1, 1);
        }
        
        this.emptyTileIndex = gridSize * gridSize - 1;
        Gdx.app.log("SlidingPuzzleGameScreen", "Tableaux initialisés");
        
        // Initialiser complètement le jeu (méthode factorisée)
        Gdx.app.log("SlidingPuzzleGameScreen", "Appel à initializeGameCompletely()...");
        initializeGameCompletely();
        Gdx.app.log("SlidingPuzzleGameScreen", "initializeGameCompletely() terminé");
        
        // Créer l'input processor
        Gdx.app.log("SlidingPuzzleGameScreen", "Création de l'input processor...");
        createInputProcessor();
        Gdx.app.log("SlidingPuzzleGameScreen", "Input processor créé");
        Gdx.app.log("SlidingPuzzleGameScreen", "Constructeur SlidingPuzzleGameScreen terminé avec succès");
    }
    
    /**
     * Initialise complètement le jeu (méthode factorisée)
     */
    private void initializeGameCompletely() {
        Gdx.app.log("SlidingPuzzleGameScreen", "Initialisation complète du puzzle coulissant pour le jour " + dayId);
        
        // Recharger le background avec les nouveaux paramètres de couleur
        reloadBackgroundTexture();
        
        // Recharger la texture du puzzle si nécessaire
        reloadPuzzleTexture();
        
        // Redimensionner les tableaux avec la nouvelle taille
        int totalTiles = gridSize * gridSize;
        this.puzzleState = new int[totalTiles];
        this.gridZones = new Rectangle[totalTiles];
        
        // Initialiser tous les Rectangle du tableau gridZones
        for (int i = 0; i < gridZones.length; i++) {
            gridZones[i] = new Rectangle(0, 0, 1, 1);
        }
        
        this.emptyTileIndex = totalTiles - 1;
        
        // Calculer les dimensions du background et positionner les boutons
        calculateBackgroundDimensions();
        updateButtonPositions();
        
        // Créer les tuiles du puzzle
        createPuzzleTiles();
        
        // Initialiser le jeu (recalcule tileSize et tileSpacing)
        initializeGame();
        
        // Forcer le recalcul des dimensions après initializeGame
        Gdx.app.log("SlidingPuzzleGameScreen", "Dimensions recalculées - tileSize: " + tileSize + ", tileSpacing: " + tileSpacing);
        
        // Vérifier les dimensions des gridZones
        if (gridZones != null && gridZones.length > 0) {
            Gdx.app.log("SlidingPuzzleGameScreen", "gridZones[0] - x: " + gridZones[0].x + ", y: " + gridZones[0].y + 
                             ", width: " + gridZones[0].width + ", height: " + gridZones[0].height);
            Gdx.app.log("SlidingPuzzleGameScreen", "gridZones[1] - x: " + gridZones[1].x + ", y: " + gridZones[1].y + 
                             ", width: " + gridZones[1].width + ", height: " + gridZones[1].height);
        }
    }
    
    /**
     * Recharge la texture du puzzle si nécessaire
     */
    private void reloadPuzzleTexture() {
        // Vérifier si la texture du puzzle est déjà chargée
        if (puzzleTexture != null) {
            return; // Déjà chargée, pas besoin de recharger
        }
        
        // Charger la texture du puzzle (même logique que dans le constructeur)
        String fullImagePath = this.theme.getFullImagePath();
        if (fullImagePath == null || fullImagePath.isEmpty()) {
            throw new IllegalStateException("Le chemin d'image du thème est invalide");
        }
        
        try {
            Gdx.app.log("SlidingPuzzleGameScreen", "Chargement de la texture depuis: " + fullImagePath);
            puzzleTexture = new Texture(Gdx.files.internal(fullImagePath));
            fullImageTexture = new Texture(Gdx.files.internal(fullImagePath));
        } catch (Exception e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement de la texture: " + e.getMessage());
            // Stack trace logged automatically by Gdx.app.error
            throw new IllegalStateException("Erreur lors du chargement de la texture: " + e.getMessage(), e);
        }
    }

    /**
     * Recharge la texture de background avec les paramètres actuels
     */
    private void reloadBackgroundTexture() {
        try {
            // Libérer l'ancienne texture si elle existe
            if (backgroundTexture != null) {
                backgroundTexture.dispose();
            }
            
            // Charger directement la texture (le filtre HSL sera appliqué via shader GPU)
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/spz/background-0.png"));
            
            Gdx.app.log("SlidingPuzzleGameScreen", "Background rechargé - Shader HSL sera appliqué au rendu - Teinte: " + backgroundHue + 
                             ", Saturation: " + backgroundSaturation + 
                             ", Luminosité: " + backgroundLightness);
        } catch (Exception e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du rechargement du background: " + e.getMessage());
            this.backgroundTexture = null;
        }
    }

    /**
     * Initialise le système de debug
     */
    private void initializeDebugManager() {
        debugManager = new SlidingPuzzleDebugManager(font);
        debugManager.setChangeCallback(new SlidingPuzzleDebugManager.DebugChangeCallback() {
            @Override
            public void onDebugParameterChanged() {
                // Appliquer les paramètres de debug sans recharger depuis JSON
                applyDebugSettings();
                // Recharger le jeu avec les nouveaux paramètres
                initializeGameCompletely();
            }
            
            @Override
            public void onDebugSettingsSaved() {
                // Ne plus recharger sur la sauvegarde, juste afficher la confirmation
                Gdx.app.log("SlidingPuzzleGameScreen", "Paramètres sauvegardés dans games.json");
            }
        });
        
        // Définir la référence du jeu pour la sauvegarde
        debugManager.setCurrentGameReference(gameReference);
        
        // Initialiser le debug avec les paramètres du jeu
        debugManager.initializeFromGameParameters(gameParameters);
    }
    
    /**
     * Applique les paramètres de debug
     */
    private void applyDebugSettings() {
        if (debugManager != null && debugManager.isDebugMode()) {
            // Appliquer les paramètres de debug
            gridSize = debugManager.getDebugSize();
            shuffleMoves = debugManager.getDebugShuffle();
            animationSpeed = debugManager.getDebugAnimationSpeed();
            
            // Appliquer les paramètres de couleur RGB
            backgroundColor = new Color(
                debugManager.getDebugBgColorR() / 255f,
                debugManager.getDebugBgColorG() / 255f,
                debugManager.getDebugBgColorB() / 255f,
                1.0f
            );
            
            // Appliquer les paramètres HSL
            backgroundHue = debugManager.getDebugBackgroundHue();
            backgroundSaturation = debugManager.getDebugBackgroundSaturation();
            backgroundLightness = debugManager.getDebugBackgroundLightness();
            
            // Recalculer les dimensions et repositionner les éléments
            calculateBackgroundDimensions();
            updateButtonPositions();
        }
    }
    
    /**
     * Recharge le jeu depuis le fichier games.json après sauvegarde
     */
    private void reloadGameFromJson() {
        try {
            // Recharger les paramètres depuis games.json
            String jsonContent = Gdx.files.local("games.json").readString();
            com.badlogic.gdx.utils.JsonReader jsonReader = new com.badlogic.gdx.utils.JsonReader();
            com.badlogic.gdx.utils.JsonValue root = jsonReader.parse(jsonContent);
            com.badlogic.gdx.utils.JsonValue gamesArray = root.get("games");

            // Trouver le jeu actuel
            com.badlogic.gdx.utils.JsonValue targetGame = null;
            for (com.badlogic.gdx.utils.JsonValue game : gamesArray) {
                if (gameReference.equals(game.getString("reference"))) {
                    targetGame = game;
                    break;
                }
            }
            
            if (targetGame != null) {
                com.badlogic.gdx.utils.JsonValue parameters = targetGame.get("parameters");
                if (parameters != null) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Rechargement des paramètres depuis games.json...");
                    
                    // Recharger TOUS les paramètres depuis le JSON (comme dans le constructeur)
                    if (parameters.has("size")) {
                        gridSize = parameters.getInt("size");
                    }
                    if (parameters.has("bgColor")) {
                        String bgColor = parameters.getString("bgColor");
                        parseBackgroundColor(bgColor);
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouvelle couleur de fond: " + bgColor);
                    }
                    if (parameters.has("shuffle")) {
                        shuffleMoves = parameters.getInt("shuffle");
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouveau nombre de mélanges: " + shuffleMoves);
                    }
                    if (parameters.has("animationSpeed")) {
                        animationSpeed = parameters.getFloat("animationSpeed");
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouvelle vitesse d'animation: " + animationSpeed);
                    }
                    
                    // Paramètres du filtre de couleur du background (manquants dans le reload)
                    if (parameters.has("bgHue")) {
                        float hue = parameters.getFloat("bgHue");
                        backgroundHue = Math.max(0, Math.min(360, hue));
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouvelle teinte de fond: " + backgroundHue);
                    }
                    if (parameters.has("bgSaturation")) {
                        float saturation = parameters.getFloat("bgSaturation");
                        backgroundSaturation = Math.max(0, Math.min(100, saturation));
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouvelle saturation de fond: " + backgroundSaturation);
                    }
                    if (parameters.has("bgLightness")) {
                        float lightness = parameters.getFloat("bgLightness");
                        backgroundLightness = Math.max(-100, Math.min(100, lightness));
                        Gdx.app.log("SlidingPuzzleGameScreen", "Nouvelle luminosité de fond: " + backgroundLightness);
                    }
                    
                    // Réinitialiser l'état du jeu
                    isPuzzleSolved = false;
                    
                    // Utiliser la méthode factorisée pour l'initialisation complète
                    Gdx.app.log("SlidingPuzzleGameScreen", "Relancement de l'initialisation complète du jeu...");
                    initializeGameCompletely();
                    
                    Gdx.app.log("SlidingPuzzleGameScreen", "Jeu rechargé avec les nouveaux paramètres");
                }
            }
        } catch (Exception e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du rechargement depuis games.json: " + e.getMessage());
            // Stack trace logged automatically by Gdx.app.error
        }
    }
    
    /**
     * Parse une couleur de fond au format "r,g,b" et met à jour backgroundColor
     */
    private void parseBackgroundColor(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length == 3) {
                float r = Integer.parseInt(parts[0]) / 255f;
                float g = Integer.parseInt(parts[1]) / 255f;
                float b = Integer.parseInt(parts[2]) / 255f;
                backgroundColor = new Color(r, g, b, 1.0f);
            }
        } catch (NumberFormatException e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Format de couleur invalide: " + colorStr + ", utilisation de la couleur par défaut");
            backgroundColor = new Color(0, 0, 0, 1); // Noir par défaut
        }
    }
    

    @Override
    protected Theme loadTheme(int day) {
        Gdx.app.log("SlidingPuzzleGameScreen", "Chargement de la ressource graphique pour le jour " + day);
        
        // Charger la texture du puzzle
        String fullImagePath = this.theme.getFullImagePath();
        if (fullImagePath == null || fullImagePath.isEmpty()) {
            throw new IllegalStateException("Le chemin d'image du thème est invalide");
        }
        
        try {
            Gdx.app.log("SlidingPuzzleGameScreen", "Chargement de la texture depuis: " + fullImagePath);
            puzzleTexture = new Texture(Gdx.files.internal(fullImagePath));
            fullImageTexture = new Texture(Gdx.files.internal(fullImagePath));
            
            // Maintenant que tous les paramètres sont initialisés et que la texture est chargée,
            // nous pouvons créer les tuiles
            createPuzzleTiles();
        } catch (Exception e) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Erreur lors du chargement de la texture: " + e.getMessage());
            // Stack trace logged automatically by Gdx.app.error
            throw new IllegalStateException("Erreur lors du chargement de la texture: " + e.getMessage(), e);
        }
        
        return this.theme;
    }

    /**
     * Crée les régions de texture pour chaque tuile du puzzle
     */
    private void createPuzzleTiles() {
        if (puzzleTexture == null) {
            throw new IllegalStateException("La texture du puzzle n'a pas été chargée");
        }

        puzzleTiles = new TextureRegion[gridSize * gridSize];
        
        // Récupérer les informations de recadrage
        Theme.CropInfo squareCrop = theme.getSquareCrop();
        if (squareCrop == null) {
            throw new IllegalStateException("Les informations de recadrage ne sont pas disponibles");
        }
        
        // Calculer la taille des tuiles dans la zone recadrée
        int tileWidth = squareCrop.getWidth() / gridSize;
        int tileHeight = squareCrop.getHeight() / gridSize;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col;
                // Inverser la ligne pour correspondre à l'orientation de la grille
                int textureRow = gridSize - 1 - row;
                puzzleTiles[index] = new TextureRegion(puzzleTexture, 
                    squareCrop.getX() + col * tileWidth, 
                    squareCrop.getY() + textureRow * tileHeight, 
                    tileWidth, 
                    tileHeight);
            }
        }
    }

    @Override
    protected void initializeGame() {
        Gdx.app.log("SlidingPuzzleGameScreen", "Initialisation du puzzle coulissant pour le jour " + dayId);

        // Vérifier que la texture est chargée
        if (whiteTexture == null) {
            Gdx.app.error("SlidingPuzzleGameScreen", "ERREUR: Texture du puzzle non chargée");
            return;
        }

        // Calculer la taille des tuiles et l'espacement en fonction de la taille de la fenêtre
        float availableWidth = viewport.getWorldWidth() - (2 * GRID_MARGIN);
        float availableHeight = viewport.getWorldHeight() - (2 * GRID_MARGIN);
        
        // Calculer la taille maximale possible pour une tuile
        float maxTileSize = Math.min(
            availableWidth / (gridSize + (gridSize - 1) * SPACING_RATIO),
            availableHeight / (gridSize + (gridSize - 1) * SPACING_RATIO)
        );
        
        // S'assurer que tileSize reste positif
        if (maxTileSize <= 0) {
            Gdx.app.error("SlidingPuzzleGameScreen", "ATTENTION: Grille trop grande pour l'espace disponible. Utilisation d'une taille minimale.");
            maxTileSize = Math.min(availableWidth, availableHeight) / gridSize;
        }
        
        tileSize = Math.max(maxTileSize, 10.0f); // Taille minimale de 10 pixels
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float totalGridSize = gridSize * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - totalGridSize) / 2;
        float marginY = (viewport.getWorldHeight() - totalGridSize) / 2;

        // Initialiser les positions des tuiles
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int positionIndex = row * gridSize + col;
                gridZones[positionIndex] = new Rectangle(
                    marginX + col * (tileSize + tileSpacing),
                    marginY + row * (tileSize + tileSpacing),
                    tileSize,
                    tileSize
                );
            }
        }

        // Initialiser un état du puzzle résoluble
        initializeSolvablePuzzle();

        // Calculer les durées d'animation
        if (fullImageTexture != null && theme != null && theme.getSquareCrop() != null) {
            animationState.calculateDurations(
                fullImageTexture.getWidth(),
                fullImageTexture.getHeight(),
                theme.getSquareCrop()
            );
        }

        // Positionner les boutons
        updateButtonPositions();
    }

    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le debug manager
        if (debugManager != null) {
            debugManager.update(delta);
        }
        
        if (tileAnimation.isActive()) {
            tileAnimation.update(delta, animationSpeed);
            
            if (!tileAnimation.isActive()) {
                // Animation terminée
                int animatingTileIndex = tileAnimation.getTileIndex();

                // Échanger les tuiles dans l'état du puzzle
                int temp = puzzleState[animatingTileIndex];
                puzzleState[animatingTileIndex] = puzzleState[emptyTileIndex];
                puzzleState[emptyTileIndex] = temp;
                emptyTileIndex = animatingTileIndex;

                // Vérifier si le puzzle est résolu
                boolean solved = true;
                for (int i = 0; i < puzzleState.length; i++) {
                    if (puzzleState[i] != i) {
                        solved = false;
                        break;
                    }
                }

                if (solved) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Puzzle résolu !");
                    isPuzzleSolved = true;
                    animationState.start();
                    
                    // Démarrer le fondu des boutons
                    isButtonsFading = true;
                    buttonsFadeTimer = 0f;
                    
                    // Jouer le son de résolution
                    if (solveSound != null) {
                        solveSound.play();
                    }
                    
                    if (game instanceof AdventCalendarGame) {
                        AdventCalendarGame adventGame = (AdventCalendarGame) game;
                        adventGame.setScore(dayId, 100);
                        adventGame.setVisited(dayId, true);
                    }
                }
            }
        }

        // Mettre à jour l'animation de victoire
        animationState.update(delta);
        animationState.updateVictoryMessageTimer(delta);
        
        // Mettre à jour le fondu des boutons
        if (isButtonsFading) {
            buttonsFadeTimer += delta;
            if (buttonsFadeTimer >= BUTTONS_FADE_DURATION) {
                buttonsFadeTimer = BUTTONS_FADE_DURATION; // Limiter à la durée maximale
            }
        }
    }

    /**
     * Calcule les dimensions et l'échelle du background avec crop pour garder l'aspect ratio
     */
    private void calculateBackgroundDimensions() {
        if (backgroundTexture == null) return;
        
        float screenWidth = DisplayConfig.WORLD_WIDTH;  // Toujours 2048
        float screenHeight = viewport.getWorldHeight(); // Variable selon aspect ratio
        float screenRatio = screenWidth / screenHeight;
        
        float bgRatio = (float)backgroundTexture.getWidth() / backgroundTexture.getHeight();
        
        // Adapter par crop (couvrir tout l'écran, crop le surplus)
        if (screenRatio > bgRatio) {
            // Écran plus large que l'image : fitter en largeur, crop en hauteur
            currentBgWidth = screenWidth;
            currentBgHeight = currentBgWidth / bgRatio;
            currentBgX = 0;
            currentBgY = (screenHeight - currentBgHeight) / 2;
        } else {
            // Écran plus haut que l'image : fitter en hauteur, crop en largeur
            currentBgHeight = screenHeight;
            currentBgWidth = currentBgHeight * bgRatio;
            currentBgX = (screenWidth - currentBgWidth) / 2;
            currentBgY = 0;
        }
        
        currentScaleX = currentBgWidth / backgroundTexture.getWidth();
        currentScaleY = currentBgHeight / backgroundTexture.getHeight();
    }
    
    /**
     * Calcule la position des boutons d'information et close en haut à droite du viewport (comme MastermindGameScreen)
     */
    private void updateButtonPositions() {
        // Obtenir les dimensions du viewport
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        // Taille de base des boutons
        float baseButtonSize = 100f; // Taille un peu plus grande pour une meilleure visibilité
        
        // Marge depuis les bords du viewport
        float marginFromEdge = 30f;
        float spacingBetweenButtons = 30f;
        
        // Position du bouton close (en haut à droite) - c'est le backButton dans MastermindGameScreen
        float closeButtonX = viewportWidth - marginFromEdge - baseButtonSize;
        float closeButtonY = viewportHeight - marginFromEdge - baseButtonSize;
        
        // Position du bouton info (juste en dessous du bouton close)
        float infoButtonX = closeButtonX;
        float infoButtonY = closeButtonY - baseButtonSize - spacingBetweenButtons;
        
        // Positionner les boutons
        infoButton.setSize(baseButtonSize, baseButtonSize);
        infoButton.setPosition(infoButtonX, infoButtonY);
        
        closeButton.setSize(baseButtonSize, baseButtonSize);
        closeButton.setPosition(closeButtonX, closeButtonY);
    }

    @Override
    protected void renderGame() {
        // Calculer l'alpha du background (même timing que les boutons)
        float bgAlpha = isButtonsFading ? (1f - buttonsFadeTimer / BUTTONS_FADE_DURATION) : 1f;
        bgAlpha = Math.max(0f, bgAlpha);
        
        // Toujours dessiner le fond de couleur en premier
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Dessiner le background par-dessus avec fade pendant le fade des boutons
        // Note : on ne teste PAS isComplete() car l'animation de victoire dure plus longtemps que le fade des boutons
        if (backgroundTexture != null && (isButtonsFading || !isPuzzleSolved)) {
            batch.setColor(1, 1, 1, bgAlpha);
            
            // Appliquer le shader HSL pour la colorisation avec dimensions calculées (crop)
            com.widedot.calendar.shaders.HSLShader.begin(batch, backgroundHue, backgroundSaturation, backgroundLightness);
            batch.draw(backgroundTexture, currentBgX, currentBgY, currentBgWidth, currentBgHeight);
            com.widedot.calendar.shaders.HSLShader.end(batch);
            
            // Reset color
            batch.setColor(1, 1, 1, 1);
        }

        // Réinitialiser la couleur avant de dessiner les tuiles
        batch.setColor(Color.WHITE);

        // Dessiner les tuiles du puzzle
        if (puzzleTiles != null) {
            if (animationState.isActive() || animationState.isComplete()) {
                float fadeProgress = animationState.getFadeProgress();
                float mergeProgress = animationState.getMergeProgress();
                float irisOpenProgress = animationState.getIrisOpenProgress();

                // Si nous sommes dans la phase d'ouverture rectangulaire ou l'animation est terminée
                if (irisOpenProgress > 0 && fullImageTexture != null) {
                    renderIrisOpenEffect(irisOpenProgress);
                } else {
                    renderMergingTiles(fadeProgress, mergeProgress);
                }
            } else {
                // Rendu normal du puzzle
                renderNormalPuzzle();
            }
        }

        // Dessiner l'interface de jeu
        drawGameInterface();

        // Dessiner le panneau d'information par-dessus tout le reste s'il est visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
        
        // Display debug interface
        if (debugManager != null) {
            debugManager.drawDebugInfo(batch, viewport.getWorldHeight());
        }
    }

    private void renderIrisOpenEffect(float progress) {
        // Utiliser DisplayConfig.WORLD_WIDTH pour la cohérence (comme MastermindGameScreen)
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        float imageWidth = fullImageTexture.getWidth();
        float imageHeight = fullImageTexture.getHeight();
        float aspectRatio = imageWidth / imageHeight;
        
        // Calculer le scale final pour maximiser l'image dans le viewport (comme MastermindGameScreen)
        float finalImageWidth, finalImageHeight;
        
        // Calculer les dimensions pour remplir l'écran tout en conservant le ratio
        if (screenWidth / aspectRatio <= screenHeight) {
            // L'image sera limitée par la largeur
            finalImageWidth = screenWidth;
            finalImageHeight = finalImageWidth / aspectRatio;
        } else {
            // L'image sera limitée par la hauteur - MAXIMISER LA HAUTEUR
            finalImageHeight = screenHeight;
            finalImageWidth = finalImageHeight * aspectRatio;
        }
        
        float finalScale = finalImageWidth / imageWidth;
        
        Theme.CropInfo squareCrop = theme.getSquareCrop();
        if (squareCrop != null) {
            // Calculer le zoom initial pour correspondre à la taille des tuiles réunies
            float initialScale = (gridSize * tileSize) / squareCrop.getWidth();
            
            // Interpoler le zoom entre la valeur initiale et finale
            float currentScale = initialScale + (finalScale - initialScale) * progress;
            
            // Calculer les dimensions avec le zoom actuel
            float scaledWidth = imageWidth * currentScale;
            float scaledHeight = imageHeight * currentScale;
            
            // Centrer l'image à l'écran
            float x = (screenWidth - scaledWidth) / 2;
            float y = (screenHeight - scaledHeight) / 2;

            // Calculer les dimensions de la zone recadrée dans l'image complète
            float cropX = squareCrop.getX() * currentScale;
            float cropY = squareCrop.getY() * currentScale;
            float cropWidth = squareCrop.getWidth() * currentScale;
            float cropHeight = squareCrop.getHeight() * currentScale;

            // Calculer les dimensions de l'ouverture rectangulaire
            float openWidth = cropWidth + (scaledWidth - cropWidth) * progress;
            float openHeight = cropHeight + (scaledHeight - cropHeight) * progress;
            float openX = x + (scaledWidth - openWidth) / 2;
            float openY = y + (scaledHeight - openHeight) / 2;

            // Dessiner d'abord le fond avec la couleur définie durant le jeu
            batch.setColor(backgroundColor);
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);

            // Dessiner l'image complète avec l'effet d'ouverture
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fullImageTexture, 
                openX, openY, openWidth, openHeight,
                (int)(squareCrop.getX() - (openWidth - cropWidth) / (2 * currentScale)),
                (int)(squareCrop.getY() - (openHeight - cropHeight) / (2 * currentScale)),
                (int)(openWidth / currentScale),
                (int)(openHeight / currentScale),
                false, false);
        }
        
        // Afficher l'interface de debug
        if (debugManager != null) {
            debugManager.drawDebugInfo(batch, viewport.getWorldHeight());
        }
    }

    private void renderMergingTiles(float fadeProgress, float mergeProgress) {
        // Vérifications de sécurité
        if (emptyTileIndex < 0 || emptyTileIndex >= puzzleTiles.length || emptyTileIndex >= gridZones.length) {
            Gdx.app.error("SlidingPuzzleGameScreen", "emptyTileIndex hors limites dans renderMergingTiles: " + emptyTileIndex + " (puzzleTiles.length=" + puzzleTiles.length + ", gridZones.length=" + gridZones.length + ")");
            return;
        }
        
        // Dessiner la case vide avec fade in
        if (fadeProgress < 1f) {
            batch.setColor(1f, 1f, 1f, fadeProgress);
            batch.draw(puzzleTiles[emptyTileIndex], 
                gridZones[emptyTileIndex].x, 
                gridZones[emptyTileIndex].y, 
                gridZones[emptyTileIndex].width, 
                gridZones[emptyTileIndex].height);
        } else if (mergeProgress < 1f) {
            renderMergingTile(emptyTileIndex, mergeProgress);
        } else {
            renderFinalTile(emptyTileIndex);
        }
        
        // Dessiner toutes les tuiles avec l'espacement réduit
        batch.setColor(Color.WHITE);
        for (int i = 0; i < gridZones.length; i++) {
            if (i != emptyTileIndex && i < puzzleTiles.length) {
                if (mergeProgress < 1f) {
                    renderMergingTile(i, mergeProgress);
                } else {
                    renderFinalTile(i);
                }
            }
        }
    }

    private void renderMergingTile(int index, float mergeProgress) {
        // Vérifications de sécurité
        if (index < 0 || index >= puzzleState.length || index >= gridZones.length) {
            Gdx.app.error("SlidingPuzzleGameScreen", "Index hors limites dans renderMergingTile: " + index + " (puzzleState.length=" + puzzleState.length + ", gridZones.length=" + gridZones.length + ")");
            return;
        }
        
        int tileNumber = puzzleState[index];
        float x = gridZones[index].x;
        float y = gridZones[index].y;
        
        // Calculer la position finale (sans espacement)
        float gridX = gridZones[0].x;
        float gridY = gridZones[0].y;
        float finalX = gridX + (index % gridSize) * tileSize;
        float finalY = gridY + (index / gridSize) * tileSize;
        
        // Calculer le décalage pour maintenir l'image centrée
        float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float finalGridWidth = gridSize * tileSize;
        float finalGridHeight = gridSize * tileSize;
        
        // Calculer le décalage pour maintenir le centre
        float offsetX = (totalGridWidth - finalGridWidth) / 2;
        float offsetY = (totalGridHeight - finalGridHeight) / 2;
        
        // Interpoler la position en tenant compte du décalage
        x = x + (finalX - x + offsetX) * mergeProgress;
        y = y + (finalY - y + offsetY) * mergeProgress;
        
        batch.draw(puzzleTiles[tileNumber], x, y, tileSize, tileSize);
    }

    private void renderFinalTile(int index) {
        int tileNumber = puzzleState[index];
        float gridX = gridZones[0].x;
        float gridY = gridZones[0].y;
        float finalX = gridX + (index % gridSize) * tileSize;
        float finalY = gridY + (index / gridSize) * tileSize;
        
        // Calculer le décalage final
        float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float finalGridWidth = gridSize * tileSize;
        float finalGridHeight = gridSize * tileSize;
        float offsetX = (totalGridWidth - finalGridWidth) / 2;
        float offsetY = (totalGridHeight - finalGridHeight) / 2;
        
        batch.draw(puzzleTiles[tileNumber], finalX + offsetX, finalY + offsetY, tileSize, tileSize);
    }

    private void renderNormalPuzzle() {
        // Calculer le nombre total de tuiles pour la grille actuelle
        int totalTiles = gridSize * gridSize;
        
        for (int i = 0; i < Math.min(totalTiles, gridZones.length); i++) {
            if (i != emptyTileIndex && (!tileAnimation.isActive() || i != tileAnimation.getTileIndex())) {
                // Vérifications de sécurité
                if (i >= puzzleState.length || i >= puzzleTiles.length) {
                    Gdx.app.error("SlidingPuzzleGameScreen", "Index hors limites dans renderNormalPuzzle: " + i + " (puzzleState.length=" + puzzleState.length + ", puzzleTiles.length=" + puzzleTiles.length + ")");
                    continue;
                }
                
                int tileNumber = puzzleState[i];
                if (tileNumber < 0 || tileNumber >= puzzleTiles.length) {
                    Gdx.app.error("SlidingPuzzleGameScreen", "tileNumber hors limites: " + tileNumber + " (puzzleTiles.length=" + puzzleTiles.length + ")");
                    continue;
                }
                
                batch.draw(puzzleTiles[tileNumber], 
                    gridZones[i].x, 
                    gridZones[i].y, 
                    gridZones[i].width, 
                    gridZones[i].height);
            }
        }

        // Dessiner la tuile en cours d'animation
        if (tileAnimation.isActive()) {
            int animTileIndex = tileAnimation.getTileIndex();
            // Vérifications de sécurité
            if (animTileIndex >= 0 && animTileIndex < puzzleState.length && animTileIndex < puzzleTiles.length) {
                int tileNumber = puzzleState[animTileIndex];
                if (tileNumber >= 0 && tileNumber < puzzleTiles.length) {
                    Vector3 currentPos = tileAnimation.getCurrentPosition();
                    batch.draw(puzzleTiles[tileNumber], currentPos.x, currentPos.y, tileSize, tileSize);
                }
            }
        }
    }

    /**
     * Crée l'input processor pour gérer les clics et raccourcis clavier
     */
    private void createInputProcessor() {
        Gdx.app.log("SlidingPuzzleGameScreen", "Début de création de l'InputAdapter pour SlidingPuzzle...");
        inputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Désactiver les entrées pendant l'animation ou les transitions
                if (tileAnimation.isActive() || TransitionScreen.isTransitionActive()) {
                    return true;
                }
                
                Vector3 worldCoords = new Vector3(screenX, screenY, 0);
                viewport.unproject(worldCoords);
                handleClick(worldCoords.x, worldCoords.y);
                return true;
            }
            
            @Override
            public boolean keyDown(int keycode) {
                Gdx.app.log("SlidingPuzzleGameScreen", "Touche pressée: " + keycode + " (D=" + Input.Keys.D + ")");
                // Gestion du debug
                if (debugManager != null && debugManager.handleKeyDown(keycode)) {
                    return true;
                }
                
                // Alt+R : Résoudre automatiquement le jeu (mode test uniquement)
                if (keycode == Input.Keys.R && isTestMode && !isPuzzleSolved && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    solveGameWithKeyboard();
                    return true;
                }
                
                // Alt+N : Déclencher la phase de victoire (mode test uniquement)
                if (keycode == Input.Keys.N && isTestMode && !isPuzzleSolved && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    triggerVictoryPhase();
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean keyUp(int keycode) {
                // Gestion du debug
                if (debugManager != null && debugManager.handleKeyUp(keycode)) {
                    return true;
                }
                return false;
            }
        };
        Gdx.app.log("SlidingPuzzleGameScreen", "InputAdapter créé avec succès pour SlidingPuzzle");
    }
    
    @Override
    protected void onScreenActivated() {
        super.onScreenActivated();
        
        Gdx.app.log("SlidingPuzzleGameScreen", "onScreenActivated appelé");
        // Initialiser le système de debug après que l'écran soit complètement initialisé
        initializeDebugManager();
        
        // Activer l'input processor quand l'écran devient actif
        Gdx.input.setInputProcessor(inputProcessor);
    }
    
    @Override
    protected void onScreenDeactivated() {
        super.onScreenDeactivated();
        // Désactiver l'input processor quand l'écran devient inactif
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    protected void handleInput() {
        // La gestion des entrées est déléguée à l'InputProcessor créé dans createInputProcessor()
        // Cette méthode peut rester vide car toutes les interactions sont gérées via les clics et raccourcis clavier
    }
    
    /**
     * Gère les clics de souris/tactiles
     */
    private void handleClick(float worldX, float worldY) {
        if (true) {
            Gdx.app.log("SlidingPuzzleGameScreen", "Clic détecté dans SlidingPuzzleGameScreen");
            Gdx.app.log("SlidingPuzzleGameScreen", "Position du clic: (" + worldX + ", " + worldY + ")");

            // Si le puzzle est résolu, retourner au menu sur n'importe quel clic
            if (isPuzzleSolved) {
                Gdx.app.log("SlidingPuzzleGameScreen", "Retour au menu après victoire");
                returnToMainMenu();
                return;
            }

            // Si le panneau d'info est visible, n'importe quel clic le ferme
            if (showInfoPanel) {
                Gdx.app.log("SlidingPuzzleGameScreen", "Fermeture du panneau d'info");
                showInfoPanel = false;
                return;
            }

            // Désactiver les clics sur les boutons s'ils sont en train de disparaître
            if (!isButtonsFading) {
                if (closeButton.contains(worldX, worldY)) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Bouton Close cliqué");
                    returnToMainMenu();
                } else if (infoButton.contains(worldX, worldY)) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Bouton Info cliqué");
                    showInfoPanel = true;
                } else {
                    handleTileClick(worldX, worldY);
                }
            } else {
                // Si les boutons sont en train de disparaître, gérer seulement les clics sur les tuiles
                handleTileClick(worldX, worldY);
            }
        }
    }
    
    /**
     * Gère les clics sur les tuiles du puzzle
     */
    private void handleTileClick(float worldX, float worldY) {
        // Gérer le déplacement des tuiles
        for (int positionIndex = 0; positionIndex < gridZones.length; positionIndex++) {
            if (gridZones[positionIndex].contains(worldX, worldY)) {
                // Ignorer le clic si c'est sur la case vide
                if (positionIndex == emptyTileIndex) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Clic sur la case vide (position " + positionIndex + ") - Ignoré");
                    break;
                }

                int tileNumber = puzzleState[positionIndex];
                Gdx.app.log("SlidingPuzzleGameScreen", "Tuile numéro " + tileNumber + " cliquée (position " + positionIndex + ")");
                Gdx.app.log("SlidingPuzzleGameScreen", "Case vide actuelle: position " + emptyTileIndex);

                if (isAdjacent(positionIndex, emptyTileIndex)) {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Déplacement de la tuile " + tileNumber + " (position " + positionIndex + ") vers la case vide (position " + emptyTileIndex + ")");

                    // Jouer le son de déplacement
                    if (slidingSound != null) {
                        Gdx.app.log("SlidingPuzzleGameScreen", "Son de déplacement joué");
                        slidingSound.play();
                    }
                    else {
                        Gdx.app.log("SlidingPuzzleGameScreen", "ERREUR: slidingSound est null");
                    }

                    // Initialiser l'animation
                    Vector3 start = new Vector3(gridZones[positionIndex].x, gridZones[positionIndex].y, 0);
                    Vector3 end = new Vector3(gridZones[emptyTileIndex].x, gridZones[emptyTileIndex].y, 0);
                    tileAnimation.start(positionIndex, start, end);
                } else {
                    Gdx.app.log("SlidingPuzzleGameScreen", "Déplacement impossible - Tuile " + tileNumber + " (position " + positionIndex + ") non adjacente à la case vide (position " + emptyTileIndex + ")");
                }
                break;
            }
        }
    }
    
    /**
     * Résout le puzzle automatiquement via raccourci clavier R (mode test uniquement)
     */
    private void solveGameWithKeyboard() {
        Gdx.app.log("SlidingPuzzleGameScreen", "Résolution automatique du puzzle coulissant via touche R (mode test)");
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, 100);
            adventGame.setVisited(dayId, true);
        }
        
        // Jouer le son de résolution
        if (solveSound != null) {
            solveSound.play();
        }
        
        // Retourner au menu immédiatement
        returnToMainMenu();
    }
    
    /**
     * Déclenche la phase de victoire via raccourci clavier N (mode test uniquement)
     */
    private void triggerVictoryPhase() {
        Gdx.app.log("SlidingPuzzleGameScreen", "Déclenchement de la phase de victoire du puzzle coulissant via touche N (mode test)");
        
        // Simuler une victoire comme si le puzzle venait d'être résolu
        isPuzzleSolved = true;
        animationState.start();
        
        // Démarrer le fondu des boutons
        isButtonsFading = true;
        buttonsFadeTimer = 0f;
        
        // Jouer le son de résolution
        if (solveSound != null) {
            solveSound.play();
        }
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, 100);
            adventGame.setVisited(dayId, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        whiteTexture.dispose();
        if (puzzleTexture != null) {
            puzzleTexture.dispose();
        }
        if (fullImageTexture != null) {
            fullImageTexture.dispose();
        }
        if (infoButtonTexture != null) {
            infoButtonTexture.dispose();
        }
        if (closeButtonTexture != null) {
            closeButtonTexture.dispose();
        }
        if (solveSound != null) {
            solveSound.dispose();
        }
        if (slidingSound != null) {
            slidingSound.dispose();
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }

    /**
     * Dessine l'interface de jeu (boutons, etc.)
     */
    private void drawGameInterface() {
        // Dessiner les boutons avec leurs textures (comme MastermindGameScreen)
        drawCloseButton();
        drawInfoButton();
    }
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
            // Calculer l'alpha pour le fondu
            float alpha = getButtonAlpha();
            
            // Dessiner l'image du bouton info avec l'alpha approprié
            batch.setColor(1f, 1f, 1f, alpha);
            
            // Utiliser la taille du rectangle du bouton (définie dans updateButtonPositions)
            float buttonWidth = infoButton.width;
            float buttonHeight = infoButton.height;
            
            // Dessiner le bouton à sa position
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, buttonWidth, buttonHeight);
        }
    }
    
    private void drawCloseButton() {
        if (closeButtonTexture != null) {
            // Calculer l'alpha pour le fondu
            float alpha = getButtonAlpha();
            
            // Dessiner l'image du bouton close avec l'alpha approprié
            batch.setColor(1f, 1f, 1f, alpha);
            
            // Utiliser la taille du rectangle du bouton (définie dans updateButtonPositions)
            float buttonWidth = closeButton.width;
            float buttonHeight = closeButton.height;
            
            // Dessiner le bouton à sa position
            batch.draw(closeButtonTexture, closeButton.x, closeButton.y, buttonWidth, buttonHeight);
        }
    }
    
    /**
     * Calcule l'alpha des boutons en fonction de l'état du fondu
     */
    private float getButtonAlpha() {
        if (!isButtonsFading) {
            return 1f; // Boutons complètement visibles
        }
        
        // Calculer le progrès du fondu (0 = visible, 1 = invisible)
        float fadeProgress = buttonsFadeTimer / BUTTONS_FADE_DURATION;
        fadeProgress = Math.min(1f, fadeProgress); // Limiter à 1
        
        // Retourner l'alpha inversé (1 - progress pour aller de visible à invisible)
        return 1f - fadeProgress;
    }
    
    /**
     * Dessine le panneau d'information (comme MastermindGameScreen)
     */
    private void drawInfoPanel() {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        // Fond semi-transparent pour conserver la visibilité du jeu
        batch.setColor(0, 0, 0, 0.4f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
        
        // Calculer la taille adaptative du panneau
        float minPanelWidth = Math.max(400, screenWidth * 0.5f);
        float maxPanelWidth = Math.min(600, screenWidth * 0.9f);
        float panelWidth = Math.min(maxPanelWidth, minPanelWidth);
        
        float minPanelHeight = Math.max(300, screenHeight * 0.4f);
        float maxPanelHeight = Math.min(500, screenHeight * 0.8f);
        float panelHeight = Math.min(maxPanelHeight, minPanelHeight);
        
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;
        
        // Fond du panneau avec coins arrondis simulés
        batch.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);
        
        // Bordure du panneau avec effet d'ombre
        float shadowOffset = 3;
        batch.setColor(0, 0, 0, 0.3f);
        batch.draw(whiteTexture, panelX + shadowOffset, panelY - shadowOffset, panelWidth, panelHeight);
        
        // Bordure principale
        batch.setColor(0.3f, 0.4f, 0.7f, 1);
        float borderWidth = 2;
        // Haut
        batch.draw(whiteTexture, panelX, panelY + panelHeight - borderWidth, panelWidth, borderWidth);
        // Bas
        batch.draw(whiteTexture, panelX, panelY, panelWidth, borderWidth);
        // Gauche
        batch.draw(whiteTexture, panelX, panelY, borderWidth, panelHeight);
        // Droite
        batch.draw(whiteTexture, panelX + panelWidth - borderWidth, panelY, borderWidth, panelHeight);
        
        // Dessiner les informations du tableau
        float textMargin = 20;
        float titleY = panelY + panelHeight - textMargin;
        float artistY = titleY - 40;
        float yearY = artistY - 40;
        float descriptionY = yearY - 80;

        font.setColor(0.2f, 0.3f, 0.8f, 1);
        
        // Titre
        layout.setText(font, theme.getTitle(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(titleY));

        // Artiste
        font.setColor(0.1f, 0.1f, 0.2f, 1);
        layout.setText(font, theme.getArtist(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(artistY));

        // Année
        layout.setText(font, String.valueOf(theme.getYear()), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(yearY));

        // Description
        layout.setText(font, theme.getDescription(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, true);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(descriptionY));
        
        // Indicateur de fermeture
        font.setColor(0.5f, 0.5f, 0.6f, 1);
        String closeHint = "Tapez pour fermer";
        layout.setText(font, closeHint);
        CarlitoFontManager.drawText(batch, layout, 
            panelX + panelWidth - layout.width - 10,
            panelY + 15);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        currentWidth = width;
        currentHeight = height;
        
        // Recalculer les dimensions du background (comme MastermindGameScreen)
        calculateBackgroundDimensions();
        
        // Recalculer la taille des tuiles et l'espacement
        float availableWidth = viewport.getWorldWidth() - (2 * GRID_MARGIN);
        float availableHeight = viewport.getWorldHeight() - (2 * GRID_MARGIN);
        
        float maxTileSize = Math.min(
            availableWidth / (gridSize + (gridSize - 1) * SPACING_RATIO),
            availableHeight / (gridSize + (gridSize - 1) * SPACING_RATIO)
        );
        
        tileSize = maxTileSize;
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float totalGridSize = gridSize * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - totalGridSize) / 2;
        float marginY = (viewport.getWorldHeight() - totalGridSize) / 2;

        // Mettre à jour les positions des tuiles
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int positionIndex = row * gridSize + col;
                // Vérification de sécurité pour éviter les erreurs d'index
                if (positionIndex < gridZones.length) {
                    gridZones[positionIndex].set(
                        marginX + col * (tileSize + tileSpacing),
                        marginY + row * (tileSize + tileSpacing),
                        tileSize,
                        tileSize
                    );
                } else {
                    Gdx.app.error("SlidingPuzzleGameScreen", "Index hors limites dans resize: " + positionIndex + " (gridZones.length=" + gridZones.length + ", gridSize=" + gridSize + ")");
                }
            }
        }

        // Mettre à jour les positions des boutons (comme MastermindGameScreen)
        updateButtonPositions();

        Gdx.app.log("SlidingPuzzleGameScreen", "Redimensionnement: " + width + "x" + height);
        Gdx.app.log("SlidingPuzzleGameScreen", "Viewport: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
        Gdx.app.log("SlidingPuzzleGameScreen", "Taille des tuiles: " + tileSize);
        Gdx.app.log("SlidingPuzzleGameScreen", "Position de la caméra: (" + viewport.getCamera().position.x + ", " + viewport.getCamera().position.y + ")");
    }
}
