package com.widedot.calendar.data;

/**
 * Classe représentant un thème (tableau)
 */
public class Theme {
    private final String name;
    private final String title;
    private final String artist;
    private final int year;
    private final String description;
    private final String fullImagePath;
    private final String squareImagePath;
    
    /**
     * Constructeur
     * @param name Le nom du thème
     * @param title Le titre du thème
     * @param artist L'artiste
     * @param year L'année de création
     * @param description La description
     * @param fullImagePath Le chemin vers l'image complète
     * @param squareImagePath Le chemin vers l'image carrée
     */
    public Theme(String name, String title, String artist, int year, String description, 
                String fullImagePath, String squareImagePath) {
        this.name = name;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.description = description;
        this.fullImagePath = fullImagePath;
        this.squareImagePath = squareImagePath;
    }
    
    /**
     * Récupère le nom du thème
     * @return Le nom
     */
    public String getName() {
        return name;
    }
    
    /**
     * Récupère le titre du thème
     * @return Le titre
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Récupère le nom de l'artiste
     * @return Le nom de l'artiste
     */
    public String getArtist() {
        return artist;
    }
    
    /**
     * Récupère l'année de création
     * @return L'année
     */
    public int getYear() {
        return year;
    }
    
    /**
     * Récupère la description
     * @return La description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Récupère le chemin de l'image complète
     * @return Le chemin de l'image
     */
    public String getFullImagePath() {
        return fullImagePath;
    }
    
    /**
     * Récupère le chemin de l'image carrée
     * @return Le chemin de l'image
     */
    public String getSquareImagePath() {
        return squareImagePath;
    }
    
    // Implémentation de l'interface Paintable (pour compatibilité)
    public String getImagePath() {
        return getFullImagePath();
    }
    
    public String toString() {
        return title + " (" + artist + ", " + year + ")";
    }
} 