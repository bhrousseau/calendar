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
    private final CropInfo squareCrop;
    
    /**
     * Classe interne pour stocker les informations de recadrage
     */
    public static class CropInfo {
        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final float matchPercentage;
        
        public CropInfo(int x, int y, int width, int height, float matchPercentage) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.matchPercentage = matchPercentage;
        }
        
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public float getMatchPercentage() { return matchPercentage; }
    }
    
    /**
     * Constructeur
     * @param name Le nom du thème
     * @param title Le titre du thème
     * @param artist L'artiste
     * @param year L'année de création
     * @param description La description
     * @param fullImagePath Le chemin vers l'image complète
     * @param squareCrop Les informations de recadrage pour l'image carrée
     */
    public Theme(String name, String title, String artist, int year, String description, 
                String fullImagePath, CropInfo squareCrop) {
        this.name = name;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.description = description;
        this.fullImagePath = fullImagePath;
        this.squareCrop = squareCrop;
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
     * Récupère les informations de recadrage pour l'image carrée
     * @return Les informations de recadrage
     */
    public CropInfo getSquareCrop() {
        return squareCrop;
    }
    
    // Implémentation de l'interface Paintable (pour compatibilité)
    public String getImagePath() {
        return getFullImagePath();
    }
    
    public String toString() {
        return title + " (" + artist + ", " + year + ")";
    }
} 