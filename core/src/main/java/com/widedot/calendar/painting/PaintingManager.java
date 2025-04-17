package com.widedot.calendar.painting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.Array;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Gestionnaire des tableaux de maîtres
 * Charge les données depuis un fichier JSON et fournit des méthodes pour accéder aux tableaux
 */
public class PaintingManager {
    private static final String PAINTINGS_FILE = "paintings.json";
    private static PaintingManager instance;
    
    private final Map<String, Painting> paintingsByName;
    private final Array<Painting> allPaintings;
    private final Map<Integer, DayMapping> dayMappings; // Correspondance jour -> mapping (tableau + type de jeu)
    
    /**
     * Classe interne représentant le mapping d'un jour
     */
    private static class DayMapping {
        private final String paintingName;
        private final String gameType;
        
        public DayMapping(String paintingName, String gameType) {
            this.paintingName = paintingName;
            this.gameType = gameType;
        }
        
        public String getPaintingName() {
            return paintingName;
        }
        
        public String getGameType() {
            return gameType;
        }
    }
    
    /**
     * Constructeur privé pour le pattern Singleton
     */
    private PaintingManager() {
        paintingsByName = new HashMap<>();
        allPaintings = new Array<>();
        dayMappings = new HashMap<>();
        loadPaintings();
    }
    
    /**
     * Récupère l'instance unique du gestionnaire de tableaux
     * @return L'instance du gestionnaire
     */
    public static PaintingManager getInstance() {
        if (instance == null) {
            instance = new PaintingManager();
        }
        return instance;
    }
    
    /**
     * Charge les tableaux depuis le fichier JSON
     */
    private void loadPaintings() {
        try {
            FileHandle file = Gdx.files.internal(PAINTINGS_FILE);
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(file);
            
            // Charger les tableaux d'abord
            JsonValue paintingsArray = root.get("paintings");
            for (JsonValue paintingValue = paintingsArray.child; paintingValue != null; paintingValue = paintingValue.next) {
                String name = paintingValue.getString("name");
                String title = paintingValue.getString("title");
                String artist = paintingValue.getString("artist");
                int year = paintingValue.getInt("year");
                String description = paintingValue.getString("description");
                String imagePath = paintingValue.getString("imagePath");
                
                // Créer un tableau sans type de jeu
                Painting painting = new Painting(name, title, artist, year, description, imagePath, "default");
                paintingsByName.put(name, painting);
                allPaintings.add(painting);
            }
            
            // Charger la correspondance jour -> tableau + type de jeu
            JsonValue dayMapping = root.get("dayMapping");
            if (dayMapping != null) {
                JsonValue day = dayMapping.child;
                while (day != null) {
                    int dayId = Integer.parseInt(day.name);
                    String paintingName = day.getString("painting");
                    String gameType = day.getString("gameType");
                    
                    // Créer un mapping jour -> (tableau + type de jeu)
                    DayMapping mapping = new DayMapping(paintingName, gameType);
                    dayMappings.put(dayId, mapping);
                    
                    day = day.next;
                }
            }
            
            System.out.println("Chargement de " + allPaintings.size + " tableaux réussi");
            System.out.println("Chargement de " + dayMappings.size() + " correspondances jour -> tableau réussi");
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des tableaux: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère un tableau par son nom
     * @param name Le nom du tableau
     * @return Le tableau correspondant ou null si non trouvé
     */
    public Painting getPaintingByName(String name) {
        return paintingsByName.get(name);
    }
    
    /**
     * Récupère un tableau par l'identifiant du jour
     * @param dayId L'identifiant du jour
     * @return Le tableau correspondant ou null si non trouvé
     */
    public Painting getPaintingByDay(int dayId) {
        DayMapping mapping = dayMappings.get(dayId);
        if (mapping != null) {
            return getPaintingByName(mapping.getPaintingName());
        }
        return null;
    }
    
    /**
     * Récupère le type de jeu associé à un jour
     * @param dayId L'identifiant du jour
     * @return Le type de jeu ou null si non trouvé
     */
    public String getGameTypeForDay(int dayId) {
        DayMapping mapping = dayMappings.get(dayId);
        return mapping != null ? mapping.getGameType() : null;
    }
    
    /**
     * Récupère tous les tableaux
     * @return Un tableau contenant tous les tableaux
     */
    public Array<Painting> getAllPaintings() {
        return allPaintings;
    }
    
    /**
     * Récupère toutes les peintures d'un type de jeu donné
     * @param gameType Le type de jeu recherché
     * @return Une liste des peintures du type spécifié
     */
    public List<Painting> getPaintingsByGameType(String gameType) {
        List<Painting> result = new ArrayList<>();
        for (Map.Entry<Integer, DayMapping> entry : dayMappings.entrySet()) {
            DayMapping mapping = entry.getValue();
            if (mapping.getGameType().equals(gameType)) {
                result.add(paintingsByName.get(mapping.getPaintingName()));
            }
        }
        return result;
    }
    
    /**
     * Vérifie si un tableau existe
     * @param name Le nom du tableau
     * @return true si le tableau existe, false sinon
     */
    public boolean hasPainting(String name) {
        return paintingsByName.containsKey(name);
    }
    
    /**
     * Vérifie si un jour a un tableau associé
     * @param dayId L'identifiant du jour
     * @return true si le jour a un tableau associé, false sinon
     */
    public boolean hasPaintingForDay(int dayId) {
        return dayMappings.containsKey(dayId);
    }
    
    /**
     * Récupère le nombre total de tableaux
     * @return Le nombre de tableaux
     */
    public int getPaintingCount() {
        return allPaintings.size;
    }
} 