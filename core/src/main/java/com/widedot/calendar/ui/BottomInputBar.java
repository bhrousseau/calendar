package com.widedot.calendar.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.widedot.calendar.platform.PlatformRegistry;

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
    
    // Curseur clignotant
    private float cursorBlinkTimer = 0f;
    private static final float CURSOR_BLINK_INTERVAL = 0.5f; // 0.5 seconde
    private boolean cursorVisible = true;

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
            if (textFieldStyle.font != null) {
                textFieldStyle.font.getData().setScale(3.0f);
            }
        } catch (Exception e) {
            textFieldStyle = new TextField.TextFieldStyle();
            textFieldStyle.font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
            textFieldStyle.font.getData().setScale(3.0f);
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
    public void act(float delta) {
        super.act(delta);
        
        // Gérer le clignotement du curseur
        cursorBlinkTimer += delta;
        if (cursorBlinkTimer >= CURSOR_BLINK_INTERVAL) {
            cursorBlinkTimer = 0f;
            cursorVisible = !cursorVisible;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // ✅ Dessiner le rectangle noir semi-transparent derrière la barre
        Color old = batch.getColor();
        batch.setColor(0, 0, 0, 0.6f); // 60% d'opacité
        batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight());
        batch.setColor(old);

        super.draw(batch, parentAlpha);
        
        // Dessiner le curseur clignotant si le champ a le focus
        if (field.hasKeyboardFocus() && cursorVisible) {
            drawCursor(batch);
        }
    }

    /**
     * Dessine le curseur clignotant vertical
     */
    private void drawCursor(Batch batch) {
        // Calculer la position du curseur à la fin du texte
        String text = field.getText();
        com.badlogic.gdx.graphics.g2d.BitmapFont font = field.getStyle().font;
        
        if (font != null) {
            // Mesurer la largeur du texte
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout();
            layout.setText(font, text);
            float textWidth = layout.width;
            
            // Position du curseur (à la fin du texte + padding)
            float cursorX = field.getX() + 10 + textWidth; // 10px de padding
            float cursorY = field.getY() + 5; // 5px de padding vertical
            float cursorHeight = field.getHeight() - 10; // Hauteur du curseur
            
            // Dessiner le curseur vertical (pipe)
            Color oldColor = batch.getColor();
            batch.setColor(Color.WHITE);
            batch.draw(backgroundTexture, cursorX, cursorY, 2, cursorHeight); // 2px de largeur
            batch.setColor(oldColor);
        }
    }

    /** Appelé à la fermeture de l'app pour nettoyer la texture */
    public static void disposeStaticResources() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
            backgroundTexture = null;
        }
    }
}