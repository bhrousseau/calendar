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

/**
 * Gestionnaire de debug pour le sliding puzzle
 * Gère tous les paramètres de debug et l'interface utilisateur
 */
public class SlidingPuzzleDebugManager {

    // Paramètres de debug
    private int debugSize = 4;
    private int debugBgColorR = 0;
    private int debugBgColorG = 0;
    private int debugBgColorB = 0;
    private float debugBackgroundHue = 0f;
    private float debugBackgroundSaturation = 0f;
    private float debugBackgroundLightness = 0f;
    private int debugShuffle = 10;
    private float debugAnimationSpeed = 10.0f;
    private boolean debugMode = false;

    // Système de debug avancé
    public enum DebugParameter {
        SIZE,
        BG_COLOR_R,
        BG_COLOR_G,
        BG_COLOR_B,
        BG_HUE,
        BG_SATURATION,
        BG_LIGHTNESS,
        SHUFFLE,
        ANIMATION_SPEED
    }

    private DebugParameter selectedDebugParameter = DebugParameter.SIZE;
    private boolean isKeyHeld = false;
    private float keyHoldTime = 0.0f;
    private static final float KEY_HOLD_DELAY = 0.5f; // Délai avant l'appui continu
    private static final float KEY_HOLD_INTERVAL = 0.05f; // Intervalle entre les modifications en mode hold

    // Interface
    private final BitmapFont font;
    private final GlyphLayout layout;
    private final com.badlogic.gdx.graphics.Texture whiteTexture;

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
        void onDebugSettingsSaved();
    }

    public SlidingPuzzleDebugManager(BitmapFont font) {
        this.font = font;
        this.layout = new GlyphLayout();
        
        // Créer une texture blanche pour le fond
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1);
        pixmap.fill();
        this.whiteTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
        pixmap.dispose();
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
        if (parameters.containsKey("size")) {
            debugSize = ((Number) parameters.get("size")).intValue();
        }
        if (parameters.containsKey("bgColor")) {
            String bgColor = (String) parameters.get("bgColor");
            parseBackgroundColor(bgColor);
        }
        if (parameters.containsKey("bgHue")) {
            debugBackgroundHue = ((Number) parameters.get("bgHue")).floatValue();
        }
        if (parameters.containsKey("bgSaturation")) {
            debugBackgroundSaturation = ((Number) parameters.get("bgSaturation")).floatValue();
        }
        if (parameters.containsKey("bgLightness")) {
            debugBackgroundLightness = ((Number) parameters.get("bgLightness")).floatValue();
        }
        if (parameters.containsKey("shuffle")) {
            debugShuffle = ((Number) parameters.get("shuffle")).intValue();
        }
        if (parameters.containsKey("animationSpeed")) {
            debugAnimationSpeed = ((Number) parameters.get("animationSpeed")).floatValue();
        }
    }

    /**
     * Parse la couleur de fond depuis une chaîne "r,g,b"
     */
    private void parseBackgroundColor(String colorStr) {
        try {
            String[] parts = colorStr.split(",");
            if (parts.length == 3) {
                debugBgColorR = Integer.parseInt(parts[0]);
                debugBgColorG = Integer.parseInt(parts[1]);
                debugBgColorB = Integer.parseInt(parts[2]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Format de couleur invalide: " + colorStr + ", utilisation des valeurs par défaut");
            debugBgColorR = 0;
            debugBgColorG = 0;
            debugBgColorB = 0;
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
                    case SIZE:
                        selectedDebugParameter = DebugParameter.ANIMATION_SPEED;
                        break;
                    case BG_COLOR_R:
                        selectedDebugParameter = DebugParameter.SIZE;
                        break;
                    case BG_COLOR_G:
                        selectedDebugParameter = DebugParameter.BG_COLOR_R;
                        break;
                    case BG_COLOR_B:
                        selectedDebugParameter = DebugParameter.BG_COLOR_G;
                        break;
                    case BG_HUE:
                        selectedDebugParameter = DebugParameter.BG_COLOR_B;
                        break;
                    case BG_SATURATION:
                        selectedDebugParameter = DebugParameter.BG_HUE;
                        break;
                    case BG_LIGHTNESS:
                        selectedDebugParameter = DebugParameter.BG_SATURATION;
                        break;
                    case SHUFFLE:
                        selectedDebugParameter = DebugParameter.BG_LIGHTNESS;
                        break;
                    case ANIMATION_SPEED:
                        selectedDebugParameter = DebugParameter.SHUFFLE;
                        break;
                }
                return true;
            }
            if (keycode == Input.Keys.DOWN) {
                // Changer de paramètre sélectionné (cycle vers le bas)
                switch (selectedDebugParameter) {
                    case SIZE:
                        selectedDebugParameter = DebugParameter.BG_COLOR_R;
                        break;
                    case BG_COLOR_R:
                        selectedDebugParameter = DebugParameter.BG_COLOR_G;
                        break;
                    case BG_COLOR_G:
                        selectedDebugParameter = DebugParameter.BG_COLOR_B;
                        break;
                    case BG_COLOR_B:
                        selectedDebugParameter = DebugParameter.BG_HUE;
                        break;
                    case BG_HUE:
                        selectedDebugParameter = DebugParameter.BG_SATURATION;
                        break;
                    case BG_SATURATION:
                        selectedDebugParameter = DebugParameter.BG_LIGHTNESS;
                        break;
                    case BG_LIGHTNESS:
                        selectedDebugParameter = DebugParameter.SHUFFLE;
                        break;
                    case SHUFFLE:
                        selectedDebugParameter = DebugParameter.ANIMATION_SPEED;
                        break;
                    case ANIMATION_SPEED:
                        selectedDebugParameter = DebugParameter.SIZE;
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
            case SIZE:
                debugSize = Math.max(2, Math.min(8, debugSize + direction));
                break;
            case BG_COLOR_R:
                debugBgColorR = Math.max(0, Math.min(255, debugBgColorR + direction * 5));
                break;
            case BG_COLOR_G:
                debugBgColorG = Math.max(0, Math.min(255, debugBgColorG + direction * 5));
                break;
            case BG_COLOR_B:
                debugBgColorB = Math.max(0, Math.min(255, debugBgColorB + direction * 5));
                break;
            case BG_HUE:
                debugBackgroundHue = Math.max(0f, Math.min(360f, debugBackgroundHue + direction * 5f));
                break;
            case BG_SATURATION:
                debugBackgroundSaturation = Math.max(0f, Math.min(100f, debugBackgroundSaturation + direction * 5f));
                break;
            case BG_LIGHTNESS:
                debugBackgroundLightness = Math.max(-100f, Math.min(100f, debugBackgroundLightness + direction * 5f));
                break;
            case SHUFFLE:
                debugShuffle = Math.max(1, Math.min(100, debugShuffle + direction));
                break;
            case ANIMATION_SPEED:
                debugAnimationSpeed = Math.max(0.1f, Math.min(50.0f, debugAnimationSpeed + direction * 0.5f));
                break;
        }

        // Notifier le changement
        if (changeCallback != null) {
            changeCallback.onDebugParameterChanged();
        }
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
            parameters.remove("size");
            parameters.addChild("size", new JsonValue(debugSize));
            parameters.remove("bgColor");
            parameters.addChild("bgColor", new JsonValue(debugBgColorR + "," + debugBgColorG + "," + debugBgColorB));
            parameters.remove("bgHue");
            parameters.addChild("bgHue", new JsonValue(debugBackgroundHue));
            parameters.remove("bgSaturation");
            parameters.addChild("bgSaturation", new JsonValue(debugBackgroundSaturation));
            parameters.remove("bgLightness");
            parameters.addChild("bgLightness", new JsonValue(debugBackgroundLightness));
            parameters.remove("shuffle");
            parameters.addChild("shuffle", new JsonValue(debugShuffle));
            parameters.remove("animationSpeed");
            parameters.addChild("animationSpeed", new JsonValue(debugAnimationSpeed));

            // Reconstruire le JSON
            String updatedJson = root.prettyPrint(JsonWriter.OutputType.json, 0);

            // Écrire le fichier mis à jour dans le répertoire local
            String localPath = "games.json"; // Fichier local sans le dossier assets
            Gdx.files.local(localPath).writeString(updatedJson, false);


            // Activer la confirmation visuelle
            showSaveConfirmation = true;
            saveConfirmationTimer = 0.0f;
            
            // Notifier que les paramètres ont été sauvegardés
            if (changeCallback != null) {
                changeCallback.onDebugSettingsSaved();
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde des paramètres: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Dessine l'interface de debug
     */
    public void drawDebugInfo(SpriteBatch batch, float viewportHeight) {
        if (!debugMode) {
            return;
        }

        float x = 20;
        float y = viewportHeight - 50; // Position plus haute pour être plus visible
        float lineHeight = 22; // Légèrement plus compact
        
        // Dessiner un fond semi-transparent pour améliorer la visibilité
        batch.setColor(0, 0, 0, 0.8f); // Fond noir semi-transparent
        batch.draw(whiteTexture, x - 10, y - 250, 450, 250); // Rectangle de fond plus grand
        batch.setColor(1, 1, 1, 1); // Remettre la couleur blanche

        // Titre
        font.setColor(1, 1, 1, 1); // Blanc
        String title = "DEBUG MODE - SLIDING PUZZLE";
        layout.setText(font, title);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        // Taille du puzzle
        boolean isSizeSelected = selectedDebugParameter == DebugParameter.SIZE;
        font.setColor(isSizeSelected ? 1.0f : 1.0f, isSizeSelected ? 0.0f : 1.0f, isSizeSelected ? 0.0f : 1.0f, 1.0f);
        String sizeText = "Size: " + debugSize + "x" + debugSize;
        layout.setText(font, sizeText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        // Couleur de fond RGB
        boolean isBgColorRSelected = selectedDebugParameter == DebugParameter.BG_COLOR_R;
        font.setColor(isBgColorRSelected ? 1.0f : 1.0f, isBgColorRSelected ? 0.0f : 1.0f, isBgColorRSelected ? 0.0f : 1.0f, 1.0f);
        String bgColorRText = "Background R: " + debugBgColorR;
        layout.setText(font, bgColorRText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        boolean isBgColorGSelected = selectedDebugParameter == DebugParameter.BG_COLOR_G;
        font.setColor(isBgColorGSelected ? 1.0f : 1.0f, isBgColorGSelected ? 0.0f : 1.0f, isBgColorGSelected ? 0.0f : 1.0f, 1.0f);
        String bgColorGText = "Background G: " + debugBgColorG;
        layout.setText(font, bgColorGText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        boolean isBgColorBSelected = selectedDebugParameter == DebugParameter.BG_COLOR_B;
        font.setColor(isBgColorBSelected ? 1.0f : 1.0f, isBgColorBSelected ? 0.0f : 1.0f, isBgColorBSelected ? 0.0f : 1.0f, 1.0f);
        String bgColorBText = "Background B: " + debugBgColorB;
        layout.setText(font, bgColorBText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        // Paramètres HSL
        boolean isBgHueSelected = selectedDebugParameter == DebugParameter.BG_HUE;
        font.setColor(isBgHueSelected ? 1.0f : 1.0f, isBgHueSelected ? 0.0f : 1.0f, isBgHueSelected ? 0.0f : 1.0f, 1.0f);
        String bgHueText = "Background Hue: " + String.format("%.1f", debugBackgroundHue);
        layout.setText(font, bgHueText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        boolean isBgSaturationSelected = selectedDebugParameter == DebugParameter.BG_SATURATION;
        font.setColor(isBgSaturationSelected ? 1.0f : 1.0f, isBgSaturationSelected ? 0.0f : 1.0f, isBgSaturationSelected ? 0.0f : 1.0f, 1.0f);
        String bgSaturationText = "Background Saturation: " + String.format("%.1f", debugBackgroundSaturation);
        layout.setText(font, bgSaturationText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        boolean isBgLightnessSelected = selectedDebugParameter == DebugParameter.BG_LIGHTNESS;
        font.setColor(isBgLightnessSelected ? 1.0f : 1.0f, isBgLightnessSelected ? 0.0f : 1.0f, isBgLightnessSelected ? 0.0f : 1.0f, 1.0f);
        String bgLightnessText = "Background Lightness: " + String.format("%.1f", debugBackgroundLightness);
        layout.setText(font, bgLightnessText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        // Nombre de mélanges
        boolean isShuffleSelected = selectedDebugParameter == DebugParameter.SHUFFLE;
        font.setColor(isShuffleSelected ? 1.0f : 1.0f, isShuffleSelected ? 0.0f : 1.0f, isShuffleSelected ? 0.0f : 1.0f, 1.0f);
        String shuffleText = "Shuffle Count: " + debugShuffle;
        layout.setText(font, shuffleText);
        font.draw(batch, layout, x, y);
        y -= lineHeight;

        // Vitesse d'animation
        boolean isAnimationSpeedSelected = selectedDebugParameter == DebugParameter.ANIMATION_SPEED;
        font.setColor(isAnimationSpeedSelected ? 1.0f : 1.0f, isAnimationSpeedSelected ? 0.0f : 1.0f, isAnimationSpeedSelected ? 0.0f : 1.0f, 1.0f);
        String animationSpeedText = "Animation Speed: " + String.format("%.1f", debugAnimationSpeed);
        layout.setText(font, animationSpeedText);
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

    public int getDebugSize() {
        return debugSize;
    }


    public int getDebugShuffle() {
        return debugShuffle;
    }

    public float getDebugAnimationSpeed() {
        return debugAnimationSpeed;
    }

    public int getDebugBgColorR() {
        return debugBgColorR;
    }

    public int getDebugBgColorG() {
        return debugBgColorG;
    }

    public int getDebugBgColorB() {
        return debugBgColorB;
    }

    public float getDebugBackgroundHue() {
        return debugBackgroundHue;
    }

    public float getDebugBackgroundSaturation() {
        return debugBackgroundSaturation;
    }

    public float getDebugBackgroundLightness() {
        return debugBackgroundLightness;
    }
}
