package com.widedot.calendar.painting;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.widedot.calendar.data.Paintable;

/**
 * Classe représentant un tableau de maître dans le package painting
 * Cette classe est utilisée par PaintingManager pour gérer les tableaux
 */
public class Painting implements Paintable {
    private final String name;
    private final String title;
    private final String artist;
    private final int year;
    private final String description;
    private final String imagePath;
    private String gameType;
    private TextureRegion texture;
    
    /**
     * Constructeur
     * 
     * @param name Nom unique du tableau
     * @param title Titre du tableau
     * @param artist Artiste qui a créé le tableau
     * @param year Année de création du tableau
     * @param description Description du tableau
     * @param imagePath Chemin vers l'image du tableau
     * @param gameType Type de jeu associé au tableau
     */
    public Painting(String name, String title, String artist, int year, String description, String imagePath, String gameType) {
        this.name = name;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.description = description;
        this.imagePath = imagePath;
        this.gameType = gameType;
    }
    
    /**
     * Charge la texture du tableau
     */
    public void loadTexture() {
        if (texture == null) {
            Texture tex = new Texture(imagePath);
            texture = new TextureRegion(tex);
        }
    }
    
    /**
     * Libère la texture du tableau
     */
    public void dispose() {
        if (texture != null) {
            texture.getTexture().dispose();
            texture = null;
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getArtist() {
        return artist;
    }
    
    @Override
    public int getYear() {
        return year;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public String getImagePath() {
        return imagePath;
    }
    
    public TextureRegion getTexture() {
        return texture;
    }
    
    /**
     * Modifie le type de jeu associé au tableau
     * @param gameType Le nouveau type de jeu
     */
    public void setGameType(String gameType) {
        this.gameType = gameType;
    }
    
    @Override
    public String toString() {
        return "Painting{" +
                "name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", year=" + year +
                ", gameType='" + gameType + '\'' +
                '}';
    }
} 