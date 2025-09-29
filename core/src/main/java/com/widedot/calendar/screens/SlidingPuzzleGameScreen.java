package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.display.DisplayConfig;
import com.widedot.calendar.display.UIPositionManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.data.Theme;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.audio.Sound;
import com.widedot.calendar.config.Config;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Écran de jeu pour le puzzle coulissant
 */
public class SlidingPuzzleGameScreen extends GameScreen {
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Rectangle backButton;
    private final Rectangle solveButton;
    private final Rectangle infoButton;
    private final Rectangle closeButton;
    private final Color infoPanelColor;
    private boolean showInfoPanel;
    private final Texture whiteTexture;
    private Color backgroundColor;
    private boolean isTestMode; // Ajout d'une variable pour le mode test
    
    // Sons
    private Sound solveSound;
    private Sound slidingSound;
    private static final String SOLVE_SOUND_PATH = "audio/win.mp3";
    private static final String SLIDING_SOUND_PATH = "audio/sliding.mp3";
    
    // Paramètres du jeu provenant de la configuration
    private int gridSize;
    private final float GRID_MARGIN = 80; // Marge autour de la grille
    private final float SPACING_RATIO = 0.05f; // Espacement fixe de 5% de la taille des tuiles
    private float animationSpeed; // Vitesse de l'animation (tuiles par seconde)
    private int shuffleMoves; // Nombre de mouvements pour mélanger le puzzle
    
    private final int[] puzzleState; // État actuel du puzzle (index = position, valeur = numéro de tuile)
    private final Rectangle[] gridZones; // Positions des tuiles (index = position)
    private float tileSize; // Taille dynamique des tuiles
    private float tileSpacing; // Espacement dynamique entre les tuiles
    private int emptyTileIndex; // Position de la case vide
    private boolean isPuzzleSolved; // Indique si le puzzle est résolu
    private Texture puzzleTexture; // Texture du puzzle
    private TextureRegion[] puzzleTiles; // Régions de texture pour chaque tuile
    private Theme theme; // Le thème du jeu

    // Variables d'animation
    private static class AnimationState {
        // Constantes de timing
        private static final float FADE_IN_DURATION = 1f;
        private static final float MERGE_DURATION = 1f;
        private static final float PHASE_DELAY = 0f;
        private static final float PIXELS_PER_SECOND = 500f;
        private static final float VICTORY_MESSAGE_DELAY = 1f;

        // État de l'animation
        private boolean isActive = false;
        private boolean isComplete = false;
        private float progress = 0f;
        private float totalDuration = 0f;
        private float irisOpenDuration = 0f;
        private float victoryMessageTimer = 0f;

        // Progression des phases
        private float fadeProgress = 0f;
        private float mergeProgress = 0f;
        private float irisOpenProgress = 0f;

        public void start() {
            isActive = true;
            isComplete = false;
            progress = 0f;
            victoryMessageTimer = 0f;
            fadeProgress = 0f;
            mergeProgress = 0f;
            irisOpenProgress = 0f;
        }

        public void update(float delta) {
            if (!isActive) return;
            
            progress += delta;
            
            // Calculer les progressions de chaque phase
            fadeProgress = Math.min(1f, progress / FADE_IN_DURATION);
            mergeProgress = Math.max(0f, Math.min(1f, (progress - FADE_IN_DURATION - PHASE_DELAY) / MERGE_DURATION));
            
            float irisOpenStart = FADE_IN_DURATION + MERGE_DURATION + PHASE_DELAY;
            if (progress >= irisOpenStart) {
                irisOpenProgress = Math.min(1f, (progress - irisOpenStart) / irisOpenDuration);
            }

            if (progress >= totalDuration) {
                isActive = false;
                isComplete = true;
                fadeProgress = 1f;
                mergeProgress = 1f;
                irisOpenProgress = 1f;
            }
        }

        public void updateVictoryMessageTimer(float delta) {
            if (isComplete) {
                victoryMessageTimer += delta;
            }
        }

        public boolean shouldShowVictoryMessage() {
            return isComplete && victoryMessageTimer >= VICTORY_MESSAGE_DELAY;
        }

        public void calculateDurations(float imageWidth, float imageHeight, Theme.CropInfo squareCrop) {
            // Calculer la durée de l'ouverture rectangulaire
            float totalDistance = Math.max(
                imageWidth - squareCrop.getWidth(),
                imageHeight - squareCrop.getHeight()
            );
            irisOpenDuration = totalDistance / PIXELS_PER_SECOND;
            
            // Calculer la durée totale
            totalDuration = FADE_IN_DURATION + MERGE_DURATION + PHASE_DELAY + irisOpenDuration;
        }

        public boolean isActive() { return isActive; }
        public boolean isComplete() { return isComplete; }
        public float getFadeProgress() { return fadeProgress; }
        public float getMergeProgress() { return mergeProgress; }
        public float getIrisOpenProgress() { return irisOpenProgress; }
    }

    // Variables d'animation de victoire
    private final AnimationState animationState = new AnimationState();
    private Texture fullImageTexture;

    // Variables d'animation des tuiles
    private static class TileAnimation {
        private boolean isActive = false;
        private float progress = 0f;
        private int tileIndex;
        private Vector3 start = new Vector3();
        private Vector3 end = new Vector3();

        public void start(int tileIndex, Vector3 start, Vector3 end) {
            this.isActive = true;
            this.progress = 0f;
            this.tileIndex = tileIndex;
            this.start.set(start);
            this.end.set(end);
        }

        public void update(float delta, float speed) {
            if (!isActive) return;
            
            progress += delta * speed;
            if (progress >= 1f) {
                isActive = false;
                progress = 1f;
            }
        }

        public boolean isActive() { return isActive; }
        public float getProgress() { return progress; }
        public int getTileIndex() { return tileIndex; }
        public Vector3 getCurrentPosition() {
            Vector3 current = new Vector3();
            current.x = start.x + (end.x - start.x) * progress;
            current.y = start.y + (end.y - start.y) * progress;
            return current;
        }
    }

    private final TileAnimation tileAnimation = new TileAnimation();

    private boolean isAdjacent(int tileIndex1, int tileIndex2) {
        // Convertir les indices linéaires en coordonnées de grille
        int row1 = tileIndex1 / gridSize;
        int col1 = tileIndex1 % gridSize;
        int row2 = tileIndex2 / gridSize;
        int col2 = tileIndex2 % gridSize;

        // Vérifier l'adjacence horizontale ou verticale
        boolean horizontalAdjacent = row1 == row2 && Math.abs(col1 - col2) == 1;
        boolean verticalAdjacent = col1 == col2 && Math.abs(row1 - row2) == 1;

        return horizontalAdjacent || verticalAdjacent;
    }

    /**
     * Initialise un état du puzzle résoluble en simulant des mouvements valides
     */
    private void initializeSolvablePuzzle() {
        // Commencer avec l'état résolu
        for (int i = 0; i < puzzleState.length; i++) {
            puzzleState[i] = i;
        }
        emptyTileIndex = puzzleState.length - 1;

        // Nombre de mouvements aléatoires à effectuer
        int numMoves = shuffleMoves;

        // Effectuer des mouvements aléatoires valides
        for (int i = 0; i < numMoves; i++) {
            // Trouver les tuiles adjacentes à la case vide
            int row = emptyTileIndex / gridSize;
            int col = emptyTileIndex % gridSize;
            int[] possibleMoves = new int[4];
            int numPossibleMoves = 0;

            // Vérifier les 4 directions possibles
            if (row > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - gridSize; // Haut
            if (row < gridSize - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + gridSize; // Bas
            if (col > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - 1; // Gauche
            if (col < gridSize - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + 1; // Droite

            // Choisir un mouvement aléatoire parmi les possibles
            if (numPossibleMoves > 0) {
                int moveIndex = (int)(Math.random() * numPossibleMoves);
                int tileToMove = possibleMoves[moveIndex];

                // Échanger la tuile avec la case vide
                int temp = puzzleState[tileToMove];
                puzzleState[tileToMove] = puzzleState[emptyTileIndex];
                puzzleState[emptyTileIndex] = temp;
                emptyTileIndex = tileToMove;
            }
        }
    }

    /**
     * Constructeur avec paramètres dynamiques
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     * @param theme Le thème du jeu
     * @param parameters Les paramètres du jeu
     */
    public SlidingPuzzleGameScreen(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters) {
        super(dayId, game);
        
        this.theme = theme;
        
        // Initialiser les paramètres avec des valeurs par défaut
        this.gridSize = 4;
        this.animationSpeed = 10f;
        this.shuffleMoves = 200;
        this.backgroundColor = new Color(0, 0, 0, 1);
        
        // Vérifier si on est en mode test via Config
        this.isTestMode = Config.getInstance().isTestModeEnabled();
        System.out.println("Mode test: " + isTestMode);
        
        // Appliquer les paramètres spécifiques s'ils existent
        if (parameters != null) {
            // Taille de la grille
            if (parameters.containsKey("size")) {
                int size = ((Number)parameters.get("size")).intValue();
                if (size >= 2 && size <= 6) {
                    this.gridSize = size;
                }
            }
            
            // Couleur de fond
            if (parameters.containsKey("bgColor")) {
                String bgColor = (String)parameters.get("bgColor");
                String[] parts = bgColor.split(",");
                if (parts.length == 3) {
                    try {
                        float r = Integer.parseInt(parts[0]) / 255f;
                        float g = Integer.parseInt(parts[1]) / 255f;
                        float b = Integer.parseInt(parts[2]) / 255f;
                        this.backgroundColor = new Color(r, g, b, 1);
                    } catch (NumberFormatException e) {
                        System.err.println("Format de couleur invalide: " + bgColor);
                    }
                }
            }
            
            // Nombre de mouvements pour mélanger
            if (parameters.containsKey("shuffle")) {
                this.shuffleMoves = ((Number)parameters.get("shuffle")).intValue();
            }
            
            // Vitesse d'animation
            if (parameters.containsKey("animationSpeed")) {
                this.animationSpeed = ((Number)parameters.get("animationSpeed")).floatValue();
            }
        }
        
        // Initialisation des couleurs et éléments UI
        this.font = new BitmapFont();
        // Utiliser une échelle entière pour éviter les problèmes d'alignement de pixels
        font.getData().setScale(1.0f);
        this.layout = new GlyphLayout();
        
        // Initialiser les boutons
        this.backButton = new Rectangle(20, 20, 100, 50);
        this.solveButton = new Rectangle(viewport.getWorldWidth() - 120, 20, 100, 50);
        this.infoButton = new Rectangle(viewport.getWorldWidth() - 70, viewport.getWorldHeight() - 70, 50, 50);
        this.closeButton = new Rectangle(viewport.getWorldWidth() - 50, viewport.getWorldHeight() - 50, 30, 30);
        this.infoPanelColor = new Color(0.3f, 0.3f, 0.3f, 0.8f);
        this.showInfoPanel = false;
        
        System.out.println("Création du puzzle coulissant pour le jour " + dayId);
        System.out.println("Paramètres: gridSize=" + gridSize + ", shuffle=" + shuffleMoves);

        // Créer une texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        this.puzzleState = new int[gridSize * gridSize];
        this.gridZones = new Rectangle[gridSize * gridSize];
        
        // Initialiser tous les Rectangle du tableau gridZones
        for (int i = 0; i < gridZones.length; i++) {
            gridZones[i] = new Rectangle(0, 0, 1, 1); // Création avec valeurs par défaut
        }
        
        this.emptyTileIndex = gridSize * gridSize - 1;
        
        // Charger le son de résolution
        try {
            this.solveSound = Gdx.audio.newSound(Gdx.files.internal(SOLVE_SOUND_PATH));
            this.slidingSound = Gdx.audio.newSound(Gdx.files.internal(SLIDING_SOUND_PATH));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des sons: " + e.getMessage());
        }

        System.out.println("Constructeur de SlidingPuzzleGameScreen terminé");
    }

    @Override
    protected Theme loadTheme(int day) {
        System.out.println("Chargement de la ressource graphique pour le jour " + day);
        
        // Charger la texture du puzzle
        String fullImagePath = this.theme.getFullImagePath();
        if (fullImagePath == null || fullImagePath.isEmpty()) {
            throw new IllegalStateException("Le chemin d'image du thème est invalide");
        }
        
        try {
            System.out.println("Chargement de la texture depuis: " + fullImagePath);
            puzzleTexture = new Texture(Gdx.files.internal(fullImagePath));
            fullImageTexture = new Texture(Gdx.files.internal(fullImagePath));
            
            // Maintenant que tous les paramètres sont initialisés et que la texture est chargée,
            // nous pouvons créer les tuiles
            createPuzzleTiles();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la texture: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException("Erreur lors du chargement de la texture: " + e.getMessage(), e);
        }
        
        return this.theme;
    }

    /**
     * Crée les régions de texture pour chaque tuile du puzzle
     */
    private void createPuzzleTiles() {
        if (puzzleTexture == null) {
            throw new IllegalStateException("La texture du puzzle n'a pas été chargée");
        }

        puzzleTiles = new TextureRegion[gridSize * gridSize];
        
        // Récupérer les informations de recadrage
        Theme.CropInfo squareCrop = theme.getSquareCrop();
        if (squareCrop == null) {
            throw new IllegalStateException("Les informations de recadrage ne sont pas disponibles");
        }
        
        // Calculer la taille des tuiles dans la zone recadrée
        int tileWidth = squareCrop.getWidth() / gridSize;
        int tileHeight = squareCrop.getHeight() / gridSize;

        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int index = row * gridSize + col;
                // Inverser la ligne pour correspondre à l'orientation de la grille
                int textureRow = gridSize - 1 - row;
                puzzleTiles[index] = new TextureRegion(puzzleTexture, 
                    squareCrop.getX() + col * tileWidth, 
                    squareCrop.getY() + textureRow * tileHeight, 
                    tileWidth, 
                    tileHeight);
            }
        }
    }

    @Override
    protected void initializeGame() {
        System.out.println("Initialisation du puzzle coulissant pour le jour " + dayId);

        // Vérifier que la texture est chargée
        if (whiteTexture == null) {
            System.err.println("ERREUR: Texture du puzzle non chargée");
            return;
        }

        // Calculer la taille des tuiles et l'espacement en fonction de la taille de la fenêtre
        float availableWidth = viewport.getWorldWidth() - (2 * GRID_MARGIN);
        float availableHeight = viewport.getWorldHeight() - (2 * GRID_MARGIN);
        
        // Calculer la taille maximale possible pour une tuile
        float maxTileSize = Math.min(
            availableWidth / (gridSize + (gridSize - 1) * SPACING_RATIO),
            availableHeight / (gridSize + (gridSize - 1) * SPACING_RATIO)
        );
        
        tileSize = maxTileSize;
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float totalGridSize = gridSize * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - totalGridSize) / 2;
        float marginY = (viewport.getWorldHeight() - totalGridSize) / 2;

        // Initialiser les positions des tuiles
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int positionIndex = row * gridSize + col;
                gridZones[positionIndex] = new Rectangle(
                    marginX + col * (tileSize + tileSpacing),
                    marginY + row * (tileSize + tileSpacing),
                    tileSize,
                    tileSize
                );
            }
        }

        // Initialiser un état du puzzle résoluble
        initializeSolvablePuzzle();

        // Calculer les durées d'animation
        if (fullImageTexture != null && theme != null && theme.getSquareCrop() != null) {
            animationState.calculateDurations(
                fullImageTexture.getWidth(),
                fullImageTexture.getHeight(),
                theme.getSquareCrop()
            );
        }

        // Positionner les boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
        infoButton.setPosition(viewport.getWorldWidth() - 70, viewport.getWorldHeight() - 70);
        closeButton.setPosition(viewport.getWorldWidth() - 50, viewport.getWorldHeight() - 50);
    }

    @Override
    protected void updateGame(float delta) {
        if (tileAnimation.isActive()) {
            tileAnimation.update(delta, animationSpeed);
            
            if (!tileAnimation.isActive()) {
                // Animation terminée
                int animatingTileIndex = tileAnimation.getTileIndex();

                // Échanger les tuiles dans l'état du puzzle
                int temp = puzzleState[animatingTileIndex];
                puzzleState[animatingTileIndex] = puzzleState[emptyTileIndex];
                puzzleState[emptyTileIndex] = temp;
                emptyTileIndex = animatingTileIndex;

                // Vérifier si le puzzle est résolu
                boolean solved = true;
                for (int i = 0; i < puzzleState.length; i++) {
                    if (puzzleState[i] != i) {
                        solved = false;
                        break;
                    }
                }

                if (solved) {
                    System.out.println("Puzzle résolu !");
                    isPuzzleSolved = true;
                    animationState.start();
                    
                    // Jouer le son de résolution
                    if (solveSound != null) {
                        solveSound.play();
                    }
                    
                    if (game instanceof AdventCalendarGame) {
                        AdventCalendarGame adventGame = (AdventCalendarGame) game;
                        adventGame.setScore(dayId, 100);
                        adventGame.setVisited(dayId, true);
                    }
                }
            }
        }

        // Mettre à jour l'animation de victoire
        animationState.update(delta);
        animationState.updateVictoryMessageTimer(delta);
    }

    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(backgroundColor);
        batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

        // Réinitialiser la couleur avant de dessiner les tuiles
        batch.setColor(Color.WHITE);

        // Dessiner les tuiles du puzzle
        if (puzzleTiles != null) {
            if (animationState.isActive() || animationState.isComplete()) {
                float fadeProgress = animationState.getFadeProgress();
                float mergeProgress = animationState.getMergeProgress();
                float irisOpenProgress = animationState.getIrisOpenProgress();

                // Si nous sommes dans la phase d'ouverture rectangulaire ou l'animation est terminée
                if (irisOpenProgress > 0 && fullImageTexture != null) {
                    renderIrisOpenEffect(irisOpenProgress);
                } else {
                    renderMergingTiles(fadeProgress, mergeProgress);
                }
            } else {
                // Rendu normal du puzzle
                renderNormalPuzzle();
            }
        }

        // Si le puzzle est résolu et l'animation de victoire est terminée, afficher le message de victoire
        if (isPuzzleSolved && animationState.shouldShowVictoryMessage()) {
            // Dessiner un fond semi-transparent pour le message
            float messageWidth = viewport.getWorldWidth() * 0.6f;
            float messageHeight = viewport.getWorldHeight() * 0.2f;
            float messageX = (viewport.getWorldWidth() - messageWidth) / 2;
            float messageY = (viewport.getWorldHeight() - messageHeight) / 2;

            batch.setColor(0, 0, 0, 0.7f);
            batch.draw(whiteTexture, messageX, messageY, messageWidth, messageHeight);

            // Afficher le message "Bravo !"
            font.setColor(1, 1, 1, 1);
            String victoryMessage = "Bravo !";
            layout.setText(font, victoryMessage);
            float textX = (viewport.getWorldWidth() - layout.width) / 2;
            float textY = messageY + messageHeight * 0.7f;
            // Arrondir les coordonnées pour éviter le flou
            font.draw(batch, victoryMessage, Math.round(textX), Math.round(textY));

            // Afficher un message pour indiquer de cliquer
            String clickMessage = "Cliquez pour retourner au menu";
            layout.setText(font, clickMessage);
            textX = (viewport.getWorldWidth() - layout.width) / 2;
            textY = messageY + messageHeight * 0.3f;
            // Arrondir les coordonnées pour éviter le flou
            font.draw(batch, clickMessage, Math.round(textX), Math.round(textY));
        }

        // Dessiner les boutons
        batch.setColor(0.5f, 0.5f, 0.5f, 1);
        
        // Dessiner le bouton Retour
        batch.draw(whiteTexture, backButton.x, backButton.y, backButton.width, backButton.height);
        
        // Dessiner le bouton Résoudre (en mode test uniquement)
        if (isTestMode) {
            batch.draw(whiteTexture, solveButton.x, solveButton.y, solveButton.width, solveButton.height);
        }
        
        // Dessiner le bouton Info
        batch.draw(whiteTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);

        // Dessiner le texte des boutons
        font.setColor(1, 1, 1, 1);
        
        // Texte du bouton Retour
        String returnText = "Retour";
        layout.setText(font, returnText);
        float returnTextX = backButton.x + (backButton.width - layout.width) / 2;
        float returnTextY = backButton.y + (backButton.height + layout.height) / 2;
        // Arrondir les coordonnées aux pixels entiers pour éviter le flou
        font.draw(batch, returnText, Math.round(returnTextX), Math.round(returnTextY));

        // Texte du bouton Résoudre (en mode test uniquement)
        if (isTestMode) {
            String solveText = "Résoudre";
            layout.setText(font, solveText);
            float solveTextX = solveButton.x + (solveButton.width - layout.width) / 2;
            float solveTextY = solveButton.y + (solveButton.height + layout.height) / 2;
            // Arrondir les coordonnées aux pixels entiers pour éviter le flou
            font.draw(batch, solveText, Math.round(solveTextX), Math.round(solveTextY));
        }

        // Texte du bouton Info
        String infoText = "Info";
        layout.setText(font, infoText);
        float infoTextX = infoButton.x + (infoButton.width - layout.width) / 2;
        float infoTextY = infoButton.y + (infoButton.height + layout.height) / 2;
        // Arrondir les coordonnées aux pixels entiers pour éviter le flou
        font.draw(batch, infoText, Math.round(infoTextX), Math.round(infoTextY));

        // Dessiner le panneau d'information si nécessaire
        if (showInfoPanel) {
            float panelWidth = 400;
            float panelHeight = 300;
            float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
            float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

            batch.setColor(infoPanelColor);
            batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

            // Dessiner les informations du tableau
            float textMargin = 20;
            float titleY = panelY + panelHeight - textMargin;
            float artistY = titleY - 40;
            float yearY = artistY - 40;
            float descriptionY = yearY - 80;

            font.setColor(1, 1, 1, 1);
            
            // Titre avec position arrondie pour éviter le flou
            layout.setText(font, theme.getTitle(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, Math.round(panelX + textMargin), Math.round(titleY));

            // Artiste avec position arrondie
            layout.setText(font, theme.getArtist(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, Math.round(panelX + textMargin), Math.round(artistY));

            // Année avec position arrondie
            layout.setText(font, String.valueOf(theme.getYear()), Color.WHITE, panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, Math.round(panelX + textMargin), Math.round(yearY));

            // Description avec position arrondie
            layout.setText(font, theme.getDescription(), Color.WHITE, panelWidth - 2 * textMargin, Align.center, true);
            font.draw(batch, layout, Math.round(panelX + textMargin), Math.round(descriptionY));
        }
    }

    private void renderIrisOpenEffect(float progress) {
        float screenWidth = viewport.getWorldWidth();
        float screenHeight = viewport.getWorldHeight();
        float imageWidth = fullImageTexture.getWidth();
        float imageHeight = fullImageTexture.getHeight();
        
        // Calculer le ratio pour s'assurer que l'image tient dans l'écran (zoom final)
        float finalScale = Math.min(
            screenWidth * 0.8f / imageWidth,
            screenHeight * 0.8f / imageHeight
        );
        
        Theme.CropInfo squareCrop = theme.getSquareCrop();
        if (squareCrop != null) {
            // Calculer le zoom initial pour correspondre à la taille des tuiles réunies
            float initialScale = (gridSize * tileSize) / squareCrop.getWidth();
            
            // Interpoler le zoom entre la valeur initiale et finale
            float currentScale = initialScale + (finalScale - initialScale) * progress;
            
            // Calculer les dimensions avec le zoom actuel
            float scaledWidth = imageWidth * currentScale;
            float scaledHeight = imageHeight * currentScale;
            float x = (screenWidth - scaledWidth) / 2;
            float y = (screenHeight - scaledHeight) / 2;

            // Calculer les dimensions de la zone recadrée dans l'image complète
            float cropX = squareCrop.getX() * currentScale;
            float cropY = squareCrop.getY() * currentScale;
            float cropWidth = squareCrop.getWidth() * currentScale;
            float cropHeight = squareCrop.getHeight() * currentScale;

            // Calculer les dimensions de l'ouverture rectangulaire
            float openWidth = cropWidth + (scaledWidth - cropWidth) * progress;
            float openHeight = cropHeight + (scaledHeight - cropHeight) * progress;
            float openX = x + (scaledWidth - openWidth) / 2;
            float openY = y + (scaledHeight - openHeight) / 2;

            // Dessiner l'image complète avec l'effet d'ouverture
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fullImageTexture, 
                openX, openY, openWidth, openHeight,
                (int)(squareCrop.getX() - (openWidth - cropWidth) / (2 * currentScale)),
                (int)(squareCrop.getY() - (openHeight - cropHeight) / (2 * currentScale)),
                (int)(openWidth / currentScale),
                (int)(openHeight / currentScale),
                false, false);
        }
    }

    private void renderMergingTiles(float fadeProgress, float mergeProgress) {
        // Dessiner la case vide avec fade in
        if (fadeProgress < 1f) {
            batch.setColor(1f, 1f, 1f, fadeProgress);
            batch.draw(puzzleTiles[emptyTileIndex], 
                gridZones[emptyTileIndex].x, 
                gridZones[emptyTileIndex].y, 
                gridZones[emptyTileIndex].width, 
                gridZones[emptyTileIndex].height);
        } else if (mergeProgress < 1f) {
            renderMergingTile(emptyTileIndex, mergeProgress);
        } else {
            renderFinalTile(emptyTileIndex);
        }
        
        // Dessiner toutes les tuiles avec l'espacement réduit
        batch.setColor(Color.WHITE);
        for (int i = 0; i < gridZones.length; i++) {
            if (i != emptyTileIndex) {
                if (mergeProgress < 1f) {
                    renderMergingTile(i, mergeProgress);
                } else {
                    renderFinalTile(i);
                }
            }
        }
    }

    private void renderMergingTile(int index, float mergeProgress) {
        int tileNumber = puzzleState[index];
        float x = gridZones[index].x;
        float y = gridZones[index].y;
        
        // Calculer la position finale (sans espacement)
        float gridX = gridZones[0].x;
        float gridY = gridZones[0].y;
        float finalX = gridX + (index % gridSize) * tileSize;
        float finalY = gridY + (index / gridSize) * tileSize;
        
        // Calculer le décalage pour maintenir l'image centrée
        float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float finalGridWidth = gridSize * tileSize;
        float finalGridHeight = gridSize * tileSize;
        
        // Calculer le décalage pour maintenir le centre
        float offsetX = (totalGridWidth - finalGridWidth) / 2;
        float offsetY = (totalGridHeight - finalGridHeight) / 2;
        
        // Interpoler la position en tenant compte du décalage
        x = x + (finalX - x + offsetX) * mergeProgress;
        y = y + (finalY - y + offsetY) * mergeProgress;
        
        batch.draw(puzzleTiles[tileNumber], x, y, tileSize, tileSize);
    }

    private void renderFinalTile(int index) {
        int tileNumber = puzzleState[index];
        float gridX = gridZones[0].x;
        float gridY = gridZones[0].y;
        float finalX = gridX + (index % gridSize) * tileSize;
        float finalY = gridY + (index / gridSize) * tileSize;
        
        // Calculer le décalage final
        float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
        float finalGridWidth = gridSize * tileSize;
        float finalGridHeight = gridSize * tileSize;
        float offsetX = (totalGridWidth - finalGridWidth) / 2;
        float offsetY = (totalGridHeight - finalGridHeight) / 2;
        
        batch.draw(puzzleTiles[tileNumber], finalX + offsetX, finalY + offsetY, tileSize, tileSize);
    }

    private void renderNormalPuzzle() {
        for (int i = 0; i < gridZones.length; i++) {
            if (i != emptyTileIndex && (!tileAnimation.isActive() || i != tileAnimation.getTileIndex())) {
                int tileNumber = puzzleState[i];
                batch.draw(puzzleTiles[tileNumber], 
                    gridZones[i].x, 
                    gridZones[i].y, 
                    gridZones[i].width, 
                    gridZones[i].height);
            }
        }

        // Dessiner la tuile en cours d'animation
        if (tileAnimation.isActive()) {
            int tileNumber = puzzleState[tileAnimation.getTileIndex()];
            Vector3 currentPos = tileAnimation.getCurrentPosition();
            batch.draw(puzzleTiles[tileNumber], currentPos.x, currentPos.y, tileSize, tileSize);
        }
    }

    @Override
    protected void handleInput() {
        // Désactiver les entrées pendant l'animation ou les transitions
        if (tileAnimation.isActive() || TransitionScreen.isTransitionActive()) {
            return;
        }

        if (Gdx.input.justTouched()) {
            System.out.println("Toucher détecté dans SlidingPuzzleGameScreen");

            float touchX = Gdx.input.getX();
            float touchY = Gdx.input.getY();

            // Convertir les coordonnées de l'écran en coordonnées du monde
            Vector3 worldPos = new Vector3(touchX, touchY, 0);
            viewport.unproject(worldPos);

            System.out.println("Position du toucher: (" + worldPos.x + ", " + worldPos.y + ")");

            // Si le puzzle est résolu, retourner au menu sur n'importe quel clic
            if (isPuzzleSolved) {
                System.out.println("Retour au menu après victoire");
                returnToMainMenu();
                return;
            }

            // Si le panneau d'info est visible, n'importe quel clic le ferme
            if (showInfoPanel) {
                System.out.println("Fermeture du panneau d'info");
                showInfoPanel = false;
                return;
            }

            if (backButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Retour cliqué");
                returnToMainMenu();
            } else if (isTestMode && solveButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Résoudre cliqué (mode test)");
                if (game instanceof AdventCalendarGame) {
                    AdventCalendarGame adventGame = (AdventCalendarGame) game;
                    adventGame.setScore(dayId, 100);
                    adventGame.setVisited(dayId, true);
                }
                // Jouer le son de résolution en mode test aussi
                if (solveSound != null) {
                    solveSound.play();
                }
                returnToMainMenu();
            } else if (infoButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Info cliqué");
                showInfoPanel = true;
            } else {
                // Gérer le déplacement des tuiles
                for (int positionIndex = 0; positionIndex < gridZones.length; positionIndex++) {
                    if (gridZones[positionIndex].contains(worldPos.x, worldPos.y)) {
                        // Ignorer le clic si c'est sur la case vide
                        if (positionIndex == emptyTileIndex) {
                            System.out.println("Clic sur la case vide (position " + positionIndex + ") - Ignoré");
                            break;
                        }

                        int tileNumber = puzzleState[positionIndex];
                        System.out.println("Tuile numéro " + tileNumber + " cliquée (position " + positionIndex + ")");
                        System.out.println("Case vide actuelle: position " + emptyTileIndex);

                        if (isAdjacent(positionIndex, emptyTileIndex)) {
                            System.out.println("Déplacement de la tuile " + tileNumber + " (position " + positionIndex + ") vers la case vide (position " + emptyTileIndex + ")");

                            // Jouer le son de déplacement
                            if (slidingSound != null) {
                                System.out.println("Son de déplacement joué");
                                slidingSound.play();
                            }
                            else {
                                System.out.println("ERREUR: slidingSound est null");
                            }

                            // Initialiser l'animation
                            Vector3 start = new Vector3(gridZones[positionIndex].x, gridZones[positionIndex].y, 0);
                            Vector3 end = new Vector3(gridZones[emptyTileIndex].x, gridZones[emptyTileIndex].y, 0);
                            tileAnimation.start(positionIndex, start, end);
                        } else {
                            System.out.println("Déplacement impossible - Tuile " + tileNumber + " (position " + positionIndex + ") non adjacente à la case vide (position " + emptyTileIndex + ")");
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
        whiteTexture.dispose();
        if (puzzleTexture != null) {
            puzzleTexture.dispose();
        }
        if (fullImageTexture != null) {
            fullImageTexture.dispose();
        }
        if (solveSound != null) {
            solveSound.dispose();
        }
        if (slidingSound != null) {
            slidingSound.dispose();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        currentWidth = width;
        currentHeight = height;
        
        // Recalculer la taille des tuiles et l'espacement
        float availableWidth = viewport.getWorldWidth() - (2 * GRID_MARGIN);
        float availableHeight = viewport.getWorldHeight() - (2 * GRID_MARGIN);
        
        float maxTileSize = Math.min(
            availableWidth / (gridSize + (gridSize - 1) * SPACING_RATIO),
            availableHeight / (gridSize + (gridSize - 1) * SPACING_RATIO)
        );
        
        tileSize = maxTileSize;
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float totalGridSize = gridSize * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - totalGridSize) / 2;
        float marginY = (viewport.getWorldHeight() - totalGridSize) / 2;

        // Mettre à jour les positions des tuiles
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int positionIndex = row * gridSize + col;
                gridZones[positionIndex].set(
                    marginX + col * (tileSize + tileSpacing),
                    marginY + row * (tileSize + tileSpacing),
                    tileSize,
                    tileSize
                );
            }
        }

        // Mettre à jour les positions des boutons avec le gestionnaire centralisé
        backButton.setPosition(20, 20);
        UIPositionManager.positionButtonsBottomRight(viewport, 
            new Rectangle[]{solveButton}, 100, 50, 20, 0);
        UIPositionManager.positionButtonsTopRight(viewport, 
            new Rectangle[]{closeButton, infoButton}, 50, 20, 20);

        System.out.println("Redimensionnement: " + width + "x" + height);
        System.out.println("Viewport: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
        System.out.println("Taille des tuiles: " + tileSize);
        System.out.println("Position de la caméra: (" + viewport.getCamera().position.x + ", " + viewport.getCamera().position.y + ")");
    }
}
