package com.widedot.calendar.gwt;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.widedot.calendar.Main;
import com.widedot.calendar.platform.PlatformFactory;
import com.widedot.calendar.platform.HtmlPlatform;
import com.google.gwt.core.client.GWT;

/** Launches the GWT application. */
public class GwtLauncher extends GwtApplication {
        @Override
        public GwtApplicationConfiguration getConfig () {
            GWT.log("GwtLauncher: getConfig() CALLED");
            // Resizable application, uses available space in browser with no padding:
            GwtApplicationConfiguration cfg = new GwtApplicationConfiguration(true);
            cfg.padVertical = 0;
            cfg.padHorizontal = 0;
            GWT.log("GwtLauncher: getConfig() returned");
            return cfg;
            // If you want a fixed size application, comment out the above resizable section,
            // and uncomment below:
            //return new GwtApplicationConfiguration(640, 480);
        }

        @Override
        public ApplicationListener createApplicationListener () {
            GWT.log("GwtLauncher: createApplicationListener() CALLED");
            // Inject the platform ONLY after LibGDX is fully initialized.
            return new Main() {
                @Override
                public void create() {
                    GWT.log("GwtLauncher: Main.create() (anonymous inner class) CALLED - ENTRY");
                    try {
                        GWT.log("GwtLauncher: Attempting PlatformFactory.setPlatform()");
                        PlatformFactory.setPlatform(new HtmlPlatform());
                        GWT.log("GwtLauncher: PlatformFactory.setPlatform() - SUCCESS");
                    } catch (Throwable t) {
                        GWT.log("GwtLauncher: ERROR during PlatformFactory.setPlatform(): " + t.getMessage());
                        throw t; // Re-throw if you want it to halt, or handle
                    }
                    
                    try {
                        GWT.log("GwtLauncher: Attempting super.create()");
                        super.create();
                        GWT.log("GwtLauncher: super.create() - SUCCESS (returned from Main.create())");
                    } catch (Throwable t) {
                        GWT.log("GwtLauncher: ERROR during super.create(): " + t.getMessage());
                        throw t; // Re-throw
                    }
                    GWT.log("GwtLauncher: Main.create() (anonymous inner class) - EXIT");
                }
            };
        }
}
