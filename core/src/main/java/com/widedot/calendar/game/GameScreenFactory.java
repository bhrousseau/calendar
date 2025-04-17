package com.widedot.calendar.game;

import com.badlogic.gdx.Game;
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.screens.GameScreen;

/**
 * Interface définissant les méthodes pour créer des écrans de jeu
 */
public interface GameScreenFactory {
    /**
     * Crée un écran de jeu en fonction du type de jeu associé à la peinture
     * @param dayId L'identifiant du jour
     * @param painting La peinture associée au jour
     * @param game L'instance du jeu
     * @return L'écran de jeu créé
     */
    GameScreen createGameScreen(int dayId, Paintable painting, Game game);
    
    /**
     * Crée un écran de jeu en fonction du type de jeu
     * @param dayId L'identifiant du jour
     * @param gameType Le type de jeu
     * @param game L'instance du jeu
     * @return L'écran de jeu créé
     */
    GameScreen createGameScreen(int dayId, String gameType, Game game);
} 