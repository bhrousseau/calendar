package com.widedot.calendar.game;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Classe qui gère l'état du jeu (scores, déverrouillage, etc.)
 */
public class GameState {
    private static GameState instance;
    
    private final Map<Integer, Boolean> unlockedPaintings;
    private final Map<Integer, Integer> scores;
    private final Map<Integer, Boolean> visitedPaintings;
    private final Random random;
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameState() {
        this.unlockedPaintings = new HashMap<>();
        this.scores = new HashMap<>();
        this.visitedPaintings = new HashMap<>();
        this.random = new Random();
        
        // Initialiser tous les tableaux comme verrouillés
        for (int i = 1; i <= 24; i++) {
            unlockedPaintings.put(i, false);
            scores.put(i, 0);
            visitedPaintings.put(i, false);
        }
    }
    
    /**
     * Récupère l'instance unique de GameState (pattern Singleton)
     * @return L'instance de GameState
     */
    public static GameState getInstance() {
        if (instance == null) {
            instance = new GameState();
        }
        return instance;
    }
    
    /**
     * Déverrouille un tableau pour un jour donné
     * @param dayId L'identifiant du jour
     */
    public void unlockPainting(int dayId) {
        if (dayId >= 1 && dayId <= 24) {
            unlockedPaintings.put(dayId, true);
        }
    }
    
    /**
     * Vérifie si un tableau est déverrouillé pour un jour donné
     * @param dayId L'identifiant du jour
     * @return true si le tableau est déverrouillé, false sinon
     */
    public boolean isPaintingUnlocked(int dayId) {
        return unlockedPaintings.getOrDefault(dayId, false);
    }
    
    /**
     * Vérifie si un tableau a été visité pour un jour donné
     * @param dayId L'identifiant du jour
     * @return true si le tableau a été visité, false sinon
     */
    public boolean isPaintingVisited(int dayId) {
        return visitedPaintings.getOrDefault(dayId, false);
    }
    
    /**
     * Marque un tableau comme visité pour un jour donné
     * @param dayId L'identifiant du jour
     */
    public void markPaintingAsVisited(int dayId) {
        if (dayId >= 1 && dayId <= 24) {
            visitedPaintings.put(dayId, true);
        }
    }
    
    /**
     * Récupère le score pour un jour donné
     * @param dayId L'identifiant du jour
     * @return Le score
     */
    public int getScore(int dayId) {
        return scores.getOrDefault(dayId, 0);
    }
    
    /**
     * Définit le score pour un jour donné
     * @param dayId L'identifiant du jour
     * @param score Le score à définir
     */
    public void setScore(int dayId, int score) {
        if (dayId >= 1 && dayId <= 24) {
            scores.put(dayId, score);
        }
    }
    
    /**
     * Récupère le générateur aléatoire
     * @return Le générateur aléatoire
     */
    public Random getRandom() {
        return random;
    }
    
    /**
     * Définit la graine du générateur aléatoire
     * @param seed La graine à définir
     */
    public void setRandomSeed(long seed) {
        random.setSeed(seed);
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
            unlockedPaintings.put(i, false);
            scores.put(i, 0);
            visitedPaintings.put(i, false);
        }
    }
} 