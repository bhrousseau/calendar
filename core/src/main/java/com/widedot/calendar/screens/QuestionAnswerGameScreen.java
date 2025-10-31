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
import com.widedot.calendar.animation.QnaAnimationManager;

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
    private boolean showInfoPanel;
    private final Texture whiteTexture;
    private Texture closeButtonTexture;
    private Texture infoButtonTexture;
    private Texture helpImageTexture;
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
    private boolean waitingForAnimation;            // Flag pour attendre la fin de l'animation avant question suivante
    private boolean pendingQuestionTransition;      // Flag pour passage à la question suivante en attente
    
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
    
    // Animation system
    private QnaAnimationManager animationManager;
    
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
        this.showInfoPanel = false;
        
        // Couleur de fond noire pour le jeu QNA
        this.backgroundColor = new Color(0f, 0f, 0f, 1);
        
        // Initialiser les variables de jeu
        this.allQuestions = new Array<>();
        this.currentQuestions = new Array<>();
        this.currentQuestionIndex = 0;
        this.correctAnswers = 0;
        this.gameFinished = false;
        this.gameWon = false;
        this.waitingForAnimation = false;
        this.pendingQuestionTransition = false;
        
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
        
        // Charger l'image d'aide
        loadHelpImage();
        
        // Créer l'input processor
        createInputProcessor();
        
        // Initialiser le système d'animation
        initializeAnimationManager();
        
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
    
    /**
     * Charge l'image d'aide
     */
    private void loadHelpImage() {
        try {
            this.helpImageTexture = new Texture(Gdx.files.internal("images/games/qna/help_qna.png"));
            // Appliquer un filtrage Linear pour l'antialiasing
            helpImageTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.log("QuestionAnswerGameScreen", "Image d'aide chargée: help_qna.png");
        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerGameScreen", "Erreur lors du chargement de l'image d'aide: " + e.getMessage());
            this.helpImageTexture = null;
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
        
        // Démarrer l'animation d'initialisation
        if (animationManager != null) {
            animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.GAME_START);
        }
        
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
                    String[] alternatives;
                    if (alternativesJson != null && alternativesJson.isArray()) {
                        alternatives = new String[alternativesJson.size];
                        for (int i = 0; i < alternativesJson.size; i++) {
                            alternatives[i] = alternativesJson.get(i).asString();
                        }
                    } else {
                        alternatives = new String[0];
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
        inputBar.setPlaceholderText(currentQuestion.question);
        inputBar.clear();
        
        Gdx.app.log("QuestionAnswerGameScreen", "Question: " + currentQuestion.question);
        Gdx.app.log("QuestionAnswerGameScreen", "Réponse correcte: " + currentQuestion.answer);
    }
    
    /**
     * Gère la soumission d'une réponse
     */
    private void handleAnswerSubmission(String answer) {
        if (gameFinished || currentQuestions.size == 0 || currentQuestionIndex >= currentQuestions.size) {
            return;
        }
        
        // Ne pas traiter de réponse si une animation est en cours
        if (waitingForAnimation) {
            return;
        }
        
        QuestionData currentQuestion = currentQuestions.get(currentQuestionIndex);
        
        // Vérifier la réponse avec AnswerMatcher
        // Créer un tableau avec la réponse correcte et les alternatives
        // Utiliser AnswerMatcher pour vérifier à la fois answer et toutes les alternatives
        String[] alternatives = currentQuestion.alternatives != null ? currentQuestion.alternatives : new String[0];
        String[] allAnswers = new String[alternatives.length + 1];
        allAnswers[0] = currentQuestion.answer;
        if (alternatives.length > 0) {
            System.arraycopy(alternatives, 0, allAnswers, 1, alternatives.length);
        }
        
        boolean isCorrect = AnswerMatcher.matchesAny(answer, allAnswers);
        
        if (isCorrect) {
            correctAnswers++;
            Gdx.app.log("QuestionAnswerGameScreen", "Réponse correcte! (" + correctAnswers + "/" + currentQuestions.size + ")");
            
            // Masquer l'input immédiatement après une bonne réponse
            hideInput();
            
            // Vérifier si c'est le dernier slot (dernière bonne réponse requise)
            boolean isLastSlot = (correctAnswers >= victoryThreshold);
            
            // Déclencher l'animation de bonne réponse
            if (animationManager != null) {
                animationManager.updateGameStats(correctAnswers, currentQuestionIndex);
                animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.CORRECT_ANSWER);
                
                // Si c'est le dernier slot, l'animation finale sera déclenchée automatiquement
                // Sinon, on attendra la fin de l'animation pour passer à la question suivante
                if (!isLastSlot) {
                    waitingForAnimation = true;
                    pendingQuestionTransition = true;
                }
            }
            
            // Son de victoire pour une bonne réponse
            if (winSound != null) {
                winSound.play(0.5f);
            }
            
            // Ne pas passer à la question suivante immédiatement
            // Attendre la fin de l'animation (géré dans le callback)
        } else {
            Gdx.app.log("QuestionAnswerGameScreen", "Réponse incorrecte. Réponse attendue: " + currentQuestion.answer);
            
            // Masquer l'input immédiatement après une mauvaise réponse
            hideInput();
            
            // Vérifier si c'est la dernière bille (il n'y aura plus de billes après celle-ci)
            // On compte les billes restantes AVANT de consommer la bille actuelle
            boolean isLastBall = false;
            if (animationManager != null) {
                // Compter les billes restantes dans le tube (y compris celle qui va être consommée)
                int remainingBalls = animationManager.getRemainingBallsCount();
                // Si il n'y a qu'une bille (celle qui va être consommée), c'est la dernière
                isLastBall = (remainingBalls == 1);
            }
            boolean notEnoughCorrectAnswers = (correctAnswers < victoryThreshold);
            
            if (isLastBall && notEnoughCorrectAnswers) {
                // C'est la dernière bille et on n'a pas résolu les slots : terminer le jeu après l'animation
                Gdx.app.log("QuestionAnswerGameScreen", "Dernière bille sans avoir résolu les slots - fin du jeu après animation");
                waitingForAnimation = true;
                pendingQuestionTransition = false; // Ne pas passer à la question suivante
                
                // Déclencher l'animation de mauvaise réponse (c'est la dernière bille)
                if (animationManager != null) {
                    animationManager.updateGameStats(correctAnswers, currentQuestionIndex);
                    animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.WRONG_ANSWER);
                }
                
                // Son d'erreur
                if (wrongSound != null) {
                    wrongSound.play(0.5f);
                }
            } else {
                // Déclencher l'animation de mauvaise réponse
                if (animationManager != null) {
                    animationManager.updateGameStats(correctAnswers, currentQuestionIndex);
                    animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.WRONG_ANSWER);
                    
                    // Attendre la fin de l'animation pour passer à la question suivante
                    waitingForAnimation = true;
                    pendingQuestionTransition = true;
                }
                
                // Son d'erreur
                if (wrongSound != null) {
                    wrongSound.play(0.5f);
                }
            }
            
            // Ne pas passer à la question suivante immédiatement
            // Attendre la fin de l'animation (géré dans le callback)
        }
    }
    
    /**
     * Passe à la question suivante (appelé après la fin de l'animation)
     * Cette méthode est appelée uniquement si on a encore des billes et qu'une transition est prévue
     */
    private void proceedToNextQuestion() {
        waitingForAnimation = false;
        pendingQuestionTransition = false;
        
        // Passer à la question suivante
        currentQuestionIndex++;
        
        if (currentQuestionIndex >= currentQuestions.size) {
            // Fin du jeu (plus de questions)
            finishGame();
        } else {
            // Afficher la question suivante
            showCurrentQuestion();
            
            // Réafficher l'input pour la nouvelle question
            showInput();
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
            
            // Déclencher l'animation de victoire
            if (animationManager != null) {
                animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.GAME_WON);
            }
            
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
            
            // Ne pas retourner automatiquement au menu : le retour sera géré par le clic pendant la phase finale
            // L'animation finale se charge d'afficher la peinture et le clic retourne au calendrier
        } else {
            Gdx.app.log("QuestionAnswerGameScreen", "Échec! Seuil non atteint.");
            
            // Déclencher l'animation de défaite
            if (animationManager != null) {
                animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.GAME_LOST);
            }
            
            // Retourner au menu après l'animation de défaite
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    returnToMainMenu();
                }
            }, 1.0f); // Délai pour permettre l'animation de défaite
        }
    }
    
    @Override
    protected void updateGame(float delta) {
        // Mettre à jour le debug manager
        if (debugManager != null) {
            debugManager.update(delta);
        }
        
        // Mettre à jour le système d'animation
        if (animationManager != null) {
            animationManager.update(delta);
        }
    }
    
    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Dessiner les animations
        if (animationManager != null) {
            animationManager.draw(batch, viewport.getWorldHeight());
        }
        
        // Dessiner les boutons par-dessus tout le reste
        drawCloseButton();
        drawInfoButton();
        
        // Dessiner le panneau d'information par-dessus tout le reste s'il est visible
        if (showInfoPanel) {
            drawInfoPanel();
        }
        
        // Dessiner l'interface de debug si en mode debug
        if (debugManager != null) {
            debugManager.drawDebugInfo(batch, viewport.getWorldHeight());
        }
        
        // Dessiner l'interface de saisie (le Stage gère son propre batch)
        // Masquer l'input si on est en phase finale
        if (inputStage != null) {
            boolean shouldShowInput = true;
            if (animationManager != null) {
                shouldShowInput = !animationManager.isInFinalPhase();
            }
            
            if (shouldShowInput) {
                inputStage.act();
                inputStage.draw();
            }
        }
    }
    
    
    private void drawInfoButton() {
        if (infoButtonTexture != null) {
            // Obtenir l'alpha et l'offset vertical pour l'animation finale
            float alpha = 1.0f;
            float verticalOffset = 0f;
            if (animationManager != null) {
                alpha = animationManager.getVictoryFadeAlpha();
                verticalOffset = animationManager.getVictoryVerticalOffset() * viewport.getWorldHeight();
            }
            
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(infoButtonTexture, infoButton.x, infoButton.y + verticalOffset, infoButton.width, infoButton.height);
            batch.setColor(1f, 1f, 1f, 1f); // Réinitialiser
        }
    }
    
    private void drawCloseButton() {
        if (closeButtonTexture != null) {
            // Obtenir l'alpha et l'offset vertical pour l'animation finale
            float alpha = 1.0f;
            float verticalOffset = 0f;
            if (animationManager != null) {
                alpha = animationManager.getVictoryFadeAlpha();
                verticalOffset = animationManager.getVictoryVerticalOffset() * viewport.getWorldHeight();
            }
            
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(closeButtonTexture, closeButton.x, closeButton.y + verticalOffset, closeButton.width, closeButton.height);
            batch.setColor(1f, 1f, 1f, 1f); // Réinitialiser
        }
    }
    
    /**
     * Masque l'interface de saisie
     */
    private void hideInput() {
        if (inputStage != null && rootTable != null) {
            rootTable.setVisible(false);
            Gdx.app.log("QuestionAnswerGameScreen", "Input masqué");
        }
    }
    
    /**
     * Affiche l'interface de saisie
     */
    private void showInput() {
        if (inputStage != null && rootTable != null) {
            rootTable.setVisible(true);
            Gdx.app.log("QuestionAnswerGameScreen", "Input affiché");
        }
    }
    
    /**
     * Dessine l'image d'aide en overlay (suivant les dimensions du background)
     */
    private void drawInfoPanel() {
        if (helpImageTexture == null) return;
        
        // Obtenir les dimensions du background depuis animationManager
        float bgX, bgY, bgWidth, bgHeight;
        if (animationManager != null) {
            // Utiliser les dimensions du background de l'animation manager
            // Note: animationManager a currentBgX/Y/Width/Height mais ils sont privés
            // On doit calculer les mêmes dimensions ici
            float screenWidth = DisplayConfig.WORLD_WIDTH;
            float screenHeight = viewport.getWorldHeight();
            float imageAspect = (float)helpImageTexture.getWidth() / helpImageTexture.getHeight();
            float screenAspect = screenWidth / screenHeight;
            
            if (screenAspect > imageAspect) {
                // Écran plus large : fitter en largeur
                bgWidth = screenWidth;
                bgHeight = bgWidth / imageAspect;
                bgX = 0;
                bgY = (screenHeight - bgHeight) / 2;
            } else {
                // Écran plus haut : fitter en hauteur
                bgHeight = screenHeight;
                bgWidth = bgHeight * imageAspect;
                bgX = (screenWidth - bgWidth) / 2;
                bgY = 0;
            }
        } else {
            // Fallback si animationManager n'est pas disponible
            float screenWidth = DisplayConfig.WORLD_WIDTH;
            float screenHeight = viewport.getWorldHeight();
            bgX = 0;
            bgY = 0;
            bgWidth = screenWidth;
            bgHeight = screenHeight;
        }
        
        // Dessiner l'image d'aide aux mêmes dimensions que le background
        batch.setColor(1, 1, 1, 1);
        batch.draw(helpImageTexture, bgX, bgY, bgWidth, bgHeight);
    }

    /**
     * Crée l'input processor pour gérer les clics et raccourcis clavier
     */
    private void createInputProcessor() {
        inputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Vérifier si le clic est sur la barre d'input
                boolean clickOnInput = false;
                if (inputStage != null && inputStage.getActors().size > 0) {
                    com.badlogic.gdx.math.Vector2 stageCoords = inputStage.screenToStageCoordinates(
                        new com.badlogic.gdx.math.Vector2(screenX, screenY)
                    );
                    for (com.badlogic.gdx.scenes.scene2d.Actor actor : inputStage.getActors()) {
                        if (actor instanceof com.widedot.calendar.ui.BottomInputBar) {
                            com.widedot.calendar.ui.BottomInputBar inputBar = (com.widedot.calendar.ui.BottomInputBar) actor;
                            clickOnInput = inputBar.containsPoint(stageCoords.x, stageCoords.y);
                            
                            // Si on a le focus et qu'on clique en dehors, sortir du mode saisie
                            if (inputBar.hasFocus() && !clickOnInput) {
                                inputBar.forceExitInputMode();
                            }
                            break;
                        }
                    }
                }
                
                // Traiter le clic pour le jeu (boutons, etc.)
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
                
                // Alt+I : Activer/désactiver le mode debug pour les positions
                if (keycode == Input.Keys.I && 
                    (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT))) {
                    if (animationManager != null) {
                        animationManager.toggleDebugMode();
                    }
                    return true;
                }
                
                // Échap : Sortir du mode saisie
                if (keycode == Input.Keys.ESCAPE) {
                    if (inputStage != null && inputStage.getActors().size > 0) {
                        // Chercher la BottomInputBar dans les acteurs du Stage
                        for (com.badlogic.gdx.scenes.scene2d.Actor actor : inputStage.getActors()) {
                            if (actor instanceof com.widedot.calendar.ui.BottomInputBar) {
                                com.widedot.calendar.ui.BottomInputBar inputBar = (com.widedot.calendar.ui.BottomInputBar) actor;
                                inputBar.forceExitInputMode();
                                break;
                            }
                        }
                    }
                    return true;
                }
                
                // Gestion des touches de debug du gestionnaire d'animation
                if (animationManager != null && animationManager.handleDebugKeyDown(keycode)) {
                    return true;
                }
                
                return false;
            }
            
            @Override
            public boolean keyUp(int keycode) {
                // Gestion du relâchement des touches de debug
                if (animationManager != null && animationManager.handleDebugKeyUp(keycode)) {
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
        // Si on est en phase finale (peinture visible), n'importe quel clic retourne au calendrier
        if (animationManager != null && animationManager.isInFinalPhase()) {
            returnToMainMenu();
            return;
        }
        
        // Si le panneau d'aide est visible, n'importe quel clic le ferme
        if (showInfoPanel) {
            showInfoPanel = false;
            return;
        }
        
        if (closeButton.contains(worldX, worldY)) {
            returnToMainMenu();
        } else if (infoButton.contains(worldX, worldY)) {
            showInfoPanel = true;
        }
        // La sortie du mode saisie est gérée dans touchDown de l'inputProcessor
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
        
        // Marquer le jeu comme terminé et gagné
        gameFinished = true;
        gameWon = true;
        
        // Déclencher l'animation de victoire
        if (animationManager != null) {
            animationManager.triggerEvent(QnaAnimationManager.AnimationEvent.GAME_WON);
        }
        
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
        
        // Masquer l'input (l'animation finale se chargera de l'afficher)
        hideInput();
        
        // Ne pas retourner automatiquement au menu : le retour sera géré par le clic pendant la phase finale
        // L'animation finale se charge d'afficher la peinture et le clic retourne au calendrier
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
        
        // Mettre à jour les positions des animations
        if (animationManager != null) {
            animationManager.calculateBackgroundDimensions(DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
            updateAnimationPositions();
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
        if (helpImageTexture != null) {
            helpImageTexture.dispose();
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
        if (animationManager != null) {
            animationManager.dispose();
        }
    }
    
    /**
     * Initialise le gestionnaire d'animation
     */
    private void initializeAnimationManager() {
        animationManager = new QnaAnimationManager(totalQuestions, victoryThreshold);
        
        // Définir le callback pour les événements d'animation
        animationManager.setCallback(new QnaAnimationManager.AnimationCallback() {
            @Override
            public void onAnimationComplete(QnaAnimationManager.AnimationState completedState) {
                Gdx.app.log("QuestionAnswerGameScreen", "Animation terminée: " + completedState);
                
                // Actions spécifiques selon l'état d'animation terminé
                switch (completedState) {
                    case INITIALIZING:
                        Gdx.app.log("QuestionAnswerGameScreen", "Animation d'initialisation terminée");
                        break;
                    case BALL_DISAPPEARING_CORRECT:
                    case BALL_APPEARING_IN_SLOT:
                    case SLOTS_ROTATING:
                    case TUBE_DESCENDING_CORRECT:
                        // Animation de bonne réponse terminée : passer à la question suivante
                        Gdx.app.log("QuestionAnswerGameScreen", "Animation de bonne réponse terminée");
                        proceedToNextQuestion();
                        break;
                    case BALL_DISAPPEARING_WRONG:
                    case TUBE_DESCENDING_WRONG:
                        // Animation de mauvaise réponse terminée
                        Gdx.app.log("QuestionAnswerGameScreen", "Animation de mauvaise réponse terminée");
                        
                        // Vérifier si toutes les billes sont épuisées et qu'on n'a pas atteint le seuil de victoire
                        boolean ballsExhausted = (animationManager != null && animationManager.areBallsExhausted());
                        boolean notEnoughCorrectAnswers = (correctAnswers < victoryThreshold);
                        
                        if (ballsExhausted && notEnoughCorrectAnswers) {
                            // Toutes les billes sont épuisées sans avoir résolu les slots : terminer le jeu
                            Gdx.app.log("QuestionAnswerGameScreen", "Toutes les billes épuisées, fin du jeu");
                            waitingForAnimation = false;
                            finishGame();
                        } else {
                            // Passer à la question suivante seulement si on a encore des billes
                            proceedToNextQuestion();
                        }
                        break;
                    case VICTORY_ANIMATION:
                        Gdx.app.log("QuestionAnswerGameScreen", "Animation de victoire terminée");
                        // Ne pas passer à la question suivante, on reste sur la peinture
                        break;
                    case DEFEAT_ANIMATION:
                        Gdx.app.log("QuestionAnswerGameScreen", "Animation de défaite terminée");
                        break;
                }
            }
            
            @Override
            public void onAnimationStarted(QnaAnimationManager.AnimationState startedState) {
                Gdx.app.log("QuestionAnswerGameScreen", "Animation démarrée: " + startedState);
                
                // Masquer l'input au début de l'animation finale
                if (startedState == QnaAnimationManager.AnimationState.VICTORY_ANIMATION) {
                    hideInput();
                }
            }
        });
        
        // Charger les textures d'animation
        animationManager.loadTextures();
        
        // Charger les sons d'animation
        animationManager.loadSounds();
        
        // Charger la peinture depuis le thème
        if (theme != null && theme.getFullImagePath() != null) {
            animationManager.loadPaintingTexture(theme.getFullImagePath());
        }
        
        // Calculer les dimensions du background
        animationManager.calculateBackgroundDimensions(DisplayConfig.WORLD_WIDTH, viewport.getWorldHeight());
        
        // Définir les positions des éléments d'animation
        updateAnimationPositions();
        
        Gdx.app.log("QuestionAnswerGameScreen", "Gestionnaire d'animation initialisé");
    }
    
    /**
     * Met à jour les positions des éléments d'animation
     */
    private void updateAnimationPositions() {
        if (animationManager == null) return;
        
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        // Calculer les positions des éléments d'animation
        // Réservoir : côté gauche de l'écran
        float reservoirWidth = 200f;
        float reservoirHeight = 300f;
        float reservoirX = 50f;
        float reservoirY = (viewportHeight - reservoirHeight) / 2;
        
        // Roue : centre de l'écran
        float wheelSize = 150f;
        float wheelX = (viewportWidth - wheelSize) / 2;
        float wheelY = (viewportHeight - wheelSize) / 2;
        
        // Zone de succès : côté droit de l'écran
        float successAreaWidth = 200f;
        float successAreaHeight = 300f;
        float successAreaX = viewportWidth - successAreaWidth - 50f;
        float successAreaY = (viewportHeight - successAreaHeight) / 2;
        
        // Définir les positions dans le gestionnaire d'animation
        animationManager.setPositions(
            reservoirX, reservoirY, reservoirWidth, reservoirHeight,
            wheelX, wheelY, wheelSize, wheelSize,
            successAreaX, successAreaY, successAreaWidth, successAreaHeight
        );
        
        // Les positions relatives des assets d'initialisation sont déjà définies dans le constructeur
        // Elles seront automatiquement recalculées lors du resize grâce au système de positions relatives
        
        Gdx.app.log("QuestionAnswerGameScreen", "Positions d'animation mises à jour");
    }
} 