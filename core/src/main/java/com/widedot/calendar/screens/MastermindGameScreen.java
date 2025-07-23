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
    private final Rectangle solveButton;
    private final Rectangle infoButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture fullImageTexture;
    private Texture sendButtonTexture;
    private Texture infoButtonTexture;
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
    private static final float TRANSITION_DURATION = 3.0f; // 3 secondes de transition
    private static final float FADE_TO_BLACK_DURATION = 1.0f; // 1 seconde pour fondu vers noir
    private static final float FADE_TO_IMAGE_DURATION = 2.0f; // 2 secondes pour fondu vers image
    
    // Constants
    private static final String CORRECT_SOUND_PATH = "audio/win.mp3";
    private static final String INCORRECT_SOUND_PATH = "audio/sliding.mp3";
    private static final String WIN_SOUND_PATH = "audio/win.mp3";
    private static final float SYMBOL_SIZE = 40f;
    private static final float CURSOR_BLINK_SPEED = 1.0f;
    
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
            
            String variantFolder = String.format("%02d", variant + 1);
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
        
        AnimatedColumn(int col) {
            super(col);
            this.animations = new Array<>();
            this.isAnimating = false;
        }
        
        void startAnimations() {
            if (!isAnimating) {
                isAnimating = true;
                
                // Créer une animation pour chaque case de la colonne
                for (int i = 0; i < rectangles.size; i++) {
                    BoxAnimation anim = new BoxAnimation(i * ANIMATION_DELAY);
                    animations.add(anim);
                }
            }
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
     * Constructeur avec paramètres dynamiques
     */
    public MastermindGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        // Stocker le thème
        this.theme = theme;
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.codeLength = 4;
        this.numberOfSymbols = 6;
        this.maxAttempts = 10;
        this.backgroundColor = new Color(0.1f, 0.1f, 0.2f, 1);
        this.textColor = new Color(1, 1, 1, 1);
        this.correctColor = new Color(0.7f, 0.9f, 0.7f, 1);
        this.incorrectColor = new Color(0.9f, 0.7f, 0.7f, 1);
        
        // Stocker le paramètre symbolImages pour plus tard (après l'initialisation des symboles)
        String symbolImagesParam = null;
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("codeLength")) {
                this.codeLength = ((Number) parameters.get("codeLength")).intValue();
                this.codeLength = Math.max(4, Math.min(6, this.codeLength)); // Limiter entre 4 et 6
            }
            if (parameters.containsKey("numberOfSymbols")) {
                this.numberOfSymbols = ((Number) parameters.get("numberOfSymbols")).intValue();
                this.numberOfSymbols = Math.max(4, Math.min(8, this.numberOfSymbols)); // Limiter entre 4 et 8
            }
            if (parameters.containsKey("maxAttempts")) {
                this.maxAttempts = ((Number) parameters.get("maxAttempts")).intValue();
                this.maxAttempts = Math.max(8, Math.min(15, this.maxAttempts)); // Limiter entre 8 et 15
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
        this.solveButton = new Rectangle(viewport.getWorldWidth() - 120, 20, 100, 50);
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
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/information.png"));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }

        // Charger la texture du fond d'écran
        try {
            this.backgroundTexture = new Texture(Gdx.files.internal("images/games/mmd/background/background.png"));
            this.backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du fond d'écran: " + e.getMessage());
            this.backgroundTexture = null;
        }
        
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
        
        // Initialiser la première tentative 
        for (int i = 0; i < codeLength; i++) {
            currentGuess.add(i % numberOfSymbols); // Commence avec les premiers symboles
        }
        
        // Charger la texture du thème
        loadThemeTexture();

        // Démarrer l'animation de la première colonne
        startColumnAnimation(0);
    }
    
    private void generateSecretCode() {
        secretCode.clear();
        
        // Générer un code avec des symboles uniques
        Array<Integer> availableSymbols = new Array<>();
        for (int i = 0; i < numberOfSymbols; i++) {
            availableSymbols.add(i);
        }
        
        // Mélanger et prendre les premiers symboles pour garantir l'unicité
        availableSymbols.shuffle();
        for (int i = 0; i < codeLength; i++) {
            secretCode.add(availableSymbols.get(i));
        }
        
        // Debug - afficher le code secret en mode test
        if (isTestMode) {
            System.out.print("Code secret: ");
            for (int symbol : secretCode) {
                System.out.print(symbolNames[symbol] + " ");
            }
            System.out.println();
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
        
        if (submitButton.contains(x, y)) {
            if (finalQuestionPhase) {
                submitFinalAnswer();
            } else {
                submitGuess();
            }
            return;
        }
        
        if (isTestMode && solveButton.contains(x, y)) {
            solveGame();
            return;
        }
        
        if (infoButton.contains(x, y)) {
            showInfoPanel = true;
            return;
        }
        
        // Vérifier si c'est un clic sur la tentative en cours (la palette n'est plus cliquable)
        handleCurrentGuessClick(x, y);
    }
    

    
    private void handleCurrentGuessClick(float x, float y) {
        // Position de la tentative en cours
        float guessY = 200;
        float guessStartX = (viewport.getWorldWidth() - codeLength * (SYMBOL_SIZE + 10)) / 2;
        
        for (int i = 0; i < codeLength; i++) {
            float symbolX = guessStartX + i * (SYMBOL_SIZE + 10);
            
            if (x >= symbolX && x <= symbolX + SYMBOL_SIZE && 
                y >= guessY && y <= guessY + SYMBOL_SIZE) {
                
                // Faire cycler le symbole à cette position (toutes les couleurs disponibles)
                int currentSymbol = currentGuess.get(i);
                int nextSymbol = (currentSymbol + 1) % numberOfSymbols;
                
                currentGuess.set(i, nextSymbol);
                return;
            }
        }
    }
    

    
    private void submitGuess() {
        if (gameFinished) return;
        
        // Démarrer l'animation de la prochaine colonne
        startColumnAnimation(attempts.size);
        
        // Créer une copie de la tentative courante
        Array<Integer> guess = new Array<>();
        for (int symbol : currentGuess) {
            guess.add(symbol);
        }
        
        // Ajouter aux tentatives
        attempts.add(guess);
        
        // Calculer le résultat
        GuessResult result = calculateResult(guess);
        results.add(result);
        
        // Vérifier si le jeu est gagné
        if (result.correctPosition == codeLength) {
            gameWon = true;
            finalQuestionPhase = true;
            
            // Charger la question finale spécifique au thème
            loadFinalQuestion();
            
            // Calculer quels carrés de l'image doivent être visibles basé sur le score
            calculateVisibleSquares();
            
            if (winSound != null) {
                winSound.play();
            }
            
            // Passer à la phase finale avec l'image
            startFinalPhase();
            
        } else if (attempts.size >= maxAttempts) {
            // Jeu perdu
            gameFinished = true;
            
            if (incorrectSound != null) {
                incorrectSound.play();
            }
            
        } else {
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
        
        // Mettre à jour les animations
        if (gridPositions != null) {
            for (AnimatedColumn column : gridPositions) {
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
                                if (anim.currentFrame >= anim.sharedFrames.size) {
                                    anim.currentFrame = anim.sharedFrames.size - 1; // Rester sur la dernière frame
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
                    }
                }
            }
        }
    }
    
    @Override
    protected void renderGame() {
        // Dessiner le fond d'écran
        if (backgroundTexture != null) {
            batch.setColor(1, 1, 1, 1);
            float screenWidth = viewport.getWorldWidth();
            float screenHeight = viewport.getWorldHeight();
            
            // Calculer les dimensions pour que l'image soit entièrement visible
            float bgRatio = (float)backgroundTexture.getWidth() / backgroundTexture.getHeight();
            float screenRatio = screenWidth / screenHeight;
            
            float bgWidth, bgHeight;
            float bgX, bgY;
            
            if (screenRatio > bgRatio) {
                // L'écran est plus large que l'image : adapter à la hauteur
                bgHeight = screenHeight * 0.9f; // 90% de la hauteur pour laisser une marge
                bgWidth = bgHeight * bgRatio;
            } else {
                // L'écran est plus haut que l'image : adapter à la largeur
                bgWidth = screenWidth * 0.9f; // 90% de la largeur pour laisser une marge
                bgHeight = bgWidth / bgRatio;
            }
            
            // Centrer le fond d'écran
            bgX = (screenWidth - bgWidth) / 2;
            bgY = (screenHeight - bgHeight) / 2;
            
            // Dessiner d'abord un fond uni
            batch.setColor(backgroundColor);
            batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
            
            // Puis dessiner le plateau de jeu centré
            batch.setColor(1, 1, 1, 1);
            batch.draw(backgroundTexture, bgX, bgY, bgWidth, bgHeight);
            
            // Modifier la partie de rendu des cases
            if (gridPositions != null) {
                float scaleX = bgWidth / backgroundTexture.getWidth();
                float scaleY = bgHeight / backgroundTexture.getHeight();
                
                for (AnimatedColumn column : gridPositions) {
                    for (int i = 0; i < column.rectangles.size; i++) {
                        Rectangle rect = column.rectangles.get(i);
                        float boxX = bgX + rect.x * scaleX;
                        float boxY = bgY + rect.y * scaleY;
                        float boxWidth = rect.width * scaleX;
                        float boxHeight = rect.height * scaleY;
                        
                        // Choisir la texture à afficher
                        Texture textureToRender = boxTexture;
                        
                        if (i < column.animations.size) {
                            BoxAnimation anim = column.animations.get(i);
                            // Si l'animation a démarré, on utilise soit la frame courante soit la dernière frame
                            if (anim.isPlaying && BoxAnimation.sharedFrames != null && BoxAnimation.sharedFrames.size > 0) {
                                int frameIndex = Math.min(anim.currentFrame, BoxAnimation.sharedFrames.size - 1);
                                textureToRender = BoxAnimation.sharedFrames.get(frameIndex);
                            }
                        }
                        
                        batch.draw(textureToRender, boxX, boxY, boxWidth, boxHeight);
                    }
                }
            }
        } else {
            // Fallback : fond uni si la texture n'est pas chargée
            batch.setColor(backgroundColor);
            batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        }

        if (isTransitioning) {
            drawTransition();
        } else if (finalQuestionPhase && !gameFinished) {
            drawFinalQuestionPhase();
        } else if (finalPhase && showWinImage) {
            drawFinalPhase();
        } else {
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
        
        // Titre
        bigFont.setColor(textColor);
        String title = "Mastermind";
        layout.setText(bigFont, title);
        bigFont.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 30);
        
        // Instructions
        font.setColor(textColor);
        String instructions = "Tentatives: " + attempts.size + "/" + maxAttempts;
        layout.setText(font, instructions);
        font.draw(batch, layout, 
            (screenWidth - layout.width) / 2,
            screenHeight - 80);
        
        // Dessiner les tentatives précédentes
        drawPreviousAttempts();
        
        // Dessiner la tentative en cours
        drawCurrentGuess();
        
        // Dessiner les symboles de sélection
        drawSymbolPalette();
        
        // Dessiner les boutons
        drawButtons();
        
        // Dessiner le bouton d'info
        drawInfoButton();
        
        // Message de fin de jeu
        if (gameFinished) {
            drawGameEndMessage();
        }
    }
    
    private void drawPreviousAttempts() {
        float startY = viewport.getWorldHeight() - 150;
        
        for (int i = 0; i < attempts.size; i++) {
            float y = startY - i * GUESS_SPACING;
            Array<Integer> attempt = attempts.get(i);
            GuessResult result = results.get(i);
            
            drawGuess(attempt, y, false);
            drawResultNextToGuess(result, y);
        }
    }
    
    private void drawCurrentGuess() {
        if (!gameFinished) {
            float y = 200;
            drawGuess(currentGuess, y, true);
        }
    }
    
    private void drawGuess(Array<Integer> guess, float y, boolean isCurrent) {
        float startX = (viewport.getWorldWidth() - codeLength * (SYMBOL_SIZE + 10)) / 2;
        
        for (int i = 0; i < codeLength; i++) {
            float x = startX + i * (SYMBOL_SIZE + 10);
            int symbolIndex = guess.get(i);
            
            // Dessiner l'image du symbole
            if (isCurrent) {
                batch.setColor(1f, 1f, 1f, 0.8f); // Légèrement transparent pour la tentative courante
            } else {
                batch.setColor(1f, 1f, 1f, 1f); // Couleur normale
            }
            
            if (symbolTextures != null && symbolIndex < symbolTextures.length && symbolTextures[symbolIndex] != null) {
                batch.draw(symbolTextures[symbolIndex], x, y, SYMBOL_SIZE, SYMBOL_SIZE);
            } else {
                // Fallback: dessiner un carré rouge en cas d'erreur
                batch.setColor(1f, 0f, 0f, 1f);
                batch.draw(whiteTexture, x, y, SYMBOL_SIZE, SYMBOL_SIZE);
            }
        }
    }
    

    

    

    
    private void drawResultNextToGuess(GuessResult result, float y) {
        // Calculer la position à droite du code
        float guessStartX = (viewport.getWorldWidth() - codeLength * (SYMBOL_SIZE + 10)) / 2;
        float guessEndX = guessStartX + codeLength * (SYMBOL_SIZE + 10);
        float resultX = guessEndX + 20; // 20 pixels d'espacement
        
        // Dessiner les indicateurs de feedback sous forme de petits ronds
        float feedbackRadius = 6f;
        float feedbackSpacing = 15f;
        
        // Dessiner les ronds pour les positions exactes (vert foncé)
        for (int i = 0; i < result.correctPosition; i++) {
            float feedbackX = resultX + i * feedbackSpacing;
            float feedbackY = y + SYMBOL_SIZE/2;
            drawSmallCircle(feedbackX, feedbackY, feedbackRadius, new Color(0.2f, 0.7f, 0.2f, 1)); // Vert foncé
        }
        
        // Dessiner les cercles pour les couleurs correctes mais mal placées (gris clair)
        for (int i = 0; i < result.correctSymbol; i++) {
            float feedbackX = resultX + (result.correctPosition + i) * feedbackSpacing;
            float feedbackY = y + SYMBOL_SIZE/2;
            drawSmallCircle(feedbackX, feedbackY, feedbackRadius, new Color(0.5f, 0.5f, 0.5f, 1)); // gris clair
        }
    }
    
    /**
     * Dessine un petit cercle pour le feedback (méthode adaptée de QuestionAnswer)
     */
    private void drawSmallCircle(float centerX, float centerY, float radius, Color color) {
        batch.setColor(color);
        
        // Dessiner le cercle rempli ligne par ligne (plus efficace et plus beau)
        for (float py = centerY - radius; py <= centerY + radius; py += 1f) {
            float dy = py - centerY;
            float distanceFromCenter = Math.abs(dy);
            
            if (distanceFromCenter <= radius) {
                // Calculer la largeur de la ligne à cette hauteur
                float halfWidth = (float) Math.sqrt(radius * radius - dy * dy);
                float lineWidth = halfWidth * 2;
                
                if (lineWidth > 0) {
                    batch.draw(whiteTexture, centerX - halfWidth, py, lineWidth, 1f);
                }
            }
        }
        
        // Dessiner un contour léger
        batch.setColor(color.r * 0.7f, color.g * 0.7f, color.b * 0.7f, color.a);
        
        // Contour simplifié avec quelques points
        for (float angle = 0; angle < 360; angle += 10) {
            float radians = angle * (float) Math.PI / 180f;
            float borderX = centerX + (float) Math.cos(radians) * (radius - 1);
            float borderY = centerY + (float) Math.sin(radians) * (radius - 1);
            batch.draw(whiteTexture, borderX - 0.5f, borderY - 0.5f, 1f, 1f);
        }
    }
    

    
    /**
     * Dessine un petit cercle vide (contour seulement) pour le feedback
     */
    private void drawSmallCircleOutline(float centerX, float centerY, float radius, Color color) {
        batch.setColor(color);
        int segments = 16;
        float angleStep = 360f / segments;
        
        for (int i = 0; i < segments; i++) {
            float angle = i * angleStep;
            float x = centerX + (float) Math.cos(Math.toRadians(angle)) * radius;
            float y = centerY + (float) Math.sin(Math.toRadians(angle)) * radius;
            
            // Dessiner des points plus épais pour le contour
            batch.draw(whiteTexture, x - 1.5f, y - 1.5f, 3f, 3f);
        }
    }
    
    private void drawSymbolPalette() {
        float symbolY = 120;
        float symbolStartX = (viewport.getWorldWidth() - numberOfSymbols * (SYMBOL_SIZE + 10)) / 2;
        
        // Titre de la palette
        font.setColor(textColor);
        String paletteTitle = "Cliquez pour sélectionner:";
        layout.setText(font, paletteTitle);
        font.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            symbolY + SYMBOL_SIZE + 20);
        
        for (int i = 0; i < numberOfSymbols; i++) {
            float symbolX = symbolStartX + i * (SYMBOL_SIZE + 10);
            
            // Dessiner l'image du symbole
            batch.setColor(1f, 1f, 1f, 1f);
            if (symbolTextures != null && i < symbolTextures.length && symbolTextures[i] != null) {
                batch.draw(symbolTextures[i], symbolX, symbolY, SYMBOL_SIZE, SYMBOL_SIZE);
            } else {
                // Fallback: dessiner un carré rouge en cas d'erreur
                batch.setColor(1f, 0f, 0f, 1f);
                batch.draw(whiteTexture, symbolX, symbolY, SYMBOL_SIZE, SYMBOL_SIZE);
            }
        }
    }
    
    private void drawButtons() {
        drawButton(backButton, "Retour", textColor);
        
        if (!gameFinished) {
            // Positionner le bouton send à droite de la zone de saisie de la réponse
            updateSubmitButtonPosition();
            drawSendButton(submitButton);
        }
        
        // Dessiner le bouton Résoudre (en mode test uniquement)
        if (isTestMode && !gameFinished) {
            drawButton(solveButton, "Résoudre", textColor);
        }
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
            inputBoxX + inputBoxWidth + 10, // 10 pixels de marge
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
        drawButton(backButton, "Retour", textColor);
        updateSubmitButtonPosition();
        drawSendButton(submitButton);
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
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
            // Dessiner l'image du bouton info
            batch.setColor(1f, 1f, 1f, 1f); // Couleur blanche pour afficher l'image normalement
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);
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
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
        infoButton.setPosition(viewport.getWorldWidth() - 60, viewport.getWorldHeight() - 60);
        
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

} 