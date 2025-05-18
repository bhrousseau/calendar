package com.widedot.calendar.game;

import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.AdventCalendarGame;
import com.badlogic.gdx.utils.Array;

/**
 * Classe qui gère l'état du jeu (scores, déverrouillage, etc.)
 */
public class GameState {
    private static GameState instance;
    private final AdventCalendarGame game;
    
    private final ObjectMap<String, Boolean> unlockedPaintings;
    private final ObjectMap<String, Integer> scores;
    private final ObjectMap<String, Boolean> visitedPaintings;
    private long gameSeed;
    private Array<Integer> shuffledDays;
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameState(AdventCalendarGame game) {
        this.game = game;
        this.unlockedPaintings = new ObjectMap<>();
        this.scores = new ObjectMap<>();
        this.visitedPaintings = new ObjectMap<>();
        this.shuffledDays = new Array<>();
        
        // Initialiser tous les tableaux comme verrouillés
        for (int i = 1; i <= 24; i++) {
            unlockedPaintings.put(String.valueOf(i), false);
            scores.put(String.valueOf(i), 0);
            visitedPaintings.put(String.valueOf(i), false);
        }
    }
    
    /**
     * Récupère l'instance unique de GameState (pattern Singleton)
     * @return L'instance de GameState
     */
    public static GameState getInstance(AdventCalendarGame game) {
        if (instance == null) {
            instance = new GameState(game);
        }
        return instance;
    }
    
    /**
     * Déverrouille un tableau pour un jour donné
     * @param dayId L'identifiant du jour
     */
    public void unlockPainting(int dayId) {
        if (dayId >= 1 && dayId <= 24) {
            unlockedPaintings.put(String.valueOf(dayId), true);
        }
    }
    
    /**
     * Vérifie si un tableau est déverrouillé pour un jour donné
     * @param dayId L'identifiant du jour
     * @return true si le tableau est déverrouillé, false sinon
     */
    public boolean isPaintingUnlocked(int dayId) {
        return unlockedPaintings.containsKey(String.valueOf(dayId)) ? unlockedPaintings.get(String.valueOf(dayId)) : false;
    }
    
    /**
     * Vérifie si un tableau a été visité pour un jour donné
     * @param dayId L'identifiant du jour
     * @return true si le tableau a été visité, false sinon
     */
    public boolean isPaintingVisited(int dayId) {
        return visitedPaintings.containsKey(String.valueOf(dayId)) ? visitedPaintings.get(String.valueOf(dayId)) : false;
    }
    
    /**
     * Marque un tableau comme visité pour un jour donné
     * @param dayId L'identifiant du jour
     */
    public void markPaintingAsVisited(int dayId) {
        if (dayId >= 1 && dayId <= 24) {
            visitedPaintings.put(String.valueOf(dayId), true);
        }
    }
    
    /**
     * Récupère le score pour un jour donné
     * @param dayId L'identifiant du jour
     * @return Le score
     */
    public int getScore(int dayId) {
        return scores.containsKey(String.valueOf(dayId)) ? scores.get(String.valueOf(dayId)) : 0;
    }
    
    /**
     * Définit le score pour un jour donné
     * @param dayId L'identifiant du jour
     * @param score Le score à définir
     */
    public void setScore(int dayId, int score) {
        if (dayId >= 1 && dayId <= 24) {
            scores.put(String.valueOf(dayId), score);
        }
    }
    
    /**
     * Récupère le générateur aléatoire
     * @return Le générateur aléatoire
     */
    public RandomXS128 getRandom() {
        return game.getRandom();
    }
    
    /**
     * Définit la graine du générateur aléatoire
     * @param seed La graine à définir
     */
    public void setRandomSeed(long seed) {
        game.getRandom().setSeed(seed);
    }
    
    /**
     * Réinitialise l'état du jeu
     */
    public void reset() {
        unlockedPaintings.clear();
        scores.clear();
        visitedPaintings.clear();
        
        // Réinitialiser tous les tableaux comme verrouillés
        for (int i = 1; i <= 24; i++) {
            unlockedPaintings.put(String.valueOf(i), false);
            scores.put(String.valueOf(i), 0);
            visitedPaintings.put(String.valueOf(i), false);
        }
    }
    
    /**
     * Définit la graine du jeu et initialise le mélange des jours
     * @param seed La graine à définir
     */
    public void initializeGameSeed(long seed) {
        this.gameSeed = seed;
        game.getRandom().setSeed(seed);
        
        // Initialiser le mélange des jours
        shuffledDays.clear();
        for (int i = 1; i <= 24; i++) {
            shuffledDays.add(i);
        }
        
        // Mélanger les jours
        for (int i = shuffledDays.size - 1; i > 0; i--) {
            int j = game.getRandom().nextInt(i + 1);
            int temp = shuffledDays.get(i);
            shuffledDays.set(i, shuffledDays.get(j));
            shuffledDays.set(j, temp);
        }
    }
    
    /**
     * Récupère la graine du jeu
     * @return La graine du jeu
     */
    public long getGameSeed() {
        return gameSeed;
    }
    
    /**
     * Récupère l'ordre mélangé des jours
     * @return L'array contenant l'ordre des jours
     */
    public Array<Integer> getShuffledDays() {
        return shuffledDays;
    }
} 