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
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.files.FileHandle;
import java.util.HashSet;
import java.util.Set;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.Config;
import com.widedot.calendar.display.DisplayConfig;
import com.widedot.calendar.utils.CarlitoFontManager;

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
    // Rectangle submitButton supprimé (plus de bouton de validation)
    private final Rectangle infoButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture fullImageTexture;
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
    private boolean showWinImage;
    private boolean showInfoPanel;
    
    // Variables pour le fade out et redémarrage
    private boolean isFadingOut;
    private float fadeOutTimer;
    private float fadeOutDuration = 2.0f; // 2 secondes pour le fade out
    private boolean isClosingBoxes;
    private float closeBoxesTimer;
    private float closeBoxesDuration = 1.0f; // 1 seconde pour fermer les cases
    private float restartDelay = 2.0f; // 2 secondes avant le redémarrage
    
    // Game parameters
        private Color correctColor;
    // incorrectColor supprimé (plus utilisé)
    private String[] symbolNames;
    private Texture[] symbolTextures;
    
    // Sounds
    private Sound winSound;
    private Sound wrongSound;
    private Sound failedSound;
    
    // Image fragments for final phase
    private static final int GRID_ROWS = 10;
    private static final int GRID_COLS = 10;
    private TextureRegion[][] imageSquares;
    private Set<Integer> visibleSquares;
    // random supprimé (plus utilisé)
    
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
    private static final float BOX_OPENING_DURATION = 0.5f; // Durée totale d'ouverture en secondes
    private static final float BOX_CLOSING_DURATION = 0.3f; // Durée totale de fermeture en secondes
    private static final float ANIMATION_DELAY = 0.0f;
    private static final int DEFAULT_ANIMATION_VARIANT = 2;
    
    // Animation state
    private static class BoxAnimation {
        int currentFrame;
        float timer;
        float delay;
        boolean isPlaying;
        boolean isClosing; // Flag pour indiquer si c'est une animation de fermeture
        static Array<Texture> sharedFrames; // Frames partagées entre toutes les animations
        
        BoxAnimation(float delay) {
            this.currentFrame = 0;
            this.timer = 0;
            this.delay = delay;
            this.isPlaying = false;
            this.isClosing = false;
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
            
            // Solution GWT-compatible : charger les frames par index numérique
            // Les frames sont nommées 001.png, 002.png, etc.
            // On essaie de charger jusqu'à ce qu'un fichier n'existe plus
            int frameIndex = 1;
            while (true) {
                // Formater le numéro avec padding à 3 chiffres (001, 002, etc.)
                String frameNumber = (frameIndex < 10 ? "00" : (frameIndex < 100 ? "0" : "")) + frameIndex;
                String framePath = "images/games/mmd/anim/opening/" + variantFolder + "/" + frameNumber + ".png";
                
                FileHandle frameFile = Gdx.files.internal(framePath);
                if (!frameFile.exists()) {
                    // Plus de frames à charger
                    break;
                }
                
                try {
                    Texture frameTexture = new Texture(frameFile);
                    frameTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    sharedFrames.add(frameTexture);
                    frameIndex++;
                } catch (Exception e) {
                    // Erreur de chargement, on arrête
                    Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de la frame " + framePath + ": " + e.getMessage());
                    break;
                }
            }
            
            Gdx.app.log("MastermindGameScreen", "Chargé " + sharedFrames.size + " frames d'animation pour la variante " + variantFolder);
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
        
        AnimatedColumn(int col) {
            super(col);
            this.animations = new Array<>();
            this.isAnimating = false;
            this.animationComplete = false;
        }
        
        void startAnimations() {
            if (!isAnimating) {
                isAnimating = true;
                animationComplete = false;
                
                // Créer une animation pour chaque case de la colonne
                for (int i = 0; i < rectangles.size; i++) {
                    BoxAnimation anim = new BoxAnimation(i * ANIMATION_DELAY);
                    animations.add(anim);
                }
            }
        }
        
        void updateTokensFade(float delta) {
            // Méthode conservée pour compatibilité mais plus utilisée
        }
        
        
        void reset() {
            this.isAnimating = false;
            this.animationComplete = false;
            // Réinitialiser les animations sans les supprimer
            for (BoxAnimation anim : this.animations) {
                anim.isPlaying = false;
                anim.isClosing = false;
                anim.currentFrame = 0;
                anim.timer = 0;
            }
        }
        
        void dispose() {
            animations.clear();
        }
    }
    
    /**
     * Acteur personnalisé pour les panneaux de transition
     */
    private class TransitionPanelActor extends Actor {
        private boolean isLeftPanel;
        private Texture panelTexture;
        
        public TransitionPanelActor(boolean isLeftPanel) {
            this.isLeftPanel = isLeftPanel;
            this.panelTexture = null; // Sera définie lors de la capture
        }
        
        public void setPanelTexture(Texture texture) {
            this.panelTexture = texture;
        }
        
        @Override
        public void draw(com.badlogic.gdx.graphics.g2d.Batch batch, float parentAlpha) {
            if (panelTexture == null || !isVisible()) return;
            
            float alpha = getColor().a * parentAlpha;
            batch.setColor(1, 1, 1, alpha);
            
            // Calculer la partie de texture à dessiner
            int texW = panelTexture.getWidth();
            int texH = panelTexture.getHeight();
            int halfW = texW / 2;
            
            if (isLeftPanel) {
                // Panneau gauche - partie gauche de la texture
                batch.draw(
                    panelTexture,
                    getX(), getY(),               // dest x,y
                    getWidth(), getHeight(),      // dest w,h
                    0, 0,                         // src x,y
                    halfW, texH,                  // src w,h
                    false, true                   // flipX, flipY
                );
            } else {
                // Panneau droit - partie droite de la texture
                batch.draw(
                    panelTexture,
                    getX(), getY(),               // dest x,y
                    getWidth(), getHeight(),      // dest w,h
                    halfW, 0,                     // src x,y
                    halfW, texH,                  // src w,h
                    false, true                   // flipX, flipY
                );
            }
        }
    }
        
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
        // Coordonnées relatives au background (pour recalcul lors du resize)
        float relativeTargetX = -1, relativeTargetY = -1; // -1 = non initialisé
        boolean isAnimating;
        float animationTime;
        float animationDuration;
        // ===== VITESSES D'ANIMATION =====
        // Modifiez ces valeurs pour changer les vitesses :
        float forwardAnimationDuration = 0.4f; // Durée pour l'animation aller (vers la grille)
        float returnAnimationDuration = 0.1f; // Durée pour l'animation retour (vers position départ)
        // Plus petit = plus rapide, Plus grand = plus lent
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
            // Token initialisé
            this.gridSlot = -1;
        }
        
        void startAnimationTo(float targetX, float targetY, boolean toGrid) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.isAnimating = true;
            this.animationTime = 0f;
            this.animationDuration = forwardAnimationDuration; // Utiliser la durée pour l'aller
            // Animation vers la grille ou retour au départ
        }
        
        void startAnimationTo(float targetX, float targetY, boolean toGrid, Runnable onComplete) {
            startAnimationTo(targetX, targetY, toGrid);
            this.onAnimationComplete = onComplete;
        }
        
        
        void startReturnAnimationTo(float targetX, float targetY, boolean toGrid, Runnable onComplete) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.isAnimating = true;
            this.animationTime = 0f;
            this.animationDuration = returnAnimationDuration; // Utiliser la durée pour le retour
            this.useStraightLine = true; // Utiliser ligne droite pour le retour
            // Animation vers la grille ou retour
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
        
    // Variables pour la transition vers l'image finale
    private boolean isTransitioningToPicture;
        
    // Variables pour l'animation de porte coulissante
    private boolean useSlidingDoorAnimation = true; // Activer/désactiver l'animation spéciale
    private float slidingDoorTimer;
    private static final float SLIDING_DOOR_WAIT = 2.5f; // Attente avant l'ouverture (2s + 0.5s)
    private static final float SLIDING_DOOR_OPEN_DURATION = 1.0f; // Durée d'ouverture des panneaux
    private float leftPanelOffset = 0f; // Décalage du panneau gauche (négatif = vers la gauche)
    private float rightPanelOffset = 0f; // Décalage du panneau droit (positif = vers la droite)

    
    // Variables pour capturer l'état du plateau de jeu
    private Texture gameStateTexture = null; // Texture contenant le rendu du plateau au moment de la victoire
    private boolean captureNextFramePending = false; // Indique qu'il faut capturer la prochaine frame
    
    // Stage pour gérer les animations avec Actions
    private Stage transitionStage;
    private Actor leftPanelActor;
    private Actor rightPanelActor;
    private boolean isUsingActionBasedTransition = false;
    private float imageAlpha = 0f; // Alpha pour le fade-in de l'image finale
    
    // Constants
    private static final String WIN_SOUND_PATH = "audio/win.mp3";
    private static final String WRONG_SOUND_PATH = "audio/wrong.wav";
    private static final String FAILED_SOUND_PATH = "audio/failed.wav";
    
    private boolean gameElementsDisabled = false;
    
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
        this.correctColor = new Color(0.7f, 0.9f, 0.7f, 1);
        // incorrectColor supprimé (plus utilisé)
        
        // Stocker le paramètre symbolImages pour plus tard (après l'initialisation des symboles)
        String symbolImagesParam = null;
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("numberOfSymbols")) {
                this.numberOfSymbols = ((Number) parameters.get("numberOfSymbols")).intValue();
                // S'assurer que le nombre de symboles est entre 4 et 6
                this.numberOfSymbols = Math.max(4, Math.min(6, this.numberOfSymbols));
                Gdx.app.log("MastermindGameScreen", "Nombre de symboles défini à : " + this.numberOfSymbols);
            }
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String) parameters.get("bgColor");
                this.backgroundColor = parseColor(bgColor);
            }
            if (parameters.containsKey("symbolImages")) {
                symbolImagesParam = (String) parameters.get("symbolImages");
            }
        }
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        Gdx.app.log("MastermindGameScreen", "Mode test Mastermind: " + isTestMode);
        
        // Initialiser les éléments UI avec Distance Field
        CarlitoFontManager.initialize();
        this.font = CarlitoFontManager.getFont();
        this.font.getData().setScale(1.2f);
        this.bigFont = CarlitoFontManager.getFont();
        this.bigFont.getData().setScale(1.8f);
        this.infoFont = CarlitoFontManager.getFont();
        this.infoFont.getData().setScale(1.0f);
        this.layout = new GlyphLayout();
        
        this.backButton = new Rectangle(20, 20, 100, 50);
        // submitButton supprimé (plus de bouton de validation)
        this.infoButton = new Rectangle(DisplayConfig.WORLD_WIDTH - 60, viewport.getWorldHeight() - 60, 40, 40);
        
        // Créer texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        
        
        // Charger la texture du bouton info
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }
        
        // Charger la texture du bouton close
        try {
            this.closeButtonTexture = new Texture(Gdx.files.internal("images/ui/close.png"));
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement du bouton close: " + e.getMessage());
            this.closeButtonTexture = null;
        }

        // Charger la texture du fond d'écran
        try {
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/mmd/background/background.png"));
            this.backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement du fond d'écran: " + e.getMessage());
            this.backgroundTexture = null;
        }
        
        // Calculer les dimensions du background et positionner les boutons
        calculateBackgroundDimensions();
        updateInfoButtonPosition();
        
        // Initialiser les variables de jeu
        this.secretCode = new Array<>();
        this.attempts = new Array<>();
        this.results = new Array<>();
        this.currentGuess = new Array<>();
        this.gameWon = false;
        this.gameFinished = false;
        this.finalPhase = false;
        this.showWinImage = false;
        
        // Initialiser les variables de fade out et redémarrage
        this.isFadingOut = false;
        this.fadeOutTimer = 0;
        this.isClosingBoxes = false;
        this.closeBoxesTimer = 0;
        
        // Initialiser les variables de transition vers la question finale
        this.isTransitioningToPicture = false;
        
        // Initialiser les variables de l'animation de porte coulissante
        this.slidingDoorTimer = 0;
        this.leftPanelOffset = 0f;
        this.rightPanelOffset = 0f;
        // slidingDoorFadeAlpha supprimé (plus de fade nécessaire)
        
        // Variables de capture d'état du plateau initialisées
        this.captureNextFramePending = false;
        if (this.gameStateTexture != null) {
            this.gameStateTexture.dispose();
            this.gameStateTexture = null;
        }
        
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
        this.symbolTextures = null;
        this.isTransitioning = false;
        this.transitionTimer = 0;
        
        // Initialiser les variables d'image
        this.visibleSquares = new HashSet<>();
        this.imageSquares = null;
        
        // Initialiser les noms des symboles
        Gdx.app.log("MastermindGameScreen", "Initialisation des symboles...");
        initializeSymbols();
        Gdx.app.log("MastermindGameScreen", "Symboles initialisés");
        
        // Charger les images de symboles selon les paramètres
        Gdx.app.log("MastermindGameScreen", "Chargement des textures de symboles...");
        if (symbolImagesParam != null && !symbolImagesParam.trim().isEmpty()) {
            Gdx.app.log("MastermindGameScreen", "Chargement des symboles depuis les paramètres...");
            loadSymbolImages(symbolImagesParam);
        } else {
            Gdx.app.log("MastermindGameScreen", "Aucun paramètre symbolImages, chargement par défaut...");
            loadDefaultSymbolImages();
        }
        
        // Charger les sons
        Gdx.app.log("MastermindGameScreen", "Chargement des sons...");
        loadSounds();
        Gdx.app.log("MastermindGameScreen", "Sons chargés");
        
        // Créer l'input processor pour les clics
        Gdx.app.log("MastermindGameScreen", "Création de l'input processor...");
        createInputProcessor();
        Gdx.app.log("MastermindGameScreen", "Input processor créé");
        
        // Charger la texture de la case
        try {
            this.boxTexture = new Texture(Gdx.files.internal("images/games/mmd/box/box-close.png"));
            this.boxTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de la texture box-close: " + e.getMessage());
            this.boxTexture = null;
        }
        
        // Charger les positions des cases depuis le JSON
        loadGridPositions();
        
        // Charger les frames d'animation partagées
        BoxAnimation.loadSharedFrames(DEFAULT_ANIMATION_VARIANT);
        
        // Charger les textures des tokens
        loadTokenTextures();
        
        // Initialiser le Stage pour les transitions
        initializeTransitionStage();
        
        Gdx.app.log("MastermindGameScreen", "Constructeur MastermindGameScreen terminé avec succès");
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
            Gdx.app.error("MastermindGameScreen", "Format de couleur invalide: " + colorStr);
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
            Gdx.app.log("MastermindGameScreen", "Chargement des symboles par défaut pour " + numberOfSymbols + " symboles");
            // Utiliser des symboles génériques - on va créer des textures de couleur unie
            createDefaultSymbolTextures();
            Gdx.app.log("MastermindGameScreen", "Chargement des symboles par défaut terminé");
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "ERREUR CRITIQUE dans loadDefaultSymbolImages: " + e.getMessage());
            // Stack trace logged automatically
        }
    }
    
    private void loadSymbolImages(String symbolImages) {
        try {
            Gdx.app.log("MastermindGameScreen", "Début du chargement des symboles: " + symbolImages);
            
            // Vérifier que symbolNames est initialisé
            if (symbolNames == null) {
                Gdx.app.error("MastermindGameScreen", "ERREUR: symbolNames n'est pas initialisé, initialisation en cours...");
                symbolNames = new String[numberOfSymbols];
                for (int i = 0; i < numberOfSymbols; i++) {
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
            
            String[] imageNames = symbolImages.split(",");
            symbolTextures = new Texture[numberOfSymbols];
            
            Gdx.app.log("MastermindGameScreen", "Nombre de symboles à charger: " + numberOfSymbols + ", Images disponibles: " + imageNames.length);
            
            for (int i = 0; i < numberOfSymbols; i++) {
                try {
                    if (i < imageNames.length) {
                        String imageName = imageNames[i].trim();
                        if (!imageName.isEmpty()) {
                            String imagePath = "images/games/mmd/symbol/" + imageName;
                            Gdx.app.log("MastermindGameScreen", "Chargement symbole " + i + ": " + imagePath);
                            
                            // Vérifier que le fichier existe
                            if (Gdx.files.internal(imagePath).exists()) {
                                Gdx.app.log("MastermindGameScreen", "Fichier trouvé, chargement en cours...");
                                symbolTextures[i] = new Texture(Gdx.files.internal(imagePath));
                                Gdx.app.log("MastermindGameScreen", "✓ Texture chargée pour symbole " + i + ": " + imagePath);
                            } else {
                                Gdx.app.error("MastermindGameScreen", "✗ Fichier non trouvé: " + imagePath);
                                createErrorTexture(i);
                            }
                        } else {
                            Gdx.app.log("MastermindGameScreen", "Nom d'image vide pour symbole " + i + ", création texture par défaut");
                            createErrorTexture(i);
                        }
                    } else {
                        Gdx.app.log("MastermindGameScreen", "Pas assez d'images (" + imageNames.length + " disponibles, " + numberOfSymbols + " requis), création texture par défaut pour symbole " + i);
                        createErrorTexture(i);
                    }
                } catch (Exception e) {
                    Gdx.app.error("MastermindGameScreen", "ERREUR lors du chargement du symbole " + i + ": " + e.getMessage());
                    // Stack trace logged automatically
                    createErrorTexture(i);
                }
            }
            
            Gdx.app.log("MastermindGameScreen", "Chargement des textures terminé, mise à jour des noms...");
            
            // Mettre à jour les noms des symboles basés sur les fichiers d'images
            for (int i = 0; i < numberOfSymbols; i++) {
                try {
                    Gdx.app.log("MastermindGameScreen", "Mise à jour du nom pour symbole " + i);
                    if (i < imageNames.length) {
                        String imageName = imageNames[i].trim();
                        Gdx.app.log("MastermindGameScreen", "Nom d'image pour symbole " + i + ": '" + imageName + "'");
                        if (!imageName.isEmpty() && imageName.contains("_")) {
                            String[] parts = imageName.split("_");
                            Gdx.app.log("MastermindGameScreen", "Parties du nom: " + java.util.Arrays.toString(parts));
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
                    Gdx.app.log("MastermindGameScreen", "✓ Nom du symbole " + i + ": " + symbolNames[i]);
                } catch (Exception e) {
                    Gdx.app.error("MastermindGameScreen", "✗ Erreur lors de la mise à jour du nom du symbole " + i + ": " + e.getMessage());
                    // Stack trace logged automatically
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
            
            Gdx.app.log("MastermindGameScreen", "Chargement des symboles terminé avec succès");
            
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "ERREUR CRITIQUE dans loadSymbolImages: " + e.getMessage());
            // Stack trace logged automatically
            // Créer des textures d'erreur seulement pour les symboles qui n'ont pas pu être chargés
            if (symbolTextures == null) {
                symbolTextures = new Texture[numberOfSymbols];
            }
            for (int i = 0; i < numberOfSymbols; i++) {
                if (symbolTextures[i] == null) {
                    Gdx.app.log("MastermindGameScreen", "Création de texture d'erreur pour symbole manquant " + i);
                    createErrorTexture(i);
                }
                if (symbolNames[i] == null) {
                    symbolNames[i] = "Symbole " + (i + 1);
                }
            }
        }
    }
    
    /**
     * Crée des textures par défaut avec des couleurs différentes
     */
    private void createDefaultSymbolTextures() {
        symbolTextures = new Texture[numberOfSymbols];
        
        // Couleurs par défaut pour les symboles
        Color[] defaultColors = {
            Color.RED,
            Color.GREEN, 
            Color.BLUE,
            Color.YELLOW,
            Color.MAGENTA,
            Color.CYAN,
            Color.ORANGE,
            Color.PINK
        };
        
        for (int i = 0; i < numberOfSymbols; i++) {
            try {
                // Créer une texture colorée de 64x64 pixels
                Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
                Color color = defaultColors[i % defaultColors.length];
                pixmap.setColor(color);
                pixmap.fill();
                
                // Ajouter une bordure noire pour mieux distinguer les symboles
                pixmap.setColor(Color.BLACK);
                pixmap.drawRectangle(0, 0, 64, 64);
                pixmap.drawRectangle(1, 1, 62, 62);
                
                symbolTextures[i] = new Texture(pixmap);
                symbolTextures[i].setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                pixmap.dispose();
                
                // Mettre à jour le nom du symbole
                symbolNames[i] = "Symbole " + (i + 1) + " (" + color.toString().substring(0, 6) + ")";
                
                Gdx.app.log("MastermindGameScreen", "✓ Texture par défaut créée pour symbole " + i + ": " + symbolNames[i]);
                
            } catch (Exception e) {
                Gdx.app.error("MastermindGameScreen", "✗ Erreur lors de la création de la texture par défaut pour symbole " + i + ": " + e.getMessage());
                // Stack trace logged automatically
                createErrorTexture(i);
            }
        }
    }
    
    private void createErrorTexture(int symbolIndex) {
        try {
            Gdx.app.log("MastermindGameScreen", "ATTENTION: Création d'une texture d'erreur pour symbole " + symbolIndex);
            // Créer une texture rouge d'erreur
            Pixmap errorPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
            errorPixmap.setColor(Color.RED);
            errorPixmap.fill();
            symbolTextures[symbolIndex] = new Texture(errorPixmap);
            errorPixmap.dispose();
            Gdx.app.log("MastermindGameScreen", "✓ Texture d'erreur créée pour symbole " + symbolIndex);
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "✗ Impossible de créer la texture d'erreur pour symbole " + symbolIndex + ": " + e.getMessage());
            // Stack trace logged automatically
            symbolTextures[symbolIndex] = null;
        }
    }
    
    private void loadSounds() {
        try {
            winSound = Gdx.audio.newSound(Gdx.files.internal(WIN_SOUND_PATH));
            wrongSound = Gdx.audio.newSound(Gdx.files.internal(WRONG_SOUND_PATH));
            failedSound = Gdx.audio.newSound(Gdx.files.internal(FAILED_SOUND_PATH));
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement des sons: " + e.getMessage());
        }
    }
    
    private void createInputProcessor() {
        Gdx.app.log("MastermindGameScreen", "Début de création de l'InputAdapter...");
        inputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
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
                if (keycode == Input.Keys.N && isTestMode && !gameFinished && !gameWon && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    triggerFinalPhase();
                    return true;
                }
                
                // Plus de gestion de saisie de texte
                return false;
            }
            
        };
        Gdx.app.log("MastermindGameScreen", "InputAdapter créé avec succès");
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
     * Calcule les dimensions et l'échelle du background avec crop pour garder l'aspect ratio
     * Logique identique à SlidingPuzzle et Crystalize pour un centrage cohérent
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
     * Calcule la position des boutons d'information et close en haut à droite du viewport
     */
    private void updateInfoButtonPosition() {
        // Obtenir les dimensions du viewport
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        // Taille de base des boutons
        float baseButtonSize = 100f; // Taille un peu plus grande pour une meilleure visibilité
        
        // Marge depuis les bords du viewport
        float marginFromEdge = 30f;
        float spacingBetweenButtons = 30f;
        
        // Position du bouton info (en haut à droite)
        float closeButtonX = viewportWidth - marginFromEdge - baseButtonSize;
        float closeButtonY = viewportHeight - marginFromEdge - baseButtonSize;
        
        // Position du bouton close (juste en dessous du bouton info)
        float infoButtonX = closeButtonX;
        float infoButtonY = closeButtonY - baseButtonSize - spacingBetweenButtons;
        
        // Positionner les boutons
        infoButton.setSize(baseButtonSize, baseButtonSize);
        infoButton.setPosition(infoButtonX, infoButtonY);
        
        backButton.setSize(baseButtonSize, baseButtonSize);
        backButton.setPosition(closeButtonX, closeButtonY);
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
            token.isVisible = true; // S'assurer que le token est visible
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
            StringBuilder secretCodeStr = new StringBuilder("Code secret (tokens): ");
            for (int token : secretCode) {
                secretCodeStr.append(token).append(" ");
            }
            Gdx.app.log("MastermindGameScreen", secretCodeStr.toString());
            Gdx.app.log("MastermindGameScreen", "Nombre total de symboles disponibles: " + numberOfSymbols);
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
                    Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de la texture du thème: " + e.getMessage());
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
        if (gameFinished) return;
        
        // Vérifier si la colonne courante est en cours d'animation
        if (gridPositions != null && attempts.size < gridPositions.size) {
            AnimatedColumn currentColumn = gridPositions.get(attempts.size);
            if (currentColumn.isAnimating && !currentColumn.animationComplete) {
                // Empêcher les clics sur les tokens de départ pendant l'animation d'ouverture
                return;
            }
        }
        
        // Calculer les dimensions du fond d'écran pour les coordonnées
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        if (backgroundTexture == null) return;
        
        float bgRatio = (float)backgroundTexture.getWidth() / backgroundTexture.getHeight();
        float screenRatio = screenWidth / screenHeight;
        
        float bgWidth, bgHeight;
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
        
        float scaleX = bgWidth / backgroundTexture.getWidth();
        
        // 1. Vérifier les clics sur les tokens en position de départ
        for (AnimatedToken token : startPositionTokens) {
            float tokenSize = 50 * scaleX; // Taille approximative du token
            if (x >= token.currentX - tokenSize/2 && x <= token.currentX + tokenSize/2 &&
                y >= token.currentY - tokenSize/2 && y <= token.currentY + tokenSize/2) {
                
                // Vérifier si le token est encore visible (pas encore déplacé)
                if (token.isVisible) {
                    // Trouver le premier slot libre
                    int freeSlot = findNextFreeSlot();
                    if (freeSlot != -1) {
                        createMovingToken(token.tokenType, freeSlot);
                    }
                }
                // Si le token n'est pas visible (déjà déplacé), ne rien faire
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
                
                // Stocker les coordonnées relatives au background (pour recalcul lors du resize)
                float relativeX = rect.x + rect.width/2;
                float relativeY = rect.y + rect.height/2;
                
                // Créer le token en mouvement
                AnimatedToken movingToken = new AnimatedToken(tokenType, startX, startY);
                movingToken.gridSlot = slot;
                movingToken.relativeTargetX = relativeX;
                movingToken.relativeTargetY = relativeY;
                
                // Marquer le slot comme réservé
                placedTokens.set(slot, tokenType);
                
                // Démarrer l'animation avec callback
                movingToken.startAnimationTo(targetX, targetY, true, () -> {
                    // À la fin de l'animation, créer un token statique sur la grille
                    AnimatedToken gridToken = new AnimatedToken(tokenType, targetX, targetY);
                    gridToken.gridSlot = slot;
                    gridToken.relativeTargetX = relativeX;
                    gridToken.relativeTargetY = relativeY;
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
            
            // Marquer qu'il faut capturer la prochaine frame avant de démarrer la transition
            captureNextFramePending = true;
            // Ne pas démarrer la transition maintenant, elle sera démarrée après la capture
            
        // Réinitialiser les variables de l'animation de porte coulissante
        slidingDoorTimer = 0;
        leftPanelOffset = 0f;
        rightPanelOffset = 0f;
            
            // Calculer le score final et enregistrer immédiatement
            int finalScore = calculateScore();
            if (game instanceof AdventCalendarGame) {
                AdventCalendarGame adventGame = (AdventCalendarGame) game;
                adventGame.setScore(dayId, finalScore);
                adventGame.setVisited(dayId, true);
            }
            
            if (winSound != null) {
                winSound.play();
            }
            
        } else if (attempts.size >= maxAttempts) {
            // Jeu perdu - démarrer le fade out des tokens
            isFadingOut = true;
            fadeOutTimer = 0;
            
            // Jouer le son d'échec pendant le fade out
            if (failedSound != null) {
                failedSound.play();
            }
            
        } else {
            // Démarrer l'animation de la prochaine colonne seulement si le jeu n'est pas gagné
            if (attempts.size < gridPositions.size) {
                startColumnAnimation(attempts.size);
                // Réinitialiser les tokens pour la prochaine tentative
                resetTokensForNextAttempt();
            }
            
            // Jouer le son "wrong" pour une tentative incorrecte
            if (wrongSound != null) {
                wrongSound.play();
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
    
    
    private void solveGame() {
        Gdx.app.log("MastermindGameScreen", "Résolution automatique du jeu Mastermind (mode test)");
        
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
    
    private void triggerFinalPhase() {
        Gdx.app.log("MastermindGameScreen", "Déclenchement de la phase finale du Mastermind (mode test - touche N)");
        
        // Simuler une victoire comme si la combinaison venait d'être trouvée
        gameWon = true;
        
        // Marquer qu'il faut capturer la prochaine frame avant de démarrer la transition
        captureNextFramePending = true;
        // Ne pas démarrer la transition maintenant, elle sera démarrée après la capture
        
        // Réinitialiser les variables de l'animation de porte coulissante
        slidingDoorTimer = 0;
        leftPanelOffset = 0f;
        rightPanelOffset = 0f;
        
        // Calculer le score final et enregistrer immédiatement
        int finalScore = calculateScore();
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, finalScore);
            adventGame.setVisited(dayId, true);
        }
        
        if (winSound != null) {
            winSound.play();
        }
    }
    
    private void clearAllTokens() {
        Gdx.app.log("MastermindGameScreen", "Suppression de tous les tokens après fade out");
        
        // Supprimer tous les tokens de la grille (tentatives précédentes)
        this.attempts.clear();
        this.results.clear();
        this.currentGuess.clear();
        
        // Nettoyer tous les tokens
        this.startPositionTokens.clear();
        this.movingTokens.clear();
        this.gridTokens.clear();
        this.placedTokens.clear();
        
        // Réinitialiser les colonnes pour supprimer les tokens affichés
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                column.reset();
            }
        }
    }
    
    private void closeAllBoxes() {
        Gdx.app.log("MastermindGameScreen", "Fermeture de toutes les cases du plateau");
        
        // Démarrer l'animation de fermeture pour toutes les cases
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                for (BoxAnimation anim : column.animations) {
                    // Démarrer l'animation de fermeture
                    anim.isPlaying = true;
                    anim.isClosing = true; // Nouveau flag pour indiquer la fermeture
                    anim.currentFrame = BoxAnimation.sharedFrames.size - 1; // Commencer par la dernière frame
                    anim.timer = 0;
                }
            }
        }
    }
    
    private void restartGame() {
        Gdx.app.log("MastermindGameScreen", "Redémarrage du jeu Mastermind avec un nouveau code");
        
        // Réinitialiser toutes les variables de jeu
        this.secretCode = new Array<>();
        this.attempts = new Array<>();
        this.results = new Array<>();
        this.currentGuess = new Array<>();
        this.gameWon = false;
        this.gameFinished = false;
        this.finalPhase = false;
        this.showWinImage = false;
        
        // Réinitialiser les variables de fade out et redémarrage
        this.isFadingOut = false;
        this.fadeOutTimer = 0;
        this.isClosingBoxes = false;
        this.closeBoxesTimer = 0;
        
        // Réinitialiser les variables de transition vers la question finale
        this.isTransitioningToPicture = false;
        
        // Réinitialiser les variables de l'animation de porte coulissante
        this.slidingDoorTimer = 0;
        this.leftPanelOffset = 0f;
        this.rightPanelOffset = 0f;
        
        // Réinitialiser les variables de capture d'état du plateau
        this.captureNextFramePending = false;
        if (this.gameStateTexture != null) {
            this.gameStateTexture.dispose();
            this.gameStateTexture = null;
        }
        
        // Nettoyer tous les tokens
        this.startPositionTokens.clear();
        this.movingTokens.clear();
        this.gridTokens.clear();
        this.placedTokens.clear();
        
        // Générer un nouveau code secret
        generateSecretCode();
        
        // Réinitialiser les tokens de départ
        initializeStartPositionTokens();
        
        // Réinitialiser les coordonnées de position
        this.positionCentersX.clear();
        this.positionCentersY.clear();
        
        // Réinitialiser les slots placés
        for (int i = 0; i < TOKENS_IN_COMBINATION; i++) {
            placedTokens.add(-1); // -1 signifie slot vide
        }
        
        // Réinitialiser les colonnes
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                column.reset();
            }
        }
        
        // Démarrer l'ouverture de la première colonne
        if (gridPositions != null && gridPositions.size > 0) {
            startColumnAnimation(0);
        }
        
        Gdx.app.log("MastermindGameScreen", "Nouveau code généré: " + secretCode);
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le Stage de transition si actif
        if (isUsingActionBasedTransition && transitionStage != null) {
            transitionStage.act(delta);
            
            // Calculer l'alpha pour le fade-in de l'image finale
            // L'image fait un fade-in pendant la première moitié de l'ouverture des panneaux
            if (leftPanelActor != null) {
                // Calculer le progrès de l'animation des panneaux (0.0 = début, 1.0 = fin)
                float screenWidth = DisplayConfig.WORLD_WIDTH;
                float halfWidth = screenWidth * 0.5f;
                float currentOffset = Math.abs(leftPanelActor.getX()); // Distance parcourue par le panneau gauche
                float animationProgress = Math.min(1.0f, currentOffset / halfWidth);
                
                // L'image fait un fade-in pendant la première moitié (0.0 à 0.5 du progrès total)
                float fadeProgress = Math.min(1.0f, animationProgress * 2.0f); // *2 pour que ça se termine à 50%
                imageAlpha = fadeProgress;
            }
        }
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
        
        // Mettre à jour le fade out des tokens en cas d'échec
        if (isFadingOut) {
            fadeOutTimer += delta;
            
            if (fadeOutTimer >= fadeOutDuration) {
                // Fade out terminé, supprimer tous les tokens et démarrer la fermeture des cases
                clearAllTokens();
                isFadingOut = false;
                isClosingBoxes = true;
                closeBoxesTimer = 0;
            }
        }
        
        // Mettre à jour la fermeture des cases
        if (isClosingBoxes) {
            if (closeBoxesTimer == 0) {
                // Première frame de la fermeture, fermer toutes les cases
                closeAllBoxes();
            }
            
            closeBoxesTimer += delta;
            
            // Calculer le temps total pour la fermeture + attente
            float totalCloseAndWaitTime = closeBoxesDuration + restartDelay;
            
            if (closeBoxesTimer >= totalCloseAndWaitTime) {
                // Fermeture et attente terminées, arrêter toutes les animations de fermeture
                if (gridPositions != null) {
                    for (AnimatedColumn column : gridPositions) {
                        for (BoxAnimation anim : column.animations) {
                            if (anim.isClosing) {
                                anim.isPlaying = false;
                                anim.isClosing = false;
                                anim.currentFrame = 0; // S'assurer qu'on reste sur la frame fermée
                            }
                        }
                    }
                }
                
                // Redémarrer directement
                isClosingBoxes = false;
                restartGame();
            }
        }
        
        // Code supprimé : clignotement du curseur (plus de phase de question finale)
        
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
        if (isTransitioningToPicture) {
            if (useSlidingDoorAnimation) {
                // Animation de porte coulissante
                slidingDoorTimer += delta;
                
                if (slidingDoorTimer <= SLIDING_DOOR_WAIT) {
                    // Phase 1: Attente
                    leftPanelOffset = 0f;
                    rightPanelOffset = 0f;
                    // Plus de fade nécessaire
                    
                    // Désactiver l'affichage des éléments de jeu dès le début de la phase d'attente
                    // pour éviter la surimpression avec les panneaux
                    if (!gameElementsDisabled) {
                        disableGameElements();
                    }
                } else if (slidingDoorTimer <= SLIDING_DOOR_WAIT + SLIDING_DOOR_OPEN_DURATION) {
                    // Phase 2: Ouverture des panneaux
                    float openProgress = (slidingDoorTimer - SLIDING_DOOR_WAIT) / SLIDING_DOOR_OPEN_DURATION;
                    float screenWidth = DisplayConfig.WORLD_WIDTH;
                    
                    // Les panneaux se déplacent vers l'extérieur
                    leftPanelOffset = -screenWidth * 0.5f * openProgress; // Panneau gauche vers la gauche
                    rightPanelOffset = screenWidth * 0.5f * openProgress; // Panneau droit vers la droite
                } else {
                    // Animation terminée, passer directement à la phase finale avec l'image complète
                    isTransitioningToPicture = false;
                    finalPhase = true;
                    gameFinished = true;
                    showWinImage = true;
                    slidingDoorTimer = 0;
                }
            }
        }
        
        // Mettre à jour les animations
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
                // Mettre à jour le fade des tokens
                column.updateTokensFade(delta);
                
                // Mettre à jour les animations si la colonne est en animation OU si on est en phase de fermeture
                if (column.isAnimating || isClosingBoxes) {
                    boolean allFinished = true;
                    
                    for (int i = 0; i < column.animations.size; i++) {
                        BoxAnimation anim = column.animations.get(i);
                        
                        // Mettre à jour le timer
                        anim.timer += delta;
                        
                        // Vérifier si l'animation doit commencer (après le délai)
                        if (anim.timer >= anim.delay && !anim.isPlaying && !anim.isClosing) {
                            anim.isPlaying = true;
                            anim.timer = 0;
                        }
                        
                        // Si l'animation est en cours
                        if (anim.isPlaying) {
                            // Système basé sur le temps au lieu de frame par frame
                            float animationDuration = anim.isClosing ? BOX_CLOSING_DURATION : BOX_OPENING_DURATION;
                            float progress = Math.min(1.0f, anim.timer / animationDuration);
                            
                            if (anim.isClosing) {
                                // Animation de fermeture : aller de la dernière vers la première frame
                                int totalFrames = BoxAnimation.sharedFrames.size;
                                anim.currentFrame = (int)((1.0f - progress) * (totalFrames - 1));
                                anim.currentFrame = Math.max(0, Math.min(anim.currentFrame, totalFrames - 1));
                                
                                if (progress >= 1.0f) {
                                    anim.currentFrame = 0; // Frame fermée
                                } else {
                                    allFinished = false;
                                }
                            } else {
                                // Animation d'ouverture : aller de la première vers la dernière frame
                                int totalFrames = BoxAnimation.sharedFrames.size;
                                anim.currentFrame = (int)(progress * (totalFrames - 1));
                                anim.currentFrame = Math.max(0, Math.min(anim.currentFrame, totalFrames - 1));
                                
                                if (progress >= 1.0f) {
                                    anim.currentFrame = totalFrames - 1; // Frame ouverte
                                } else {
                                    allFinished = false;
                                }
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
        // Exception : continuer à dessiner si on doit capturer le framebuffer
        // MAIS ne pas dessiner pendant la transition (les panneaux se chargent de l'affichage)
        // ET ne pas dessiner si les panneaux sont déjà en cours d'affichage (après la phase d'attente)
        boolean shouldDrawGameElements = (!gameElementsDisabled || captureNextFramePending) && 
                                       (!isTransitioningToPicture || 
                                        (useSlidingDoorAnimation && slidingDoorTimer <= SLIDING_DOOR_WAIT));
        
        if (shouldDrawGameElements) {
            // Dessiner le fond d'écran
            if (backgroundTexture != null) {
                batch.setColor(1, 1, 1, 1);
                
                // Calculer les dimensions du background une seule fois
                calculateBackgroundDimensions();
                
                // Dessiner d'abord un fond uni
                batch.setColor(backgroundColor);
                batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
                
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
            }
        }

        // Toujours dessiner l'interface appropriée
        if (isUsingActionBasedTransition) {
            // Dessiner d'abord l'image finale en arrière-plan avec le batch principal
            batch.setColor(1, 1, 1, 1);
            drawFinalPhase();
            
            // IMPORTANT : Terminer le batch principal avant d'utiliser le Stage
            batch.end();
            
            // Puis dessiner le Stage avec les panneaux par-dessus (le Stage gère son propre batch)
            transitionStage.act(Gdx.graphics.getDeltaTime());
            transitionStage.draw();
            
            // Redémarrer le batch principal pour les overlays
            batch.begin();
        } else if (isTransitioningToPicture && !isUsingActionBasedTransition) {
            drawTransitionToPicture();
        } else if (isTransitioning) {
            drawTransition();
        } else if (finalPhase && showWinImage) {
            drawFinalPhase();
        } else if (shouldDrawGameElements) {
            drawGameInterface();
        }
        
        if (captureNextFramePending) {
            updateInfoButtonPosition();
            drawCloseButton();
            drawInfoButton();
        }
        
        // Capturer le framebuffer si demandé (AVANT les overlays)
        if (captureNextFramePending) {
            try {
                // Capturer la zone correspondant au monde logique du viewport
                // Nous devons capturer les pixels physiques qui correspondent à la zone du monde
                int viewportWidth = (int) viewport.getScreenWidth();
                int viewportHeight = (int) viewport.getScreenHeight();
                
                // Calculer l'offset pour centrer la capture dans la zone visible
                int screenWidth = Gdx.graphics.getBackBufferWidth();
                int screenHeight = Gdx.graphics.getBackBufferHeight();
                int offsetX = (screenWidth - viewportWidth) / 2;
                int offsetY = (screenHeight - viewportHeight) / 2;
                
                Pixmap pm = ScreenUtils.getFrameBufferPixmap(offsetX, offsetY, viewportWidth, viewportHeight);
                
                // Libérer l'ancienne texture
                if (gameStateTexture != null) {
                    gameStateTexture.dispose();
                }
                
                // Créer la nouvelle texture
                gameStateTexture = new Texture(pm);
                pm.dispose();
                
                // Démarrer la transition avec Actions
                captureNextFramePending = false;
                
                // Démarrer la transition basée sur les Actions
                startActionBasedTransition();
                
                Gdx.app.log("MastermindGameScreen", "État du plateau de jeu capturé avec succès");
                Gdx.app.log("MastermindGameScreen", "  Texture: " + gameStateTexture.getWidth() + "x" + gameStateTexture.getHeight());
                Gdx.app.log("MastermindGameScreen", "  Viewport screen: " + viewport.getScreenWidth() + "x" + viewport.getScreenHeight());
                Gdx.app.log("MastermindGameScreen", "  Viewport world: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
                
            } catch (Exception e) {
                Gdx.app.error("MastermindGameScreen", "Erreur lors de la capture de l'état du plateau: " + e.getMessage());
                // Stack trace logged automatically
                // En cas d'erreur, utiliser l'animation classique
                captureNextFramePending = false;
                isTransitioningToPicture = true;
            }
        }
        
        // Les boutons sont maintenant inclus dans drawGameInterface() et capturés avec le reste
        
        // Dessiner le panneau d'info par-dessus tout le reste s'il est visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
    }
    
    private void drawGameInterface() {
        
        // Dessiner les résultats des tentatives précédentes
        drawAttemptResults();
        
        // Dessiner les boutons info et close (maintenant inclus dans la capture)
        // S'assurer que les positions des boutons sont à jour
        updateInfoButtonPosition();
        drawCloseButton();
        drawInfoButton();
    }
    
    private void drawAttemptResults() {
        if (gridPositions == null || results.size == 0) return;
        
        // Calculer l'alpha pour le fade out des indicateurs
        float alpha = 1.0f;
        if (isFadingOut) {
            alpha = Math.max(0.0f, 1.0f - (fadeOutTimer / fadeOutDuration));
        }
        
        batch.setColor(1, 1, 1, alpha);
        
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
        
        // Remettre la couleur normale
        batch.setColor(1, 1, 1, 1);
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
    
    private void drawGameEndMessage() {

    }
    
    private void drawTransition() {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        if (transitionTimer <= FADE_TO_BLACK_DURATION) {
            // Phase 1: Fondu vers le noir
            float fadeProgress = transitionTimer / FADE_TO_BLACK_DURATION;
            
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
        CarlitoFontManager.drawText(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 50);
    }
    
    private void drawFinalPhase() {
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer l'espace disponible pour l'image (utiliser tout l'écran)
        float availableHeight = screenHeight - 20; // Petite marge
        
        // Afficher l'image complète en plein écran
        if (fullImageTexture != null && availableHeight > 100) {
            drawCompleteImage(availableHeight);
        }
    }
    
    private void drawCompleteImage(float availableHeight) {
        if (fullImageTexture == null) {
            return;
        }
        
        // Obtenir les dimensions originales de l'image
        float originalWidth = fullImageTexture.getWidth();
        float originalHeight = fullImageTexture.getHeight();
        float aspectRatio = originalWidth / originalHeight;
        
        // Calculer les dimensions adaptatives pour utiliser tout l'écran
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        float imageWidth, imageHeight;
        
        // Calculer les dimensions pour remplir l'écran tout en conservant le ratio
        if (screenWidth / aspectRatio <= screenHeight) {
            // L'image sera limitée par la largeur
            imageWidth = screenWidth;
            imageHeight = imageWidth / aspectRatio;
        } else {
            // L'image sera limitée par la hauteur
            imageHeight = screenHeight;
            imageWidth = imageHeight * aspectRatio;
        }
        
        // Dessiner d'abord un fond noir pour toute la zone
        batch.setColor(0f, 0f, 0f, 1f);
        batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
        
        // Centrer l'image
        float imageX = (screenWidth - imageWidth) / 2;
        float imageY = (screenHeight - imageHeight) / 2;
        
        // Dessiner l'image complète par-dessus le fond noir avec fade-in
        // Utiliser imageAlpha pour l'effet de fade-in, ou 1.0 si pas en transition
        float alpha = isUsingActionBasedTransition ? imageAlpha : 1f;
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(fullImageTexture, imageX, imageY, imageWidth, imageHeight);
    }
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
            // Dessiner l'image du bouton info à sa taille naturelle
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            
            // Utiliser la taille du rectangle du bouton (définie dans updateInfoButtonPosition)
            float buttonWidth = infoButton.width;
            float buttonHeight = infoButton.height;
            
            // Dessiner le bouton à sa position
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, buttonWidth, buttonHeight);
        }
    }
    
    private void drawCloseButton() {
        if (closeButtonTexture != null) {
            // Dessiner l'image du bouton close à sa taille naturelle
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            
            // Utiliser la taille du rectangle du bouton (définie dans updateInfoButtonPosition)
            float buttonWidth = backButton.width;
            float buttonHeight = backButton.height;
            
            // Dessiner le bouton à sa position
            batch.draw(closeButtonTexture, backButton.x, backButton.y, buttonWidth, buttonHeight);
        }
    }
    
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
        
        // Adapter la taille de la police selon la taille du panneau
        float scaleFactor = Math.min(panelWidth / 500f, panelHeight / 400f);
        float titleScale = Math.max(1.0f, 1.5f * scaleFactor);
        float textScale = Math.max(0.8f, 1.0f * scaleFactor);
        
        // Titre avec police adaptative
        infoFont.getData().setScale(titleScale);
        infoFont.setColor(0.2f, 0.3f, 0.8f, 1);
        String title = "Règles du Mastermind";
        layout.setText(infoFont, title);
        CarlitoFontManager.drawText(batch, layout, 
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
                    CarlitoFontManager.drawText(batch, layout, panelX + leftMargin, textY);
                    infoFont.getData().setScale(textScale);
                    infoFont.setColor(0.1f, 0.1f, 0.2f, 1);
                } else {
                    layout.setText(infoFont, rule);
                    CarlitoFontManager.drawText(batch, layout, panelX + leftMargin, textY);
                }
            }
            textY -= lineHeight;
        }
        
        // Indicateur de fermeture
        infoFont.getData().setScale(textScale * 0.9f);
        infoFont.setColor(0.5f, 0.5f, 0.6f, 1);
        String closeHint = "Tapez pour fermer";
        layout.setText(infoFont, closeHint);
        CarlitoFontManager.drawText(batch, layout, 
            panelX + panelWidth - layout.width - 10,
            panelY + 15);
        
        // Remettre la police à sa taille normale
        infoFont.getData().setScale(1.0f);
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        
        // Recalculer les dimensions du background si disponible
        if (backgroundTexture != null) {
            calculateBackgroundDimensions();
        }
        
        // Recalculer les positions de tous les tokens (placés et en mouvement)
        // pour qu'ils maintiennent leur position relative au background
        recalculateTokenPositions();
        
        // Les boutons se repositionnent automatiquement basé sur la taille de l'écran
        // Le bouton d'information se positionne selon les coordonnées du background
        updateInfoButtonPosition();
        
        // Adapter la police d'information à la nouvelle taille d'écran
        float screenDiagonal = (float) Math.sqrt(width * width + height * height);
        float baseScale = Math.max(0.8f, Math.min(1.5f, screenDiagonal / 1000f));
        infoFont.getData().setScale(baseScale);
        
        // CORRECTION: Mettre à jour le Stage de transition avec les nouvelles dimensions du viewport
        if (transitionStage != null) {
            transitionStage.getViewport().update(width, height, true);
            
            // Recalculer les tailles et positions des acteurs avec les nouvelles dimensions
            float screenWidth = DisplayConfig.WORLD_WIDTH;
            float screenHeight = viewport.getWorldHeight();
            float halfWidth = screenWidth * 0.5f;
            
            if (leftPanelActor != null) {
                leftPanelActor.setSize(halfWidth, screenHeight);
                // Si l'animation est en cours, conserver la position actuelle
                if (!isUsingActionBasedTransition) {
                    leftPanelActor.setPosition(0, 0);
                }
                Gdx.app.log("MastermindGameScreen", "Resize - Panneau gauche: " + halfWidth + "x" + screenHeight);
            }
            
            if (rightPanelActor != null) {
                rightPanelActor.setSize(halfWidth, screenHeight);
                // Si l'animation est en cours, ajuster la position X de base
                if (!isUsingActionBasedTransition) {
                    rightPanelActor.setPosition(halfWidth, 0);
                } else {
                    // En cours d'animation: ajuster seulement la position X de base
                    float currentOffsetX = rightPanelActor.getX() - halfWidth;
                    rightPanelActor.setPosition(halfWidth + currentOffsetX, 0);
                }
                Gdx.app.log("MastermindGameScreen", "Resize - Panneau droit: " + halfWidth + "x" + screenHeight);
            }
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        bigFont.dispose();
        infoFont.dispose();
        whiteTexture.dispose();
        if (fullImageTexture != null) fullImageTexture.dispose();
        if (infoButtonTexture != null) infoButtonTexture.dispose();
        if (closeButtonTexture != null) closeButtonTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose(); // Dispose the new texture
        if (winSound != null) winSound.dispose();
        if (wrongSound != null) wrongSound.dispose();
        if (failedSound != null) failedSound.dispose();
        
        // Libérer la texture de l'état du plateau de jeu
        if (gameStateTexture != null) {
            gameStateTexture.dispose();
            gameStateTexture = null;
        }
        
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
        
        // Nettoyer le Stage de transition
        if (transitionStage != null) {
            transitionStage.dispose();
        }
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
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement des positions: " + e.getMessage());
            // Stack trace logged automatically
        }
    }

    // Ajouter une méthode pour démarrer l'animation d'une colonne
    private void startColumnAnimation(int columnIndex) {
        if (gridPositions != null && columnIndex >= 0 && columnIndex < gridPositions.size) {
            AnimatedColumn column = gridPositions.get(columnIndex);
            column.startAnimations();
        }
    }

    /**
     * Initialise le Stage pour les transitions avec Actions
     */
    private void initializeTransitionStage() {
        // Créer le Stage avec son propre batch pour éviter les conflits
        transitionStage = new Stage(viewport);
        
        // Créer les acteurs pour les panneaux
        leftPanelActor = new TransitionPanelActor(true);
        rightPanelActor = new TransitionPanelActor(false);
        
        // backgroundFadeActor supprimé - l'image sera dessinée directement avec le batch principal
        
        // Configurer les tailles et positions initiales
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        float halfWidth = screenWidth * 0.5f;
        
        // Panneaux initialement centrés et invisibles
        leftPanelActor.setSize(halfWidth, screenHeight);
        leftPanelActor.setPosition(0, 0);
        leftPanelActor.setVisible(false);
        
        rightPanelActor.setSize(halfWidth, screenHeight);
        rightPanelActor.setPosition(halfWidth, 0);
        rightPanelActor.setVisible(false);
        
        // Ajouter seulement les panneaux au stage (l'image sera dessinée séparément)
        transitionStage.addActor(leftPanelActor);       // Panneau gauche
        transitionStage.addActor(rightPanelActor);      // Panneau droit
    }
    
    /**
     * Démarre la transition avec Actions LibGDX
     */
    private void startActionBasedTransition() {
        if (gameStateTexture == null) {
            Gdx.app.error("MastermindGameScreen", "Impossible de démarrer la transition : texture d'état non capturée");
            return;
        }
        
        isUsingActionBasedTransition = true;
        
        // Désactiver l'ancien système de transition pour éviter les conflits
        isTransitioningToPicture = false;
        
        // Initialiser l'alpha pour le fade-in de l'image
        imageAlpha = 0f;
        
        // Configurer les textures des panneaux
        ((TransitionPanelActor) leftPanelActor).setPanelTexture(gameStateTexture);
        ((TransitionPanelActor) rightPanelActor).setPanelTexture(gameStateTexture);
        
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float halfWidth = screenWidth * 0.5f;
        
        // Rendre les panneaux visibles (l'image sera dessinée séparément)
        leftPanelActor.setVisible(true);
        rightPanelActor.setVisible(true);
        
        // Créer la séquence d'animation
        leftPanelActor.addAction(Actions.sequence(
            Actions.delay(SLIDING_DOOR_WAIT), // Phase d'attente (2.5s)
            Actions.moveBy(-halfWidth, 0, SLIDING_DOOR_OPEN_DURATION), // Glisser vers la gauche
            Actions.run(() -> {
                // Callback à la fin de l'animation des panneaux
                Gdx.app.log("MastermindGameScreen", "Animation des panneaux terminée");
            })
        ));
        
        rightPanelActor.addAction(Actions.sequence(
            Actions.delay(SLIDING_DOOR_WAIT), // Phase d'attente (2.5s)
            Actions.moveBy(halfWidth, 0, SLIDING_DOOR_OPEN_DURATION), // Glisser vers la droite
            Actions.run(() -> {
                // Animation terminée, passer directement à la phase finale avec l'image complète
                isTransitioningToPicture = false;
                finalPhase = true;
                gameFinished = true;
                showWinImage = true;
                isUsingActionBasedTransition = false;
                Gdx.app.log("MastermindGameScreen", "Transition vers image finale terminée");
            })
        ));
        
        // Masquer les éléments du jeu immédiatement
        disableGameElements();
        
        Gdx.app.log("MastermindGameScreen", "Transition avec Actions démarrée");
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
                }
                
                // Charger la texture de position correspondante
                String positionPath = "images/games/mmd/token/" + (i < 10 ? "0" : "") + i + "s.png";
                if (Gdx.files.internal(positionPath).exists()) {
                    Texture positionTexture = new Texture(Gdx.files.internal(positionPath));
                    positionTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    tokenPositionTextures.add(positionTexture);
                }
            } catch (Exception e) {
                Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement du token " + i + ": " + e.getMessage());
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
            Gdx.app.log("MastermindGameScreen", "✓ Texture dot-white chargée");
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de dot-white.png: " + e.getMessage());
            dotWhiteTexture = null;
        }
        
        try {
            // Charger dot-black.png (token correct mais mal placé)
            dotBlackTexture = new Texture(Gdx.files.internal("images/games/mmd/indicator/dot-black.png"));
            dotBlackTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.log("MastermindGameScreen", "✓ Texture dot-black chargée");
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de dot-black.png: " + e.getMessage());
            dotBlackTexture = null;
        }
        
        try {
            // Charger dot-empty.png (token incorrect)
            dotEmptyTexture = new Texture(Gdx.files.internal("images/games/mmd/indicator/dot-empty.png"));
            dotEmptyTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.log("MastermindGameScreen", "✓ Texture dot-empty chargée");
        } catch (Exception e) {
            Gdx.app.error("MastermindGameScreen", "Erreur lors du chargement de dot-empty.png: " + e.getMessage());
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
        float baseX = 100;
        float baseY = 925;
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
     * Recalcule les positions de tous les tokens (placés et en mouvement) lors du resize
     * Utilise les coordonnées relatives au background stockées dans chaque token
     */
    private void recalculateTokenPositions() {
        // Recalculer les positions des tokens dans la grille (placés)
        for (AnimatedToken token : gridTokens) {
            if (token.relativeTargetX >= 0 && token.relativeTargetY >= 0) {
                // Recalculer la position en coordonnées écran
                token.currentX = currentBgX + token.relativeTargetX * currentScaleX;
                token.currentY = currentBgY + token.relativeTargetY * currentScaleY;
            }
        }
        
        // Recalculer les positions cibles des tokens en mouvement
        for (AnimatedToken token : movingTokens) {
            if (token.relativeTargetX >= 0 && token.relativeTargetY >= 0) {
                // Recalculer la position cible en coordonnées écran
                token.targetX = currentBgX + token.relativeTargetX * currentScaleX;
                token.targetY = currentBgY + token.relativeTargetY * currentScaleY;
                
                // Mettre à jour aussi la position de départ (relative aux positions de tokens)
                if (token.tokenType < positionCentersX.size) {
                    token.startX = positionCentersX.get(token.tokenType);
                    token.startY = positionCentersY.get(token.tokenType);
                }
            }
        }
    }
    
    /**
     * Dessine tous les types de tokens (position de départ, en mouvement, sur grille)
     */
    private void drawAllTokens(float scaleX, float scaleY) {
        if (tokenTextures == null) return;
        
        // Calculer l'alpha pour le fade out
        float alpha = 1.0f;
        if (isFadingOut) {
            alpha = Math.max(0.0f, 1.0f - (fadeOutTimer / fadeOutDuration));
        }
        
        batch.setColor(1, 1, 1, alpha);
        
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
        
        // Remettre la couleur normale
        batch.setColor(1, 1, 1, 1);
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

    private void drawTransitionToPicture() {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();

        // Utiliser uniquement l'animation de porte coulissante
        drawSlidingDoorTransition(screenWidth, screenHeight);
    }
    
    /**
     * Dessine l'animation de porte coulissante pour la transition vers l'image finale
     */
    private void drawSlidingDoorTransition(float screenWidth, float screenHeight) {
        // Dessiner d'abord l'image complète en arrière-plan
        batch.setColor(1, 1, 1, 1);
        drawFinalPhase();
        
        // Créer les deux panneaux qui se déplacent
        float halfWidth = screenWidth * 0.5f;
        
        // Calculer la hauteur d'affichage en fonction du ratio entre pixels et coordonnées monde
        // La texture a été capturée en pixels physiques, nous devons la mapper aux coordonnées monde
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        float screenPixelWidth = viewport.getScreenWidth();
        float screenPixelHeight = viewport.getScreenHeight();
        
        // Ratio de conversion pixels -> monde
        float pixelToWorldRatioX = worldWidth / screenPixelWidth;
        float pixelToWorldRatioY = worldHeight / screenPixelHeight;
        
        // La hauteur d'affichage doit correspondre à la hauteur de la texture convertie en coordonnées monde
        float displayHeight = worldHeight;
        
        Gdx.app.log("MastermindGameScreen", "DEBUG Ratios - World: " + worldWidth + "x" + worldHeight + 
                         ", ScreenPixels: " + screenPixelWidth + "x" + screenPixelHeight);
        Gdx.app.log("MastermindGameScreen", "DEBUG Ratios - PixelToWorld: " + pixelToWorldRatioX + "x" + pixelToWorldRatioY);
        
        // Panneau gauche (se déplace vers la gauche)
        float leftPanelX = leftPanelOffset;
        float leftPanelWidth = halfWidth;
        float leftPanelHeight = displayHeight;
        
        // Panneau droit (se déplace vers la droite)  
        float rightPanelX = halfWidth + rightPanelOffset;
        float rightPanelWidth = halfWidth;
        
        // Dessiner les panneaux avec la texture capturée du plateau de jeu
        if (gameStateTexture != null) {
            batch.setColor(1, 1, 1, 1);
            
            // Obtenir les dimensions de la texture
            int texW = gameStateTexture.getWidth();
            int texH = gameStateTexture.getHeight();
            int halfW = texW / 2;
            
            Gdx.app.log("MastermindGameScreen", "DEBUG: Texture - w:" + texW + " h:" + texH + " half:" + halfW);
            Gdx.app.log("MastermindGameScreen", "DEBUG: Panneaux - leftX:" + leftPanelX + " rightX:" + rightPanelX + " width:" + leftPanelWidth + " height:" + displayHeight);
            
            // Panneau gauche - partie gauche de la texture
            batch.draw(
                gameStateTexture,
                leftPanelX, 0,               // dest x,y
                leftPanelWidth, displayHeight, // dest w,h
                0, 0,                         // src x,y
                halfW, texH,                  // src w,h
                false, true                   // flipX, flipY
            );
            
            // Panneau droit - partie droite de la texture
            batch.draw(
                gameStateTexture,
                rightPanelX, 0,               // dest x,y
                rightPanelWidth, displayHeight, // dest w,h
                halfW, 0,                     // src x,y
                halfW, texH,                  // src w,h
                false, true                   // flipX, flipY
            );
            
            Gdx.app.log("MastermindGameScreen", "Dessin des panneaux - Texture: " + texW + "x" + texH + 
                             ", Panneaux: " + leftPanelWidth + "x" + leftPanelHeight);
        }
    }
    
    // Nouvelle méthode pour extraire la logique de dessin des cases et jetons
    private void drawGridAndTokens(float bgX, float bgY, float scaleX, float scaleY) {
        // Les cases restent toujours visibles (pas de fade out)
        for (AnimatedColumn column : gridPositions) {
            for (int i = 0; i < column.rectangles.size; i++) {
                Rectangle rect = column.rectangles.get(i);
                float boxX = bgX + rect.x * scaleX;
                float boxY = bgY + rect.y * scaleY;
                float boxWidth = rect.width * scaleX;
                float boxHeight = rect.height * scaleY;
                
                // Choisir la texture à afficher pour la case
                Texture textureToRender = boxTexture;
                
                // Gérer l'animation des cases (ouverture ou fermeture)
                if (i < column.animations.size) {
                    BoxAnimation anim = column.animations.get(i);
                    if (anim.isPlaying && BoxAnimation.sharedFrames != null && BoxAnimation.sharedFrames.size > 0) {
                        int frameIndex = Math.max(0, Math.min(anim.currentFrame, BoxAnimation.sharedFrames.size - 1));
                        textureToRender = BoxAnimation.sharedFrames.get(frameIndex);
                    }
                }
                
                // Dessiner la case toujours visible
                batch.setColor(1, 1, 1, 1);
                batch.draw(textureToRender, boxX, boxY, boxWidth, boxHeight);
            }
        }

        // Calculer l'alpha pour le fade out des tokens seulement
        float tokenAlpha = 1.0f;
        if (isFadingOut) {
            tokenAlpha = Math.max(0.0f, 1.0f - (fadeOutTimer / fadeOutDuration));
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
                            
                            batch.setColor(1, 1, 1, tokenAlpha);
                            batch.draw(tokenTexture, tokenX, tokenY, tokenWidth, tokenHeight);
                        }
                    }
                }
            }
        }
        
        // Remettre la couleur normale
        batch.setColor(1, 1, 1, 1);
        // La colonne courante est maintenant entièrement gérée par les tokens animés
    }

    private void disableGameElements() {
        gameElementsDisabled = true;
        
        // Plus besoin de préserver les positions - les boutons sont capturés avec le reste
        
        // Libérer les ressources du jeu seulement si on n'a pas de capture en cours
        if (backgroundTexture != null && !captureNextFramePending) {
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