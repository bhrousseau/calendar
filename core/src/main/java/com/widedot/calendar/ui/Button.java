package com.widedot.calendar.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.widedot.calendar.utils.CarlitoFontManager;

/**
 * Classe représentant un bouton dans l'interface utilisateur
 */
public class Button {
    private final Rectangle bounds;
    private final String text;
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final Texture texture;
    private final Color textColor;
    private final Color backgroundColor;
    
    /**
     * Constructeur
     * @param x Position X du bouton
     * @param y Position Y du bouton
     * @param width Largeur du bouton
     * @param height Hauteur du bouton
     * @param text Texte du bouton
     * @param font Police de caractères
     * @param texture Texture du bouton
     * @param textColor Couleur du texte
     * @param backgroundColor Couleur de fond
     */
    public Button(float x, float y, float width, float height, String text, 
                 BitmapFont font, Texture texture, Color textColor, Color backgroundColor) {
        this.bounds = new Rectangle(x, y, width, height);
        this.text = text;
        this.font = font;
        this.layout = new GlyphLayout();
        this.texture = texture;
        this.textColor = textColor;
        this.backgroundColor = backgroundColor;
    }
    
    /**
     * Dessine le bouton
     * @param batch SpriteBatch pour le rendu
     */
    public void draw(SpriteBatch batch) {
        // Dessiner le fond du bouton
        batch.setColor(backgroundColor);
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(1, 1, 1, 1);
        
        // Dessiner le texte
        font.setColor(textColor);
        layout.setText(font, text, textColor, bounds.width, Align.center, false);
        float textX = bounds.x + (bounds.width - layout.width) / 2;
        float textY = bounds.y + (bounds.height + layout.height) / 2;
        CarlitoFontManager.drawText(batch, layout, textX, textY);
    }
    
    /**
     * Vérifie si un point est à l'intérieur du bouton
     * @param x Position X du point
     * @param y Position Y du point
     * @return true si le point est à l'intérieur du bouton, false sinon
     */
    public boolean contains(float x, float y) {
        return bounds.contains(x, y);
    }
    
    /**
     * Met à jour la position du bouton
     * @param x Nouvelle position X
     * @param y Nouvelle position Y
     */
    public void setPosition(float x, float y) {
        bounds.setPosition(x, y);
    }
    
    /**
     * Met à jour la taille du bouton
     * @param width Nouvelle largeur
     * @param height Nouvelle hauteur
     */
    public void setSize(float width, float height) {
        bounds.setSize(width, height);
    }
    
    /**
     * Récupère le texte du bouton
     * @return Le texte du bouton
     */
    public String getText() {
        return text;
    }
    
    /**
     * Récupère les limites du bouton
     * @return Les limites du bouton
     */
    public Rectangle getBounds() {
        return bounds;
    }
} 