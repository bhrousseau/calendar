package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Gestionnaire des templates de jeux
 * Charge les données depuis le fichier gameTemplates.json
 */
public class GameTemplateManager {
    private static final String TEMPLATES_FILE = "gameTemplates.json";
    private static GameTemplateManager instance;
    
    private final ObjectMap<String, GameTemplate> templates; // Type de jeu -> Template
    
    /**
     * Classe interne représentant un template de jeu
     */
    public static class GameTemplate {
        private final String gameTemplate;
        private final String name;
        private final ObjectMap<String, Object> defaultParameters;
        private final ObjectMap<String, String> parameterTypes;
        private final ObjectMap<String, ObjectMap<String, Object>> presets;
        
        public GameTemplate(String gameTemplate, String name, 
                          ObjectMap<String, Object> defaultParameters,
                          ObjectMap<String, String> parameterTypes,
                          ObjectMap<String, ObjectMap<String, Object>> presets) {
            this.gameTemplate = gameTemplate;
            this.name = name;
            this.defaultParameters = defaultParameters;
            this.parameterTypes = parameterTypes;
            this.presets = presets;
        }
        
        public String getGameTemplate() {
            return gameTemplate;
        }
        
        public String getName() {
            return name;
        }
        
        public ObjectMap<String, Object> getDefaultParameters() {
            return defaultParameters;
        }
        
        public ObjectMap<String, String> getParameterTypes() {
            return parameterTypes;
        }
        
        public ObjectMap<String, ObjectMap<String, Object>> getPresets() {
            return presets;
        }
        
        public ObjectMap<String, Object> getPresetParameters(String presetName) {
            return presets.get(presetName);
        }
    }
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private GameTemplateManager() {
        templates = new ObjectMap<>();
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
                    String name = templateValue.getString("name");
                    
                    // Charger les paramètres par défaut
                    ObjectMap<String, Object> defaultParams = new ObjectMap<>();
                    JsonValue defaultParamsValue = templateValue.get("defaultParameters");
                    if (defaultParamsValue != null) {
                        for (JsonValue param = defaultParamsValue.child; param != null; param = param.next) {
                            addParameterToMap(param, defaultParams);
                        }
                    } else {
                        throw new IllegalStateException("Le template '" + gameTemplate + "' n'a pas de paramètres par défaut");
                    }
                    
                    // Charger les types de paramètres
                    ObjectMap<String, String> paramTypes = new ObjectMap<>();
                    JsonValue paramTypesValue = templateValue.get("parameterTypes");
                    if (paramTypesValue != null) {
                        for (JsonValue param = paramTypesValue.child; param != null; param = param.next) {
                            paramTypes.put(param.name, param.asString());
                        }
                    } else {
                        throw new IllegalStateException("Le template '" + gameTemplate + "' n'a pas de types de paramètres");
                    }
                    
                    // Charger les presets
                    ObjectMap<String, ObjectMap<String, Object>> presets = new ObjectMap<>();
                    JsonValue presetsValue = templateValue.get("presets");
                    if (presetsValue != null) {
                        for (JsonValue preset = presetsValue.child; preset != null; preset = preset.next) {
                            String presetName = preset.name;
                            ObjectMap<String, Object> presetParams = new ObjectMap<>();
                            
                            for (JsonValue param = preset.child; param != null; param = param.next) {
                                addParameterToMap(param, presetParams);
                            }
                            
                            presets.put(presetName, presetParams);
                        }
                    }
                    
                    // Créer et stocker le template
                    GameTemplate template = new GameTemplate(gameTemplate, name, defaultParams, paramTypes, presets);
                    templates.put(gameTemplate, template);
                }
            }
            
            Gdx.app.log("GameTemplateManager", "Chargement de " + templates.size + " templates de jeux réussi");
        } catch (Exception e) {
            Gdx.app.error("GameTemplateManager", "Erreur lors du chargement des templates de jeux: " + e.getMessage(), e);
            throw new RuntimeException("Échec du chargement des templates de jeux: " + e.getMessage(), e);
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
        return templates.size;
    }
} 