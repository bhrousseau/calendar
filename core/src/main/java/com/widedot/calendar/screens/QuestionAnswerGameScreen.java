package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.Config;
import com.widedot.calendar.display.DisplayConfig;
import com.widedot.calendar.config.DayMappingManager;
import com.widedot.calendar.ui.BottomInputBar;
import com.widedot.calendar.utils.AnswerMatcher;
import com.widedot.calendar.debug.QuestionAnswerDebugManager;
import com.widedot.calendar.utils.CarlitoFontManager;

/**
 * Écran de jeu pour le mini-jeu de Questions et Réponses
 */
public class QuestionAnswerGameScreen extends GameScreen {
    
    /**
     * Classe pour stocker les données d'une question
     */
    private static class QuestionData {
        int id;
        String question;
        String answer;
        String[] alternatives;
        
        QuestionData(int id, String question, String answer, String[] alternatives) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.alternatives = alternatives;
        }
    }
    // Input processor pour les clics et raccourcis clavier
    private InputAdapter inputProcessor;
    
    // UI
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Rectangle closeButton;
    private final Rectangle infoButton;
    private final Color infoPanelColor;
    private boolean showInfoPanel;
    private final Texture whiteTexture;
    private Texture closeButtonTexture;
    private Texture infoButtonTexture;
    private Color backgroundColor;
    private boolean isTestMode;
    
    // Variables de calcul du background mutualisées (comme MastermindGameScreen)
    private float currentBgX, currentBgY, currentBgWidth, currentBgHeight;
    private float currentScaleX, currentScaleY;
    
    // Game data
    private Theme theme;
    private String gameReference;
    private ObjectMap<String, Object> gameParameters;
    
    // Questions et réponses
    private Array<QuestionData> allQuestions;
    private Array<QuestionData> currentQuestions;
    private int currentQuestionIndex;
    private int correctAnswers;
    private int totalQuestions;
    private int victoryThreshold;
    private boolean gameFinished;
    private boolean gameWon;
    
    // UI pour les questions
    private BottomInputBar inputBar;
    private com.badlogic.gdx.scenes.scene2d.Stage inputStage;
    private com.badlogic.gdx.scenes.scene2d.ui.Skin inputSkin;
    private com.badlogic.gdx.scenes.scene2d.ui.Table rootTable;
    
    // Sons
    private Sound winSound;
    private Sound wrongSound;
    
    // Debug system
    private String questionsFile = ""; // Valeur par défaut vide
    private QuestionAnswerDebugManager debugManager;
    
    /**
     * Constructeur avec paramètres dynamiques
     */
    public QuestionAnswerGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        // Stocker le thème et les paramètres
        this.theme = theme;
        this.gameParameters = parameters;
        
        // Obtenir la référence du jeu pour la sauvegarde
        this.gameReference = DayMappingManager.getInstance().getGameReferenceForDay(dayId);
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        Gdx.app.log("QuestionAnswerGameScreen", "Mode test: " + isTestMode);
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            if (parameters.containsKey("questionsFile")) {
                String file = (String) parameters.get("questionsFile");
                this.questionsFile = (file != null) ? file : "";
            }
            if (parameters.containsKey("totalQuestions")) {
                this.totalQuestions = ((Number) parameters.get("totalQuestions")).intValue();
            } else {
                this.totalQuestions = 5; // Valeur par défaut
            }
            if (parameters.containsKey("victoryThreshold")) {
                this.victoryThreshold = ((Number) parameters.get("victoryThreshold")).intValue();
            } else {
                this.victoryThreshold = 3; // Valeur par défaut
            }
        } else {
            this.totalQuestions = 5;
            this.victoryThreshold = 3;
        }
        
        // Initialisation des couleurs et éléments UI
        // Utiliser le gestionnaire Distance Field pour une qualité optimale
        CarlitoFontManager.initialize();
        this.font = CarlitoFontManager.getFont();
        font.getData().setScale(1.0f);
        this.layout = new GlyphLayout();
        
        // Initialiser les boutons (tailles seront définies dans updateButtonPositions)
        this.closeButton = new Rectangle(0, 0, 100, 100);
        this.infoButton = new Rectangle(0, 0, 100, 100);
        this.infoPanelColor = new Color(0.3f, 0.3f, 0.3f, 0.8f);
        this.showInfoPanel = false;
        
        // Couleur de fond par défaut
        this.backgroundColor = new Color(0.1f, 0.1f, 0.2f, 1);
        
        // Initialiser les variables de jeu
        this.allQuestions = new Array<>();
        this.currentQuestions = new Array<>();
        this.currentQuestionIndex = 0;
        this.correctAnswers = 0;
        this.gameFinished = false;
        this.gameWon = false;
        
        // Charger les sons
        try {
            this.winSound = Gdx.audio.newSound(Gdx.files.internal("audio/win.mp3"));
            this.wrongSound = Gdx.audio.newSound(Gdx.files.internal("audio/wrong.wav"));
        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerGameScreen", "Erreur lors du chargement des sons: " + e.getMessage());
            this.winSound = null;
            this.wrongSound = null;
        }
        
        Gdx.app.log("QuestionAnswerGameScreen", "Création du jeu QNA pour le jour " + dayId);
        String logFile = questionsFile.isEmpty() ? "(aucun)" : questionsFile;
        Gdx.app.log("QuestionAnswerGameScreen", "Fichier de questions: " + logFile);
        Gdx.app.log("QuestionAnswerGameScreen", "Nombre de questions: " + totalQuestions);
        Gdx.app.log("QuestionAnswerGameScreen", "Seuil de victoire: " + victoryThreshold);

        // Créer une texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        
        // Charger les textures des boutons
        loadButtonTextures();
        
        // Créer l'input processor
        createInputProcessor();
        
        Gdx.app.log("QuestionAnswerGameScreen", "Constructeur QuestionAnswerGameScreen terminé avec succès");
    }
    
    /**
     * Charge les textures des boutons
     */
    private void loadButtonTextures() {
        // Charger la texture du bouton close
        try {
            this.closeButtonTexture = new Texture(Gdx.files.internal("images/ui/close.png"));
        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerGameScreen", "Erreur lors du chargement du bouton close: " + e.getMessage());
            this.closeButtonTexture = null;
        }
        
        // Charger la texture du bouton info
        try {
            this.infoButtonTexture = new Texture(Gdx.files.internal("images/ui/help.png"));
        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerGameScreen", "Erreur lors du chargement du bouton info: " + e.getMessage());
            this.infoButtonTexture = null;
        }
    }
    
    @Override
    protected Theme loadTheme(int day) {
        return theme; // Le thème est déjà fourni au constructeur
    }
   
    @Override
    protected void onScreenActivated() {
        super.onScreenActivated();
        
        // Initialiser le système de debug après que l'écran soit complètement initialisé
        initializeDebugManager();
        
        // Créer un InputMultiplexer pour gérer à la fois l'input processor et le stage
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(inputStage);
        multiplexer.addProcessor(inputProcessor);
        
        // Activer le multiplexer quand l'écran devient actif
        Gdx.input.setInputProcessor(multiplexer);
    }
    
    @Override
    protected void onScreenDeactivated() {
        super.onScreenDeactivated();
        // Désactiver l'input processor quand l'écran devient inactif
        Gdx.input.setInputProcessor(null);
    }
    
    @Override
    protected void initializeGame() {
        // Positionner les boutons
        updateButtonPositions();
        
        // Charger les questions
        loadQuestions();
        
        // Sélectionner les questions au hasard
        selectRandomQuestions();
        
        // Initialiser l'interface de saisie
        initializeInputInterface();
        
        Gdx.app.log("QuestionAnswerGameScreen", "Jeu QNA initialisé");
    }
    
    /**
     * Calcule la position des boutons d'information et close en haut à droite du viewport (comme MastermindGameScreen)
     */
    private void updateButtonPositions() {
        // Obtenir les dimensions du viewport
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        // Taille de base des boutons
        float baseButtonSize = 100f;
        
        // Marge depuis les bords du viewport
        float marginFromEdge = 30f;
        float spacingBetweenButtons = 30f;
        
        // Position du bouton close (en haut à droite)
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
    
    /**
     * Charge les questions depuis le fichier JSON
     */
    private void loadQuestions() {
        allQuestions.clear();
        
        if (questionsFile.isEmpty()) {
            Gdx.app.log("QuestionAnswerGameScreen", "Aucun fichier de questions spécifié");
                    return;
        }
        
        try {
            String filePath = "quizz/" + questionsFile;
            JsonReader jsonReader = new JsonReader();
            JsonValue jsonData = jsonReader.parse(Gdx.files.internal(filePath));
            
            JsonValue questionsArray = jsonData.get("questions");
            if (questionsArray != null) {
                for (JsonValue questionJson : questionsArray) {
                    int id = questionJson.getInt("id");
                    String question = questionJson.getString("question");
                    String answer = questionJson.getString("answer");
                    
                    // Charger les alternatives
                    JsonValue alternativesJson = questionJson.get("alternatives");
                    String[] alternatives = new String[alternativesJson.size];
                    for (int i = 0; i < alternativesJson.size; i++) {
                        alternatives[i] = alternativesJson.get(i).asString();
                    }
                    
                    QuestionData questionData = new QuestionData(id, question, answer, alternatives);
                    allQuestions.add(questionData);
                }
                
                Gdx.app.log("QuestionAnswerGameScreen", "Chargé " + allQuestions.size + " questions depuis " + filePath);
            }
        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerGameScreen", "Erreur lors du chargement des questions: " + e.getMessage());
        }
    }
    
    /**
     * Sélectionne des questions au hasard pour cette session
     */
    private void selectRandomQuestions() {
        currentQuestions.clear();
        
        if (allQuestions.size == 0) {
            Gdx.app.log("QuestionAnswerGameScreen", "Aucune question disponible");
            return;
        }
        
        // Créer une copie de toutes les questions
        Array<QuestionData> availableQuestions = new Array<>(allQuestions);
        
        // Sélectionner le nombre de questions demandé
        int questionsToSelect = Math.min(totalQuestions, availableQuestions.size);
        
        for (int i = 0; i < questionsToSelect; i++) {
            int randomIndex = (int) (Math.random() * availableQuestions.size);
            QuestionData selectedQuestion = availableQuestions.get(randomIndex);
            currentQuestions.add(selectedQuestion);
            availableQuestions.removeIndex(randomIndex);
        }
        
        currentQuestionIndex = 0;
        correctAnswers = 0;
        gameFinished = false;
        gameWon = false;
        
        Gdx.app.log("QuestionAnswerGameScreen", "Sélectionné " + currentQuestions.size + " questions au hasard");
    }
    
    /**
     * Initialise l'interface de saisie avec BottomInputBar
     */
    private void initializeInputInterface() {
        // Créer un skin basique pour l'input
        inputSkin = new com.badlogic.gdx.scenes.scene2d.ui.Skin();
        
        // Créer le stage pour l'interface avec son propre SpriteBatch
        inputStage = new com.badlogic.gdx.scenes.scene2d.Stage(viewport);
        
        // Créer la table racine
        rootTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        rootTable.setFillParent(true);
        inputStage.addActor(rootTable);
        
        // Créer la barre d'input
        inputBar = new BottomInputBar(inputSkin);
        inputBar.setListener(new BottomInputBar.Listener() {
    @Override
            public void onSubmit(String text) {
                handleAnswerSubmission(text);
            }
            
            @Override
            public void onFocusChanged(boolean focused) {
                Gdx.app.log("QuestionAnswerGameScreen", "Focus changed: " + focused);
            }
        });
        
        // Ajouter la barre d'input en bas de l'écran
        rootTable.bottom().left().right();
        rootTable.add(inputBar).expandX().fillX().pad(20);
        
        // Afficher la première question
        showCurrentQuestion();
    }
    
    /**
     * Affiche la question courante dans la barre d'input
     */
    private void showCurrentQuestion() {
        if (currentQuestions.size == 0 || currentQuestionIndex >= currentQuestions.size) {
            return;
        }
        
        QuestionData currentQuestion = currentQuestions.get(currentQuestionIndex);
        String placeholder = "Question " + (currentQuestionIndex + 1) + "/" + currentQuestions.size + ": " + currentQuestion.question;
        inputBar.setPlaceholderText(placeholder);
        inputBar.clear();
        
        Gdx.app.log("QuestionAnswerGameScreen", "Question " + (currentQuestionIndex + 1) + ": " + currentQuestion.question);
        Gdx.app.log("QuestionAnswerGameScreen", "Réponse correcte: " + currentQuestion.answer);
    }
    
    /**
     * Gère la soumission d'une réponse
     */
    private void handleAnswerSubmission(String answer) {
        if (gameFinished || currentQuestions.size == 0 || currentQuestionIndex >= currentQuestions.size) {
            return;
        }
        
        QuestionData currentQuestion = currentQuestions.get(currentQuestionIndex);
        
        // Vérifier la réponse avec AnswerMatcher
        // Créer un tableau avec la réponse correcte et les alternatives
        String[] allAnswers = new String[currentQuestion.alternatives.length + 1];
        allAnswers[0] = currentQuestion.answer;
        System.arraycopy(currentQuestion.alternatives, 0, allAnswers, 1, currentQuestion.alternatives.length);
        
        boolean isCorrect = AnswerMatcher.matchesAny(answer, allAnswers);
        
        if (isCorrect) {
            correctAnswers++;
            Gdx.app.log("QuestionAnswerGameScreen", "Réponse correcte! (" + correctAnswers + "/" + currentQuestions.size + ")");
            
            // Son de victoire pour une bonne réponse
            if (winSound != null) {
                winSound.play(0.5f);
            }
        } else {
            Gdx.app.log("QuestionAnswerGameScreen", "Réponse incorrecte. Réponse attendue: " + currentQuestion.answer);
            
            // Son d'erreur
            if (wrongSound != null) {
                wrongSound.play(0.5f);
            }
        }
        
        // Passer à la question suivante
        currentQuestionIndex++;
        
        if (currentQuestionIndex >= currentQuestions.size) {
            // Fin du jeu
            finishGame();
        } else {
            // Afficher la question suivante
            showCurrentQuestion();
        }
    }
    
    /**
     * Termine le jeu et vérifie la victoire
     */
    private void finishGame() {
        gameFinished = true;
        
        Gdx.app.log("QuestionAnswerGameScreen", "Jeu terminé! Réponses correctes: " + correctAnswers + "/" + currentQuestions.size);
        Gdx.app.log("QuestionAnswerGameScreen", "Seuil requis: " + victoryThreshold);
        
        if (correctAnswers >= victoryThreshold) {
            gameWon = true;
            Gdx.app.log("QuestionAnswerGameScreen", "VICTOIRE! Seuil atteint!");
            
            // Jouer la musique de victoire
            if (winSound != null) {
                winSound.play(1.0f);
            }
            
            // Déverrouiller la victoire
            if (game instanceof AdventCalendarGame) {
                AdventCalendarGame adventGame = (AdventCalendarGame) game;
                adventGame.setScore(dayId, 100);
                adventGame.setVisited(dayId, true);
            }
            
            // Retourner au menu après un délai
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    returnToMainMenu();
                }
            }, 2.0f);
        } else {
            Gdx.app.log("QuestionAnswerGameScreen", "Échec! Seuil non atteint.");
            
            // Retourner au menu après un délai
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    returnToMainMenu();
                }
            }, 2.0f);
        }
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le debug manager
        if (debugManager != null) {
            debugManager.update(delta);
        }
    }
    
    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Dessiner l'interface de jeu
        drawGameInterface();
        
        // Dessiner le panneau d'information par-dessus tout le reste s'il est visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
        
        // Dessiner l'interface de debug si en mode debug
        if (debugManager != null) {
            debugManager.drawDebugInfo(batch, viewport.getWorldHeight());
        }
        
        // Dessiner l'interface de saisie (le Stage gère son propre batch)
        if (inputStage != null) {
            inputStage.act();
            inputStage.draw();
        }
    }
    
    /**
     * Dessine l'interface de jeu (boutons, etc.)
     */
    private void drawGameInterface() {
        // Dessiner les boutons avec leurs textures
        drawCloseButton();
        drawInfoButton();
    }
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
        batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);
        }
    }
    
    private void drawCloseButton() {
        if (closeButtonTexture != null) {
        batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(closeButtonTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);
        }
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
        
        // Dessiner les informations du thème avec shader Distance Field
        float textMargin = 20;
        float titleY = panelY + panelHeight - textMargin;
        float artistY = titleY - 40;
        float yearY = artistY - 40;
        float descriptionY = yearY - 80;

        font.setColor(0.2f, 0.3f, 0.8f, 1);
        
        // Titre
        layout.setText(font, theme.getTitle(), Color.WHITE, panelWidth - 2 * textMargin, com.badlogic.gdx.utils.Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(titleY));

        // Artiste
        font.setColor(0.1f, 0.1f, 0.2f, 1);
        layout.setText(font, theme.getArtist(), Color.WHITE, panelWidth - 2 * textMargin, com.badlogic.gdx.utils.Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(artistY));

        // Année
        layout.setText(font, String.valueOf(theme.getYear()), Color.WHITE, panelWidth - 2 * textMargin, com.badlogic.gdx.utils.Align.center, false);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(yearY));

        // Description
        layout.setText(font, theme.getDescription(), Color.WHITE, panelWidth - 2 * textMargin, com.badlogic.gdx.utils.Align.center, true);
        CarlitoFontManager.drawText(batch, layout, Math.round(panelX + textMargin), Math.round(descriptionY));
        
        // Indicateur de fermeture
        font.setColor(0.5f, 0.5f, 0.6f, 1);
        String closeHint = "Tapez pour fermer";
        layout.setText(font, closeHint);
        CarlitoFontManager.drawText(batch, layout, 
            panelX + panelWidth - layout.width - 10,
            panelY + 15);
    }

    /**
     * Crée l'input processor pour gérer les clics et raccourcis clavier
     */
    private void createInputProcessor() {
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
                // Gestion du debug
                if (debugManager != null && debugManager.handleKeyDown(keycode)) {
                    return true;
                }
                
                // Alt+R : Résoudre automatiquement le jeu (mode test uniquement)
                if (keycode == Input.Keys.R && isTestMode && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    solveGame();
                    return true;
                }
                
                // Alt+N : Déclencher la phase de victoire (mode test uniquement)
                if (keycode == Input.Keys.N && isTestMode && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    triggerVictoryPhase();
                    return true;
                }
                
                return false;
            }
        };
    }
    
    /**
     * Gère les clics de souris/tactiles
     */
    private void handleClick(float worldX, float worldY) {
        // Si le panneau d'info est visible, n'importe quel clic le ferme
        if (showInfoPanel) {
            showInfoPanel = false;
            return;
        }
        
        if (closeButton.contains(worldX, worldY)) {
            returnToMainMenu();
        } else if (infoButton.contains(worldX, worldY)) {
            showInfoPanel = true;
        }
    }
    
    @Override
    protected void handleInput() {
        // La gestion des entrées est déléguée à l'InputProcessor créé dans createInputProcessor()
        // Cette méthode peut rester vide car toutes les interactions sont gérées via les clics et raccourcis clavier
    }
    
    /**
     * Résout automatiquement le jeu (mode test)
     */
    private void solveGame() {
        Gdx.app.log("QuestionAnswerGameScreen", "Résolution automatique du jeu QNA (mode test)");
        
        if (game instanceof AdventCalendarGame) {
            AdventCalendarGame adventGame = (AdventCalendarGame) game;
            adventGame.setScore(dayId, 100);
            adventGame.setVisited(dayId, true);
        }
        
        // Retourner au menu immédiatement
        returnToMainMenu();
    }
    
    /**
     * Déclenche la phase de victoire (mode test - touche N)
     */
    private void triggerVictoryPhase() {
        Gdx.app.log("QuestionAnswerGameScreen", "Déclenchement de la phase de victoire (mode test - touche N)");
        
        // Utiliser la méthode solveGame() existante
        solveGame();
    }
    
    /**
     * Initialise le système de debug
     */
    private void initializeDebugManager() {
        debugManager = new QuestionAnswerDebugManager(font);
        debugManager.setCurrentGameReference(gameReference);
        debugManager.setChangeCallback(new QuestionAnswerDebugManager.DebugChangeCallback() {
            public void onDebugParameterChanged() {
                // Mettre à jour tous les paramètres
                questionsFile = debugManager.getCurrentQuestionsFile();
                totalQuestions = debugManager.getTotalQuestions();
                victoryThreshold = debugManager.getVictoryThreshold();
                
                String logFile = questionsFile.isEmpty() ? "(aucun)" : questionsFile;
                Gdx.app.log("QuestionAnswerGameScreen", "Paramètres changés:");
                Gdx.app.log("QuestionAnswerGameScreen", "  Fichier: " + logFile);
                Gdx.app.log("QuestionAnswerGameScreen", "  Questions: " + totalQuestions);
                Gdx.app.log("QuestionAnswerGameScreen", "  Seuil: " + victoryThreshold);
                
                // Recharger les questions si le fichier a changé
                loadQuestions();
                selectRandomQuestions();
                showCurrentQuestion();
            }
            
            @Override
            public void onDebugSettingsSaved() {
                Gdx.app.log("QuestionAnswerGameScreen", "Paramètres sauvegardés dans games.json");
            }
        });
        
        // Définir la référence du jeu pour la sauvegarde
        debugManager.setCurrentGameReference(gameReference);
        
        // Initialiser le debug avec les paramètres du jeu
        debugManager.initializeFromGameParameters(gameParameters);
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        
        // Mettre à jour les positions des boutons
        updateButtonPositions();
        
        // Mettre à jour le stage de l'interface de saisie
        if (inputStage != null) {
            inputStage.getViewport().update(width, height, true);
        }
        
        Gdx.app.log("QuestionAnswerGameScreen", "Redimensionnement: " + width + "x" + height);
        Gdx.app.log("QuestionAnswerGameScreen", "Viewport: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
    }
    
    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        whiteTexture.dispose();
        if (closeButtonTexture != null) {
            closeButtonTexture.dispose();
        }
        if (infoButtonTexture != null) {
            infoButtonTexture.dispose();
        }
        if (winSound != null) {
            winSound.dispose();
        }
        if (wrongSound != null) {
            wrongSound.dispose();
        }
        if (inputStage != null) {
            inputStage.dispose();
        }
        if (inputSkin != null) {
            inputSkin.dispose();
        }
    }
} 