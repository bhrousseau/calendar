package com.widedot.calendar.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Gestionnaire pour la font Distance Field Carlito
 */
public class CarlitoFontManager {
    private static BitmapFont font;
    private static ShaderProgram fontShader;
    private static boolean initialized = false;
    
    /**
     * Initialise la font Carlito avec le shader Distance Field
     */
    public static void initialize() {
        if (initialized) return;
        
        try {
            // Charger la texture avec filtrage linéaire et mipmaps
            Texture texture = new Texture(Gdx.files.internal("skin/carlito.png"), true);
            texture.setFilter(Texture.TextureFilter.MipMapLinearNearest, Texture.TextureFilter.Linear);
            
            // Créer la font avec la texture configurée
            font = new BitmapFont(Gdx.files.internal("skin/carlito.fnt"), new TextureRegion(texture), false);
            
            // Charger le shader Distance Field
            fontShader = new ShaderProgram(Gdx.files.internal("shaders/carlito.vert"), Gdx.files.internal("shaders/carlito.frag"));
            if (!fontShader.isCompiled()) {
                Gdx.app.error("CarlitoFontManager", "Shader compilation failed:\n" + fontShader.getLog());
                throw new RuntimeException("Failed to compile font shader");
            }
            
            initialized = true;
            Gdx.app.log("CarlitoFontManager", "Font Carlito Distance Field initialized successfully");
            
        } catch (Exception e) {
            Gdx.app.error("CarlitoFontManager", "Failed to initialize Carlito font: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Carlito font", e);
        }
    }
    
    /**
     * Dessine du texte avec la font Distance Field
     */
    public static void drawText(SpriteBatch batch, String text, float x, float y) {
        if (!initialized) {
            initialize();
        }
        
        // Sauvegarder le shader actuel
        ShaderProgram originalShader = batch.getShader();
        
        // Utiliser le shader Distance Field
        batch.setShader(fontShader);
        font.draw(batch, text, x, y);
        
        // Restaurer le shader original
        batch.setShader(originalShader);
    }
    
    /**
     * Dessine un GlyphLayout avec la font Distance Field
     */
    public static void drawText(SpriteBatch batch, com.badlogic.gdx.graphics.g2d.GlyphLayout layout, float x, float y) {
        if (!initialized) {
            initialize();
        }
        
        ShaderProgram originalShader = batch.getShader();
        batch.setShader(fontShader);
        font.draw(batch, layout, x, y);
        batch.setShader(originalShader);
    }
    
    /**
     * Dessine du texte avec une échelle spécifique
     */
    public static void drawText(SpriteBatch batch, String text, float x, float y, float scale) {
        if (!initialized) {
            initialize();
        }
        
        float originalScale = font.getData().scaleX;
        font.getData().setScale(scale);
        drawText(batch, text, x, y);
        font.getData().setScale(originalScale);
    }
    
    /**
     * Obtient la font pour un usage direct (avec gestion manuelle du shader)
     */
    public static BitmapFont getFont() {
        if (!initialized) {
            initialize();
        }
        return font;
    }
    
    /**
     * Obtient le shader Distance Field
     */
    public static ShaderProgram getShader() {
        if (!initialized) {
            initialize();
        }
        return fontShader;
    }
    
    /**
     * Libère les ressources
     */
    public static void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (fontShader != null) {
            fontShader.dispose();
            fontShader = null;
        }
        initialized = false;
    }
}
