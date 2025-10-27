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

    // ✅ Texture statique réutilisable pour le fond noir
    private static Texture backgroundTexture;

    public BottomInputBar(Skin skin) {
        super(skin);
        pad(20);

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

    /** Appelé à la fermeture de l'app pour nettoyer la texture */
    public static void disposeStaticResources() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }
}