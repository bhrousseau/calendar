package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * Gestionnaire des configurations de jeux
 * Charge les données depuis le fichier games.json
 */
public class GameManager {
    private static final String GAMES_FILE = "games.json";
    private static GameManager instance;
    
    private final Map<String, GameConfig> gamesByReference;
    private final Array<GameConfig> allGames;
    
    /**
     * Classe interne représentant une configuration de jeu
     */
    public static class GameConfig {
        private final String reference;
        private final String gameTemplate;
        private final String theme;
        private final List<String> presets;
        private final Map<String, Object> parameters;
        
        public GameConfig(String reference, String gameTemplate, String theme, 
                        List<String> presets, Map<String, Object> parameters) {
            this.reference = reference;
            this.gameTemplate = gameTemplate;
            this.theme = theme;
            this.presets = presets;
            this.parameters = parameters;
        }
        
        public String getReference() {
            return reference;
        }
        
        public String getGameTemplate() {
            return gameTemplate;
        }
        
        public String getTheme() {
            return theme;
        }
        
        public List<String> getPresets() {
            return presets;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
    }
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameManager() {
        gamesByReference = new HashMap<>();
        allGames = new Array<>();
        loadGames();
    }
    
    /**
     * Récupère l'instance unique du gestionnaire de jeux
     * @return L'instance du gestionnaire
     */
    public static GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }
    
    /**
     * Charge les configurations de jeux depuis le fichier JSON
     */
    private void loadGames() {
        try {
            FileHandle file = Gdx.files.internal(GAMES_FILE);
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);
            
            // Charger les jeux
            JsonValue gamesArray = root.get("games");
            if (gamesArray != null) {
                for (JsonValue gameValue = gamesArray.child; gameValue != null; gameValue = gameValue.next) {
                    String reference = gameValue.getString("reference");
                    String gameTemplate = gameValue.getString("gameTemplate");
                    String theme = gameValue.getString("theme");
                    
                    // Charger les presets
                    List<String> presets = new ArrayList<>();
                    JsonValue presetsValue = gameValue.get("presets");
                    if (presetsValue != null && presetsValue.isArray()) {
                        for (int i = 0; i < presetsValue.size; i++) {
                            presets.add(presetsValue.getString(i));
                        }
                    }
                    
                    // Charger les paramètres
                    Map<String, Object> parameters = new HashMap<>();
                    JsonValue paramsValue = gameValue.get("parameters");
                    if (paramsValue != null) {
                        for (JsonValue param = paramsValue.child; param != null; param = param.next) {
                            addParameterToMap(param, parameters);
                        }
                    }
                    
                    // Créer et stocker la configuration
                    GameConfig gameConfig = new GameConfig(reference, gameTemplate, theme, presets, parameters);
                    gamesByReference.put(reference, gameConfig);
                    allGames.add(gameConfig);
                }
            }
            
            System.out.println("Chargement de " + allGames.size + " configurations de jeux réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des configurations de jeux: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ajoute un paramètre à une map en fonction de son type
     */
    private void addParameterToMap(JsonValue param, Map<String, Object> map) {
        String name = param.name;
        if (param.isNumber()) {
            if (param.asString().contains(".")) {
                map.put(name, param.asFloat());
            } else {
                map.put(name, param.asInt());
            }
        } else if (param.isBoolean()) {
            map.put(name, param.asBoolean());
        } else {
            map.put(name, param.asString());
        }
    }
    
    /**
     * Récupère une configuration de jeu par sa référence
     * @param reference La référence du jeu
     * @return La configuration correspondante ou null si non trouvée
     */
    public GameConfig getGameByReference(String reference) {
        return gamesByReference.get(reference);
    }
    
    /**
     * Récupère toutes les configurations de jeux
     * @return Un tableau contenant toutes les configurations de jeux
     */
    public Array<GameConfig> getAllGames() {
        return allGames;
    }
    
    /**
     * Récupère les configurations de jeux d'un type donné
     * @param gameTemplate Le type de jeu
     * @return Une liste des configurations du type spécifié
     */
    public List<GameConfig> getGamesByType(String gameTemplate) {
        List<GameConfig> result = new ArrayList<>();
        for (GameConfig config : allGames) {
            if (config.getGameTemplate().equals(gameTemplate)) {
                result.add(config);
            }
        }
        return result;
    }
    
    /**
     * Vérifie si une configuration de jeu existe
     * @param reference La référence du jeu
     * @return true si la configuration existe, false sinon
     */
    public boolean hasGame(String reference) {
        return gamesByReference.containsKey(reference);
    }
    
    /**
     * Récupère le nombre total de configurations de jeux
     * @return Le nombre de configurations
     */
    public int getGameCount() {
        return allGames.size;
    }
} 