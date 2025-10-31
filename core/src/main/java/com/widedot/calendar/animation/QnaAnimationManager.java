package com.widedot.calendar.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Pixmap;
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
import com.widedot.calendar.display.DisplayConfig;

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
        
        VICTORY_ANIMATION,         // Animation finale de victoire (rotation center wheel + fade out)
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
    private Sound open2Sound;
    
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
    private float wheelOuterPermanentRotation;     // Rotation permanente accumulée de la wheel-outer
    
    // Tube de billes (colonne verticale)
    private float tubeColumnBaseX, tubeColumnBaseY; // Position bille du bas (relatif)
    private float ballHeight;                       // Hauteur d'une bille (relatif)
    private float tubeCurrentOffset;                // Offset pour descente animée
    private boolean isTubeDescending;               // Flag pour l'animation de descente
    private float tubeDescentTimer;                 // Timer pour l'animation de descente
    private float tubeDescentDuration;              // Durée de l'animation de descente
    
    // État d'animation
    private float stateTimer;                       // Timer pour animations chronométrées
    private int nextSlotIndex;                      // Prochain slot à remplir
    
    // Animation finale de victoire
    private float victoryAnimationTimer;            // Timer pour l'animation finale
    private float victoryRotationDuration;          // Durée de rotation center wheel (première partie)
    private float victoryWaitAfterRotation;          // Délai après rotation (500ms) + son open2
    private float victoryFadeOutDuration;           // Durée du fade out et montée (deuxième partie)
    private float victoryPaintingFadeInDuration;    // Durée du fade in de la peinture (troisième partie)
    private float victoryCenterWheelRotation;       // Rotation actuelle de la center wheel
    private float victoryFadeAlpha;                  // Alpha pour le fade out (1.0 -> 0.0)
    private float victoryVerticalOffset;            // Offset vertical pour le mouvement vers le haut (0 -> viewportHeight)
    private float victoryPaintingAlpha;              // Alpha pour le fade in de la peinture (0.0 -> 1.0)
    private Texture paintingTexture;                 // Texture de la peinture à afficher
    private Texture whiteTexture;                    // Texture blanche pour le fond noir
    private boolean open2SoundPlayed;                // Flag pour jouer open2.wav une seule fois
    private boolean winSoundPlayedForPainting;       // Flag pour jouer win.mp3 une seule fois au début du fade in
    
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
    private float ballMoveDuration = 0.1f;
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
        this.wheelOuterPermanentRotation = 0f;
        this.tubeColumnBaseX = 0.785f;
        this.tubeColumnBaseY = 0.512f;
        this.ballHeight = 0.044f;
        this.tubeCurrentOffset = 0f;
        this.isTubeDescending = false;
        this.tubeDescentTimer = 0f;
        this.tubeDescentDuration = 0.4f;
        this.stateTimer = 0f;
        this.nextSlotIndex = 0;
        
        // Initialiser l'animation finale de victoire
        this.victoryAnimationTimer = 0f;
        this.victoryRotationDuration = 1.5f;
        this.victoryWaitAfterRotation = 1.0f;
        this.victoryFadeOutDuration = 2.0f;  // Durée doublée pour le scroll vertical
        this.victoryPaintingFadeInDuration = 0.8f;
        this.victoryCenterWheelRotation = 0f;
        this.victoryFadeAlpha = 1.0f;
        this.victoryVerticalOffset = 0f;
        this.victoryPaintingAlpha = 0f;
        this.paintingTexture = null;
        this.open2SoundPlayed = false;
        this.winSoundPlayedForPainting = false;
        
        // Initialiser les éléments animés
        this.wheel = new AnimatedWheel();
        this.reservoir = new AnimatedReservoir();
        
        // Initialiser la police de debug
        this.debugFont = CarlitoFontManager.getFont();
        if (debugFont != null) {
            debugFont.getData().setScale(0.8f);
        }
        
        // Créer une texture blanche pour le fond noir
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();
        
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
        
        // Mettre à jour l'animation finale de victoire
        if (currentState == AnimationState.VICTORY_ANIMATION) {
            victoryAnimationTimer += delta;
            updateVictoryAnimation(delta);
        }
        
        // Mettre à jour l'animation de descente du tube avec gravité
        if (isTubeDescending) {
            tubeDescentTimer += delta;
            float progress = Math.min(tubeDescentTimer / tubeDescentDuration, 1f);
            
            // Interpolation avec gravité (accélération) : pow2In pour simuler la chute
            float easedProgress = Interpolation.pow2In.apply(progress);
            
            // Calculer l'offset de descente (de 0 à -ballHeight)
            float ballHeightAbs = ballHeight * currentBgHeight;
            tubeCurrentOffset = -easedProgress * ballHeightAbs;
            
            // Vérifier si l'animation est terminée
            if (progress >= 1f) {
                // Animation terminée : finaliser la descente
                finalizeTubeDescent();
            }
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
     * 
     * Pendant l'animation finale de victoire :
     * - Tous les assets ont un fade out et montent vers le haut
     * - La center wheel tourne en sens inverse
     * - Après l'animation, la peinture est affichée en plein écran
     */
    public void draw(SpriteBatch batch, float viewportHeight) {
        // Calculer l'alpha et l'offset vertical pour l'animation finale
        float alpha = 1.0f;
        float verticalOffset = 0f;
        if (currentState == AnimationState.VICTORY_ANIMATION) {
            alpha = victoryFadeAlpha;
            verticalOffset = victoryVerticalOffset * viewportHeight;
            
            // Dessiner la peinture en premier (sous tous les autres éléments)
            // Elle sera révélée naturellement quand les autres éléments montent
            drawFinalPainting(batch, viewportHeight);
        }
        
        // 1. Dessiner le background avec crop (comme SlidingPuzzle)
        if (backgroundTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            batch.draw(backgroundTexture, currentBgX, currentBgY + verticalOffset, currentBgWidth, currentBgHeight);
        }
        
        // 2. Dessiner door
        if (doorTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            batch.draw(doorTexture, doorPosition.x, doorPosition.y + verticalOffset);
        }
        
        // 3. Dessiner wheel-outer (avec rotation permanente + rotation d'animation)
        if (wheelOuterTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            float centerX = wheelOuterPosition.x + wheelOuterTexture.getWidth() / 2;
            float centerY = wheelOuterPosition.y + wheelOuterTexture.getHeight() / 2;
            
            // Rotation totale = rotation permanente accumulée + rotation d'animation courante
            float totalRotation = wheelOuterPermanentRotation + slotsCurrentRotation;
            
            batch.draw(wheelOuterTexture,
                      wheelOuterPosition.x, wheelOuterPosition.y + verticalOffset,
                      wheelOuterTexture.getWidth() / 2, wheelOuterTexture.getHeight() / 2,
                      wheelOuterTexture.getWidth(), wheelOuterTexture.getHeight(),
                      1f, 1f,
                      totalRotation,
                      0, 0,
                      wheelOuterTexture.getWidth(), wheelOuterTexture.getHeight(),
                      false, false);
        }
        
        // 4. Dessiner wheel-center (avec rotation si animation finale)
        if (wheelCenterTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            float centerX = wheelCenterPosition.x + wheelCenterTexture.getWidth() / 2;
            float centerY = wheelCenterPosition.y + wheelCenterTexture.getHeight() / 2;
            
            if (currentState == AnimationState.VICTORY_ANIMATION) {
                // Rotation en sens inverse pendant l'animation finale
                batch.draw(wheelCenterTexture,
                          wheelCenterPosition.x, wheelCenterPosition.y + verticalOffset,
                          wheelCenterTexture.getWidth() / 2, wheelCenterTexture.getHeight() / 2,
                          wheelCenterTexture.getWidth(), wheelCenterTexture.getHeight(),
                          1f, 1f,
                          victoryCenterWheelRotation,
                          0, 0,
                          wheelCenterTexture.getWidth(), wheelCenterTexture.getHeight(),
                          false, false);
            } else {
                batch.draw(wheelCenterTexture, wheelCenterPosition.x, wheelCenterPosition.y + verticalOffset);
            }
        }
        
        // 5. Dessiner les billes du tube (uniquement celles qui sont IN_TUBE ou DISAPPEARING_FROM_TUBE)
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.IN_TUBE || 
                ball.state == BallState.DISAPPEARING_FROM_TUBE) {
                ball.draw(batch, alpha, verticalOffset);
            }
        }
        
        // 6. Dessiner tube-front
        if (tubeFrontTexture != null) {
            batch.setColor(1, 1, 1, alpha);
            batch.draw(tubeFrontTexture, tubeFrontPosition.x, tubeFrontPosition.y + verticalOffset);
        }
        
        // 7. Dessiner les slots (back, billes dans slots, front)
        for (Slot slot : slots) {
            slot.draw(batch, alpha, verticalOffset);
        }
        
        // Réinitialiser la couleur pour les éléments suivants
        batch.setColor(1, 1, 1, 1);
        
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
     * Dessine la peinture finale en plein écran (comme Mastermind)
     */
    private void drawFinalPainting(SpriteBatch batch, float viewportHeight) {
        if (paintingTexture == null) return;
        
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewportHeight;
        
        // Calculer les dimensions adaptatives pour utiliser toute la hauteur du viewport
        float originalWidth = paintingTexture.getWidth();
        float originalHeight = paintingTexture.getHeight();
        float aspectRatio = originalWidth / originalHeight;
        
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
        
        // Dessiner un fond noir pour toute la zone
        batch.setColor(0f, 0f, 0f, 1f);
        if (whiteTexture != null) {
            batch.draw(whiteTexture, 0, 0, screenWidth, screenHeight);
        } else if (backgroundTexture != null) {
            batch.draw(backgroundTexture, 0, 0, screenWidth, screenHeight);
        }
        
        // Centrer l'image
        float imageX = (screenWidth - imageWidth) / 2;
        float imageY = (screenHeight - imageHeight) / 2;
        
        // Dessiner la peinture complète (visible dès le début, révélée par le scroll vertical)
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(paintingTexture, imageX, imageY, imageWidth, imageHeight);
        
        // Réinitialiser la couleur
        batch.setColor(1, 1, 1, 1);
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
     * Charge la texture de la peinture pour l'animation finale
     */
    public void loadPaintingTexture(String paintingPath) {
        try {
            if (paintingTexture != null) {
                paintingTexture.dispose();
            }
            paintingTexture = new Texture(Gdx.files.internal(paintingPath));
            // Appliquer un filtrage Linear pour l'antialiasing lors du redimensionnement
            paintingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            Gdx.app.log("QnaAnimationManager", "Peinture chargée avec antialiasing: " + paintingPath);
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement peinture: " + e.getMessage());
            paintingTexture = null;
        }
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
        
        try {
            open2Sound = Gdx.audio.newSound(Gdx.files.internal("audio/open2.mp3"));
            Gdx.app.log("QnaAnimationManager", "Son open2.mp3 chargé");
        } catch (Exception e) {
            Gdx.app.error("QnaAnimationManager", "Erreur chargement open2.mp3: " + e.getMessage());
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
     * Obtient l'alpha actuel pour le fade out (utilisé par QuestionAnswerGameScreen pour les boutons)
     */
    public float getVictoryFadeAlpha() {
        if (currentState == AnimationState.VICTORY_ANIMATION) {
            return victoryFadeAlpha;
        }
        return 1.0f;
    }
    
    /**
     * Obtient l'offset vertical actuel pour le mouvement vers le haut (utilisé par QuestionAnswerGameScreen pour les boutons)
     */
    public float getVictoryVerticalOffset() {
        if (currentState == AnimationState.VICTORY_ANIMATION) {
            return victoryVerticalOffset;
        }
        return 0f;
    }
    
    /**
     * Vérifie si on est en phase finale (peinture visible ou animation finale en cours)
     */
    public boolean isInFinalPhase() {
        if (currentState == AnimationState.VICTORY_ANIMATION) {
            // Dès que l'animation finale commence, on masque l'input
            // La peinture devient visible après le fade out
            return true;
        }
        return false;
    }
    
    /**
     * Vérifie si toutes les billes sont épuisées (plus de billes dans le tube)
     */
    public boolean areBallsExhausted() {
        return getRemainingBallsCount() == 0;
    }
    
    /**
     * Compte le nombre de billes restantes dans le tube
     */
    public int getRemainingBallsCount() {
        int remainingBalls = 0;
        for (AnimatedBall ball : balls) {
            if (ball.state == BallState.IN_TUBE) {
                remainingBalls++;
            }
        }
        return remainingBalls;
    }
    
    /**
     * Obtient la texture de la peinture (pour affichage dans QuestionAnswerGameScreen)
     */
    public Texture getPaintingTexture() {
        return paintingTexture;
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
                // SENS ANTIHORAIRE : on utilise un angle négatif
                float angleStep = -360f / victoryThreshold;
                float targetRotation = angleStep; // Rotation d'un slot (sens antihoraire)
                
                // Appliquer la rotation progressive
                slotsCurrentRotation = easedProgress * targetRotation;
                
                // Rotation terminée ?
                if (stateTimer >= wheelSpinDuration) {
                    // Finaliser la rotation : accumuler dans la rotation permanente
                    wheelOuterPermanentRotation += angleStep;
                    
                    // Recalculer les angles de base des slots pour prendre en compte la rotation
                    for (Slot slot : slots) {
                        slot.baseAngle += angleStep;
                    }
                    
                    // Reset rotation actuelle (animation terminée)
                    slotsCurrentRotation = 0f;
                    
                    transitionToTubeDescending();
                }
                break;
                
            case TUBE_DESCENDING_CORRECT:
            case TUBE_DESCENDING_WRONG:
                // L'animation de descente est gérée par isTubeDescending
                // On attend juste que l'animation soit terminée (géré dans finalizeTubeDescent)
                break;
                
            case VICTORY_ANIMATION:
                // L'animation finale est gérée par updateVictoryAnimation()
                // Pas de transition automatique ici
                break;
        }
    }
    
    /**
     * Met à jour l'animation finale de victoire
     */
    private void updateVictoryAnimation(float delta) {
        float rotationEndTime = victoryRotationDuration;
        float fadeOutStartTime = rotationEndTime + victoryWaitAfterRotation;
        float totalDuration = fadeOutStartTime + victoryFadeOutDuration + victoryPaintingFadeInDuration;
        
        if (victoryAnimationTimer < rotationEndTime) {
            // Phase 1 : Rotation d'un quart de tour de la center wheel (sens horaire, opposé à l'outer wheel)
            float progress = victoryAnimationTimer / victoryRotationDuration;
            float easedProgress = Interpolation.smooth.apply(progress);
            
            // Rotation d'un quart de tour (90°) dans le sens horaire (positif)
            victoryCenterWheelRotation = 90f * easedProgress;
        } else if (victoryAnimationTimer < fadeOutStartTime) {
            // Phase 1.5 : Attente après rotation (2.0s) + jouer open2.wav
            if (!open2SoundPlayed) {
                // Jouer le son open2.wav une seule fois à la fin de la rotation
                if (open2Sound != null) {
                    open2Sound.play();
                }
                open2SoundPlayed = true;
                Gdx.app.log("QnaAnimationManager", "Son open2.wav joué après rotation");
            }
            // Maintenir la rotation finale (quart de tour)
            victoryCenterWheelRotation = 90f;
        } else if (victoryAnimationTimer < fadeOutStartTime + victoryFadeOutDuration) {
            // Phase 2 : Montée de tous les assets (sans fade out, restent opaques)
            float fadeProgress = (victoryAnimationTimer - fadeOutStartTime) / victoryFadeOutDuration;
            float easedFadeProgress = Interpolation.smooth.apply(fadeProgress);
            
            // Alpha reste à 1.0 (pas de fade out)
            victoryFadeAlpha = 1.0f;
            
            // L'offset vertical augmente (les assets montent)
            // On utilise une interpolation pour une montée fluide
            // Multiplier par 1.5 pour monter plus haut (1.5 fois la hauteur du viewport)
            victoryVerticalOffset = easedFadeProgress * 1.5f; // Multiplié par viewportHeight dans draw
        } else if (victoryAnimationTimer < totalDuration) {
            // Phase 3 : Fade in de la peinture (plus nécessaire maintenant, mais gardé pour compatibilité)
            float fadeInProgress = (victoryAnimationTimer - fadeOutStartTime - victoryFadeOutDuration) / victoryPaintingFadeInDuration;
            float easedFadeInProgress = Interpolation.smooth.apply(fadeInProgress);
            
            // Alpha augmente de 0.0 à 1.0 (pour la peinture, mais elle est déjà visible)
            victoryPaintingAlpha = easedFadeInProgress;
            
            // Les assets restent opaques et sont complètement montés
            victoryFadeAlpha = 1.0f;
            victoryVerticalOffset = 1.5f; // Montée plus haute
        } else {
            // Animation terminée
            victoryPaintingAlpha = 1.0f;
            victoryFadeAlpha = 1.0f; // Reste opaque
            victoryVerticalOffset = 1.5f; // Montée plus haute
            
            // Ne pas passer à COMPLETED ici, on reste en VICTORY_ANIMATION pour afficher la peinture
            // Le callback gérera le retour au calendrier sur clic
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
        // Vérifier si c'est le dernier slot (tous les slots sont remplis)
        if (nextSlotIndex >= victoryThreshold) {
            // Dernier slot rempli : démarrer l'animation finale au lieu de tourner
            startVictoryAnimation();
            return;
        }
        
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
        
        // Démarrer l'animation de descente du tube avec gravité
        startTubeDescentAnimation();
        
        // Le son close.mp3 sera joué après que les billes soient tombées (dans finalizeTubeDescent)
        
        Gdx.app.log("QnaAnimationManager", "Descente du tube démarrée (avec animation)");
    }
    
    /**
     * Démarre l'animation de descente du tube avec gravité
     */
    private void startTubeDescentAnimation() {
        isTubeDescending = true;
        tubeDescentTimer = 0f;
        tubeCurrentOffset = 0f;
    }
    
    /**
     * Finalise la descente du tube après l'animation
     */
    private void finalizeTubeDescent() {
        // L'animation est terminée
        isTubeDescending = false;
        tubeDescentTimer = 0f;
        
        // Décrémenter l'index de chaque bille dans le tube
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
        
        // Réinitialiser l'offset
        tubeCurrentOffset = 0f;
        
        // Vérifier si toutes les billes sont épuisées avant de jouer le son
        boolean allBallsExhausted = areBallsExhausted();
        
        // Jouer le son de fermeture APRÈS que les billes soient tombées
        // Ne pas jouer si c'est la dernière bille qui vient de disparaître
        if (closeSound != null && !allBallsExhausted) {
            closeSound.play();
        }
        
        // Transition vers IDLE
        transitionToIdle();
        
        Gdx.app.log("QnaAnimationManager", "Descente du tube terminée");
    }
    
    private void transitionToIdle() {
        // Stocker l'état précédent avant de passer à COMPLETED
        AnimationState previousState = currentState;
        currentState = AnimationState.COMPLETED;
        
        if (callback != null) {
            callback.onAnimationComplete(previousState);
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
        victoryAnimationTimer = 0f;
        victoryCenterWheelRotation = 0f;
        victoryFadeAlpha = 1.0f;
        victoryVerticalOffset = 0f;
        victoryPaintingAlpha = 0f;
        open2SoundPlayed = false;
        winSoundPlayedForPainting = false;
        
        // Notifier le callback pour masquer l'input au début de l'animation finale
        if (callback != null) {
            callback.onAnimationStarted(currentState);
        }
        
        Gdx.app.log("QnaAnimationManager", "Animation finale de victoire démarrée");
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
        
        // Réinitialiser les rotations
        slotsCurrentRotation = 0f;
        wheelOuterPermanentRotation = 0f;
        nextSlotIndex = 0;
        
        // Réinitialiser l'animation de descente du tube
        isTubeDescending = false;
        tubeDescentTimer = 0f;
        tubeCurrentOffset = 0f;
        
        // Réinitialiser les angles de base des slots
        for (int i = 0; i < slots.size; i++) {
            Slot slot = slots.get(i);
            slot.baseAngle = slotsStartAngle + (360f / victoryThreshold) * i;
        }
        
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
        
        // Nettoyer la peinture
        if (paintingTexture != null) {
            paintingTexture.dispose();
        }
        
        // Nettoyer la texture blanche
        if (whiteTexture != null) {
            whiteTexture.dispose();
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
        if (open2Sound != null) {
            open2Sound.dispose();
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
        
        void draw(SpriteBatch batch, float alpha, float verticalOffset) {
            // Dessiner le slot-back
            if (slotBackTexture != null) {
                batch.setColor(1, 1, 1, alpha);
                float slotWidth = slotBackTexture.getWidth();
                float slotHeight = slotBackTexture.getHeight();
                batch.draw(slotBackTexture, x - slotWidth / 2, y - slotHeight / 2 + verticalOffset);
            }
            
            // Dessiner la bille si présente
            if (ball != null && isOccupied) {
                ball.draw(batch, alpha, verticalOffset);
            }
            
            // Dessiner le slot-front
            if (slotFrontTexture != null) {
                batch.setColor(1, 1, 1, alpha);
                float slotWidth = slotFrontTexture.getWidth();
                float slotHeight = slotFrontTexture.getHeight();
                batch.draw(slotFrontTexture, x - slotWidth / 2, y - slotHeight / 2 + verticalOffset);
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
        
        void draw(SpriteBatch batch, float alpha, float verticalOffset) {
            // Ne pas dessiner si dans état WAITING ou complètement transparent
            if (state == BallState.WAITING || alpha <= 0.0f) return;
            
            // Dessiner la texture de la bille avec scale et alpha
            if (texture != null) {
                float ballWidth = texture.getWidth();
                float ballHeight = texture.getHeight();
                
                // Appliquer l'alpha combiné (alpha de la bille * alpha de l'animation finale)
                float combinedAlpha = this.alpha * alpha;
                batch.setColor(1, 1, 1, combinedAlpha);
                
                // Dessiner avec scale (centré)
                float scaledWidth = ballWidth * scale;
                float scaledHeight = ballHeight * scale;
                batch.draw(texture, 
                          x - scaledWidth / 2, 
                          y - scaledHeight / 2 + verticalOffset,
                          scaledWidth, 
                          scaledHeight);
                
                // Réinitialiser la couleur
                batch.setColor(1, 1, 1, 1);
            }
        }
        
        void draw(SpriteBatch batch) {
            // Méthode legacy pour compatibilité
            draw(batch, 1.0f, 0f);
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
