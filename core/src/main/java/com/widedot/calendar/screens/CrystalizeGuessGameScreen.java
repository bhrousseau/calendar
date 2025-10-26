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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.widedot.calendar.ui.BottomInputBar;
import com.widedot.calendar.platform.PlatformRegistry;
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
    private final Rectangle closeButton;
    private final Rectangle infoButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture closeButtonTexture;
    private Texture infoButtonTexture;
    private boolean showInfoPanel;
    
    // Nouveau système d'input
    private Stage inputStage;
    private Skin inputSkin;
    private BottomInputBar inputBar;
    private Table rootTable;
    private boolean inputBarVisible = true;
    
    // Game data
    private int maxAttempts;
    private int currentAttempt;
    private int initialCrystalSize;
    private boolean gameWon;
    private boolean gameFinished;
    private Theme theme;
    
    // Input system (simplifié)
    private String correctAnswer;
    private int wrongAnswers;
    private boolean hasUsedHelp;
    
    // Image textures
    private Texture originalImageTexture;
    private Texture currentCrystalizedTexture; // Texture actuellement affichée
    
    // Shader et rendu
    private CrystallizeShader crystallizeShader;
    private FrameBuffer currentFrameBuffer; // FrameBuffer pour la texture shader
    
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
    
    
    
    // Background avec HSL (mêmes unités que SlidingPuzzle)
    private Texture backgroundTexture;
    private float backgroundHue = 0f;        // 0-360
    private float backgroundSaturation = 0f; // 0-100
    private float backgroundLightness = 0f;  // -100 à +100
    private float currentBgWidth;
    private float currentBgHeight;
    private float currentBgX;
    private float currentBgY;
    private float currentScaleX;
    private float currentScaleY;
    
    // Fade des boutons et du fond
    private boolean isButtonsFading = false;
    private float buttonsFadeTimer = 0f;
    private static final float BUTTONS_FADE_DURATION = 2f;
    
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
        Gdx.app.log("CrystalizeGuessGameScreen", "Mode test CrystalizeGuess: " + isTestMode);
        
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
            
            // Charger les paramètres HSL du background (mêmes unités que SlidingPuzzle)
            if (parameters.containsKey("bgHue")) {
                this.backgroundHue = Math.max(0, Math.min(360, ((Number) parameters.get("bgHue")).floatValue()));
            }
            if (parameters.containsKey("bgSaturation")) {
                this.backgroundSaturation = Math.max(0, Math.min(100, ((Number) parameters.get("bgSaturation")).floatValue()));
            }
            if (parameters.containsKey("bgLightness")) {
                this.backgroundLightness = Math.max(-100, Math.min(100, ((Number) parameters.get("bgLightness")).floatValue()));
            }
            
            Gdx.app.log("CrystalizeGuessGameScreen", "Paramètres du shader chargés:");
            Gdx.app.log("CrystalizeGuessGameScreen", "  - Randomness: " + currentRandomness);
            Gdx.app.log("CrystalizeGuessGameScreen", "  - EdgeThickness: " + currentEdgeThickness);
            Gdx.app.log("CrystalizeGuessGameScreen", "  - Stretch: " + currentStretch);
            Gdx.app.log("CrystalizeGuessGameScreen", "  - FadeEdges: " + currentFadeEdges);
            Gdx.app.log("CrystalizeGuessGameScreen", "  - EdgeColor: R=" + currentEdgeColorR + " G=" + currentEdgeColorG + " B=" + currentEdgeColorB + " A=" + currentEdgeColorA);
            Gdx.app.log("CrystalizeGuessGameScreen", "  - Background HSL: H=" + backgroundHue + " S=" + backgroundSaturation + " L=" + backgroundLightness);
        }
        
        // Charger la texture du background
        Gdx.app.log("CrystalizeGuessGameScreen", "Chargement du background...");
        try {
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/cgg/background-0.png"));
            Gdx.app.log("CrystalizeGuessGameScreen", "Background chargé.");
        } catch (Exception e) {
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors du chargement du background: " + e.getMessage());
            this.backgroundTexture = null;
        }
        
        // Initialiser les éléments UI
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.bigFont = new BitmapFont();
        this.bigFont.getData().setScale(2.0f);
        this.layout = new GlyphLayout();
        
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors du chargement du bouton close: " + e.getMessage());
            this.closeButtonTexture = null;
        }
        
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
        } catch (Exception e) {
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }
        
        // Charger les sons
        loadSounds();
        
        // Initialiser l'état du jeu
        this.currentAttempt = 0;
        this.gameWon = false;
        this.gameFinished = false;
        this.showInfoPanel = false;
        
        // Initialiser le système d'input (simplifié)
        this.correctAnswer = theme.getTitle(); // Utiliser le titre du thème comme réponse correcte
        this.wrongAnswers = 0;
        this.hasUsedHelp = false;
        
        // Initialiser le nouveau système d'input
        initializeInputSystem();
        
        // Initialiser la taille de cristal actuelle
        this.currentCrystalSize = initialCrystalSize;
        
        // Créer l'input processor
        createInputProcessor();
        
        // Initialiser le système de debug
        initializeDebugManager();
        
        // Mettre à jour les positions des boutons
        updateButtonPositions();
        
        // Le shader sera initialisé plus tard quand le viewport sera prêt
        
        Gdx.app.log("CrystalizeGuessGameScreen", "Constructeur CrystalizeGuessGameScreen terminé");
    }
    
    /**
     * Initialise le nouveau système d'input avec BottomInputBar
     */
    private void initializeInputSystem() {
        // Créer le stage pour l'input
        inputStage = new Stage(viewport);
        
        // Créer le skin (utiliser le skin par défaut de LibGDX)
        try {
            inputSkin = new Skin(Gdx.files.internal("uiskin.json"));
            Gdx.app.log("CrystalizeGuessGameScreen", "Skin uiskin.json chargé avec succès");
        } catch (Exception e) {
            Gdx.app.log("CrystalizeGuessGameScreen", "Skin uiskin.json non trouvé, création d'un skin basique");
            // Fallback: créer un skin basique avec les styles par défaut
            inputSkin = createBasicSkin();
        }
        
        // Créer la table racine
        rootTable = new Table();
        rootTable.setFillParent(true);
        inputStage.addActor(rootTable);
        
        // Créer la barre d'input
        inputBar = new BottomInputBar(inputSkin);
        inputBar.setListener(new BottomInputBar.Listener() {
            @Override
            public void onSubmit(String text) {
                handleUserInput(text);
            }
            
            @Override
            public void onFocusChanged(boolean focused) {
                handleInputFocusChange(focused);
            }
        });
        
        // Ajouter la barre d'input en bas
        rootTable.add().expand().fill();
        rootTable.row();
        rootTable.add(inputBar).expandX().fillX().pad(0).height(120).colspan(1); // Pas de padding horizontal pour toute la largeur
        
        // Définir le texte de placeholder spécifique au jeu crystalize
        inputBar.setPlaceholderText("Donner le nom de ce tableau ...");
        
        Gdx.app.log("CrystalizeGuessGameScreen", "Système d'input initialisé");
    }
    
    /**
     * Crée un skin basique avec les styles nécessaires
     */
    private Skin createBasicSkin() {
        Skin basicSkin = new Skin();
        
        // Créer une police basique
        BitmapFont basicFont = new BitmapFont();
        
        // Créer un style TextField basique
        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle textFieldStyle = 
            new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle();
        textFieldStyle.font = basicFont;
        textFieldStyle.fontColor = Color.WHITE;
        // Pas de cursor personnalisé pour éviter les erreurs
        
        // Ajouter le style au skin
        basicSkin.add("default", textFieldStyle);
        
        Gdx.app.log("CrystalizeGuessGameScreen", "Skin basique créé avec succès");
        return basicSkin;
    }
    
    /**
     * Gère la soumission de l'input utilisateur
     */
    private void handleUserInput(String text) {
        if (gameFinished || text.trim().isEmpty()) return;
        
        // Vérifier si la réponse est correcte
        if (text.trim().equalsIgnoreCase(correctAnswer)) {
            // Effacer le champ de saisie
            inputBar.clear();
            
            // Masquer le champ de saisie immédiatement
            inputBarVisible = false;
            
            // Réponse correcte - gagner le jeu
            gameWon = true;
            gameFinished = true;
            
            // Démarrer le fondu des boutons et du fond
            isButtonsFading = true;
            buttonsFadeTimer = 0f;
            
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
            
            // Démarrer l'animation de révélation finale
            startFinalRevealAnimation();
        } else {
            // Réponse incorrecte
            wrongAnswers++;
            
            if (wrongSound != null) {
                wrongSound.play();
            }
            
            // Si 5 mauvaises réponses, améliorer l'image
            if (wrongAnswers >= 5 && !hasUsedHelp) {
                hasUsedHelp = true;
                improveImage();
            }
        }
    }
    
    /**
     * Gère le changement de focus de l'input (pour le clavier virtuel mobile)
     */
    private void handleInputFocusChange(boolean focused) {
        boolean isMobileWeb = PlatformRegistry.isMobileBrowser();
        if (isMobileWeb) {
            // Ajuster le padding bas pour surélever la barre au-dessus du clavier
            float paddingBottom = focused ? 280f : 0f;
            rootTable.padBottom(paddingBottom);
            rootTable.invalidateHierarchy();
        }
    }
    
    @Override
    protected Theme loadTheme(int day) {
        Gdx.app.log("CrystalizeGuessGameScreen", "Chargement de la texture pour le jour " + day);
        
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors du chargement de la texture: " + e.getMessage());
            // Stack trace logged automatically
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
        Gdx.app.log("CrystalizeGuessGameScreen", "Initialisation du shader de cristallisation");
        
        try {
            // Créer le shader
            crystallizeShader = new CrystallizeShader();
            Gdx.app.log("CrystalizeGuessGameScreen", "Shader de cristallisation initialisé avec succès");
            
        } catch (Exception e) {
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors de l'initialisation du shader: " + e.getMessage());
            // Stack trace logged automatically
        }
    }
    
    
    /**
     * Initialise le jeu avec l'image la plus cristallisée
     */
    private void initializeGameWithCrystalizedImage(String imagePath) {
        Gdx.app.log("CrystalizeGuessGameScreen", "=== INITIALISATION du jeu avec image cristallisée ===");
        
        try {
            Gdx.app.log("CrystalizeGuessGameScreen", "Chargement de l'image: " + imagePath);
            
            // Appliquer le shader le plus fort (tentative 0) seulement si le shader est initialisé
            if (crystallizeShader != null) {
                Gdx.app.log("CrystalizeGuessGameScreen", "Application du shader initial (cristallisation maximale)...");
                currentCrystalizedTexture = applyCrystallizeShader(originalImageTexture, initialCrystalSize);
                Gdx.app.log("CrystalizeGuessGameScreen", "Shader appliqué avec succès");
            } else {
                Gdx.app.log("CrystalizeGuessGameScreen", "Shader pas encore initialisé, utilisation de l'image originale");
                currentCrystalizedTexture = originalImageTexture;
            }
            
            Gdx.app.log("CrystalizeGuessGameScreen", "=== INITIALISATION TERMINÉE ===");
            
        } catch (Exception e) {
            Gdx.app.error("CrystalizeGuessGameScreen", "=== ERREUR INITIALISATION ===");
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors de l'initialisation: " + e.getMessage());
            // Stack trace logged automatically
            
            // En cas d'erreur, utiliser l'image originale
            Gdx.app.log("CrystalizeGuessGameScreen", "Utilisation de l'image originale comme fallback");
            currentCrystalizedTexture = originalImageTexture;
        }
    }
    
    /**
     * Applique l'effet de cristallisation avec le shader
     */
    private Texture applyCrystallizeShader(Texture sourceTexture, float crystalSize) {
        
        if (crystalSize <= 1 || crystallizeShader == null) {
            Gdx.app.log("CrystalizeGuessGameScreen", "CrystalSize <= 1 ou shader non initialisé, retour de l'image originale");
            return sourceTexture;
        }
        
        try {
            return renderWithShader(sourceTexture, crystalSize);
        } catch (Exception e) {
            Gdx.app.error("CrystalizeGuessGameScreen", "ERREUR dans applyCrystallizeShader: " + e.getMessage());
            // Stack trace logged automatically
            return sourceTexture;
        }
    }
    
    /**
     * Effectue le rendu avec le shader de cristallisation
     * Retourne une copie de la texture avec le shader appliqué
     */
    private Texture renderWithShader(Texture sourceTexture, float crystalSize) {
        // Utiliser les dimensions RÉELLES de la texture source (pas les dimensions d'affichage)
        // pour que animatedTexture ait les mêmes dimensions que originalImageTexture
        int renderWidth = sourceTexture.getWidth();
        int renderHeight = sourceTexture.getHeight();

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

            // Dessin dans le FBO avec les dimensions calculées
            batch.draw(sourceTexture, 0, 0, renderWidth, renderHeight);

            batch.end();
        } finally {
            // Restaure l'état du batch
            batch.setShader(prevShader);
            batch.setProjectionMatrix(prevProj);
            batch.setTransformMatrix(prevTrans);
            tempFrameBuffer.end();
            
            // S'assurer que la projection du viewport est correctement restaurée
            // après l'utilisation du shader pour éviter l'étirement du viewport
            if (viewport != null) {
                viewport.apply();
                batch.setProjectionMatrix(viewport.getCamera().combined);
            }
        }

        // Dispose l'ancien framebuffer si existant
        if (currentFrameBuffer != null && currentFrameBuffer != tempFrameBuffer) {
            currentFrameBuffer.dispose();
        }
        
        // Stocker le nouveau framebuffer
        currentFrameBuffer = tempFrameBuffer;
        
        // Retourner la texture du framebuffer (elle reste valide tant que le framebuffer existe)
        Texture resultTexture = tempFrameBuffer.getColorBufferTexture();
        resultTexture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest, com.badlogic.gdx.graphics.Texture.TextureFilter.Nearest);
        
        return resultTexture;
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Format de couleur invalide: " + colorStr);
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Format de couleur invalide: " + colorStr + ", utilisation des valeurs par défaut");
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors du chargement des sons: " + e.getMessage());
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
                // Alt+R : Résoudre automatiquement le jeu (mode test uniquement)
                if (keycode == Input.Keys.R && isTestMode && !gameFinished && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    solveGame();
                    return true;
                }
                
                // Alt+N : Déclencher la phase de victoire (mode test uniquement)
                if (keycode == Input.Keys.N && isTestMode && !gameFinished && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    triggerVictoryPhase();
                    return true;
                }
                
                // Plus de gestion d'input clavier - géré par BottomInputBar
                
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
            
            @Override
            public boolean keyTyped(char character) {
                // Plus de gestion de saisie de texte - géré par BottomInputBar
                return false;
            }
        };
    }
    
    @Override
    protected void onScreenActivated() {
        super.onScreenActivated();
        // Utiliser un InputMultiplexer pour gérer à la fois l'input processor et le stage
        com.badlogic.gdx.InputMultiplexer multiplexer = new com.badlogic.gdx.InputMultiplexer();
        multiplexer.addProcessor(inputStage);
        multiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(multiplexer);
    }
    
    @Override
    protected void onScreenDeactivated() {
        super.onScreenDeactivated();
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    protected void initializeGame() {
        Gdx.app.log("CrystalizeGuessGameScreen", "Initialisation du jeu CrystalizeGuess pour le jour " + dayId);
        updateButtonPositions();
        calculateBackgroundDimensions();
        
        // Initialiser le shader maintenant que le viewport est prêt
        initializeCrystallizeShader();
        
        // Appliquer le shader initial après l'initialisation
        if (originalImageTexture != null) {
            Gdx.app.log("CrystalizeGuessGameScreen", "Application du shader initial après initialisation...");
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
        
        // Plus de bouton submit - l'input se fait via la barre d'input
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
    
    // Méthode submitAnswer supprimée - remplacée par handleUserInput
    
    private void improveImage() {
        // Améliorer l'image en réduisant le crystal size à 75
        Gdx.app.log("CrystalizeGuessGameScreen", "Amélioration de l'image après 5 mauvaises réponses");
        startCrystalizeAnimation(75);
    }
    
    private void startFinalRevealAnimation() {
        Gdx.app.log("CrystalizeGuessGameScreen", "=== DÉBUT Animation de révélation finale ===");
        Gdx.app.log("CrystalizeGuessGameScreen", "Animation de " + ((int)currentCrystalSize) + " à 1");
        
        isAnimating = true;
        animationTime = 0.0f;
        startCrystalSize = (int) currentCrystalSize;
        endCrystalSize = 1; // Révéler complètement l'image
        
        Gdx.app.log("CrystalizeGuessGameScreen", "isAnimating=" + isAnimating + ", startCrystalSize=" + startCrystalSize + ", endCrystalSize=" + endCrystalSize);
    }
    
    /**
     * Démarre l'animation de cristallisation
     */
    private void startCrystalizeAnimation() {
        startCrystalizeAnimation(endCrystalSize);
    }
    
    /**
     * Démarre l'animation de cristallisation avec une taille cible spécifique
     */
    private void startCrystalizeAnimation(int targetCrystalSize) {
        Gdx.app.log("CrystalizeGuessGameScreen", "=== DÉBUT ANIMATION cristallisation vers " + targetCrystalSize + " ===");
        
        startCrystalSize = (int) currentCrystalSize;
        endCrystalSize = targetCrystalSize;
        
        Gdx.app.log("CrystalizeGuessGameScreen", "Animation de " + startCrystalSize + " à " + endCrystalSize);
        
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
        Gdx.app.log("CrystalizeGuessGameScreen", "Résolution automatique du jeu CrystalizeGuess (mode test)");
        
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
    
    /**
     * Déclenche la phase de victoire (mode test - touche N)
     */
    private void triggerVictoryPhase() {
        Gdx.app.log("CrystalizeGuessGameScreen", "Déclenchement de la phase de victoire (mode test - touche N)");
        
        // Simuler une victoire comme si la bonne réponse venait d'être trouvée
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
            Gdx.app.log("CrystalizeGuessGameScreen", "Score enregistré: " + finalScore + " pour le jour " + dayId);
        }
        
        // Démarrer l'animation de révélation finale
        startFinalRevealAnimation();
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
        
        // Plus de bouton submit - remplacé par la barre d'input
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le stage d'input
        if (inputStage != null) {
            inputStage.act(delta);
        }
        
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
        
        // Mettre à jour le fondu des boutons et du fond
        if (isButtonsFading) {
            buttonsFadeTimer += delta;
            if (buttonsFadeTimer >= BUTTONS_FADE_DURATION) {
                buttonsFadeTimer = BUTTONS_FADE_DURATION; // Limiter à la durée maximale
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
            Gdx.app.error("CrystalizeGuessGameScreen", "Erreur lors de la génération de la texture animée: " + e.getMessage());
            // Stack trace logged automatically
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
        Gdx.app.log("CrystalizeGuessGameScreen", "=== FIN ANIMATION cristallisation ===");
        
        // Mettre à jour la taille de cristal actuelle
        currentCrystalSize = endCrystalSize;
        
        disposeOldTexture();
        updateCurrentTexture();
        resetAnimation();
        
        // Si c'est l'animation de révélation finale, afficher l'image complète
        if (gameWon && endCrystalSize <= 1) {
            Gdx.app.log("CrystalizeGuessGameScreen", "Révélation finale terminée");
        }
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
        // Ne pas disposer currentCrystalizedTexture car elle peut provenir d'un FrameBuffer
        // Le FrameBuffer sera disposé automatiquement lors de la création d'un nouveau
        // ou dans dispose()
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
        // Calculer l'alpha du background (même timing que les boutons)
        float bgAlpha = isButtonsFading ? (1f - buttonsFadeTimer / BUTTONS_FADE_DURATION) : 1f;
        bgAlpha = Math.max(0f, bgAlpha);
        
        // Toujours dessiner le fond de couleur en premier
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Dessiner le background par-dessus avec fade pendant le fade des boutons
        if (backgroundTexture != null && (isButtonsFading || !gameWon)) {
            batch.setColor(1, 1, 1, bgAlpha);
            
            // Utiliser les paramètres HSL du debug manager si en mode debug, sinon les paramètres normaux
            float bgHue, bgSaturation, bgLightness;
            if (debugManager != null && debugManager.isDebugMode()) {
                bgHue = debugManager.getDebugBackgroundHue();
                bgSaturation = debugManager.getDebugBackgroundSaturation();
                bgLightness = debugManager.getDebugBackgroundLightness();
            } else {
                bgHue = backgroundHue;
                bgSaturation = backgroundSaturation;
                bgLightness = backgroundLightness;
            }
            
            // Appliquer le shader HSL pour la colorisation avec dimensions calculées (crop)
            com.widedot.calendar.shaders.HSLShader.begin(batch, bgHue, bgSaturation, bgLightness);
            batch.draw(backgroundTexture, currentBgX, currentBgY, currentBgWidth, currentBgHeight);
            com.widedot.calendar.shaders.HSLShader.end(batch);
            
            // Reset color
            batch.setColor(1, 1, 1, 1);
        }
        
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
        
        // Dessiner le stage d'input par-dessus tout (seulement si visible)
        if (inputStage != null && inputBarVisible) {
            inputStage.draw();
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
            Gdx.app.log("CrystalizeGuessGameScreen", "ERREUR: textureToDraw est null !");
            return;
        }
        
        // Toutes les textures ont les mêmes dimensions réelles, donc on peut utiliser
        // les dimensions de la texture affichée pour calculer l'affichage
        DisplayInfo displayInfo = calculateDisplayInfo(textureToDraw);
        
        batch.setColor(1, 1, 1, 1);
        batch.draw(textureToDraw, displayInfo.x, displayInfo.y, displayInfo.width, displayInfo.height);
    }
    
    /**
     * Détermine quelle texture afficher selon l'état du jeu
     */
    private Texture getCurrentTexture() {
        // Toujours afficher l'animation en priorité si elle est active
        if (isAnimating && animatedTexture != null) {
            return animatedTexture;
        } else if (gameFinished && !isAnimating) {
            // Afficher l'image originale seulement si le jeu est terminé ET qu'il n'y a plus d'animation
            return originalImageTexture;
        } else {
            return currentCrystalizedTexture;
        }
    }
    
    private void drawGameInterface() {
        // Calculer l'alpha pour les boutons (fade out si victoire)
        float buttonsAlpha = isButtonsFading ? (1f - buttonsFadeTimer / BUTTONS_FADE_DURATION) : 1f;
        buttonsAlpha = Math.max(0f, buttonsAlpha);
        
        // Dessiner les boutons avec fade out
        if (!isButtonsFading || buttonsAlpha > 0f) {
            drawCloseButton(buttonsAlpha);
            drawInfoButton(buttonsAlpha);
        }
        
        // Plus de bouton submit ni d'affichage d'input - remplacé par la barre d'input
    }
    
    private void drawCloseButton(float alpha) {
        if (closeButtonTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            batch.draw(closeButtonTexture, closeButton.x, closeButton.y, 
                      closeButton.width, closeButton.height);
            batch.setColor(1, 1, 1, 1); // Reset
        }
    }
    
    private void drawInfoButton(float alpha) {
        if (infoButtonTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, 
                      infoButton.width, infoButton.height);
            batch.setColor(1, 1, 1, 1); // Reset
        }
    }
    
    // Méthodes drawSubmitButton et drawUserInput supprimées - remplacées par BottomInputBar
    
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
        calculateBackgroundDimensions();
        
        // Mettre à jour le stage d'input
        if (inputStage != null) {
            inputStage.getViewport().update(width, height, true);
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        bigFont.dispose();
        whiteTexture.dispose();
        if (originalImageTexture != null) originalImageTexture.dispose();
        // currentCrystalizedTexture et animatedTexture proviennent du FrameBuffer, ne pas les disposer séparément
        if (crystallizeShader != null) {
            crystallizeShader.dispose();
        }
        if (currentFrameBuffer != null) {
            currentFrameBuffer.dispose();
        }
        if (closeButtonTexture != null) closeButtonTexture.dispose();
        if (infoButtonTexture != null) infoButtonTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (winSound != null) winSound.dispose();
        if (wrongSound != null) wrongSound.dispose();
        if (failedSound != null) failedSound.dispose();
        
        // Disposer le stage d'input
        if (inputStage != null) {
            inputStage.dispose();
        }
        if (inputSkin != null) {
            inputSkin.dispose();
        }
    }
}

