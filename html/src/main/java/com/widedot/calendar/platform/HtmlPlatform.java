package com.widedot.calendar.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Input;

public class HtmlPlatform implements PlatformSpecific {
    private Preferences preferences;

    @Override
    public void initialize() {
        // Do not access Gdx.app here because it might not be initialized yet in GWT.
        // Preferences will be created lazily when first needed.
    }

    @Override
    public void dispose() {
        if (preferences != null) {
            try {
            preferences.flush();
            } catch (Exception ignored) {
            }
        }
    }

    private Preferences prefs() {
        if (preferences == null && Gdx.app != null) {
            preferences = Gdx.app.getPreferences("calendar-game");
        }
        return preferences;
    }

    @Override
    public FileHandle getFile(String path) {
        return Gdx.files.internal(path);
    }

    @Override
    public void saveData(String key, String data) {
        Preferences p = prefs();
        if (p != null) {
            p.putString(key, data);
            p.flush();
        }
    }

    @Override
    public String loadData(String key) {
        Preferences p = prefs();
        return p == null ? null : p.getString(key, null);
    }

    @Override
    public boolean isTouchDevice() {
        return Gdx.input.isPeripheralAvailable(Input.Peripheral.MultitouchScreen);
    }

    @Override
    public void openURL(String url) {
        Gdx.net.openURI(url);
    }

    @Override
    public void share(String text) {
        // Sharing not supported in HTML platform
        Gdx.app.log("HtmlPlatform", "Sharing not supported in HTML platform");
    }

    @Override
    public void vibrate(int duration) {
        // Vibration not supported in HTML platform
        Gdx.app.log("HtmlPlatform", "Vibration not supported in HTML platform");
    }

    @Override
    public void showKeyboard(boolean show) {
        // Handled automatically by the browser
    }

    @Override
    public String getDeviceLanguage() {
        return "en";
    }

    @Override
    public String getDeviceModel() {
        return "Web Browser";
    }

    @Override
    public String getDeviceVersion() {
        return "1.0";
    }
} 