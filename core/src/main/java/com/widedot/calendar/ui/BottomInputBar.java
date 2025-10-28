package com.widedot.calendar.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.widedot.calendar.platform.PlatformRegistry;
import com.widedot.calendar.platform.KeyboardInsetsRegistry;
import com.widedot.calendar.utils.CarlitoFontManager;

/**
 * Barre d'input en bas d'écran avec TextField monoligne
 * Gère le clavier virtuel mobile et la validation par Enter
 */
public class BottomInputBar extends Table {

    public interface Listener {
        void onSubmit(String text);
        default void onFocusChanged(boolean focused) {}
        default void onExitInputMode() {}
    }

    private final TextField field;
    private Listener listener;
    
    // Offset du clavier pour iOS
    private int keyboardBottomInsetPx = 0; // pixels HTML

    // ✅ Texture statique réutilisable pour le fond noir
    private static Texture backgroundTexture;

    public BottomInputBar(Skin skin) {
        super(skin);
        pad(20);
        
        // S'enregistrer pour recevoir les notifications d'insets clavier
        KeyboardInsetsRegistry.register(this);

        // Créer la texture blanche une seule fois
        if (backgroundTexture == null) {
            Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.WHITE);
            pixmap.fill();
            backgroundTexture = new Texture(pixmap);
            pixmap.dispose();
        }

        // Style du TextField
        TextField.TextFieldStyle textFieldStyle;
        
        // Créer directement le style sans dépendre du skin
        textFieldStyle = new TextField.TextFieldStyle();
        // Créer une font indépendante avec les bonnes configurations Distance Field (comme CarlitoFontManager)
        com.badlogic.gdx.graphics.Texture textFieldTexture = new com.badlogic.gdx.graphics.Texture(Gdx.files.internal("skin/carlito.png"), true);
        textFieldTexture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearNearest, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        textFieldStyle.font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("skin/carlito.fnt"), new com.badlogic.gdx.graphics.g2d.TextureRegion(textFieldTexture), false);
        textFieldStyle.font.getData().setScale(2.0f);
        textFieldStyle.fontColor = Color.WHITE;
        
        // Créer un curseur personnalisé
        Pixmap pm = new Pixmap(2, 16, Pixmap.Format.RGBA8888);
        pm.setColor(Color.WHITE);
        pm.fill();
        textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
        pm.dispose();

        field = new TextField("", textFieldStyle);
        field.setMessageText("Tapez votre réponse...");
        field.setFocusTraversal(false);
        field.setMaxLength(100);

        // Enter → submit, Échap → sortir du mode saisie
        field.setTextFieldListener((tf, c) -> {
            if (c == '\n' || c == '\r') {
                submit();
            } else if (c == 27) { // Échap (ASCII 27)
                exitInputMode();
            }
        });
        
        // Ajouter un listener pour gérer la touche Échap au niveau du Stage
        field.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    exitInputMode();
                    return true;
                }
                return false;
            }
        });

        add(field).expandX().fillX();

        // Ajouter un listener sur le field pour le focus synchrone (crucial pour iOS)
        field.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // 1) Prendre le focus Scene2D
                Stage stage = getStage();
                if (stage != null) stage.setKeyboardFocus(field);

                // 2) Demander le keyboard natif (sera routé vers IosKeyboardBridge)
                if (PlatformRegistry.get() != null && PlatformRegistry.get().isMobileBrowser()) {
                    PlatformRegistry.get().onVirtualKeyboardRequest(true);
                }
                return false; // laisser TextField gérer le reste
            }
        });

        // Gestion du focus (mobile)
        field.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (listener != null) listener.onFocusChanged(focused);
                if (PlatformRegistry.get() != null && PlatformRegistry.get().isMobileBrowser()) {
                    PlatformRegistry.get().onVirtualKeyboardRequest(focused);
                }
            }
        });

        field.setOnscreenKeyboard(visible -> Gdx.input.setOnscreenKeyboardVisible(visible));
        
        // Ajouter un listener global au niveau de la Table pour capturer les clics en dehors du TextField
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // Vérifier si le clic est en dehors du TextField
                // Convertir les coordonnées locales du TextField
                com.badlogic.gdx.math.Vector2 localCoords = new com.badlogic.gdx.math.Vector2(x, y);
                localCoords = field.parentToLocalCoordinates(localCoords);
                
                // Si le clic est en dehors du TextField, sortir du mode saisie
                if (localCoords.x < 0 || localCoords.x > field.getWidth() || 
                    localCoords.y < 0 || localCoords.y > field.getHeight()) {
                    exitInputMode();
                    return true;
                }
                return false;
            }
        });
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    private void submit() {
        String text = field.getText().trim();
        if (text.isEmpty()) return;
        if (listener != null) listener.onSubmit(text);
        field.setText("");

        // Vider l'input natif si on est sur mobile
        if (PlatformRegistry.get() != null && PlatformRegistry.get().isMobileBrowser()) {
            PlatformRegistry.get().clearNativeInput();
        }

        Stage stage = getStage();
        if (stage != null) stage.setKeyboardFocus(null);
    }
    
    private void exitInputMode() {
        // Vider le champ pour réafficher le hint avec la question
        field.setText("");
        
        // Perdre le focus du TextField
        Stage stage = getStage();
        if (stage != null) stage.setKeyboardFocus(null);
        
        // Notifier le listener pour réafficher la question
        if (listener != null) listener.onExitInputMode();
    }

    public void focus() {
        Stage stage = getStage();
        if (stage != null) stage.setKeyboardFocus(field);
    }

    public void clear() {
        field.setText("");
    }

    public String getText() {
        return field.getText();
    }

    public void setPlaceholderText(String placeholderText) {
        field.setMessageText(placeholderText);
    }
    
    /**
     * Met à jour le texte du champ (utilisé pour la synchronisation avec l'input natif)
     * @param text Le nouveau texte
     */
    public void setText(String text) {
        field.setText(text);
    }
    
    /**
     * Ajoute du texte au champ (utilisé pour la synchronisation avec l'input natif)
     * @param text Le texte à ajouter
     */
    public void appendText(String text) {
        field.setText(field.getText() + text);
    }
    
    /**
     * Force la sortie du mode saisie (appelé par les jeux lors d'un clic en dehors)
     */
    public void forceExitInputMode() {
        exitInputMode();
    }
    
    /**
     * Vérifie si le champ a le focus
     */
    public boolean hasFocus() {
        Stage stage = getStage();
        return stage != null && stage.getKeyboardFocus() == field;
    }
    
    /**
     * Vérifie si un point (en coordonnées du Stage) est sur le TextField
     */
    public boolean containsPoint(float stageX, float stageY) {
        com.badlogic.gdx.math.Vector2 localCoords = new com.badlogic.gdx.math.Vector2(stageX, stageY);
        localCoords = field.stageToLocalCoordinates(localCoords);
        return localCoords.x >= 0 && localCoords.x <= field.getWidth() && 
               localCoords.y >= 0 && localCoords.y <= field.getHeight();
    }
    
    /**
     * Définit l'offset du clavier pour iOS/Android
     * @param px Hauteur du clavier en pixels HTML
     */
    public void setKeyboardInsetPx(int px) {
        this.keyboardBottomInsetPx = px;
    }
    
    /**
     * Synchronise le texte avec l'input natif (Android composition)
     * @param text Le texte de l'input natif
     */
    public void syncWithNativeInput(String text) {
        if (text != null && !text.equals(field.getText())) {
            field.setText(text);
        }
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        
        // Appliquer l'offset du clavier pour iOS
        if (keyboardBottomInsetPx > 0) {
            // Convertir pixels HTML -> pixels LibGDX (GWT remappe déjà, souvent 1:1)
            float offset = keyboardBottomInsetPx;
            
            // Laisser 8dp d'air
            float extra = 8f * Gdx.graphics.getDensity();
            
            // Positionner la barre au-dessus du clavier
            setY(offset + extra);
        } else {
            // Pas de clavier, position normale
            setY(0);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // ✅ Dessiner le rectangle noir semi-transparent derrière la barre
        Color old = batch.getColor();
        batch.setColor(0, 0, 0, 0.6f); // 60% d'opacité
        batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight());
        batch.setColor(old);

        // Appliquer le shader Distance Field pour le TextField
        com.badlogic.gdx.graphics.glutils.ShaderProgram originalShader = batch.getShader();
        batch.setShader(CarlitoFontManager.getShader());
        
        super.draw(batch, parentAlpha);
        
        // Restaurer le shader d'origine
        batch.setShader(originalShader);
    }

    /**
     * Méthode pour se désenregistrer du registre d'insets
     */
    public void dispose() {
        KeyboardInsetsRegistry.unregister(this);
    }

    /** Appelé à la fermeture de l'app pour nettoyer la texture */
    public static void disposeStaticResources() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }
}