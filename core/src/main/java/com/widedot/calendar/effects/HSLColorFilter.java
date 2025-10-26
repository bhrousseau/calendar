package com.widedot.calendar.effects;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.Gdx;

/**
 * Filtre de colorisation HSL pour les images
 */
public class HSLColorFilter {
    
    /**
     * Applique un filtre de couleur HSL à un Pixmap
     * @param pixmap Le pixmap à modifier
     * @param hue Teinte (0-360)
     * @param saturation Saturation (0-100)
     * @param lightness Luminosité (-100 à +100)
     */
    public static void applyHSLFilter(Pixmap pixmap, float hue, float saturation, float lightness) {
        // DÉSACTIVER LE FILTRE HSL EN MODE HTML/GWT
        // Raison: La manipulation pixel par pixel est trop lente en HTML
        // et peut causer des crashs sur de grandes images (2048x1280 = 2.6M pixels)
        if (com.badlogic.gdx.Application.ApplicationType.WebGL.equals(Gdx.app.getType())) {
            Gdx.app.log("HSLColorFilter", "Filtre HSL désactivé en mode HTML (non compatible GWT)");
            return;
        }
        
        int width = pixmap.getWidth();
        int height = pixmap.getHeight();
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = pixmap.getPixel(x, y);
                
                // Extraire les composantes RGBA (ordre LibGDX standard: RGBA8888)
                int r = (pixel & 0xff000000) >>> 24;
                int g = (pixel & 0x00ff0000) >>> 16;
                int b = (pixel & 0x0000ff00) >>> 8;
                int a = (pixel & 0x000000ff);
                
                // Pour une image en niveaux de gris, utiliser la luminosité comme base
                float grayValue = (r + g + b) / 3.0f / 255.0f;
                
                // Appliquer la colorisation type Photoshop Colorize
                // Dans Photoshop, on part du gris et on applique la teinte avec l'intensité de saturation
                
                // Étape 1 : Ajuster la luminosité avec le paramètre lightness
                // Utiliser une transformation non-linéaire pour éviter le clipping brutal
                float adjustedGray;
                float lightnessNorm = lightness / 100.0f; // -1 à +1
                
                if (lightnessNorm >= 0) {
                    // Lightness positif : compresser vers le blanc en préservant les détails
                    // Formule: nouveau = ancien + (1 - ancien) * lightness
                    // Cela crée une courbe douce vers le blanc
                    adjustedGray = grayValue + (1 - grayValue) * lightnessNorm;
                } else {
                    // Lightness négatif : compresser vers le noir en préservant les détails
                    // Formule: nouveau = ancien * (1 + lightness)
                    // Cela crée une courbe douce vers le noir
                    adjustedGray = grayValue * (1 + lightnessNorm);
                }
                
                adjustedGray = Math.max(0, Math.min(1, adjustedGray));
                
                // Étape 2 : Générer la couleur avec saturation maximale
                // Obtenir la couleur colorisée avec saturation à 100% (couleur pure)
                int[] colorRgb = hslToRgb(hue, 100, adjustedGray);
                
                // Étape 3 : Blender entre le gris original et la couleur pure selon la saturation
                // Saturation 0 = garder le gris, Saturation 100 = couleur pure
                float saturationFactor = saturation / 100.0f;
                int grayInt = (int)(adjustedGray * 255);
                
                // Assurer que les valeurs sont dans la plage 0-255
                int finalR = Math.max(0, Math.min(255, (int)(grayInt + (colorRgb[0] - grayInt) * saturationFactor)));
                int finalG = Math.max(0, Math.min(255, (int)(grayInt + (colorRgb[1] - grayInt) * saturationFactor)));
                int finalB = Math.max(0, Math.min(255, (int)(grayInt + (colorRgb[2] - grayInt) * saturationFactor)));
                
                // Reconstituer le pixel avec la nouvelle couleur (ordre LibGDX standard: RGBA8888)
                int newPixel = (finalR & 0xff) << 24 | (finalG & 0xff) << 16 | (finalB & 0xff) << 8 | (a & 0xff);
                pixmap.drawPixel(x, y, newPixel);
            }
        }
    }
    
    /**
     * Convertit HSL en RGB (format: H=0-360, S=0-100, L=0-1)
     */
    private static int[] hslToRgb(float h, float s, float l) {
        // Normaliser h entre 0 et 360
        h = h % 360;
        if (h < 0) h += 360;
        
        // Convertir s de 0-100 à 0-1
        s = Math.max(0, Math.min(100, s)) / 100.0f;
        
        // Clamp l entre 0 et 1
        l = Math.max(0, Math.min(1, l));
        
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = l - c / 2;
        
        float r, g, b;
        if (h < 60) {
            r = c; g = x; b = 0;
        } else if (h < 120) {
            r = x; g = c; b = 0;
        } else if (h < 180) {
            r = 0; g = c; b = x;
        } else if (h < 240) {
            r = 0; g = x; b = c;
        } else if (h < 300) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        // Clamper les valeurs finales entre 0 et 255
        int finalR = Math.max(0, Math.min(255, Math.round((r + m) * 255)));
        int finalG = Math.max(0, Math.min(255, Math.round((g + m) * 255)));
        int finalB = Math.max(0, Math.min(255, Math.round((b + m) * 255)));
        
        return new int[]{finalR, finalG, finalB};
    }
}

