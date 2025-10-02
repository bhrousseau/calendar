package com.widedot.calendar.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.widedot.calendar.shaders.CrystallizeShader;

/**
 * Gestionnaire de debug pour le shader de cristallisation
 * Gère tous les paramètres de debug et l'interface utilisateur
 */
public class CrystallizeDebugManager {
    
    // Paramètres de debug
    private float debugCrystalSize = 150.0f;
    private float debugEdgeThickness = 0.4f;
    private float debugRandomness = 0.0f;
    private float debugStretch = 1.0f;
    private float debugEdgeColorR = 0.0f;
    private float debugEdgeColorG = 0.0f;
    private float debugEdgeColorB = 0.0f;
    private float debugEdgeColorA = 1.0f;
    private boolean debugFadeEdges = false;
    private boolean debugMode = false;
    
    // Système de debug avancé
    public enum DebugParameter {
        CRYSTAL_SIZE,
        EDGE_THICKNESS,
        RANDOMNESS,
        STRETCH,
        EDGE_COLOR_R,
        EDGE_COLOR_G,
        EDGE_COLOR_B,
        EDGE_COLOR_A,
        FADE_EDGES
    }
    
    private DebugParameter selectedDebugParameter = DebugParameter.CRYSTAL_SIZE;
    private boolean isKeyHeld = false;
    private float keyHoldTime = 0.0f;
    private static final float KEY_HOLD_DELAY = 0.5f; // Délai avant l'appui continu
    private static final float KEY_HOLD_INTERVAL = 0.05f; // Intervalle entre les modifications en mode hold
    
    // Interface
    private final BitmapFont font;
    private final GlyphLayout layout;
    
    // Callback pour appliquer les changements
    private DebugChangeCallback changeCallback;
    
    // Informations pour la sauvegarde
    private String currentGameReference;
    private String gamesJsonPath = "games.json";
    
    // Confirmation de sauvegarde
    private boolean showSaveConfirmation = false;
    private float saveConfirmationTimer = 0.0f;
    private static final float SAVE_CONFIRMATION_DURATION = 2.0f; // 2 secondes
    
    /**
     * Interface pour les callbacks de changement
     */
    public interface DebugChangeCallback {
        void onDebugParameterChanged();
    }
    
    public CrystallizeDebugManager(BitmapFont font) {
        this.font = font;
        this.layout = new GlyphLayout();
    }
    
    /**
     * Définit le callback pour les changements de paramètres
     */
    public void setChangeCallback(DebugChangeCallback callback) {
        this.changeCallback = callback;
    }
    
    /**
     * Définit la référence du jeu actuel pour la sauvegarde
     */
    public void setCurrentGameReference(String gameReference) {
        this.currentGameReference = gameReference;
    }
    
    /**
     * Initialise les paramètres de debug avec les valeurs du jeu
     */
    public void initializeFromGameParameters(ObjectMap<String, Object> parameters) {
        if (parameters == null) return;
        
        // Charger les paramètres de debug depuis la configuration du jeu
        if (parameters.containsKey("randomness")) {
            debugRandomness = ((Number) parameters.get("randomness")).floatValue();
        }
        if (parameters.containsKey("edgeThickness")) {
            debugEdgeThickness = ((Number) parameters.get("edgeThickness")).floatValue();
        }
        if (parameters.containsKey("stretch")) {
            debugStretch = ((Number) parameters.get("stretch")).floatValue();
        }
        if (parameters.containsKey("edgeColor")) {
            String edgeColorStr = (String) parameters.get("edgeColor");
            parseEdgeColorFromString(edgeColorStr);
        }
        if (parameters.containsKey("fadeEdges")) {
            debugFadeEdges = (Boolean) parameters.get("fadeEdges");
        }
        if (parameters.containsKey("initialCrystalSize")) {
            debugCrystalSize = ((Number) parameters.get("initialCrystalSize")).floatValue();
        }
    }
    
    /**
     * Parse une couleur au format "r,g,b,a" et met à jour les paramètres RGBA
     */
    private void parseEdgeColorFromString(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length == 4) {
                debugEdgeColorR = Float.parseFloat(parts[0]);
                debugEdgeColorG = Float.parseFloat(parts[1]);
                debugEdgeColorB = Float.parseFloat(parts[2]);
                debugEdgeColorA = Float.parseFloat(parts[3]);
            }
        } catch (NumberFormatException e) {
            debugEdgeColorR = 0.0f;
            debugEdgeColorG = 0.0f;
            debugEdgeColorB = 0.0f;
            debugEdgeColorA = 1.0f;
        }
    }
    
    /**
     * Gère les événements clavier
     */
            public boolean handleKeyDown(int keycode) {
                if (keycode == Input.Keys.D) {
                    debugMode = !debugMode;
                    return true;
                }
                
                if (keycode == Input.Keys.S && debugMode) {
                    saveCurrentSettings();
                    return true;
                }
        
        if (debugMode) {
                    if (keycode == Input.Keys.UP) {
                        // Changer de paramètre sélectionné (cycle vers le haut)
                        switch (selectedDebugParameter) {
                            case CRYSTAL_SIZE:
                                selectedDebugParameter = DebugParameter.FADE_EDGES;
                                break;
                            case EDGE_THICKNESS:
                                selectedDebugParameter = DebugParameter.CRYSTAL_SIZE;
                                break;
                            case RANDOMNESS:
                                selectedDebugParameter = DebugParameter.EDGE_THICKNESS;
                                break;
                            case STRETCH:
                                selectedDebugParameter = DebugParameter.RANDOMNESS;
                                break;
                            case EDGE_COLOR_R:
                                selectedDebugParameter = DebugParameter.STRETCH;
                                break;
                            case EDGE_COLOR_G:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_R;
                                break;
                            case EDGE_COLOR_B:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_G;
                                break;
                            case EDGE_COLOR_A:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_B;
                                break;
                            case FADE_EDGES:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_A;
                                break;
                        }
                        return true;
                    }
                    if (keycode == Input.Keys.DOWN) {
                        // Changer de paramètre sélectionné (cycle vers le bas)
                        switch (selectedDebugParameter) {
                            case CRYSTAL_SIZE:
                                selectedDebugParameter = DebugParameter.EDGE_THICKNESS;
                                break;
                            case EDGE_THICKNESS:
                                selectedDebugParameter = DebugParameter.RANDOMNESS;
                                break;
                            case RANDOMNESS:
                                selectedDebugParameter = DebugParameter.STRETCH;
                                break;
                            case STRETCH:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_R;
                                break;
                            case EDGE_COLOR_R:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_G;
                                break;
                            case EDGE_COLOR_G:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_B;
                                break;
                            case EDGE_COLOR_B:
                                selectedDebugParameter = DebugParameter.EDGE_COLOR_A;
                                break;
                            case EDGE_COLOR_A:
                                selectedDebugParameter = DebugParameter.FADE_EDGES;
                                break;
                            case FADE_EDGES:
                                selectedDebugParameter = DebugParameter.CRYSTAL_SIZE;
                                break;
                        }
                        return true;
                    }
            if (keycode == Input.Keys.LEFT) {
                // Diminuer la valeur du paramètre sélectionné
                modifySelectedParameter(-1);
                isKeyHeld = true;
                keyHoldTime = 0.0f;
                return true;
            }
            if (keycode == Input.Keys.RIGHT) {
                // Augmenter la valeur du paramètre sélectionné
                modifySelectedParameter(1);
                isKeyHeld = true;
                keyHoldTime = 0.0f;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Gère la relâche des touches
     */
    public boolean handleKeyUp(int keycode) {
        if (debugMode && (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT)) {
            isKeyHeld = false;
            keyHoldTime = 0.0f;
            return true;
        }
        return false;
    }
    
    /**
     * Met à jour le système de debug (pour l'appui prolongé)
     */
    public void update(float delta) {
        // Gérer l'appui prolongé en mode debug
        if (debugMode && isKeyHeld) {
            keyHoldTime += delta;
            if (keyHoldTime >= KEY_HOLD_DELAY) {
                // Démarrer l'appui continu
                if (keyHoldTime >= KEY_HOLD_DELAY + KEY_HOLD_INTERVAL) {
                    // Modifier le paramètre sélectionné
                    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                        modifySelectedParameter(-1);
                    } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                        modifySelectedParameter(1);
                    }
                    keyHoldTime = KEY_HOLD_DELAY; // Reset pour le prochain intervalle
                }
            }
        }
        
        // Gérer la confirmation de sauvegarde
        if (showSaveConfirmation) {
            saveConfirmationTimer += delta;
            if (saveConfirmationTimer >= SAVE_CONFIRMATION_DURATION) {
                showSaveConfirmation = false;
                saveConfirmationTimer = 0.0f;
            }
        }
    }
    
    /**
     * Modifie la valeur du paramètre sélectionné
     */
    private void modifySelectedParameter(int direction) {
        switch (selectedDebugParameter) {
            case CRYSTAL_SIZE:
                debugCrystalSize = Math.max(1, Math.min(500, debugCrystalSize + direction));
                break;
            case EDGE_THICKNESS:
                debugEdgeThickness = Math.max(0.0f, Math.min(2.0f, debugEdgeThickness + direction * 0.01f));
                break;
            case RANDOMNESS:
                // Palier très fin pour le randomness
                debugRandomness = Math.max(0.0f, Math.min(1.0f, debugRandomness + direction * 0.001f));
                break;
            case STRETCH:
                debugStretch = Math.max(0.1f, Math.min(5.0f, debugStretch + direction * 0.01f));
                break;
            case EDGE_COLOR_R:
                debugEdgeColorR = Math.max(0.0f, Math.min(1.0f, debugEdgeColorR + direction * 0.01f));
                break;
            case EDGE_COLOR_G:
                debugEdgeColorG = Math.max(0.0f, Math.min(1.0f, debugEdgeColorG + direction * 0.01f));
                break;
            case EDGE_COLOR_B:
                debugEdgeColorB = Math.max(0.0f, Math.min(1.0f, debugEdgeColorB + direction * 0.01f));
                break;
            case EDGE_COLOR_A:
                debugEdgeColorA = Math.max(0.0f, Math.min(1.0f, debugEdgeColorA + direction * 0.01f));
                break;
            case FADE_EDGES:
                // Toggle pour le booléen
                debugFadeEdges = !debugFadeEdges;
                break;
        }
        
        // Notifier le changement
        if (changeCallback != null) {
            changeCallback.onDebugParameterChanged();
        }
    }
    
    
    /**
     * Applique les paramètres de debug au shader
     */
    public void applyDebugParameters(CrystallizeShader shader, float crystalSize) {
        if (shader == null) return;
        
        // Configuration complète du shader avec les paramètres de debug
        shader.setCrystalSize(crystalSize);
        shader.setRandomness(debugRandomness);
        shader.setEdgeThickness(debugEdgeThickness);
        shader.setStretch(debugStretch);
        shader.setEdgeColor(debugEdgeColorR, debugEdgeColorG, debugEdgeColorB, debugEdgeColorA);
        shader.setFadeEdges(debugFadeEdges);
    }
    
    
    /**
     * Dessine l'interface de debug
     */
    public void drawDebugInfo(SpriteBatch batch, float viewportHeight) {
        if (!debugMode) return;
        
        float x = 20;
        float y = viewportHeight - 30; // Utiliser la hauteur du viewport passée en paramètre
        float lineHeight = 22; // Légèrement plus compact
        
        // Titre
        font.setColor(1, 1, 1, 1); // Blanc
        String title = "DEBUG MODE";
        layout.setText(font, title);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Taille des cellules
        boolean isCrystalSizeSelected = selectedDebugParameter == DebugParameter.CRYSTAL_SIZE;
        font.setColor(isCrystalSizeSelected ? 1.0f : 1.0f, isCrystalSizeSelected ? 0.0f : 1.0f, isCrystalSizeSelected ? 0.0f : 1.0f, 1.0f);
        String crystalSizeText = "Crystal Size: " + String.format("%.0f", debugCrystalSize);
        layout.setText(font, crystalSizeText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Épaisseur des bords (edge thickness)
        boolean isEdgeThicknessSelected = selectedDebugParameter == DebugParameter.EDGE_THICKNESS;
        font.setColor(isEdgeThicknessSelected ? 1.0f : 1.0f, isEdgeThicknessSelected ? 0.0f : 1.0f, isEdgeThicknessSelected ? 0.0f : 1.0f, 1.0f);
        String edgeThicknessText = "Edge Thickness: " + String.format("%.3f", debugEdgeThickness);
        layout.setText(font, edgeThicknessText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Randomness
        boolean isRandomnessSelected = selectedDebugParameter == DebugParameter.RANDOMNESS;
        font.setColor(isRandomnessSelected ? 1.0f : 1.0f, isRandomnessSelected ? 0.0f : 1.0f, isRandomnessSelected ? 0.0f : 1.0f, 1.0f);
        String randomnessText = "Randomness: " + String.format("%.4f", debugRandomness);
        layout.setText(font, randomnessText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Stretch
        boolean isStretchSelected = selectedDebugParameter == DebugParameter.STRETCH;
        font.setColor(isStretchSelected ? 1.0f : 1.0f, isStretchSelected ? 0.0f : 1.0f, isStretchSelected ? 0.0f : 1.0f, 1.0f);
        String stretchText = "Stretch: " + String.format("%.3f", debugStretch);
        layout.setText(font, stretchText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Couleur de bord R
        boolean isEdgeColorRSelected = selectedDebugParameter == DebugParameter.EDGE_COLOR_R;
        font.setColor(isEdgeColorRSelected ? 1.0f : 1.0f, isEdgeColorRSelected ? 0.0f : 1.0f, isEdgeColorRSelected ? 0.0f : 1.0f, 1.0f);
        String edgeColorRText = "Edge Color R: " + String.format("%.3f", debugEdgeColorR);
        layout.setText(font, edgeColorRText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Couleur de bord G
        boolean isEdgeColorGSelected = selectedDebugParameter == DebugParameter.EDGE_COLOR_G;
        font.setColor(isEdgeColorGSelected ? 1.0f : 1.0f, isEdgeColorGSelected ? 0.0f : 1.0f, isEdgeColorGSelected ? 0.0f : 1.0f, 1.0f);
        String edgeColorGText = "Edge Color G: " + String.format("%.3f", debugEdgeColorG);
        layout.setText(font, edgeColorGText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Couleur de bord B
        boolean isEdgeColorBSelected = selectedDebugParameter == DebugParameter.EDGE_COLOR_B;
        font.setColor(isEdgeColorBSelected ? 1.0f : 1.0f, isEdgeColorBSelected ? 0.0f : 1.0f, isEdgeColorBSelected ? 0.0f : 1.0f, 1.0f);
        String edgeColorBText = "Edge Color B: " + String.format("%.3f", debugEdgeColorB);
        layout.setText(font, edgeColorBText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Couleur de bord A
        boolean isEdgeColorASelected = selectedDebugParameter == DebugParameter.EDGE_COLOR_A;
        font.setColor(isEdgeColorASelected ? 1.0f : 1.0f, isEdgeColorASelected ? 0.0f : 1.0f, isEdgeColorASelected ? 0.0f : 1.0f, 1.0f);
        String edgeColorAText = "Edge Color A: " + String.format("%.3f", debugEdgeColorA);
        layout.setText(font, edgeColorAText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
        // Fade Edges
        boolean isFadeEdgesSelected = selectedDebugParameter == DebugParameter.FADE_EDGES;
        font.setColor(isFadeEdgesSelected ? 1.0f : 1.0f, isFadeEdgesSelected ? 0.0f : 1.0f, isFadeEdgesSelected ? 0.0f : 1.0f, 1.0f);
        String fadeEdgesText = "Fade Edges: " + (debugFadeEdges ? "ON" : "OFF");
        layout.setText(font, fadeEdgesText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;
        
                // Instructions
                font.setColor(1, 1, 1, 1); // Blanc
                String instructions = "UP/DOWN: Select Parameter | LEFT/RIGHT: Modify Value";
                layout.setText(font, instructions);
                font.draw(batch, layout, x, y);
                y -= lineHeight;

                String holdInstructions = "Hold LEFT/RIGHT for continuous modification";
                layout.setText(font, holdInstructions);
                font.draw(batch, layout, x, y);
                y -= lineHeight;
                
                String saveInstructions = "S: Save current settings to games.json";
                layout.setText(font, saveInstructions);
                font.draw(batch, layout, x, y);
                y -= lineHeight;
                
                // Afficher la confirmation de sauvegarde si active
                if (showSaveConfirmation) {
                    font.setColor(0.0f, 1.0f, 0.0f, 1.0f); // Vert
                    String confirmationText = "✓ SETTINGS SAVED!";
                    layout.setText(font, confirmationText);
                    font.draw(batch, layout, x, y);
                    font.setColor(1.0f, 1.0f, 1.0f, 1.0f); // Remettre en blanc
                }
    }
    
    // Getters pour les paramètres de debug
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public float getDebugCrystalSize() {
        return debugCrystalSize;
    }
    
    public float getDebugEdgeThickness() {
        return debugEdgeThickness;
    }
    
    public float getDebugRandomness() {
        return debugRandomness;
    }
    
    public float getDebugStretch() {
        return debugStretch;
    }
    
    public float getDebugEdgeColorR() {
        return debugEdgeColorR;
    }
    
    public float getDebugEdgeColorG() {
        return debugEdgeColorG;
    }
    
    public float getDebugEdgeColorB() {
        return debugEdgeColorB;
    }
    
    public float getDebugEdgeColorA() {
        return debugEdgeColorA;
    }
    
    public boolean getDebugFadeEdges() {
        return debugFadeEdges;
    }
    
    /**
     * Sauvegarde les paramètres actuels de debug dans le fichier games.json
     */
    private void saveCurrentSettings() {
        if (currentGameReference == null || currentGameReference.isEmpty()) {
            System.err.println("Impossible de sauvegarder : référence du jeu non définie");
            return;
        }
        
        try {
            System.out.println("Sauvegarde des paramètres de debug pour le jeu: " + currentGameReference);
            
            // Lire le fichier games.json
            String jsonContent = Gdx.files.internal(gamesJsonPath).readString();
            
            // Parser le JSON avec JsonReader
            JsonReader jsonReader = new JsonReader();
            JsonValue root = jsonReader.parse(jsonContent);
            JsonValue gamesArray = root.get("games");
            
            // Trouver le jeu correspondant
            JsonValue targetGame = null;
            for (JsonValue game : gamesArray) {
                if (currentGameReference.equals(game.getString("reference"))) {
                    targetGame = game;
                    break;
                }
            }
            
            if (targetGame == null) {
                System.err.println("Jeu non trouvé dans games.json: " + currentGameReference);
                return;
            }
            
            // Mettre à jour les paramètres
            JsonValue parameters = targetGame.get("parameters");
            if (parameters == null) {
                parameters = new JsonValue(JsonValue.ValueType.object);
                targetGame.addChild("parameters", parameters);
            }
            
            // Sauvegarder les paramètres actuels
            parameters.remove("initialCrystalSize");
            parameters.addChild("initialCrystalSize", new JsonValue((int)debugCrystalSize));
            parameters.remove("randomness");
            parameters.addChild("randomness", new JsonValue(debugRandomness));
            parameters.remove("edgeThickness");
            parameters.addChild("edgeThickness", new JsonValue(debugEdgeThickness));
            parameters.remove("stretch");
            parameters.addChild("stretch", new JsonValue(debugStretch));
            parameters.remove("fadeEdges");
            parameters.addChild("fadeEdges", new JsonValue(debugFadeEdges));
            parameters.remove("edgeColor");
            parameters.addChild("edgeColor", new JsonValue(String.format("%.3f,%.3f,%.3f,%.3f", 
                debugEdgeColorR, debugEdgeColorG, debugEdgeColorB, debugEdgeColorA)));
            
            // Reconstruire le JSON
            String updatedJson = root.prettyPrint(JsonWriter.OutputType.json, 0);
            
            // Écrire le fichier mis à jour dans le répertoire local
            String localPath = "games.json"; // Fichier local sans le dossier assets
            Gdx.files.local(localPath).writeString(updatedJson, false);
            
            System.out.println("Paramètres sauvegardés avec succès dans: " + localPath);
            System.out.println("  - InitialCrystalSize: " + (int)debugCrystalSize);
            System.out.println("  - Randomness: " + debugRandomness);
            System.out.println("  - EdgeThickness: " + debugEdgeThickness);
            System.out.println("  - Stretch: " + debugStretch);
            System.out.println("  - FadeEdges: " + debugFadeEdges);
            System.out.println("  - EdgeColor: " + String.format("%.3f,%.3f,%.3f,%.3f", 
                debugEdgeColorR, debugEdgeColorG, debugEdgeColorB, debugEdgeColorA));
            
            // Activer la confirmation visuelle
            showSaveConfirmation = true;
            saveConfirmationTimer = 0.0f;
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des paramètres: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
