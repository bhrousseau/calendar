package com.widedot.calendar.display;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Gestionnaire centralisé pour le positionnement des éléments UI
 */
public class UIPositionManager {
    
    /**
     * Positionne des boutons en haut à droite du viewport
     * @param viewport Le viewport de référence
     * @param buttons Les boutons à positionner (du haut vers le bas)
     * @param buttonSize Taille des boutons
     * @param marginFromEdge Marge depuis les bords
     * @param spacingBetweenButtons Espacement entre les boutons
     */
    public static void positionButtonsTopRight(Viewport viewport, Rectangle[] buttons, 
                                               float buttonSize, float marginFromEdge, float spacingBetweenButtons) {
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        float viewportHeight = viewport.getWorldHeight();
        
        float baseX = viewportWidth - marginFromEdge - buttonSize;
        float currentY = viewportHeight - marginFromEdge - buttonSize;
        
        for (Rectangle button : buttons) {
            button.setSize(buttonSize, buttonSize);
            button.setPosition(baseX, currentY);
            currentY -= (buttonSize + spacingBetweenButtons);
        }
    }
    
    /**
     * Positionne des boutons en bas à droite du viewport
     * @param viewport Le viewport de référence
     * @param buttons Les boutons à positionner (de droite à gauche)
     * @param buttonWidth Largeur des boutons
     * @param buttonHeight Hauteur des boutons
     * @param marginFromEdge Marge depuis les bords
     * @param spacingBetweenButtons Espacement entre les boutons
     */
    public static void positionButtonsBottomRight(Viewport viewport, Rectangle[] buttons,
                                                  float buttonWidth, float buttonHeight,
                                                  float marginFromEdge, float spacingBetweenButtons) {
        float viewportWidth = DisplayConfig.WORLD_WIDTH;
        
        float currentX = viewportWidth - marginFromEdge - buttonWidth;
        float baseY = marginFromEdge;
        
        for (Rectangle button : buttons) {
            button.setSize(buttonWidth, buttonHeight);
            button.setPosition(currentX, baseY);
            currentX -= (buttonWidth + spacingBetweenButtons);
        }
    }
    
    /**
     * Centre un élément horizontalement dans le viewport
     * @param viewport Le viewport de référence
     * @param elementWidth Largeur de l'élément
     * @return Position X centrée
     */
    public static float centerHorizontally(Viewport viewport, float elementWidth) {
        return (DisplayConfig.WORLD_WIDTH - elementWidth) / 2f;
    }
    
    /**
     * Centre un élément verticalement dans le viewport
     * @param viewport Le viewport de référence
     * @param elementHeight Hauteur de l'élément
     * @return Position Y centrée
     */
    public static float centerVertically(Viewport viewport, float elementHeight) {
        return (viewport.getWorldHeight() - elementHeight) / 2f;
    }
    
    /**
     * Calcule les dimensions adaptatives d'un panneau
     * @param viewport Le viewport de référence
     * @param minWidth Largeur minimale
     * @param maxWidth Largeur maximale
     * @param minHeight Hauteur minimale
     * @param maxHeight Hauteur maximale
     * @param widthRatio Ratio de largeur par rapport au viewport
     * @param heightRatio Ratio de hauteur par rapport au viewport
     * @return Rectangle avec les dimensions calculées
     */
    public static Rectangle calculateAdaptivePanelSize(Viewport viewport, 
                                                       float minWidth, float maxWidth,
                                                       float minHeight, float maxHeight,
                                                       float widthRatio, float heightRatio) {
        float screenWidth = DisplayConfig.WORLD_WIDTH;
        float screenHeight = viewport.getWorldHeight();
        
        float panelWidth = Math.max(minWidth, Math.min(maxWidth, screenWidth * widthRatio));
        float panelHeight = Math.max(minHeight, Math.min(maxHeight, screenHeight * heightRatio));
        
        float panelX = centerHorizontally(viewport, panelWidth);
        float panelY = centerVertically(viewport, panelHeight);
        
        return new Rectangle(panelX, panelY, panelWidth, panelHeight);
    }
}
