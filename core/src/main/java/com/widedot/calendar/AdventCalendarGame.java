package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.config.GameConfig;
import com.widedot.calendar.game.DefaultGameScreenFactory;
import com.widedot.calendar.game.GameScreenFactory;
import com.widedot.calendar.painting.Painting;
import com.widedot.calendar.painting.PaintingManager;
import com.widedot.calendar.screens.GameScreen;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AdventCalendarGame extends Game {
    private final SpriteBatch batch;
    private final Map<Integer, Boolean> unlockedPaintings;
    private final Map<Integer, Integer> scores;
    private final Map<Integer, Boolean> visitedPaintings;
    private final Random random;
    private final GameConfig config;
    private final PaintingManager paintingManager;
    private final GameScreenFactory gameScreenFactory;
    
    public AdventCalendarGame() {
        this.batch = new SpriteBatch();
        this.unlockedPaintings = new HashMap<>();
        this.scores = new HashMap<>();
        this.visitedPaintings = new HashMap<>();
        this.config = GameConfig.getInstance();
        this.paintingManager = PaintingManager.getInstance();
        this.gameScreenFactory = DefaultGameScreenFactory.getInstance();
        
        // Initialiser le générateur aléatoire avec le seed de la configuration
        this.random = new Random(config.getGameSeed());
        
        // Initialiser tous les tableaux comme verrouillés
        for (int i = 1; i <= 24; i++) {
            unlockedPaintings.put(i, false);
            scores.put(i, 0);
            visitedPaintings.put(i, false);
        }
    }

    /**
     * Vérifie si un jour peut être déverrouillé
     * @param dayId L'ID du jour à vérifier
     * @return true si le jour peut être déverrouillé, false sinon
     */
    public boolean canUnlockDay(int dayId) {
        // Vérifier si le tableau existe
        if (!paintingManager.hasPaintingForDay(dayId)) {
            System.out.println("Aucun tableau associé au jour " + dayId);
            return false;
        }
        
        // Vérifier si le jour est déjà déverrouillé
        if (unlockedPaintings.get(dayId)) {
            System.out.println("Le jour " + dayId + " est déjà déverrouillé");
            return true;
        }
        
        // Vérifier si le jour est valide (entre 1 et 24)
        if (dayId < 1 || dayId > 24) {
            System.out.println("Le jour " + dayId + " n'est pas valide (doit être entre 1 et 24)");
            return false;
        }
        
        // Vérifier si le jour est déjà passé ou en cours
        if (!isDayValid(dayId)) {
            System.out.println("Le jour " + dayId + " n'est pas encore valide (date)");
            return false;
        }
        
        // Cas particulier pour le jour 1 (pas de jour précédent)
        if (dayId == 1) {
            System.out.println("Le jour 1 peut toujours être déverrouillé");
            return true;
        }
        
        // Vérifier si le jour précédent a été déverrouillé et résolu
        int previousDay = dayId - 1;
        if (!unlockedPaintings.get(previousDay) || scores.get(previousDay) == 0) {
            System.out.println("Le jour précédent " + previousDay + " n'a pas été déverrouillé ou résolu");
            return false;
        }
        
        System.out.println("Le jour " + dayId + " peut être déverrouillé");
        return true;
    }
    
    /**
     * Vérifie si un jour est valide (déjà passé ou en cours)
     * @param dayId L'ID du jour à vérifier
     * @return true si le jour est valide, false sinon
     */
    private boolean isDayValid(int dayId) {
        if (config.isTestModeEnabled()) {
            // En mode test, utiliser la date de test
            return dayId <= config.getTestDay();
        } else {
            // Utiliser la date réelle
            Calendar now = Calendar.getInstance();
            int currentYear = now.get(Calendar.YEAR);
            int currentMonth = now.get(Calendar.MONTH);
            int currentDay = now.get(Calendar.DAY_OF_MONTH);
            
            // Le jeu n'est jouable qu'en décembre
            if (currentMonth != Calendar.DECEMBER) {
                return false;
            }
            
            // Le jeu est jouable jusqu'au 31 décembre
            if (currentDay > 31) {
                return false;
            }
            
            // Vérifier si le jour est déjà passé ou en cours
            return dayId <= currentDay;
        }
    }

    /**
     * Déverrouille un tableau
     * @param dayId L'ID du jour à déverrouiller
     */
    public void unlockPainting(int dayId) {
        if (canUnlockDay(dayId)) {
            unlockedPaintings.put(dayId, true);
            // Réinitialiser l'état de visite
            visitedPaintings.put(dayId, false);
        }
    }

    /**
     * Vérifie si un tableau est déverrouillé
     * @param dayId L'ID du jour à vérifier
     * @return true si le tableau est déverrouillé, false sinon
     */
    public boolean isPaintingUnlocked(int dayId) {
        return unlockedPaintings.get(dayId);
    }
    
    /**
     * Vérifie si un tableau a été visité
     * @param dayId L'ID du jour à vérifier
     * @return true si le tableau a été visité, false sinon
     */
    public boolean isPaintingVisited(int dayId) {
        return visitedPaintings.get(dayId);
    }
    
    /**
     * Marque un tableau comme visité
     * @param dayId L'ID du jour à marquer comme visité
     */
    public void markPaintingAsVisited(int dayId) {
        visitedPaintings.put(dayId, true);
    }

    /**
     * Récupère le score d'un jour
     * @param dayId L'ID du jour
     * @return Le score du jour
     */
    public int getScore(int dayId) {
        return scores.get(dayId);
    }

    /**
     * Définit le score d'un jour
     * @param dayId L'ID du jour
     * @param score Le score à définir
     */
    public void setScore(int dayId, int score) {
        scores.put(dayId, score);
    }
    
    /**
     * Récupère la map des tableaux déverrouillés
     * @return La map des tableaux déverrouillés
     */
    public Map<Integer, Boolean> getUnlockedPaintings() {
        return unlockedPaintings;
    }
    
    /**
     * Récupère la map des scores
     * @return La map des scores
     */
    public Map<Integer, Integer> getScores() {
        return scores;
    }
    
    /**
     * Récupère la map des tableaux visités
     * @return La map des tableaux visités
     */
    public Map<Integer, Boolean> getVisitedPaintings() {
        return visitedPaintings;
    }
    
    /**
     * Récupère le générateur aléatoire
     * @return Le générateur aléatoire
     */
    public Random getRandom() {
        return random;
    }
    
    /**
     * Récupère un tableau par son identifiant de jour
     * @param dayId L'identifiant du jour
     * @return Le tableau correspondant ou null si non trouvé
     */
    public Painting getPainting(int dayId) {
        return paintingManager.getPaintingByDay(dayId);
    }
    
    /**
     * Récupère le type de jeu associé à un tableau
     * @param dayId L'identifiant du tableau
     * @return Le type de jeu ou null si le tableau n'existe pas
     */
    public String getGameType(int dayId) {
        return PaintingManager.getInstance().getGameTypeForDay(dayId);
    }

    /**
     * Lance le jeu pour un jour donné
     * @param dayId L'identifiant du jour
     */
    public void launchGame(int dayId) {
        System.out.println("Tentative de lancement du mini-jeu pour le jour " + dayId);
        if (isDayValid(dayId) && isPaintingUnlocked(dayId)) {
            Painting painting = getPainting(dayId);
            if (painting != null && this != null) {
                
                // Sauvegarder l'écran actuel pour pouvoir y revenir
                Screen currentScreen = getScreen();
                
                // Marquer le tableau comme visité
                markPaintingAsVisited(dayId);
                
                try {
                    // Créer et définir le nouvel écran de jeu
                    GameScreen gameScreen = gameScreenFactory.createGameScreen(dayId, painting, this);
                    
                    // Mettre à jour l'écran de AdventCalendarGame
                    setScreen(gameScreen);
                    
                    // Mettre à jour l'écran de Main
                    if (this instanceof Game) {
                        Game mainGame = (Game) Gdx.app.getApplicationListener();
                        if (mainGame instanceof Main) {
                            ((Main) mainGame).setScreen(gameScreen);
                            System.out.println("Écran de Main mis à jour avec: " + gameScreen.getClass().getSimpleName());
                        }
                    }

                } catch (Exception e) {
                    System.err.println("ERREUR lors de la création du nouvel écran: " + e.getMessage());
                    e.printStackTrace();
                    // En cas d'erreur, revenir à l'écran précédent
                    if (currentScreen != null) {
                        setScreen(currentScreen);
                    }
                }
            } else {
                System.out.println("Peinture non trouvée pour le jour " + dayId);
            }
        } else {
            System.out.println("Le jour " + dayId + " n'est pas valide ou n'est pas déverrouillé");
        }
    }

    @Override
    public void create() {
    }
    
    @Override
    public void render() {
        super.render();
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
    
    @Override
    public void pause() {
        super.pause();
    }
    
    @Override
    public void resume() {
        super.resume();
    }
    
    @Override
    public void dispose() {
        batch.dispose();
        super.dispose();
    }
} 