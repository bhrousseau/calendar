#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_crystalSize;
uniform float u_randomness;
uniform float u_edgeThickness;
uniform vec2 u_resolution;
uniform float u_stretch;
uniform vec4 u_edgeColor;
uniform bool u_fadeEdges;

// Fonction de bruit pseudo-aléatoire (identique à JHLabs)
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

// Fonction smoothStep (identique à ImageMath.smoothStep)
float smoothStep(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

// Fonction clamp (identique à ImageMath.clamp)
int clampInt(int x, int minVal, int maxVal) {
    return int(clamp(float(x), float(minVal), float(maxVal)));
}

// Fonction mixColors (identique à ImageMath.mixColors)
vec4 mixColors(float t, vec4 c1, vec4 c2) {
    return mix(c1, c2, t);
}

// Implémentation fidèle de l'algorithme JHLabs CrystallizeFilter
void main() {
    vec2 uv = v_texCoords;
    
    // Corriger l'inversion Y (LibGDX vs OpenGL)
    uv.y = 1.0 - uv.y;
    
    // Convertir les coordonnées de texture en coordonnées pixel
    vec2 pixelCoords = uv * u_resolution;
    
    // Centrer les coordonnées par rapport au centre de l'image
    vec2 center = u_resolution * 0.5;
    vec2 centeredCoords = pixelCoords - center;
    
    // Transformation identique à JHLabs (matrice identité pour l'instant)
    float nx = centeredCoords.x; // m00*x + m01*y (matrice identité)
    float ny = centeredCoords.y; // m10*x + m11*y (matrice identité)
    
    // Application de l'échelle (statique)
    nx /= u_crystalSize;
    ny /= u_crystalSize * u_stretch;
    
    // Décalage pour éviter les artefacts autour de 0,0 (identique à JHLabs)
    nx += 1000.0;
    ny += 1000.0;
    
    // Algorithme Voronoi organique fidèle à JHLabs
    // Générer des points aléatoires dans une zone plus large
    float minDist1 = 999999.0;
    float minDist2 = 999999.0;
    vec2 closestCell1 = vec2(0.0);
    vec2 closestCell2 = vec2(0.0);
    
    // Chercher dans une zone plus large (5x5) pour des cellules plus organiques
    for (int i = -2; i <= 2; i++) {
        for (int j = -2; j <= 2; j++) {
            vec2 gridPos = floor(vec2(nx, ny)) + vec2(float(i), float(j));
            
            // Générer plusieurs points aléatoires par cellule de grille
            // pour créer des cellules Voronoi plus organiques
            for (int k = 0; k < 3; k++) {
                vec2 seed = gridPos + vec2(float(k), float(k * 2));
                
                // Offset aléatoire simple et stable
                vec2 offset = vec2(
                    random(seed) - 0.5,
                    random(seed + vec2(1.0, 0.0)) - 0.5
                ) * (0.8 + u_randomness * 0.4);
                
                // Position du centre de la cellule avec offset
                vec2 cellCenter = gridPos + 0.5 + offset;
                
                // Distance au centre de cette cellule
                float dist = distance(vec2(nx, ny), cellCenter);
                
                // Garder les deux plus proches distances
                if (dist < minDist1) {
                    minDist2 = minDist1;
                    closestCell2 = closestCell1;
                    minDist1 = dist;
                    closestCell1 = cellCenter;
                } else if (dist < minDist2) {
                    minDist2 = dist;
                    closestCell2 = cellCenter;
                }
            }
        }
    }
    
    // Convertir les coordonnées de cellule en coordonnées de texture
    // (inverse de la transformation appliquée plus haut)
    float srcx = (closestCell1.x - 1000.0) * u_crystalSize;
    float srcy = (closestCell1.y - 1000.0) * u_crystalSize * u_stretch;
    
    // Remettre les coordonnées centrées dans le système de coordonnées de l'image
    srcx += center.x;
    srcy += center.y;
    
    // Clamp aux dimensions de l'image (identique à ImageMath.clamp)
    srcx = clamp(srcx, 0.0, u_resolution.x - 1.0);
    srcy = clamp(srcy, 0.0, u_resolution.y - 1.0);
    
    // Convertir en coordonnées de texture (0,1)
    vec2 sampleUV = vec2(srcx, srcy) / u_resolution;
    
    // Récupérer la couleur du pixel au centre de la cellule
    vec4 color = texture2D(u_texture, sampleUV);
    
    // Calculer le facteur de bord (statique)
    float f = (minDist2 - minDist1) / u_edgeThickness;
    f = smoothStep(0.0, u_edgeThickness, f);
    
    // Application des bords (identique à JHLabs)
    if (u_fadeEdges) {
        // Fade edges: mélanger avec la couleur de la cellule secondaire
        float srcx2 = (closestCell2.x - 1000.0) * u_crystalSize;
        float srcy2 = (closestCell2.y - 1000.0) * u_crystalSize * u_stretch;
        
        // Remettre les coordonnées centrées dans le système de coordonnées de l'image
        srcx2 += center.x;
        srcy2 += center.y;
        
        srcx2 = clamp(srcx2, 0.0, u_resolution.x - 1.0);
        srcy2 = clamp(srcy2, 0.0, u_resolution.y - 1.0);
        vec2 sampleUV2 = vec2(srcx2, srcy2) / u_resolution;
        vec4 color2 = texture2D(u_texture, sampleUV2);
        
        // Mélanger les deux couleurs (identique à ImageMath.mixColors)
        vec4 mixedColor = mixColors(0.5, color2, color);
        gl_FragColor = mixColors(f, mixedColor, color);
    } else {
        // Edge color: mélanger avec la couleur de bord
        gl_FragColor = mixColors(f, u_edgeColor, color);
    }
}

