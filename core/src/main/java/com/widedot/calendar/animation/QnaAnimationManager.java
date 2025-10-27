package com.widedot.calendar.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Interpolation;
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
        IDLE,                    // État initial, aucune animation
        INITIALIZING,           // Animation d'initialisation (remplissage réservoir)
        BALL_MOVING_CORRECT,    // Animation de déplacement de bille (bonne réponse)
        BALL_DISAPPEARING,      // Animation de disparition de bille (mauvaise réponse)
        WHEEL_SPINNING,         // Animation de rotation de roue
        VICTORY_ANIMATION,      // Animation finale de victoire
        DEFEAT_ANIMATION,       // Animation finale de défaite
        COMPLETED               // Animation terminée
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
    private AnimatedWheel wheel;
    private AnimatedReservoir reservoir;
    private Array<Texture> ballTextures;
    private Array<Texture> wheelTextures;
    private Array<Texture> reservoirTextures;
    
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
    
    // Mode debug
    private boolean debugMode = false;
    private BitmapFont debugFont;
    private int selectedAsset = 0; // 0=door, 1=wheelOuter, 2=wheelCenter, 3=tubeFront
    private String[] assetNames = {"Door", "Wheel Outer", "Wheel Center", "Tube Front"};
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
        this.ballTextures = new Array<>();
        this.wheelTextures = new Array<>();
        this.reservoirTextures = new Array<>();
        
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
        // Mettre à jour les billes
        for (int i = balls.size - 1; i >= 0; i--) {
            AnimatedBall ball = balls.get(i);
            ball.update(delta);
            
            // Supprimer les billes terminées
            if (ball.isCompleted()) {
                balls.removeIndex(i);
            }
        }
        
        // Mettre à jour la roue
        wheel.update(delta);
        
        // Mettre à jour le réservoir
        reservoir.update(delta);
        
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
        
        // Vérifier les transitions d'état
        checkStateTransitions();
    }
    
    /**
     * Dessine toutes les animations
     */
    public void draw(SpriteBatch batch, float viewportHeight) {
        // Dessiner le background avec crop (comme SlidingPuzzle)
        if (backgroundTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(backgroundTexture, currentBgX, currentBgY, currentBgWidth, currentBgHeight);
        }
        
        // Dessiner les autres assets positionnables
        if (doorTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(doorTexture, doorPosition.x, doorPosition.y);
        }
        
        if (wheelOuterTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(wheelOuterTexture, wheelOuterPosition.x, wheelOuterPosition.y);
        }
        
        if (wheelCenterTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(wheelCenterTexture, wheelCenterPosition.x, wheelCenterPosition.y);
        }
        
        if (tubeFrontTexture != null) {
            batch.setColor(1, 1, 1, 1);
            batch.draw(tubeFrontTexture, tubeFrontPosition.x, tubeFrontPosition.y);
        }
        
        // Dessiner le réservoir
        reservoir.draw(batch);
        
        // Dessiner la roue
        wheel.draw(batch);
        
        // Dessiner les billes
        for (AnimatedBall ball : balls) {
            ball.draw(batch);
        }
        
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
        
        Gdx.app.log("QnaAnimationManager", "Textures chargées");
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
        
        switch (selectedAsset) {
            case 0: relativePosition = doorRelativePosition; break;
            case 1: relativePosition = wheelOuterRelativePosition; break;
            case 2: relativePosition = wheelCenterRelativePosition; break;
            case 3: relativePosition = tubeFrontRelativePosition; break;
        }
        
        if (relativePosition != null && backgroundTexture != null) {
            // Convertir le déplacement en pixels en déplacement relatif au background
            float relativeDeltaX = deltaX / currentBgWidth;
            float relativeDeltaY = deltaY / currentBgHeight;
            
            // Appliquer le déplacement relatif
            relativePosition.add(relativeDeltaX, relativeDeltaY);
            
            // Limiter les positions relatives entre 0 et 1
            relativePosition.x = Math.max(0f, Math.min(1f, relativePosition.x));
            relativePosition.y = Math.max(0f, Math.min(1f, relativePosition.y));
            
            // Recalculer la position absolue
            updateAbsolutePositionsFromRelative();
            
            Gdx.app.log("QnaAnimationManager", assetNames[selectedAsset] + " position relative: (" + 
                GwtCompatibleFormatter.formatFloat(relativePosition.x, 3) + ", " + 
                GwtCompatibleFormatter.formatFloat(relativePosition.y, 3) + ")");
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
            "Tube Front: (" + GwtCompatibleFormatter.formatFloat(tubeFrontRelativePosition.x, 3) + ", " + GwtCompatibleFormatter.formatFloat(tubeFrontRelativePosition.y, 3) + ")"
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
    
    private void startInitializationAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.INITIALIZING;
        
        // Créer les billes pour le réservoir
        balls.clear();
        for (int i = 0; i < totalQuestions; i++) {
            AnimatedBall ball = new AnimatedBall(i);
            ball.startReservoirFillAnimation(reservoirX, reservoirY, reservoirWidth, reservoirHeight, i * 0.1f);
            balls.add(ball);
        }
        
        // Démarrer l'animation de remplissage du réservoir
        reservoir.startFillAnimation(reservoirFillDuration);
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation d'initialisation démarrée");
    }
    
    private void startCorrectAnswerAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.BALL_MOVING_CORRECT;
        
        // Trouver une bille disponible dans le réservoir
        AnimatedBall ballToMove = findAvailableBall();
        if (ballToMove != null) {
            ballToMove.startMoveToSuccessArea(successAreaX, successAreaY, successAreaWidth, successAreaHeight, ballMoveDuration);
        }
        
        // Démarrer la rotation de la roue
        wheel.startSpinAnimation(wheelSpinDuration);
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de bonne réponse démarrée");
    }
    
    private void startWrongAnswerAnimation() {
        if (currentState != AnimationState.IDLE) return;
        
        currentState = AnimationState.BALL_DISAPPEARING;
        
        // Trouver une bille disponible dans le réservoir
        AnimatedBall ballToRemove = findAvailableBall();
        if (ballToRemove != null) {
            ballToRemove.startDisappearAnimation(ballDisappearDuration);
        }
        
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation de mauvaise réponse démarrée");
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
        // Vérifier si toutes les animations en cours sont terminées
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
        
        if (!reservoir.isCompleted()) {
            allAnimationsComplete = false;
        }
        
        // Si toutes les animations sont terminées, passer à l'état suivant
        if (allAnimationsComplete && currentState != AnimationState.IDLE && currentState != AnimationState.COMPLETED) {
            AnimationState previousState = currentState;
            currentState = AnimationState.COMPLETED;
            
            if (callback != null) {
                callback.onAnimationComplete(previousState);
            }
            
            // Retourner à l'état IDLE après un court délai
            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    currentState = AnimationState.IDLE;
                }
            });
        }
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
        // TODO: Charger les textures des billes
        // Pour l'instant, créer des textures temporaires
        ballTextures.clear();
    }
    
    private void loadWheelTextures() {
        // TODO: Charger les textures de la roue
        wheelTextures.clear();
    }
    
    private void loadReservoirTextures() {
        // TODO: Charger les textures du réservoir
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
        
        // Nettoyer la police de debug
        if (debugFont != null) {
            debugFont.dispose();
        }
        
        Gdx.app.log("QnaAnimationManager", "Ressources libérées");
    }
    
    // ===== CLASSES INTERNES POUR LES ÉLÉMENTS ANIMÉS =====
    
    /**
     * Classe pour représenter une bille animée
     */
    private class AnimatedBall {
        private int id;
        private float x, y;
        private float startX, startY;
        private float targetX, targetY;
        private float animationTime;
        private float animationDuration;
        private boolean isAnimating;
        private boolean isCompleted;
        private boolean isInReservoir;
        private boolean isDisappearing;
        private Texture texture;
        
        AnimatedBall(int id) {
            this.id = id;
            this.isAnimating = false;
            this.isCompleted = false;
            this.isInReservoir = false;
            this.isDisappearing = false;
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
        }
        
        private void startAnimation(float targetX, float targetY, float duration) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.animationDuration = duration;
            this.isAnimating = true;
            this.animationTime = 0;
        }
        
        void update(float delta) {
            if (!isAnimating) return;
            
            animationTime += delta;
            float progress = Math.min(animationTime / animationDuration, 1f);
            
            if (isDisappearing) {
                // Animation de disparition (fade out)
                // TODO: Implémenter le fade out
            } else {
                // Animation de mouvement avec interpolation
                float easedProgress = Interpolation.smooth.apply(progress);
                x = startX + (targetX - startX) * easedProgress;
                y = startY + (targetY - startY) * easedProgress;
            }
            
            if (progress >= 1f) {
                isAnimating = false;
                isCompleted = true;
                if (isDisappearing) {
                    // La bille disparaît complètement
                } else {
                    // La bille arrive à destination
                    x = targetX;
                    y = targetY;
                }
            }
        }
        
        void draw(SpriteBatch batch) {
            if (isDisappearing && isCompleted()) return;
            
            // TODO: Dessiner la texture de la bille
            // Pour l'instant, dessiner un cercle simple
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
