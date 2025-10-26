package com.widedot.calendar.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Shader de cristallisation pour LibGDX.
 * Compatible GWT/HTML - charge les shaders depuis des fichiers externes.
 * Remplace le filtre JHLabs par un shader GPU plus performant.
 */
public class CrystallizeShader {
    
    private ShaderProgram shader;
    
    public CrystallizeShader() {
        Gdx.app.log("CrystallizeShader", "Chargement du shader depuis les fichiers externes...");
        
        ShaderProgram.pedantic = false; // Plus indulgent pour WebGL
        
        try {
            String vert = Gdx.files.internal("shaders/crystallize.vert").readString();
            String frag = Gdx.files.internal("shaders/crystallize.frag").readString();
            
            shader = new ShaderProgram(vert, frag);
            
            if (!shader.isCompiled()) {
                Gdx.app.error("CrystallizeShader", "Erreur de compilation du shader: " + shader.getLog());
                throw new RuntimeException("Erreur compilation shader cristallisation: " + shader.getLog());
            }
            
            Gdx.app.log("CrystallizeShader", "Shader chargé et compilé avec succès");
        } catch (Throwable e) {
            Gdx.app.error("CrystallizeShader", "Exception lors du chargement: " + e.getClass().getName() + ": " + e.getMessage());
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append("  at ").append(element.toString()).append("\n");
            }
            Gdx.app.error("CrystallizeShader", stackTrace.toString());
            throw new RuntimeException("Failed to load Crystallize shader", e);
        }
    }
    
    public ShaderProgram getShader() {
        return shader;
    }
    
    public void setCrystalSize(float crystalSize) {
        shader.setUniformf("u_crystalSize", crystalSize);
    }
    
    public void setRandomness(float randomness) {
        shader.setUniformf("u_randomness", randomness);
    }
    
    public void setEdgeThickness(float edgeThickness) {
        shader.setUniformf("u_edgeThickness", edgeThickness);
    }
    
    public void setResolution(float width, float height) {
        shader.setUniformf("u_resolution", width, height);
    }
    
    public void setStretch(float stretch) {
        shader.setUniformf("u_stretch", stretch);
    }
    
    public void setEdgeColor(float r, float g, float b, float a) {
        shader.setUniformf("u_edgeColor", r, g, b, a);
    }
    
    public void setFadeEdges(boolean fadeEdges) {
        shader.setUniformi("u_fadeEdges", fadeEdges ? 1 : 0);
    }
    
    
    public void dispose() {
        if (shader != null) {
            shader.dispose();
        }
    }
}
