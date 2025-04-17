package com.widedot.calendar;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;

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
        
        game = new AdventCalendarGame();
        setScreen(new AdventCalendarScreen(game));
    }
    
    @Override
    public void render() {
        screen.render(Gdx.graphics.getDeltaTime());
    }
    
    @Override
    public void dispose() {
        if (screen != null) {
            screen.dispose();
        }
        if (game != null) {
            game.dispose();
        }
    }
}
