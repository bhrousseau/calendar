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
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.audio.Sound;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.Config;

/**
 * Écran de jeu pour le mini-jeu de Questions et Réponses
 */
public class QuestionAnswerGameScreen extends GameScreen {
    // Input processor pour la saisie de texte
    private InputAdapter inputProcessor;
    // UI
    private final BitmapFont font;
    private final BitmapFont bigFont;
    private final GlyphLayout layout;
    private final Rectangle backButton;
    private final Rectangle submitButton;
    private final Rectangle solveButton;
    private Color backgroundColor;
    private boolean isTestMode;
    private final Texture whiteTexture;
    private Texture fullImageTexture;
    private Texture sendButtonTexture;
    private Theme theme;
    
    // Game data
    private Array<QuestionData> questions;
    private Array<QuestionData> gameQuestions; // Les 10 questions pour cette partie
    private QuestionData currentQuestion;
    private String userAnswer;
    private String statusMessage;
    private Color statusColor;
    private boolean gameWon;
    private boolean showFeedback;
    private float feedbackTimer;

    // Game progression
    private int currentQuestionIndex;
    private int correctAnswers;
    private int totalQuestions;
    private boolean gameFinished;
    private boolean finalQuestionPhase;
    private QuestionData finalQuestion;
    private boolean[] questionResults; // true = correct, false = incorrect
    
    // Game parameters
    private String questionsFile;
    private boolean caseSensitive;
    private Color textColor;
    private Color inputColor;
    private Color correctColor;
    private Color incorrectColor;
    private Color circleColor;
    private Color circleEmptyColor;
    
    // Input handling
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
    
    // Constants
    private static final String CORRECT_SOUND_PATH = "audio/win.mp3";
    private static final String INCORRECT_SOUND_PATH = "audio/sliding.mp3";
    private static final String WIN_SOUND_PATH = "audio/win.mp3";
    private static final float FEEDBACK_DURATION = 1.0f;
    private static final float CURSOR_BLINK_SPEED = 1.0f;
    
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
    
    /**
     * Constructeur avec paramètres dynamiques
     */
    public QuestionAnswerGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        // Stocker le thème
        this.theme = theme;
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.questionsFile = "questions.json";
        this.caseSensitive = false;
        this.backgroundColor = new Color(0.1f, 0.1f, 0.2f, 1);
        this.textColor = new Color(1, 1, 1, 1);
        this.inputColor = new Color(0.8f, 0.9f, 1, 1);
        this.correctColor = new Color(0.7f, 0.9f, 0.7f, 1); // Vert pastel
        this.incorrectColor = new Color(0.9f, 0.7f, 0.7f, 1); // Rouge pastel
        this.circleColor = new Color(0.7f, 0.9f, 0.7f, 1); // Vert pastel
        this.circleEmptyColor = new Color(0.3f, 0.3f, 0.3f, 1);
        
        // Initialiser les variables de progression
        this.totalQuestions = 10;
        this.currentQuestionIndex = 0;
        this.correctAnswers = 0;
        this.gameFinished = false;
        this.finalQuestionPhase = false;
        this.finalQuestion = null;
        this.questionResults = new boolean[20]; // Taille maximale pour couvrir tous les cas
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("questionsFile")) {
                this.questionsFile = (String) parameters.get("questionsFile");
            }
            if (parameters.containsKey("caseSensitive")) {
                this.caseSensitive = (Boolean) parameters.get("caseSensitive");
            }
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String) parameters.get("bgColor");
                this.backgroundColor = parseColor(bgColor);
            }
            if (parameters.containsKey("textColor")) {
                String color = (String) parameters.get("textColor");
                this.textColor = parseColor(color);
            }
            if (parameters.containsKey("totalQuestions")) {
                this.totalQuestions = ((Number) parameters.get("totalQuestions")).intValue();
                this.totalQuestions = Math.max(1, Math.min(20, this.totalQuestions)); // Limiter entre 1 et 20
            }
        }
        
        // Réinitialiser le tableau des résultats avec la bonne taille
        this.questionResults = new boolean[this.totalQuestions];
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        System.out.println("Mode test QuestionAnswer: " + isTestMode);
        
        // Initialiser les éléments UI
        this.font = new BitmapFont();
        this.font.getData().setScale(1.2f);
        this.bigFont = new BitmapFont();
        this.bigFont.getData().setScale(1.8f);
        this.layout = new GlyphLayout();
        
        this.backButton = new Rectangle(20, 20, 100, 50);
        this.submitButton = new Rectangle(0, 0, 120, 50); // Position sera calculée dynamiquement
        this.solveButton = new Rectangle(viewport.getWorldWidth() - 240, 20, 100, 50);
        
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
        
        // Initialiser l'état du jeu
        this.userAnswer = "";
        this.statusMessage = "";
        this.statusColor = textColor;
        this.gameWon = false;
        this.showFeedback = false;
        this.feedbackTimer = 0;
        this.inputText = "";
        this.inputFocused = true;
        this.cursorBlinkTimer = 0;
        this.showCursor = true;
        
        // Charger les sons
        loadSounds();
        
        // Initialiser le générateur de nombres aléatoires et les variables d'image
        this.random = new Random();
        this.visibleSquares = new HashSet<>();
        this.imageSquares = null;
        
        // Créer l'input processor pour la saisie de texte
        createInputProcessor();
        

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
    
    private void loadSounds() {
        try {
            this.correctSound = Gdx.audio.newSound(Gdx.files.internal(CORRECT_SOUND_PATH));
            this.incorrectSound = Gdx.audio.newSound(Gdx.files.internal(INCORRECT_SOUND_PATH));
            this.winSound = Gdx.audio.newSound(Gdx.files.internal(WIN_SOUND_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sons: " + e.getMessage());
        }
    }
    
    private void createInputProcessor() {
        inputProcessor = new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (gameFinished || showFeedback) return false;
                
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
                if (gameFinished || showFeedback) return false;
                
                if (keycode == Input.Keys.BACKSPACE && inputText.length() > 0) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                    return true;
                } else if (keycode == Input.Keys.ENTER || keycode == Input.Keys.NUMPAD_ENTER) {
                    submitAnswer();
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
            
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldPos = new Vector3(screenX, screenY, 0);
                viewport.unproject(worldPos);
                
                // Si le jeu est terminé, retourner au menu sur n'importe quel clic
                if (gameFinished) {
                    System.out.println("Retour au menu après victoire");
                    returnToMainMenu();
                    return true;
                }
                
                if (backButton.contains(worldPos.x, worldPos.y)) {
                    returnToMainMenu();
                    return true;
                } else if (submitButton.contains(worldPos.x, worldPos.y)) {
                    submitAnswer();
                    return true;
                } else if (isTestMode && solveButton.contains(worldPos.x, worldPos.y)) {
                    System.out.println("Bouton Résoudre cliqué (mode test) - QuestionAnswer");
                    solveGame();
                    return true;
                }
                return false;
            }
        };
    }
    
    @Override
    protected Theme loadTheme(int day) {
        return theme; // Le thème est déjà fourni au constructeur
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
        // Charger les questions depuis le fichier JSON
        loadQuestions();
        
        // Préparer les questions pour la partie
        prepareGameQuestions();
        
        // Démarrer avec la première question
        startQuestion(0);
        
        // Positionner les boutons
        updateButtonPositions();
        
        // Charger la texture du thème dès l'initialisation
        loadThemeTexture();
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
     * Calcule quels carrés doivent être visibles basé sur le pourcentage de bonnes réponses
     * Maximum 50% de l'image révélée (50 carrés sur 100)
     */
    private void calculateVisibleSquares() {
        visibleSquares.clear();
        
        // Calculer le pourcentage de bonnes réponses
        float scorePercentage = (float) correctAnswers / totalQuestions;
        
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
    
    private void prepareGameQuestions() {
        gameQuestions = new Array<>();
        
        if (questions.size >= totalQuestions) {
            // Mélanger les questions et en prendre le nombre demandé
            Array<QuestionData> shuffledQuestions = new Array<>(questions);
            shuffledQuestions.shuffle();
            
            for (int i = 0; i < totalQuestions; i++) {
                gameQuestions.add(shuffledQuestions.get(i));
            }
        } else {
            // Pas assez de questions, utiliser toutes les questions disponibles
            totalQuestions = questions.size;
            gameQuestions.addAll(questions);
            gameQuestions.shuffle();
        }
        

    }
    
    private void startQuestion(int index) {
        if (index < totalQuestions && index < gameQuestions.size) {
            currentQuestion = gameQuestions.get(index);
            inputText = "";
            statusMessage = "";
            showFeedback = false;
        } else {
            // Toutes les questions ont été posées
            finishGame();
        }
    }
    
    private void finishGame() {
        // Passer à la phase finale avec question sur le thème
        finalQuestionPhase = true;
        
        // Charger la question finale spécifique au thème
        loadFinalQuestion();
        
        // Calculer quels carrés de l'image doivent être visibles basé sur le score
        calculateVisibleSquares();
        
        // Réinitialiser l'état pour la question finale
        inputText = "";
        statusMessage = "";
        statusColor = textColor;
        showFeedback = false;
        
        // Repositionner les boutons pour la phase finale
        updateButtonPositions();
    }
    
    private void loadFinalQuestion() {
        // Charger la question spécifique au thème depuis le fichier JSON
        if (theme != null) {
            String themeName = theme.getName();
            String themeTitle = theme.getTitle();
            String fullImagePath = theme.getFullImagePath();
            
            // Forcer le rechargement de la texture si elle n'est pas chargée
            if (fullImageTexture == null && fullImagePath != null && !fullImagePath.isEmpty()) {
                try {
                    fullImageTexture = new Texture(Gdx.files.internal(fullImagePath));
                    // Découper l'image en carrés après rechargement
                    createImageSquares();
                } catch (Exception e) {
                    System.err.println("Erreur rechargement texture: " + e.getMessage());
                }
            }
            
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
    
    private void completeFinalGame() {
        gameFinished = true;
        gameWon = true;
        

        
        // Calculer le score final basé sur les questions générales ET la question finale
        int baseScore = (int) ((correctAnswers * 80.0f) / totalQuestions); // 80% max pour les questions générales
        int finalScore = baseScore + 20; // +20% pour la question finale réussie
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, finalScore);
            adventGame.setVisited(dayId, true);
        }
        
        if (winSound != null) {
            winSound.play();
        }
        
        statusMessage = "Bravo! oeuvre identifiée correctement!";
        statusColor = correctColor;
    }
    
    private void loadQuestions() {
        questions = new Array<>();
        
        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(Gdx.files.internal(questionsFile));
            
            JsonValue questionsArray = root.get("questions");
            if (questionsArray != null) {
                for (JsonValue questionValue = questionsArray.child; questionValue != null; questionValue = questionValue.next) {
                    String question = questionValue.getString("question");
                    String answer = questionValue.getString("answer");
                    
                    QuestionData data = new QuestionData(question, answer);
                    
                    // Charger les réponses alternatives si elles existent
                    JsonValue alternativesArray = questionValue.get("alternatives");
                    if (alternativesArray != null) {
                        for (JsonValue alt = alternativesArray.child; alt != null; alt = alt.next) {
                            data.acceptedAnswers.add(alt.asString());
                        }
                    }
                    
                    questions.add(data);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des questions: " + e.getMessage());
            // Créer une question par défaut en cas d'erreur
            questions.add(new QuestionData("Question par défaut?", "réponse"));
        }
    }
    
    private void updateButtonPositions() {
        if (finalQuestionPhase && !gameFinished) {
            updateButtonPositionsForFinalPhase();
        } else {
            updateButtonPositionsForNormalPhase();
        }
    }
    
    private void updateButtonPositionsForNormalPhase() {
        // Calculer la position de la zone de saisie pour positionner le bouton send
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float centerX = viewport.getWorldWidth() / 2;
        float inputY = viewport.getWorldHeight() / 2; // Utiliser le nouveau positionnement centré
        float inputBoxX = centerX - inputBoxWidth / 2;
        float inputBoxY = inputY - inputBoxHeight / 2;
        
        // Définir la taille du bouton send (carré basé sur la hauteur de la zone de saisie)
        float sendButtonSize = inputBoxHeight;
        
        // Positionner le bouton send juste à côté du bord droit de la zone de saisie
        submitButton.setPosition(
            inputBoxX + inputBoxWidth + 10, // 10 pixels de marge
            inputBoxY
        );
        submitButton.setSize(sendButtonSize, sendButtonSize);
        
        // Positionner le bouton Résoudre en bas à droite
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
    }
    
    private void updateButtonPositionsForFinalPhase() {
        // Calculer la position de la zone de saisie dans la phase finale
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer la position Y de la zone de saisie selon la nouvelle disposition
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
        
        // Positionner le bouton Résoudre en bas à droite
        solveButton.setPosition(screenWidth - 120, 20);
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le timer de feedback
        if (showFeedback) {
            feedbackTimer += delta;
            if (feedbackTimer >= FEEDBACK_DURATION) {
                showFeedback = false;
                feedbackTimer = 0;
                
                // Passer à la question suivante après le feedback
                if (!gameFinished) {
                    currentQuestionIndex++;
                    startQuestion(currentQuestionIndex);
                }
            }
        }
        
        // Mettre à jour le clignotement du curseur
        if (!gameFinished) {
            cursorBlinkTimer += delta * CURSOR_BLINK_SPEED;
            showCursor = (cursorBlinkTimer % 2.0f) < 1.0f;
        }
    }
    
    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());
        
        if (currentQuestion == null && !gameFinished) {
            // Afficher un message d'erreur
            font.setColor(incorrectColor);
            layout.setText(font, "Erreur: Aucune question disponible");
            font.draw(batch, layout, 
                (viewport.getWorldWidth() - layout.width) / 2,
                viewport.getWorldHeight() / 2);
            return;
        }
        
        float centerX = viewport.getWorldWidth() / 2;
        float screenCenterY = viewport.getWorldHeight() / 2;
        
        // Centrer verticalement tout l'ensemble des éléments
        float inputY = screenCenterY; // Zone de saisie au centre
        float statusY = inputY - 40; // Message de résultat (40px sous la zone de saisie)
        float progressY = inputY - 140; // Jauge : 40px sous le message de résultat + 60px d'espacement interne
        
        // Si en phase finale, afficher l'image et la question finale
        if (finalQuestionPhase && !gameFinished) {
            float questionY = viewport.getWorldHeight() * 0.65f; // Position originale pour la phase finale
            drawFinalPhase(questionY, inputY, statusY);
            return;
        }
        
        // Si le jeu est terminé, afficher l'écran de fin
        if (gameFinished) {
            drawGameFinishedScreen();
            return;
        }
        
        // Calculer la position de la question juste au-dessus de la zone de saisie
        float questionInputBoxHeight = 50;
        float questionSpacing = 80; // Espacement entre la question et la zone de saisie
        float questionY = inputY + questionInputBoxHeight / 2 + questionSpacing;
        
        // Dessiner la question centrée juste au-dessus de la zone de saisie
        bigFont.setColor(textColor);
        layout.setText(bigFont, currentQuestion.question, textColor, viewport.getWorldWidth() - 40, Align.left, true);
        bigFont.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            questionY);
        
        // Dessiner la zone de saisie
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float inputBoxX = centerX - inputBoxWidth / 2;
        float inputBoxY = inputY - inputBoxHeight / 2;
        
        // Fond de la zone de saisie
        batch.setColor(0.2f, 0.2f, 0.3f, 1);
        batch.draw(whiteTexture, inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight);
        
        // Bordure de la zone de saisie
        batch.setColor(inputFocused ? inputColor : textColor);
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
        font.setColor(inputColor);
        String displayText = inputText;
        if (inputFocused && showCursor) {
            displayText += "|";
        }
        layout.setText(font, displayText);
        font.draw(batch, layout, 
            inputBoxX + 10,
            inputBoxY + inputBoxHeight / 2 + layout.height / 2);
        
        // Message de statut juste en dessous de la zone de saisie
        if (!statusMessage.isEmpty()) {
            font.setColor(statusColor);
            layout.setText(font, statusMessage);
            font.draw(batch, layout, 
                (viewport.getWorldWidth() - layout.width) / 2,
                statusY);
        }
        
        // Dessiner la progression et les étoiles en dessous de la zone de saisie
        if (!finalQuestionPhase) {
            drawProgressBar(progressY);
        }
        
        // Dessiner les boutons seulement si le jeu n'est pas terminé
        if (!gameFinished) {
            drawButton(backButton, "Retour", textColor);
            drawSendButton(submitButton);
            // Dessiner le bouton Résoudre (en mode test uniquement)
            if (isTestMode) {
                drawButton(solveButton, "Résoudre", textColor);
            }
        } else {
            drawButton(backButton, "Retour", textColor);
        }
    }
    
    private void drawProgressBar(float y) {
        // Afficher le numéro de question actuelle
        font.setColor(textColor);
        String progressText = "Question " + (currentQuestionIndex+1) + " / " + totalQuestions;
        layout.setText(font, progressText);
        font.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            y + 60); // Doubler l'espace entre le texte et la jauge
        
        // Dessiner les cercles
        float circleSize = 25f;
        float spacing = 8f;
        float totalWidth = totalQuestions * circleSize + (totalQuestions - 1) * spacing;
        float startX = (viewport.getWorldWidth() - totalWidth) / 2;
        
        for (int i = 0; i < totalQuestions; i++) {
            float circleX = startX + i * (circleSize + spacing);
            float circleY = y - circleSize / 2;
            
            // Choisir la couleur du cercle selon le résultat de la question
            Color currentCircleColor;
            if (i < currentQuestionIndex) {
                // Question déjà répondue : vert si correct, rouge si incorrect
                currentCircleColor = questionResults[i] ? this.correctColor : this.incorrectColor;
            } else if (i == currentQuestionIndex) {
                // Question en cours : couleur par défaut
                currentCircleColor = this.circleEmptyColor;
            } else {
                // Questions pas encore posées : gris
                currentCircleColor = this.circleEmptyColor;
            }
            
            drawCircle(circleX, circleY, circleSize, currentCircleColor);
        }
    }
    
    private void drawCircle(float x, float y, float size, Color color) {
        // Dessiner un cercle en utilisant des lignes horizontales
        batch.setColor(color);
        
        float centerX = x + size / 2;
        float centerY = y + size / 2;
        float radius = size / 2;
        
        // Dessiner le cercle rempli ligne par ligne (plus efficace)
        for (float py = y; py <= y + size; py += 1f) {
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
    
    private void drawFinalPhase(float questionY, float inputY, float statusY) {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        
        // Calculer le pourcentage pour l'affichage
        float scorePercentage = (correctAnswers * 100.0f) / totalQuestions;
        
        // Variables pour le positionnement vertical depuis le haut
        float currentY = screenHeight - 30; // Commencer 30px du haut
        float spacing = 15; // Espacement entre les éléments
        
        // 1. Afficher le score des questions générales en haut
        font.setColor(textColor);
        String scoreText = "Score : " + correctAnswers + "/" + totalQuestions + " (" + (int)scorePercentage + "%)";
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
        
        // 4. Message de statut
        if (!statusMessage.isEmpty()) {
            font.setColor(statusColor);
            layout.setText(font, statusMessage);
            font.draw(batch, layout, 
                (screenWidth - layout.width) / 2,
                currentY);
            currentY -= (layout.height + spacing);
        }
        
        // 5. Calculer l'espace disponible pour l'image
        float bottomMargin = 80; // Marge en bas pour les boutons
        float availableHeight = currentY - bottomMargin;
        
        // 6. Afficher l'image adaptative
        if (fullImageTexture != null && availableHeight > 100) { // Minimum 100px de hauteur
            drawAdaptiveImage(availableHeight);
        }
        
        // 7. Dessiner les boutons en bas
        drawButton(backButton, "Retour", textColor);
        drawSendButton(submitButton);
        // Dessiner le bouton Résoudre (en mode test uniquement)
        if (isTestMode) {
            drawButton(solveButton, "Résoudre", textColor);
        }
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
        float imageY = availableHeight - imageHeight + 40; // 40px de marge en bas de l'espace
        
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
    
    private void drawImageCentered(float centerY) {
        if (fullImageTexture == null || imageSquares == null) {
            return;
        }
        
        // Calculer la taille et position de l'image (centrée)
        float imageWidth = 250f;
        float imageHeight = 250f;
        float imageX = (viewport.getWorldWidth() - imageWidth) / 2;
        float imageY = centerY - imageHeight / 2;
        
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
    
    private void drawInputArea(float inputY) {
        float inputBoxWidth = 400;
        float inputBoxHeight = 50;
        float inputBoxX = (viewport.getWorldWidth() - inputBoxWidth) / 2;
        float inputBoxY = inputY - inputBoxHeight / 2;
        
        // Fond de la zone de saisie
        batch.setColor(0.2f, 0.2f, 0.3f, 1);
        batch.draw(whiteTexture, inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight);
        
        // Bordure de la zone de saisie
        batch.setColor(inputFocused ? inputColor : textColor);
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
        font.setColor(inputColor);
        String displayText = inputText;
        if (inputFocused && showCursor) {
            displayText += "|";
        }
        layout.setText(font, displayText);
        font.draw(batch, layout, 
            inputBoxX + 10,
            inputBoxY + inputBoxHeight / 2 + layout.height / 2);
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
        batch.setColor(inputFocused ? inputColor : textColor);
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
        font.setColor(inputColor);
        String displayText = inputText;
        if (inputFocused && showCursor) {
            displayText += "|";
        }
        layout.setText(font, displayText);
        font.draw(batch, layout, 
            inputBoxX + 10,
            inputBoxY + inputBoxHeight / 2 + layout.height / 2);
    }
    
    private void drawGameFinishedScreen() {
        float centerX = viewport.getWorldWidth() / 2;
        
        // Titre de fin
        bigFont.setColor(correctColor);
        String title = "Félicitations !";
        layout.setText(bigFont, title);
        bigFont.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            viewport.getWorldHeight() * 0.7f);
        
        // Score final
        font.setColor(textColor);
        String scoreText = "Questions générales: " + correctAnswers + " / " + totalQuestions;
        layout.setText(font, scoreText);
        font.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            viewport.getWorldHeight() * 0.6f);
        
        // Message de succès
        String successText = "oeuvre correctement identifiée !";
        layout.setText(font, successText);
        font.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            viewport.getWorldHeight() * 0.55f);
        
        // Afficher l'image finale non floutée
        if (fullImageTexture != null) {
            float imageWidth = 200f;
            float imageHeight = 200f;
            float imageX = (viewport.getWorldWidth() - imageWidth) / 2;
            float imageY = viewport.getWorldHeight() * 0.25f;
            
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fullImageTexture, imageX, imageY, imageWidth, imageHeight);
        }
        
        // Message pour indiquer de cliquer
        font.setColor(textColor);
        String clickMessage = "Cliquez pour retourner au menu";
        layout.setText(font, clickMessage);
        font.draw(batch, layout, 
            (viewport.getWorldWidth() - layout.width) / 2,
            viewport.getWorldHeight() * 0.15f);
        
        // Dessiner le bouton Retour
        drawButton(backButton, "Retour", textColor);
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
            drawButton(button, "Send", textColor);
        }
    }
    
    @Override
    protected void handleInput() {
        // La gestion des entrées est maintenant déléguée à l'InputProcessor
        // Cette méthode peut rester vide ou gérer des entrées spécifiques si nécessaire
    }
    
    private void submitAnswer() {
        if (gameFinished || inputText.trim().isEmpty()) {
            return;
        }
        
        // Gérer la phase finale différemment
        if (finalQuestionPhase) {
            submitFinalAnswer();
            return;
        }
        
        // Phase normale (questions générales)
        if (currentQuestion == null) {
            return;
        }
        
        boolean isCorrect = currentQuestion.isAnswerCorrect(inputText, caseSensitive);
        
        // Enregistrer le résultat de cette question
        questionResults[currentQuestionIndex] = isCorrect;
        
        if (isCorrect) {
            correctAnswers++;
            statusMessage = "Bonne réponse!";
            statusColor = correctColor;
            
            if (correctSound != null) {
                correctSound.play();
            }
            
        } else {
            statusMessage = "Mauvaise réponse!";
            statusColor = incorrectColor;
            
            if (incorrectSound != null) {
                incorrectSound.play();
            }
        }
        
        showFeedback = true;
        feedbackTimer = 0;
        inputText = ""; // Vider le champ de saisie
    }
    
    private void submitFinalAnswer() {
        if (finalQuestion == null || inputText.trim().isEmpty()) {
            return;
        }
        
        boolean isCorrect = finalQuestion.isAnswerCorrect(inputText, caseSensitive);
        
        if (isCorrect) {
            statusMessage = "Correct! oeuvre identifiée!";
            statusColor = correctColor;
            
            if (correctSound != null) {
                correctSound.play();
            }
            
            // Terminer le jeu avec succès
            completeFinalGame();
            
        } else {
            statusMessage = "Mauvaise réponse. Essayez encore!";
            statusColor = incorrectColor;
            
            if (incorrectSound != null) {
                incorrectSound.play();
            }
            
            showFeedback = false; // Permettre de réessayer immédiatement
        }
        
        inputText = ""; // Vider le champ de saisie
    }
    
    /**
     * Résout automatiquement le jeu (mode test)
     */
    private void solveGame() {
        System.out.println("Résolution automatique du jeu QuestionAnswer (mode test)");
        
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
        if (fullImageTexture != null) fullImageTexture.dispose();
        if (sendButtonTexture != null) sendButtonTexture.dispose();
        if (correctSound != null) correctSound.dispose();
        if (incorrectSound != null) incorrectSound.dispose();
        if (winSound != null) winSound.dispose();
        
        // Nettoyer les ressources d'image découpée
        imageSquares = null;
        if (visibleSquares != null) {
            visibleSquares.clear();
        }
    }
} 