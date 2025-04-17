package com.widedot.calendar.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un panneau dans l'interface utilisateur
 */
public class Panel {
    private final Rectangle bounds;
    private final Texture texture;
    private final Color backgroundColor;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final List<Button> buttons;
    private final List<String> textLines;
    private final float textMargin;
    
    /**
     * Constructeur
     * @param x Position X du panneau
     * @param y Position Y du panneau
     * @param width Largeur du panneau
     * @param height Hauteur du panneau
     * @param texture Texture du panneau
     * @param backgroundColor Couleur de fond
     * @param font Police de caractères
     * @param textMargin Marge pour le texte
     */
    public Panel(float x, float y, float width, float height, Texture texture, 
                Color backgroundColor, BitmapFont font, float textMargin) {
        this.bounds = new Rectangle(x, y, width, height);
        this.texture = texture;
        this.backgroundColor = backgroundColor;
        this.font = font;
        this.layout = new GlyphLayout();
        this.buttons = new ArrayList<>();
        this.textLines = new ArrayList<>();
        this.textMargin = textMargin;
    }
    
    /**
     * Ajoute un bouton au panneau
     * @param button Le bouton à ajouter
     */
    public void addButton(Button button) {
        buttons.add(button);
    }
    
    /**
     * Ajoute une ligne de texte au panneau
     * @param text La ligne de texte à ajouter
     */
    public void addTextLine(String text) {
        textLines.add(text);
    }
    
    /**
     * Efface toutes les lignes de texte du panneau
     */
    public void clearTextLines() {
        textLines.clear();
    }
    
    /**
     * Dessine le panneau
     * @param batch SpriteBatch pour le rendu
     */
    public void draw(SpriteBatch batch) {
        // Dessiner le fond du panneau
        batch.setColor(backgroundColor);
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(1, 1, 1, 1);
        
        // Dessiner les boutons
        for (Button button : buttons) {
            button.draw(batch);
        }
        
        // Dessiner les lignes de texte
        font.setColor(1, 1, 1, 1);
        float textY = bounds.y + bounds.height - textMargin;
        for (String line : textLines) {
            layout.setText(font, line, Color.WHITE, bounds.width - 2 * textMargin, Align.center, false);
            float textX = bounds.x + textMargin;
            font.draw(batch, layout, textX, textY);
            textY -= layout.height + 10;
        }
    }
    
    /**
     * Vérifie si un point est à l'intérieur du panneau
     * @param x Position X du point
     * @param y Position Y du point
     * @return true si le point est à l'intérieur du panneau, false sinon
     */
    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }
    
    /**
     * Met à jour la position du panneau
     * @param x Nouvelle position X
     * @param y Nouvelle position Y
     */
    public void setPosition(float x, float y) {
        bounds.setPosition(x, y);
    }
    
    /**
     * Met à jour la taille du panneau
     * @param width Nouvelle largeur
     * @param height Nouvelle hauteur
     */
    public void setSize(float width, float height) {
        bounds.setSize(width, height);
    }
    
    /**
     * Récupère les limites du panneau
     * @return Les limites du panneau
     */
    public Rectangle getBounds() {
        return bounds;
    }
    
    /**
     * Récupère les boutons du panneau
     * @return Les boutons du panneau
     */
    public List<Button> getButtons() {
        return buttons;
    }
} 