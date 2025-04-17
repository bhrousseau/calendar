package com.widedot.calendar.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.widedot.calendar.AdventCalendarGame;
import com.widedot.calendar.AdventCalendarScreen;
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.painting.PaintingManager;
import com.widedot.calendar.utils.ResourceManager;

/**
 * Écran de jeu par défaut
 */
public class DefaultGameScreen extends GameScreen {
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Rectangle backButton;
    private final Rectangle solveButton;
    private final Rectangle infoButton;
    private final Rectangle closeButton;
    private final Color infoPanelColor;
    private boolean showInfoPanel;
    private final Texture unavailableTexture;
    private final Texture doorClosedTexture;
    private final Texture doorOpenTexture;

    /**
     * Constructeur
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     */
    public DefaultGameScreen(int dayId, Game game) {
        super(dayId, game);
        this.font = new BitmapFont();
        this.layout = new GlyphLayout();
        this.backButton = new Rectangle(0, 0, 100, 50);
        this.solveButton = new Rectangle(0, 0, 100, 50);
        this.infoButton = new Rectangle(0, 0, 50, 50);
        this.closeButton = new Rectangle(0, 0, 30, 30);
        this.infoPanelColor = new Color(0.2f, 0.2f, 0.2f, 0.8f);
        this.showInfoPanel = false;
        this.unavailableTexture = ResourceManager.getInstance().getTexture("images/unavailable.png");
        this.doorClosedTexture = ResourceManager.getInstance().getTexture("images/locked.png");
        this.doorOpenTexture = ResourceManager.getInstance().getTexture("images/unlocked.png");
    }

    @Override
    protected Paintable loadPaintable(int day) {
        return PaintingManager.getInstance().getPaintingByDay(day);
    }

    @Override
    protected void initializeGame() {
        System.out.println("Initialisation du jeu par défaut pour le jour " + dayId);
        
        // Vérifier que les textures sont chargées
        if (unavailableTexture == null) {
            System.err.println("ERREUR: Texture 'unavailable.png' non chargée");
        }
        
        if (doorClosedTexture == null) {
            System.err.println("ERREUR: Texture 'locked.png' non chargée");
        }
        
        if (doorOpenTexture == null) {
            System.err.println("ERREUR: Texture 'unlocked.png' non chargée");
        }
        
        // Initialiser les positions des boutons
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        
        // Positionner les boutons
        backButton.setPosition(20, 20);
        solveButton.setPosition(screenWidth - 120, 20);
        infoButton.setPosition(screenWidth - 70, screenHeight - 70);
        closeButton.setPosition(screenWidth - 50, screenHeight - 50);
        
        System.out.println("Initialisation du jeu par défaut terminée");
    }

    @Override
    protected void updateGame(float delta) {
        // Mise à jour du jeu
    }

    @Override
    protected void renderGame() {
        // Dessiner le fond
        batch.setColor(1, 1, 1, 1);
        batch.draw(unavailableTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        // Dessiner la porte
        if (showInfoPanel) {
            batch.draw(doorOpenTexture, Gdx.graphics.getWidth() / 2 - 150, Gdx.graphics.getHeight() / 2 - 200, 300, 400);
        } else {
            batch.draw(doorClosedTexture, Gdx.graphics.getWidth() / 2 - 150, Gdx.graphics.getHeight() / 2 - 200, 300, 400);
        }
        
        // Dessiner les boutons
        batch.setColor(0.5f, 0.5f, 0.5f, 1);
        batch.draw(unavailableTexture, backButton.x, backButton.y, backButton.width, backButton.height);
        batch.draw(unavailableTexture, solveButton.x, solveButton.y, solveButton.width, solveButton.height);
        batch.draw(unavailableTexture, infoButton.x, infoButton.y, infoButton.width, infoButton.height);
        
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
            float panelX = Gdx.graphics.getWidth() / 2 - panelWidth / 2;
            float panelY = Gdx.graphics.getHeight() / 2 - panelHeight / 2;
            
            batch.setColor(infoPanelColor);
            batch.draw(unavailableTexture, panelX, panelY, panelWidth, panelHeight);
            
            // Dessiner le bouton de fermeture
            batch.setColor(0.5f, 0.5f, 0.5f, 1);
            batch.draw(unavailableTexture, closeButton.x, closeButton.y, closeButton.width, closeButton.height);
            
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
            float touchX = Gdx.input.getX();
            float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
            
            if (backButton.contains(touchX, touchY)) {
                returnToMainMenu();
            } else if (solveButton.contains(touchX, touchY)) {
                // Logique de résolution du jeu
                if (game instanceof AdventCalendarGame) {
                    AdventCalendarGame adventGame = (AdventCalendarGame) game;
                    adventGame.setScore(dayId, 100);
                    adventGame.markPaintingAsVisited(dayId);
                }
                returnToMainMenu();
            } else if (infoButton.contains(touchX, touchY)) {
                showInfoPanel = true;
            } else if (showInfoPanel && closeButton.contains(touchX, touchY)) {
                showInfoPanel = false;
            }
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }
} 