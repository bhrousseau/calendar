package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.AdventCalendarScreen;
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.painting.PaintingManager;
import com.widedot.calendar.utils.ResourceManager;

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
    private final Texture puzzleTexture;
    private final int GRID_SIZE = 3;
    private final int[] puzzleState;
    private final Rectangle[] puzzleTiles;
    private final float TILE_SIZE = 200;
    private final float TILE_SPACING = 10;
    private int emptyTileIndex;

    /**
     * Constructeur
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     */
    public SlidingPuzzleGameScreen(int dayId, Game game) {
        super(dayId, game);
        System.out.println("Constructeur de SlidingPuzzleGameScreen appelé pour le jour " + dayId);
        
        this.font = new BitmapFont();
        this.layout = new GlyphLayout();
        this.backButton = new Rectangle(0, 0, 100, 50);
        this.solveButton = new Rectangle(0, 0, 100, 50);
        this.infoButton = new Rectangle(0, 0, 50, 50);
        this.closeButton = new Rectangle(0, 0, 30, 30);
        this.infoPanelColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
        this.showInfoPanel = false;
        
        // Charger la texture du puzzle
        String imagePath = paintable.getImagePath();
        System.out.println("Chargement de la texture du puzzle: " + imagePath);
        this.puzzleTexture = ResourceManager.getInstance().getTexture(imagePath);
        
        this.puzzleState = new int[GRID_SIZE * GRID_SIZE];
        this.puzzleTiles = new Rectangle[GRID_SIZE * GRID_SIZE];
        this.emptyTileIndex = GRID_SIZE * GRID_SIZE - 1;
        
        System.out.println("Constructeur de SlidingPuzzleGameScreen terminé");
    }

    @Override
    protected Paintable loadPaintable(int day) {
        System.out.println("Chargement de la peinture pour le jour " + day);
        return PaintingManager.getInstance().getPaintingByDay(day);
    }

    @Override
    protected void initializeGame() {
        System.out.println("Initialisation du puzzle coulissant pour le jour " + dayId);
        
        // Vérifier que la texture est chargée
        if (puzzleTexture == null) {
            System.err.println("ERREUR: Texture du puzzle non chargée");
            return;
        }
        
        // Initialiser l'état du puzzle
        for (int i = 0; i < puzzleState.length; i++) {
            puzzleState[i] = i;
        }
        
        // Mélanger le puzzle
        for (int i = puzzleState.length - 1; i > 0; i--) {
            int j = (int)(Math.random() * (i + 1));
            int temp = puzzleState[i];
            puzzleState[i] = puzzleState[j];
            puzzleState[j] = temp;
        }
        
        // Trouver l'index de la case vide
        for (int i = 0; i < puzzleState.length; i++) {
            if (puzzleState[i] == puzzleState.length - 1) {
                emptyTileIndex = i;
                break;
            }
        }
        
        // Initialiser les positions des tuiles
        float gridWidth = GRID_SIZE * (TILE_SIZE + TILE_SPACING) - TILE_SPACING;
        float gridHeight = gridWidth;
        float startX = (currentWidth - gridWidth) / 2;
        float startY = (currentHeight - gridHeight) / 2;
        
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int index = i * GRID_SIZE + j;
                puzzleTiles[index] = new Rectangle(
                    startX + j * (TILE_SIZE + TILE_SPACING),
                    startY + (GRID_SIZE - 1 - i) * (TILE_SIZE + TILE_SPACING),
                    TILE_SIZE,
                    TILE_SIZE
                );
            }
        }
        
        // Positionner les boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(currentWidth - 120, 20);
        infoButton.setPosition(currentWidth - 70, currentHeight - 70);
        closeButton.setPosition(currentWidth - 50, currentHeight - 50);
        
        System.out.println("Initialisation du puzzle coulissant terminée");
    }

    @Override
    protected void updateGame(float delta) {
        // Mise à jour du jeu
    }

    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(1, 1, 1, 1);
        batch.draw(puzzleTexture, 0, 0, currentWidth, currentHeight);
        
        // Dessiner les tuiles du puzzle
        for (int i = 0; i < puzzleState.length; i++) {
            if (puzzleState[i] != puzzleState.length - 1) {
                int row = puzzleState[i] / GRID_SIZE;
                int col = puzzleState[i] % GRID_SIZE;
                float srcX = col * TILE_SIZE;
                float srcY = (GRID_SIZE - 1 - row) * TILE_SIZE;
                
                TextureRegion region = new TextureRegion(puzzleTexture, (int)srcX, (int)srcY, (int)TILE_SIZE, (int)TILE_SIZE);
                batch.draw(region,
                    puzzleTiles[i].x, puzzleTiles[i].y, puzzleTiles[i].width, puzzleTiles[i].height);
            }
        }
        
        // Dessiner les boutons
        batch.setColor(0.5f, 0.5f, 0.5f, 1);
        batch.draw(puzzleTexture, backButton.x, backButton.y, backButton.width, backButton.height);
        batch.draw(puzzleTexture, solveButton.x, solveButton.y, solveButton.width, solveButton.height);
        batch.draw(puzzleTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);
        
        // Dessiner le texte des boutons
        font.setColor(1, 1, 1, 1);
        layout.setText(font, "Retour", new Color(1, 1, 1, 1), backButton.width, Align.center, false);
        font.draw(batch, layout, backButton.x, backButton.y + backButton.height / 2 + layout.height / 2);
        
        layout.setText(font, "Résoudre", new Color(1, 1, 1, 1), solveButton.width, Align.center, false);
        font.draw(batch, layout, solveButton.x, solveButton.y + solveButton.height / 2 + layout.height / 2);
        
        layout.setText(font, "Info", new Color(1, 1, 1, 1), infoButton.width, Align.center, false);
        font.draw(batch, layout, infoButton.x, infoButton.y + infoButton.height / 2 + layout.height / 2);
        
        // Dessiner le panneau d'information si nécessaire
        if (showInfoPanel) {
            float panelWidth = 400;
            float panelHeight = 300;
            float panelX = (currentWidth - panelWidth) / 2;
            float panelY = (currentHeight - panelHeight) / 2;
            
            batch.setColor(infoPanelColor);
            batch.draw(puzzleTexture, panelX, panelY, panelWidth, panelHeight);
            
            // Dessiner le bouton de fermeture
            batch.setColor(0.5f, 0.5f, 0.5f, 1);
            batch.draw(puzzleTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);
            
            // Dessiner les informations du tableau
            float textMargin = 20;
            float titleY = panelY + panelHeight - textMargin;
            float artistY = titleY - 40;
            float yearY = artistY - 40;
            float descriptionY = yearY - 80;
            
            font.setColor(1, 1, 1, 1);
            layout.setText(font, paintable.getTitle(), new Color(1, 1, 1, 1), panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, panelX + textMargin, titleY);
            
            layout.setText(font, paintable.getArtist(), new Color(1, 1, 1, 1), panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, panelX + textMargin, artistY);
            
            layout.setText(font, String.valueOf(paintable.getYear()), new Color(1, 1, 1, 1), panelWidth - 2 * textMargin, Align.center, false);
            font.draw(batch, layout, panelX + textMargin, yearY);
            
            layout.setText(font, paintable.getDescription(), new Color(1, 1, 1, 1), panelWidth - 2 * textMargin, Align.center, true);
            font.draw(batch, layout, panelX + textMargin, descriptionY);
        }
    }

    @Override
    protected void handleInput() {
        if (Gdx.input.justTouched()) {
            System.out.println("Toucher détecté dans SlidingPuzzleGameScreen");
            
            float touchX = Gdx.input.getX();
            float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            // Convertir les coordonnées de l'écran en coordonnées du monde
            Vector3 worldPos = new Vector3(touchX, touchY, 0);
            camera.unproject(worldPos);
            
            System.out.println("Position du toucher: (" + worldPos.x + ", " + worldPos.y + ")");
            
            if (backButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Retour cliqué");
                returnToMainMenu();
            } else if (solveButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Résoudre cliqué");
                // Logique de résolution du jeu
                if (game instanceof AdventCalendarGame) {
                    AdventCalendarGame adventGame = (AdventCalendarGame) game;
                    adventGame.setScore(dayId, 100);
                    adventGame.markPaintingAsVisited(dayId);
                }
                returnToMainMenu();
            } else if (infoButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Info cliqué");
                showInfoPanel = true;
            } else if (showInfoPanel && closeButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Fermer cliqué");
                showInfoPanel = false;
            } else {
                // Gérer le déplacement des tuiles
                for (int i = 0; i < puzzleTiles.length; i++) {
                    if (puzzleTiles[i].contains(worldPos.x, worldPos.y)) {
                        System.out.println("Tuile " + i + " cliquée");
                        
                        // Vérifier si la tuile est adjacente à la case vide
                        int row = i / GRID_SIZE;
                        int col = i % GRID_SIZE;
                        int emptyRow = emptyTileIndex / GRID_SIZE;
                        int emptyCol = emptyTileIndex % GRID_SIZE;
                        
                        if ((Math.abs(row - emptyRow) == 1 && col == emptyCol) ||
                            (Math.abs(col - emptyCol) == 1 && row == emptyRow)) {
                            System.out.println("Tuile " + i + " déplacée");
                            
                            // Échanger les tuiles
                            int temp = puzzleState[i];
                            puzzleState[i] = puzzleState[emptyTileIndex];
                            puzzleState[emptyTileIndex] = temp;
                            emptyTileIndex = i;
                            
                            // Vérifier si le puzzle est résolu
                            boolean solved = true;
                            for (int j = 0; j < puzzleState.length; j++) {
                                if (puzzleState[j] != j) {
                                    solved = false;
                                    break;
                                }
                            }
                            
                            if (solved) {
                                System.out.println("Puzzle résolu !");
                                if (game instanceof AdventCalendarGame) {
                                    AdventCalendarGame adventGame = (AdventCalendarGame) game;
                                    adventGame.setScore(dayId, 100);
                                    adventGame.markPaintingAsVisited(dayId);
                                }
                                returnToMainMenu();
                            }
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
    }
} 