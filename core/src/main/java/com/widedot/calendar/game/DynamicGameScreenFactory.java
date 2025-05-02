package com.widedot.calendar.game;

import com.badlogic.gdx.Game;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.screens.GameScreen;
import com.widedot.calendar.config.GameManager;
import com.widedot.calendar.config.GameTemplateManager;
import com.widedot.calendar.config.ThemeManager;
import com.widedot.calendar.config.DayMappingManager;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Implémentation dynamique de la fabrique d'écrans de jeu qui utilise la réflexion
 * pour instancier les écrans de jeu à partir des configurations JSON
 */
public class DynamicGameScreenFactory {
    private static DynamicGameScreenFactory instance;

    private final Map<String, Class<?>> gameClassCache;

    /**
     * Constructeur privé pour le pattern Singleton
     */
    private DynamicGameScreenFactory() {
        gameClassCache = new HashMap<>();
    }

    /**
     * Récupère l'instance unique de DynamicGameScreenFactory (pattern Singleton)
     * @return L'instance de DynamicGameScreenFactory
     */
    public static DynamicGameScreenFactory getInstance() {
        if (instance == null) {
            instance = new DynamicGameScreenFactory();
        }
        return instance;
    }

    /**
     * Crée un écran de jeu pour le jour et le template spécifiés
     * @param dayId L'ID du jour
     * @param gameTemplate Le type de jeu à créer
     * @param game L'instance du jeu principal
     * @return L'écran de jeu créé
     */
    public GameScreen createGameScreen(int dayId, String gameTemplate, Game game) {
        if (gameTemplate == null || gameTemplate.isEmpty()) {
            throw new IllegalArgumentException("Le type de jeu ne peut pas être null ou vide");
        }

        // Récupérer les gestionnaires nécessaires
        GameTemplateManager templateManager = GameTemplateManager.getInstance();
        GameManager gameManager = GameManager.getInstance();
        DayMappingManager dayMappingManager = DayMappingManager.getInstance();

        // Récupérer le template de jeu
        GameTemplateManager.GameTemplate template = templateManager.getTemplateByType(gameTemplate);
        if (template == null) {
            throw new IllegalArgumentException("Aucun template trouvé pour le type de jeu: " + gameTemplate);
        }

        // Récupérer la référence de jeu à partir du mapping jour -> jeu
        String gameReference = dayMappingManager.getGameReferenceForDay(dayId);
        if (gameReference == null) {
            throw new IllegalArgumentException("Aucune référence de jeu trouvée pour le jour: " + dayId);
        }

        // Récupérer la configuration de jeu associée à la référence de jeu
        GameManager.GameConfig gameConfig = gameManager.getGameByReference(gameReference);
        if (gameConfig == null) {
            throw new IllegalArgumentException("Aucune configuration trouvée pour le jeu: " + gameReference);
        }

        // Créer l'écran de jeu
        return createGameScreen(gameConfig, template, dayId, game);
    }

     /**
     * Crée un écran de jeu à partir d'une configuration et d'un template
     * @param gameConfig La configuration du jeu
     * @param template Le template du jeu
     * @param dayId L'identifiant du jour
     * @param game L'instance du jeu
     * @return L'écran de jeu créé
     */
    private GameScreen createGameScreen(GameManager.GameConfig gameConfig,
                                        GameTemplateManager.GameTemplate template,
                                        int dayId, Game game) {
        // Fusionner les paramètres dans l'ordre de priorité:
        // 1. Paramètres par défaut du template
        // 2. Paramètres des presets appliqués
        // 3. Paramètres spécifiques de la configuration
        Map<String, Object> finalParameters = new HashMap<>(template.getDefaultParameters());

        // Appliquer les presets
        List<String> presets = gameConfig.getPresets();
        if (presets != null) {
            for (String preset : presets) {
                Map<String, Object> presetParams = template.getPresetParameters(preset);
                if (presetParams != null) {
                    finalParameters.putAll(presetParams);
                } else {
                    System.err.println("ATTENTION: Preset '" + preset + "' non trouvé pour le type de jeu " + template.getGameTemplate());
                }
            }
        }

        // Appliquer les paramètres spécifiques
        Map<String, Object> specificParams = gameConfig.getParameters();
        if (specificParams != null) {
            finalParameters.putAll(specificParams);
        }

        // Récupérer le thème associé
        String themeName = gameConfig.getTheme();
        Theme theme = null;
        if (themeName != null) {
            theme = ThemeManager.getInstance().getThemeByName(themeName);
            System.out.println("Chargement du thème " + themeName + " -> " + (theme != null ? "trouvé" : "non trouvé"));
            if (theme == null) {
                throw new IllegalArgumentException("Thème non trouvé: " + themeName);
            }
        } else {
            throw new IllegalArgumentException("Aucun thème spécifié pour la configuration du jeu");
        }

        try {
            // Récupérer la classe du jeu
            String gameClassName = "com.widedot.calendar.screens." + template.getGameClass();
            Class<?> gameClass = getGameClass(gameClassName);

            // Créer l'instance du jeu avec le constructeur approprié
            Constructor<?> constructor;
            Object[] parameters;

            System.out.println("Création d'un jeu de type " + template.getGameTemplate() + " avec la classe " + gameClassName);
            System.out.println("Thème utilisé: " + (theme == null ? "null" : theme.getName()));
            System.out.println("Paramètres: " + finalParameters);

            try {
                // Essayer d'abord le constructeur avec paramètres (dayId, game, theme, parameters)
                constructor = gameClass.getConstructor(int.class, Game.class, Theme.class, Map.class);
                parameters = new Object[]{dayId, game, theme, finalParameters};
                System.out.println("Utilisation du constructeur avec (dayId, game, theme, parameters)");
            } catch (NoSuchMethodException e1) {
                try {
                    // Sinon essayer le constructeur avec paramètres (dayId, game, parameters)
                    constructor = gameClass.getConstructor(int.class, Game.class, Map.class);
                    parameters = new Object[]{dayId, game, finalParameters};
                    System.out.println("Utilisation du constructeur avec (dayId, game, parameters)");
                } catch (NoSuchMethodException e2) {
                    throw new RuntimeException("Aucun constructeur compatible trouvé pour la classe " + gameClassName);
                }
            }

            return (GameScreen) constructor.newInstance(parameters);
        } catch (Exception e) {
            System.err.println("ERREUR lors de l'instanciation de l'écran de jeu: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Échec d'instanciation de l'écran de jeu pour le jour " + dayId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Récupère la classe d'un écran de jeu à partir de son nom complet
     * @param className Le nom complet de la classe
     * @return La classe
     * @throws ClassNotFoundException Si la classe n'est pas trouvée
     */
    private Class<?> getGameClass(String className) throws ClassNotFoundException {
        // Vérifier si la classe est déjà dans le cache
        if (gameClassCache.containsKey(className)) {
            return gameClassCache.get(className);
        }

        // Sinon charger la classe et la mettre en cache
        Class<?> gameClass = Class.forName(className);
        gameClassCache.put(className, gameClass);
        return gameClass;
    }
}
