package com.widedot.calendar.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
        try {
            textFieldStyle = skin.get(TextField.TextFieldStyle.class);
            // Créer une nouvelle font Carlito spécifiquement pour le TextField
            textFieldStyle.font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("skin/carlito.fnt"));
            textFieldStyle.font.getData().setScale(2.0f);
            textFieldStyle.fontColor = Color.WHITE;
            Pixmap pm = new Pixmap(2, 16, Pixmap.Format.RGBA8888);
            pm.setColor(Color.WHITE);
            pm.fill();
            textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(new Texture(pm)));
            pm.dispose();
        } catch (Exception e) {
            textFieldStyle = new TextField.TextFieldStyle();
            // Créer une nouvelle font Carlito spécifiquement pour le TextField
            textFieldStyle.font = new com.badlogic.gdx.graphics.g2d.BitmapFont(Gdx.files.internal("skin/carlito.fnt"));
            textFieldStyle.font.getData().setScale(2.0f);
            textFieldStyle.fontColor = Color.WHITE;
        }

        field = new TextField("", textFieldStyle);
        field.setMessageText("Tapez votre réponse...");
        field.setFocusTraversal(false);
        field.setMaxLength(100);

        // Enter → submit
        field.setTextFieldListener((tf, c) -> {
            if (c == '\n' || c == '\r') submit();
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