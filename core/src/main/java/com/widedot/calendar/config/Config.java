package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Classe qui gère la configuration du jeu à partir d'un fichier JSON.
 */
public class Config {
    private static Config instance;
    private final JsonValue config;
    
    // Constantes pour les mois
    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;
    
    // Valeurs par défaut
    private boolean testModeEnabled = true;
    private int testDay = 24;
    private int testMonth = DECEMBER;
    private int testYear = 2023;
    private boolean testUnlocked = false;
    private boolean testUseSave = true;  // Par défaut, on utilise la sauvegarde
    private long gameSeed = 12345;
    
    // Nouveaux paramètres pour les modes de calendrier
    private String calendarMode = "month";  // Valeurs possibles: "month" ou "year"
    private int monthModeMonth = DECEMBER;
    private int monthModeYear = 2023;
    private int yearModeYear = 2023;
    
    /**
     * Constructeur privé pour le pattern Singleton.
     */
    private Config() {
        JsonReader jsonReader = new JsonReader();
        FileHandle configFile = Gdx.files.internal("config.json");
        
        if (configFile.exists()) {
            config = jsonReader.parse(configFile);
            loadConfig();
        } else {
            config = null;
            Gdx.app.log("Config", "Fichier de configuration non trouvé. Utilisation des valeurs par défaut.");
        }
    }
    
    /**
     * Obtient l'instance unique de GameConfig (pattern Singleton).
     * @return L'instance de GameConfig
     */
    public static Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }
    
    /**
     * Charge la configuration à partir du fichier JSON.
     */
    private void loadConfig() {
        if (config != null) {
            // Charger les paramètres de test
            JsonValue testMode = config.get("test_mode");
            if (testMode != null) {
                testModeEnabled = testMode.getBoolean("enabled", testModeEnabled);
                testDay = testMode.getInt("day", testDay);
                testMonth = testMode.getInt("month", testMonth);
                testYear = testMode.getInt("year", testYear);
                testUnlocked = testMode.getBoolean("unlocked", testUnlocked);
                testUseSave = testMode.getBoolean("use_save", testUseSave);
            }
            
            // Charger les paramètres du jeu
            JsonValue game = config.get("game");
            if (game != null) {
                gameSeed = game.getLong("seed", gameSeed);
                
                // Charger le mode de calendrier
                calendarMode = game.getString("calendar_mode", calendarMode);
                
                // Charger les paramètres du mode "month"
                JsonValue monthMode = game.get("month_mode");
                if (monthMode != null) {
                    monthModeMonth = monthMode.getInt("month", monthModeMonth);
                    monthModeYear = monthMode.getInt("year", monthModeYear);
                }
                
                // Charger les paramètres du mode "year"
                JsonValue yearMode = game.get("year_mode");
                if (yearMode != null) {
                    yearModeYear = yearMode.getInt("year", yearModeYear);
                }
            }
        }
    }
    
    /**
     * Vérifie si le mode test est activé.
     * @return true si le mode test est activé, false sinon
     */
    public boolean isTestModeEnabled() {
        return testModeEnabled;
    }
    
    /**
     * Obtient le jour de test.
     * @return Le jour de test
     */
    public int getTestDay() {
        return testDay;
    }
    
    /**
     * Obtient le mois de test.
     * @return Le mois de test
     */
    public int getTestMonth() {
        return testMonth;
    }
    
    /**
     * Obtient l'année de test.
     * @return L'année de test
     */
    public int getTestYear() {
        return testYear;
    }
    
    /**
     * Vérifie si les cases doivent être déverrouillées automatiquement en mode test.
     * @return true si les cases doivent être déverrouillées, false sinon
     */
    public boolean isTestUnlocked() {
        return testUnlocked;
    }
    
    /**
     * Vérifie si la sauvegarde doit être utilisée en mode test.
     * @return true si la sauvegarde doit être utilisée, false sinon
     */
    public boolean isTestUseSave() {
        return testUseSave;
    }
    
    /**
     * Obtient le seed du jeu.
     * @return Le seed du jeu
     */
    public long getGameSeed() {
        return gameSeed;
    }
    
    /**
     * Obtient le mode de calendrier.
     * @return Le mode de calendrier ("day", "month" ou "year")
     */
    public String getCalendarMode() {
        return calendarMode;
    }
    
    /**
     * Obtient le mois configuré pour le mode "month".
     * @return Le mois configuré
     */
    public int getMonthModeMonth() {
        return monthModeMonth;
    }
    
    /**
     * Obtient l'année configurée pour le mode "month".
     * @return L'année configurée
     */
    public int getMonthModeYear() {
        return monthModeYear;
    }
    
    /**
     * Obtient l'année configurée pour le mode "year".
     * @return L'année configurée
     */
    public int getYearModeYear() {
        return yearModeYear;
    }
} 