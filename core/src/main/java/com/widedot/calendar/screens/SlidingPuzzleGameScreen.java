package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
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

import java.util.Map;

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
    private static final String SOLVE_SOUND_PATH = "audio/win.wav";
    private static final String SLIDING_SOUND_PATH = "audio/sliding.wav";
    
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

    // Variables pour l'animation
    private boolean isAnimating = false;
    private int animatingTileIndex;
    private float animationProgress = 0f;
    private Vector3 animationStart = new Vector3();
    private Vector3 animationEnd = new Vector3();
    
    // Variables pour l'animation de victoire
    private boolean isVictoryAnimating = false;
    private float victoryAnimationProgress = 0f;
    private float victoryAnimationDuration = 4.5f; // Sera calculé dynamiquement
    private float fadeInDuration = 1f;
    private float mergeDuration = 1.5f;
    private float phaseDelay = 0.5f;
    private float currentTileSpacing;
    private boolean isVictoryAnimationComplete = false;
    private Texture fullImageTexture;
    private float irisOpenProgress = 0f;
    private Rectangle irisOpenRect;
    private float irisOpenDuration = 0f; // Sera calculé dynamiquement
    private float pixelsPerSecond = 500f; // Vitesse de déplacement des pixels

    // Flag pour éviter la double initialisation du puzzle
    private boolean isInitialized = false;

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
    public SlidingPuzzleGameScreen(int dayId, Game game, Theme theme, Map<String, Object> parameters) {
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
        // Si le jeu est déjà initialisé, ne pas le refaire
        if (isInitialized) {
            System.out.println("Le puzzle a déjà été initialisé, on ne le refait pas");
            return;
        }
        
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

        // Calculer la durée de l'animation d'ouverture rectangulaire
        if (fullImageTexture != null && theme != null && theme.getSquareCrop() != null) {
            Theme.CropInfo squareCrop = theme.getSquareCrop();
            float imageWidth = fullImageTexture.getWidth();
            float imageHeight = fullImageTexture.getHeight();
            
            // Calculer la distance totale à parcourir (différence entre l'image complète et la zone recadrée)
            float totalDistance = Math.max(
                imageWidth - squareCrop.getWidth(),
                imageHeight - squareCrop.getHeight()
            );
            
            // Calculer la durée en fonction de la distance et de la vitesse
            irisOpenDuration = totalDistance / pixelsPerSecond;
            
            // Mettre à jour la durée totale de l'animation
            victoryAnimationDuration = fadeInDuration + mergeDuration + phaseDelay + irisOpenDuration;
        }

        // Positionner les boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
        infoButton.setPosition(viewport.getWorldWidth() - 70, viewport.getWorldHeight() - 70);
        closeButton.setPosition(viewport.getWorldWidth() - 50, viewport.getWorldHeight() - 50);
        
        // Marquer le jeu comme initialisé
        isInitialized = true;
        System.out.println("Initialisation du puzzle terminée");
    }

    @Override
    protected void updateGame(float delta) {
        if (isAnimating) {
            // Mettre à jour la progression de l'animation
            animationProgress += delta * animationSpeed;
            
            if (animationProgress >= 1f) {
                // Animation terminée
                isAnimating = false;
                animationProgress = 0f;

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
                    isVictoryAnimating = true;
                    isVictoryAnimationComplete = false;
                    victoryAnimationProgress = 0f;
                    currentTileSpacing = tileSpacing;
                    
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
        if (isVictoryAnimating) {
            victoryAnimationProgress += delta;
            
            // Calculer la progression de l'effet d'ouverture
            float irisOpenStart = fadeInDuration + mergeDuration + phaseDelay;
            if (victoryAnimationProgress >= irisOpenStart) {
                irisOpenProgress = Math.min(1f, (victoryAnimationProgress - irisOpenStart) / irisOpenDuration);
            }
            
            if (victoryAnimationProgress >= victoryAnimationDuration) {
                isVictoryAnimating = false;
                isVictoryAnimationComplete = true;
            }
        }
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
            if (isVictoryAnimating || isVictoryAnimationComplete) {
                // Calculer la progression de l'animation
                float fadeProgress = Math.min(victoryAnimationProgress / fadeInDuration, 1f);
                float mergeProgress = Math.max(0f, Math.min(1f, (victoryAnimationProgress - fadeInDuration - phaseDelay) / mergeDuration));
                
                // Si l'animation est terminée, utiliser la progression maximale
                if (isVictoryAnimationComplete) {
                    fadeProgress = 1f;
                    mergeProgress = 1f;
                    irisOpenProgress = 1f;
                }

                // Si nous sommes dans la phase d'ouverture rectangulaire
                if (irisOpenProgress > 0 && fullImageTexture != null) {
                    // Calculer la taille et la position pour centrer l'image complète
                    float screenWidth = viewport.getWorldWidth();
                    float screenHeight = viewport.getWorldHeight();
                    float imageWidth = fullImageTexture.getWidth();
                    float imageHeight = fullImageTexture.getHeight();
                    
                    // Calculer le ratio pour s'assurer que l'image tient dans l'écran (zoom final)
                    float finalScale = Math.min(
                        screenWidth * 0.8f / imageWidth,
                        screenHeight * 0.8f / imageHeight
                    );
                    
                    // Récupérer les informations de recadrage
                    Theme.CropInfo squareCrop = theme.getSquareCrop();
                    if (squareCrop != null) {
                        // Calculer le zoom initial pour correspondre à la taille des tuiles réunies
                        float initialScale = (gridSize * tileSize) / squareCrop.getWidth();
                        
                        // Interpoler le zoom entre la valeur initiale et finale
                        float currentScale = initialScale + (finalScale - initialScale) * irisOpenProgress;
                        
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
                        float openWidth = cropWidth + (scaledWidth - cropWidth) * irisOpenProgress;
                        float openHeight = cropHeight + (scaledHeight - cropHeight) * irisOpenProgress;
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
                } else {
                    // Dessiner la case vide avec fade in
                    if (fadeProgress < 1f) {
                        batch.setColor(1f, 1f, 1f, fadeProgress);
                        batch.draw(puzzleTiles[emptyTileIndex], 
                            gridZones[emptyTileIndex].x, 
                            gridZones[emptyTileIndex].y, 
                            gridZones[emptyTileIndex].width, 
                            gridZones[emptyTileIndex].height);
                    } else if (mergeProgress < 1f) {
                        // Garder la pièce visible après le fade in et l'animer avec les autres
                        batch.setColor(Color.WHITE);
                        float x = gridZones[emptyTileIndex].x;
                        float y = gridZones[emptyTileIndex].y;
                        
                        // Calculer la position finale (sans espacement)
                        float gridX = gridZones[0].x;
                        float gridY = gridZones[0].y;
                        float finalX = gridX + (emptyTileIndex % gridSize) * tileSize;
                        float finalY = gridY + (emptyTileIndex / gridSize) * tileSize;
                        
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
                        
                        batch.draw(puzzleTiles[emptyTileIndex], x, y, tileSize, tileSize);
                    } else {
                        // Afficher la pièce à sa position finale pendant le délai
                        batch.setColor(Color.WHITE);
                        float gridX = gridZones[0].x;
                        float gridY = gridZones[0].y;
                        float finalX = gridX + (emptyTileIndex % gridSize) * tileSize;
                        float finalY = gridY + (emptyTileIndex / gridSize) * tileSize;
                        
                        // Appliquer le décalage final
                        float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
                        float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
                        float finalGridWidth = gridSize * tileSize;
                        float finalGridHeight = gridSize * tileSize;
                        float offsetX = (totalGridWidth - finalGridWidth) / 2;
                        float offsetY = (totalGridHeight - finalGridHeight) / 2;
                        
                        batch.draw(puzzleTiles[emptyTileIndex], finalX + offsetX, finalY + offsetY, tileSize, tileSize);
                    }
                    
                    // Dessiner toutes les tuiles avec l'espacement réduit
                    batch.setColor(Color.WHITE);
                    for (int i = 0; i < gridZones.length; i++) {
                        if (i != emptyTileIndex) {
                            int tileNumber = puzzleState[i];
                            float x = gridZones[i].x;
                            float y = gridZones[i].y;
                            
                            // Calculer la position finale (sans espacement)
                            float gridX = gridZones[0].x;
                            float gridY = gridZones[0].y;
                            float finalX = gridX + (i % gridSize) * tileSize;
                            float finalY = gridY + (i / gridSize) * tileSize;
                            
                            if (mergeProgress < 1f) {
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
                            } else {
                                // Calculer le décalage final
                                float totalGridWidth = gridSize * (tileSize + tileSpacing) - tileSpacing;
                                float totalGridHeight = gridSize * (tileSize + tileSpacing) - tileSpacing;
                                float finalGridWidth = gridSize * tileSize;
                                float finalGridHeight = gridSize * tileSize;
                                float offsetX = (totalGridWidth - finalGridWidth) / 2;
                                float offsetY = (totalGridHeight - finalGridHeight) / 2;
                                
                                batch.draw(puzzleTiles[tileNumber], finalX + offsetX, finalY + offsetY, tileSize, tileSize);
                            }
                        }
                    }
                }
            } else {
                // Rendu normal du puzzle
                for (int i = 0; i < gridZones.length; i++) {
                    if (i != emptyTileIndex && (!isAnimating || i != animatingTileIndex)) {
                        int tileNumber = puzzleState[i];
                        batch.draw(puzzleTiles[tileNumber], 
                            gridZones[i].x, 
                            gridZones[i].y, 
                            gridZones[i].width, 
                            gridZones[i].height);
                    }
                }

                // Dessiner la tuile en cours d'animation
                if (isAnimating) {
                    int tileNumber = puzzleState[animatingTileIndex];
                    float x = animationStart.x + (animationEnd.x - animationStart.x) * animationProgress;
                    float y = animationStart.y + (animationEnd.y - animationStart.y) * animationProgress;
                    batch.draw(puzzleTiles[tileNumber], x, y, tileSize, tileSize);
                }
            }
        }

        // Si le puzzle est résolu et l'animation de victoire est terminée, afficher le message de victoire
        if (isPuzzleSolved && !isVictoryAnimating) {
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

    @Override
    protected void handleInput() {
        // Désactiver les entrées pendant l'animation ou les transitions
        if (isAnimating || TransitionScreen.isTransitionActive()) {
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
                            isAnimating = true;
                            animationProgress = 0f;
                            animatingTileIndex = positionIndex;

                            // Définir les positions de début et de fin de l'animation
                            animationStart.set(gridZones[positionIndex].x, gridZones[positionIndex].y, 0);
                            animationEnd.set(gridZones[emptyTileIndex].x, gridZones[emptyTileIndex].y, 0);
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

        // Mettre à jour les positions des boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
        infoButton.setPosition(viewport.getWorldWidth() - 70, viewport.getWorldHeight() - 70);
        closeButton.setPosition(viewport.getWorldWidth() - 50, viewport.getWorldHeight() - 50);

        System.out.println("Redimensionnement: " + width + "x" + height);
        System.out.println("Viewport: " + viewport.getWorldWidth() + "x" + viewport.getWorldHeight());
        System.out.println("Taille des tuiles: " + tileSize);
        System.out.println("Position de la caméra: (" + viewport.getCamera().position.x + ", " + viewport.getCamera().position.y + ")");
    }
}
