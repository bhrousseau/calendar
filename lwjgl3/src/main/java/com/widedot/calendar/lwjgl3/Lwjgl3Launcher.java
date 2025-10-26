package com.widedot.calendar.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.widedot.calendar.Main;
import com.widedot.calendar.platform.PlatformFactory;
import com.widedot.calendar.platform.Lwjgl3Platform;
import com.widedot.calendar.platform.PlatformRegistry;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        
        Lwjgl3ApplicationConfiguration config = getDefaultConfiguration();
        new Lwjgl3Application(new Main() {
            @Override
            public void create() {
                // Initialiser la plateforme après que LibGDX soit prêt
                PlatformFactory.setPlatform(new Lwjgl3Platform());
                // Enregistrer l'info de plateforme pour le système d'input
                PlatformRegistry.set(new DesktopPlatformInfo());
                super.create();
            }
        }, config);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("calendar");
        //// Vsync limits the frames per second to what your hardware can display, and helps eliminate
        //// screen tearing. This setting doesn't always work on Linux, so the line after is a safeguard.
        configuration.useVsync(true);
        //// Limits FPS to the refresh rate of the currently active monitor, plus 1 to try to match fractional
        //// refresh rates. The Vsync setting above should limit the actual FPS to match the monitor.
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        //// If you remove the above line and set Vsync to false, you can get unlimited FPS, which can be
        //// useful for testing performance, but can also be very stressful to some hardware.
        //// You may also need to configure GPU drivers to fully disable Vsync; this can cause screen tearing.

        // Démarrer en mode fenêtré avec une taille raisonnable
        configuration.setWindowedMode(1280, 720);
        configuration.setResizable(true); // Permettre le redimensionnement
        
        //// You can change these files; they are in lwjgl3/src/main/resources/ .
        //// They can also be loaded from the root of assets/ .
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
        return configuration;
    }
}