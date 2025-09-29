package com.widedot.calendar.display;

/**
 * Configuration centralisée pour l'affichage
 */
public final class DisplayConfig {
    // Largeur logique fixe du monde
    public static final float WORLD_WIDTH = 2048f;
    
    // Ratios d'aspect supportés
    public static final float RATIO_4_3 = 4f/3f;      // 1.333...
    public static final float RATIO_16_10 = 16f/10f;  // 1.6
    public static final float RATIO_16_9 = 16f/9f;    // 1.777...
    public static final float RATIO_21_9 = 21f/9f;    // 2.333...
    
    // Hauteurs correspondantes pour la largeur fixe
    public static final float HEIGHT_4_3 = WORLD_WIDTH / RATIO_4_3;     // 1536
    public static final float HEIGHT_16_10 = WORLD_WIDTH / RATIO_16_10; // 1280
    public static final float HEIGHT_16_9 = WORLD_WIDTH / RATIO_16_9;   // 1152
    public static final float HEIGHT_21_9 = WORLD_WIDTH / RATIO_21_9;   // 878
    
    // Limites de ratio pour différents modes
    public static final float MIN_SUPPORTED_RATIO = RATIO_4_3;
    public static final float MAX_SUPPORTED_RATIO = RATIO_21_9;
    
    // Ratio de référence (16:10)
    public static final float REFERENCE_RATIO = RATIO_16_10;
    public static final float REFERENCE_HEIGHT = HEIGHT_16_10;
    
    // Tailles de fenêtre par défaut
    public static final int DEFAULT_WINDOWED_WIDTH = 1280;
    public static final int DEFAULT_WINDOWED_HEIGHT = 800; // Ratio 16:10
    
    private DisplayConfig() {
        // Classe utilitaire - pas d'instanciation
    }
}
