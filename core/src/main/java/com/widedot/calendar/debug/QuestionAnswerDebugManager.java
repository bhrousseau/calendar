package com.widedot.calendar.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ObjectMap;
import com.widedot.calendar.utils.CarlitoFontManager;

/**
 * Gestionnaire de debug pour QuestionAnswerGameScreen
 */
public class QuestionAnswerDebugManager {
    private final BitmapFont font;
    private boolean debugMode = false;
    private String currentQuestionsFile = "";
    private int selectedFileIndex = 0;
    private int totalQuestions = 5;
    private int victoryThreshold = 3;
    private int selectedParameter = 0; // 0=questionsFile, 1=totalQuestions, 2=victoryThreshold
    private String[] availableFiles = {
        "quizz_cultureg.json",
        "quizz_culturepop.json", 
        "quizz_gastro .json",
        "quizz_merveille.json",
        "quizz_mytho.json",
        "quizz_painting.json"
    };
    
    // Callback pour notifier les changements
    private DebugChangeCallback changeCallback;
    
    // Informations pour la sauvegarde
    private String currentGameReference;
    private String gamesJsonPath = "games.json";
    
    // Confirmation de sauvegarde
    private boolean showSaveConfirmation = false;
    private float saveConfirmationTimer = 0.0f;
    private static final float SAVE_CONFIRMATION_DURATION = 2.0f; // 2 secondes
    
    public interface DebugChangeCallback {
        void onDebugParameterChanged();
        void onDebugSettingsSaved();
    }
    
    public QuestionAnswerDebugManager(BitmapFont font) {
        CarlitoFontManager.initialize();
        this.font = CarlitoFontManager.getFont();
    }
    
    public void setChangeCallback(DebugChangeCallback callback) {
        this.changeCallback = callback;
    }
    
    public void setCurrentGameReference(String gameReference) {
        this.currentGameReference = gameReference;
    }
    
    public void initializeFromGameParameters(ObjectMap<String, Object> parameters) {
        if (parameters != null) {
            if (parameters.containsKey("questionsFile")) {
                String file = (String) parameters.get("questionsFile");
                if (file != null && !file.isEmpty()) {
                    // Trouver l'index du fichier dans la liste
                    for (int i = 0; i < availableFiles.length; i++) {
                        if (availableFiles[i].equals(file)) {
                            selectedFileIndex = i;
                            currentQuestionsFile = file;
                            break;
                        }
                    }
                } else {
                    // Fichier vide ou null, utiliser le premier fichier par défaut
                    selectedFileIndex = 0;
                    currentQuestionsFile = availableFiles[0];
                }
            } else {
                // Aucun paramètre questionsFile, utiliser le premier fichier par défaut
                selectedFileIndex = 0;
                currentQuestionsFile = availableFiles[0];
            }
            
            // Charger totalQuestions
            if (parameters.containsKey("totalQuestions")) {
                this.totalQuestions = ((Number) parameters.get("totalQuestions")).intValue();
            }
            
            // Charger victoryThreshold
            if (parameters.containsKey("victoryThreshold")) {
                this.victoryThreshold = ((Number) parameters.get("victoryThreshold")).intValue();
            }
        } else {
            // Aucun paramètre, utiliser les valeurs par défaut
            selectedFileIndex = 0;
            currentQuestionsFile = availableFiles[0];
        }
    }
    
    public boolean handleKeyDown(int keycode) {
        // Alt+D : Activer/désactiver le mode debug
        if (keycode == com.badlogic.gdx.Input.Keys.D && (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ALT_LEFT) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ALT_RIGHT))) {
            debugMode = !debugMode;
            Gdx.app.log("QuestionAnswerDebugManager", "Mode debug: " + (debugMode ? "ACTIVÉ" : "DÉSACTIVÉ"));
            return true;
        }
        
        // Alt+S : Sauvegarder les paramètres (uniquement en mode debug)
        if (keycode == com.badlogic.gdx.Input.Keys.S && debugMode && (com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ALT_LEFT) || com.badlogic.gdx.Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ALT_RIGHT))) {
            saveDebugSettings();
            return true;
        }
        
        // En mode debug, gérer les touches de navigation
        if (debugMode) {
            switch (keycode) {
                case com.badlogic.gdx.Input.Keys.UP:
                    // Sélectionner le paramètre précédent
                    selectedParameter = (selectedParameter - 1 + 3) % 3;
                    Gdx.app.log("QuestionAnswerDebugManager", "Paramètre sélectionné: " + selectedParameter);
                    return true;
                    
                case com.badlogic.gdx.Input.Keys.DOWN:
                    // Sélectionner le paramètre suivant
                    selectedParameter = (selectedParameter + 1) % 3;
                    Gdx.app.log("QuestionAnswerDebugManager", "Paramètre sélectionné: " + selectedParameter);
                    return true;
                    
                case com.badlogic.gdx.Input.Keys.LEFT:
                    // Modifier la valeur du paramètre sélectionné
                    switch (selectedParameter) {
                        case 0: // questionsFile
                            selectedFileIndex = (selectedFileIndex - 1 + availableFiles.length) % availableFiles.length;
                            currentQuestionsFile = availableFiles[selectedFileIndex];
                            Gdx.app.log("QuestionAnswerDebugManager", "Fichier sélectionné: " + currentQuestionsFile);
                            break;
                        case 1: // totalQuestions
                            totalQuestions = Math.max(1, totalQuestions - 1);
                            Gdx.app.log("QuestionAnswerDebugManager", "Nombre de questions: " + totalQuestions);
                            break;
                        case 2: // victoryThreshold
                            victoryThreshold = Math.max(1, victoryThreshold - 1);
                            Gdx.app.log("QuestionAnswerDebugManager", "Seuil de victoire: " + victoryThreshold);
                            break;
                    }
                    if (changeCallback != null) {
                        changeCallback.onDebugParameterChanged();
                    }
                    return true;
                    
                case com.badlogic.gdx.Input.Keys.RIGHT:
                    // Modifier la valeur du paramètre sélectionné
                    switch (selectedParameter) {
                        case 0: // questionsFile
                            selectedFileIndex = (selectedFileIndex + 1) % availableFiles.length;
                            currentQuestionsFile = availableFiles[selectedFileIndex];
                            Gdx.app.log("QuestionAnswerDebugManager", "Fichier sélectionné: " + currentQuestionsFile);
                            break;
                        case 1: // totalQuestions
                            totalQuestions = Math.min(20, totalQuestions + 1);
                            Gdx.app.log("QuestionAnswerDebugManager", "Nombre de questions: " + totalQuestions);
                            break;
                        case 2: // victoryThreshold
                            victoryThreshold = Math.min(totalQuestions, victoryThreshold + 1);
                            Gdx.app.log("QuestionAnswerDebugManager", "Seuil de victoire: " + victoryThreshold);
                            break;
                    }
                    if (changeCallback != null) {
                        changeCallback.onDebugParameterChanged();
                    }
                    return true;
                    
                default:
                    return false;
            }
        }
        
        return false;
    }
    
    public boolean handleKeyUp(int keycode) {
        return false;
    }
    
    public void update(float delta) {
        // Gérer la confirmation de sauvegarde
        if (showSaveConfirmation) {
            saveConfirmationTimer += delta;
            if (saveConfirmationTimer >= SAVE_CONFIRMATION_DURATION) {
                showSaveConfirmation = false;
                saveConfirmationTimer = 0.0f;
            }
        }
    }
    
    public void drawDebugInfo(SpriteBatch batch, float viewportHeight) {
        if (!debugMode) return;

        // Afficher l'interface de debug complète
        float y = viewportHeight - 30;
        float lineHeight = 25;
        
        // Titre
        font.setColor(1f, 1f, 0f, 1f);
        CarlitoFontManager.drawText(batch, "=== DEBUG QNA ===", 20, y);
        y -= lineHeight;
        
        // Instructions
        font.setColor(0.8f, 0.8f, 0.8f, 1f);
        CarlitoFontManager.drawText(batch, "Alt+D: Quitter debug", 20, y);
        y -= lineHeight;
        CarlitoFontManager.drawText(batch, "↑↓: Sélectionner paramètre", 20, y);
        y -= lineHeight;
        CarlitoFontManager.drawText(batch, "←→: Modifier valeur", 20, y);
        y -= lineHeight;
        CarlitoFontManager.drawText(batch, "Alt+S: Sauvegarder", 20, y);
        y -= lineHeight;
        
        // Paramètres avec sélection
        String[] paramNames = {"Fichier de questions", "Nombre de questions", "Seuil de victoire"};
        String[] paramValues = {currentQuestionsFile, String.valueOf(totalQuestions), String.valueOf(victoryThreshold)};
        
        for (int i = 0; i < paramNames.length; i++) {
            if (i == selectedParameter) {
                font.setColor(1f, 0f, 0f, 1f); // Rouge pour la sélection
            } else {
                font.setColor(0.8f, 0.8f, 0.8f, 1f);
            }
            
            CarlitoFontManager.drawText(batch, paramNames[i] + ": " + paramValues[i], 20, y);
            y -= lineHeight;
        }
        
        // Liste des fichiers disponibles (si questionsFile sélectionné)
        if (selectedParameter == 0) {
            font.setColor(0.7f, 0.7f, 0.7f, 1f);
            CarlitoFontManager.drawText(batch, "Fichiers disponibles:", 20, y);
            y -= lineHeight;
            
            for (int i = 0; i < availableFiles.length; i++) {
                if (i == selectedFileIndex) {
                    font.setColor(1f, 1f, 0f, 1f);
                    CarlitoFontManager.drawText(batch, "► " + availableFiles[i], 40, y);
                } else {
                    font.setColor(0.6f, 0.6f, 0.6f, 1f);
                    CarlitoFontManager.drawText(batch, "  " + availableFiles[i], 40, y);
                }
                y -= lineHeight;
            }
        }
        
        // Afficher la confirmation de sauvegarde si active
        if (showSaveConfirmation) {
            font.setColor(0.0f, 1.0f, 0.0f, 1.0f); // Vert
            CarlitoFontManager.drawText(batch, "✓ PARAMÈTRES SAUVEGARDÉS!", 20, y);
            font.setColor(1.0f, 1.0f, 1.0f, 1.0f); // Remettre en blanc
        }
    }
    
    private void saveDebugSettings() {
        if (currentGameReference == null || currentGameReference.isEmpty()) {
            Gdx.app.error("QuestionAnswerDebugManager", "Impossible de sauvegarder : référence du jeu non définie");
            return;
        }

        try {
            // Lire le fichier games.json
            String jsonContent = Gdx.files.internal(gamesJsonPath).readString();

            // Parser le JSON avec JsonReader
            com.badlogic.gdx.utils.JsonReader jsonReader = new com.badlogic.gdx.utils.JsonReader();
            com.badlogic.gdx.utils.JsonValue root = jsonReader.parse(jsonContent);
            com.badlogic.gdx.utils.JsonValue gamesArray = root.get("games");

            // Trouver le jeu correspondant
            com.badlogic.gdx.utils.JsonValue targetGame = null;
            for (com.badlogic.gdx.utils.JsonValue game : gamesArray) {
                if (currentGameReference.equals(game.getString("reference"))) {
                    targetGame = game;
                    break;
                }
            }

            if (targetGame == null) {
                Gdx.app.error("QuestionAnswerDebugManager", "Jeu non trouvé dans games.json: " + currentGameReference);
                return;
            }

            // Mettre à jour les paramètres
            com.badlogic.gdx.utils.JsonValue parameters = targetGame.get("parameters");
            if (parameters == null) {
                parameters = new com.badlogic.gdx.utils.JsonValue(com.badlogic.gdx.utils.JsonValue.ValueType.object);
                targetGame.addChild("parameters", parameters);
            }

            // Sauvegarder les paramètres actuels
            parameters.remove("questionsFile");
            if (!currentQuestionsFile.isEmpty()) {
                parameters.addChild("questionsFile", new com.badlogic.gdx.utils.JsonValue(currentQuestionsFile));
            }
            
            parameters.remove("totalQuestions");
            parameters.addChild("totalQuestions", new com.badlogic.gdx.utils.JsonValue(totalQuestions));
            
            parameters.remove("victoryThreshold");
            parameters.addChild("victoryThreshold", new com.badlogic.gdx.utils.JsonValue(victoryThreshold));

            // Reconstruire le JSON
            String updatedJson = root.prettyPrint(com.badlogic.gdx.utils.JsonWriter.OutputType.json, 0);

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

            String logMessage = currentQuestionsFile.isEmpty() ? "questionsFile = (aucun)" : "questionsFile = " + currentQuestionsFile;
            Gdx.app.log("QuestionAnswerDebugManager", "Paramètres sauvegardés: " + logMessage + ", totalQuestions = " + totalQuestions + ", victoryThreshold = " + victoryThreshold);

        } catch (Exception e) {
            Gdx.app.error("QuestionAnswerDebugManager", "Erreur lors de la sauvegarde des paramètres: " + e.getMessage());
        }
    }
    
    // Getters
    public boolean isDebugMode() {
        return debugMode;
    }
    
    public String getCurrentQuestionsFile() {
        return currentQuestionsFile;
    }
    
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    public int getVictoryThreshold() {
        return victoryThreshold;
    }
}
