package com.widedot.calendar.shaders;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Shader de cristallisation pour LibGDX
 * Remplace le filtre JHLabs par un shader GPU plus performant
 */
public class CrystallizeShader {
    
    private static final String VERTEX_SHADER = 
        "attribute vec4 a_position;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = u_projTrans * a_position;\n" +
        "}\n";
    
    private static final String FRAGMENT_SHADER = 
        "#ifdef GL_ES\n" +
        "precision mediump float;\n" +
        "#endif\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform float u_crystalSize;\n" +
        "uniform float u_randomness;\n" +
        "uniform float u_edgeThickness;\n" +
        "uniform vec2 u_resolution;\n" +
        "uniform float u_stretch;\n" +
        "uniform vec4 u_edgeColor;\n" +
        "uniform bool u_fadeEdges;\n" +
        "\n" +
        "// Fonction de bruit pseudo-aléatoire (identique à JHLabs)\n" +
        "float random(vec2 st) {\n" +
        "    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);\n" +
        "}\n" +
        "\n" +
        "// Fonction smoothStep (identique à ImageMath.smoothStep)\n" +
        "float smoothStep(float edge0, float edge1, float x) {\n" +
        "    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);\n" +
        "    return t * t * (3.0 - 2.0 * t);\n" +
        "}\n" +
        "\n" +
        "// Fonction clamp (identique à ImageMath.clamp)\n" +
        "int clampInt(int x, int minVal, int maxVal) {\n" +
        "    return int(clamp(float(x), float(minVal), float(maxVal)));\n" +
        "}\n" +
        "\n" +
        "// Fonction mixColors (identique à ImageMath.mixColors)\n" +
        "vec4 mixColors(float t, vec4 c1, vec4 c2) {\n" +
        "    return mix(c1, c2, t);\n" +
        "}\n" +
        "\n" +
        "// Implémentation fidèle de l'algorithme JHLabs CrystallizeFilter\n" +
        "void main() {\n" +
        "    vec2 uv = v_texCoords;\n" +
        "    \n" +
        "    // Corriger l'inversion Y (LibGDX vs OpenGL)\n" +
        "    uv.y = 1.0 - uv.y;\n" +
        "    \n" +
        "    // Convertir les coordonnées de texture en coordonnées pixel\n" +
        "    vec2 pixelCoords = uv * u_resolution;\n" +
        "    \n" +
        "    // Centrer les coordonnées par rapport au centre de l'image\n" +
        "    vec2 center = u_resolution * 0.5;\n" +
        "    vec2 centeredCoords = pixelCoords - center;\n" +
        "    \n" +
        "    // Transformation identique à JHLabs (matrice identité pour l'instant)\n" +
        "    float nx = centeredCoords.x; // m00*x + m01*y (matrice identité)\n" +
        "    float ny = centeredCoords.y; // m10*x + m11*y (matrice identité)\n" +
        "    \n" +
        "    // Application de l'échelle (statique)\n" +
        "    nx /= u_crystalSize;\n" +
        "    ny /= u_crystalSize * u_stretch;\n" +
        "    \n" +
        "    // Décalage pour éviter les artefacts autour de 0,0 (identique à JHLabs)\n" +
        "    nx += 1000.0;\n" +
        "    ny += 1000.0;\n" +
        "    \n" +
        "    // Algorithme Voronoi organique fidèle à JHLabs\n" +
        "    // Générer des points aléatoires dans une zone plus large\n" +
        "    float minDist1 = 999999.0;\n" +
        "    float minDist2 = 999999.0;\n" +
        "    vec2 closestCell1 = vec2(0.0);\n" +
        "    vec2 closestCell2 = vec2(0.0);\n" +
        "    \n" +
        "    // Chercher dans une zone plus large (5x5) pour des cellules plus organiques\n" +
        "    for (int i = -2; i <= 2; i++) {\n" +
        "        for (int j = -2; j <= 2; j++) {\n" +
        "            vec2 gridPos = floor(vec2(nx, ny)) + vec2(float(i), float(j));\n" +
        "            \n" +
        "                        // Générer plusieurs points aléatoires par cellule de grille\n" +
            "            // pour créer des cellules Voronoi plus organiques\n" +
            "            for (int k = 0; k < 3; k++) {\n" +
            "                vec2 seed = gridPos + vec2(float(k), float(k * 2));\n" +
            "                \n" +
                "                // Offset aléatoire simple et stable\n" +
                "                vec2 offset = vec2(\n" +
                "                    random(seed) - 0.5,\n" +
                "                    random(seed + vec2(1.0, 0.0)) - 0.5\n" +
                "                ) * (0.8 + u_randomness * 0.4);\n" +
        "                \n" +
        "                // Position du centre de la cellule avec offset\n" +
        "                vec2 cellCenter = gridPos + 0.5 + offset;\n" +
        "                \n" +
        "                // Distance au centre de cette cellule\n" +
        "                float dist = distance(vec2(nx, ny), cellCenter);\n" +
        "                \n" +
        "                // Garder les deux plus proches distances\n" +
        "                if (dist < minDist1) {\n" +
        "                    minDist2 = minDist1;\n" +
        "                    closestCell2 = closestCell1;\n" +
        "                    minDist1 = dist;\n" +
        "                    closestCell1 = cellCenter;\n" +
        "                } else if (dist < minDist2) {\n" +
        "                    minDist2 = dist;\n" +
        "                    closestCell2 = cellCenter;\n" +
        "                }\n" +
        "            }\n" +
        "        }\n" +
        "    }\n" +
        "    \n" +
        "    // Convertir les coordonnées de cellule en coordonnées de texture\n" +
        "    // (inverse de la transformation appliquée plus haut)\n" +
        "    float srcx = (closestCell1.x - 1000.0) * u_crystalSize;\n" +
        "    float srcy = (closestCell1.y - 1000.0) * u_crystalSize * u_stretch;\n" +
        "    \n" +
        "    // Remettre les coordonnées centrées dans le système de coordonnées de l'image\n" +
        "    srcx += center.x;\n" +
        "    srcy += center.y;\n" +
        "    \n" +
        "    // Clamp aux dimensions de l'image (identique à ImageMath.clamp)\n" +
        "    srcx = clamp(srcx, 0.0, u_resolution.x - 1.0);\n" +
        "    srcy = clamp(srcy, 0.0, u_resolution.y - 1.0);\n" +
        "    \n" +
        "    // Convertir en coordonnées de texture (0,1)\n" +
        "    vec2 sampleUV = vec2(srcx, srcy) / u_resolution;\n" +
        "    \n" +
        "    // Récupérer la couleur du pixel au centre de la cellule\n" +
        "    vec4 color = texture2D(u_texture, sampleUV);\n" +
        "    \n" +
        "    // Calculer le facteur de bord (statique)\n" +
        "    float f = (minDist2 - minDist1) / u_edgeThickness;\n" +
        "    f = smoothStep(0.0, u_edgeThickness, f);\n" +
        "    \n" +
        "    // Application des bords (identique à JHLabs)\n" +
        "    if (u_fadeEdges) {\n" +
        "        // Fade edges: mélanger avec la couleur de la cellule secondaire\n" +
        "        float srcx2 = (closestCell2.x - 1000.0) * u_crystalSize;\n" +
        "        float srcy2 = (closestCell2.y - 1000.0) * u_crystalSize * u_stretch;\n" +
        "        \n" +
        "        // Remettre les coordonnées centrées dans le système de coordonnées de l'image\n" +
        "        srcx2 += center.x;\n" +
        "        srcy2 += center.y;\n" +
        "        \n" +
        "        srcx2 = clamp(srcx2, 0.0, u_resolution.x - 1.0);\n" +
        "        srcy2 = clamp(srcy2, 0.0, u_resolution.y - 1.0);\n" +
        "        vec2 sampleUV2 = vec2(srcx2, srcy2) / u_resolution;\n" +
        "        vec4 color2 = texture2D(u_texture, sampleUV2);\n" +
        "        \n" +
        "        // Mélanger les deux couleurs (identique à ImageMath.mixColors)\n" +
        "        vec4 mixedColor = mixColors(0.5, color2, color);\n" +
        "        gl_FragColor = mixColors(f, mixedColor, color);\n" +
        "    } else {\n" +
        "        // Edge color: mélanger avec la couleur de bord\n" +
        "        gl_FragColor = mixColors(f, u_edgeColor, color);\n" +
        "    }\n" +
        "}\n";
    
    private ShaderProgram shader;
    
    public CrystallizeShader() {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (!shader.isCompiled()) {
            Gdx.app.error("CrystallizeShader", "Shader compilation failed: " + shader.getLog());
            throw new RuntimeException("Erreur compilation shader cristallisation: " + shader.getLog());
        } else {
            Gdx.app.log("CrystallizeShader", "Shader compiled successfully");
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
