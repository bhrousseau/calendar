package com.widedot.calendar.game;

import com.badlogic.gdx.Game;
import com.widedot.calendar.data.Paintable;
import com.widedot.calendar.screens.GameScreen;
import com.widedot.calendar.screens.DefaultGameScreen;
import com.widedot.calendar.screens.SlidingPuzzleGameScreen;
import com.widedot.calendar.painting.PaintingManager;

/**
 * Implémentation par défaut de la fabrique d'écrans de jeu
 */
public class DefaultGameScreenFactory implements GameScreenFactory {
    private static DefaultGameScreenFactory instance;
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private DefaultGameScreenFactory() {
    }
    
    /**
     * Récupère l'instance unique de DefaultGameScreenFactory (pattern Singleton)
     * @return L'instance de DefaultGameScreenFactory
     */
    public static DefaultGameScreenFactory getInstance() {
        if (instance == null) {
            instance = new DefaultGameScreenFactory();
        }
        return instance;
    }
    
    @Override
    public GameScreen createGameScreen(int dayId, Paintable painting, Game game) {
        System.out.println("Création d'un écran de jeu pour le jour " + dayId);
        String gameType = PaintingManager.getInstance().getGameTypeForDay(dayId);
        System.out.println("Type de jeu: " + gameType);
        
        try {
            GameScreen gameScreen;
            switch (gameType) {
                case "sliding_puzzle":
                    System.out.println("Création d'un puzzle coulissant");
                    gameScreen = new SlidingPuzzleGameScreen(dayId, game);
                    break;
                case "default":
                default:
                    System.out.println("Création d'un jeu par défaut");
                    gameScreen = new DefaultGameScreen(dayId, game);
                    break;
            }
            System.out.println("Écran de jeu créé avec succès: " + gameScreen.getClass().getSimpleName());
            return gameScreen;
        } catch (Exception e) {
            System.err.println("ERREUR lors de la création de l'écran de jeu: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public GameScreen createGameScreen(int dayId, String gameType, Game game) {
        System.out.println("Création d'un écran de jeu pour le jour " + dayId + " avec le type " + gameType);
        
        try {
            GameScreen gameScreen;
            switch (gameType) {
                case "sliding_puzzle":
                    System.out.println("Création d'un puzzle coulissant");
                    gameScreen = new SlidingPuzzleGameScreen(dayId, game);
                    break;
                case "default":
                default:
                    System.out.println("Création d'un jeu par défaut");
                    gameScreen = new DefaultGameScreen(dayId, game);
                    break;
            }
            System.out.println("Écran de jeu créé avec succès: " + gameScreen.getClass().getSimpleName());
            return gameScreen;
        } catch (Exception e) {
            System.err.println("ERREUR lors de la création de l'écran de jeu: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
} 