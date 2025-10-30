package com.widedot.calendar.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.utils.CarlitoFontManager;
import com.widedot.calendar.utils.GwtCompatibleFormatter;

/**
 * Gestionnaire d'animations pour le jeu QNA (Questions et Réponses)
 * 
 * Système d'événements/états pour gérer les animations :
 * - Initialisation : remplissage du réservoir de billes
 * - Bonne réponse : déplacement de bille + rotation de roue
 * - Mauvaise réponse : disparition de bille
 * - Victoire : animation finale de succès
 * - Défaite : animation finale d'échec
 */
public class QnaAnimationManager implements Disposable {
    
    /**
     * États possibles de l'animation
     */
    public enum AnimationState {
        IDLE,                       // État initial, aucune animation
        INITIALIZING,              // Animation d'initialisation (remplissage tube)
        
        // Mauvaise réponse
        BALL_DISAPPEARING_WRONG,   // Zoom out + fade + son wrong
        TUBE_DESCENDING_WRONG,     // Descente des billes
        
        // Bonne réponse (séquence)
        BALL_DISAPPEARING_CORRECT, // Zoom out + fade + son win
        WAITING_BEFORE_SLOT,       // Attente 1s
        BALL_APPEARING_IN_SLOT,    // Zoom in + fade in dans slot
        SLOTS_ROTATING,            // Rotation slots + wheel + son sliding
        TUBE_DESCENDING_CORRECT,   // Descente des billes + son close
        
        VICTORY_ANIMATION,         // Animation finale de victoire
        DEFEAT_ANIMATION,          // Animation finale de défaite
        COMPLETED                  // Animation terminée
    }
    
    /**
     * Événements déclencheurs d'animations
     */
    public enum AnimationEvent {
        GAME_START,             // Début du jeu
        CORRECT_ANSWER,         // Bonne réponse
        WRONG_ANSWER,           // Mauvaise réponse
        GAME_WON,               // Jeu gagné
        GAME_LOST,              // Jeu perdu
        RESET                   // Reset du jeu
    }
    
    /**
     * États possibles d'une bille
     */
    private enum BallState {
        IN_TUBE,                    // Dans le tube (position fixe)
        DISAPPEARING_FROM_TUBE,     // Zoom out + fade out du tube
        WAITING,                    // Attente (invisible)
        APPEARING_IN_SLOT,          // Zoom in + fade in dans le slot
        IN_SLOT,                    // Dans le slot (fixe)
        ROTATING_WITH_SLOT          // Tourne avec le slot
    }
    
    /**
     * Interface pour les callbacks d'animation
     */
    public interface AnimationCallback {
        void onAnimationComplete(AnimationState completedState);
        void onAnimationStarted(AnimationState startedState);
    }
    
    // État actuel
    private AnimationState currentState = AnimationState.IDLE;
    private AnimationCallback callback;
    
    // Configuration du jeu
    private int totalQuestions;
    private int victoryThreshold;
    private int currentCorrectAnswers;
    private int currentQuestionIndex;
    
    // Éléments graphiques animés
    private Array<AnimatedBall> balls;
    private Array<Slot> slots;
    private AnimatedWheel wheel;
    private AnimatedReservoir reservoir;
    
    // Textures
    private Texture ballTexture;
    private Texture slotBackTexture;
    private Texture slotFrontTexture;
    private Array<Texture> ballTextures;
    private Array<Texture> wheelTextures;
    private Array<Texture> reservoirTextures;
    
    // Sons
    private Sound winSound;
    private Sound wrongSound;
    private Sound slidingSound;
    private Sound closeSound;
    
    // Assets d'initialisation (ordre d'affichage : arrière vers avant)
    private Texture backgroundTexture;
    private Texture doorTexture;
    private Texture wheelOuterTexture;
    private Texture wheelCenterTexture;
    private Texture tubeFrontTexture;
    
    // Positions des assets d'initialisation (relatives au background)
    private Vector2 backgroundPosition;
    private Vector2 doorPosition;
    private Vector2 wheelOuterPosition;
    private Vector2 wheelCenterPosition;
    private Vector2 tubeFrontPosition;
    
    // Positions relatives au background (pour le repère constant)
    private Vector2 doorRelativePosition;
    private Vector2 wheelOuterRelativePosition;
    private Vector2 wheelCenterRelativePosition;
    private Vector2 tubeFrontRelativePosition;
    
    // Variables pour le background (comme SlidingPuzzle)
    private float currentBgX, currentBgY, currentBgWidth, currentBgHeight;
    private float currentScaleX, currentScaleY;
    
    // Slots (emplacements finaux pour billes)
    private float slotsRadius;                     // Rayon du cercle (relatif au background)
    private float slotsStartAngle;                 // Angle de départ (0° = droite)
    private float slotsCurrentRotation;            // Rotation actuelle (pour animation)
    
    // Tube de billes (colonne verticale)
    private float tubeColumnBaseX, tubeColumnBaseY; // Position bille du bas (relatif)
    private float ballHeight;                       // Hauteur d'une bille (relatif)
    private float tubeCurrentOffset;                // Offset pour descente animée
    
    // État d'animation
    private float stateTimer;                       // Timer pour animations chronométrées
    private int nextSlotIndex;                      // Prochain slot à remplir
    
    // Mode debug
    private boolean debugMode = false;
    private BitmapFont debugFont;
    private int selectedAsset = 0; // 0=door, 1=wheelOuter, 2=wheelCenter, 3=tubeFront, 4=slotsRadius, 5=tubeBase, 6=ballHeight
    private String[] assetNames = {"Door", "Wheel Outer", "Wheel Center", "Tube Front", "Slots Radius", "Tube Base", "Ball Height"};
    private float moveStep = 1f; // Pas de déplacement de 1px
    private float fastMoveStep = 10f; // Pas de déplacement rapide de 10px
    private float keyRepeatTimer = 0f;
    private float keyRepeatDelay = 0.1f; // Délai initial avant répétition
    private float keyRepeatInterval = 0.05f; // Intervalle de répétition
    private int lastPressedKey = -1;
    
    // Positions et dimensions
    private float reservoirX, reservoirY, reservoirWidth, reservoirHeight;
    private float wheelX, wheelY, wheelWidth, wheelHeight;
    private float successAreaX, successAreaY, successAreaWidth, successAreaHeight;
    
    // Configuration des animations
    private float ballMoveDuration = 1.0f;
    private float ballDisappearDuration = 0.5f;
    private float wheelSpinDuration = 1.5f;
    private float reservoirFillDuration = 2.0f;
    private float victoryAnimationDuration = 3.0f;
    private float defeatAnimationDuration = 2.0f;
    
    /**
     * Constructeur
     */
    public QnaAnimationManager(int totalQuestions, int victoryThreshold) {
        this.totalQuestions = totalQuestions;
        this.victoryThreshold = victoryThreshold;
        this.currentCorrectAnswers = 0;
        this.currentQuestionIndex = 0;
        
        // Initialiser les collections
        this.balls = new Array<>();
        this.slots = new Array<>();
        this.ballTextures = new Array<>();
        this.wheelTextures = new Array<>();
        this.reservoirTextures = new Array<>();
        
        // Initialiser les slots
        for (int i = 0; i < victoryThreshold; i++) {
            slots.add(new Slot(i));
        }
        
        // Initialiser les positions des assets d'initialisation
        this.backgroundPosition = new Vector2(0, 0);
        this.doorPosition = new Vector2(0, 0);
        this.wheelOuterPosition = new Vector2(0, 0);
        this.wheelCenterPosition = new Vector2(0, 0);
        this.tubeFrontPosition = new Vector2(0, 0);
        
        // Initialiser les positions relatives au background (pourcentage de la taille du background)
        this.doorRelativePosition = new Vector2(0.045f, 0.154f); // Position relative au background
        this.wheelOuterRelativePosition = new Vector2(0.292f, 0.164f);
        this.wheelCenterRelativePosition = new Vector2(0.403f, 0.352f);
        this.tubeFrontRelativePosition = new Vector2(0.766f, 0.472f);
        
        // Initialiser les paramètres des slots et du tube (valeurs par défaut)
        this.slotsRadius = 0.182f;
        this.slotsStartAngle = 0f;
        this.slotsCurrentRotation = 0f;
        this.tubeColumnBaseX = 0.785f;
        this.tubeColumnBaseY = 0.512f;
        this.ballHeight = 0.044f;
        this.tubeCurrentOffset = 0f;
        this.stateTimer = 0f;
        this.nextSlotIndex = 0;
        
        // Initialiser les éléments animés
        this.wheel = new AnimatedWheel();
        this.reservoir = new AnimatedReservoir();
        
        // Initialiser la police de debug
        this.debugFont = CarlitoFontManager.getFont();
        if (debugFont != null) {
            debugFont.getData().setScale(0.8f);
        }
        
        Gdx.app.log("QnaAnimationManager", "Initialisé avec " + totalQuestions + " questions, seuil: " + victoryThreshold);
    }
    
    /**
     * Définit le callback pour les événements d'animation
     */
    public void setCallback(AnimationCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Déclenche un événement d'animation
     */
    public void triggerEvent(AnimationEvent event) {
        Gdx.app.log("QnaAnimationManager", "Événement déclenché: " + event);
        
        switch (event) {
            case GAME_START:
                startInitializationAnimation();
                break;
            case CORRECT_ANSWER:
                startCorrectAnswerAnimation();
                break;
            case WRONG_ANSWER:
                startWrongAnswerAnimation();
                break;
            case GAME_WON:
                startVictoryAnimation();
                break;
            case GAME_LOST:
                startDefeatAnimation();
                break;
            case RESET:
                resetAnimations();
                break;
        }
    }
    
    /**
     * Met à jour les animations
     */
    public void update(float delta) {
        // Mettre à jour les positions des slots
        for (Slot slot : slots) {
            slot.updatePosition();
        }
        
        // Mettre à jour les billes
        for (int i = balls.size - 1; i >= 0; i--) {
            AnimatedBall ball = balls.get(i);
            ball.update(delta);
            
            // Ne plus supprimer automatiquement les billes terminées
            // Elles peuvent rester dans le tableau avec état WAITING ou IN_SLOT
        }
        
        // Mettre à jour la roue
        wheel.update(delta);
        
        // Mettre à jour le réservoir
        reservoir.update(delta);
        
        // Mettre à jour le timer d'état si dans un état chronométré
        if (currentState != AnimationState.IDLE && currentState != AnimationState.COMPLETED) {
            stateTimer += delta;
            updateStateTransitions();
        }
        
        // Mettre à jour le système de répétition des touches en mode debug
        if (debugMode && lastPressedKey != -1) {
            keyRepeatTimer += delta;
            if (keyRepeatTimer >= keyRepeatDelay) {
                // Première répétition après le délai initial
                if (keyRepeatTimer >= keyRepeatDelay + keyRepeatInterval) {
                    handleDebugKeyRepeat(lastPressedKey);
                    keyRepeatTimer = keyRepeatDelay; // Reset pour les répétitions suivantes
                }
            }
        }
        
        // Vérifier les transitions d'état (legacy)
        checkStateTransitions();
    }
    
    /**
     * Dessine toutes les animations
     * Ordre de rendu (Z-order) :
     * 1. Background
     * 2. Door
     * 3. Wheel-outer (avec rotation)
     * 4. Wheel-center
     * 5. Billes du tube (sous tube-front)
     * 6. Tube-front
     * 7. Slots (back, billes, front)
     */
    public void draw(SpriteBatch batch, float viewportHeight) {
        // 1. Dessiner le background avec crop (comme SlidingPuzzle)
        if (backgroundTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(backgroundTexture, currentBgX, currentBgY, currentBgWidth, currentBgHeight);
        }
        
        // 2. Dessiner door
        if (doorTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(doorTexture, doorPosition.x, doorPosition.y);
        }
        
        // 3. Dessiner wheel-outer (avec rotation)
        if (wheelOuterTexture != null) {
            batch.setColor(1, 1, 1, 1);
            float centerX = wheelOuterPosition.x + wheelOuterTexture.getWidth() / 2;
            float centerY = wheelOuterPosition.y + wheelOuterTexture.getHeight() / 2;
            batch.draw(wheelOuterTexture,
                      wheelOuterPosition.x, wheelOuterPosition.y,
                      wheelOuterTexture.getWidth() / 2, wheelOuterTexture.getHeight() / 2,
                      wheelOuterTexture.getWidth(), wheelOuterTexture.getHeight(),
                      1f, 1f,
                      slotsCurrentRotation,
                      0, 0,
                      wheelOuterTexture.getWidth(), wheelOuterTexture.getHeight(),
                      false, false);
        }
        
        // 4. Dessiner wheel-center
        if (wheelCenterTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(wheelCenterTexture, wheelCenterPosition.x, wheelCenterPosition.y);
        }
        
        // 5. Dessiner les billes du tube (uniquement celles qui sont IN_TUBE ou DISAPPEARING_FROM_TUBE)
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.IN_TUBE || 
                ball.state == BallState.DISAPPEARING_FROM_TUBE) {
                ball.draw(batch);
            }
        }
        
        // 6. Dessiner tube-front
        if (tubeFrontTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(tubeFrontTexture, tubeFrontPosition.x, tubeFrontPosition.y);
        }
        
        // 7. Dessiner les slots (back, billes dans slots, front)
        for (Slot slot : slots) {
            slot.draw(batch);
        }
        
        // Dessiner le réservoir (legacy, si nécessaire)
        reservoir.draw(batch);
        
        // Dessiner la roue (legacy, si nécessaire)
        wheel.draw(batch);
        
        // Dessiner les informations de debug si le mode debug est activé
        if (debugMode && debugFont != null) {
            drawDebugInfo(batch, viewportHeight);
        }
    }
    
    /**
     * Définit les positions des éléments graphiques
     */
    public void setPositions(float reservoirX, float reservoirY, float reservoirWidth, float reservoirHeight,
                           float wheelX, float wheelY, float wheelWidth, float wheelHeight,
                           float successAreaX, float successAreaY, float successAreaWidth, float successAreaHeight) {
        this.reservoirX = reservoirX;
        this.reservoirY = reservoirY;
        this.reservoirWidth = reservoirWidth;
        this.reservoirHeight = reservoirHeight;
        
        this.wheelX = wheelX;
        this.wheelY = wheelY;
        this.wheelWidth = wheelWidth;
        this.wheelHeight = wheelHeight;
        
        this.successAreaX = successAreaX;
        this.successAreaY = successAreaY;
        this.successAreaWidth = successAreaWidth;
        this.successAreaHeight = successAreaHeight;
        
        // Mettre à jour les positions des éléments animés
        reservoir.setPosition(reservoirX, reservoirY, reservoirWidth, reservoirHeight);
        wheel.setPosition(wheelX, wheelY, wheelWidth, wheelHeight);
    }
    
    /**
     * Calcule les dimensions et l'échelle du background avec crop (comme SlidingPuzzle)
     */
    public void calculateBackgroundDimensions(float screenWidth, float screenHeight) {
        if (backgroundTexture == null) return;
        
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
        
        // Recalculer les positions absolues à partir des positions relatives
        updateAbsolutePositionsFromRelative();
    }
    
    /**
     * Met à jour les positions absolues à partir des positions relatives au background
     */
    private void updateAbsolutePositionsFromRelative() {
        if (backgroundTexture == null) return;
        
        // Calculer les positions absolues basées sur les positions relatives et les dimensions du background
        doorPosition.set(
            currentBgX + doorRelativePosition.x * currentBgWidth,
            currentBgY + doorRelativePosition.y * currentBgHeight
        );
        
        wheelOuterPosition.set(
            currentBgX + wheelOuterRelativePosition.x * currentBgWidth,
            currentBgY + wheelOuterRelativePosition.y * currentBgHeight
        );
        
        wheelCenterPosition.set(
            currentBgX + wheelCenterRelativePosition.x * currentBgWidth,
            currentBgY + wheelCenterRelativePosition.y * currentBgHeight
        );
        
        tubeFrontPosition.set(
            currentBgX + tubeFrontRelativePosition.x * currentBgWidth,
            currentBgY + tubeFrontRelativePosition.y * currentBgHeight
        );
    }
    
    /**
     * Charge les textures nécessaires
     */
    public void loadTextures() {
        loadInitializationAssets();
        loadBallTextures();
        loadWheelTextures();
        loadReservoirTextures();
        loadSlotTextures();
        
        Gdx.app.log("QnaAnimationManager", "Textures chargées");
    }
    
    /**
     * Charge les sons nécessaires
     */
    public void loadSounds() {
        try {
            winSound = Gdx.audio.newSound(Gdx.files.internal("audio/win.mp3"));
            Gdx.app.log("QnaAnimationManager", "Son win.mp3 chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement win.mp3: " + e.getMessage());
        }
        
        try {
            wrongSound = Gdx.audio.newSound(Gdx.files.internal("audio/wrong.wav"));
            Gdx.app.log("QnaAnimationManager", "Son wrong.wav chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement wrong.wav: " + e.getMessage());
        }
        
        try {
            slidingSound = Gdx.audio.newSound(Gdx.files.internal("audio/sliding.mp3"));
            Gdx.app.log("QnaAnimationManager", "Son sliding.mp3 chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement sliding.mp3: " + e.getMessage());
        }
        
        try {
            closeSound = Gdx.audio.newSound(Gdx.files.internal("audio/close.mp3"));
            Gdx.app.log("QnaAnimationManager", "Son close.mp3 chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement close.mp3: " + e.getMessage());
        }
        
        Gdx.app.log("QnaAnimationManager", "Sons chargés");
    }
    
    /**
     * Charge les assets d'initialisation
     */
    private void loadInitializationAssets() {
        try {
            backgroundTexture = new Texture(Gdx.files.internal("images/games/qna/background.png"));
            Gdx.app.log("QnaAnimationManager", "Background chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement background: " + e.getMessage());
            backgroundTexture = null;
        }
        
        try {
            doorTexture = new Texture(Gdx.files.internal("images/games/qna/door.png"));
            Gdx.app.log("QnaAnimationManager", "Door chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement door: " + e.getMessage());
            doorTexture = null;
        }
        
        try {
            wheelOuterTexture = new Texture(Gdx.files.internal("images/games/qna/wheel-outer.png"));
            Gdx.app.log("QnaAnimationManager", "Wheel-outer chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement wheel-outer: " + e.getMessage());
            wheelOuterTexture = null;
        }
        
        try {
            wheelCenterTexture = new Texture(Gdx.files.internal("images/games/qna/wheel-center.png"));
            Gdx.app.log("QnaAnimationManager", "Wheel-center chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement wheel-center: " + e.getMessage());
            wheelCenterTexture = null;
        }
        
        try {
            tubeFrontTexture = new Texture(Gdx.files.internal("images/games/qna/tube-front.png"));
            Gdx.app.log("QnaAnimationManager", "Tube-front chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement tube-front: " + e.getMessage());
            tubeFrontTexture = null;
        }
    }
    
    /**
     * Met à jour les statistiques du jeu
     */
    public void updateGameStats(int correctAnswers, int questionIndex) {
        this.currentCorrectAnswers = correctAnswers;
        this.currentQuestionIndex = questionIndex;
    }
    
    /**
     * Vérifie si une animation est en cours
     */
    public boolean isAnimating() {
        return currentState != AnimationState.IDLE && currentState != AnimationState.COMPLETED;
    }
    
    /**
     * Obtient l'état actuel
     */
    public AnimationState getCurrentState() {
        return currentState;
    }
    
    /**
     * Active/désactive le mode debug
     */
    public void toggleDebugMode() {
        debugMode = !debugMode;
        Gdx.app.log("QnaAnimationManager", "Mode debug positionnement: " + (debugMode ? "ACTIVÉ" : "DÉSACTIVÉ"));
    }
    
    /**
     * Gère les touches du clavier pour le debug
     */
    public boolean handleDebugKeyDown(int keycode) {
        if (!debugMode) return false;
        
        switch (keycode) {
            case com.badlogic.gdx.Input.Keys.PAGE_UP:
                // Remonter dans la liste
                selectedAsset = (selectedAsset - 1 + assetNames.length) % assetNames.length;
                Gdx.app.log("QnaAnimationManager", "Asset sélectionné: " + assetNames[selectedAsset]);
                return true;
                
            case com.badlogic.gdx.Input.Keys.PAGE_DOWN:
                // Descendre dans la liste
                selectedAsset = (selectedAsset + 1) % assetNames.length;
                Gdx.app.log("QnaAnimationManager", "Asset sélectionné: " + assetNames[selectedAsset]);
                return true;
                
            case com.badlogic.gdx.Input.Keys.LEFT:
                // Déplacer vers la gauche
                float leftStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(-leftStep, 0);
                startKeyRepeat(keycode);
                return true;
                
            case com.badlogic.gdx.Input.Keys.RIGHT:
                // Déplacer vers la droite
                float rightStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(rightStep, 0);
                startKeyRepeat(keycode);
                return true;
                
            case com.badlogic.gdx.Input.Keys.UP:
                // Déplacer vers le haut
                float upStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(0, upStep);
                startKeyRepeat(keycode);
                return true;
                
            case com.badlogic.gdx.Input.Keys.DOWN:
                // Déplacer vers le bas
                float downStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(0, -downStep);
                startKeyRepeat(keycode);
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Gère le relâchement des touches pour arrêter la répétition
     */
    public boolean handleDebugKeyUp(int keycode) {
        if (!debugMode) return false;
        
        if (keycode == lastPressedKey) {
            stopKeyRepeat();
            return true;
        }
        return false;
    }
    
    /**
     * Démarre la répétition d'une touche
     */
    private void startKeyRepeat(int keycode) {
        lastPressedKey = keycode;
        keyRepeatTimer = 0f;
    }
    
    /**
     * Arrête la répétition des touches
     */
    private void stopKeyRepeat() {
        lastPressedKey = -1;
        keyRepeatTimer = 0f;
    }
    
    /**
     * Gère la répétition d'une touche
     */
    private void handleDebugKeyRepeat(int keycode) {
        switch (keycode) {
            case com.badlogic.gdx.Input.Keys.LEFT:
                float leftStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(-leftStep, 0);
                break;
            case com.badlogic.gdx.Input.Keys.RIGHT:
                float rightStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(rightStep, 0);
                break;
            case com.badlogic.gdx.Input.Keys.UP:
                float upStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(0, upStep);
                break;
            case com.badlogic.gdx.Input.Keys.DOWN:
                float downStep = isCtrlPressed() ? fastMoveStep : moveStep;
                moveSelectedAsset(0, -downStep);
                break;
        }
    }
    
    /**
     * Vérifie si Ctrl est pressé
     */
    private boolean isCtrlPressed() {
        return com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_LEFT) || 
               com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.CONTROL_RIGHT);
    }
    
    /**
     * Déplace l'asset sélectionné (en coordonnées relatives au background)
     */
    private void moveSelectedAsset(float deltaX, float deltaY) {
        Vector2 relativePosition = null;
        float singleValue = -1f; // Pour les valeurs qui ne sont pas des positions 2D
        
        switch (selectedAsset) {
            case 0: relativePosition = doorRelativePosition; break;
            case 1: relativePosition = wheelOuterRelativePosition; break;
            case 2: relativePosition = wheelCenterRelativePosition; break;
            case 3: relativePosition = tubeFrontRelativePosition; break;
            case 4: // Slots Radius
                singleValue = slotsRadius;
                break;
            case 5: // Tube Base
                relativePosition = new Vector2(tubeColumnBaseX, tubeColumnBaseY);
                break;
            case 6: // Ball Height
                singleValue = ballHeight;
                break;
        }
        
        if (backgroundTexture != null) {
            // Convertir le déplacement en pixels en déplacement relatif au background
            float relativeDeltaX = deltaX / currentBgWidth;
            float relativeDeltaY = deltaY / currentBgHeight;
            
            if (relativePosition != null) {
                // Position 2D
                relativePosition.add(relativeDeltaX, relativeDeltaY);
                relativePosition.x = Math.max(0f, Math.min(1f, relativePosition.x));
                relativePosition.y = Math.max(0f, Math.min(1f, relativePosition.y));
                
                // Mettre à jour selon le type
                if (selectedAsset == 5) {
                    tubeColumnBaseX = relativePosition.x;
                    tubeColumnBaseY = relativePosition.y;
                }
                
                // Recalculer la position absolue
                updateAbsolutePositionsFromRelative();
                
                Gdx.app.log("QnaAnimationManager", assetNames[selectedAsset] + " position relative: (" + 
                    GwtCompatibleFormatter.formatFloat(relativePosition.x, 3) + ", " + 
                    GwtCompatibleFormatter.formatFloat(relativePosition.y, 3) + ")");
            } else if (singleValue >= 0f) {
                // Valeur simple (utiliser seulement deltaX pour augmenter/diminuer)
                float change = relativeDeltaX;
                if (selectedAsset == 4) {
                    // Slots Radius
                    slotsRadius += change;
                    slotsRadius = Math.max(0.01f, Math.min(0.5f, slotsRadius));
                    Gdx.app.log("QnaAnimationManager", "Slots Radius: " + GwtCompatibleFormatter.formatFloat(slotsRadius, 3));
                } else if (selectedAsset == 6) {
                    // Ball Height
                    ballHeight += change;
                    ballHeight = Math.max(0.01f, Math.min(0.2f, ballHeight));
                    Gdx.app.log("QnaAnimationManager", "Ball Height: " + GwtCompatibleFormatter.formatFloat(ballHeight, 3));
                }
            }
        }
    }
    
    /**
     * Définit les positions relatives des assets d'initialisation
     */
    public void setInitializationRelativePositions(float doorX, float doorY,
                                                 float wheelOuterX, float wheelOuterY,
                                                 float wheelCenterX, float wheelCenterY,
                                                 float tubeFrontX, float tubeFrontY) {
        doorRelativePosition.set(doorX, doorY);
        wheelOuterRelativePosition.set(wheelOuterX, wheelOuterY);
        wheelCenterRelativePosition.set(wheelCenterX, wheelCenterY);
        tubeFrontRelativePosition.set(tubeFrontX, tubeFrontY);
        
        // Recalculer les positions absolues
        updateAbsolutePositionsFromRelative();
        
        Gdx.app.log("QnaAnimationManager", "Positions relatives d'initialisation mises à jour");
    }
    
    /**
     * Dessine les informations de debug (style QuestionAnswerDebugManager)
     */
    private void drawDebugInfo(SpriteBatch batch, float viewportHeight) {
        float y = viewportHeight - 30;
        float lineHeight = 25;
        float indent = 40;
        
        // Titre
        debugFont.setColor(1f, 1f, 0f, 1f); // Jaune
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "=== DEBUG POSITIONNEMENT QNA (Alt+I) ===", 20, y);
        y -= lineHeight;
        
        // Instructions
        debugFont.setColor(0.8f, 0.8f, 0.8f, 1f);
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "Alt+I: Quitter debug", 20, y);
        y -= lineHeight;
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "Page Up/Down: Sélectionner asset", 20, y);
        y -= lineHeight;
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "←→: Déplacer X    ↑↓: Déplacer Y", 20, y);
        y -= lineHeight;
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "Pas: 1px | Ctrl+Flèches: 10px | Répétition: Maintenir", 20, y);
        y -= lineHeight;
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "Positions: Relatives au background (0.0-1.0)", 20, y);
        y -= lineHeight + 5;
        
        // Background (fixe)
        debugFont.setColor(0.6f, 0.6f, 0.6f, 1f);
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "Background: FIXE (crop auto)", 20, y);
        y -= lineHeight;
        
        // Assets positionnables avec sélection
        String[] positions = {
            "Door: (" + GwtCompatibleFormatter.formatFloat(doorRelativePosition.x, 3) + ", " + GwtCompatibleFormatter.formatFloat(doorRelativePosition.y, 3) + ")",
            "Wheel Outer: (" + GwtCompatibleFormatter.formatFloat(wheelOuterRelativePosition.x, 3) + ", " + GwtCompatibleFormatter.formatFloat(wheelOuterRelativePosition.y, 3) + ")",
            "Wheel Center: (" + GwtCompatibleFormatter.formatFloat(wheelCenterRelativePosition.x, 3) + ", " + GwtCompatibleFormatter.formatFloat(wheelCenterRelativePosition.y, 3) + ")",
            "Tube Front: (" + GwtCompatibleFormatter.formatFloat(tubeFrontRelativePosition.x, 3) + ", " + GwtCompatibleFormatter.formatFloat(tubeFrontRelativePosition.y, 3) + ")",
            "Slots Radius: " + GwtCompatibleFormatter.formatFloat(slotsRadius, 3) + " (← → pour modifier)",
            "Tube Base: (" + GwtCompatibleFormatter.formatFloat(tubeColumnBaseX, 3) + ", " + GwtCompatibleFormatter.formatFloat(tubeColumnBaseY, 3) + ")",
            "Ball Height: " + GwtCompatibleFormatter.formatFloat(ballHeight, 3) + " (← → pour modifier)"
        };
        
        for (int i = 0; i < positions.length; i++) {
            if (i == selectedAsset) {
                debugFont.setColor(1f, 0f, 0f, 1f); // Rouge pour la sélection
                com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "► " + positions[i], 20, y);
            } else {
                debugFont.setColor(0.8f, 0.8f, 0.8f, 1f);
                com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "  " + positions[i], 20, y);
            }
            y -= lineHeight;
        }
        
        y -= 10;
        
        // Informations de jeu
        debugFont.setColor(0f, 1f, 0f, 1f); // Vert
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, "État: " + currentState.name(), 20, y);
        y -= lineHeight;
        
        debugFont.setColor(1f, 0.5f, 0f, 1f); // Orange
        com.widedot.calendar.utils.CarlitoFontManager.drawText(batch, 
            "Questions: " + currentQuestionIndex + "/" + totalQuestions + 
            " | Bonnes: " + currentCorrectAnswers + "/" + victoryThreshold, 20, y);
    }
    
    // ===== MÉTHODES PRIVÉES =====
    
    /**
     * Met à jour les transitions d'état automatiques basées sur le timer
     */
    private void updateStateTransitions() {
        switch (currentState) {
            case WAITING_BEFORE_SLOT:
                // Attente de 1 seconde avant d'apparaître dans le slot
                if (stateTimer >= 1.0f) {
                    transitionToAppearInSlot();
                }
                break;
                
            case SLOTS_ROTATING:
                // Animer la rotation progressive
                float progress = Math.min(stateTimer / wheelSpinDuration, 1f);
                float easedProgress = Interpolation.smooth.apply(progress);
                
                // Calculer l'angle de rotation (un arc de cercle entre deux slots)
                float angleStep = 360f / victoryThreshold;
                float targetRotation = angleStep; // Rotation d'un slot
                
                // Appliquer la rotation progressive
                slotsCurrentRotation = easedProgress * targetRotation;
                
                // Rotation terminée ?
                if (stateTimer >= wheelSpinDuration) {
                    // Finaliser la rotation
                    slotsCurrentRotation = angleStep;
                    
                    // Recalculer les angles de base des slots pour prendre en compte la rotation
                    for (Slot slot : slots) {
                        slot.baseAngle += angleStep;
                    }
                    slotsCurrentRotation = 0f; // Reset rotation actuelle
                    
                    transitionToTubeDescending();
                }
                break;
                
            case TUBE_DESCENDING_CORRECT:
            case TUBE_DESCENDING_WRONG:
                // Descente terminée ?
                if (stateTimer >= 0.3f) {
                    transitionToIdle();
                }
                break;
        }
    }
    
    private void transitionToAppearInSlot() {
        currentState = AnimationState.BALL_APPEARING_IN_SLOT;
        stateTimer = 0f;
        
        // Trouver le prochain slot libre
        Slot targetSlot = findNextEmptySlot();
        if (targetSlot != null) {
            // Créer une nouvelle bille dans le slot
            AnimatedBall newBall = new AnimatedBall(balls.size, -1); // ID unique, pas d'index tube
            newBall.startAppearInSlot(targetSlot, 0.5f);
            targetSlot.assignBall(newBall);
            balls.add(newBall);
            
            nextSlotIndex++;
        }
        
        Gdx.app.log("QnaAnimationManager", "Bille apparaît dans le slot");
    }
    
    private void transitionToSlotRotating() {
        currentState = AnimationState.SLOTS_ROTATING;
        stateTimer = 0f;
        
        // Jouer le son de glissement
        if (slidingSound != null) {
            slidingSound.play();
        }
        
        Gdx.app.log("QnaAnimationManager", "Rotation des slots démarrée");
    }
    
    private void transitionToTubeDescending() {
        AnimationState previousState = currentState;
        
        if (previousState == AnimationState.SLOTS_ROTATING) {
            currentState = AnimationState.TUBE_DESCENDING_CORRECT;
        } else {
            currentState = AnimationState.TUBE_DESCENDING_WRONG;
        }
        
        stateTimer = 0f;
        
        // Animer la descente du tube
        animateTubeDescending();
        
        // Jouer le son de fermeture
        if (closeSound != null) {
            closeSound.play();
        }
        
        Gdx.app.log("QnaAnimationManager", "Descente du tube démarrée");
    }
    
    private void transitionToIdle() {
        currentState = AnimationState.COMPLETED;
        
        if (callback != null) {
            callback.onAnimationComplete(currentState);
        }
        
        // Retourner à l'état IDLE
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                currentState = AnimationState.IDLE;
                stateTimer = 0f;
            }
        });
        
        Gdx.app.log("QnaAnimationManager", "Transition vers IDLE");
    }
    
    private void animateTubeDescending() {
        // Calculer la hauteur d'une bille en pixels absolus
        float ballHeightAbs = ballHeight * currentBgHeight;
        
        // L'offset cible est -ballHeight (les billes descendent)
        // Pour simuler l'animation, on va simplement décrémenter l'index de chaque bille
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.IN_TUBE) {
                ball.moveDownInTube();
            }
        }
        
        // Supprimer la bille du bas si elle a disparu (index -1)
        for (int i = balls.size - 1; i >= 0; i--) {
            AnimatedBall ball = balls.get(i);
            if (ball.tubeIndex < 0 && ball.state == BallState.IN_TUBE) {
                balls.removeIndex(i);
            }
        }
    }
    
    private Slot findNextEmptySlot() {
        if (nextSlotIndex < slots.size) {
            return slots.get(nextSlotIndex);
        }
        return null;
    }
    
    private void startInitializationAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.INITIALIZING;
        stateTimer = 0f;
        nextSlotIndex = 0;
        
        // Créer les billes pour le tube
        balls.clear();
        for (int i = 0; i < totalQuestions; i++) {
            AnimatedBall ball = new AnimatedBall(i, i); // ID = index dans le tube
            balls.add(ball);
        }
        
        // Démarrer l'animation de remplissage du réservoir (legacy)
        reservoir.startFillAnimation(reservoirFillDuration);
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation d'initialisation démarrée : " + totalQuestions + " billes créées");
    }
    
    private void startCorrectAnswerAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.BALL_DISAPPEARING_CORRECT;
        stateTimer = 0f;
        
        // Jouer le son de victoire
        if (winSound != null) {
            winSound.play();
        }
        
        // Trouver la bille du bas dans le tube
        AnimatedBall bottomBall = findBottomBall();
        if (bottomBall != null) {
            bottomBall.startDisappearFromTube(ballDisappearDuration);
        }
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de bonne réponse démarrée (séquence complète)");
    }
    
    private void startWrongAnswerAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.BALL_DISAPPEARING_WRONG;
        stateTimer = 0f;
        
        // Jouer le son d'échec
        if (wrongSound != null) {
            wrongSound.play();
        }
        
        // Trouver la bille du bas dans le tube
        AnimatedBall bottomBall = findBottomBall();
        if (bottomBall != null) {
            bottomBall.startDisappearFromTube(ballDisappearDuration);
        }
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de mauvaise réponse démarrée");
    }
    
    private AnimatedBall findBottomBall() {
        // Trouver la bille avec tubeIndex = 0 (bille du bas)
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.IN_TUBE && ball.tubeIndex == 0) {
                return ball;
            }
        }
        return null;
    }
    
    private void startVictoryAnimation() {
        currentState = AnimationState.VICTORY_ANIMATION;
        
        // Animation de victoire : toutes les billes restantes se déplacent vers la zone de succès
        for (AnimatedBall ball : balls) {
            if (ball.isInReservoir()) {
                ball.startMoveToSuccessArea(successAreaX, successAreaY, successAreaWidth, successAreaHeight, ballMoveDuration);
            }
        }
        
        // Animation spéciale de la roue
        wheel.startVictoryAnimation(victoryAnimationDuration);
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de victoire démarrée");
    }
    
    private void startDefeatAnimation() {
        currentState = AnimationState.DEFEAT_ANIMATION;
        
        // Animation de défaite : toutes les billes restantes disparaissent
        for (AnimatedBall ball : balls) {
            if (ball.isInReservoir()) {
                ball.startDisappearAnimation(ballDisappearDuration);
            }
        }
        
        // Animation spéciale de la roue
        wheel.startDefeatAnimation(defeatAnimationDuration);
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de défaite démarrée");
    }
    
    private void resetAnimations() {
        currentState = AnimationState.IDLE;
        balls.clear();
        wheel.reset();
        reservoir.reset();
        
        Gdx.app.log("QnaAnimationManager", "Animations réinitialisées");
    }
    
    private void checkStateTransitions() {
        // Gérer les transitions d'état basées sur l'état des animations
        
        switch (currentState) {
            case BALL_DISAPPEARING_CORRECT:
                // Vérifier si la bille a terminé de disparaître
                if (isBallDisappearanceComplete()) {
                    currentState = AnimationState.WAITING_BEFORE_SLOT;
                    stateTimer = 0f;
                    Gdx.app.log("QnaAnimationManager", "Bille disparue, attente avant apparition dans slot");
                }
                break;
                
            case BALL_DISAPPEARING_WRONG:
                // Vérifier si la bille a terminé de disparaître
                if (isBallDisappearanceComplete()) {
                    transitionToTubeDescending();
                    Gdx.app.log("QnaAnimationManager", "Bille disparue, descente du tube");
                }
                break;
                
            case BALL_APPEARING_IN_SLOT:
                // Vérifier si la bille a terminé d'apparaître
                if (isBallAppearanceComplete()) {
                    transitionToSlotRotating();
                    Gdx.app.log("QnaAnimationManager", "Bille apparue, rotation des slots");
                }
                break;
                
            case INITIALIZING:
                // Vérifier si l'initialisation est terminée
                if (reservoir.isCompleted()) {
                    transitionToIdle();
                    Gdx.app.log("QnaAnimationManager", "Initialisation terminée");
                }
                break;
        }
        
        // Legacy check (pour compatibilité avec anciennes animations)
        if (currentState == AnimationState.VICTORY_ANIMATION || 
            currentState == AnimationState.DEFEAT_ANIMATION) {
            boolean allAnimationsComplete = true;
            
            for (AnimatedBall ball : balls) {
                if (!ball.isCompleted()) {
                    allAnimationsComplete = false;
                    break;
                }
            }
            
            if (!wheel.isCompleted()) {
                allAnimationsComplete = false;
            }
            
            if (allAnimationsComplete) {
                transitionToIdle();
            }
        }
    }
    
    private boolean isBallDisappearanceComplete() {
        // Vérifier si toutes les billes en train de disparaître ont terminé
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.DISAPPEARING_FROM_TUBE && ball.isAnimating()) {
                return false;
            }
        }
        return true;
    }
    
    private boolean isBallAppearanceComplete() {
        // Vérifier si toutes les billes en train d'apparaître ont terminé
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.APPEARING_IN_SLOT && ball.isAnimating()) {
                return false;
            }
        }
        return true;
    }
    
    private AnimatedBall findAvailableBall() {
        for (AnimatedBall ball : balls) {
            if (ball.isInReservoir() && !ball.isAnimating()) {
                return ball;
            }
        }
        return null;
    }
    
    private void loadBallTextures() {
        try {
            ballTexture = new Texture(Gdx.files.internal("images/games/qna/ball.png"));
            Gdx.app.log("QnaAnimationManager", "Ball texture chargée");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement ball.png: " + e.getMessage());
            ballTexture = null;
        }
    }
    
    private void loadSlotTextures() {
        try {
            slotBackTexture = new Texture(Gdx.files.internal("images/games/qna/slot-back.png"));
            Gdx.app.log("QnaAnimationManager", "Slot-back texture chargée");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement slot-back.png: " + e.getMessage());
            slotBackTexture = null;
        }
        
        try {
            slotFrontTexture = new Texture(Gdx.files.internal("images/games/qna/slot-front.png"));
            Gdx.app.log("QnaAnimationManager", "Slot-front texture chargée");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement slot-front.png: " + e.getMessage());
            slotFrontTexture = null;
        }
    }
    
    private void loadWheelTextures() {
        // TODO: Charger les textures de la roue (si nécessaire pour animations futures)
        wheelTextures.clear();
    }
    
    private void loadReservoirTextures() {
        // TODO: Charger les textures du réservoir (si nécessaire pour animations futures)
        reservoirTextures.clear();
    }
    
    @Override
    public void dispose() {
        // Nettoyer les textures d'initialisation
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (doorTexture != null) {
            doorTexture.dispose();
        }
        if (wheelOuterTexture != null) {
            wheelOuterTexture.dispose();
        }
        if (wheelCenterTexture != null) {
            wheelCenterTexture.dispose();
        }
        if (tubeFrontTexture != null) {
            tubeFrontTexture.dispose();
        }
        
        // Nettoyer les nouvelles textures
        if (ballTexture != null) {
            ballTexture.dispose();
        }
        if (slotBackTexture != null) {
            slotBackTexture.dispose();
        }
        if (slotFrontTexture != null) {
            slotFrontTexture.dispose();
        }
        
        // Nettoyer les textures existantes
        for (Texture texture : ballTextures) {
            texture.dispose();
        }
        for (Texture texture : wheelTextures) {
            texture.dispose();
        }
        for (Texture texture : reservoirTextures) {
            texture.dispose();
        }
        
        ballTextures.clear();
        wheelTextures.clear();
        reservoirTextures.clear();
        
        // Nettoyer les sons
        if (winSound != null) {
            winSound.dispose();
        }
        if (wrongSound != null) {
            wrongSound.dispose();
        }
        if (slidingSound != null) {
            slidingSound.dispose();
        }
        if (closeSound != null) {
            closeSound.dispose();
        }
        
        // Nettoyer la police de debug
        if (debugFont != null) {
            debugFont.dispose();
        }
        
        Gdx.app.log("QnaAnimationManager", "Ressources libérées");
    }
    
    // ===== CLASSES INTERNES POUR LES ÉLÉMENTS ANIMÉS =====
    
    /**
     * Classe pour représenter un slot (emplacement final pour une bille)
     */
    private class Slot {
        private int index;                  // Index du slot (0 à victoryThreshold-1)
        private float baseAngle;            // Angle de base sur le cercle (degrés)
        private float x, y;                 // Position absolue calculée
        private AnimatedBall ball;          // Bille dans ce slot (null si vide)
        private boolean isOccupied;         // Slot occupé ?
        
        Slot(int index) {
            this.index = index;
            this.isOccupied = false;
            this.ball = null;
            
            // Calculer l'angle de base
            this.baseAngle = slotsStartAngle + (360f / victoryThreshold) * index;
        }
        
        void updatePosition() {
            // Calculer l'angle actuel avec la rotation
            float currentAngle = baseAngle + slotsCurrentRotation;
            
            // Centre du cercle = centre de la texture wheel-outer
            float centerX = currentBgX + wheelOuterRelativePosition.x * currentBgWidth;
            float centerY = currentBgY + wheelOuterRelativePosition.y * currentBgHeight;
            
            // Ajouter la moitié de la taille de la texture pour obtenir le centre
            if (wheelOuterTexture != null) {
                centerX += wheelOuterTexture.getWidth() / 2f;
                centerY += wheelOuterTexture.getHeight() / 2f;
            }
            
            // Rayon absolu
            float radius = slotsRadius * currentBgWidth;
            
            // Calculer la position
            x = centerX + radius * MathUtils.cosDeg(currentAngle);
            y = centerY + radius * MathUtils.sinDeg(currentAngle);
        }
        
        void draw(SpriteBatch batch) {
            // Dessiner le slot-back
            if (slotBackTexture != null) {
                float slotWidth = slotBackTexture.getWidth();
                float slotHeight = slotBackTexture.getHeight();
                batch.draw(slotBackTexture, x - slotWidth / 2, y - slotHeight / 2);
            }
            
            // Dessiner la bille si présente
            if (ball != null && isOccupied) {
                ball.draw(batch);
            }
            
            // Dessiner le slot-front
            if (slotFrontTexture != null) {
                float slotWidth = slotFrontTexture.getWidth();
                float slotHeight = slotFrontTexture.getHeight();
                batch.draw(slotFrontTexture, x - slotWidth / 2, y - slotHeight / 2);
            }
        }
        
        void assignBall(AnimatedBall ball) {
            this.ball = ball;
            this.isOccupied = true;
        }
        
        boolean isEmpty() {
            return !isOccupied;
        }
        
        float getX() {
            return x;
        }
        
        float getY() {
            return y;
        }
    }
    
    /**
     * Classe pour représenter une bille animée
     */
    private class AnimatedBall {
        private int id;
        private int tubeIndex;              // Position dans le tube (0 = bas)
        private float x, y;
        private float startX, startY;
        private float targetX, targetY;
        private float animationTime;
        private float animationDuration;
        private boolean isAnimating;
        private boolean isCompleted;
        private boolean isInReservoir;
        private boolean isDisappearing;
        
        // Nouveaux attributs pour animations
        private BallState state;
        private float scale;                // Pour zoom (0.0 à 1.0)
        private float alpha;                // Pour fade (0.0 à 1.0)
        private Slot assignedSlot;          // Slot assigné (si applicable)
        private Texture texture;
        
        AnimatedBall(int id, int tubeIndex) {
            this.id = id;
            this.tubeIndex = tubeIndex;
            this.state = BallState.IN_TUBE;
            this.isAnimating = false;
            this.isCompleted = false;
            this.isInReservoir = true;
            this.isDisappearing = false;
            this.scale = 1.0f;
            this.alpha = 1.0f;
            this.assignedSlot = null;
            this.texture = ballTexture;
        }
        
        void updateTubePosition() {
            // Mettre à jour la position en fonction de l'index dans le tube
            if (state == BallState.IN_TUBE) {
                float baseX = currentBgX + tubeColumnBaseX * currentBgWidth;
                float baseY = currentBgY + tubeColumnBaseY * currentBgHeight;
                float ballHeightAbs = ballHeight * currentBgHeight;
                
                x = baseX;
                y = baseY + (tubeIndex * ballHeightAbs) + tubeCurrentOffset;
            }
        }
        
        void startReservoirFillAnimation(float reservoirX, float reservoirY, float reservoirWidth, float reservoirHeight, float delay) {
            this.startX = reservoirX + reservoirWidth / 2;
            this.startY = reservoirY + reservoirHeight / 2;
            this.x = startX;
            this.y = startY;
            this.isInReservoir = true;
            
            // Animation avec délai
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    startAnimation(startX, startY, 0.5f);
                }
            });
        }
        
        void startMoveToSuccessArea(float successX, float successY, float successWidth, float successHeight, float duration) {
            this.targetX = successX + successWidth / 2;
            this.targetY = successY + successHeight / 2;
            this.startX = x;
            this.startY = y;
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
            this.isInReservoir = false;
        }
        
        void startDisappearAnimation(float duration) {
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
            this.isDisappearing = true;
            this.state = BallState.DISAPPEARING_FROM_TUBE;
        }
        
        void startDisappearFromTube(float duration) {
            this.state = BallState.DISAPPEARING_FROM_TUBE;
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
            this.isDisappearing = true;
            this.isInReservoir = false;
        }
        
        void startAppearInSlot(Slot slot, float duration) {
            this.state = BallState.APPEARING_IN_SLOT;
            this.assignedSlot = slot;
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
            this.isDisappearing = false;
            this.scale = 0.0f;
            this.alpha = 0.0f;
            
            // Position de la bille = centre du slot
            this.x = slot.getX();
            this.y = slot.getY();
        }
        
        void moveDownInTube() {
            // Décrémenter l'index dans le tube (descendre)
            if (tubeIndex > 0) {
                tubeIndex--;
            }
        }
        
        private void startAnimation(float targetX, float targetY, float duration) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
        }
        
        void update(float delta) {
            if (!isAnimating) {
                // Si dans le tube, mettre à jour la position
                if (state == BallState.IN_TUBE) {
                    updateTubePosition();
                } else if (state == BallState.IN_SLOT && assignedSlot != null) {
                    // Si dans un slot, suivre la position du slot
                    x = assignedSlot.getX();
                    y = assignedSlot.getY();
                }
                return;
            }
            
            animationTime += delta;
            float progress = Math.min(animationTime / animationDuration, 1f);
            float easedProgress = Interpolation.smooth.apply(progress);
            
            switch (state) {
                case DISAPPEARING_FROM_TUBE:
                    // Zoom out + fade out
                    scale = 1.0f - easedProgress;      // 1.0 → 0.0
                    alpha = 1.0f - easedProgress;      // 1.0 → 0.0
                    updateTubePosition();              // Maintenir la position dans le tube
                    break;
                    
                case APPEARING_IN_SLOT:
                    // Zoom in + fade in
                    scale = easedProgress;             // 0.0 → 1.0
                    alpha = easedProgress;             // 0.0 → 1.0
                    // Position = centre du slot
                    if (assignedSlot != null) {
                        x = assignedSlot.getX();
                        y = assignedSlot.getY();
                    }
                    break;
                    
                default:
                    // Animation de mouvement classique
                    x = startX + (targetX - startX) * easedProgress;
                    y = startY + (targetY - startY) * easedProgress;
                    break;
            }
            
            if (progress >= 1f) {
                isAnimating = false;
                isCompleted = true;
                
                // Transition d'état
                switch (state) {
                    case DISAPPEARING_FROM_TUBE:
                        state = BallState.WAITING;
                        scale = 0.0f;
                        alpha = 0.0f;
                        break;
                        
                    case APPEARING_IN_SLOT:
                        state = BallState.IN_SLOT;
                        scale = 1.0f;
                        alpha = 1.0f;
                        break;
                        
                    default:
                        x = targetX;
                        y = targetY;
                        break;
                }
            }
        }
        
        void draw(SpriteBatch batch) {
            // Ne pas dessiner si dans état WAITING ou complètement transparent
            if (state == BallState.WAITING || alpha <= 0.0f) return;
            
            // Dessiner la texture de la bille avec scale et alpha
            if (texture != null) {
                float ballWidth = texture.getWidth();
                float ballHeight = texture.getHeight();
                
                // Appliquer l'alpha
                batch.setColor(1, 1, 1, alpha);
                
                // Dessiner avec scale (centré)
                float scaledWidth = ballWidth * scale;
                float scaledHeight = ballHeight * scale;
                batch.draw(texture, 
                          x - scaledWidth / 2, 
                          y - scaledHeight / 2,
                          scaledWidth, 
                          scaledHeight);
                
                // Réinitialiser la couleur
                batch.setColor(1, 1, 1, 1);
            }
        }
        
        boolean isAnimating() {
            return isAnimating;
        }
        
        boolean isCompleted() {
            return isCompleted;
        }
        
        boolean isInReservoir() {
            return isInReservoir && !isCompleted;
        }
    }
    
    /**
     * Classe pour représenter la roue animée
     */
    private class AnimatedWheel {
        private float x, y, width, height;
        private float rotation;
        private float targetRotation;
        private float animationTime;
        private float animationDuration;
        private boolean isAnimating;
        private boolean isCompleted;
        private Texture texture;
        
        AnimatedWheel() {
            this.isAnimating = false;
            this.isCompleted = false;
            this.rotation = 0;
        }
        
        void setPosition(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        void startSpinAnimation(float duration) {
            this.animationDuration = duration;
            this.targetRotation = rotation + 360f; // Rotation complète
            this.isAnimating = true;
            this.animationTime = 0;
            this.isCompleted = false;
        }
        
        void startVictoryAnimation(float duration) {
            this.animationDuration = duration;
            this.targetRotation = rotation + 720f; // Double rotation pour la victoire
            this.isAnimating = true;
            this.animationTime = 0;
            this.isCompleted = false;
        }
        
        void startDefeatAnimation(float duration) {
            this.animationDuration = duration;
            this.targetRotation = rotation + 180f; // Demi-tour pour la défaite
            this.isAnimating = true;
            this.animationTime = 0;
            this.isCompleted = false;
        }
        
        void update(float delta) {
            if (!isAnimating) return;
            
            animationTime += delta;
            float progress = Math.min(animationTime / animationDuration, 1f);
            
            float easedProgress = Interpolation.smooth.apply(progress);
            rotation = rotation + (targetRotation - rotation) * easedProgress;
            
            if (progress >= 1f) {
                rotation = targetRotation;
                isAnimating = false;
                isCompleted = true;
            }
        }
        
        void draw(SpriteBatch batch) {
            // TODO: Dessiner la texture de la roue avec rotation
        }
        
        void reset() {
            this.rotation = 0;
            this.isAnimating = false;
            this.isCompleted = false;
        }
        
        boolean isCompleted() {
            return isCompleted;
        }
    }
    
    /**
     * Classe pour représenter le réservoir animé
     */
    private class AnimatedReservoir {
        private float x, y, width, height;
        private float fillLevel;
        private float animationTime;
        private float animationDuration;
        private boolean isAnimating;
        private boolean isCompleted;
        private Texture texture;
        
        AnimatedReservoir() {
            this.isAnimating = false;
            this.isCompleted = false;
            this.fillLevel = 0;
        }
        
        void setPosition(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        void startFillAnimation(float duration) {
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
            this.isCompleted = false;
        }
        
        void update(float delta) {
            if (!isAnimating) return;
            
            animationTime += delta;
            float progress = Math.min(animationTime / animationDuration, 1f);
            
            fillLevel = progress; // Le réservoir se remplit progressivement
            
            if (progress >= 1f) {
                fillLevel = 1f;
                isAnimating = false;
                isCompleted = true;
            }
        }
        
        void draw(SpriteBatch batch) {
            // TODO: Dessiner le réservoir avec le niveau de remplissage
        }
        
        void reset() {
            this.fillLevel = 0;
            this.isAnimating = false;
            this.isCompleted = false;
        }
        
        boolean isCompleted() {
            return isCompleted;
        }
    }
}
