package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array;
import com.widedot.calendar.data.Theme;
import com.widedot.calendar.config.GameManager;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Gestionnaire des thèmes (tableaux)
 * Charge les données depuis le fichier themes.json et intègre les fonctionnalités de l'ancien PaintingManager
 */
public class ThemeManager {
    private static final String THEMES_FILE = "themes.json";
    private static ThemeManager instance;
    
    private final Map<String, Theme> themesByName;
    private final Array<Theme> allThemes;
    private final Map<Integer, String> dayToThemeMap; // Correspondance jour -> nom de thème
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private ThemeManager() {
        themesByName = new HashMap<>();
        allThemes = new Array<>();
        dayToThemeMap = new HashMap<>();
        loadThemes();
        loadDayToThemeMapping();
    }
    
    /**
     * Récupère l'instance unique du gestionnaire de thèmes
     * @return L'instance du gestionnaire
     */
    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }
    
    /**
     * Charge les thèmes depuis le fichier JSON
     */
    private void loadThemes() {
        try {
            FileHandle file = Gdx.files.internal(THEMES_FILE);
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);
            
            // Charger les thèmes
            JsonValue themesArray = root.get("themes");
            for (JsonValue themeValue = themesArray.child; themeValue != null; themeValue = themeValue.next) {
                String name = themeValue.getString("name");
                String title = themeValue.getString("title");
                String artist = themeValue.getString("artist");
                int year = themeValue.getInt("year");
                String description = themeValue.getString("description");
                
                // Récupérer les chemins d'images
                JsonValue images = themeValue.get("images");
                String fullImagePath = images.getString("full");
                String squareImagePath = images.getString("square");
                
                // Créer un thème
                Theme theme = new Theme(name, title, artist, year, description, fullImagePath, squareImagePath);
                themesByName.put(name, theme);
                allThemes.add(theme);
            }
            
            System.out.println("Chargement de " + allThemes.size + " thèmes réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des thèmes: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les associations jour -> thème depuis DayMappingManager
     */
    private void loadDayToThemeMapping() {
        try {
            DayMappingManager dayMappingManager = DayMappingManager.getInstance();
            GameManager gameManager = GameManager.getInstance();
            
            for (int i = 1; i <= 24; i++) {
                if (dayMappingManager.hasGameForDay(i)) {
                    String gameRef = dayMappingManager.getGameReferenceForDay(i);
                    if (gameRef != null) {
                        // Obtenir la configuration du jeu et extraire directement l'attribut theme
                        GameManager.GameConfig gameConfig = gameManager.getGameByReference(gameRef);
                        if (gameConfig != null) {
                            String themeName = gameConfig.getTheme();
                            if (themeName != null) {
                                dayToThemeMap.put(i, themeName);
                            }
                        }
                    }
                }
            }
            
            System.out.println("Chargement de " + dayToThemeMap.size() + " correspondances jour -> thème réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des correspondances jour -> thème: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère un thème par son nom
     * @param name Le nom du thème
     * @return Le thème correspondant ou null si non trouvé
     */
    public Theme getThemeByName(String name) {
        return themesByName.get(name);
    }
    
    /**
     * Récupère le thème associé à un jour
     * @param dayId L'identifiant du jour
     * @return Le thème associé au jour ou null si non trouvé
     */
    public Theme getThemeByDay(int dayId) {
        String themeName = dayToThemeMap.get(dayId);
        System.out.println("Recherche de thème pour le jour " + dayId + ": " + (themeName != null ? themeName : "non trouvé"));
        
        if (themeName == null) {
            System.out.println("Aucun thème associé au jour " + dayId + ". Jour-thèmes disponibles: " + dayToThemeMap.keySet());
            return null;
        }
        
        Theme theme = themesByName.get(themeName);
        if (theme == null) {
            System.out.println("Thème '" + themeName + "' référencé mais non trouvé dans les thèmes disponibles: " + themesByName.keySet());
        }
        
        return theme;
    }
    
    /**
     * Récupère le type de jeu associé à un jour
     * @param dayId L'identifiant du jour
     * @return Le type de jeu ou null si non trouvé
     */
    public String getGameTemplateForDay(int dayId) {
        // Obtenir la référence de jeu depuis DayMappingManager
        DayMappingManager dayMappingManager = DayMappingManager.getInstance();
        String gameRef = dayMappingManager.getGameReferenceForDay(dayId);
        
        if (gameRef != null) {
            // Obtenir la configuration du jeu depuis GameManager
            GameManager gameManager = GameManager.getInstance();
            GameManager.GameConfig gameConfig = gameManager.getGameByReference(gameRef);
            
            if (gameConfig != null) {
                // Utiliser directement l'attribut gameTemplate de la config
                return gameConfig.getGameTemplate();
            }
        }
        
        // Par défaut, retourner SPZ si aucun type ne peut être extrait
        return "SPZ";
    }
    
    /**
     * Récupère tous les thèmes
     * @return Un tableau contenant tous les thèmes
     */
    public Array<Theme> getAllThemes() {
        return allThemes;
    }
    
    /**
     * Récupère tous les thèmes d'un type de jeu donné
     * @param gameTemplate Le type de jeu recherché
     * @return Une liste des thèmes du type spécifié
     */
    public List<Theme> getThemesByGameTemplate(String gameTemplate) {
        // Comme tous les thèmes ont le même type de jeu SPZ par défaut
        // On retourne simplement tous les thèmes si le type est SPZ
        if ("SPZ".equals(gameTemplate)) {
            List<Theme> result = new ArrayList<>();
            for (Theme theme : allThemes) {
                result.add(theme);
            }
            return result;
        }
        // Sinon, on retourne une liste vide
        return new ArrayList<>();
    }
    
    /**
     * Vérifie si un thème existe
     * @param name Le nom du thème
     * @return true si le thème existe, false sinon
     */
    public boolean hasTheme(String name) {
        return themesByName.containsKey(name);
    }
    
    /**
     * Vérifie si un jour a un thème associé
     * @param dayId L'identifiant du jour
     * @return true si le jour a un thème associé, false sinon
     */
    public boolean hasThemeForDay(int dayId) {
        return dayToThemeMap.containsKey(dayId);
    }
    
    /**
     * Récupère le nombre total de thèmes
     * @return Le nombre de thèmes
     */
    public int getThemeCount() {
        return allThemes.size;
    }
} 