package com.widedot.calendar.data;

/**
 * Classe représentant un chef-d'œuvre
 */
public class Masterpiece implements Paintable {
    private final String name;
    private final String title;
    private final String artist;
    private final int year;
    private final String description;
    private final String imagePath;
    private final String gameType;
    
    /**
     * Constructeur
     * 
     * @param name Nom unique du chef-d'œuvre
     * @param title Titre du chef-d'œuvre
     * @param artist Artiste qui a créé le chef-d'œuvre
     * @param year Année de création
     * @param description Description du chef-d'œuvre
     * @param imagePath Chemin vers l'image du chef-d'œuvre
     * @param gameType Type de jeu associé au chef-d'œuvre
     */
    public Masterpiece(String name, String title, String artist, int year, String description, String imagePath, String gameType) {
        this.name = name;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.description = description;
        this.imagePath = imagePath;
        this.gameType = gameType;
    }
    
    /**
     * Récupère le nom du chef-d'œuvre
     * @return Le nom du chef-d'œuvre
     */
    @Override
    public String getName() {
        return name;
    }
    
    /**
     * Récupère le titre du chef-d'œuvre
     * @return Le titre du chef-d'œuvre
     */
    @Override
    public String getTitle() {
        return title;
    }
    
    /**
     * Récupère l'artiste qui a créé le chef-d'œuvre
     * @return L'artiste
     */
    @Override
    public String getArtist() {
        return artist;
    }
    
    /**
     * Récupère l'année de création du chef-d'œuvre
     * @return L'année de création
     */
    @Override
    public int getYear() {
        return year;
    }
    
    /**
     * Récupère la description du chef-d'œuvre
     * @return La description
     */
    @Override
    public String getDescription() {
        return description;
    }
    
    /**
     * Récupère le chemin vers l'image du chef-d'œuvre
     * @return Le chemin de l'image
     */
    @Override
    public String getImagePath() {
        return imagePath;
    }
    
    /**
     * Récupère le type de jeu associé au chef-d'œuvre
     * @return Le type de jeu
     */
    public String getGameType() {
        return gameType;
    }
    
    @Override
    public String toString() {
        return "Masterpiece{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", year=" + year +
                '}';
    }
} 