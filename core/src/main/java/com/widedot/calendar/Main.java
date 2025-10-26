package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.platform.PlatformFactory;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private AdventCalendarGame game;
    
    @Override
    public void create() {
        Gdx.app.log("DEBUG", "Début de l'initialisation du jeu");
        Gdx.app.log("DEBUG", "Version de LibGDX: " + Gdx.app.getVersion());
        Gdx.app.log("DEBUG", "Type d'application: " + Gdx.app.getType());
        Gdx.app.log("DEBUG", "Dimensions de la fenêtre: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        Gdx.app.log("DEBUG", "Type de graphics: " + Gdx.graphics.getClass().getName());
        Gdx.app.log("DEBUG", "GL30 disponible: " + (Gdx.graphics.isGL30Available() ? "oui" : "non"));
        Gdx.app.log("DEBUG", "Main.create()");

        // Vérifier que la plateforme est initialisée
        if (PlatformFactory.getPlatform() == null) {
            Gdx.app.error("DEBUG", "Platform not initialized. Make sure to set the platform before creating the game.");
            throw new IllegalStateException("Platform not initialized. Make sure to set the platform before creating the game.");
        }
        
        Gdx.app.log("DEBUG", "before new AdventCalendarGame()");
        game = new AdventCalendarGame();
        Gdx.app.log("DEBUG", "after new AdventCalendarGame()");
        game.create();
        Gdx.app.log("DEBUG", "after AdventCalendarGame.create()");
        setScreen(game.getScreen());
    }
    
    @Override
    public void render() {
        game.render();
    }
    
    @Override
    public void resize(int width, int height) {
        Gdx.app.log("Main", "resize appelé avec dimensions: " + width + "x" + height);
        super.resize(width, height);
        game.resize(width, height);
    }
    
    @Override
    public void dispose() {
        if (game != null) {
            game.dispose();
        }
    }
}
