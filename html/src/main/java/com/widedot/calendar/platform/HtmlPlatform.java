package com.widedot.calendar.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.google.gwt.core.client.GWT;
import com.google.gwt.storage.client.Storage;

public class HtmlPlatform implements PlatformSpecific {
    private Storage localStorage;

    @Override
    public void initialize() {
        localStorage = Storage.getLocalStorageIfSupported();
    }

    @Override
    public void dispose() {
        // Nothing to dispose
    }

    @Override
    public FileHandle getFile(String path) {
        return Gdx.files.internal(path);
    }

    @Override
    public void saveData(String key, String data) {
        if (localStorage != null) {
            localStorage.setItem(key, data);
        }
    }

    @Override
    public String loadData(String key) {
        if (localStorage != null) {
            return localStorage.getItem(key);
        }
        return null;
    }

    @Override
    public boolean isTouchDevice() {
        return true; // Most web browsers support touch
    }

    @Override
    public void openURL(String url) {
        GWT.getWindow().open(url, "_blank");
    }

    @Override
    public void share(String text) {
        if (GWT.isClient()) {
            nativeShare(text);
        }
    }

    private native void nativeShare(String text) /*-{
        if (navigator.share) {
            navigator.share({
                title: 'Calendar Game',
                text: text
            });
        }
    }-*/;

    @Override
    public void vibrate(int duration) {
        if (GWT.isClient()) {
            nativeVibrate(duration);
        }
    }

    private native void nativeVibrate(int duration) /*-{
        if (navigator.vibrate) {
            navigator.vibrate(duration);
        }
    }-*/;

    @Override
    public void showKeyboard(boolean show) {
        // Handled automatically by the browser
    }

    @Override
    public String getDeviceLanguage() {
        return GWT.getLocale();
    }

    @Override
    public String getDeviceModel() {
        return "Web Browser";
    }

    @Override
    public String getDeviceVersion() {
        return GWT.getVersion();
    }
} 