package com.widedot.calendar.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Shader GPU pour appliquer un filtre HSL (Hue, Saturation, Lightness) sur les textures.
 * Compatible GWT/HTML contrairement au filtre CPU HSLColorFilter.
 */
public final class HSLShader {
    private static ShaderProgram shader;

    /**
     * Charge le shader HSL si pas déjà chargé
     */
    public static void ensureLoaded() {
        if (shader != null) {
            return;
        }
        
        ShaderProgram.pedantic = false; // Plus indulgent pour WebGL
        
        try {
            String vert = Gdx.files.internal("shaders/passthrough.vert").readString();
            String frag = Gdx.files.internal("shaders/hsl.frag").readString();
            
            shader = new ShaderProgram(vert, frag);
            
            if (!shader.isCompiled()) {
                Gdx.app.error("HSLShader", "Erreur de compilation du shader HSL: " + shader.getLog());
                throw new IllegalStateException("HSL shader compile error: " + shader.getLog());
            }
            
            Gdx.app.log("HSLShader", "Shader HSL chargé avec succès");
        } catch (Throwable e) {
            Gdx.app.error("HSLShader", "Exception lors du chargement du shader: " + e.getClass().getName() + ": " + e.getMessage());
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
            }
            Gdx.app.error("HSLShader", stackTrace.toString());
            throw new RuntimeException("Failed to load HSL shader", e);
        }
    }

    /**
     * Applique le shader HSL pour dessiner avec un SpriteBatch.
     * Le batch doit déjà avoir appelé begin().
     * 
     * @param batch SpriteBatch actif (begin() déjà appelé)
     * @param hueDeg Teinte en degrés (0..360)
     * @param saturation Saturation en pourcentage (0..100)
     * @param lightness Luminosité en pourcentage (-100..+100)
     */
    public static void begin(SpriteBatch batch, float hueDeg, float saturation, float lightness) {
        ensureLoaded();
        batch.setShader(shader);
        shader.setUniformf("u_hue_deg", hueDeg);
        shader.setUniformf("u_saturation_01", saturation / 100f);
        shader.setUniformf("u_lightness_01", lightness / 100f);
    }

    /**
     * Restaure le shader par défaut du batch.
     * @param batch SpriteBatch actif
     */
    public static void end(SpriteBatch batch) {
        batch.setShader(null);
    }
    
    /**
     * Libère les ressources du shader
     */
    public static void dispose() {
        if (shader != null) {
            shader.dispose();
            shader = null;
        }
    }
}

