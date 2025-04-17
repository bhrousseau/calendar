package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Calendar;

/**
 * Classe qui gère la configuration du jeu à partir d'un fichier JSON.
 */
public class GameConfig {
    private static GameConfig instance;
    private final JsonValue config;
    
    // Valeurs par défaut
    private boolean testModeEnabled = true;
    private int testDay = 24;
    private int testMonth = Calendar.DECEMBER;
    private int testYear = 2023;
    private long gameSeed = 12345;
    
    /**
     * Constructeur privé pour le pattern Singleton.
     */
    private GameConfig() {
        JsonReader jsonReader = new JsonReader();
        FileHandle configFile = Gdx.files.internal("config.json");
        
        if (configFile.exists()) {
            config = jsonReader.parse(configFile);
            loadConfig();
        } else {
            config = null;
            System.out.println("Fichier de configuration non trouvé. Utilisation des valeurs par défaut.");
        }
    }
    
    /**
     * Obtient l'instance unique de GameConfig (pattern Singleton).
     * @return L'instance de GameConfig
     */
    public static GameConfig getInstance() {
        if (instance == null) {
            instance = new GameConfig();
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
            }
            
            // Charger les paramètres du jeu
            JsonValue game = config.get("game");
            if (game != null) {
                gameSeed = game.getLong("seed", gameSeed);
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
     * Obtient le seed du jeu.
     * @return Le seed du jeu
     */
    public long getGameSeed() {
        return gameSeed;
    }
} 