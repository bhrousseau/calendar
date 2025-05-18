package com.widedot.calendar.game;

import com.badlogic.gdx.Game;

/**
 * Fabrique pour créer les différents types de jeux
 */
public class GameFactory {
    private static GameFactory instance;
    private final Game gameInstance;
    
    /**
     * Constructeur privé pour le pattern Singleton
     * @param gameInstance L'instance du jeu
     */
    private GameFactory(Game gameInstance) {
        this.gameInstance = gameInstance;
    }
    
    /**
     * Récupère l'instance unique de GameFactory (pattern Singleton)
     * @param gameInstance L'instance du jeu
     * @return L'instance de GameFactory
     */
    public static GameFactory getInstance(Game gameInstance) {
        if (instance == null) {
            instance = new GameFactory(gameInstance);
        }
        return instance;
    }



} 