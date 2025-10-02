package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.Config;
import com.widedot.calendar.display.DisplayConfig;
import com.widedot.calendar.shaders.CrystallizeShader;
import com.widedot.calendar.debug.CrystallizeDebugManager;
import com.widedot.calendar.config.DayMappingManager;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Jeu de devinette d'image avec effet de cristallisation
 * L'image est masquée par un filtre "crystalize" qui se réduit à chaque tentative
 */
public class CrystalizeGuessGameScreen extends GameScreen {
    // Input processor pour les clics et raccourcis clavier
    private InputAdapter inputProcessor;
    
    // UI
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout;
    private final Rectangle submitButton;
    private final Rectangle closeButton;
    private final Rectangle infoButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture closeButtonTexture;
    private Texture infoButtonTexture;
    private boolean showInfoPanel;
    
    // Game data
    private int maxAttempts;
    private int currentAttempt;
    private int initialCrystalSize;
    private boolean gameWon;
    private boolean gameFinished;
    private Theme theme;
    
    // Image textures
    private Texture originalImageTexture;
    private Texture currentCrystalizedTexture; // Texture actuellement affichée
    
    // Shader et rendu
    private CrystallizeShader crystallizeShader;
    
    // Système de debug
    private CrystallizeDebugManager debugManager;
    private ObjectMap<String, Object> gameParameters;
    private String gameReference;
    
    // Animation
    private boolean isAnimating;
    private float animationTime;
    private float animationDuration = 1.5f; // Durée de l'animation en secondes
    private int startCrystalSize;
    private int endCrystalSize;
    private Texture animatedTexture;
    
    // Paramètres du shader (fidèles à JHLabs)
    private float currentCrystalSize;
    private float currentRandomness = 0.0f;  // Par défaut dans JHLabs
    private float currentEdgeThickness = 0.4f;  // Par défaut dans JHLabs
    private float currentStretch = 1.0f;  // Par défaut dans JHLabs
    private boolean currentFadeEdges = false;  // Par défaut dans JHLabs
    private float currentEdgeColorR = 0.0f;
    private float currentEdgeColorG = 0.0f;
    private float currentEdgeColorB = 0.0f;
    private float currentEdgeColorA = 1.0f;
    
    
    
    // Sons
    private Sound winSound;
    private Sound wrongSound;
    private Sound failedSound;
    
    // Constants
    private static final String WIN_SOUND_PATH = "audio/win.mp3";
    private static final String WRONG_SOUND_PATH = "audio/wrong.wav";
    private static final String FAILED_SOUND_PATH = "audio/failed.wav";
    
    /**
     * Constructeur avec paramètres dynamiques
     */
    public CrystalizeGuessGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        this.theme = theme;
        this.gameParameters = parameters; // Stocker les paramètres pour le debug
        
        // Obtenir la référence du jeu pour la sauvegarde
        this.gameReference = DayMappingManager.getInstance().getGameReferenceForDay(dayId);
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.maxAttempts = 5;
        this.initialCrystalSize = 150; // Plus gros pour rendre le jeu plus difficile
        this.backgroundColor = new Color(0, 0, 0, 1);
        
        // Vérifier si on est en mode test
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        System.out.println("Mode test CrystalizeGuess: " + isTestMode);
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("maxAttempts")) {
                this.maxAttempts = ((Number) parameters.get("maxAttempts")).intValue();
            }
            if (parameters.containsKey("initialCrystalSize")) {
                this.initialCrystalSize = ((Number) parameters.get("initialCrystalSize")).intValue();
            }
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String) parameters.get("bgColor");
                this.backgroundColor = parseColor(bgColor);
            }
            
            // Charger les paramètres du shader
            if (parameters.containsKey("randomness")) {
                this.currentRandomness = ((Number) parameters.get("randomness")).floatValue();
            }
            if (parameters.containsKey("edgeThickness")) {
                this.currentEdgeThickness = ((Number) parameters.get("edgeThickness")).floatValue();
            }
            if (parameters.containsKey("stretch")) {
                this.currentStretch = ((Number) parameters.get("stretch")).floatValue();
            }
            if (parameters.containsKey("fadeEdges")) {
                this.currentFadeEdges = (Boolean) parameters.get("fadeEdges");
            }
            if (parameters.containsKey("edgeColor")) {
                String edgeColorStr = (String) parameters.get("edgeColor");
                parseEdgeColorFromString(edgeColorStr);
            }
            
            System.out.println("Paramètres du shader chargés:");
            System.out.println("  - Randomness: " + currentRandomness);
            System.out.println("  - EdgeThickness: " + currentEdgeThickness);
            System.out.println("  - Stretch: " + currentStretch);
            System.out.println("  - FadeEdges: " + currentFadeEdges);
            System.out.println("  - EdgeColor: R=" + currentEdgeColorR + " G=" + currentEdgeColorG + " B=" + currentEdgeColorB + " A=" + currentEdgeColorA);
        }
        
        // Initialiser les éléments UI
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.bigFont = new BitmapFont();
        this.bigFont.getData().setScale(2.0f);
        this.layout = new GlyphLayout();
        
        this.submitButton = new Rectangle(0, 0, 200, 80);
        this.closeButton = new Rectangle(0, 0, 100, 100);
        this.infoButton = new Rectangle(0, 0, 100, 100);
        
        // Créer texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        
        // Charger les textures des boutons
        try {
            this.closeButtonTexture = new Texture(Gdx.files.internal("images/ui/close.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton close: " + e.getMessage());
            this.closeButtonTexture = null;
        }
        
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }
        
        // Charger les sons
        loadSounds();
        
        // Initialiser l'état du jeu
        this.currentAttempt = 0;
        this.gameWon = false;
        this.gameFinished = false;
        this.showInfoPanel = false;
        
        // Créer l'input processor
        createInputProcessor();
        
        // Initialiser le système de debug
        initializeDebugManager();
        
        // Mettre à jour les positions des boutons
        updateButtonPositions();
        
        // Le shader sera initialisé plus tard quand le viewport sera prêt
        
        System.out.println("Constructeur CrystalizeGuessGameScreen terminé");
    }
    
    @Override
    protected Theme loadTheme(int day) {
        System.out.println("Chargement de la texture pour le jour " + day);
        
        String fullImagePath = theme.getFullImagePath();
        if (fullImagePath == null || fullImagePath.isEmpty()) {
            throw new IllegalStateException("Le chemin d'image du thème est invalide");
        }
        
        try {
            // Charger l'image originale
            originalImageTexture = new Texture(Gdx.files.internal(fullImagePath));
            
            // Initialiser avec l'image la plus cristallisée
            initializeGameWithCrystalizedImage(fullImagePath);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la texture: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Erreur lors du chargement de la texture", e);
        }
        
        return theme;
    }
    
    /**
     * Initialise le système de debug
     */
    private void initializeDebugManager() {
        debugManager = new CrystallizeDebugManager(font);
        debugManager.setChangeCallback(new CrystallizeDebugManager.DebugChangeCallback() {
            @Override
            public void onDebugParameterChanged() {
                applyDebugShader();
            }
        });
        
        // Définir la référence du jeu pour la sauvegarde
        debugManager.setCurrentGameReference(gameReference);
        
        // Initialiser le debug avec les paramètres du jeu
        debugManager.initializeFromGameParameters(gameParameters);
    }
    
    /**
     * Initialise le shader de cristallisation
     */
    private void initializeCrystallizeShader() {
        System.out.println("Initialisation du shader de cristallisation");
        
        try {
            // Créer le shader
            crystallizeShader = new CrystallizeShader();
            System.out.println("Shader de cristallisation initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du shader: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    /**
     * Initialise le jeu avec l'image la plus cristallisée
     */
    private void initializeGameWithCrystalizedImage(String imagePath) {
        System.out.println("=== INITIALISATION du jeu avec image cristallisée ===");
        
        try {
            System.out.println("Chargement de l'image: " + imagePath);
            
            // Appliquer le shader le plus fort (tentative 0) seulement si le shader est initialisé
            if (crystallizeShader != null) {
                System.out.println("Application du shader initial (cristallisation maximale)...");
                currentCrystalizedTexture = applyCrystallizeShader(originalImageTexture, initialCrystalSize);
                System.out.println("Shader appliqué avec succès");
            } else {
                System.out.println("Shader pas encore initialisé, utilisation de l'image originale");
                currentCrystalizedTexture = originalImageTexture;
            }
            
            System.out.println("=== INITIALISATION TERMINÉE ===");
            
        } catch (Exception e) {
            System.err.println("=== ERREUR INITIALISATION ===");
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
            
            // En cas d'erreur, utiliser l'image originale
            System.out.println("Utilisation de l'image originale comme fallback");
            currentCrystalizedTexture = originalImageTexture;
        }
    }
    
    /**
     * Applique l'effet de cristallisation avec le shader
     */
    private Texture applyCrystallizeShader(Texture sourceTexture, float crystalSize) {
        
        if (crystalSize <= 1 || crystallizeShader == null) {
            System.out.println("CrystalSize <= 1 ou shader non initialisé, retour de l'image originale");
            return sourceTexture;
        }
        
        try {
            return renderWithShader(sourceTexture, crystalSize);
        } catch (Exception e) {
            System.err.println("ERREUR dans applyCrystallizeShader: " + e.getMessage());
            e.printStackTrace();
            return sourceTexture;
        }
    }
    
    /**
     * Effectue le rendu avec le shader de cristallisation
     */
    private Texture renderWithShader(Texture sourceTexture, float crystalSize) {
        int width = sourceTexture.getWidth();
        int height = sourceTexture.getHeight();
        
        // Utiliser la résolution originale de l'image
        int renderWidth = width;
        int renderHeight = height;

        FrameBuffer tempFrameBuffer = new FrameBuffer(com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888, renderWidth, renderHeight, false);

        // -- Sauvegarde de l'état du batch
        final com.badlogic.gdx.graphics.glutils.ShaderProgram prevShader = batch.getShader();
        final com.badlogic.gdx.math.Matrix4 prevProj = new com.badlogic.gdx.math.Matrix4(batch.getProjectionMatrix());
        final com.badlogic.gdx.math.Matrix4 prevTrans = new com.badlogic.gdx.math.Matrix4(batch.getTransformMatrix());

        // -- Projection "pixel-perfect" pour le FBO
        com.badlogic.gdx.math.Matrix4 fboProj = new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, renderWidth, renderHeight);

        tempFrameBuffer.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        try {
            // Applique la projection FBO
            batch.setProjectionMatrix(fboProj);
            batch.setTransformMatrix(new com.badlogic.gdx.math.Matrix4().idt());

            // Configure + utilise le shader
            batch.setShader(crystallizeShader.getShader());
            batch.begin();

            // Configuration complète du shader (fidèle à JHLabs)
            crystallizeShader.setCrystalSize(crystalSize);
            
            // Utiliser les paramètres de debug si en mode debug, sinon les paramètres normaux
            if (debugManager != null && debugManager.isDebugMode()) {
                debugManager.applyDebugParameters(crystallizeShader, crystalSize);
            } else {
                crystallizeShader.setRandomness(currentRandomness);
                crystallizeShader.setEdgeThickness(currentEdgeThickness);
                crystallizeShader.setStretch(currentStretch);
                crystallizeShader.setEdgeColor(currentEdgeColorR, currentEdgeColorG, currentEdgeColorB, currentEdgeColorA);
                crystallizeShader.setFadeEdges(currentFadeEdges);
            }
            crystallizeShader.setResolution(renderWidth, renderHeight);

            // Dessin dans le FBO
            batch.draw(sourceTexture, 0, 0, renderWidth, renderHeight);

            batch.end();
        } finally {
            // Restaure l'état du batch
            batch.setShader(prevShader);
            batch.setProjectionMatrix(prevProj);
            batch.setTransformMatrix(prevTrans);
            tempFrameBuffer.end();
        }

        // Important : le color buffer est retourné "tel quel".
        // Utiliser Nearest pour une netteté maximale :
        Texture out = tempFrameBuffer.getColorBufferTexture();
        out.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest, com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest);
        return out;
    }
    
    
    
    
    
    private Color parseColor(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length == 3) {
                float r = Integer.parseInt(parts[0]) / 255f;
                float g = Integer.parseInt(parts[1]) / 255f;
                float b = Integer.parseInt(parts[2]) / 255f;
                return new Color(r, g, b, 1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Format de couleur invalide: " + colorStr);
        }
        return new Color(0, 0, 0, 1);
    }
    
    /**
     * Parse une couleur au format "r,g,b,a" et met à jour les paramètres RGBA
     */
    private void parseEdgeColorFromString(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length == 4) {
                currentEdgeColorR = Float.parseFloat(parts[0]);
                currentEdgeColorG = Float.parseFloat(parts[1]);
                currentEdgeColorB = Float.parseFloat(parts[2]);
                currentEdgeColorA = Float.parseFloat(parts[3]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Format de couleur invalide: " + colorStr + ", utilisation des valeurs par défaut");
            currentEdgeColorR = 0.0f;
            currentEdgeColorG = 0.0f;
            currentEdgeColorB = 0.0f;
            currentEdgeColorA = 1.0f;
        }
    }
    
    private void loadSounds() {
        try {
            winSound = Gdx.audio.newSound(Gdx.files.internal(WIN_SOUND_PATH));
            wrongSound = Gdx.audio.newSound(Gdx.files.internal(WRONG_SOUND_PATH));
            failedSound = Gdx.audio.newSound(Gdx.files.internal(FAILED_SOUND_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sons: " + e.getMessage());
        }
    }
    
    private void createInputProcessor() {
        inputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (TransitionScreen.isTransitionActive()) {
                    return true;
                }
                
                Vector3 worldCoords = new Vector3(screenX, screenY, 0);
                viewport.unproject(worldCoords);
                handleClick(worldCoords.x, worldCoords.y);
                return true;
            }
            
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.R && isTestMode && !gameFinished) {
                    solveGame();
                    return true;
                }
                
                if (keycode == Input.Keys.N && isTestMode && !gameFinished) {
                    revealMore();
                    return true;
                }
                
                // Déléguer la gestion du debug au debug manager
                if (debugManager.handleKeyDown(keycode)) {
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean keyUp(int keycode) {
                // Déléguer la gestion du debug au debug manager
                if (debugManager.handleKeyUp(keycode)) {
                    return true;
                }
                return false;
            }
        };
    }
    
    @Override
    protected void onScreenActivated() {
        super.onScreenActivated();
        Gdx.input.setInputProcessor(inputProcessor);
    }
    
    @Override
    protected void onScreenDeactivated() {
        super.onScreenDeactivated();
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    protected void initializeGame() {
        System.out.println("Initialisation du jeu CrystalizeGuess pour le jour " + dayId);
        updateButtonPositions();
        
        // Initialiser le shader maintenant que le viewport est prêt
        initializeCrystallizeShader();
        
        // Appliquer le shader initial après l'initialisation
        if (originalImageTexture != null) {
            System.out.println("Application du shader initial après initialisation...");
            currentCrystalizedTexture = applyCrystallizeShader(originalImageTexture, initialCrystalSize);
        }
    }
    
    @Override
    protected void handleInput() {
        // Géré par l'InputProcessor
    }
    
    private void handleClick(float x, float y) {
        // Si le panneau d'info est visible, le fermer
        if (showInfoPanel) {
            showInfoPanel = false;
            return;
        }
        
        // Si le jeu est terminé, retourner au menu
        if (gameFinished) {
            returnToMainMenu();
            return;
        }
        
        // Vérifier les boutons
        if (closeButton.contains(x, y)) {
            returnToMainMenu();
            return;
        }
        
        if (infoButton.contains(x, y)) {
            showInfoPanel = true;
            return;
        }
        
        if (submitButton.contains(x, y) && !gameFinished) {
            makeGuess();
            return;
        }
    }
    
    private void makeGuess() {
        if (gameFinished || isAnimating) return;
        
        currentAttempt++;
        
        // Démarrer l'animation de cristallisation
        startCrystalizeAnimation();
        
        // Vérifier si c'est gagné (dernière tentative)
        if (currentAttempt >= maxAttempts) {
            gameWon = true;
            gameFinished = true;
            
            if (winSound != null) {
                winSound.play();
            }
            
            // Enregistrer le score
            int finalScore = calculateScore();
            if (game instanceof AdventCalendarGame) {
                AdventCalendarGame adventGame = (AdventCalendarGame) game;
                adventGame.setScore(dayId, finalScore);
                adventGame.setVisited(dayId, true);
            }
        } else {
            // Jouer le son de tentative
            if (wrongSound != null) {
                wrongSound.play();
            }
        }
    }
    
    /**
     * Démarre l'animation de cristallisation
     */
    private void startCrystalizeAnimation() {
        System.out.println("=== DÉBUT ANIMATION cristallisation pour tentative " + currentAttempt + " ===");
        
        // Calculer les tailles de cristaux de début et fin
        float startProgress = (float) (currentAttempt - 1) / maxAttempts;
        float endProgress = (float) currentAttempt / maxAttempts;
        
        startCrystalSize = (int) (initialCrystalSize * (1.0f - startProgress) + 1);
        endCrystalSize = (int) (initialCrystalSize * (1.0f - endProgress) + 1);
        
        System.out.println("Animation de " + startCrystalSize + " à " + endCrystalSize);
        
        // Démarrer l'animation
        isAnimating = true;
        animationTime = 0.0f;
    }
    
    
    private int calculateScore() {
        // Score basé sur le nombre de tentatives
        float efficiency = (float) (maxAttempts - currentAttempt) / maxAttempts;
        return Math.min(100, Math.max(10, (int) (efficiency * 100)));
    }
    
    private void solveGame() {
        System.out.println("Résolution automatique du jeu CrystalizeGuess (mode test)");
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, 100);
            adventGame.setVisited(dayId, true);
        }
        
        if (winSound != null) {
            winSound.play();
        }
        
        returnToMainMenu();
    }
    
    private void revealMore() {
        System.out.println("Révéler plus (mode test - touche N)");
        
        if (currentAttempt < maxAttempts) {
            makeGuess();
        }
    }
    
    private void updateButtonPositions() {
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        float baseButtonSize = 100f;
        float marginFromEdge = 30f;
        float spacingBetweenButtons = 30f;
        
        // Bouton close en haut à droite
        float closeButtonX = viewportWidth - marginFromEdge - baseButtonSize;
        float closeButtonY = viewportHeight - marginFromEdge - baseButtonSize;
        
        // Bouton info juste en dessous
        float infoButtonX = closeButtonX;
        float infoButtonY = closeButtonY - baseButtonSize - spacingBetweenButtons;
        
        closeButton.setSize(baseButtonSize, baseButtonSize);
        closeButton.setPosition(closeButtonX, closeButtonY);
        
        infoButton.setSize(baseButtonSize, baseButtonSize);
        infoButton.setPosition(infoButtonX, infoButtonY);
        
        // Bouton submit en bas au centre
        submitButton.setSize(200, 80);
        submitButton.setPosition((viewportWidth - submitButton.width) / 2, 50);
    }
    
    @Override
    protected void updateGame(float delta) {
        // Gérer l'animation de cristallisation
        if (isAnimating) {
            animationTime += delta;
            
            // Générer la texture animée avec la taille de cristaux interpolée
            updateAnimatedTexture();
            
            if (animationTime >= animationDuration) {
                // Fin de l'animation
                finishCrystalizeAnimation();
            }
        }
        
        // Mettre à jour le système de debug
        if (debugManager != null) {
            debugManager.update(delta);
        }
    }
    
    /**
     * Met à jour la texture animée avec interpolation
     */
    private void updateAnimatedTexture() {
        float progress = calculateAnimationProgress();
        float currentCrystalSize = interpolateCrystalSize(progress);
        
        try {
            animatedTexture = applyCrystallizeShader(originalImageTexture, currentCrystalSize);
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération de la texture animée: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calcule le progrès de l'animation (0.0 à 1.0)
     */
    private float calculateAnimationProgress() {
        return Math.min(animationTime / animationDuration, 1.0f);
    }
    
    /**
     * Interpole la taille de cristal entre start et end
     */
    private float interpolateCrystalSize(float progress) {
        return startCrystalSize + (endCrystalSize - startCrystalSize) * progress;
    }
    
    /**
     * Termine l'animation de cristallisation
     */
    private void finishCrystalizeAnimation() {
        System.out.println("=== FIN ANIMATION cristallisation ===");
        
        disposeOldTexture();
        updateCurrentTexture();
        resetAnimation();
    }
    
    
    /**
     * Applique le shader avec les paramètres de debug
     */
    private void applyDebugShader() {
        if (crystallizeShader != null && originalImageTexture != null && debugManager != null) {
           
            // Libérer l'ancienne texture si nécessaire
            if (currentCrystalizedTexture != null && currentCrystalizedTexture != originalImageTexture) {
                currentCrystalizedTexture.dispose();
            }
            
            // Appliquer le shader avec les paramètres de debug
            currentCrystalizedTexture = applyCrystallizeShader(originalImageTexture, debugManager.getDebugCrystalSize());
        }
    }
    
    /**
     * Libère l'ancienne texture si nécessaire
     */
    private void disposeOldTexture() {
        if (currentCrystalizedTexture != null && currentCrystalizedTexture != originalImageTexture) {
            currentCrystalizedTexture.dispose();
        }
    }
    
    /**
     * Met à jour la texture actuelle avec la texture animée
     */
    private void updateCurrentTexture() {
        currentCrystalizedTexture = animatedTexture;
        animatedTexture = null;
    }
    
    /**
     * Réinitialise l'état de l'animation
     */
    private void resetAnimation() {
        isAnimating = false;
        animationTime = 0.0f;
    }
    
    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Dessiner l'image (cristallisée ou originale selon l'état)
        drawImage();
        
        // Dessiner l'interface
        drawGameInterface();
        
        // Dessiner les informations de debug si en mode debug
        if (debugManager != null) {
            debugManager.drawDebugInfo(batch, viewport.getWorldHeight());
        }
        
        // Dessiner le panneau d'info si visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
    }
    
    /**
     * Calcule les dimensions et position d'affichage pour une texture
     * en respectant les proportions et en centrant l'image
     */
    private DisplayInfo calculateDisplayInfo(Texture texture) {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        float imageWidth = texture.getWidth();
        float imageHeight = texture.getHeight();
        float textureAspect = imageWidth / imageHeight;
        float screenAspect = screenWidth / screenHeight;
        
        float displayWidth, displayHeight, displayX, displayY;
        
        if (textureAspect > screenAspect) {
            // L'image est plus large que l'écran, ajuster la hauteur
            displayWidth = screenWidth;
            displayHeight = screenWidth / textureAspect;
            displayX = 0;
            displayY = (screenHeight - displayHeight) / 2;
        } else {
            // L'image est plus haute que l'écran, ajuster la largeur
            displayHeight = screenHeight;
            displayWidth = screenHeight * textureAspect;
            displayX = (screenWidth - displayWidth) / 2;
            displayY = 0;
        }
        
        return new DisplayInfo(displayX, displayY, displayWidth, displayHeight);
    }
    
    /**
     * Classe interne pour stocker les informations d'affichage
     */
    private static class DisplayInfo {
        final float x, y, width, height;
        
        DisplayInfo(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
    
    private void drawImage() {
        Texture textureToDraw = getCurrentTexture();
        
        if (textureToDraw == null) {
            System.out.println("ERREUR: textureToDraw est null !");
            return;
        }
        
        // TOUJOURS utiliser les dimensions de la texture qui est réellement affichée
        // pour éviter les incohérences entre image originale et texture cristallisée
        DisplayInfo displayInfo = calculateDisplayInfo(textureToDraw);
        
        batch.setColor(1, 1, 1, 1);
        batch.draw(textureToDraw, displayInfo.x, displayInfo.y, displayInfo.width, displayInfo.height);
    }
    
    /**
     * Détermine quelle texture afficher selon l'état du jeu
     */
    private Texture getCurrentTexture() {
        if (gameFinished) {
            return originalImageTexture;
        } else if (isAnimating && animatedTexture != null) {
            return animatedTexture;
        } else {
            return currentCrystalizedTexture;
        }
    }
    
    private void drawGameInterface() {
        // Dessiner les boutons
        drawCloseButton();
        drawInfoButton();
        
        if (!gameFinished) {
            drawSubmitButton();
            drawAttemptCounter();
        } else if (gameWon) {
            drawVictoryMessage();
        }
    }
    
    private void drawCloseButton() {
        if (closeButtonTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(closeButtonTexture, closeButton.x, closeButton.y, 
                      closeButton.width, closeButton.height);
        }
    }
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, 
                      infoButton.width, infoButton.height);
        }
    }
    
    private void drawSubmitButton() {
        // Fond du bouton
        batch.setColor(0.2f, 0.6f, 0.9f, 1);
        batch.draw(whiteTexture, submitButton.x, submitButton.y, 
                  submitButton.width, submitButton.height);
        
        // Bordure
        batch.setColor(1, 1, 1, 1);
        float borderWidth = 2;
        batch.draw(whiteTexture, submitButton.x, submitButton.y + submitButton.height - borderWidth, 
                  submitButton.width, borderWidth);
        batch.draw(whiteTexture, submitButton.x, submitButton.y, submitButton.width, borderWidth);
        batch.draw(whiteTexture, submitButton.x, submitButton.y, borderWidth, submitButton.height);
        batch.draw(whiteTexture, submitButton.x + submitButton.width - borderWidth, submitButton.y, 
                  borderWidth, submitButton.height);
        
        // Texte
        font.setColor(1, 1, 1, 1);
        String text = "Révéler plus";
        layout.setText(font, text);
        font.draw(batch, layout, 
                 submitButton.x + (submitButton.width - layout.width) / 2,
                 submitButton.y + (submitButton.height + layout.height) / 2);
    }
    
    private void drawAttemptCounter() {
        font.setColor(1, 1, 1, 1);
        String text = "Tentatives: " + currentAttempt + " / " + maxAttempts;
        layout.setText(font, text);
        font.draw(batch, layout, 
                 (DisplayConfig.WORLD_WIDTH - layout.width) / 2,
                 viewport.getWorldHeight() - 50);
    }
    
    private void drawVictoryMessage() {
        bigFont.setColor(0.2f, 0.9f, 0.2f, 1);
        String text = "Image révélée !";
        layout.setText(bigFont, text);
        bigFont.draw(batch, layout, 
                    (DisplayConfig.WORLD_WIDTH - layout.width) / 2,
                    viewport.getWorldHeight() - 100);
        
        font.setColor(1, 1, 1, 1);
        String clickText = "Cliquez pour continuer";
        layout.setText(font, clickText);
        font.draw(batch, layout, 
                 (DisplayConfig.WORLD_WIDTH - layout.width) / 2,
                 50);
    }
    
    private void drawInfoPanel() {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        // Fond semi-transparent
        batch.setColor(0, 0, 0, 0.4f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
        
        // Panneau
        float panelWidth = Math.min(600, screenWidth * 0.9f);
        float panelHeight = Math.min(400, screenHeight * 0.7f);
        float panelX = (screenWidth - panelWidth) / 2;
        float panelY = (screenHeight - panelHeight) / 2;
        
        // Fond du panneau
        batch.setColor(0.95f, 0.95f, 0.98f, 0.95f);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);
        
        // Bordure
        batch.setColor(0.3f, 0.4f, 0.7f, 1);
        float borderWidth = 2;
        batch.draw(whiteTexture, panelX, panelY + panelHeight - borderWidth, panelWidth, borderWidth);
        batch.draw(whiteTexture, panelX, panelY, panelWidth, borderWidth);
        batch.draw(whiteTexture, panelX, panelY, borderWidth, panelHeight);
        batch.draw(whiteTexture, panelX + panelWidth - borderWidth, panelY, borderWidth, panelHeight);
        
        // Contenu
        font.setColor(0.2f, 0.3f, 0.8f, 1);
        String title = "Devine l'Image";
        layout.setText(font, title);
        font.draw(batch, layout, 
                 panelX + (panelWidth - layout.width) / 2,
                 panelY + panelHeight - 30);
        
        font.setColor(0.1f, 0.1f, 0.2f, 1);
        float textY = panelY + panelHeight - 80;
        float lineHeight = 25;
        
        String[] rules = {
            "Objectif : Deviner quelle image se cache",
            "derrière l'effet de cristallisation.",
            "",
            "À chaque tentative, l'image devient",
            "plus claire et les détails apparaissent.",
            "",
            "Vous avez " + maxAttempts + " tentatives pour",
            "révéler complètement l'image.",
            "",
            "Cliquez n'importe où pour fermer."
        };
        
        for (String rule : rules) {
            layout.setText(font, rule);
            font.draw(batch, layout, panelX + 20, textY);
            textY -= lineHeight;
        }
    }
    
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        updateButtonPositions();
    }
    
    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        bigFont.dispose();
        whiteTexture.dispose();
        if (originalImageTexture != null) originalImageTexture.dispose();
        if (currentCrystalizedTexture != null && currentCrystalizedTexture != originalImageTexture) {
            currentCrystalizedTexture.dispose();
        }
        if (animatedTexture != null) {
            animatedTexture.dispose();
        }
        if (crystallizeShader != null) {
            crystallizeShader.dispose();
        }
        if (closeButtonTexture != null) closeButtonTexture.dispose();
        if (infoButtonTexture != null) infoButtonTexture.dispose();
        if (winSound != null) winSound.dispose();
        if (wrongSound != null) wrongSound.dispose();
        if (failedSound != null) failedSound.dispose();
    }
}

