package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.Config;

/**
 * Écran de jeu pour le mini-jeu Mastermind
 */
public class MastermindGameScreen extends GameScreen {
    // Input processor pour les clics
    private InputAdapter inputProcessor;
    
    // UI
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final BitmapFont infoFont; // Police dédiée pour le panneau d'info
    private final GlyphLayout layout;
    private final Rectangle backButton;
    private final Rectangle submitButton;
    private final Rectangle infoButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture fullImageTexture;
    private Texture sendButtonTexture;
    private Texture infoButtonTexture;
    private Texture closeButtonTexture;
    private Texture backgroundTexture;
    private Theme theme;
    
    // Game data
    private Array<Integer> secretCode;
    private Array<Array<Integer>> attempts;
    private Array<GuessResult> results;
    private Array<Integer> currentGuess;
    private int maxAttempts;
    private int codeLength;
    private int numberOfSymbols;
    private boolean gameWon;
    private boolean gameFinished;
    private boolean finalPhase;
    private boolean finalQuestionPhase;
    private boolean showWinImage;
    private boolean showInfoPanel;
    
    // Game parameters
    private Color textColor;
    private Color correctColor;
    private Color incorrectColor;
    private String[] symbolNames;
    private Texture[] symbolTextures;
    
    // Final question
    private QuestionData finalQuestion;
    private String inputText;
    private boolean inputFocused;
    private float cursorBlinkTimer;
    private boolean showCursor;
    
    // Sounds
    private Sound correctSound;
    private Sound incorrectSound;
    private Sound winSound;
    
    // Image fragments for final phase
    private static final int TOTAL_IMAGE_SQUARES = 100;
    private static final int GRID_ROWS = 10;
    private static final int GRID_COLS = 10;
    private TextureRegion[][] imageSquares;
    private Set<Integer> visibleSquares;
    private Random random;
    
    // Transition variables
    private boolean isTransitioning;
    private float transitionTimer;
    private static final float TRANSITION_DURATION = 3.0f; // Durée de transition
    private static final float FADE_TO_BLACK_DURATION = 1.0f; // Durée pour fondu vers noir
    private static final float FADE_TO_IMAGE_DURATION = 2.0f; // Durée pour fondu vers image
    
    // Nouvelles variables pour les cases
    private Texture boxTexture;
    private Array<AnimatedColumn> gridPositions;
    
    // Animation constants
    private static final float FRAME_DURATION = 1f/60f;
    private static final float ANIMATION_DELAY = 0.0f;
    private static final int ANIMATION_VARIANTS = 6;
    private static final int DEFAULT_ANIMATION_VARIANT = 2;
    
    // Animation state
    private static class BoxAnimation {
        int currentFrame;
        float timer;
        float delay;
        boolean isPlaying;
        static Array<Texture> sharedFrames; // Frames partagées entre toutes les animations
        
        BoxAnimation(float delay) {
            this.currentFrame = 0;
            this.timer = 0;
            this.delay = delay;
            this.isPlaying = false;
        }
        
        static void loadSharedFrames(int variant) {
            if (sharedFrames != null) {
                // Nettoyer les anciennes frames si elles existent
                for (Texture frame : sharedFrames) {
                    frame.dispose();
                }
                sharedFrames.clear();
            } else {
                sharedFrames = new Array<>();
            }
            
            String variantFolder = (variant + 1 < 10 ? "0" : "") + (variant + 1);
            FileHandle[] frameFiles = Gdx.files.internal("images/games/mmd/anim/opening/" + variantFolder).list();
            
            // Trier les fichiers par nom pour assurer l'ordre correct
            Array<FileHandle> sortedFiles = new Array<>(frameFiles);
            sortedFiles.sort((f1, f2) -> f1.name().compareTo(f2.name()));
            
            for (FileHandle frame : sortedFiles) {
                if (frame.extension().equalsIgnoreCase("png")) {
                    Texture frameTexture = new Texture(frame);
                    frameTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    sharedFrames.add(frameTexture);
                }
            }
        }
        
        static void disposeSharedFrames() {
            if (sharedFrames != null) {
                for (Texture frame : sharedFrames) {
                    frame.dispose();
                }
                sharedFrames.clear();
                sharedFrames = null;
            }
        }
    }
    
    private static class Column {
        int col;
        Array<Rectangle> rectangles;
        
        Column(int col) {
            this.col = col;
            this.rectangles = new Array<>();
        }
    }
    
    private static class AnimatedColumn extends Column {
        Array<BoxAnimation> animations;
        boolean isAnimating;
        boolean animationComplete;
        boolean tokensFadeStarted;
        float tokensFadeTimer;
        static final float TOKENS_FADE_DURATION = 1.0f; // Durée pour le fade-in
        
        AnimatedColumn(int col) {
            super(col);
            this.animations = new Array<>();
            this.isAnimating = false;
            this.animationComplete = false;
            this.tokensFadeStarted = false;
            this.tokensFadeTimer = 0;
        }
        
        void startAnimations() {
            if (!isAnimating) {
                isAnimating = true;
                animationComplete = false;
                tokensFadeStarted = false;
                tokensFadeTimer = 0;
                
                // Créer une animation pour chaque case de la colonne
                for (int i = 0; i < rectangles.size; i++) {
                    BoxAnimation anim = new BoxAnimation(i * ANIMATION_DELAY);
                    animations.add(anim);
                }
            }
        }
        
        void updateTokensFade(float delta) {
            if (animationComplete && !tokensFadeStarted) {
                tokensFadeStarted = true;
                tokensFadeTimer = 0;
            }
            
            if (tokensFadeStarted) {
                tokensFadeTimer += delta;
            }
        }
        
        float getTokensAlpha() {
            if (!animationComplete || !tokensFadeStarted) {
                return 0f; // Invisible avant la fin de l'animation
            }
            
            if (tokensFadeTimer >= TOKENS_FADE_DURATION) {
                return 1f; // Complètement visible après la durée de fade
            }
            
            // Fade progressif de 0 à 1 sur la durée définie
            return tokensFadeTimer / TOKENS_FADE_DURATION;
        }
        
        void dispose() {
            animations.clear();
        }
    }
    
    /**
     * Classe interne pour représenter une question
     */
    private static class QuestionData {
        public String question;
        public String answer;
        public Array<String> acceptedAnswers;
        
        public QuestionData(String question, String answer) {
            this.question = question;
            this.answer = answer;
            this.acceptedAnswers = new Array<>();
            this.acceptedAnswers.add(answer);
        }
        
        public boolean isAnswerCorrect(String userAnswer, boolean caseSensitive) {
            String testAnswer = caseSensitive ? userAnswer : userAnswer.toLowerCase();
            for (String accepted : acceptedAnswers) {
                String acceptedCompare = caseSensitive ? accepted : accepted.toLowerCase();
                if (testAnswer.trim().equals(acceptedCompare.trim())) {
                    return true;
                }
            }
            return false;
        }
    }
    private static final float GUESS_SPACING = 60f;
    
    /**
     * Classe interne pour représenter le résultat d'une tentative
     */
    private static class GuessResult {
        public int correctPosition; // Nombre de symboles à la bonne position
        public int correctSymbol;   // Nombre de symboles corrects mais mal placés
        
        public GuessResult(int correctPosition, int correctSymbol) {
            this.correctPosition = correctPosition;
            this.correctSymbol = correctSymbol;
        }
    }
    
    /**
     * Classe pour gérer les tokens animés avec trajectoires courbes
     */
    private static class AnimatedToken {
        int tokenType; // Type de token (0-5)
        float currentX, currentY; // Position actuelle
        float startX, startY; // Position de départ
        float targetX, targetY; // Position cible
        boolean isAnimating;
        float animationTime;
        float animationDuration;
        // ===== VITESSES D'ANIMATION =====
        // Modifiez ces valeurs pour changer les vitesses :
        float forwardAnimationDuration = 0.4f; // Durée pour l'animation aller (vers la grille)
        float returnAnimationDuration = 0.1f; // Durée pour l'animation retour (vers position départ)
        // Plus petit = plus rapide, Plus grand = plus lent
        boolean isPlaced; // true si le token est placé sur la grille, false s'il est en position de départ
        int gridSlot; // Slot sur la grille (-1 si pas placé)
        boolean isVisible = true; // Pour masquer temporairement les tokens de départ
        boolean curveUp = true; // true = courbe vers le haut, false = courbe vers le bas
        boolean useStraightLine = false; // true pour ligne droite, false pour courbe
        Runnable onAnimationComplete; // Callback appelée à la fin de l'animation
        
        AnimatedToken(int tokenType, float startX, float startY) {
            this.tokenType = tokenType;
            this.startX = startX;
            this.startY = startY;
            this.currentX = startX;
            this.currentY = startY;
            this.isAnimating = false;
            this.isPlaced = false;
            this.gridSlot = -1;
        }
        
        void startAnimationTo(float targetX, float targetY, boolean toGrid) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.isAnimating = true;
            this.animationTime = 0f;
            this.animationDuration = forwardAnimationDuration; // Utiliser la durée pour l'aller
            // Si on va vers la grille, on sera placé à la fin
            // Si on revient au départ, on ne sera plus placé
            this.isPlaced = toGrid;
        }
        
        void startAnimationTo(float targetX, float targetY, boolean toGrid, Runnable onComplete) {
            startAnimationTo(targetX, targetY, toGrid);
            this.onAnimationComplete = onComplete;
        }
        
        void startAnimationTo(float targetX, float targetY, boolean toGrid, boolean curveUp, Runnable onComplete) {
            startAnimationTo(targetX, targetY, toGrid);
            this.curveUp = curveUp;
            this.useStraightLine = false; // Utiliser la courbe par défaut
            this.onAnimationComplete = onComplete;
        }
        
        void startReturnAnimationTo(float targetX, float targetY, boolean toGrid, Runnable onComplete) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.isAnimating = true;
            this.animationTime = 0f;
            this.animationDuration = returnAnimationDuration; // Utiliser la durée pour le retour
            this.useStraightLine = true; // Utiliser ligne droite pour le retour
            this.isPlaced = toGrid;
            this.onAnimationComplete = onComplete;
        }
        
        void update(float delta) {
            if (!isAnimating) return;
            
            animationTime += delta;
            float progress = Math.min(animationTime / animationDuration, 1f);
            
            if (useStraightLine) {
                // Interpolation linéaire pour une trajectoire en ligne droite (retour)
                currentX = startX + (targetX - startX) * progress;
                currentY = startY + (targetY - startY) * progress;
            } else {
                // Courbe de Bézier quadratique pour une trajectoire courbe (aller)
                float midX = (startX + targetX) / 2;
                float midY;
                if (curveUp) {
                    midY = Math.max(startY, targetY) + 100; // Point de contrôle plus haut
                } else {
                    midY = Math.min(startY, targetY) - 100; // Point de contrôle plus bas
                }
                
                // Interpolation avec courbe de Bézier
                float t = progress;
                float invT = 1f - t;
                
                currentX = invT * invT * startX + 2 * invT * t * midX + t * t * targetX;
                currentY = invT * invT * startY + 2 * invT * t * midY + t * t * targetY;
            }
            
            if (progress >= 1f) {
                currentX = targetX;
                currentY = targetY;
                isAnimating = false;
                
                // Mettre à jour les positions de départ/cible pour la prochaine animation
                startX = currentX;
                startY = currentY;
                
                // Exécuter le callback s'il existe
                if (onAnimationComplete != null) {
                    onAnimationComplete.run();
                    onAnimationComplete = null;
                }
            }
        }
    }
    
    // Tokens
    private Array<Texture> tokenTextures;
    private Array<Texture> tokenPositionTextures; // Images de position de départ pour les tokens
    private static final int TOKENS_IN_COMBINATION = 4; // Nombre fixe de jetons dans la combinaison
    private static final int MAX_TOTAL_TOKENS = 6; // Maximum de jetons disponibles (pour le niveau hard)
    
    // Indicateurs de feedback
    private Texture dotWhiteTexture;   // Token correct et bien placé
    private Texture dotBlackTexture;   // Token correct mais mal placé
    private Texture dotEmptyTexture;   // Token incorrect
    
    // Système de tokens simplifié
    private Array<AnimatedToken> startPositionTokens; // Tokens statiques en position de départ
    private Array<AnimatedToken> movingTokens; // Tokens en cours d'animation vers la grille
    private Array<AnimatedToken> gridTokens; // Tokens placés définitivement sur la grille
    private Array<Integer> placedTokens; // Tokens placés dans la colonne courante (-1 si vide)
    
    // Coordonnées des positions calculées une seule fois
    private Array<Float> positionCentersX; // Coordonnées X des centres des positions
    private Array<Float> positionCentersY; // Coordonnées Y des centres des positions
    
    // Variables de calcul du background mutualisées
    private float currentBgX, currentBgY, currentBgWidth, currentBgHeight;
    private float currentScaleX, currentScaleY;
    
    // Nouvelles variables pour la transition vers la question finale
    private boolean isTransitioningToQuestion;
    private float questionTransitionTimer;
    private static final float WAIT_BEFORE_TRANSITION = 1.0f; // Attente d'une seconde avant la transition
    private static final float FADE_TO_BG_DURATION = 1.0f;
    private static final float FADE_FROM_BG_DURATION = 1.0f;
    private static final float QUESTION_TRANSITION_DURATION = WAIT_BEFORE_TRANSITION + FADE_TO_BG_DURATION + FADE_FROM_BG_DURATION;
    
    // Constants
    private static final String CORRECT_SOUND_PATH = "audio/win.mp3";
    private static final String INCORRECT_SOUND_PATH = "audio/sliding.mp3";
    private static final String WIN_SOUND_PATH = "audio/win.mp3";
    private static final float SYMBOL_SIZE = 40f;
    private static final float CURSOR_BLINK_SPEED = 1.0f;
    
    private boolean gameElementsDisabled = false;
    
    // Couleur de fond pour la phase de question finale
    private static final Color QUESTION_PHASE_BG_COLOR = new Color(0.15f, 0.15f, 0.2f, 1);
    
    /**
     * Constructeur avec paramètres dynamiques
     */
    public MastermindGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        // Stocker le thème
        this.theme = theme;
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.codeLength = TOKENS_IN_COMBINATION; // Toujours 4 jetons à deviner
        this.numberOfSymbols = 6; // Valeur par défaut si aucun paramètre n'est fourni
        this.backgroundColor = new Color(0.1f, 0.1f, 0.2f, 1);
        this.textColor = new Color(1, 1, 1, 1);
        this.correctColor = new Color(0.7f, 0.9f, 0.7f, 1);
        this.incorrectColor = new Color(0.9f, 0.7f, 0.7f, 1);
        
        // Stocker le paramètre symbolImages pour plus tard (après l'initialisation des symboles)
        String symbolImagesParam = null;
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("numberOfSymbols")) {
                this.numberOfSymbols = ((Number) parameters.get("numberOfSymbols")).intValue();
                // S'assurer que le nombre de symboles est entre 4 et 6
                this.numberOfSymbols = Math.max(4, Math.min(6, this.numberOfSymbols));
                System.out.println("Nombre de symboles défini à : " + this.numberOfSymbols);
            }
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String) parameters.get("bgColor");
                this.backgroundColor = parseColor(bgColor);
            }
            if (parameters.containsKey("textColor")) {
                String color = (String) parameters.get("textColor");
                this.textColor = parseColor(color);
            }
            if (parameters.containsKey("symbolImages")) {
                symbolImagesParam = (String) parameters.get("symbolImages");
            }
        }
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        System.out.println("Mode test Mastermind: " + isTestMode);
        
        // Initialiser les éléments UI
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.bigFont = new BitmapFont();
        this.bigFont.getData().setScale(1.8f);
        this.bigFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.infoFont = new BitmapFont();
        this.infoFont.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        this.layout = new GlyphLayout();
        
        this.backButton = new Rectangle(20, 20, 100, 50);
        this.submitButton = new Rectangle(0, 0, 120, 50); // Position sera calculée dynamiquement
        this.infoButton = new Rectangle(viewport.getWorldWidth() - 60, viewport.getWorldHeight() - 60, 40, 40);
        
        // Créer texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        
        // Charger la texture du bouton send
        try {
            this.sendButtonTexture = new Texture(Gdx.files.internal("images/ui/send.jpg"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton send: " + e.getMessage());
            this.sendButtonTexture = null;
        }
        
        // Charger la texture du bouton info
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }
        
        // Charger la texture du bouton close
        try {
            this.closeButtonTexture = new Texture(Gdx.files.internal("images/ui/close.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton close: " + e.getMessage());
            this.closeButtonTexture = null;
        }

        // Charger la texture du fond d'écran
        try {
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/mmd/background/background.png"));
            this.backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du fond d'écran: " + e.getMessage());
            this.backgroundTexture = null;
        }
        
        // Positionner le bouton d'information selon les coordonnées du background
        updateInfoButtonPosition();
        
        // Initialiser les variables de jeu
        this.secretCode = new Array<>();
        this.attempts = new Array<>();
        this.results = new Array<>();
        this.currentGuess = new Array<>();
        this.gameWon = false;
        this.gameFinished = false;
        this.finalPhase = false;
        this.finalQuestionPhase = false;
        this.showWinImage = false;
        
        // Initialiser le système de tokens simplifié
        this.startPositionTokens = new Array<>();
        this.movingTokens = new Array<>();
        this.gridTokens = new Array<>();
        this.placedTokens = new Array<>();
        this.positionCentersX = new Array<>();
        this.positionCentersY = new Array<>();
        for (int i = 0; i < TOKENS_IN_COMBINATION; i++) {
            placedTokens.add(-1); // -1 signifie slot vide
        }
        this.showInfoPanel = false;
        this.inputText = "";
        this.inputFocused = true;
        this.cursorBlinkTimer = 0;
        this.showCursor = true;
        this.symbolTextures = null;
        this.isTransitioning = false;
        this.transitionTimer = 0;
        
        // Initialiser le générateur de nombres aléatoires et les variables d'image
        this.random = new Random();
        this.visibleSquares = new HashSet<>();
        this.imageSquares = null;
        
        // Initialiser les noms des symboles
        System.out.println("Initialisation des symboles...");
        initializeSymbols();
        System.out.println("Symboles initialisés");
        
        // Charger les images de symboles selon les paramètres
        System.out.println("Chargement des textures de symboles...");
        if (symbolImagesParam != null && !symbolImagesParam.trim().isEmpty()) {
            System.out.println("Chargement des symboles depuis les paramètres...");
            loadSymbolImages(symbolImagesParam);
        } else {
            System.out.println("Aucun paramètre symbolImages, chargement par défaut...");
            loadDefaultSymbolImages();
        }
        
        // Charger les sons
        System.out.println("Chargement des sons...");
        loadSounds();
        System.out.println("Sons chargés");
        
        // Créer l'input processor pour les clics
        System.out.println("Création de l'input processor...");
        createInputProcessor();
        System.out.println("Input processor créé");
        
        // Charger la texture de la case
        try {
            this.boxTexture = new Texture(Gdx.files.internal("images/games/mmd/box/box-close.png"));
            this.boxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la texture box-close: " + e.getMessage());
            this.boxTexture = null;
        }
        
        // Charger les positions des cases depuis le JSON
        loadGridPositions();
        
        // Charger les frames d'animation partagées
        BoxAnimation.loadSharedFrames(DEFAULT_ANIMATION_VARIANT);
        
        // Charger les textures des tokens
        loadTokenTextures();
        
        System.out.println("Constructeur MastermindGameScreen terminé avec succès");
    }
    
    @Override
    protected Theme loadTheme(int day) {
        return theme; // Le thème est déjà fourni au constructeur
    }
    
    @Override
    protected void handleInput() {
        // La gestion des entrées est déléguée à l'InputProcessor créé dans createInputProcessor()
        // Cette méthode peut rester vide car toutes les interactions sont gérées via les clics
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
        return new Color(1, 1, 1, 1);
    }
    
    private void initializeSymbols() {
        // Initialiser les noms des symboles par défaut
        symbolNames = new String[numberOfSymbols];
        
        String[] baseNames = {
            "Symbole 1", "Symbole 2", "Symbole 3", "Symbole 4", 
            "Symbole 5", "Symbole 6", "Symbole 7", "Symbole 8"
        };
        
        for (int i = 0; i < numberOfSymbols; i++) {
            symbolNames[i] = baseNames[i % baseNames.length];
        }
    }
    
    private void loadDefaultSymbolImages() {
        try {
            System.out.println("Chargement des symboles par défaut pour " + numberOfSymbols + " symboles");
            // Utiliser le preset "formes_mixtes" par défaut, en prenant seulement le nombre de symboles nécessaire
            String defaultSymbols = "rouge01_triangle.png,vert01_carre.png,bleu01_pyramyde.png,jaune01_rond.png,orange01_octogone.png,violet01_etoile.png,bleu05_cercle.png,rouge04_triangle.png";
            System.out.println("Symboles par défaut: " + defaultSymbols);
            loadSymbolImages(defaultSymbols);
            System.out.println("Chargement des symboles par défaut terminé");
        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE dans loadDefaultSymbolImages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadSymbolImages(String symbolImages) {
        try {
            System.out.println("Début du chargement des symboles: " + symbolImages);
            
            // Vérifier que symbolNames est initialisé
            if (symbolNames == null) {
                System.err.println("ERREUR: symbolNames n'est pas initialisé, initialisation en cours...");
                symbolNames = new String[numberOfSymbols];
                for (int i = 0; i < numberOfSymbols; i++) {
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
            
            String[] imageNames = symbolImages.split(",");
            symbolTextures = new Texture[numberOfSymbols];
            
            System.out.println("Nombre de symboles à charger: " + numberOfSymbols + ", Images disponibles: " + imageNames.length);
            
            for (int i = 0; i < numberOfSymbols; i++) {
                try {
                    if (i < imageNames.length) {
                        String imageName = imageNames[i].trim();
                        if (!imageName.isEmpty()) {
                            String imagePath = "images/games/mmd/symbol/" + imageName;
                            System.out.println("Chargement symbole " + i + ": " + imagePath);
                            
                            // Vérifier que le fichier existe
                            if (Gdx.files.internal(imagePath).exists()) {
                                System.out.println("Fichier trouvé, chargement en cours...");
                                symbolTextures[i] = new Texture(Gdx.files.internal(imagePath));
                                System.out.println("✓ Texture chargée pour symbole " + i + ": " + imagePath);
                            } else {
                                System.err.println("✗ Fichier non trouvé: " + imagePath);
                                createErrorTexture(i);
                            }
                        } else {
                            System.out.println("Nom d'image vide pour symbole " + i + ", création texture par défaut");
                            createErrorTexture(i);
                        }
                    } else {
                        System.out.println("Pas assez d'images (" + imageNames.length + " disponibles, " + numberOfSymbols + " requis), création texture par défaut pour symbole " + i);
                        createErrorTexture(i);
                    }
                } catch (Exception e) {
                    System.err.println("ERREUR lors du chargement du symbole " + i + ": " + e.getMessage());
                    e.printStackTrace();
                    createErrorTexture(i);
                }
            }
            
            System.out.println("Chargement des textures terminé, mise à jour des noms...");
            
            // Mettre à jour les noms des symboles basés sur les fichiers d'images
            for (int i = 0; i < numberOfSymbols; i++) {
                try {
                    System.out.println("Mise à jour du nom pour symbole " + i);
                    if (i < imageNames.length) {
                        String imageName = imageNames[i].trim();
                        System.out.println("Nom d'image pour symbole " + i + ": '" + imageName + "'");
                        if (!imageName.isEmpty() && imageName.contains("_")) {
                            String[] parts = imageName.split("_");
                            System.out.println("Parties du nom: " + java.util.Arrays.toString(parts));
                            if (parts.length >= 2) {
                                String color = parts[0];
                                String shape = parts[1].replace(".png", "");
                                symbolNames[i] = color + " " + shape;
                            } else {
                                symbolNames[i] = imageName.replace(".png", "");
                            }
                        } else if (!imageName.isEmpty()) {
                            symbolNames[i] = imageName.replace(".png", "");
                        } else {
                            symbolNames[i] = "Symbole " + (i + 1);
                        }
                    } else {
                        symbolNames[i] = "Symbole " + (i + 1);
                    }
                    System.out.println("✓ Nom du symbole " + i + ": " + symbolNames[i]);
                } catch (Exception e) {
                    System.err.println("✗ Erreur lors de la mise à jour du nom du symbole " + i + ": " + e.getMessage());
                    e.printStackTrace();
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
            
            System.out.println("Chargement des symboles terminé avec succès");
            
        } catch (Exception e) {
            System.err.println("ERREUR CRITIQUE dans loadSymbolImages: " + e.getMessage());
            e.printStackTrace();
            // Créer des textures d'erreur seulement pour les symboles qui n'ont pas pu être chargés
            if (symbolTextures == null) {
                symbolTextures = new Texture[numberOfSymbols];
            }
            for (int i = 0; i < numberOfSymbols; i++) {
                if (symbolTextures[i] == null) {
                    System.out.println("Création de texture d'erreur pour symbole manquant " + i);
                    createErrorTexture(i);
                }
                if (symbolNames[i] == null) {
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
        }
    }
    
    private void createErrorTexture(int symbolIndex) {
        try {
            System.out.println("ATTENTION: Création d'une texture d'erreur pour symbole " + symbolIndex);
            // Créer une texture rouge d'erreur
            Pixmap errorPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            errorPixmap.setColor(Color.RED);
            errorPixmap.fill();
            symbolTextures[symbolIndex] = new Texture(errorPixmap);
            errorPixmap.dispose();
            System.out.println("✓ Texture d'erreur créée pour symbole " + symbolIndex);
        } catch (Exception e) {
            System.err.println("✗ Impossible de créer la texture d'erreur pour symbole " + symbolIndex + ": " + e.getMessage());
            e.printStackTrace();
            symbolTextures[symbolIndex] = null;
        }
    }
    
    private void loadSounds() {
        try {
            correctSound = Gdx.audio.newSound(Gdx.files.internal(CORRECT_SOUND_PATH));
            incorrectSound = Gdx.audio.newSound(Gdx.files.internal(INCORRECT_SOUND_PATH));
            winSound = Gdx.audio.newSound(Gdx.files.internal(WIN_SOUND_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sons: " + e.getMessage());
        }
    }
    
    private void createInputProcessor() {
        System.out.println("Début de création de l'InputAdapter...");
        inputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldCoords = camera.unproject(new Vector3(screenX, screenY, 0));
                handleClick(worldCoords.x, worldCoords.y);
                return true;
            }
            
            @Override
            public boolean keyTyped(char character) {
                if (!finalQuestionPhase || gameFinished) return false;
                
                // Gérer les caractères imprimables et accentués
                if (Character.isLetterOrDigit(character) || 
                    character == ' ' || character == '\t' || character == '\u00A0' || // espaces divers
                    isAcceptableSymbol(character)) {
                    inputText += character;
                    return true;
                }
                return false;
            }
            
            @Override
            public boolean keyDown(int keycode) {
                // Gestion de la touche R pour résoudre (en mode test uniquement)
                if (keycode == Input.Keys.R && isTestMode && !gameFinished) {
                    solveGame();
                    return true;
                }
                
                if (!finalQuestionPhase || gameFinished) return false;
                
                if (keycode == Input.Keys.BACKSPACE && inputText.length() > 0) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                    return true;
                } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    submitFinalAnswer();
                    return true;
                }
                return false;
            }
            
            // Méthode pour valider les symboles acceptables
            private boolean isAcceptableSymbol(char character) {
                // Accepter certains symboles utiles pour les réponses
                String acceptableSymbols = ".-'\"()[]{},:;!?/\\";
                return acceptableSymbols.indexOf(character) != -1;
            }
        };
        System.out.println("InputAdapter créé avec succès");
    }
    
    @Override
    protected void onScreenActivated() {
        super.onScreenActivated();
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
    protected void initializeGame() {
        // Générer le code secret
        generateSecretCode();
        
        // Initialiser le système de tokens simplifié
        initializeStartPositionTokens();
        
        // Charger la texture du thème
        loadThemeTexture();

        // Démarrer l'animation de la première colonne
        startColumnAnimation(0);
    }
    
    /**
     * Calcule les dimensions et l'échelle du background une seule fois
     */
    private void calculateBackgroundDimensions() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        if (backgroundTexture != null) {
            float bgRatio = (float)backgroundTexture.getWidth() / backgroundTexture.getHeight();
            float screenRatio = screenWidth / screenHeight;
            
            if (screenRatio > bgRatio) {
                currentBgHeight = screenHeight;
                currentBgWidth = currentBgHeight * bgRatio;
            } else {
                currentBgHeight = screenHeight;
                currentBgWidth = currentBgHeight * bgRatio;
                if (currentBgWidth > screenWidth) {
                    currentBgWidth = screenWidth;
                    currentBgHeight = currentBgWidth / bgRatio;
                }
            }
            
            currentBgX = (screenWidth - currentBgWidth) / 2;
            currentBgY = (screenHeight - currentBgHeight) / 2;
            
            currentScaleX = currentBgWidth / backgroundTexture.getWidth();
            currentScaleY = currentBgHeight / backgroundTexture.getHeight();
        }
    }
    
    /**
     * Calcule la position des boutons d'information et close en fonction des coordonnées du background
     * Le bouton help sera centré sur des coordonnées spécifiques dans le référentiel de l'image background
     * Le bouton close sera positionné à un décalage vertical par rapport au help
     * La taille sera gérée lors du dessin avec l'échelle du background
     */
    private void updateInfoButtonPosition() {
        if (backgroundTexture != null) {
            // Coordonnées dans le référentiel de l'image background
            float bgX = 1867f;
            float bgY = 959f;
            
            // Taille de base des boutons (sera redimensionnée lors du dessin)
            float baseButtonSize = 40f;
            
            // Convertir en coordonnées écran pour le bouton help
            float helpScreenX = currentBgX + (bgX * currentScaleX);
            float helpScreenY = currentBgY + (bgY * currentScaleY);
            
            // Positionner le bouton help
            infoButton.setSize(baseButtonSize, baseButtonSize);
            infoButton.setPosition(
                helpScreenX - baseButtonSize / 2,
                helpScreenY - baseButtonSize / 2
            );
            
            // Positionner le bouton close avec un décalage vertical (dans le référentiel background)
            float closeBgY = bgY + 105f;
            float closeScreenX = currentBgX + (bgX * currentScaleX);
            float closeScreenY = currentBgY + (closeBgY * currentScaleY);
            
            backButton.setSize(baseButtonSize, baseButtonSize);
            backButton.setPosition(
                closeScreenX - baseButtonSize / 2,
                closeScreenY - baseButtonSize / 2
            );
        } else {
            // Fallback: position et taille par défaut si pas de background
            infoButton.setSize(40, 40);
            infoButton.setPosition(viewport.getWorldWidth() - 60, viewport.getWorldHeight() - 60);
            backButton.setSize(40, 40);
            backButton.setPosition(20, 20);
        }
    }
    
    /**
     * Initialise les tokens statiques en position de départ pour un nouveau tour
     */
    private void initializeStartPositionTokens() {
        startPositionTokens.clear();
        
        // Réinitialiser les slots placés
        for (int i = 0; i < placedTokens.size; i++) {
            placedTokens.set(i, -1);
        }
        
        // Créer les tokens statiques en position de départ
        for (int i = 0; i < numberOfSymbols; i++) {
            AnimatedToken token = new AnimatedToken(i, 0, 0); // Position sera définie au premier rendu
            startPositionTokens.add(token);
        }
    }
    
    private void generateSecretCode() {
        secretCode.clear();
        
        // Générer un code avec des tokens uniques
        Array<Integer> availableTokens = new Array<>();
        for (int i = 0; i < numberOfSymbols; i++) { // Utiliser numberOfSymbols au lieu de MAX_TOTAL_TOKENS
            availableTokens.add(i);
        }
        
        // Mélanger et prendre les premiers tokens pour le code
        availableTokens.shuffle();
        for (int i = 0; i < TOKENS_IN_COMBINATION; i++) {
            secretCode.add(availableTokens.get(i));
        }
        
        // Debug - afficher le code secret en mode test
        if (isTestMode) {
            System.out.print("Code secret (tokens): ");
            for (int token : secretCode) {
                System.out.print(token + " ");
            }
            System.out.println();
            System.out.println("Nombre total de symboles disponibles: " + numberOfSymbols);
        }
    }
    
    private void loadThemeTexture() {
        if (theme != null && fullImageTexture == null) {
            String fullImagePath = theme.getFullImagePath();
            
            if (fullImagePath != null && !fullImagePath.isEmpty()) {
                try {
                    fullImageTexture = new Texture(Gdx.files.internal(fullImagePath));
                    // Découper l'image en carrés une fois qu'elle est chargée
                    createImageSquares();
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de la texture du thème: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Découpe l'image en carrés pour l'affichage progressif
     */
    private void createImageSquares() {
        if (fullImageTexture == null) {
            return;
        }
        
        imageSquares = new TextureRegion[GRID_ROWS][GRID_COLS];
        int squareWidth = fullImageTexture.getWidth() / GRID_COLS;
        int squareHeight = fullImageTexture.getHeight() / GRID_ROWS;
        
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int x = col * squareWidth;
                int y = row * squareHeight;
                imageSquares[row][col] = new TextureRegion(fullImageTexture, x, y, squareWidth, squareHeight);
            }
        }
    }
    
    /**
     * Calcule quels carrés doivent être visibles basé sur le score
     */
    private void calculateVisibleSquares() {
        visibleSquares.clear();
        
        // Calculer le score basé sur le nombre de tentatives utilisées
        float scorePercentage = Math.max(0, (float)(maxAttempts - attempts.size) / maxAttempts);
        
        // Calculer le nombre de carrés à afficher (minimum 1, maximum 50 carrés = 50% de l'image)
        int maxSquares = TOTAL_IMAGE_SQUARES / 2; // 50 carrés maximum
        int squaresToShow = Math.max(1, (int) (scorePercentage * maxSquares));
        
        // Créer une liste de tous les indices de carrés possibles (0-99)
        Array<Integer> availableSquares = new Array<>();
        for (int i = 0; i < TOTAL_IMAGE_SQUARES; i++) {
            availableSquares.add(i);
        }
        
        // Mélanger et sélectionner aléatoirement les carrés à afficher
        availableSquares.shuffle();
        for (int i = 0; i < squaresToShow; i++) {
            visibleSquares.add(availableSquares.get(i));
        }
    }
    
    /**
     * Charge la question finale spécifique au thème depuis theme_questions.json
     */
    private void loadFinalQuestion() {
        if (theme != null) {
            String themeName = theme.getName();
            String themeTitle = theme.getTitle();
            
            try {
                JsonReader jsonReader = new JsonReader();
                JsonValue root = jsonReader.parse(Gdx.files.internal("theme_questions.json"));
                
                JsonValue themeQuestions = root.get("themeQuestions");
                if (themeQuestions != null && themeQuestions.has(themeName)) {
                    JsonValue themeQuestion = themeQuestions.get(themeName);
                    
                    String question = themeQuestion.getString("question");
                    String answer = themeQuestion.getString("answer");
                    
                    finalQuestion = new QuestionData(question, answer);
                    
                    // Charger les réponses alternatives
                    JsonValue alternativesArray = themeQuestion.get("alternatives");
                    if (alternativesArray != null) {
                        for (JsonValue alt = alternativesArray.child; alt != null; alt = alt.next) {
                            finalQuestion.acceptedAnswers.add(alt.asString());
                        }
                    }
                    
                    return;
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la question finale: " + e.getMessage());
            }
            
            // Fallback: utiliser les métadonnées du thème
            finalQuestion = new QuestionData(
                "Quel est le titre de cette oeuvre ?",
                themeTitle
            );
            
            // Ajouter des alternatives basées sur le nom du thème
            if (themeName != null) {
                finalQuestion.acceptedAnswers.add(themeName.replace("_", " "));
                finalQuestion.acceptedAnswers.add(themeName.replace("_", " ").toLowerCase());
                finalQuestion.acceptedAnswers.add(themeTitle.toLowerCase());
            }
        } else {
            // Question par défaut si pas de thème
            finalQuestion = new QuestionData(
                "Quel est le nom de cette oeuvre ?",
                "oeuvre"
            );
        }
    }
    
    private void handleClick(float x, float y) {
        // Si le panneau d'info est visible, n'importe quel clic le ferme
        if (showInfoPanel) {
            showInfoPanel = false;
            return;
        }
        
        // Ignorer les clics pendant la transition
        if (isTransitioning) {
            return;
        }
        
        if (gameFinished) {
            if (finalPhase) {
                returnToMainMenu();
            }
            return;
        }
        
        // Vérifier si c'est un clic sur les boutons
        if (backButton.contains(x, y)) {
            returnToMainMenu();
            return;
        }
        
        // Le bouton de validation a été supprimé - validation automatique
        
        // Le bouton Résoudre a été remplacé par la touche R
        
        if (infoButton.contains(x, y)) {
            showInfoPanel = true;
            return;
        }
        
        // Nouveau système de gestion des clics sur les tokens
        handleTokenClick(x, y);
    }
    

    
    /**
     * Nouvelle méthode pour gérer les clics sur les tokens avec le système d'animation
     */
    private void handleTokenClick(float x, float y) {
        if (gameFinished || finalQuestionPhase) return;
        
        // Calculer les dimensions du fond d'écran pour les coordonnées
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        if (backgroundTexture == null) return;
        
        float bgRatio = (float)backgroundTexture.getWidth() / backgroundTexture.getHeight();
        float screenRatio = screenWidth / screenHeight;
        
        float bgWidth, bgHeight, bgX, bgY;
        if (screenRatio > bgRatio) {
            bgHeight = screenHeight;
            bgWidth = bgHeight * bgRatio;
        } else {
            bgHeight = screenHeight;
            bgWidth = bgHeight * bgRatio;
            if (bgWidth > screenWidth) {
                bgWidth = screenWidth;
                bgHeight = bgWidth / bgRatio;
            }
        }
        
        bgX = (screenWidth - bgWidth) / 2;
        bgY = (screenHeight - bgHeight) / 2;
        float scaleX = bgWidth / backgroundTexture.getWidth();
        float scaleY = bgHeight / backgroundTexture.getHeight();
        
        // 1. Vérifier les clics sur les tokens en position de départ
        for (AnimatedToken token : startPositionTokens) {
            float tokenSize = 50 * scaleX; // Taille approximative du token
            if (x >= token.currentX - tokenSize/2 && x <= token.currentX + tokenSize/2 &&
                y >= token.currentY - tokenSize/2 && y <= token.currentY + tokenSize/2) {
                
                // Trouver le premier slot libre
                int freeSlot = findNextFreeSlot();
                if (freeSlot != -1) {
                    createMovingToken(token.tokenType, freeSlot);
                }
                return;
            }
        }
        
        // 2. Vérifier les clics sur les tokens placés dans la grille
        for (AnimatedToken token : gridTokens) {
            float tokenSize = 50 * scaleX; // Taille approximative du token
            if (x >= token.currentX - tokenSize/2 && x <= token.currentX + tokenSize/2 &&
                y >= token.currentY - tokenSize/2 && y <= token.currentY + tokenSize/2) {
                
                // Supprimer le token de la grille
                removeTokenFromGrid(token);
                return;
            }
        }
    }
    
    /**
     * Trouve le prochain slot libre dans la grille
     */
    private int findNextFreeSlot() {
        for (int i = 0; i < placedTokens.size; i++) {
            if (placedTokens.get(i) == -1) {
                return i;
            }
        }
        return -1; // Aucun slot libre
    }
    
    /**
     * Trouve un token par son type dans la grille
     */
    private AnimatedToken findTokenByType(int tokenType) {
        for (AnimatedToken token : gridTokens) {
            if (token.tokenType == tokenType) {
                return token;
            }
        }
        return null;
    }
    
    /**
     * Crée un nouveau token en mouvement vers la grille
     */
    private void createMovingToken(int tokenType, int slot) {
        if (gridPositions != null && attempts.size < gridPositions.size) {
            AnimatedColumn activeColumn = gridPositions.get(attempts.size);
            if (slot < activeColumn.rectangles.size) {
                // Masquer temporairement le token de départ correspondant
                for (AnimatedToken startToken : startPositionTokens) {
                    if (startToken.tokenType == tokenType) {
                        startToken.isVisible = false;
                        break;
                    }
                }
                
                // Position de départ (copier depuis le token statique)
                float startX = positionCentersX.get(tokenType);
                float startY = positionCentersY.get(tokenType);
                
                // Position cible sur la grille
                Rectangle rect = activeColumn.rectangles.get(slot);
                float targetX = currentBgX + (rect.x + rect.width/2) * currentScaleX;
                float targetY = currentBgY + (rect.y + rect.height/2) * currentScaleY;
                
                // Créer le token en mouvement
                AnimatedToken movingToken = new AnimatedToken(tokenType, startX, startY);
                movingToken.gridSlot = slot;
                
                // Marquer le slot comme réservé
                placedTokens.set(slot, tokenType);
                
                // Démarrer l'animation avec callback
                movingToken.startAnimationTo(targetX, targetY, true, () -> {
                    // À la fin de l'animation, créer un token statique sur la grille
                    AnimatedToken gridToken = new AnimatedToken(tokenType, targetX, targetY);
                    gridToken.gridSlot = slot;
                    gridToken.isPlaced = true;
                    gridTokens.add(gridToken);
                    
                    // Vérifier la validation automatique
                    checkAutoValidation();
                });
                
                movingTokens.add(movingToken);
            }
        }
    }
    
    /**
     * Supprime un token de la grille (il disparaît simplement)
     */
    private void removeTokenFromGrid(AnimatedToken token) {
        // Libérer le slot
        if (token.gridSlot != -1) {
            placedTokens.set(token.gridSlot, -1);
        }
        
        // Supprimer le token de la grille immédiatement
        gridTokens.removeValue(token, true);
        
        // Créer un token en mouvement pour l'animation de retour
        AnimatedToken returningToken = new AnimatedToken(token.tokenType, token.currentX, token.currentY);
        
        // Position cible : position de départ
        float targetX = positionCentersX.get(token.tokenType);
        float targetY = positionCentersY.get(token.tokenType);
        
        // Démarrer l'animation de retour en ligne droite
        returningToken.startReturnAnimationTo(targetX, targetY, false, () -> {
            // À la fin de l'animation, rendre visible le token de départ
            for (AnimatedToken startToken : startPositionTokens) {
                if (startToken.tokenType == token.tokenType) {
                    startToken.isVisible = true;
                    break;
                }
            }
        });
        
        movingTokens.add(returningToken);
    }
    
    /**
     * Vérifie si tous les tokens sont placés et valide automatiquement
     */
    private void checkAutoValidation() {
        boolean allPlaced = true;
        
        for (int slot : placedTokens) {
            if (slot == -1) {
                allPlaced = false;
                break;
            }
        }
        
        if (allPlaced) {
            // Validation automatique immédiate (appelée depuis le callback donc animation finie)
            submitGuess();
        }
    }
    
    /**
     * Prépare un nouveau tour en créant de nouveaux tokens de départ
     */
    private void resetTokensForNextAttempt() {
        // Réinitialiser les slots placés
        for (int i = 0; i < placedTokens.size; i++) {
            placedTokens.set(i, -1);
        }
        
        // Vider les tokens de la grille courante (ils restent dans l'historique via drawGridAndTokens)
        gridTokens.clear();
        
        // Vider les tokens en mouvement (ne devrait pas arriver mais par sécurité)
        movingTokens.clear();
        
        // Rendre tous les tokens de départ visibles à nouveau
        for (AnimatedToken startToken : startPositionTokens) {
            startToken.isVisible = true;
        }
    }

    
    private void submitGuess() {
        if (gameFinished) return;
        
        // Créer une copie de la tentative courante basée sur les tokens placés
        Array<Integer> guess = new Array<>();
        for (int tokenType : placedTokens) {
            guess.add(tokenType);
        }
        
        // Ajouter aux tentatives
        attempts.add(guess);
        
        // Calculer le résultat
        GuessResult result = calculateResult(guess);
        results.add(result);
        
        // Vérifier si le jeu est gagné
        if (result.correctPosition == TOKENS_IN_COMBINATION) {
            gameWon = true;
            
            // Démarrer la transition vers la question finale
            isTransitioningToQuestion = true;
            questionTransitionTimer = 0;
            
            // Charger la question finale spécifique au thème
            loadFinalQuestion();
            
            // Calculer quels carrés de l'image doivent être visibles basé sur le score
            calculateVisibleSquares();
            
            if (winSound != null) {
                winSound.play();
            }
            
        } else if (attempts.size >= maxAttempts) {
            // Jeu perdu
            gameFinished = true;
            
            if (incorrectSound != null) {
                incorrectSound.play();
            }
            
        } else {
            // Démarrer l'animation de la prochaine colonne seulement si le jeu n'est pas gagné
            if (attempts.size < gridPositions.size) {
                startColumnAnimation(attempts.size);
                // Réinitialiser les tokens pour la prochaine tentative
                resetTokensForNextAttempt();
            }
            
            if (correctSound != null) {
                correctSound.play();
            }
        }
    }
    
    private GuessResult calculateResult(Array<Integer> guess) {
        int correctPosition = 0;
        int correctSymbol = 0;
        
        // Compter les positions exactes
        boolean[] usedSecret = new boolean[codeLength];
        boolean[] usedGuess = new boolean[codeLength];
        
        for (int i = 0; i < codeLength; i++) {
            if (guess.get(i).equals(secretCode.get(i))) {
                correctPosition++;
                usedSecret[i] = true;
                usedGuess[i] = true;
            }
        }
        
        // Compter les symboles corrects mais mal placés
        for (int i = 0; i < codeLength; i++) {
            if (!usedGuess[i]) {
                for (int j = 0; j < codeLength; j++) {
                    if (!usedSecret[j] && guess.get(i).equals(secretCode.get(j))) {
                        correctSymbol++;
                        usedSecret[j] = true;
                        break;
                    }
                }
            }
        }
        
        return new GuessResult(correctPosition, correctSymbol);
    }
    
    private int calculateScore() {
        // Score basé sur le nombre de tentatives utilisées
        // Plus on trouve rapidement, plus le score est élevé
        float efficiency = (float)(maxAttempts - attempts.size) / maxAttempts;
        return Math.min(100, Math.max(10, (int)(efficiency * 100)));
    }
    
    private void startFinalPhase() {
        finalPhase = true;
        calculateVisibleSquares();
        
        // Afficher l'image immédiatement
        showWinImage = true;
    }
    
    private void solveGame() {
        System.out.println("Résolution automatique du jeu Mastermind (mode test)");
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, 100);
            adventGame.setVisited(dayId, true);
        }
        
        // Jouer le son de victoire
        if (winSound != null) {
            winSound.play();
        }
        
        // Retourner au menu immédiatement
        returnToMainMenu();
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour les animations des tokens en mouvement
        for (AnimatedToken token : movingTokens) {
            token.update(delta);
        }
        
        // Supprimer les tokens qui ont fini leur animation
        Array<AnimatedToken> tokensToRemove = new Array<>();
        for (AnimatedToken token : movingTokens) {
            if (!token.isAnimating) {
                tokensToRemove.add(token);
            }
        }
        for (AnimatedToken token : tokensToRemove) {
            movingTokens.removeValue(token, true);
        }
        
        // Mettre à jour le clignotement du curseur dans la phase finale
        if (finalQuestionPhase && !gameFinished && !isTransitioning) {
            cursorBlinkTimer += delta * CURSOR_BLINK_SPEED;
            showCursor = (cursorBlinkTimer % 2.0f) < 1.0f;
        }
        
        // Mettre à jour la transition
        if (isTransitioning) {
            transitionTimer += delta;
            
            if (transitionTimer >= TRANSITION_DURATION) {
                // Transition terminée, passer à la phase finale normale
                isTransitioning = false;
                gameFinished = true;
                finalPhase = true;
                transitionTimer = 0;
            }
        }

        // Mettre à jour la transition vers la question finale
        if (isTransitioningToQuestion) {
            questionTransitionTimer += delta;
            
            // Vérifier si on doit désactiver les éléments du jeu
            if (!gameElementsDisabled && questionTransitionTimer > WAIT_BEFORE_TRANSITION + FADE_TO_BG_DURATION) {
                disableGameElements();
            }
            
            if (questionTransitionTimer >= QUESTION_TRANSITION_DURATION) {
                // Transition terminée, passer à la phase de question
                isTransitioningToQuestion = false;
                finalQuestionPhase = true;
                questionTransitionTimer = 0;
            }
        }
        
        // Mettre à jour les animations
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                // Mettre à jour le fade des tokens
                column.updateTokensFade(delta);
                
                if (column.isAnimating) {
                    boolean allFinished = true;
                    
                    for (int i = 0; i < column.animations.size; i++) {
                        BoxAnimation anim = column.animations.get(i);
                        
                        // Mettre à jour le timer
                        anim.timer += delta;
                        
                        // Vérifier si l'animation doit commencer (après le délai)
                        if (anim.timer >= anim.delay && !anim.isPlaying) {
                            anim.isPlaying = true;
                            anim.timer = 0;
                        }
                        
                        // Si l'animation est en cours
                        if (anim.isPlaying) {
                            // Avancer l'animation
                            if (anim.timer >= FRAME_DURATION) {
                                anim.currentFrame++;
                                anim.timer = 0;
                                
                                // Vérifier si l'animation est terminée
                                if (anim.currentFrame >= BoxAnimation.sharedFrames.size) {
                                    anim.currentFrame = BoxAnimation.sharedFrames.size - 1; // Rester sur la dernière frame
                                } else {
                                    allFinished = false;
                                }
                            } else {
                                allFinished = false;
                            }
                        } else {
                            allFinished = false;
                        }
                    }
                    
                    // Si toutes les animations de la colonne sont terminées
                    if (allFinished) {
                        column.isAnimating = false;
                        column.animationComplete = true;
                    }
                }
            }
        }
    }
    
    @Override
    protected void renderGame() {
        // Ne dessiner le fond d'écran et les éléments du jeu que s'ils ne sont pas désactivés
        if (!gameElementsDisabled) {
            // Dessiner le fond d'écran
            if (backgroundTexture != null) {
                batch.setColor(1, 1, 1, 1);
                
                // Calculer les dimensions du background une seule fois
                calculateBackgroundDimensions();
                // Mettre à jour la position du bouton d'information
                updateInfoButtonPosition();
                
                // Dessiner d'abord un fond uni
                batch.setColor(backgroundColor);
                batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
                
                // Puis dessiner le plateau de jeu centré
                batch.setColor(1, 1, 1, 1);
                batch.draw(backgroundTexture, currentBgX, currentBgY, currentBgWidth, currentBgHeight);
                
                // Dessiner la colonne des positions de tokens (utilise les variables calculées)
                drawTokenPositions();
                
                // Dessiner les cases et les jetons seulement si on n'est pas dans la phase finale
                if (gridPositions != null) {
                    // Dessiner les cases et les jetons
                    drawGridAndTokens(currentBgX, currentBgY, currentScaleX, currentScaleY);
                }
                
                // Dessiner tous les types de tokens
                drawAllTokens(currentScaleX, currentScaleY);
            } else {
                // Fallback : fond uni si la texture n'est pas chargée
                batch.setColor(backgroundColor);
                batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            }
        }

        // Toujours dessiner l'interface appropriée
        if (isTransitioningToQuestion) {
            drawTransitionToQuestion();
        } else if (isTransitioning) {
            drawTransition();
        } else if (finalQuestionPhase && !gameFinished) {
            drawFinalQuestionPhase();
        } else if (finalPhase && showWinImage) {
            drawFinalPhase();
        } else if (!gameElementsDisabled) {
            drawGameInterface();
        }
        
        // Dessiner le panneau d'info par-dessus tout le reste s'il est visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
    }
    
    private void drawGameInterface() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Dessiner les résultats des tentatives précédentes
        drawAttemptResults();
        
        // Dessiner les boutons
        drawButtons();
        
        // Dessiner les boutons d'info et close avec l'échelle du background
        drawInfoButton(currentScaleX, currentScaleY);
        drawCloseButton(currentScaleX, currentScaleY);
        
        // Message de fin de jeu
        if (gameFinished) {
            drawGameEndMessage();
        }
    }
    
    private void drawAttemptResults() {
        if (gridPositions == null || results.size == 0) return;
        
        // Afficher les résultats pour chaque tentative terminée
        for (int attemptIndex = 0; attemptIndex < results.size; attemptIndex++) {
            if (attemptIndex < gridPositions.size) {
                AnimatedColumn column = gridPositions.get(attemptIndex);
                GuessResult result = results.get(attemptIndex);
                
                // Calculer le centre de la colonne (au-dessus de la première case)
                if (column.rectangles.size > 0) {
                    Rectangle firstRect = column.rectangles.get(0);
                    
                    // Centre de la première case de la colonne
                    float columnCenterX = currentBgX + (firstRect.x + firstRect.width/2) * currentScaleX;
                    float columnCenterY = currentBgY + (firstRect.y + firstRect.height/2) * currentScaleY;
                    
                    // Position des indicateurs au-dessus de la colonne
                    // Décalage vertical pour placer les indicateurs au-dessus
                    float offsetY = 118f * currentScaleY;
                    float feedbackCenterY = columnCenterY + offsetY;
                    
                    drawResultFeedback(result, columnCenterX, feedbackCenterY, currentScaleX, currentScaleY);
                }
            }
        }
    }
    
    private void drawResultFeedback(GuessResult result, float centerX, float centerY, float scaleX, float scaleY) {
        // Taille de base des indicateurs (en pixels dans l'image originale) avec facteur 4x
        float baseIndicatorSize = 40f;
        float baseSpacing = 22f;
        
        // Appliquer l'échelle comme le bouton close (même logique que les tokens)
        float indicatorSize = baseIndicatorSize * scaleX; // Même échelle que le background
        float spacing = baseSpacing * scaleX; // Espacement proportionnel
        
        // Créer un tableau de 4 indicateurs
        Texture[] indicators = new Texture[4];
        
        // Remplir avec les indicateurs blancs (positions correctes)
        for (int i = 0; i < result.correctPosition; i++) {
            indicators[i] = dotWhiteTexture;
        }
        
        // Remplir avec les indicateurs noirs (symboles corrects mais mal placés)
        for (int i = result.correctPosition; i < result.correctPosition + result.correctSymbol; i++) {
            indicators[i] = dotBlackTexture;
        }
        
        // Remplir le reste avec les indicateurs vides
        for (int i = result.correctPosition + result.correctSymbol; i < 4; i++) {
            indicators[i] = dotEmptyTexture;
        }
        
        // Positions des 4 indicateurs en carré tourné à 45 degrés (comme un signe plus)
        // Position 0: haut
        // Position 1: droite  
        // Position 2: bas
        // Position 3: gauche
        float[][] positions = {
            {centerX, centerY + spacing},           // Haut
            {centerX + spacing, centerY},           // Droite
            {centerX, centerY - spacing},           // Bas
            {centerX - spacing, centerY}            // Gauche
        };
        
        // Dessiner les 4 indicateurs
        batch.setColor(1, 1, 1, 1); // Couleur blanche pour les textures
        for (int i = 0; i < 4; i++) {
            if (indicators[i] != null) {
                float indicatorX = positions[i][0] - indicatorSize / 2; // Centrer horizontalement
                float indicatorY = positions[i][1] - indicatorSize / 2; // Centrer verticalement
                batch.draw(indicators[i], indicatorX, indicatorY, indicatorSize, indicatorSize);
            }
        }
    }
    
    
    private void drawButtons() {
        // Le bouton retour a été remplacé par le bouton close avec image
        
        // Le bouton de validation a été supprimé - validation automatique
        
        // Le bouton Résoudre a été remplacé par la touche R
    }
    
    private void updateSubmitButtonPosition() {
        if (finalQuestionPhase) {
            // En phase finale, positionner à droite de la zone de saisie de la question finale
            updateSubmitButtonPositionForFinalPhase();
        } else {
            // En jeu normal, positionner au centre en bas
            submitButton.setSize(50, 50); // Bouton carré
            submitButton.setPosition(
                (viewport.getWorldWidth() - submitButton.width) / 2,
                50
            );
        }
    }
    
    private void updateSubmitButtonPositionForFinalPhase() {
        // Calculer la position de la zone de saisie dans la phase finale
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer la position Y de la zone de saisie selon la disposition de la phase finale
        float currentY = screenHeight - 30; // Score
        currentY -= (font.getLineHeight() + 15); // Après le score
        if (finalQuestion != null) {
            currentY -= (font.getLineHeight() + 15); // Après la question
        }
        float inputBoxY = currentY - inputBoxHeight / 2; // Position de la zone de saisie
        float inputBoxX = (screenWidth - inputBoxWidth) / 2;
        
        // Définir la taille du bouton send (carré basé sur la hauteur de la zone de saisie)
        float sendButtonSize = inputBoxHeight;
        
        // Positionner le bouton send juste à côté du bord droit de la zone de saisie
        submitButton.setPosition(
            inputBoxX + inputBoxWidth + 10, // Marge entre la zone de saisie et le bouton
            inputBoxY
        );
        submitButton.setSize(sendButtonSize, sendButtonSize);
    }
    
    private void drawGameEndMessage() {
        float centerX = viewport.getWorldWidth() / 2;
        float centerY = viewport.getWorldHeight() / 2;
        
        if (gameWon) {
            bigFont.setColor(correctColor);
            String message = "Bravo!";
            layout.setText(bigFont, message);
            bigFont.draw(batch, layout, 
                centerX - layout.width / 2,
                centerY + 50);
            
            font.setColor(textColor);
            String scoreText = "Score: " + calculateScore() + "%";
            layout.setText(font, scoreText);
            font.draw(batch, layout, 
                centerX - layout.width / 2,
                centerY);
                
        } else {
            bigFont.setColor(incorrectColor);
            String message = "Échec!";
            layout.setText(bigFont, message);
            bigFont.draw(batch, layout, 
                centerX - layout.width / 2,
                centerY + 50);
            
            font.setColor(textColor);
            String codeText = "Code: ";
            for (int i = 0; i < secretCode.size; i++) {
                codeText += symbolNames[secretCode.get(i)];
                if (i < secretCode.size - 1) codeText += " ";
            }
            layout.setText(font, codeText);
            font.draw(batch, layout, 
                centerX - layout.width / 2,
                centerY);
        }
        
        // Instructions pour continuer
        font.setColor(textColor);
        String instruction = gameWon ? "Cliquez pour voir l'image!" : "Cliquez pour continuer";
        layout.setText(font, instruction);
        font.draw(batch, layout, 
            centerX - layout.width / 2,
            centerY - 50);
    }
    
    private void drawTransition() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        if (transitionTimer <= FADE_TO_BLACK_DURATION) {
            // Phase 1: Fondu de l'image partielle vers le noir
            float fadeProgress = transitionTimer / FADE_TO_BLACK_DURATION;
            
            // Dessiner l'image partielle qui disparaît progressivement
            drawFinalQuestionPhaseImageOnly();
            
            // Superposer un voile noir qui s'intensifie
            batch.setColor(0, 0, 0, fadeProgress);
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
            
        } else {
            // Phase 2: Fondu du noir vers l'image complète
            float fadeProgress = (transitionTimer - FADE_TO_BLACK_DURATION) / FADE_TO_IMAGE_DURATION;
            fadeProgress = Math.min(1.0f, fadeProgress); // Limiter à 1.0
            
            // Dessiner l'image complète
            if (fullImageTexture != null) {
                // Calculer la taille adaptative pour l'image complète
                float originalWidth = fullImageTexture.getWidth();
                float originalHeight = fullImageTexture.getHeight();
                float aspectRatio = originalWidth / originalHeight;
                
                float maxWidth = screenWidth * 0.8f;
                float maxHeight = screenHeight * 0.8f;
                
                float imageWidth, imageHeight;
                if (maxWidth / aspectRatio <= maxHeight) {
                    imageWidth = maxWidth;
                    imageHeight = imageWidth / aspectRatio;
                } else {
                    imageHeight = maxHeight;
                    imageWidth = imageHeight * aspectRatio;
                }
                
                float imageX = (screenWidth - imageWidth) / 2;
                float imageY = (screenHeight - imageHeight) / 2;
                
                // Dessiner l'image complète avec l'alpha qui augmente
                batch.setColor(1f, 1f, 1f, fadeProgress);
                batch.draw(fullImageTexture, imageX, imageY, imageWidth, imageHeight);
            }
            
            // Superposer un voile noir qui s'estompe
            batch.setColor(0, 0, 0, 1.0f - fadeProgress);
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
        }
        
        // Titre "Bonne réponse!" centré
        batch.setColor(correctColor.r, correctColor.g, correctColor.b, 1.0f);
        bigFont.setColor(correctColor);
        String title = "Bonne réponse!";
        layout.setText(bigFont, title);
        bigFont.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 50);
    }
    
    private void drawFinalQuestionPhaseImageOnly() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer l'espace pour l'image comme dans drawFinalQuestionPhase
        float currentY = screenHeight - 30; // Score
        currentY -= (font.getLineHeight() + 15); // Après le score
        if (finalQuestion != null) {
            currentY -= (font.getLineHeight() + 15); // Après la question
        }
        currentY -= (50 + 15); // Après la zone de saisie
        
        float bottomMargin = 80;
        float availableHeight = currentY - bottomMargin;
        
        // Afficher l'image adaptative partielle
        if (fullImageTexture != null && availableHeight > 100) {
            drawAdaptiveImage(availableHeight);
        }
    }
    
    private void drawFinalPhase() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Titre
        bigFont.setColor(correctColor);
        String title = "Félicitations!";
        layout.setText(bigFont, title);
        bigFont.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 30);
        
        // Score
        font.setColor(textColor);
        String scoreText = "Score: " + calculateScore() + "%";
        layout.setText(font, scoreText);
        font.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 80);
        
        // Calculer l'espace disponible pour l'image
        float availableHeight = screenHeight - 150; // Laisser de l'espace pour le titre et score
        
        // Afficher l'image complète
        if (fullImageTexture != null && availableHeight > 100) {
            drawCompleteImage(availableHeight);
        }
        
        // Instructions
        font.setColor(textColor);
        String instruction = "Cliquez pour retourner au menu";
        layout.setText(font, instruction);
        font.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            30);
    }
    
    private void drawCompleteImage(float availableHeight) {
        if (fullImageTexture == null) {
            return;
        }
        
        // Obtenir les dimensions originales de l'image
        float originalWidth = fullImageTexture.getWidth();
        float originalHeight = fullImageTexture.getHeight();
        float aspectRatio = originalWidth / originalHeight;
        
        // Calculer les dimensions adaptatives
        float screenWidth = viewport.getWorldWidth();
        float maxWidth = screenWidth * 0.8f; // Maximum 80% de la largeur de l'écran
        
        float imageHeight = Math.min(availableHeight, maxWidth / aspectRatio);
        float imageWidth = imageHeight * aspectRatio;
        
        // Si l'image est trop large, ajuster par la largeur
        if (imageWidth > maxWidth) {
            imageWidth = maxWidth;
            imageHeight = imageWidth / aspectRatio;
        }
        
        // Centrer l'image horizontalement
        float imageX = (screenWidth - imageWidth) / 2;
        
        // Positionner l'image verticalement dans l'espace disponible
        float imageY = (availableHeight - imageHeight) / 2 + 50; // Centré avec marge
        
        // Dessiner l'image complète
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(fullImageTexture, imageX, imageY, imageWidth, imageHeight);
    }
    
    private void drawFinalQuestionPhase() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer le score pour l'affichage
        int score = calculateScore();
        
        // Variables pour le positionnement vertical depuis le haut
        float currentY = screenHeight - 30; // Commencer 30px du haut
        float spacing = 15; // Espacement entre les éléments
        
        // 1. Afficher le score du Mastermind en haut
        font.setColor(textColor);
        String scoreText = "Score Mastermind: " + score + "%";
        layout.setText(font, scoreText);
        font.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            currentY);
        currentY -= (layout.height + spacing);
        
        // 2. Afficher la question finale
        if (finalQuestion != null) {
            font.setColor(textColor);
            layout.setText(font, finalQuestion.question);
            font.draw(batch, layout, 
                (screenWidth - layout.width) / 2,
                currentY);
            currentY -= (layout.height + spacing);
        }
        
        // 3. Dessiner la zone de saisie
        float inputBoxHeight = 50;
        drawInputAreaCenteredAt(currentY - inputBoxHeight / 2);
        currentY -= (inputBoxHeight + spacing);
        
        // 4. Calculer l'espace disponible pour l'image
        float bottomMargin = 80; // Marge en bas pour les boutons
        float availableHeight = currentY - bottomMargin;
        
        // 5. Afficher l'image adaptative
        if (fullImageTexture != null && availableHeight > 100) { // Minimum 100px de hauteur
            drawAdaptiveImage(availableHeight);
        }
        
        // 6. Dessiner les boutons en bas
        // Le bouton retour a été remplacé par le bouton close avec image
        drawCloseButton(currentScaleX, currentScaleY);
        // Le bouton de validation a été supprimé pour la phase finale aussi
        if (sendButtonTexture != null) {
            // Garder le bouton send pour la phase finale seulement
            updateSubmitButtonPosition();
            drawSendButton(submitButton);
        }
    }
    
    private void drawInputAreaCenteredAt(float centerY) {
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float inputBoxX = (viewport.getWorldWidth() - inputBoxWidth) / 2;
        float inputBoxY = centerY - inputBoxHeight / 2;
        
        // Fond de la zone de saisie
        batch.setColor(0.2f, 0.2f, 0.3f, 1);
        batch.draw(whiteTexture, inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight);
        
        // Bordure de la zone de saisie
        batch.setColor(inputFocused ? correctColor : textColor);
        float borderWidth = 2;
        // Haut
        batch.draw(whiteTexture, inputBoxX, inputBoxY + inputBoxHeight - borderWidth, inputBoxWidth, borderWidth);
        // Bas
        batch.draw(whiteTexture, inputBoxX, inputBoxY, inputBoxWidth, borderWidth);
        // Gauche
        batch.draw(whiteTexture, inputBoxX, inputBoxY, borderWidth, inputBoxHeight);
        // Droite
        batch.draw(whiteTexture, inputBoxX + inputBoxWidth - borderWidth, inputBoxY, borderWidth, inputBoxHeight);
        
        // Texte de saisie
        font.setColor(correctColor);
        String displayText = inputText;
        if (inputFocused && showCursor) {
            displayText += "|";
        }
        layout.setText(font, displayText);
        font.draw(batch, layout, 
            inputBoxX + 10,
            inputBoxY + inputBoxHeight / 2 + layout.height / 2);
    }
    
    /**
     * Dessine l'image adaptative qui s'ajuste à l'espace disponible tout en conservant le ratio
     */
    private void drawAdaptiveImage(float availableHeight) {
        if (fullImageTexture == null || imageSquares == null) {
            return;
        }
        
        // Obtenir les dimensions originales de l'image
        float originalWidth = fullImageTexture.getWidth();
        float originalHeight = fullImageTexture.getHeight();
        float aspectRatio = originalWidth / originalHeight;
        
        // Calculer les dimensions adaptatives
        float screenWidth = viewport.getWorldWidth();
        float maxWidth = screenWidth * 0.8f; // Maximum 80% de la largeur de l'écran
        
        float imageHeight = Math.min(availableHeight, maxWidth / aspectRatio);
        float imageWidth = imageHeight * aspectRatio;
        
        // Si l'image est trop large, ajuster par la largeur
        if (imageWidth > maxWidth) {
            imageWidth = maxWidth;
            imageHeight = imageWidth / aspectRatio;
        }
        
        // Centrer l'image horizontalement
        float imageX = (screenWidth - imageWidth) / 2;
        
        // Positionner l'image verticalement dans l'espace disponible
        float imageY = (availableHeight - imageHeight) / 2 + 50; // Centré avec marge
        
        // Calculer la taille de chaque carré
        float squareWidth = imageWidth / GRID_COLS;
        float squareHeight = imageHeight / GRID_ROWS;
        
        // Dessiner un fond noir pour l'image
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(whiteTexture, imageX, imageY, imageWidth, imageHeight);
        
        // Dessiner seulement les carrés visibles
        batch.setColor(1f, 1f, 1f, 1f);
        
        int squareIndex = 0;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                // Arrêter après 100 carrés
                if (squareIndex >= TOTAL_IMAGE_SQUARES) {
                    break;
                }
                
                // Dessiner le carré seulement s'il est dans la liste des carrés visibles
                if (visibleSquares.contains(squareIndex)) {
                    float squareX = imageX + col * squareWidth;
                    float squareY = imageY + (GRID_ROWS - 1 - row) * squareHeight; // Inverser Y pour LibGDX
                    
                    batch.draw(imageSquares[row][col], squareX, squareY, squareWidth, squareHeight);
                }
                
                squareIndex++;
            }
            
            // Arrêter après 100 carrés
            if (squareIndex >= TOTAL_IMAGE_SQUARES) {
                break;
            }
        }
    }
    
    private void drawButton(Rectangle button, String text, Color color) {
        // Fond du bouton
        batch.setColor(0.3f, 0.3f, 0.4f, 1);
        batch.draw(whiteTexture, button.x, button.y, button.width, button.height);
        
        // Bordure du bouton
        batch.setColor(color);
        float borderWidth = 2;
        // Haut
        batch.draw(whiteTexture, button.x, button.y + button.height - borderWidth, button.width, borderWidth);
        // Bas
        batch.draw(whiteTexture, button.x, button.y, button.width, borderWidth);
        // Gauche
        batch.draw(whiteTexture, button.x, button.y, borderWidth, button.height);
        // Droite
        batch.draw(whiteTexture, button.x + button.width - borderWidth, button.y, borderWidth, button.height);
        
        // Texte du bouton
        font.setColor(color);
        layout.setText(font, text);
        font.draw(batch, layout, 
            button.x + (button.width - layout.width) / 2,
            button.y + (button.height + layout.height) / 2);
    }
    
    private void drawSendButton(Rectangle button) {
        if (sendButtonTexture != null) {
            // Dessiner l'image du bouton send
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            batch.draw(sendButtonTexture, button.x, button.y, button.width, button.height);
        } else {
            // Fallback: dessiner un bouton texte si l'image n'est pas disponible
            drawButton(button, "Valider", textColor);
        }
    }
    
    private void drawInfoButton(float scaleX, float scaleY) {
        if (infoButtonTexture != null) {
            // Dessiner l'image du bouton info avec la même logique que les tokens
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            
            // Calculer la taille du bouton basée sur l'échelle (comme les tokens)
            float buttonWidth = infoButtonTexture.getWidth() * scaleX;
            float buttonHeight = infoButtonTexture.getHeight() * scaleY;
            
            // Centrer le bouton sur sa position
            float drawX = infoButton.x + (infoButton.width - buttonWidth) / 2;
            float drawY = infoButton.y + (infoButton.height - buttonHeight) / 2;
            
            batch.draw(infoButtonTexture, drawX, drawY, buttonWidth, buttonHeight);
        } else {
            // Fallback: dessiner un bouton rectangulaire simple si l'image n'est pas disponible
            batch.setColor(0.3f, 0.6f, 0.9f, 0.8f);
            batch.draw(whiteTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);
            
            // Bordure noire
            batch.setColor(0, 0, 0, 1);
            float borderWidth = 2;
            // Haut
            batch.draw(whiteTexture, infoButton.x, infoButton.y + infoButton.height - borderWidth, infoButton.width, borderWidth);
            // Bas
            batch.draw(whiteTexture, infoButton.x, infoButton.y, infoButton.width, borderWidth);
            // Gauche
            batch.draw(whiteTexture, infoButton.x, infoButton.y, borderWidth, infoButton.height);
            // Droite
            batch.draw(whiteTexture, infoButton.x + infoButton.width - borderWidth, infoButton.y, borderWidth, infoButton.height);
            
            // Lettre "i"
            bigFont.setColor(Color.WHITE);
            layout.setText(bigFont, "i");
            float centerX = infoButton.x + infoButton.width / 2;
            float centerY = infoButton.y + infoButton.height / 2;
            bigFont.draw(batch, layout, 
                centerX - layout.width / 2,
                centerY + layout.height / 2);
        }
    }
    
    private void drawCloseButton(float scaleX, float scaleY) {
        if (closeButtonTexture != null) {
            // Dessiner l'image du bouton close avec la même logique que les tokens
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            
            // Calculer la taille du bouton basée sur l'échelle (comme les tokens)
            float buttonWidth = closeButtonTexture.getWidth() * scaleX;
            float buttonHeight = closeButtonTexture.getHeight() * scaleY;
            
            // Centrer le bouton sur sa position
            float drawX = backButton.x + (backButton.width - buttonWidth) / 2;
            float drawY = backButton.y + (backButton.height - buttonHeight) / 2;
            
            batch.draw(closeButtonTexture, drawX, drawY, buttonWidth, buttonHeight);
        } else {
            // Fallback: dessiner un bouton rectangulaire simple si l'image n'est pas disponible
            drawButton(backButton, "Retour", textColor);
        }
    }
    
    private void drawInfoPanel() {
        float screenWidth = viewport.getWorldWidth();
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
        
        // Adapter la taille de la police selon la taille du panneau
        float scaleFactor = Math.min(panelWidth / 500f, panelHeight / 400f);
        float titleScale = Math.max(1.0f, 1.5f * scaleFactor);
        float textScale = Math.max(0.8f, 1.0f * scaleFactor);
        
        // Titre avec police adaptative
        infoFont.getData().setScale(titleScale);
        infoFont.setColor(0.2f, 0.3f, 0.8f, 1);
        String title = "Règles du Mastermind";
        layout.setText(infoFont, title);
        infoFont.draw(batch, layout, 
            panelX + (panelWidth - layout.width) / 2,
            panelY + panelHeight - 30 * scaleFactor);
        
        // Contenu des règles avec police adaptative
        infoFont.getData().setScale(textScale);
        infoFont.setColor(0.1f, 0.1f, 0.2f, 1);
        float textY = panelY + panelHeight - 70 * scaleFactor;
        float lineHeight = 20 * scaleFactor;
        float leftMargin = 15 * scaleFactor;
        
        String[] rules = {
            "Objectif :",
            "Deviner le code secret composé de " + codeLength + " symboles uniques.",
            "",
            "Comment jouer :",
            "• Cliquez sur les symboles en bas pour sélectionner",
            "• Cliquez sur votre tentative pour changer les symboles",
            "• Cliquez sur le bouton d'envoi pour soumettre",
            "",
            "Feedback (à droite de chaque tentative) :",
            "• Rond vert foncé = bon symbole, bonne position",
            "• Cercle vert clair = bon symbole, mauvaise position",
            "",
            "Vous avez " + maxAttempts + " tentatives maximum.",
            "",
            "📱 Cliquez n'importe où pour fermer cette aide."
        };
        
        for (String rule : rules) {
            if (!rule.isEmpty()) {
                // Gérer les titres en gras
                if (rule.endsWith(":") && !rule.startsWith("•")) {
                    infoFont.setColor(0.2f, 0.3f, 0.8f, 1);
                    float tempScale = textScale * 1.1f;
                    infoFont.getData().setScale(tempScale);
                    layout.setText(infoFont, rule);
                    infoFont.draw(batch, layout, panelX + leftMargin, textY);
                    infoFont.getData().setScale(textScale);
                    infoFont.setColor(0.1f, 0.1f, 0.2f, 1);
                } else {
                    layout.setText(infoFont, rule);
                    infoFont.draw(batch, layout, panelX + leftMargin, textY);
                }
            }
            textY -= lineHeight;
        }
        
        // Indicateur de fermeture
        infoFont.getData().setScale(textScale * 0.9f);
        infoFont.setColor(0.5f, 0.5f, 0.6f, 1);
        String closeHint = "Tapez pour fermer";
        layout.setText(infoFont, closeHint);
        infoFont.draw(batch, layout, 
            panelX + panelWidth - layout.width - 10,
            panelY + 15);
        
        // Remettre la police à sa taille normale
        infoFont.getData().setScale(1.0f);
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        // Les boutons se repositionnent automatiquement basé sur la taille de l'écran
        // Le bouton d'information se positionne selon les coordonnées du background
        updateInfoButtonPosition();
        
        // Adapter la police d'information à la nouvelle taille d'écran
        float screenDiagonal = (float) Math.sqrt(width * width + height * height);
        float baseScale = Math.max(0.8f, Math.min(1.5f, screenDiagonal / 1000f));
        infoFont.getData().setScale(baseScale);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        bigFont.dispose();
        infoFont.dispose();
        whiteTexture.dispose();
        if (fullImageTexture != null) fullImageTexture.dispose();
        if (sendButtonTexture != null) sendButtonTexture.dispose();
        if (infoButtonTexture != null) infoButtonTexture.dispose();
        if (closeButtonTexture != null) closeButtonTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose(); // Dispose the new texture
        if (correctSound != null) correctSound.dispose();
        if (incorrectSound != null) incorrectSound.dispose();
        if (winSound != null) winSound.dispose();
        
        // Libérer les textures des symboles
        if (symbolTextures != null) {
            for (Texture texture : symbolTextures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
            symbolTextures = null;
        }
        
        // Nettoyer les ressources d'image découpée
        imageSquares = null;
        if (visibleSquares != null) {
            visibleSquares.clear();
        }
        if (boxTexture != null) boxTexture.dispose();
        
        // Nettoyer les animations
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                column.dispose();
            }
        }
        
        // Nettoyer les frames d'animation partagées
        BoxAnimation.disposeSharedFrames();

        // Nettoyer les textures des tokens
        if (tokenTextures != null) {
            for (Texture texture : tokenTextures) {
                texture.dispose();
            }
            tokenTextures.clear();
        }
        
        // Nettoyer les textures de position des tokens
        if (tokenPositionTextures != null) {
            for (Texture texture : tokenPositionTextures) {
                texture.dispose();
            }
            tokenPositionTextures.clear();
        }
        
        // Nettoyer les textures d'indicateurs
        if (dotWhiteTexture != null) dotWhiteTexture.dispose();
        if (dotBlackTexture != null) dotBlackTexture.dispose();
        if (dotEmptyTexture != null) dotEmptyTexture.dispose();
    }
    
    /**
     * Soumets la réponse finale et termine le jeu
     */
    private void submitFinalAnswer() {
        if (finalQuestion == null || inputText.trim().isEmpty()) {
            return;
        }
        
        boolean isCorrect = finalQuestion.isAnswerCorrect(inputText, false); // case insensitive
        
        if (isCorrect) {
            // Démarrer la transition avec fondu noir vers image complète
            isTransitioning = true;
            transitionTimer = 0;
            
            // Calculer le score final avec bonus pour la question finale
            int baseScore = calculateScore();
            int finalScore = Math.min(100, baseScore + 20); // +20% pour la question finale réussie
            
            if (game instanceof AdventCalendarGame) {
                AdventCalendarGame adventGame = (AdventCalendarGame) game;
                adventGame.setScore(dayId, finalScore);
                adventGame.setVisited(dayId, true);
            }
            
            if (winSound != null) {
                winSound.play();
            }
        } else {
            // Mauvaise réponse, permettre de réessayer
            if (incorrectSound != null) {
                incorrectSound.play();
            }
        }
        
        inputText = ""; // Vider le champ de saisie
    }
    
    private void loadGridPositions() {
        gridPositions = new Array<>();
        try {
            String jsonContent = Gdx.files.internal("images/games/mmd/background/background-mask.json").readString();
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(jsonContent);
            
            // Compter le nombre de colonnes pour définir maxAttempts
            int columnCount = 0;
            for (JsonValue columnValue = root.child; columnValue != null; columnValue = columnValue.next) {
                columnCount++;
            }
            // Mettre à jour maxAttempts avec le nombre de colonnes
            this.maxAttempts = columnCount;
            
            for (JsonValue columnValue = root.child; columnValue != null; columnValue = columnValue.next) {
                int colIndex = columnValue.getInt("col");
                AnimatedColumn column = new AnimatedColumn(colIndex);
                
                JsonValue rectanglesArray = columnValue.get("rectangles");
                for (JsonValue rectValue = rectanglesArray.child; rectValue != null; rectValue = rectValue.next) {
                    Rectangle rect = new Rectangle(
                        rectValue.getInt("x"),
                        rectValue.getInt("y"),
                        rectValue.getInt("width"),
                        rectValue.getInt("height")
                    );
                    column.rectangles.add(rect);
                }
                
                gridPositions.add(column);
            }
            
            // Trier les colonnes par index
            gridPositions.sort((c1, c2) -> Integer.compare(c1.col, c2.col));
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des positions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Ajouter une méthode pour démarrer l'animation d'une colonne
    private void startColumnAnimation(int columnIndex) {
        if (gridPositions != null && columnIndex >= 0 && columnIndex < gridPositions.size) {
            AnimatedColumn column = gridPositions.get(columnIndex);
            column.startAnimations();
        }
    }

    private void loadTokenTextures() {
        tokenTextures = new Array<>();
        tokenPositionTextures = new Array<>();
        
        for (int i = 1; i <= MAX_TOTAL_TOKENS; i++) {
            try {
                // Charger la texture du token
                String tokenPath = "images/games/mmd/token/" + (i < 10 ? "0" : "") + i + ".png";
                if (Gdx.files.internal(tokenPath).exists()) {
                    Texture tokenTexture = new Texture(Gdx.files.internal(tokenPath));
                    tokenTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    tokenTextures.add(tokenTexture);
                } else {
                    System.err.println("Token texture non trouvée: " + tokenPath);
                    // Créer une texture de remplacement
                    Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                    pixmap.setColor(1, 0, 0, 1);
                    pixmap.fill();
                    Texture fallbackTexture = new Texture(pixmap);
                    tokenTextures.add(fallbackTexture);
                    pixmap.dispose();
                }
                
                // Charger la texture de position correspondante
                String positionPath = "images/games/mmd/token/" + (i < 10 ? "0" : "") + i + "s.png";
                if (Gdx.files.internal(positionPath).exists()) {
                    Texture positionTexture = new Texture(Gdx.files.internal(positionPath));
                    positionTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    tokenPositionTextures.add(positionTexture);
                } else {
                    System.err.println("Token position texture non trouvée: " + positionPath);
                    // Créer une texture de remplacement transparente
                    Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                    pixmap.setColor(0, 0, 0, 0.3f); // Gris semi-transparent
                    pixmap.fill();
                    Texture fallbackTexture = new Texture(pixmap);
                    tokenPositionTextures.add(fallbackTexture);
                    pixmap.dispose();
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement du token " + i + ": " + e.getMessage());
                // Créer une texture de remplacement en cas d'erreur pour le token
                Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                pixmap.setColor(1, 0, 0, 1);
                pixmap.fill();
                Texture fallbackTexture = new Texture(pixmap);
                tokenTextures.add(fallbackTexture);
                pixmap.dispose();
                
                // Créer une texture de remplacement pour la position
                Pixmap posPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                posPixmap.setColor(0, 0, 0, 0.3f);
                posPixmap.fill();
                Texture fallbackPosTexture = new Texture(posPixmap);
                tokenPositionTextures.add(fallbackPosTexture);
                posPixmap.dispose();
            }
        }
        
        // Charger les textures d'indicateurs
        loadIndicatorTextures();
    }
    
    /**
     * Charge les textures des indicateurs de feedback
     */
    private void loadIndicatorTextures() {
        try {
            // Charger dot-white.png (token correct et bien placé)
            dotWhiteTexture = new Texture(Gdx.files.internal("images/games/mmd/indicator/dot-white.png"));
            dotWhiteTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("✓ Texture dot-white chargée");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de dot-white.png: " + e.getMessage());
            dotWhiteTexture = null;
        }
        
        try {
            // Charger dot-black.png (token correct mais mal placé)
            dotBlackTexture = new Texture(Gdx.files.internal("images/games/mmd/indicator/dot-black.png"));
            dotBlackTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("✓ Texture dot-black chargée");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de dot-black.png: " + e.getMessage());
            dotBlackTexture = null;
        }
        
        try {
            // Charger dot-empty.png (token incorrect)
            dotEmptyTexture = new Texture(Gdx.files.internal("images/games/mmd/indicator/dot-empty.png"));
            dotEmptyTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            System.out.println("✓ Texture dot-empty chargée");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de dot-empty.png: " + e.getMessage());
            dotEmptyTexture = null;
        }
    }

    /**
     * Dessine la colonne des images de position des tokens à gauche de l'écran
     */
    private void drawTokenPositions() {
        if (tokenPositionTextures == null || tokenPositionTextures.size == 0) {
            return;
        }
        
        batch.setColor(1, 1, 1, 1);
        
        // Position de base relative au fond d'écran (coordonnées dans l'image originale)
        float baseX = 70;
        float baseY = 850;
        float spacing = 115f;
        
        // Convertir les coordonnées relatives au fond d'écran en coordonnées écran (utilise les variables calculées)
        float startX = currentBgX + (baseX * currentScaleX);
        float startY = currentBgY + (baseY * currentScaleY);
        float scaledSpacing = spacing * currentScaleY;
        
        // Vider et recalculer les coordonnées des centres
        positionCentersX.clear();
        positionCentersY.clear();
        
        // Dessiner seulement les images de position correspondant au nombre de tokens en jeu
        int tokensToDisplay = Math.min(numberOfSymbols, tokenPositionTextures.size);
        for (int i = 0; i < tokensToDisplay; i++) {
            Texture positionTexture = tokenPositionTextures.get(i);
            if (positionTexture != null) {
                // Calculer la taille de l'image en fonction de l'échelle
                float imageWidth = positionTexture.getWidth() * currentScaleX;
                float imageHeight = positionTexture.getHeight() * currentScaleY;
                
                // Calculer la position du centre de l'image
                float centerX = startX;
                float centerY = startY - (i * scaledSpacing);
                
                // Stocker les coordonnées du centre pour réutilisation
                positionCentersX.add(centerX);
                positionCentersY.add(centerY);
                
                // Convertir la position du centre en position du coin supérieur gauche
                float posX = centerX - (imageWidth / 2);
                float posY = centerY - (imageHeight / 2);
                
                batch.draw(positionTexture, posX, posY, imageWidth, imageHeight);
            }
        }
        
        // Mettre à jour les positions de départ des tokens animés
        updateTokenStartPositions();
    }
    
    /**
     * Met à jour les positions des tokens de départ en utilisant les coordonnées calculées
     */
    private void updateTokenStartPositions() {
        for (AnimatedToken token : startPositionTokens) {
            if (token.tokenType < positionCentersX.size) {
                // Utiliser exactement les mêmes coordonnées que l'image de position
                token.currentX = positionCentersX.get(token.tokenType);
                token.currentY = positionCentersY.get(token.tokenType);
                token.startX = token.currentX;
                token.startY = token.currentY;
            }
        }
    }
    
    /**
     * Dessine tous les types de tokens (position de départ, en mouvement, sur grille)
     */
    private void drawAllTokens(float scaleX, float scaleY) {
        if (tokenTextures == null) return;
        
        batch.setColor(1, 1, 1, 1);
        
        // Dessiner les tokens en position de départ (statiques) seulement s'ils sont visibles
        for (AnimatedToken token : startPositionTokens) {
            if (token.isVisible) {
                drawSingleToken(token, scaleX, scaleY);
            }
        }
        
        // Dessiner les tokens en mouvement
        for (AnimatedToken token : movingTokens) {
            drawSingleToken(token, scaleX, scaleY);
        }
        
        // Dessiner les tokens placés sur la grille (colonne courante seulement)
        for (AnimatedToken token : gridTokens) {
            drawSingleToken(token, scaleX, scaleY);
        }
    }
    
    /**
     * Dessine un token individuel
     */
    private void drawSingleToken(AnimatedToken token, float scaleX, float scaleY) {
        if (token.tokenType < tokenTextures.size) {
            Texture tokenTexture = tokenTextures.get(token.tokenType);
            if (tokenTexture != null) {
                // Calculer la taille du token basée sur l'échelle
                float tokenWidth = tokenTexture.getWidth() * scaleX;
                float tokenHeight = tokenTexture.getHeight() * scaleY;
                
                // Centrer le token sur sa position courante
                float drawX = token.currentX - tokenWidth / 2;
                float drawY = token.currentY - tokenHeight / 2;
                
                batch.draw(tokenTexture, drawX, drawY, tokenWidth, tokenHeight);
            }
        }
    }

    private void drawTransitionToQuestion() {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();

        if (questionTransitionTimer <= WAIT_BEFORE_TRANSITION) {
            // Phase 1: Attente, afficher simplement l'interface du jeu
            batch.setColor(1, 1, 1, 1);
            drawGameInterface();
        } else if (questionTransitionTimer <= WAIT_BEFORE_TRANSITION + FADE_TO_BG_DURATION) {
            // Phase 2: Fondu du jeu vers la couleur de fond de la question
            float fadeProgress = (questionTransitionTimer - WAIT_BEFORE_TRANSITION) / FADE_TO_BG_DURATION;
            
            // Dessiner l'interface du jeu qui disparaît seulement si les éléments ne sont pas désactivés
            if (!gameElementsDisabled) {
                batch.setColor(1, 1, 1, 1 - fadeProgress);
                drawGameInterface();
            }
            
            // Superposer la couleur de fond qui apparaît
            batch.setColor(
                QUESTION_PHASE_BG_COLOR.r,
                QUESTION_PHASE_BG_COLOR.g,
                QUESTION_PHASE_BG_COLOR.b,
                fadeProgress
            );
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
            
        } else {
            // Phase 3: Fondu de la couleur de fond vers l'interface de question
            float fadeProgress = (questionTransitionTimer - (WAIT_BEFORE_TRANSITION + FADE_TO_BG_DURATION)) / FADE_FROM_BG_DURATION;
            
            // Dessiner uniquement le fond plein
            batch.setColor(QUESTION_PHASE_BG_COLOR);
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
            
            // Dessiner l'interface de question qui apparaît progressivement
            batch.setColor(1, 1, 1, fadeProgress);
            drawFinalQuestionPhase();
        }
    }

    // Nouvelle méthode pour extraire la logique de dessin des cases et jetons
    private void drawGridAndTokens(float bgX, float bgY, float scaleX, float scaleY) {
        for (AnimatedColumn column : gridPositions) {
            for (int i = 0; i < column.rectangles.size; i++) {
                Rectangle rect = column.rectangles.get(i);
                float boxX = bgX + rect.x * scaleX;
                float boxY = bgY + rect.y * scaleY;
                float boxWidth = rect.width * scaleX;
                float boxHeight = rect.height * scaleY;
                
                // Choisir la texture à afficher pour la case
                Texture textureToRender = boxTexture;
                
                if (i < column.animations.size) {
                    BoxAnimation anim = column.animations.get(i);
                    if (anim.isPlaying && BoxAnimation.sharedFrames != null && BoxAnimation.sharedFrames.size > 0) {
                        int frameIndex = Math.min(anim.currentFrame, BoxAnimation.sharedFrames.size - 1);
                        textureToRender = BoxAnimation.sharedFrames.get(frameIndex);
                    }
                }
                
                // Dessiner la case
                batch.setColor(1, 1, 1, 1);
                batch.draw(textureToRender, boxX, boxY, boxWidth, boxHeight);
            }
        }

        // Dessiner seulement les tokens des tentatives précédentes (pas la colonne courante)
        for (AnimatedColumn column : gridPositions) {
            if (column.col < attempts.size) {
                Array<Integer> tokensToShow = attempts.get(column.col);
                
                for (int i = 0; i < Math.min(column.rectangles.size, TOKENS_IN_COMBINATION); i++) {
                    Rectangle rect = column.rectangles.get(i);
                    
                    if (i < tokensToShow.size && tokenTextures != null && tokensToShow.get(i) >= 0 && tokensToShow.get(i) < tokenTextures.size) {
                        Texture tokenTexture = tokenTextures.get(tokensToShow.get(i));
                        
                        if (tokenTexture != null) {
                            // Calculer la taille réelle du token (comme dans drawAnimatedTokens)
                            float tokenWidth = tokenTexture.getWidth() * scaleX;
                            float tokenHeight = tokenTexture.getHeight() * scaleY;
                            
                            // Calculer le centre de la case
                            float boxCenterX = bgX + (rect.x + rect.width/2) * scaleX;
                            float boxCenterY = bgY + (rect.y + rect.height/2) * scaleY;
                            
                            // Centrer le token dans la case (comme les tokens animés)
                            float tokenX = boxCenterX - tokenWidth / 2;
                            float tokenY = boxCenterY - tokenHeight / 2;
                            
                            batch.setColor(1, 1, 1, 1f);
                            batch.draw(tokenTexture, tokenX, tokenY, tokenWidth, tokenHeight);
                        }
                    }
                }
            }
        }
        // La colonne courante est maintenant entièrement gérée par les tokens animés
    }

    private void disableGameElements() {
        gameElementsDisabled = true;
        // Libérer les ressources du jeu
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
        if (boxTexture != null) {
            boxTexture.dispose();
            boxTexture = null;
        }
        if (tokenTextures != null) {
            for (Texture texture : tokenTextures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
            tokenTextures.clear();
        }
        if (tokenPositionTextures != null) {
            for (Texture texture : tokenPositionTextures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
            tokenPositionTextures.clear();
        }
        
        // Nettoyer les textures d'indicateurs
        if (dotWhiteTexture != null) {
            dotWhiteTexture.dispose();
            dotWhiteTexture = null;
        }
        if (dotBlackTexture != null) {
            dotBlackTexture.dispose();
            dotBlackTexture = null;
        }
        if (dotEmptyTexture != null) {
            dotEmptyTexture.dispose();
            dotEmptyTexture = null;
        }
        // Nettoyer les animations
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                column.dispose();
            }
            gridPositions.clear();
        }
        // Nettoyer les frames d'animation partagées
        BoxAnimation.disposeSharedFrames();
    }
} 