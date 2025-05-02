package com.widedot.calendar.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire des associations jour -> référence de jeu
 * Charge les données depuis le fichier dayMapping.json
 */
public class DayMappingManager {
    private static final String DAY_MAPPING_FILE = "dayMapping.json";
    private static DayMappingManager instance;
    
    private final Map<Integer, String> dayMappings; // Correspondance jour -> référence de jeu
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private DayMappingManager() {
        dayMappings = new HashMap<>();
        loadDayMappings();
    }
    
    /**
     * Récupère l'instance unique du gestionnaire de mappings
     * @return L'instance du gestionnaire
     */
    public static DayMappingManager getInstance() {
        if (instance == null) {
            instance = new DayMappingManager();
        }
        return instance;
    }
    
    /**
     * Charge les mappings jour -> référence de jeu depuis le fichier JSON
     */
    private void loadDayMappings() {
        try {
            FileHandle file = Gdx.files.internal(DAY_MAPPING_FILE);
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);
            
            // Charger les mappings
            JsonValue dayMapping = root.get("dayMapping");
            if (dayMapping != null) {
                for (JsonValue day = dayMapping.child; day != null; day = day.next) {
                    int dayId = Integer.parseInt(day.name); // Le nom est le numéro du jour
                    String gameReference = day.asString(); // La valeur est la référence du jeu
                    
                    dayMappings.put(dayId, gameReference);
                }
            }
            
            System.out.println("Chargement de " + dayMappings.size() + " correspondances jour -> jeu réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des mappings jour -> jeu: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère la référence de jeu pour un jour donné
     * @param dayId L'identifiant du jour
     * @return La référence de jeu ou null si non trouvée
     */
    public String getGameReferenceForDay(int dayId) {
        return dayMappings.get(dayId);
    }
    
    /**
     * Vérifie si un jour a une référence de jeu
     * @param dayId L'identifiant du jour
     * @return true si le jour a une référence de jeu, false sinon
     */
    public boolean hasGameForDay(int dayId) {
        return dayMappings.containsKey(dayId);
    }
    
    /**
     * Récupère le nombre total de mappings
     * @return Le nombre de mappings
     */
    public int getMappingCount() {
        return dayMappings.size();
    }
} 