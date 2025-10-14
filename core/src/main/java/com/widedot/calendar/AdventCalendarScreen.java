package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.widedot.calendar.display.InputManager;
import com.widedot.calendar.display.DisplayConfig;
import com.widedot.calendar.display.ViewportManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.config.ThemeManager;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.screens.TransitionScreen;

/**
 * Écran principal du calendrier de l'Avent
 * Affiche la grille de 24 cases et gère les interactions
 */
public class AdventCalendarScreen implements Screen {

    // Constants - utiliser le système centralisé
    // WORLD_WIDTH et WORLD_HEIGHT sont maintenant gérés par DisplayConfig et ViewportManager
    private static final float DOOR_SLIDE_SPEED = 2.0f;
    private static final float DRAG_THRESHOLD = 10f;
    private static final int FALLBACK_TEXTURE_SIZE = 64;
    
    // Resource paths
    private static final String FOREGROUND_TEXTURE_PATH = "images/calendar/foreground.png";
    private static final String SHADOW_TEXTURE_PATH = "images/calendar/shadow.png";
    private static final String MASK_JSON_PATH = "images/calendar/mask.json";
    private static final String DOOR_IMAGE_PATH_PREFIX = "images/calendar/door-";
    private static final String ARROW_TEXTURE_PATH = "images/calendar/arrow.png";
    private static final String LOCKED_SOUND_PATH = "audio/locked.mp3";
    private static final String OPEN_SOUND_PATH = "audio/open2.mp3";
    private static final String ENTER_SOUND_PATH = "audio/enter.mp3";

    // Core components
    private final AdventCalendarGame adventGame;
    private final OrthographicCamera camera;
    private Viewport viewport;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final ThemeManager themeManager;

    // Textures
    private final Texture foregroundTexture;
    private final Texture shadowTexture;
    private final Texture arrowTexture;
    private final ObjectMap<String, Texture> themeIconTextures = new ObjectMap<>();
    private final ObjectMap<Integer, Texture> doorTextures = new ObjectMap<>();

    // Sounds
    private Sound lockedSound;
    private Sound openSound;
    private Sound enterSound;

    // Door system
    private final Array<DoorPosition> originalDoorPositions = new Array<>();
    private final ObjectMap<Integer, Rectangle> boxes = new ObjectMap<>();
    private final ObjectMap<Integer, Float> doorSlideProgress = new ObjectMap<>();
    private final ObjectMap<Integer, Boolean> doorSliding = new ObjectMap<>();
    
    // Arrow animation
    private float arrowAnimationTime = 0f;
    private static final float ARROW_ANIMATION_SPEED = 2.0f;
    private static final float ARROW_AMPLITUDE = 8.0f;
    private static final float ARROW_ZOOM_MIN = 0.8f;  // Zoom minimum (en haut)
    private static final float ARROW_ZOOM_MAX = 1.2f; // Zoom maximum (en bas)

    // Camera and input
    private boolean isDragging = false;
    private boolean touchProcessed = false;
    private float lastTouchScreenX = 0f;
    private float initialTouchScreenX = 0f;
    private final Vector3 touchPos = new Vector3();

    /**
     * Door position data structure for JSON-based layout
     */
    private static class DoorPosition {
        public final int dayId;
        public final float originalX;
        public final float originalY;
        public final float originalWidth;
        public final float originalHeight;

        public DoorPosition(int dayId, float originalX, float originalY, float originalWidth, float originalHeight) {
            this.dayId = dayId;
            this.originalX = originalX;
            this.originalY = originalY;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }
    }

    /**
     * Constructor for AdventCalendarGame
     */
    public AdventCalendarScreen(AdventCalendarGame adventGame) {
        this(adventGame, adventGame);
    }

    /**
     * Constructor for Game (with cast)
     */
    public AdventCalendarScreen(Game game) {
        this(game, (AdventCalendarGame) game);
    }

    /**
     * Main constructor - all initialization logic centralized here
     */
    private AdventCalendarScreen(Game game, AdventCalendarGame adventGame) {
        Gdx.app.log("AdventCalendarScreen", "Initializing screen");
        
        this.adventGame = adventGame;
        this.themeManager = ThemeManager.getInstance();

        // Initialize core components avec le système centralisé
        this.camera = new OrthographicCamera();
        this.viewport = ViewportManager.createViewport(camera);
        this.camera.position.x = DisplayConfig.WORLD_WIDTH / 2;
        this.camera.update();

        this.batch = new SpriteBatch();
        this.font = new BitmapFont();

        // Load resources
        this.foregroundTexture = loadTextureSafe(FOREGROUND_TEXTURE_PATH);
        this.shadowTexture = loadTextureSafe(SHADOW_TEXTURE_PATH);
        this.arrowTexture = loadTextureSafe(ARROW_TEXTURE_PATH);
        loadSounds();

        // Initialize door system
        initializeDoorSystem();

        // Set initial window size
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    /**
     * Load all sound resources
     */
    private void loadSounds() {
        this.lockedSound = loadSoundSafe(LOCKED_SOUND_PATH);
        this.openSound = loadSoundSafe(OPEN_SOUND_PATH);
        this.enterSound = loadSoundSafe(ENTER_SOUND_PATH);
    }

    /**
     * Initialize the door system from JSON data
     */
    private void initializeDoorSystem() {
        try {
            JsonReader jsonReader = new JsonReader();
            JsonValue maskData = jsonReader.parse(Gdx.files.internal(MASK_JSON_PATH));
            
            int doorNumber = 1;
            Array<Integer> shuffledDays = adventGame.getGameState().getShuffledDays();
            
            for (JsonValue rowData = maskData.child; rowData != null; rowData = rowData.next) {
                JsonValue rectangles = rowData.get("rectangles");
                
                for (JsonValue rectData = rectangles.child; rectData != null; rectData = rectData.next) {
                    int dayId = shuffledDays.get(doorNumber - 1);
                    
                    // Convert absolute JSON coordinates to relative (0-1) coordinates
                    float relativeX = (float) rectData.getInt("x") / foregroundTexture.getWidth();
                    float relativeY = 1.0f - ((float) (rectData.getInt("y") + rectData.getInt("height")) / foregroundTexture.getHeight());
                    float relativeWidth = (float) rectData.getInt("width") / foregroundTexture.getWidth();
                    float relativeHeight = (float) rectData.getInt("height") / foregroundTexture.getHeight();
                    
                    // Store door position and initialize state
                    originalDoorPositions.add(new DoorPosition(dayId, relativeX, relativeY, relativeWidth, relativeHeight));
                    boxes.put(dayId, new Rectangle());
                    doorSlideProgress.put(dayId, 0.0f);
                    doorSliding.put(dayId, false);
                    
                    // Load door texture
                    String doorPath = DOOR_IMAGE_PATH_PREFIX + doorNumber + ".png";
                    doorTextures.put(dayId, loadTextureSafe(doorPath));
                    
                    doorNumber++;
                }
            }
            
            updateDoorPositions();
            
            // Déverrouiller la première case (jour 1 - case de départ)
            int startingDay = 1;
            Gdx.app.log("AdventCalendarScreen", "Is day 1 unlocked before init: " + adventGame.isUnlocked(startingDay));
            if (!adventGame.isUnlocked(startingDay)) {
                adventGame.unlock(startingDay);
                Gdx.app.log("AdventCalendarScreen", "Unlocked starting door (day " + startingDay + ")");
            }
            Gdx.app.log("AdventCalendarScreen", "Is day 1 unlocked after init: " + adventGame.isUnlocked(startingDay));
            
            Gdx.app.log("AdventCalendarScreen", "Loaded " + doorTextures.size + " doors");
            
        } catch (Exception e) {
            Gdx.app.error("AdventCalendarScreen", "Failed to initialize door system", e);
        }
    }

    @Override
    public void render(float delta) {
        clearScreen();
        updateDoorAnimations(delta);
        updateArrowAnimation(delta);
        updateCamera();
        
        batch.begin();
        renderPaintingIcons();
        renderForegroundAndShadow();
        renderDoors();
        renderArrowAnimations();
        batch.end();
        
        handleInput();
    }

    /**
     * Clear the screen with black background
     */
    private void clearScreen() {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /**
     * Update camera state
     */
    private void updateCamera() {
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }

    /**
     * Render painting icons in background
     * Only show images when the game is completed (score > 0)
     */
    private void renderPaintingIcons() {
        for (int dayId = 1; dayId <= 24; dayId++) {
            Rectangle box = boxes.get(dayId);
            if (box == null) continue;

            // Only show the image if the game is completed (score > 0)
            if (adventGame.getScore(dayId) > 0) {
                Texture themeIcon = getThemeIconForDay(dayId);
                if (themeIcon != null) {
                    renderScaledIcon(themeIcon, box);
                }
            }
        }
    }

    /**
     * Render an icon scaled to fill its box while maintaining aspect ratio
     */
    private void renderScaledIcon(Texture icon, Rectangle box) {
        float iconAspectRatio = (float) icon.getWidth() / icon.getHeight();
        float boxAspectRatio = box.width / box.height;
        
        float iconWidth, iconHeight;
        if (iconAspectRatio > boxAspectRatio) {
            iconHeight = box.height;
            iconWidth = iconHeight * iconAspectRatio;
        } else {
            iconWidth = box.width;
            iconHeight = iconWidth / iconAspectRatio;
        }
        
        float iconX = box.x + (box.width - iconWidth) / 2;
        float iconY = box.y + (box.height - iconHeight) / 2;
        
        batch.draw(icon, iconX, iconY, iconWidth, iconHeight);
    }

    /**
     * Render foreground image and shadow overlay
     */
    private void renderForegroundAndShadow() {
        ForegroundDimensions dims = calculateForegroundDimensions();
        batch.draw(foregroundTexture, dims.x, dims.y, dims.width, dims.height);
        batch.draw(shadowTexture, dims.x, dims.y, dims.width, dims.height);
    }

    /**
     * Calculate foreground image dimensions and position
     */
    private ForegroundDimensions calculateForegroundDimensions() {
        float aspectRatio = (float) foregroundTexture.getWidth() / foregroundTexture.getHeight();
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        float width = worldHeight * aspectRatio;
        float x = (worldWidth - width) / 2;
        return new ForegroundDimensions(x, 0, width, worldHeight);
    }

    /**
     * Simple data structure for foreground dimensions
     */
    private static class ForegroundDimensions {
        final float x, y, width, height;
        
        ForegroundDimensions(float x, float y, float width, float height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }

    /**
     * Render doors with clipping animation
     */
    private void renderDoors() {
        for (int dayId = 1; dayId <= 24; dayId++) {
            Rectangle box = boxes.get(dayId);
            Texture doorTexture = doorTextures.get(dayId);
            
            if (box == null || doorTexture == null) continue;
            
            DoorState state = getDoorState(dayId);
            if (state.shouldRender()) {
                if (state.isAnimating()) {
                    renderDoorWithClipping(doorTexture, box, state.slideProgress);
            } else {
                    renderDoorNormal(doorTexture, box, state.slideProgress);
                }
            }
        }
    }

    /**
     * Get current state of a door
     */
    private DoorState getDoorState(int dayId) {
        boolean isLocked = !adventGame.isUnlocked(dayId);
        boolean isVisited = adventGame.isVisited(dayId);
        boolean isSliding = doorSliding.get(dayId, false);
        float slideProgress = doorSlideProgress.get(dayId, 0.0f);
        
        return new DoorState(isLocked, isVisited, isSliding, slideProgress);
    }

    /**
     * Door state encapsulation
     */
    private static class DoorState {
        final boolean isLocked, isVisited, isSliding;
        final float slideProgress;
        
        DoorState(boolean isLocked, boolean isVisited, boolean isSliding, float slideProgress) {
            this.isLocked = isLocked;
            this.isVisited = isVisited;
            this.isSliding = isSliding;
            this.slideProgress = slideProgress;
        }
        
        boolean shouldRender() {
            return isLocked || (!isVisited && (isSliding || slideProgress < 1.0f));
        }
        
        boolean isAnimating() {
            return isSliding && slideProgress > 0.0f && slideProgress < 1.0f;
        }
    }

    /**
     * Render door with clipping animation
     */
    private void renderDoorWithClipping(Texture doorTexture, Rectangle box, float slideProgress) {
        batch.flush();
        
        float visibleHeight = box.height * (1.0f - slideProgress);
        float clipTop = box.y + visibleHeight;
        
        Vector3 bottomLeft = new Vector3(box.x, box.y, 0);
        Vector3 topRight = new Vector3(box.x + box.width, clipTop, 0);
        camera.project(bottomLeft);
        camera.project(topRight);
        
        int scissorX = (int) bottomLeft.x;
        int scissorY = (int) bottomLeft.y;
        int scissorWidth = (int) (topRight.x - bottomLeft.x);
        int scissorHeight = (int) (topRight.y - bottomLeft.y);
        
        if (scissorHeight > 0) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            HdpiUtils.glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
            
            batch.draw(doorTexture, box.x, box.y, box.width, box.height);
            
            batch.flush();
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
    }

    /**
     * Render door normally (without clipping)
     */
    private void renderDoorNormal(Texture doorTexture, Rectangle box, float slideProgress) {
        float slideOffset = box.height * slideProgress;
        float doorY = box.y - slideOffset;
        batch.draw(doorTexture, box.x, doorY, box.width, box.height);
    }

    /**
     * Update door sliding animations
     */
    private void updateDoorAnimations(float delta) {
        for (int dayId = 1; dayId <= 24; dayId++) {
            if (doorSliding.get(dayId, false)) {
                float progress = doorSlideProgress.get(dayId, 0.0f) + DOOR_SLIDE_SPEED * delta;
                
                if (progress >= 1.0f) {
                    progress = 1.0f;
                    doorSliding.put(dayId, false);
                    // Marquer automatiquement comme visitée à la fin de l'animation
                    adventGame.setVisited(dayId, true);
                }
                
                doorSlideProgress.put(dayId, progress);
            }
        }
    }

    /**
     * Update arrow animation timing
     */
    private void updateArrowAnimation(float delta) {
        arrowAnimationTime += delta * ARROW_ANIMATION_SPEED;
    }

    /**
     * Render arrow animations in foreground for all opened doors but not completed games
     */
    private void renderArrowAnimations() {
        for (int dayId = 1; dayId <= 24; dayId++) {
            Rectangle box = boxes.get(dayId);
            if (box == null) continue;

            // Show arrow animation only for opened doors (visited) but not completed games
            if (adventGame.isVisited(dayId) && adventGame.getScore(dayId) == 0) {
                renderArrowAnimation(box);
            }
        }
    }

    /**
     * Render arrow animation in the center of the box
     */
    private void renderArrowAnimation(Rectangle box) {
        if (arrowTexture == null) return;
        
        // Calculate sinusoidal movement
        float offsetY = (float) Math.sin(arrowAnimationTime) * ARROW_AMPLITUDE;
        
        // Calculate zoom effect based on position
        // When arrow is at top (sin = -1), zoom is maximum
        // When arrow is at bottom (sin = 1), zoom is minimum
        float sinValue = (float) Math.sin(arrowAnimationTime);
        float zoomFactor = ARROW_ZOOM_MAX - (ARROW_ZOOM_MAX - ARROW_ZOOM_MIN) * (sinValue + 1.0f) / 2.0f;
        
        // Calculate arrow position (in lower part of box)
        float baseArrowSize = Math.min(box.width, box.height) * 0.5f; // 50% of box size
        float arrowSize = baseArrowSize * zoomFactor;
        float arrowX = box.x + (box.width - arrowSize) / 2;
        // Position in lower part: more towards the bottom
        float lowerPartCenter = box.y + box.height * 0.10f; // 5% down the box (lower part)
        float arrowY = lowerPartCenter - arrowSize / 2 + offsetY;
        
        // Draw the arrow
        batch.draw(arrowTexture, arrowX, arrowY, arrowSize, arrowSize);
    }

    /**
     * Handle user input (touch/mouse)
     */
    private void handleInput() {
        if (TransitionScreen.isTransitionActive()) return;
        
        // Gestion des raccourcis fullscreen
        handleGlobalInput();
        
        if (Gdx.input.justTouched()) {
            handleTouchStart();
        }
        
        if (Gdx.input.isTouched()) {
            handleTouchContinue();
        } else if (!touchProcessed) {
            handleTouchEnd();
        }
    }

    /**
     * Handle global input (fullscreen toggles)
     * Délègue au gestionnaire centralisé
     */
    private void handleGlobalInput() {
        InputManager.handleGlobalInput();
    }

    /**
     * Toggle between fullscreen and windowed mode
     * @deprecated Cette méthode n'est plus utilisée, la gestion est centralisée dans WindowManager
     */
    @Deprecated
    private void toggleFullscreen() {
        // Cette méthode est conservée pour compatibilité mais n'est plus utilisée
        // La gestion est maintenant centralisée dans WindowManager via InputManager
    }

    /**
     * Handle start of touch
     */
    private void handleTouchStart() {
        touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPos);
        lastTouchScreenX = Gdx.input.getX();
        initialTouchScreenX = Gdx.input.getX();
        isDragging = false;
        touchProcessed = false;
    }

    /**
     * Handle continuing touch (drag detection and camera movement)
     */
    private void handleTouchContinue() {
        float currentX = Gdx.input.getX();
        float deltaX = Math.abs(currentX - lastTouchScreenX);
        
        if (!isDragging && deltaX > DRAG_THRESHOLD) {
            isDragging = true;
        }
        
        if (isDragging) {
            float screenDelta = lastTouchScreenX - currentX;
            float worldDelta = screenDelta * (viewport.getWorldWidth() / Gdx.graphics.getWidth());
            camera.position.x = constrainCameraX(camera.position.x + worldDelta);
            camera.update();
            lastTouchScreenX = currentX;
        }
    }

    /**
     * Handle end of touch (process clicks)
     */
    private void handleTouchEnd() {
        // Vérifier si il y a eu un mouvement significatif depuis le début du toucher
        float totalMovement = Math.abs(Gdx.input.getX() - initialTouchScreenX);
        
        // Ne traiter comme un clic que s'il n'y a pas eu de drag ET pas de mouvement significatif
        if (!isDragging && totalMovement <= DRAG_THRESHOLD) {
            processClick();
        }
        
        touchProcessed = true;
        isDragging = false;
    }

    /**
     * Process click on doors
     */
    private void processClick() {
        for (int dayId = 1; dayId <= 24; dayId++) {
            Rectangle box = boxes.get(dayId);
            if (box.contains(touchPos.x, touchPos.y)) {
                handleDoorClick(dayId);
                break;
            }
        }
    }

    /**
     * Handle click on specific door with state machine logic
     */
    private void handleDoorClick(int dayId) {
        DoorState state = getDoorState(dayId);
        
        // Debug: Afficher l'état complet de la porte
        Gdx.app.log("AdventCalendarScreen", "Door " + dayId + " clicked - isLocked: " + state.isLocked + 
                   ", isVisited: " + state.isVisited + ", isSliding: " + state.isSliding + 
                   ", slideProgress: " + state.slideProgress);
        
        if (state.isLocked) {
            // Porte verrouillée : Juste jouer le son, pas de déverrouillage par clic
            Gdx.app.log("AdventCalendarScreen", "Door " + dayId + " is locked - playing locked sound");
            playSound(lockedSound);
        } else if (!state.isVisited && !state.isSliding) {
            // Porte déverrouillée : Démarrer l'animation d'ouverture
            Gdx.app.log("AdventCalendarScreen", "Starting opening animation for door " + dayId);
            doorSliding.put(dayId, true);
            playSound(openSound);
        } else if (state.isVisited) {
            // Porte ouverte : Lancer le jeu
            Gdx.app.log("AdventCalendarScreen", "Launching game for door " + dayId);
            playSound(enterSound);
            adventGame.launchGame(dayId);
        } else {
            // Debug: État non géré
            Gdx.app.log("AdventCalendarScreen", "Door " + dayId + " click ignored - unhandled state");
        }
    }

    /**
     * Get theme icon texture for a day
     */
    private Texture getThemeIconForDay(int dayId) {
        String cacheKey = String.valueOf(dayId);
        if (themeIconTextures.containsKey(cacheKey)) {
            return themeIconTextures.get(cacheKey);
        }
        
        Theme theme = getThemeForDay(dayId);
        if (theme != null) {
            try {
                String iconPath = theme.getFullImagePath().replace("full", "icon").replace(".jpg", ".png");
                Texture iconTexture = new Texture(Gdx.files.internal(iconPath));
                themeIconTextures.put(cacheKey, iconTexture);
                return iconTexture;
            } catch (Exception e) {
                Gdx.app.error("AdventCalendarScreen", "Failed to load icon for day " + dayId, e);
            }
        }
        
        return null;
    }

    /**
     * Get theme for a specific day
     */
    private Theme getThemeForDay(int dayId) {
        Theme theme = themeManager.getThemeByDay(dayId);
        if (theme == null && themeManager.getThemeCount() > 0) {
            int themeIndex = (dayId - 1) % themeManager.getThemeCount();
            theme = themeManager.getAllThemes().get(themeIndex);
        }
        return theme;
    }

    /**
     * Update door positions based on current foreground scaling
     */
    private void updateDoorPositions() {
        ForegroundDimensions dims = calculateForegroundDimensions();
        
        for (DoorPosition doorPos : originalDoorPositions) {
            float doorX = dims.x + (doorPos.originalX * dims.width);
            float doorY = dims.y + (doorPos.originalY * dims.height);
            float doorWidth = doorPos.originalWidth * dims.width;
            float doorHeight = doorPos.originalHeight * dims.height;
            
            Rectangle doorRect = boxes.get(doorPos.dayId);
            if (doorRect != null) {
                doorRect.set(doorX, doorY, doorWidth, doorHeight);
            }
        }
    }

    /**
     * Constrain camera X position within foreground bounds
     */
    private float constrainCameraX(float newCameraX) {
        ForegroundDimensions dims = calculateForegroundDimensions();
        float halfViewport = viewport.getWorldWidth() / 2;
        
        if (dims.width <= viewport.getWorldWidth()) {
            return dims.x + dims.width / 2;
        }
        
        float minX = dims.x + halfViewport;
        float maxX = dims.x + dims.width - halfViewport;
        return Math.max(minX, Math.min(maxX, newCameraX));
    }

    /**
     * Safely play a sound
     */
    private void playSound(Sound sound) {
        if (sound != null) {
            sound.play();
        }
    }

    /**
     * Load texture with fallback
     */
    private Texture loadTextureSafe(String path) {
        try {
            return new Texture(Gdx.files.internal(path));
        } catch (Exception e) {
            Gdx.app.error("AdventCalendarScreen", "Failed to load texture: " + path, e);
            return createFallbackTexture();
        }
    }

    /**
     * Create fallback texture for failed loads
     */
    private Texture createFallbackTexture() {
        Pixmap pixmap = new Pixmap(FALLBACK_TEXTURE_SIZE, FALLBACK_TEXTURE_SIZE, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 0, 0, 1);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /**
     * Load sound with error handling
     */
    private Sound loadSoundSafe(String path) {
        try {
            return Gdx.audio.newSound(Gdx.files.internal(path));
        } catch (Exception e) {
            Gdx.app.error("AdventCalendarScreen", "Failed to load sound: " + path, e);
            return null;
        }
    }

    @Override
    public void resize(int width, int height) {
        this.viewport = ViewportManager.updateViewportWithReconfiguration(viewport, width, height);
        camera.position.x = constrainCameraX(DisplayConfig.WORLD_WIDTH / 2f);
        camera.update();
        updateDoorPositions();
    }

    @Override
    public void dispose() {
        // Dispose core components
        batch.dispose();
        font.dispose();
        foregroundTexture.dispose();
        shadowTexture.dispose();

        // Dispose sounds
        if (lockedSound != null) lockedSound.dispose();
        if (openSound != null) openSound.dispose();
        if (enterSound != null) enterSound.dispose();

        // Dispose door textures
        for (Texture texture : doorTextures.values()) {
            texture.dispose();
        }

        // Dispose theme icon textures
        for (Texture texture : themeIconTextures.values()) {
            texture.dispose();
        }
    }

    @Override public void show() {}
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
}