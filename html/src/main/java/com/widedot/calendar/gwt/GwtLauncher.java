package com.widedot.calendar.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader;
import com.badlogic.gdx.Gdx;
import com.widedot.calendar.Main;
import com.widedot.calendar.platform.PlatformFactory;
import com.widedot.calendar.platform.HtmlPlatform;
import com.widedot.calendar.platform.PlatformRegistry;
import com.widedot.calendar.IosInsetsBridge;
import com.google.gwt.core.client.GWT;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        
        @Override
        public Preloader.PreloaderCallback getPreloaderCallback() {
            return createPreloaderPanel(GWT.getHostPageBaseURL() + "logo.png");
        }

        @Override
        public GwtApplicationConfiguration getConfig () {
            logToConsole("GwtLauncher: getConfig() CALLED");
            // Resizable application, uses available space in browser with no padding:
            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
            cfg.padVertical = 0;
            cfg.padHorizontal = 0;
            cfg.useDebugGL = true; // Activer les logs
            
            // Note: preventDefault n'est pas disponible dans GwtApplicationConfiguration
            // La gestion du preventDefault se fait au niveau CSS avec touch-action: none
            
            logToConsole("GwtLauncher: getConfig() returned with preventDefault=false");
            return cfg;
        }

        @Override
        public ApplicationListener createApplicationListener () {
            logToConsole("GwtLauncher: createApplicationListener() CALLED");
            // Inject the platform ONLY after LibGDX is fully initialized.
            return new Main() {
                @Override
                public void create() {
                    logToConsole("GwtLauncher: Main.create() (anonymous inner class) CALLED - ENTRY");
                    
                    // Activer le niveau de log maximum
                    if (Gdx.app != null) {
                        Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
                        logToConsole("GwtLauncher: Log level set to DEBUG");
                    }
                    
                    try {
                        logToConsole("GwtLauncher: Attempting PlatformFactory.setPlatform()");
                        PlatformFactory.setPlatform(new HtmlPlatform());
                        logToConsole("GwtLauncher: PlatformFactory.setPlatform() - SUCCESS");
                    } catch (Throwable t) {
                        logToConsole("GwtLauncher: ERROR during PlatformFactory.setPlatform(): " + t.getMessage());
                        logStackTrace(t);
                        throw t;
                    }
                    
                    try {
                        logToConsole("GwtLauncher: Attempting PlatformRegistry.set()");
                        PlatformRegistry.set(new GwtPlatformInfo());
                        logToConsole("GwtLauncher: PlatformRegistry.set() - SUCCESS");
                    } catch (Throwable t) {
                        logToConsole("GwtLauncher: ERROR during PlatformRegistry.set(): " + t.getMessage());
                        logStackTrace(t);
                        throw t;
                    }
                    
                    try {
                        logToConsole("GwtLauncher: Installing IosInsetsBridge");
                        IosInsetsBridge.install();
                        logToConsole("GwtLauncher: IosInsetsBridge installed - SUCCESS");
                    } catch (Throwable t) {
                        logToConsole("GwtLauncher: ERROR during IosInsetsBridge installation: " + t.getMessage());
                        logStackTrace(t);
                        // Ne pas faire échouer l'app pour ça
                    }
                    
                    try {
                        logToConsole("GwtLauncher: Attempting super.create()");
                        super.create();
                        logToConsole("GwtLauncher: super.create() - SUCCESS (returned from Main.create())");
                    } catch (Throwable t) {
                        logToConsole("GwtLauncher: ERROR during super.create(): " + t.getMessage());
                        logStackTrace(t);
                        throw t;
                    }
                    logToConsole("GwtLauncher: Main.create() (anonymous inner class) - EXIT");
                }
            };
        }
        
        /**
         * Log directement dans la console JavaScript du navigateur.
         * Cette méthode utilise JSNI (JavaScript Native Interface) pour contourner GWT.log()
         */
        private static void logToConsole(String message) {
            jsConsoleLog(message);
        }
        
        /**
         * Méthode JSNI pour logger directement dans console.log()
         */
        private static native void jsConsoleLog(String message) /*-{
            console.log(message);
        }-*/;
        
        /**
         * Log la stack trace d'une exception
         */
        private static void logStackTrace(Throwable t) {
            logToConsole("Stack trace:");
            logToConsole(getStackTraceAsString(t));
        }
        
        /**
         * Convertit une stack trace en String
         */
        private static String getStackTraceAsString(Throwable t) {
            StringBuilder sb = new StringBuilder();
            sb.append(t.getClass().getName()).append(": ").append(t.getMessage()).append("\n");
            for (StackTraceElement element : t.getStackTrace()) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
            if (t.getCause() != null) {
                sb.append("Caused by: ").append(getStackTraceAsString(t.getCause()));
            }
            return sb.toString();
        }
}
