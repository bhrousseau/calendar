package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.widedot.calendar.config.Config;
import com.widedot.calendar.game.DynamicGameScreenFactory;
import com.widedot.calendar.config.ThemeManager;
import com.widedot.calendar.data.Theme;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Classe principale du jeu
 */
public class AdventCalendarGame extends Game {
   
    // Batch graphique
    private SpriteBatch batch;
    
    // Gestion temporelle
    private Calendar calendar;
    private final Config config;
    
    // Fabrique d'écrans de jeu
    private final DynamicGameScreenFactory gameScreenFactory;
    
    // Gestionnaires
    private final ThemeManager themeManager;
    
    // Maps pour stocker l'état des peintures
    private final Map<Integer, Boolean> unlockedPaintings;
    private final Map<Integer, Integer> scores;
    private final Map<Integer, Boolean> visitedPaintings;
    private final Random random;
    
    /**
     * Constructeur
     */
    public AdventCalendarGame() {
        this.batch = new SpriteBatch();
        this.calendar = Calendar.getInstance();
        this.config = Config.getInstance();
        this.gameScreenFactory = DynamicGameScreenFactory.getInstance();
        this.themeManager = ThemeManager.getInstance();
        
        this.unlockedPaintings = new HashMap<>();
        this.scores = new HashMap<>();
        this.visitedPaintings = new HashMap<>();
        
        // Initialiser le générateur aléatoire avec le seed de la configuration
        this.random = new Random(config.getGameSeed());
        
        // Initialiser tous les tableaux comme verrouillés
        for (int i = 1; i <= 24; i++) {
            unlockedPaintings.put(i, false);
            scores.put(i, 0);
            visitedPaintings.put(i, false);
        }
        
        // En mode test, tous les jours sont déverrouillés seulement si unlocked=true
        if (config.isTestModeEnabled() && config.isTestUnlocked()) {
            for (int i = 1; i <= 24; i++) {
                unlockedPaintings.put(i, true);
            }
        }
    }
    
    /**
     * Récupère la fabrique d'écrans de jeu
     * @return La fabrique d'écrans de jeu
     */
    public DynamicGameScreenFactory getGameScreenFactory() {
        return gameScreenFactory;
    }
    
    /**
     * Vérifie si un jour peut être déverrouillé
     * @param day L'ID du jour à vérifier
     * @return true si le jour peut être déverrouillé, false sinon
     */
    public boolean canUnlock(int day) {
        // Vérifier si le tableau existe
        if (!themeManager.hasThemeForDay(day)) {
            System.out.println("Aucun tableau associé au jour " + day);
            return false;
        }
        
        // Vérifier si le jour est déjà déverrouillé
        if (unlockedPaintings.get(day)) {
            System.out.println("Le jour " + day + " est déjà déverrouillé");
            return true;
        }
        
        // Vérifier si le jour est valide (entre 1 et 24)
        if (day < 1 || day > 24) {
            System.out.println("Le jour " + day + " n'est pas valide (doit être entre 1 et 24)");
            return false;
        }
        
        // Vérifier si le jour est déjà passé ou en cours
        if (!isDayValid(day)) {
            System.out.println("Le jour " + day + " n'est pas encore valide (date)");
            return false;
        }
        
        // Cas particulier pour le jour 1 (pas de jour précédent)
        if (day == 1) {
            System.out.println("Le jour 1 peut toujours être déverrouillé");
            return true;
        }
        
        // Vérifier si le jour précédent a été déverrouillé et résolu
        int previousDay = day - 1;
        if (!unlockedPaintings.get(previousDay) || scores.get(previousDay) == 0) {
            System.out.println("Le jour précédent " + previousDay + " n'a pas été déverrouillé ou résolu");
            return false;
        }
        
        System.out.println("Le jour " + day + " peut être déverrouillé");
        return true;
    }
    
    /**
     * Vérifie si un jour est valide (déjà passé ou en cours)
     * @param day L'ID du jour à vérifier
     * @return true si le jour est valide, false sinon
     */
    private boolean isDayValid(int day) {
        String calendarMode = config.getCalendarMode();
        
        // Déterminer la date de référence (date actuelle ou date de test)
        Calendar referenceDate = Calendar.getInstance();
        
        // En mode test, vérifier si la date de test est dans le futur
        if (config.isTestModeEnabled()) {
            // Créer une date avec les valeurs de test
            Calendar testDate = Calendar.getInstance();
            testDate.set(config.getTestYear(), config.getTestMonth() - 1, config.getTestDay());
            
            // Utiliser la date de test pour la vérification
            referenceDate.set(config.getTestYear(), config.getTestMonth() - 1, config.getTestDay());
        }

        System.out.println("calendarMode: " + calendarMode);
        System.out.println("referenceDate: " + referenceDate.getTime());
        
        switch (calendarMode) {
            case "month":
                return isValidDayInMonth(day, referenceDate);
            case "year":
                return isValidDayInYear(day, referenceDate);
            default:
                // Mode par défaut est "month"
                return isValidDayInMonth(day, referenceDate);
        }
    }
    
    /**
     * Vérifie si un jour est valide dans le mode "month"
     * @param day Le jour à vérifier
     * @param referenceDate La date de référence (actuelle ou de test)
     * @return true si le jour est valide, false sinon
     */
    private boolean isValidDayInMonth(int day, Calendar referenceDate) {
        if (day < 1 || day > 31) {
            return false;
        }
        
        // Vérifier si le jour demandé est valide pour le mois spécifié
        Calendar targetDateMonth = Calendar.getInstance();
        targetDateMonth.set(config.getMonthModeYear(), config.getMonthModeMonth() - 1, 1);
        int maxDayInMonth = targetDateMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        System.out.println("Mois configuré: " + config.getMonthModeMonth() + ", max jours: " + maxDayInMonth);
        System.out.println("Jour demandé: " + day + ", jour référence: " + referenceDate.get(Calendar.DAY_OF_MONTH));
        
        // Si le jour demandé dépasse le nombre de jours dans le mois, c'est invalide
        if (day > maxDayInMonth) {
            System.out.println("Jour " + day + " dépasse le max de " + maxDayInMonth + " jours dans le mois");
            return false;
        }
        
        // Le jour est valide si le jour courant est >= au jour demandé
        boolean isValid = referenceDate.get(Calendar.DAY_OF_MONTH) >= day;
        System.out.println("Jour " + day + " est " + (isValid ? "valide" : "invalide") + " (jour courant: " + referenceDate.get(Calendar.DAY_OF_MONTH) + ")");
        return isValid;
    }
    
    /**
     * Vérifie si un jour est valide dans le mode "year"
     * @param day Le jour à vérifier (jour de l'année)
     * @param referenceDate La date de référence (actuelle ou de test)
     * @return true si le jour est valide, false sinon
     */
    private boolean isValidDayInYear(int day, Calendar referenceDate) {
        if (day < 1 || day > 366) { // 366 pour supporter les années bissextiles
            return false;
        }
        
        // Vérifier si l'année spécifiée est bissextile
        Calendar yearCal = Calendar.getInstance();
        yearCal.set(Calendar.YEAR, config.getYearModeYear());
        int maxDayInYear = yearCal.getActualMaximum(Calendar.DAY_OF_YEAR);
        
        // Si le jour demandé dépasse le nombre de jours dans l'année, c'est invalide
        if (day > maxDayInYear) {
            return false;
        }
        
        // Le jour est valide si le jour courant de l'année est >= au jour demandé
        return referenceDate.get(Calendar.DAY_OF_YEAR) >= day;
    }
    
    /**
     * Déverrouille un tableau
     * @param day L'ID du jour à déverrouiller
     * @return true si le jour a été déverrouillé, false sinon
     */
    public boolean unlock(int day) {
        if (canUnlock(day)) {
            unlockedPaintings.put(day, true);
            // Réinitialiser l'état de visite
            visitedPaintings.put(day, false);
            return true;
        }
        return false;
    }
    
    /**
     * Vérifie si un tableau est déverrouillé
     * @param day L'ID du jour à vérifier
     * @return true si le tableau est déverrouillé, false sinon
     */
    public boolean isUnlocked(int day) {
        return unlockedPaintings.get(day);
    }
    
    /**
     * Vérifie si un tableau a été visité
     * @param day L'ID du jour à vérifier
     * @return true si le tableau a été visité, false sinon
     */
    public boolean isVisited(int day) {
        return visitedPaintings.containsKey(day) && visitedPaintings.get(day);
    }
    
    /**
     * Marque un tableau comme visité
     * @param day L'ID du jour à marquer
     * @param visited true si visité, false sinon
     */
    public void setVisited(int day, boolean visited) {
        visitedPaintings.put(day, visited);
    }
    
    /**
     * Récupère le thème associé à un jour
     * @param day L'ID du jour
     * @return Le thème associé au jour ou null si non trouvé
     */
    public Theme getThemeForDay(int day) {
        return themeManager.getThemeByDay(day);
    }
    
    /**
     * Récupère le score d'un jour
     * @param day L'ID du jour
     * @return Le score du jour
     */
    public int getScore(int day) {
        return scores.get(day);
    }
    
    /**
     * Définit le score d'un jour
     * @param day L'ID du jour
     * @param score Le score à définir
     */
    public void setScore(int day, int score) {
        scores.put(day, score);
    }
    
    /**
     * Lance le jeu associé à un jour
     * @param day L'ID du jour
     */
    public void launchGame(int day) {
        System.out.println("Tentative de lancement du mini-jeu pour le jour " + day);
        
        // Vérifier d'abord que le jour est valide et déverrouillé
        if (!isDayValid(day)) {
            System.err.println("Le jour " + day + " n'est pas valide");
            return;
        }
        
        if (!isUnlocked(day)) {
            System.err.println("Le jour " + day + " n'est pas déverrouillé");
            return;
        }
        
        // Récupérer le thème pour ce jour
        Theme theme = getThemeForDay(day);
        if (theme == null) {
            System.err.println("Aucun thème trouvé pour le jour " + day);
            return;
        }
        
        // Récupérer le type de jeu associé au jour
        String gameTemplate = themeManager.getGameTemplateForDay(day);
        if (gameTemplate == null || gameTemplate.isEmpty()) {
            System.err.println("Aucun type de jeu défini pour le jour " + day);
            return;
        }
        
        try {
            // Créer et afficher l'écran de jeu correspondant
            System.out.println("Lancement du jeu " + gameTemplate + " pour le jour " + day);
            setScreen(gameScreenFactory.createGameScreen(day, gameTemplate, this));
        } catch (Exception e) {
            System.err.println("Erreur lors du lancement du jeu pour le jour " + day + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void create() {
        // En mode test, utiliser une date fixe
        if (config.isTestModeEnabled()) {
            calendar.set(config.getTestYear(), config.getTestMonth(), config.getTestDay());
        }
        
        // Aller à l'écran du calendrier de l'avent
        setScreen(new AdventCalendarScreen(this));
    }
    
    @Override
    public void render() {
        // Effacer l'écran
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // Rendre l'écran actif
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
        // Libérer les ressources
        batch.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
    
    /**
     * Récupère le batch graphique
     * @return Le batch graphique
     */
    public SpriteBatch getBatch() {
        return batch;
    }
    
    /**
     * Récupère le générateur aléatoire
     * @return Le générateur aléatoire
     */
    public Random getRandom() {
        return random;
    }
} 