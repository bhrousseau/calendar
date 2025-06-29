package com.widedot.calendar.game;

import com.badlogic.gdx.Game;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.screens.GameScreen;
import com.widedot.calendar.config.GameManager;
import com.widedot.calendar.config.GameTemplateManager;
import com.widedot.calendar.config.ThemeManager;
import com.widedot.calendar.config.DayMappingManager;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.screens.SlidingPuzzleGameScreen;
import com.widedot.calendar.screens.QuestionAnswerGameScreen;

/**
 * Implémentation dynamique de la fabrique d'écrans de jeu qui utilise la réflexion
 * pour instancier les écrans de jeu à partir des configurations JSON
 */
public class DynamicGameScreenFactory {
    private static DynamicGameScreenFactory instance;

    /**
     * Constructeur privé pour le pattern Singleton
     */
    private DynamicGameScreenFactory() {
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
        String gameReference = DayMappingManager.getInstance().getGameReferenceForDay(dayId);
        if (gameReference == null) {
            throw new IllegalArgumentException("Aucune configuration trouvée pour le jour " + dayId);
        }

        GameManager.GameConfig gameConfig = GameManager.getInstance().getGameByReference(gameReference);
        if (gameConfig == null) {
            throw new IllegalArgumentException("Aucune configuration trouvée pour la référence " + gameReference);
        }

        GameTemplateManager.GameTemplate template = GameTemplateManager.getInstance().getTemplateByType(gameTemplate);
        if (template == null) {
            throw new IllegalArgumentException("Aucun template trouvé pour le type de jeu " + gameTemplate);
        }

        return createGameScreen(gameConfig, template, dayId, game);
    }

    private GameScreen createGameScreen(GameManager.GameConfig gameConfig,
                                        GameTemplateManager.GameTemplate template,
                                        int dayId, Game game) {
        // Fusionner les paramètres dans l'ordre de priorité:
        // 1. Paramètres par défaut du template
        // 2. Paramètres des presets appliqués
        // 3. Paramètres spécifiques de la configuration
        ObjectMap<String, Object> finalParameters = new ObjectMap<>();
        ObjectMap<String, Object> defaultParams = template.getDefaultParameters();
        if (defaultParams != null) {
            for (ObjectMap.Entry<String, Object> entry : defaultParams.entries()) {
                finalParameters.put(entry.key, entry.value);
            }
        }

        // Appliquer les presets
        Array<String> presets = gameConfig.getPresets();
        if (presets != null) {
            for (String preset : presets) {
                ObjectMap<String, Object> presetParams = template.getPresetParameters(preset);
                if (presetParams != null) {
                    for (ObjectMap.Entry<String, Object> entry : presetParams.entries()) {
                        finalParameters.put(entry.key, entry.value);
                    }
                } else {
                    Gdx.app.error("DynamicGameScreenFactory", "ATTENTION: Preset '" + preset + "' non trouvé pour le type de jeu " + template.getGameTemplate());
                }
            }
        }

        // Appliquer les paramètres spécifiques
        ObjectMap<String, Object> specificParams = gameConfig.getParameters();
        if (specificParams != null) {
            for (ObjectMap.Entry<String, Object> entry : specificParams.entries()) {
                finalParameters.put(entry.key, entry.value);
            }
        }

        // Récupérer le thème associé
        String themeName = gameConfig.getTheme();
        Theme theme = ThemeManager.getInstance().getThemeByName(themeName);
        if (theme == null) {
            throw new IllegalArgumentException("Thème non trouvé: " + themeName);
        }

        // Créer l'écran de jeu via le registre
        String gameName = template.getName();
        GameScreenLoader loader = GameScreenRegistry.getLoader(gameName);
        if (loader == null) {
            throw new RuntimeException("Aucun loader trouvé pour le jeu: " + gameName);
        }
        return loader.create(dayId, game, theme, finalParameters);
    }
}

/**
 * Interface fonctionnelle pour loader d'écran de jeu
 */
interface GameScreenLoader {
    GameScreen create(int dayId, Game game, Theme theme, ObjectMap<String, Object> parameters);
}

/**
 * Registre statique des loaders d'écrans de jeu
 */
class GameScreenRegistry {
    private static final ObjectMap<String, GameScreenLoader> registry = new ObjectMap<>();
    static {
        // Associer le nom du jeu à son loader
        registry.put("slidingPuzzle", (dayId, game, theme, parameters) -> new SlidingPuzzleGameScreen(dayId, game, theme, parameters));
        registry.put("questionAnswer", (dayId, game, theme, parameters) -> new QuestionAnswerGameScreen(dayId, game, theme, parameters));
        // Ajouter ici d'autres jeux si besoin
    }
    public static GameScreenLoader getLoader(String name) {
        return registry.get(name);
    }
}
