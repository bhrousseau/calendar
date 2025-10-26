#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

// Entrées (mêmes unités que le filtre CPU)
uniform float u_hue_deg;        // 0..360
uniform float u_saturation_01;  // 0..1
uniform float u_lightness_01;   // -1..1

// RGB [0,1] -> HSL (h[0..1], s[0..1], l[0..1])
vec3 rgb2hsl(vec3 c){
    float maxc = max(c.r, max(c.g, c.b));
    float minc = min(c.r, min(c.g, c.b));
    float l = (maxc + minc) * 0.5;
    float d = maxc - minc;
    float h = 0.0;
    float s = 0.0;
    if(d > 1e-6){
        s = d / (1.0 - abs(2.0*l - 1.0));
        if(maxc == c.r)      h = ( (c.g - c.b) / d + (c.g < c.b ? 6.0 : 0.0) ) / 6.0;
        else if(maxc == c.g) h = ( (c.b - c.r) / d + 2.0 ) / 6.0;
        else                 h = ( (c.r - c.g) / d + 4.0 ) / 6.0;
    }
    return vec3(h, s, l);
}

// helper pour hsl2rgb
float hue2rgb(float p, float q, float t){
    if(t < 0.0) t += 1.0;
    if(t > 1.0) t -= 1.0;
    if(t < 1.0/6.0) return p + (q - p) * 6.0 * t;
    if(t < 1.0/2.0) return q;
    if(t < 2.0/3.0) return p + (q - p) * (2.0/3.0 - t) * 6.0;
    return p;
}

// HSL -> RGB
vec3 hsl2rgb(vec3 hsl){
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;
    if(s < 1e-6) return vec3(l); // gris
    float q = l < 0.5 ? l * (1.0 + s) : (l + s - l*s);
    float p = 2.0*l - q;
    float r = hue2rgb(p, q, h + 1.0/3.0);
    float g = hue2rgb(p, q, h);
    float b = hue2rgb(p, q, h - 1.0/3.0);
    return vec3(r,g,b);
}

void main(){
    vec4 src = texture2D(u_texture, v_texCoords);
    vec3 hsl = rgb2hsl(src.rgb);

    // Photoshop-like "Colorize" : on impose la teinte et on mixe la saturation
    float hue = mod(u_hue_deg / 360.0, 1.0);
    // Ajuste la luminosité (courbe douce) en -1..+1
    float L = hsl.z;
    float lAdj = u_lightness_01;
    float L2 = lAdj >= 0.0 ? L + (1.0 - L) * lAdj : L * (1.0 + lAdj);
    L2 = clamp(L2, 0.0, 1.0);

    // Couleur "pure" à S=1, L=L2
    vec3 pure = hsl2rgb(vec3(hue, 1.0, L2));

    // Interpolation selon saturation demandée (0..1)
    float sFac = clamp(u_saturation_01, 0.0, 1.0);
    vec3 gray = vec3(L2);
    vec3 outRgb = mix(gray, pure, sFac);

    // Appliquer la couleur du batch (notamment l'alpha pour le fade)
    gl_FragColor = vec4(outRgb, src.a) * v_color;
}

