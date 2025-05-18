package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.platform.PlatformFactory;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game {
    private AdventCalendarGame game;
    
    @Override
    public void create() {
        System.out.println("Début de l'initialisation du jeu");
        System.out.println("Version de LibGDX: " + Gdx.app.getVersion());
        System.out.println("Type d'application: " + Gdx.app.getType());
        System.out.println("Dimensions de la fenêtre: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());
        System.out.println("Type de graphics: " + Gdx.graphics.getClass().getName());
        System.out.println("GL30 disponible: " + (Gdx.graphics.isGL30Available() ? "oui" : "non"));
        
        // Vérifier que la plateforme est initialisée
        if (PlatformFactory.getPlatform() == null) {
            throw new IllegalStateException("Platform not initialized. Make sure to set the platform before creating the game.");
        }
        
        game = new AdventCalendarGame();
        game.create();
        setScreen(game.getScreen());
    }
    
    @Override
    public void render() {
        game.render();
    }
    
    @Override
    public void resize(int width, int height) {
        System.out.println("Main.resize appelé avec dimensions: " + width + "x" + height);
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
