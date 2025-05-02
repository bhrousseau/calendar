package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des templates de jeux
 * Charge les données depuis le fichier gameTemplates.json
 */
public class GameTemplateManager {
    private static final String TEMPLATES_FILE = "gameTemplates.json";
    private static GameTemplateManager instance;
    
    private final Map<String, GameTemplate> templates; // Type de jeu -> Template
    
    /**
     * Classe interne représentant un template de jeu
     */
    public static class GameTemplate {
        private final String gameTemplate;
        private final String gameClass;
        private final Map<String, Object> defaultParameters;
        private final Map<String, String> parameterTypes;
        private final Map<String, Map<String, Object>> presets;
        
        public GameTemplate(String gameTemplate, String gameClass, 
                          Map<String, Object> defaultParameters,
                          Map<String, String> parameterTypes,
                          Map<String, Map<String, Object>> presets) {
            this.gameTemplate = gameTemplate;
            this.gameClass = gameClass;
            this.defaultParameters = defaultParameters;
            this.parameterTypes = parameterTypes;
            this.presets = presets;
        }
        
        public String getGameTemplate() {
            return gameTemplate;
        }
        
        public String getGameClass() {
            return gameClass;
        }
        
        public Map<String, Object> getDefaultParameters() {
            return defaultParameters;
        }
        
        public Map<String, String> getParameterTypes() {
            return parameterTypes;
        }
        
        public Map<String, Map<String, Object>> getPresets() {
            return presets;
        }
        
        public Map<String, Object> getPresetParameters(String presetName) {
            return presets.get(presetName);
        }
    }
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameTemplateManager() {
        templates = new HashMap<>();
        loadTemplates();
    }
    
    /**
     * Récupère l'instance unique du gestionnaire de templates
     * @return L'instance du gestionnaire
     */
    public static GameTemplateManager getInstance() {
        if (instance == null) {
            instance = new GameTemplateManager();
        }
        return instance;
    }
    
    /**
     * Charge les templates depuis le fichier JSON
     */
    private void loadTemplates() {
        try {
            FileHandle file = Gdx.files.internal(TEMPLATES_FILE);
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);
            
            // Charger les templates
            JsonValue templatesObj = root.get("gameTemplates");
            if (templatesObj != null) {
                for (JsonValue templateValue = templatesObj.child; templateValue != null; templateValue = templateValue.next) {
                    String gameTemplate = templateValue.name;
                    String gameClass = templateValue.getString("gameClass");
                    
                    // Charger les paramètres par défaut
                    Map<String, Object> defaultParams = new HashMap<>();
                    JsonValue defaultParamsValue = templateValue.get("defaultParameters");
                    if (defaultParamsValue != null) {
                        for (JsonValue param = defaultParamsValue.child; param != null; param = param.next) {
                            addParameterToMap(param, defaultParams);
                        }
                    } else {
                        throw new IllegalStateException("Le template '" + gameTemplate + "' n'a pas de paramètres par défaut");
                    }
                    
                    // Charger les types de paramètres
                    Map<String, String> paramTypes = new HashMap<>();
                    JsonValue paramTypesValue = templateValue.get("parameterTypes");
                    if (paramTypesValue != null) {
                        for (JsonValue param = paramTypesValue.child; param != null; param = param.next) {
                            paramTypes.put(param.name, param.asString());
                        }
                    } else {
                        throw new IllegalStateException("Le template '" + gameTemplate + "' n'a pas de types de paramètres");
                    }
                    
                    // Charger les presets
                    Map<String, Map<String, Object>> presets = new HashMap<>();
                    JsonValue presetsValue = templateValue.get("presets");
                    if (presetsValue != null) {
                        for (JsonValue preset = presetsValue.child; preset != null; preset = preset.next) {
                            String presetName = preset.name;
                            Map<String, Object> presetParams = new HashMap<>();
                            
                            for (JsonValue param = preset.child; param != null; param = param.next) {
                                addParameterToMap(param, presetParams);
                            }
                            
                            presets.put(presetName, presetParams);
                        }
                    }
                    
                    // Créer et stocker le template
                    GameTemplate template = new GameTemplate(gameTemplate, gameClass, defaultParams, paramTypes, presets);
                    templates.put(gameTemplate, template);
                }
            }
            
            System.out.println("Chargement de " + templates.size() + " templates de jeux réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des templates de jeux: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec du chargement des templates de jeux: " + e.getMessage(), e);
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
     * Récupère un template par son type de jeu
     * @param gameTemplate Le type de jeu
     * @return Le template correspondant ou null si non trouvé
     */
    public GameTemplate getTemplateByType(String gameTemplate) {
        if (!templates.containsKey(gameTemplate)) {
            throw new IllegalArgumentException("Aucun template trouvé pour le type de jeu: " + gameTemplate);
        }
        return templates.get(gameTemplate);
    }
    
    /**
     * Vérifie si un template existe pour un type de jeu
     * @param gameTemplate Le type de jeu
     * @return true si le template existe, false sinon
     */
    public boolean hasTemplate(String gameTemplate) {
        return templates.containsKey(gameTemplate);
    }
    
    /**
     * Récupère le nombre total de templates
     * @return Le nombre de templates
     */
    public int getTemplateCount() {
        return templates.size();
    }
} 