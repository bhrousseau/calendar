package com.widedot.calendar.data;

/**
 * Interface définissant les méthodes communes pour les objets représentant des peintures.
 * Cette interface permet de standardiser l'accès aux propriétés des peintures,
 * qu'elles soient représentées par Painting ou Masterpiece.
 */
public interface Paintable {
    /**
     * Récupère le nom de la peinture
     * @return Le nom de la peinture
     */
    String getName();
    
    /**
     * Récupère le titre de la peinture
     * @return Le titre de la peinture
     */
    String getTitle();
    
    /**
     * Récupère l'artiste qui a créé la peinture
     * @return L'artiste
     */
    String getArtist();
    
    /**
     * Récupère l'année de création de la peinture
     * @return L'année de création
     */
    int getYear();
    
    /**
     * Récupère la description de la peinture
     * @return La description
     */
    String getDescription();
    
    /**
     * Récupère le chemin vers l'image de la peinture
     * @return Le chemin de l'image
     */
    String getImagePath();
} 