package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Gestionnaire des configurations de jeux
 * Charge les données depuis le fichier games.json
 */
public class GameManager {
    private static final String GAMES_FILE = "games.json";
    private static GameManager instance;
    
    private final ObjectMap<String, GameConfig> gamesByReference;
    private final Array<GameConfig> allGames;
    
    /**
     * Classe interne représentant une configuration de jeu
     */
    public static class GameConfig {
        private final String reference;
        private final String gameTemplate;
        private final String theme;
        private final Array<String> presets;
        private final ObjectMap<String, Object> parameters;
        
        public GameConfig(String reference, String gameTemplate, String theme, 
                        Array<String> presets, ObjectMap<String, Object> parameters) {
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
        
        public Array<String> getPresets() {
            return presets;
        }
        
        public ObjectMap<String, Object> getParameters() {
            return parameters;
        }
    }
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameManager() {
        gamesByReference = new ObjectMap<>();
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
                    Array<String> presets = new Array<>();
                    JsonValue presetsValue = gameValue.get("presets");
                    if (presetsValue != null) {
                        for (JsonValue preset = presetsValue.child; preset != null; preset = preset.next) {
                            presets.add(preset.asString());
                        }
                    }
                    
                    // Charger les paramètres
                    ObjectMap<String, Object> parameters = new ObjectMap<>();
                    JsonValue paramsValue = gameValue.get("parameters");
                    if (paramsValue != null) {
                        for (JsonValue param = paramsValue.child; param != null; param = param.next) {
                            addParameterToMap(param, parameters);
                        }
                    }
                    
                    // Créer et stocker la configuration
                    GameConfig config = new GameConfig(reference, gameTemplate, theme, presets, parameters);
                    gamesByReference.put(reference, config);
                    allGames.add(config);
                }
            }
            
            Gdx.app.log("GameManager", "Chargement de " + allGames.size + " configurations de jeux réussi");
        } catch (Exception e) {
            Gdx.app.error("GameManager", "Erreur lors du chargement des configurations de jeux: " + e.getMessage(), e);
            throw new RuntimeException("Échec du chargement des configurations de jeux: " + e.getMessage(), e);
        }
    }
    
    /**
     * Ajoute un paramètre à une map en fonction de son type
     */
    private void addParameterToMap(JsonValue param, ObjectMap<String, Object> map) {
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
    public Array<GameConfig> getGamesByType(String gameTemplate) {
        Array<GameConfig> result = new Array<>();
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