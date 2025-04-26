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
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.painting.PaintingManager;
import com.badlogic.gdx.graphics.Pixmap;

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
    private final int GRID_SIZE = 3;
    private final float GRID_MARGIN = 80; // Marge autour de la grille
    private final float SPACING_RATIO = 0.05f; // Espacement fixe de 5% de la taille des tuiles
    private final int[] puzzleState; // État actuel du puzzle (index = position, valeur = numéro de tuile)
    private final Rectangle[] gridZones; // Positions des tuiles (index = position)
    private float tileSize; // Taille dynamique des tuiles
    private float tileSpacing; // Espacement dynamique entre les tuiles
    private int emptyTileIndex; // Position de la case vide
    private boolean isPuzzleSolved; // Indique si le puzzle est résolu

    private boolean isAdjacent(int tileIndex1, int tileIndex2) {
        // Convertir les indices linéaires en coordonnées de grille
        int row1 = tileIndex1 / GRID_SIZE;
        int col1 = tileIndex1 % GRID_SIZE;
        int row2 = tileIndex2 / GRID_SIZE;
        int col2 = tileIndex2 % GRID_SIZE;

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
        int numMoves = 100 + (int)(Math.random() * 100); // Entre 100 et 200 mouvements

        // Effectuer des mouvements aléatoires valides
        for (int i = 0; i < numMoves; i++) {
            // Trouver les tuiles adjacentes à la case vide
            int row = emptyTileIndex / GRID_SIZE;
            int col = emptyTileIndex % GRID_SIZE;
            int[] possibleMoves = new int[4];
            int numPossibleMoves = 0;

            // Vérifier les 4 directions possibles
            if (row > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - GRID_SIZE; // Haut
            if (row < GRID_SIZE - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + GRID_SIZE; // Bas
            if (col > 0) possibleMoves[numPossibleMoves++] = emptyTileIndex - 1; // Gauche
            if (col < GRID_SIZE - 1) possibleMoves[numPossibleMoves++] = emptyTileIndex + 1; // Droite

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

        // Créer une texture blanche
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new Texture(pixmap);
        pixmap.dispose();

        this.puzzleState = new int[GRID_SIZE * GRID_SIZE];
        this.gridZones = new Rectangle[GRID_SIZE * GRID_SIZE];
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
        if (whiteTexture == null) {
            System.err.println("ERREUR: Texture du puzzle non chargée");
            return;
        }

        // Calculer la taille des tuiles et l'espacement en fonction de la taille de la fenêtre
        float availableWidth = viewport.getWorldWidth() - (2 * GRID_MARGIN);
        float availableHeight = viewport.getWorldHeight() - (2 * GRID_MARGIN);
        
        // Calculer la taille maximale possible pour une tuile
        float maxTileSize = Math.min(
            availableWidth / (GRID_SIZE + (GRID_SIZE - 1) * SPACING_RATIO),
            availableHeight / (GRID_SIZE + (GRID_SIZE - 1) * SPACING_RATIO)
        );
        
        tileSize = maxTileSize;
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float gridSize = GRID_SIZE * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - gridSize) / 2;
        float marginY = (viewport.getWorldHeight() - gridSize) / 2;

        // Initialiser les positions des tuiles
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int positionIndex = row * GRID_SIZE + col;
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

        // Positionner les boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(viewport.getWorldWidth() - 120, 20);
        infoButton.setPosition(viewport.getWorldWidth() - 70, viewport.getWorldHeight() - 70);
        closeButton.setPosition(viewport.getWorldWidth() - 50, viewport.getWorldHeight() - 50);
    }

    @Override
    protected void updateGame(float delta) {
        // Mise à jour du jeu
    }

    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(1, 1, 1, 1);

        // Si le puzzle est résolu, afficher le message de victoire
        if (isPuzzleSolved) {
            // Dessiner un fond semi-transparent
            batch.setColor(0, 0, 0, 0.5f);
            batch.draw(whiteTexture, 0, 0, viewport.getWorldWidth(), viewport.getWorldHeight());

            // Afficher le message "Bravo !"
            font.setColor(1, 1, 1, 1);
            String victoryMessage = "Bravo !";
            layout.setText(font, victoryMessage);
            float textX = (viewport.getWorldWidth() - layout.width) / 2;
            float textY = (viewport.getWorldHeight() + layout.height) / 2;
            font.draw(batch, victoryMessage, textX, textY);

            // Afficher un message pour indiquer de cliquer
            String clickMessage = "Cliquez pour retourner au menu";
            layout.setText(font, clickMessage);
            textX = (viewport.getWorldWidth() - layout.width) / 2;
            textY = (viewport.getWorldHeight() - layout.height) / 2;
            font.draw(batch, clickMessage, textX, textY);
            return;
        }

        // Dessiner la grille de debug
        batch.setColor(1, 0, 0, 0.2f); // Rouge plus transparent
        for (int i = 0; i < gridZones.length; i++) {
            if (i != emptyTileIndex) { // Ne pas dessiner la case vide
                // Vérifier si la tuile est adjacente à la case vide
                int row = i / GRID_SIZE;
                int col = i % GRID_SIZE;
                int emptyRow = emptyTileIndex / GRID_SIZE;
                int emptyCol = emptyTileIndex % GRID_SIZE;
                boolean isAdjacent = (Math.abs(row - emptyRow) == 1 && col == emptyCol) ||
                                   (Math.abs(col - emptyCol) == 1 && row == emptyRow);

                // Changer la couleur en vert si la tuile est adjacente
                if (isAdjacent) {
                    batch.setColor(0, 1, 0, 0.2f); // Vert pour les tuiles adjacentes
                } else {
                    batch.setColor(1, 0, 0, 0.2f); // Rouge pour les autres tuiles
                }

                batch.draw(whiteTexture,
                    gridZones[i].x, gridZones[i].y,
                    gridZones[i].width, gridZones[i].height);
            }
        }

        // // Dessiner les lignes de la grille
        // batch.setColor(1, 1, 1, 0.8f); // Blanc semi-transparent

        // // Utiliser les coordonnées de la première tuile comme référence
        // float startX = gridZones[0].x;
        // float startY = gridZones[0].y;
        // float gridSize = GRID_SIZE * (tileSize + tileSpacing) - tileSpacing;

        // // Lignes verticales
        // for (int i = 0; i <= GRID_SIZE; i++) {
        //     float x = startX + i * (tileSize + tileSpacing) - tileSpacing/2;
        //     batch.draw(whiteTexture, x, startY, 1, gridSize);
        // }

        // // Lignes horizontales
        // for (int i = 0; i <= GRID_SIZE; i++) {
        //     float y = startY + i * (tileSize + tileSpacing) - tileSpacing/2;
        //     batch.draw(whiteTexture, startX, y, gridSize, 1);
        // }

        // Dessiner les numéros des tuiles
        for (int i = 0; i < gridZones.length; i++) {
            if (i != emptyTileIndex) {
                // Numéro de la tuile en blanc
                font.setColor(1, 1, 1, 1);
                String tileNumber = String.valueOf(puzzleState[i]);
                layout.setText(font, tileNumber);
                float textX = gridZones[i].x + (gridZones[i].width - layout.width) / 2;
                float textY = gridZones[i].y + (gridZones[i].height + layout.height) / 2 + 10;
                font.draw(batch, tileNumber, textX, textY);

                // ID de la case en rouge
                font.setColor(1, 0, 0, 1);
                String positionId = String.valueOf(i);
                layout.setText(font, positionId);
                textX = gridZones[i].x + (gridZones[i].width - layout.width) / 2;
                textY = gridZones[i].y + (gridZones[i].height + layout.height) / 2 - 10;
                font.draw(batch, positionId, textX, textY);
            }
        }

        // Dessiner les boutons
        batch.setColor(0.5f, 0.5f, 0.5f, 1);
        batch.draw(whiteTexture, backButton.x, backButton.y, backButton.width, backButton.height);
        batch.draw(whiteTexture, solveButton.x, solveButton.y, solveButton.width, solveButton.height);
        batch.draw(whiteTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);

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
            float panelX = (viewport.getWorldWidth() - panelWidth) / 2;
            float panelY = (viewport.getWorldHeight() - panelHeight) / 2;

            batch.setColor(infoPanelColor);
            batch.draw(whiteTexture, panelX, panelY, panelWidth, panelHeight);

            // Dessiner le bouton de fermeture
            batch.setColor(0.5f, 0.5f, 0.5f, 1);
            batch.draw(whiteTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);

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

            if (backButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Retour cliqué");
                returnToMainMenu();
            } else if (solveButton.contains(worldPos.x, worldPos.y)) {
                System.out.println("Bouton Résoudre cliqué");
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
                for (int positionIndex = 0; positionIndex < gridZones.length; positionIndex++) {
                    System.out.println("Test position : " + positionIndex + " coordonnées du rectangle (x,y,x',y'): " + gridZones[positionIndex].x + ", " + gridZones[positionIndex].y + ", " + (gridZones[positionIndex].x + gridZones[positionIndex].width) + ", " + (gridZones[positionIndex].y + gridZones[positionIndex].height));
                    if (gridZones[positionIndex].contains(worldPos.x, worldPos.y)) {
                        // Ignorer le clic si c'est sur la case vide
                        if (positionIndex == emptyTileIndex) {
                            System.out.println("Clic sur la case vide (position " + positionIndex + ") - Ignoré");
                            break;
                        }

                        int tileNumber = puzzleState[positionIndex];
                        System.out.println("Tuile numéro " + tileNumber + " cliquée (position " + positionIndex + ")");
                        System.out.println("Case vide actuelle: position " + emptyTileIndex);
                        System.out.println("coordonnées du rectangle (x,y,x',y'): " + gridZones[positionIndex].x + ", " + gridZones[positionIndex].y + ", " + (gridZones[positionIndex].x + gridZones[positionIndex].width) + ", " + (gridZones[positionIndex].y + gridZones[positionIndex].height));

                        if (isAdjacent(positionIndex, emptyTileIndex)) {
                            System.out.println("Déplacement de la tuile " + tileNumber + " (position " + positionIndex + ") vers la case vide (position " + emptyTileIndex + ")");

                            // Échanger les tuiles
                            int temp = puzzleState[positionIndex];
                            puzzleState[positionIndex] = puzzleState[emptyTileIndex];
                            puzzleState[emptyTileIndex] = temp;
                            emptyTileIndex = positionIndex;

                            System.out.println("Nouvelle position de la case vide: " + emptyTileIndex);
                            System.out.println("État du puzzle après déplacement: " + java.util.Arrays.toString(puzzleState));

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
                                if (game instanceof AdventCalendarGame) {
                                    AdventCalendarGame adventGame = (AdventCalendarGame) game;
                                    adventGame.setScore(dayId, 100);
                                    adventGame.markPaintingAsVisited(dayId);
                                }
                            }
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
            availableWidth / (GRID_SIZE + (GRID_SIZE - 1) * SPACING_RATIO),
            availableHeight / (GRID_SIZE + (GRID_SIZE - 1) * SPACING_RATIO)
        );
        
        tileSize = maxTileSize;
        tileSpacing = tileSize * SPACING_RATIO;

        // Calculer la taille totale de la grille
        float gridSize = GRID_SIZE * (tileSize + tileSpacing) - tileSpacing;

        // Calculer les marges pour centrer la grille dans le viewport
        float marginX = (viewport.getWorldWidth() - gridSize) / 2;
        float marginY = (viewport.getWorldHeight() - gridSize) / 2;

        // Mettre à jour les positions des tuiles
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int positionIndex = row * GRID_SIZE + col;
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
